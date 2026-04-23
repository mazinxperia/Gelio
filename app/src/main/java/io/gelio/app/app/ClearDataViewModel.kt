package io.gelio.app.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.gelio.app.data.cleardata.ClearDataPhase
import io.gelio.app.data.cleardata.ClearDataProgress
import io.gelio.app.data.cleardata.ClearDataScanResult
import io.gelio.app.data.cleardata.ClearDataSummary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClearDataLogEntry(
    val timestamp: String,
    val phase: ClearDataPhase,
    val message: String,
    val warning: Boolean = false,
    val error: Boolean = false,
)

data class ClearDataUiState(
    val running: Boolean = false,
    val phase: ClearDataPhase = ClearDataPhase.Idle,
    val progress: Float = 0f,
    val progressLabel: String = "Scan app-private data before clearing.",
    val scanResult: ClearDataScanResult? = null,
    val summary: ClearDataSummary? = null,
    val logs: List<ClearDataLogEntry> = emptyList(),
    val completedResetToken: Long = 0L,
)

class ClearDataViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    private val repository = appContainer.clearDataRepository
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _uiState = MutableStateFlow(ClearDataUiState())
    val uiState: StateFlow<ClearDataUiState> = _uiState

    fun clearSession() {
        if (_uiState.value.running) return
        _uiState.value = ClearDataUiState()
    }

    fun scanAppData() {
        if (_uiState.value.running) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    running = true,
                    phase = ClearDataPhase.Scan,
                    progress = 0f,
                    progressLabel = "Scanning",
                    summary = null,
                    logs = listOf(log(ClearDataPhase.Scan, "Scanning app-private storage and content.")),
                )
            }
            runCatching {
                repository.scanAppData(onProgress = ::onProgress)
            }.onSuccess { scanResult ->
                _uiState.update {
                    it.copy(
                        running = false,
                        phase = ClearDataPhase.Ready,
                        progress = 1f,
                        progressLabel = "Scan complete",
                        scanResult = scanResult,
                        logs = (it.logs + log(ClearDataPhase.Ready, "Found ${scanResult.totalFiles} private entries and ${scanResult.counts.totalRows} content rows.")).takeLast(MAX_LOGS),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        running = false,
                        phase = ClearDataPhase.Error,
                        progressLabel = "Scan failed",
                        logs = (it.logs + log(ClearDataPhase.Error, error.message ?: "Scan failed.", error = true)).takeLast(MAX_LOGS),
                    )
                }
            }
        }
    }

    fun clearEverything(onAfterSuccessfulReset: () -> Unit) {
        val scanResult = _uiState.value.scanResult ?: return
        if (_uiState.value.running) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    running = true,
                    phase = ClearDataPhase.Clearing,
                    progress = 0f,
                    progressLabel = "Clearing",
                    summary = null,
                    logs = listOf(log(ClearDataPhase.Clearing, "Factory reset started. Downloads backups will be left untouched.")),
                )
            }
            runCatching {
                repository.clearAllAppData(scanResult, onProgress = ::onProgress)
            }.onSuccess { summary ->
                onAfterSuccessfulReset()
                _uiState.update {
                    val warningLogs = summary.warnings.map { warning ->
                        log(ClearDataPhase.Complete, warning, warning = true)
                    }
                    it.copy(
                        running = false,
                        phase = ClearDataPhase.Complete,
                        progress = 1f,
                        progressLabel = "Factory reset complete",
                        scanResult = null,
                        summary = summary,
                        completedResetToken = it.completedResetToken + 1L,
                        logs = (it.logs + warningLogs + log(
                            ClearDataPhase.Complete,
                            "Cleared ${summary.contentRowsCleared} content rows and deleted ${summary.deletedEntries} private media/cache entries.",
                        )).takeLast(MAX_LOGS),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        running = false,
                        phase = ClearDataPhase.Error,
                        progressLabel = "Reset failed",
                        logs = (it.logs + log(ClearDataPhase.Error, error.message ?: "Factory reset failed.", error = true)).takeLast(MAX_LOGS),
                    )
                }
            }
        }
    }

    fun resetAppImmediately(onAfterSuccessfulReset: () -> Unit = {}) {
        if (_uiState.value.running) return
        viewModelScope.launch {
            runCatching {
                repository.wipeAllAppData()
            }.onSuccess {
                onAfterSuccessfulReset()
            }
        }
    }

    private fun onProgress(progress: ClearDataProgress) {
        _uiState.update {
            it.copy(
                phase = progress.phase,
                progress = progress.progress.coerceIn(0f, 1f),
                progressLabel = progress.message,
                logs = (it.logs + log(
                    phase = progress.phase,
                    message = progress.message,
                    warning = progress.warning,
                )).takeLast(MAX_LOGS),
            )
        }
    }

    private fun log(
        phase: ClearDataPhase,
        message: String,
        warning: Boolean = false,
        error: Boolean = false,
    ): ClearDataLogEntry =
        ClearDataLogEntry(
            timestamp = LocalTime.now().format(timeFormatter),
            phase = phase,
            message = message,
            warning = warning,
            error = error,
        )

    companion object {
        private const val MAX_LOGS = 80

        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ClearDataViewModel(appContainer)
                }
            }
    }
}
