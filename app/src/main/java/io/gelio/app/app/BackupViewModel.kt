package io.gelio.app.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.gelio.app.data.backup.BackupImportRequest
import io.gelio.app.data.backup.BackupInspection
import io.gelio.app.data.backup.BackupPhase
import io.gelio.app.data.backup.BackupProgress
import io.gelio.app.data.backup.BackupSummary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BackupLogEntry(
    val timestamp: String,
    val phase: BackupPhase,
    val message: String,
    val error: Boolean = false,
)

enum class BackupOperation {
    None,
    Export,
    Import,
}

data class BackupUiState(
    val running: Boolean = false,
    val inspectingImportFile: Boolean = false,
    val phase: BackupPhase = BackupPhase.Idle,
    val progress: Float = 0f,
    val progressLabel: String = "Ready",
    val selectedImportUri: Uri? = null,
    val importInspection: BackupInspection? = null,
    val activeOperation: BackupOperation = BackupOperation.None,
    val completedFiles: Int = 0,
    val totalFiles: Int = 0,
    val completedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val summary: BackupSummary? = null,
    val logs: List<BackupLogEntry> = emptyList(),
)

class BackupViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    private val repository = appContainer.backupRepository
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState

    fun selectImportFile(uri: Uri?) {
        if (_uiState.value.running) return
        if (uri == null) {
            _uiState.update {
                it.copy(
                    selectedImportUri = null,
                    importInspection = null,
                    inspectingImportFile = false,
                    activeOperation = BackupOperation.None,
                    summary = null,
                    progress = 0f,
                    progressLabel = "Ready",
                    completedFiles = 0,
                    totalFiles = 0,
                    completedBytes = 0L,
                    totalBytes = 0L,
                    logs = listOf(log(BackupPhase.Idle, "Import file cleared.")),
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                selectedImportUri = uri,
                importInspection = null,
                inspectingImportFile = true,
                activeOperation = BackupOperation.Import,
                summary = null,
                progress = 0f,
                phase = BackupPhase.Inspect,
                progressLabel = "Selected backup. Starting inspection.",
                completedFiles = 0,
                totalFiles = 0,
                completedBytes = 0L,
                totalBytes = 0L,
                logs = listOf(
                    log(BackupPhase.Inspect, "Selected .kskm import file."),
                ),
            )
        }

        viewModelScope.launch {
            runCatching { repository.inspectBackup(uri, ::onProgress) }
                .onSuccess { inspection ->
                    _uiState.update { state ->
                        if (state.selectedImportUri != uri) {
                            state
                        } else {
                            state.copy(
                                inspectingImportFile = false,
                                phase = BackupPhase.Idle,
                                importInspection = inspection,
                                progressLabel = if (inspection.passwordProtected) {
                                    "Password detected • 100%"
                                } else {
                                    "Inspection complete • 100%"
                                },
                                completedFiles = 0,
                                totalFiles = inspection.fileCount ?: state.totalFiles,
                                completedBytes = 0L,
                                totalBytes = inspection.mediaBytes ?: state.totalBytes,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state.selectedImportUri != uri) {
                            state
                        } else {
                            state.copy(
                                inspectingImportFile = false,
                                phase = BackupPhase.Error,
                                progressLabel = "Import file inspection failed",
                                logs = state.logs + log(
                                    BackupPhase.Error,
                                    error.message ?: "Unable to inspect selected backup.",
                                    error = true,
                                ),
                            )
                        }
                    }
                }
        }
    }

    fun clearSession() {
        if (_uiState.value.running) return
        _uiState.value = BackupUiState()
    }

    fun exportBackup(
        password: String?,
        compressionLevel: Int,
    ) {
        if (_uiState.value.running) return
        viewModelScope.launch {
            runOperation(
                operation = BackupOperation.Export,
                startMessage = "Starting export operation.",
                preserveExistingLogs = false,
            ) {
                repository.exportBackupToDownloads(
                    password = password?.takeIf { it.isNotBlank() },
                    compressionLevel = compressionLevel,
                    onProgress = ::onProgress,
                )
            }
        }
    }

    fun importBackup(
        password: String?,
    ) {
        val inputUri = _uiState.value.selectedImportUri ?: return
        if (_uiState.value.running) return
        viewModelScope.launch {
            runOperation(
                operation = BackupOperation.Import,
                startMessage = "Starting import operation.",
                preserveExistingLogs = true,
            ) {
                repository.importBackup(
                    request = BackupImportRequest(
                        inputUri = inputUri,
                        password = password?.takeIf { it.isNotBlank() },
                    ),
                    onProgress = ::onProgress,
                )
            }
        }
    }

    private suspend fun runOperation(
        operation: BackupOperation,
        startMessage: String,
        preserveExistingLogs: Boolean,
        block: suspend () -> BackupSummary,
    ) {
        _uiState.update {
            val startPhase = if (operation == BackupOperation.Import) BackupPhase.Verify else BackupPhase.Scan
            val updatedLogs = if (preserveExistingLogs && it.logs.isNotEmpty()) {
                it.logs + log(startPhase, startMessage)
            } else {
                listOf(log(startPhase, startMessage))
            }
            it.copy(
                running = true,
                inspectingImportFile = false,
                activeOperation = operation,
                phase = startPhase,
                progress = 0f,
                progressLabel = "Starting",
                completedFiles = 0,
                totalFiles = if (preserveExistingLogs) it.totalFiles else 0,
                completedBytes = 0L,
                totalBytes = if (preserveExistingLogs) it.totalBytes else 0L,
                summary = null,
                logs = updatedLogs,
            )
        }
        runCatching { block() }
            .onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        running = false,
                        phase = BackupPhase.Complete,
                        progress = 1f,
                        progressLabel = "Verified",
                        completedFiles = summary.fileCount,
                        totalFiles = summary.fileCount,
                        completedBytes = summary.totalBytes,
                        totalBytes = summary.totalBytes,
                        summary = summary,
                        logs = it.logs + log(
                            BackupPhase.Complete,
                            "Operation complete: ${summary.fileCount} files verified${summary.outputLabel?.let { label -> " and saved to $label" }.orEmpty()}.",
                        ),
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        running = false,
                        phase = BackupPhase.Error,
                        progressLabel = "Failed",
                        completedFiles = 0,
                        totalFiles = 0,
                        completedBytes = 0L,
                        totalBytes = 0L,
                        logs = it.logs + log(BackupPhase.Error, error.message ?: "Backup operation failed.", error = true),
                    )
                }
            }
    }

    private fun onProgress(progress: BackupProgress) {
        val ratio = when {
            progress.progressFraction != null -> progress.progressFraction
            progress.totalBytes > 0 -> {
            progress.completedBytes.toFloat() / progress.totalBytes.toFloat()
            }
            progress.totalFiles > 0 -> {
            progress.completedFiles.toFloat() / progress.totalFiles.toFloat()
            }
            else -> {
            when (progress.phase) {
                BackupPhase.Idle -> 0f
                BackupPhase.Scan -> 0.08f
                BackupPhase.Repair -> 0.18f
                BackupPhase.Package -> 0.48f
                BackupPhase.Inspect -> 0.32f
                BackupPhase.Verify -> 0.76f
                BackupPhase.Import -> 0.84f
                BackupPhase.Complete -> 1f
                BackupPhase.Error -> 0f
            }
            }
        }.coerceIn(0f, 1f)
        val statusLabel = when (progress.phase) {
            BackupPhase.Inspect -> "Inspection ${((progress.progressFraction ?: ratio) * 100f).roundToInt().coerceIn(0, 100)}% done"
            else -> progress.message
        }

        _uiState.update {
            it.copy(
                phase = progress.phase,
                progress = ratio,
                progressLabel = statusLabel,
                completedFiles = progress.completedFiles,
                totalFiles = progress.totalFiles,
                completedBytes = progress.completedBytes,
                totalBytes = progress.totalBytes,
                logs = it.logs + log(progress.phase, progress.message),
            )
        }
    }

    private fun log(phase: BackupPhase, message: String, error: Boolean = false): BackupLogEntry =
        BackupLogEntry(
            timestamp = LocalTime.now().format(timeFormatter),
            phase = phase,
            message = message,
            error = error,
        )

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    BackupViewModel(appContainer)
                }
            }
    }
}
