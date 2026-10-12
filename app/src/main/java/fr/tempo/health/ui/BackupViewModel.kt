package fr.tempo.health.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.tempo.health.TempoHealthApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val configured: Boolean = false,
    val busy: Boolean = false,
    val lastBackupEpochMs: Long? = null,
    val message: String? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = (application as TempoHealthApplication).fullBackupManager

    private val _state = MutableStateFlow(
        BackupUiState(
            configured = manager.hasDestination(),
            lastBackupEpochMs = manager.lastBackupEpochMs()
        )
    )
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun chooseDestination(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            val remembered = manager.rememberDestination(uri)

            val result = if (remembered) {
                withContext(Dispatchers.IO) {
                    manager.backupToSavedLocation()
                }
            } else {
                null
            }

            _state.value = BackupUiState(
                configured = manager.hasDestination(),
                busy = false,
                lastBackupEpochMs = manager.lastBackupEpochMs(),
                message = result?.message
                    ?: if (remembered) null else "Impossible de mémoriser cet emplacement."
            )
        }
    }

    fun backupNow() {
        if (_state.value.busy) return

        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            val result = withContext(Dispatchers.IO) {
                manager.backupToSavedLocation()
            }
            _state.value = _state.value.copy(
                configured = manager.hasDestination(),
                busy = false,
                lastBackupEpochMs = manager.lastBackupEpochMs(),
                message = result.message
            )
        }
    }

    fun restoreFrom(
        uri: Uri,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (_state.value.busy) return

        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            val result = withContext(Dispatchers.IO) {
                manager.restoreFrom(uri)
            }
            _state.value = _state.value.copy(
                configured = manager.hasDestination(),
                busy = false,
                lastBackupEpochMs = manager.lastBackupEpochMs(),
                message = result.message
            )
            onComplete(result.success)
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
