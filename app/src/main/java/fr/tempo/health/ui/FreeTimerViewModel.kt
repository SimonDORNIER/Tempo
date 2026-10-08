package fr.tempo.health.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FreeTimerUiState(
    val durationSeconds: Int = 5 * 60,
    val remainingSeconds: Int = 5 * 60,
    val running: Boolean = false,
    val finished: Boolean = false
)

class FreeTimerViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(
        "tempo-health-settings",
        Application.MODE_PRIVATE
    )

    private val initialMinutes =
        preferences.getInt("free-timer-minutes", 5).coerceIn(1, 60)

    private val _state = MutableStateFlow(
        FreeTimerUiState(
            durationSeconds = initialMinutes * 60,
            remainingSeconds = initialMinutes * 60
        )
    )
    val state: StateFlow<FreeTimerUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun setMinutes(minutes: Int) {
        val safe = minutes.coerceIn(1, 60)
        if (_state.value.running) return

        preferences.edit().putInt("free-timer-minutes", safe).apply()
        _state.value = FreeTimerUiState(
            durationSeconds = safe * 60,
            remainingSeconds = safe * 60
        )
    }

    fun toggleRunning() {
        val current = _state.value

        if (current.finished || current.remainingSeconds <= 0) {
            reset()
        }

        if (_state.value.running) {
            timerJob?.cancel()
            _state.value = _state.value.copy(running = false)
        } else {
            _state.value = _state.value.copy(running = true, finished = false)
            launchTimer()
        }
    }

    fun reset() {
        timerJob?.cancel()
        _state.value = _state.value.copy(
            remainingSeconds = _state.value.durationSeconds,
            running = false,
            finished = false
        )
    }

    private fun launchTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.running && _state.value.remainingSeconds > 0) {
                delay(1_000)
                val current = _state.value
                if (!current.running) break

                val next = (current.remainingSeconds - 1).coerceAtLeast(0)
                _state.value = current.copy(remainingSeconds = next)

                if (next == 0) {
                    _state.value = _state.value.copy(
                        running = false,
                        finished = true
                    )
                    playFinishTone()
                }
            }
        }
    }

    private fun playFinishTone() {
        val volume = preferences.getInt("sound-volume", 70).coerceIn(0, 100)
        if (volume <= 0) return

        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)

        viewModelScope.launch {
            delay(1_000)
            tone.release()
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
