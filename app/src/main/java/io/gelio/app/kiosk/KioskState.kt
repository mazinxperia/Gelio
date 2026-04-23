package io.gelio.app.kiosk

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KioskPermissionState(
    val overlay: Boolean = false,
    val usageAccess: Boolean = false,
    val exactAlarms: Boolean = false,
    val ignoreBatteryOptimizations: Boolean = false,
    val accessibility: Boolean = false,
    val defaultHome: Boolean = false,
) {
    val allGranted: Boolean
        get() = overlay && usageAccess && exactAlarms && ignoreBatteryOptimizations && accessibility && defaultHome
}

data class KioskRuntimeState(
    val desiredEnabled: Boolean = false,
    val permissionState: KioskPermissionState = KioskPermissionState(),
    val active: Boolean = false,
    val adminMaintenance: Boolean = false,
    val grantSession: Boolean = false,
) {
    val enforcingClientLockdown: Boolean
        get() = active && !adminMaintenance && !grantSession
}

internal object KioskRuntimeRegistry {
    private val _adminMaintenance = MutableStateFlow(false)
    private val _grantSession = MutableStateFlow(false)
    private val _currentRoute = MutableStateFlow("")
    private val _manualSleepActive = MutableStateFlow(false)

    val adminMaintenance = _adminMaintenance.asStateFlow()
    val grantSession = _grantSession.asStateFlow()
    val currentRoute = _currentRoute.asStateFlow()
    val manualSleepActive = _manualSleepActive.asStateFlow()

    fun setAdminMaintenance(value: Boolean) {
        _adminMaintenance.value = value
    }

    fun setGrantSession(value: Boolean) {
        _grantSession.value = value
    }

    fun setCurrentRoute(route: String) {
        _currentRoute.value = route
    }

    fun setManualSleepActive(value: Boolean) {
        _manualSleepActive.value = value
    }
}
