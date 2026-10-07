package fr.tempo.health.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.tempo.health.TempoHealthApplication
import fr.tempo.health.data.DailyCheckInEntity
import fr.tempo.health.data.DailyHealthEntity
import fr.tempo.health.data.HealthConnectAvailability
import fr.tempo.health.domain.RecoveryEngine
import fr.tempo.health.domain.RecoveryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository =
        (application as TempoHealthApplication).healthRepository

    val requiredPermissions: Set<String> = repository.requiredPermissions

    val recentDays: StateFlow<List<DailyHealthEntity>> =
        repository.observeRecentDays()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val todayCheckIn: StateFlow<DailyCheckInEntity?> =
        repository.observeTodayCheckIn()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null
            )

    val recovery: StateFlow<RecoveryResult> =
        combine(recentDays, todayCheckIn) { days, checkIn ->
            RecoveryEngine.calculate(days, checkIn)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RecoveryEngine.calculate(emptyList(), null)
        )

    private val _availability =
        MutableStateFlow(repository.availability())
    val availability: StateFlow<HealthConnectAvailability> =
        _availability.asStateFlow()

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshPermissionState()
    }

    fun refreshPermissionState() {
        _availability.value = repository.availability()
        if (_availability.value != HealthConnectAvailability.AVAILABLE) {
            _hasPermissions.value = false
            return
        }

        viewModelScope.launch {
            runCatching { repository.hasRequiredPermissions() }
                .onSuccess { granted ->
                    _hasPermissions.value = granted
                    if (granted) {
                        autoSyncIfNeeded()
                    }
                }
                .onFailure {
                    _message.value = "Impossible de vérifier les autorisations."
                }
        }
    }

    fun onPermissionsResult(grantedPermissions: Set<String>) {
        _hasPermissions.value =
            grantedPermissions.containsAll(requiredPermissions)

        if (_hasPermissions.value) {
            _message.value = "Autorisations accordées."
            sync()
        } else {
            _message.value =
                "Certaines données Santé Connect restent non autorisées."
        }
    }

    fun saveCheckIn(energy: Int, pain: Int) {
        viewModelScope.launch {
            repository.saveTodayCheckIn(energy, pain)
        }
    }

    fun sync() {
        launchSync(full = true)
    }

    private fun autoSyncIfNeeded() {
        if (_syncing.value) return

        viewModelScope.launch {
            val shouldSync = runCatching {
                repository.shouldAutoSync()
            }.getOrDefault(true)

            if (shouldSync) {
                launchSync(full = recentDays.value.isEmpty())
            }
        }
    }

    private fun launchSync(full: Boolean) {
        if (_syncing.value) return

        viewModelScope.launch {
            _syncing.value = true
            if (full) {
                _message.value = null
            }

            runCatching {
                if (full) {
                    repository.syncLast28Days()
                } else {
                    repository.syncRecentDays(3)
                }
            }.onSuccess {
                if (full) {
                    _message.value = "Synchronisation des 28 derniers jours terminée."
                }
                _hasPermissions.value = true
            }.onFailure { error ->
                if (full) {
                    _message.value =
                        error.message ?: "Synchronisation impossible."
                }
            }

            _syncing.value = false
        }
    }
}
