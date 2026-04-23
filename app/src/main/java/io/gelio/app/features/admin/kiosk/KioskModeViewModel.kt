package io.gelio.app.features.admin.kiosk

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.gelio.app.app.AppContainer
import io.gelio.app.kiosk.KioskPermissions
import io.gelio.app.kiosk.KioskRuntimeState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class KioskGrantTarget {
    OVERLAY,
    USAGE,
    EXACT_ALARMS,
    BATTERY,
    ACCESSIBILITY,
    HOME,
}

class KioskModeViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    private val permissions = KioskPermissions(appContainer.applicationContext)

    val settings = appContainer.settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        io.gelio.app.data.model.AppSettings(),
    )

    val runtimeState: StateFlow<KioskRuntimeState> = appContainer.kioskController.runtimeState

    init {
        refreshPermissionState()
    }

    fun refreshPermissionState() {
        appContainer.kioskController.refreshPermissionState()
    }

    fun beginGrantSession() {
        appContainer.kioskController.beginGrantSession()
    }

    fun endGrantSession() {
        appContainer.kioskController.endGrantSession()
    }

    fun permissionIntent(target: KioskGrantTarget): Intent? =
        when (target) {
            KioskGrantTarget.OVERLAY -> permissions.overlayIntent()
            KioskGrantTarget.USAGE -> permissions.usageAccessIntent()
            KioskGrantTarget.EXACT_ALARMS -> permissions.exactAlarmIntent()
            KioskGrantTarget.BATTERY -> permissions.batteryOptimizationIntent()
            KioskGrantTarget.ACCESSIBILITY -> permissions.accessibilityIntent()
            KioskGrantTarget.HOME -> permissions.launcherIntent()
        }

    fun onGrantResult(target: KioskGrantTarget?) {
        viewModelScope.launch {
            if (target == KioskGrantTarget.HOME) {
                val delayMs = permissions.launcherRecheckDelayMs()
                if (delayMs > 0) delay(delayMs)
            }
            endGrantSession()
            refreshPermissionState()
        }
    }

    fun setKioskModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateKioskModeEnabled(enabled)
        }
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    KioskModeViewModel(appContainer)
                }
            }
    }
}
