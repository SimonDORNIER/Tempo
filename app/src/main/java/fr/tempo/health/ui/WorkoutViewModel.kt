package fr.tempo.health.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.tempo.health.TempoHealthApplication
import fr.tempo.health.data.WorkoutHistoryEntity
import fr.tempo.health.domain.ExerciseCategory
import fr.tempo.health.domain.RecoveryResult
import fr.tempo.health.domain.WorkoutHistoryHint
import fr.tempo.health.domain.WorkoutPlan
import fr.tempo.health.domain.WorkoutPlanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WorkoutPhase {
    READY,
    PREPARE,
    WORK,
    REST,
    COMPLETE
}

data class WorkoutUiState(
    val plan: WorkoutPlan? = null,
    val exerciseIndex: Int = 0,
    val phase: WorkoutPhase = WorkoutPhase.READY,
    val remainingSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val paused: Boolean = false,
    val midpointPlayed: Boolean = false,
    val completedExercises: Int = 0,
    val perceivedDifficulty: Int? = null,
    val historyId: Long? = null
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TempoHealthApplication
    private val repository = app.workoutRepository

    private val preferences = application.getSharedPreferences(
        "tempo-health-settings",
        Application.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(WorkoutUiState())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()

    val history: StateFlow<List<WorkoutHistoryEntity>> =
        repository.observeRecent()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    private val _soundVolume = MutableStateFlow(
        preferences.getInt("sound-volume", 70).coerceIn(0, 100)
    )
    val soundVolume: StateFlow<Int> = _soundVolume.asStateFlow()

    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private var sessionStartedAtEpochMs: Long? = null
    private var sessionRecorded = false

    init {
        rebuildToneGenerator()
    }

    fun previewPlan(recovery: RecoveryResult): WorkoutPlan =
        WorkoutPlanner.build(
            recovery = recovery,
            history = history.value.map { it.toHint() }
        )

    fun prepare(recovery: RecoveryResult) {
        timerJob?.cancel()
        sessionStartedAtEpochMs = null
        sessionRecorded = false

        _state.value = WorkoutUiState(
            plan = previewPlan(recovery),
            exerciseIndex = 0,
            phase = WorkoutPhase.READY,
            remainingSeconds = 0
        )
    }

    fun start() {
        val plan = _state.value.plan ?: return
        if (plan.items.isEmpty()) return

        sessionStartedAtEpochMs = System.currentTimeMillis()
        sessionRecorded = false

        _state.value = _state.value.copy(
            exerciseIndex = 0,
            phase = WorkoutPhase.PREPARE,
            remainingSeconds = PREPARE_SECONDS,
            elapsedSeconds = 0,
            paused = false,
            midpointPlayed = false,
            completedExercises = 0,
            perceivedDifficulty = null,
            historyId = null
        )
        playStartTone()
        launchTimer()
    }

    fun togglePause() {
        if (_state.value.phase == WorkoutPhase.READY ||
            _state.value.phase == WorkoutPhase.COMPLETE
        ) {
            return
        }

        _state.value = _state.value.copy(
            paused = !_state.value.paused
        )
    }

    fun skip() {
        val state = _state.value
        val plan = state.plan ?: return
        if (state.phase == WorkoutPhase.COMPLETE) return

        val nextIndex = state.exerciseIndex + 1
        if (nextIndex >= plan.items.size) {
            completeSession()
        } else {
            _state.value = state.copy(
                exerciseIndex = nextIndex,
                phase = WorkoutPhase.PREPARE,
                remainingSeconds = PREPARE_SECONDS,
                midpointPlayed = false,
                paused = false
            )
            playStartTone()
        }
    }

    fun stop() {
        timerJob?.cancel()
        recordSessionIfNeeded(completed = false)
        _state.value = WorkoutUiState()
        sessionStartedAtEpochMs = null
    }

    fun rateDifficulty(difficulty: Int) {
        val safe = difficulty.coerceIn(1, 5)
        _state.value = _state.value.copy(perceivedDifficulty = safe)

        val historyId = _state.value.historyId
        if (historyId != null) {
            viewModelScope.launch {
                repository.setDifficulty(historyId, safe)
            }
        }
    }

    fun setSoundVolume(volume: Int) {
        val safe = volume.coerceIn(0, 100)
        _soundVolume.value = safe
        preferences.edit().putInt("sound-volume", safe).apply()
        rebuildToneGenerator()
        playMidpointTone()
    }

    private fun launchTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.phase != WorkoutPhase.COMPLETE &&
                _state.value.plan != null
            ) {
                delay(1000)

                val state = _state.value
                if (state.paused ||
                    state.phase == WorkoutPhase.READY ||
                    state.phase == WorkoutPhase.COMPLETE
                ) {
                    continue
                }

                val newRemaining = (state.remainingSeconds - 1).coerceAtLeast(0)
                val newElapsed = state.elapsedSeconds + 1
                var midpointPlayed = state.midpointPlayed

                if (state.phase == WorkoutPhase.WORK && !midpointPlayed) {
                    val current = currentItem(state)
                    if (current != null &&
                        newRemaining <= current.workSeconds / 2
                    ) {
                        playMidpointTone()
                        midpointPlayed = true
                    }
                }

                _state.value = state.copy(
                    remainingSeconds = newRemaining,
                    elapsedSeconds = newElapsed,
                    midpointPlayed = midpointPlayed
                )

                if (newRemaining == 0) {
                    advancePhase()
                }
            }
        }
    }

    private fun advancePhase() {
        val state = _state.value
        val plan = state.plan ?: return
        val current = currentItem(state) ?: return

        when (state.phase) {
            WorkoutPhase.PREPARE -> {
                _state.value = state.copy(
                    phase = WorkoutPhase.WORK,
                    remainingSeconds = current.workSeconds,
                    midpointPlayed = false
                )
                playStartTone()
            }

            WorkoutPhase.WORK -> {
                playEndTone()

                _state.value = state.copy(
                    completedExercises = (state.completedExercises + 1)
                        .coerceAtMost(plan.items.size)
                )

                if (current.restSeconds > 0 &&
                    state.exerciseIndex < plan.items.lastIndex
                ) {
                    _state.value = _state.value.copy(
                        phase = WorkoutPhase.REST,
                        remainingSeconds = current.restSeconds,
                        midpointPlayed = false
                    )
                } else {
                    moveToNextExercise()
                }
            }

            WorkoutPhase.REST -> {
                moveToNextExercise()
            }

            else -> Unit
        }
    }

    private fun moveToNextExercise() {
        val state = _state.value
        val plan = state.plan ?: return
        val nextIndex = state.exerciseIndex + 1

        if (nextIndex > plan.items.lastIndex) {
            completeSession()
            return
        }

        _state.value = state.copy(
            exerciseIndex = nextIndex,
            phase = WorkoutPhase.PREPARE,
            remainingSeconds = PREPARE_SECONDS,
            midpointPlayed = false
        )
        playStartTone()
    }

    private fun completeSession() {
        timerJob?.cancel()
        _state.value = _state.value.copy(
            phase = WorkoutPhase.COMPLETE,
            remainingSeconds = 0,
            paused = false
        )
        playCompleteTone()
        recordSessionIfNeeded(completed = true)
    }

    private fun recordSessionIfNeeded(completed: Boolean) {
        if (sessionRecorded) return

        val state = _state.value
        val plan = state.plan ?: return
        val startedAt = sessionStartedAtEpochMs ?: return
        if (state.elapsedSeconds <= 0) return

        sessionRecorded = true

        viewModelScope.launch {
            val id = repository.saveSession(
                plan = plan,
                startedAtEpochMs = startedAt,
                endedAtEpochMs = System.currentTimeMillis(),
                durationSeconds = state.elapsedSeconds,
                completedExercises = state.completedExercises,
                completed = completed
            )

            val difficulty = _state.value.perceivedDifficulty
            if (difficulty != null) {
                repository.setDifficulty(id, difficulty)
            }

            _state.value = _state.value.copy(historyId = id)
        }
    }

    private fun currentItem(state: WorkoutUiState = _state.value) =
        state.plan?.items?.getOrNull(state.exerciseIndex)

    private fun rebuildToneGenerator() {
        toneGenerator?.release()
        toneGenerator = if (_soundVolume.value > 0) {
            ToneGenerator(
                AudioManager.STREAM_MUSIC,
                _soundVolume.value
            )
        } else {
            null
        }
    }

    private fun playStartTone() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    private fun playMidpointTone() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
    }

    private fun playEndTone() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
    }

    private fun playCompleteTone() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    }

    override fun onCleared() {
        timerJob?.cancel()
        toneGenerator?.release()
        toneGenerator = null
        super.onCleared()
    }

    private fun WorkoutHistoryEntity.toHint(): WorkoutHistoryHint =
        WorkoutHistoryHint(
            startedAtEpochMs = startedAtEpochMs,
            muscleGroups = muscleGroupsCsv
                .split(",")
                .mapNotNull { raw ->
                    runCatching { ExerciseCategory.valueOf(raw) }.getOrNull()
                }
                .toSet(),
            perceivedDifficulty = perceivedDifficulty
        )

    companion object {
        private const val PREPARE_SECONDS = 5
    }
}
