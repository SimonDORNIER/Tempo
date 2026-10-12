package fr.tempo.health.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import fr.tempo.health.TempoHealthApplication
import fr.tempo.health.reminders.ReminderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TempoHealthApplication
    private val manager = app.reminderManager

    private val _settings = MutableStateFlow(manager.load())
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    fun setMoveEnabled(enabled: Boolean) =
        update(_settings.value.copy(moveEnabled = enabled))

    fun setMoveIntervalHours(hours: Int) =
        update(
            _settings.value.copy(
                moveIntervalHours = hours.coerceIn(1, 4)
            )
        )

    fun setCheckInEnabled(enabled: Boolean) =
        update(_settings.value.copy(checkInEnabled = enabled))

    fun setCheckInHour(hour: Int) =
        update(_settings.value.copy(checkInHour = hour.coerceIn(6, 22)))

    fun setMobilityEnabled(enabled: Boolean) =
        update(_settings.value.copy(mobilityEnabled = enabled))

    fun setMobilityHour(hour: Int) =
        update(_settings.value.copy(mobilityHour = hour.coerceIn(6, 22)))

    fun setWorkoutEnabled(enabled: Boolean) =
        update(_settings.value.copy(workoutEnabled = enabled))

    fun setWorkoutHour(hour: Int) =
        update(_settings.value.copy(workoutHour = hour.coerceIn(6, 22)))

    private fun update(value: ReminderSettings) {
        _settings.value = value
        manager.save(value)
        app.fullBackupManager.requestAutoBackup()
    }
}
