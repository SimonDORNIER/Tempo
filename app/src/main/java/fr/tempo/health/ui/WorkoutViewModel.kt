package fr.tempo.health.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.tempo.health.TempoHealthApplication
import fr.tempo.health.data.WorkoutHistoryEntity
import fr.tempo.health.domain.Exercise
import fr.tempo.health.domain.ExerciseCategory
import fr.tempo.health.domain.Equipment
import fr.tempo.health.domain.TrainingPreferences
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
import org.json.JSONArray
import org.json.JSONObject

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

    private val _trainingPreferences = MutableStateFlow(loadTrainingPreferences())
    val trainingPreferences: StateFlow<TrainingPreferences> =
        _trainingPreferences.asStateFlow()

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
            history = history.value.map { it.toHint() },
            preferences = _trainingPreferences.value
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

    fun setDurationMinutes(minutes: Int) {
        updateTrainingPreferences(
            _trainingPreferences.value.copy(
                durationMinutes = minutes.coerceIn(8, 40)
            )
        )
    }

    fun toggleEquipment(equipment: Equipment) {
        val current = _trainingPreferences.value.availableEquipment
        val updated = if (equipment in current) {
            current - equipment
        } else {
            current + equipment
        }

        updateTrainingPreferences(
            _trainingPreferences.value.copy(
                availableEquipment = updated
            )
        )
    }

    fun toggleFavoriteExercise(id: String) {
        val current = _trainingPreferences.value
        val favorites = if (id in current.favoriteExerciseIds) {
            current.favoriteExerciseIds - id
        } else {
            current.favoriteExerciseIds + id
        }

        updateTrainingPreferences(
            current.copy(
                favoriteExerciseIds = favorites,
                avoidedExerciseIds = current.avoidedExerciseIds - id
            )
        )
    }

    fun toggleAvoidedExercise(id: String) {
        val current = _trainingPreferences.value
        val avoided = if (id in current.avoidedExerciseIds) {
            current.avoidedExerciseIds - id
        } else {
            current.avoidedExerciseIds + id
        }

        updateTrainingPreferences(
            current.copy(
                avoidedExerciseIds = avoided,
                favoriteExerciseIds = current.favoriteExerciseIds - id
            )
        )
    }

    fun toggleCategory(category: ExerciseCategory) {
        val current = _trainingPreferences.value
        val disabled = if (category in current.disabledCategories) {
            current.disabledCategories - category
        } else {
            current.disabledCategories + category
        }

        updateTrainingPreferences(
            current.copy(disabledCategories = disabled)
        )
    }

    fun setCategoryIcon(category: ExerciseCategory, icon: String) {
        val allowed = setOf(
            "🔥", "💪", "🦵", "🧠", "🧘", "🤸",
            "⚡", "❤️", "🏃", "🫁", "🦴", "🎯"
        )
        if (icon !in allowed) return

        val current = _trainingPreferences.value
        updateTrainingPreferences(
            current.copy(
                categoryIcons = current.categoryIcons + (category to icon)
            )
        )
    }

    fun resetCategoryIcon(category: ExerciseCategory) {
        val current = _trainingPreferences.value
        updateTrainingPreferences(
            current.copy(
                categoryIcons = current.categoryIcons - category
            )
        )
    }

    fun setExerciseWorkSeconds(id: String, seconds: Int) {
        val safe = seconds.coerceIn(15, 120)
        val current = _trainingPreferences.value

        updateTrainingPreferences(
            current.copy(
                exerciseWorkSeconds =
                    current.exerciseWorkSeconds + (id to safe)
            )
        )
    }

    fun resetExerciseWorkSeconds(id: String) {
        val current = _trainingPreferences.value
        updateTrainingPreferences(
            current.copy(
                exerciseWorkSeconds = current.exerciseWorkSeconds - id
            )
        )
    }

    fun saveExercise(exercise: Exercise) {
        val current = _trainingPreferences.value
        val isCustom = current.customExercises.any { it.id == exercise.id }

        val updated = if (isCustom) {
            current.copy(
                customExercises = current.customExercises.map {
                    if (it.id == exercise.id) exercise else it
                }
            )
        } else {
            current.copy(
                exerciseOverrides =
                    current.exerciseOverrides + (exercise.id to exercise)
            )
        }

        updateTrainingPreferences(updated)
    }

    fun addCustomExercise(): String {
        val id = "user-" + System.currentTimeMillis()
        val exercise = Exercise(
            id = id,
            name = "Nouvel exercice",
            category = ExerciseCategory.MOBILITY,
            defaultWorkSeconds = 40,
            instructions = "Décris ici comment réaliser l'exercice.",
            cue = "Ajoute un conseil simple d'exécution.",
            requiredEquipment = null
        )

        updateTrainingPreferences(
            _trainingPreferences.value.copy(
                customExercises =
                    _trainingPreferences.value.customExercises + exercise
            )
        )
        return id
    }

    fun deleteCustomExercise(id: String) {
        val current = _trainingPreferences.value
        updateTrainingPreferences(
            current.copy(
                customExercises = current.customExercises.filterNot {
                    it.id == id
                },
                favoriteExerciseIds = current.favoriteExerciseIds - id,
                avoidedExerciseIds = current.avoidedExerciseIds - id,
                exerciseWorkSeconds = current.exerciseWorkSeconds - id
            )
        )
    }

    fun resetExerciseOverride(id: String) {
        val current = _trainingPreferences.value
        updateTrainingPreferences(
            current.copy(
                exerciseOverrides = current.exerciseOverrides - id,
                exerciseWorkSeconds = current.exerciseWorkSeconds - id
            )
        )
    }

    fun resetLibraryCustomizations() {
        updateTrainingPreferences(
            _trainingPreferences.value.copy(
                disabledCategories = emptySet(),
                categoryIcons = emptyMap(),
                exerciseWorkSeconds = emptyMap()
            )
        )
    }

    private fun updateTrainingPreferences(value: TrainingPreferences) {
        _trainingPreferences.value = value

        preferences.edit()
            .putInt("training-duration", value.durationMinutes)
            .putString(
                "training-equipment",
                value.availableEquipment.joinToString(",") { it.name }
            )
            .putString(
                "training-favorites",
                value.favoriteExerciseIds.joinToString(",")
            )
            .putString(
                "training-avoided",
                value.avoidedExerciseIds.joinToString(",")
            )
            .putString(
                "training-disabled-categories",
                value.disabledCategories.joinToString(",") { it.name }
            )
            .putString(
                "training-category-icons",
                value.categoryIcons.entries.joinToString("|") {
                    it.key.name + "=" + it.value
                }
            )
            .putString(
                "training-work-seconds",
                value.exerciseWorkSeconds.entries.joinToString("|") {
                    it.key + "=" + it.value
                }
            )
            .putString(
                "training-exercise-overrides",
                exercisesToJson(value.exerciseOverrides.values.toList())
            )
            .putString(
                "training-custom-exercises",
                exercisesToJson(value.customExercises)
            )
            .apply()
    }

    private fun loadTrainingPreferences(): TrainingPreferences {
        val equipmentRaw = preferences.getString(
            "training-equipment",
            "MAT,CHAIR"
        ).orEmpty()

        val equipment = equipmentRaw
            .split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { raw ->
                runCatching { Equipment.valueOf(raw) }.getOrNull()
            }
            .toSet()

        fun csvSet(key: String): Set<String> =
            preferences.getString(key, "")
                .orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()

        val disabledCategories = csvSet("training-disabled-categories")
            .mapNotNull { raw ->
                runCatching { ExerciseCategory.valueOf(raw) }.getOrNull()
            }
            .toSet()

        val categoryIcons = preferences.getString(
            "training-category-icons",
            ""
        ).orEmpty()
            .split("|")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                val category = parts.getOrNull(0)?.let { raw ->
                    runCatching { ExerciseCategory.valueOf(raw) }.getOrNull()
                }
                val icon = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                if (category != null && icon != null) category to icon else null
            }
            .toMap()

        val exerciseWorkSeconds = preferences.getString(
            "training-work-seconds",
            ""
        ).orEmpty()
            .split("|")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                val id = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                val seconds = parts.getOrNull(1)?.toIntOrNull()
                if (id != null && seconds != null) {
                    id to seconds.coerceIn(15, 120)
                } else {
                    null
                }
            }
            .toMap()

        val exerciseOverrides = exercisesFromJson(
            preferences.getString(
                "training-exercise-overrides",
                "[]"
            ).orEmpty()
        ).associateBy { it.id }

        val customExercises = exercisesFromJson(
            preferences.getString(
                "training-custom-exercises",
                "[]"
            ).orEmpty()
        )

        return TrainingPreferences(
            durationMinutes = preferences
                .getInt("training-duration", 20)
                .coerceIn(8, 40),
            availableEquipment = equipment,
            favoriteExerciseIds = csvSet("training-favorites"),
            avoidedExerciseIds = csvSet("training-avoided"),
            disabledCategories = disabledCategories,
            categoryIcons = categoryIcons,
            exerciseWorkSeconds = exerciseWorkSeconds,
            exerciseOverrides = exerciseOverrides,
            customExercises = customExercises
        )
    }


    private fun exercisesToJson(exercises: List<Exercise>): String {
        val array = JSONArray()
        exercises.forEach { exercise ->
            array.put(
                JSONObject()
                    .put("id", exercise.id)
                    .put("name", exercise.name)
                    .put("category", exercise.category.name)
                    .put("seconds", exercise.defaultWorkSeconds)
                    .put("instructions", exercise.instructions)
                    .put("cue", exercise.cue)
                    .put(
                        "equipment",
                        exercise.requiredEquipment?.name ?: ""
                    )
            )
        }
        return array.toString()
    }

    private fun exercisesFromJson(raw: String): List<Exercise> {
        return runCatching {
            val array = JSONArray(raw.ifBlank { "[]" })
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val category = runCatching {
                        ExerciseCategory.valueOf(
                            item.optString("category")
                        )
                    }.getOrDefault(ExerciseCategory.MOBILITY)

                    val equipment = item.optString("equipment")
                        .takeIf { it.isNotBlank() }
                        ?.let { rawEquipment ->
                            runCatching {
                                Equipment.valueOf(rawEquipment)
                            }.getOrNull()
                        }

                    val id = item.optString("id").trim()
                    if (id.isBlank()) continue

                    add(
                        Exercise(
                            id = id,
                            name = item.optString("name", "Exercice"),
                            category = category,
                            defaultWorkSeconds = item
                                .optInt("seconds", 40)
                                .coerceIn(15, 120),
                            instructions = item.optString(
                                "instructions",
                                ""
                            ),
                            cue = item.optString("cue", ""),
                            requiredEquipment = equipment
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
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

    private fun defaultCategoryIcon(category: ExerciseCategory): String =
        when (category) {
            ExerciseCategory.WARMUP -> "🔥"
            ExerciseCategory.FULL_BODY -> "⚡"
            ExerciseCategory.UPPER_BODY -> "💪"
            ExerciseCategory.CORE -> "🧠"
            ExerciseCategory.LOWER_BODY -> "🦵"
            ExerciseCategory.MOBILITY -> "🤸"
            ExerciseCategory.STRETCHING -> "🧘"
        }

    companion object {
        private const val PREPARE_SECONDS = 5
    }
}
