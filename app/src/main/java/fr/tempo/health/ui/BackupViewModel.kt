package fr.tempo.health.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.tempo.health.TempoHealthApplication
import fr.tempo.health.data.BackupPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val configured: Boolean = false,
    val accessible: Boolean = false,
    val stale: Boolean = false,
    val busy: Boolean = false,
    val lastBackupEpochMs: Long? = null,
    val restorePreview: BackupPreview? = null,
    val message: String? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = (application as TempoHealthApplication).fullBackupManager

    private var pendingRestoreUri: Uri? = null

    private val _state = MutableStateFlow(readStatus())

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

            _state.value = readStatus().copy(
                busy = false,
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
            _state.value = readStatus().copy(
                busy = false,
                message = result.message
            )
        }
    }

    fun inspectRestore(uri: Uri) {
        if (_state.value.busy) return

        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            val preview = withContext(Dispatchers.IO) {
                manager.inspect(uri)
            }

            pendingRestoreUri = if (preview != null) uri else null
            _state.value = readStatus().copy(
                busy = false,
                restorePreview = preview,
                message = if (preview == null) {
                    "Ce fichier n'est pas une sauvegarde Tempo Health valide."
                } else {
                    "Sauvegarde vérifiée. Confirme la restauration."
                }
            )
        }
    }

    fun confirmRestore(
        onComplete: (Boolean) -> Unit = {}
    ) {
        val uri = pendingRestoreUri ?: return
        if (_state.value.busy) return

        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            val result = withContext(Dispatchers.IO) {
                manager.restoreFrom(uri)
            }
            if (result.success) {
                pendingRestoreUri = null
            }
            _state.value = readStatus().copy(
                busy = false,
                restorePreview = if (result.success) null else _state.value.restorePreview,
                message = result.message
            )
            onComplete(result.success)
        }
    }

    fun cancelRestore() {
        pendingRestoreUri = null
        _state.value = _state.value.copy(
            restorePreview = null,
            message = null
        )
    }

    fun refreshStatus() {
        _state.value = readStatus().copy(
            message = _state.value.message,
            restorePreview = _state.value.restorePreview
        )
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun readStatus(): BackupUiState {
        val status = manager.status()
        return BackupUiState(
            configured = status.configured,
            accessible = status.accessible,
            stale = status.stale,
            lastBackupEpochMs = status.lastBackupEpochMs
        )
    }
}
