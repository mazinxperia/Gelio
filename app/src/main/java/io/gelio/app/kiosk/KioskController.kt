package io.gelio.app.kiosk

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.gelio.app.app.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class KioskController(
    private val appContainer: AppContainer,
) {
    private val appContext = appContainer.applicationContext
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val permissionsHelper = KioskPermissions(appContext)
    private val overlayManager = KioskOverlayManager(appContext)
    private val permissionState = MutableStateFlow(permissionsHelper.currentState())
    private val desiredEnabled = appContainer.settingsRepository.settings
        .map { it.kioskModeEnabled }
        .stateIn(controllerScope, SharingStarted.Eagerly, false)
    private val _lastKnownPermissionState = MutableStateFlow(permissionState.value)
    private val screenOffReceiver = KioskScreenOffReceiver()
    private var currentActivityRef: WeakReference<Activity>? = null
    private var screenOffRegistered = false
    private var heartbeatJob: Job? = null
    private var rapidRehideJob: Job? = null
    private var insetsListenerInstalled = false
    private var systemUiGuardInstalled = false

    val runtimeState: StateFlow<KioskRuntimeState> = combine(
        desiredEnabled,
        permissionState,
        KioskRuntimeRegistry.adminMaintenance,
        KioskRuntimeRegistry.grantSession,
    ) { desired, permissions, adminMaintenance, grantSession ->
        KioskRuntimeState(
            desiredEnabled = desired,
            permissionState = permissions,
            active = desired && permissions.allGranted,
            adminMaintenance = adminMaintenance,
            grantSession = grantSession,
        )
    }.stateIn(
        controllerScope,
        SharingStarted.Eagerly,
        KioskRuntimeState(),
    )

    fun enable(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        controllerScope.launch {
            refreshPermissionState()
            if (!permissionState.value.allGranted) return@launch
            appContainer.settingsRepository.updateKioskModeEnabled(true)
            applyRuntimeState(activity, desiredOverride = true)
        }
    }

    fun disable(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        controllerScope.launch {
            KioskRuntimeRegistry.setGrantSession(false)
            appContainer.settingsRepository.updateKioskModeEnabled(false)
            stopRuntime(activity)
        }
    }

    fun reapply(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        controllerScope.launch {
            refreshPermissionState()
            applyRuntimeState(activity)
        }
    }

    fun beginGrantSession() {
        KioskRuntimeRegistry.setGrantSession(true)
        currentActivityRef?.get()?.let { activity ->
            controllerScope.launch {
                applyRuntimeState(activity)
            }
        }
    }

    fun endGrantSession() {
        KioskRuntimeRegistry.setGrantSession(false)
        currentActivityRef?.get()?.let { activity ->
            controllerScope.launch {
                refreshPermissionState()
                applyRuntimeState(activity)
            }
        }
    }

    fun updateCurrentRoute(route: String) {
        KioskRuntimeRegistry.setCurrentRoute(route)
        KioskRuntimeRegistry.setAdminMaintenance(route.startsWith("admin"))
        currentActivityRef?.get()?.let { activity ->
            controllerScope.launch {
                applyRuntimeState(activity)
            }
        }
    }

    fun reapplyCurrentState() {
        currentActivityRef?.get()?.let { activity ->
            controllerScope.launch {
                refreshPermissionState()
                applyRuntimeState(activity)
            }
        }
    }

    fun refreshPermissionState() {
        val fresh = permissionsHelper.currentState()
        permissionState.value = fresh
        _lastKnownPermissionState.value = fresh
    }

    fun requestManualSleep(): Boolean {
        val state = currentRuntimeState()
        if (!state.active) return false
        cancelSelfHeal()
        currentActivityRef?.get()?.let { activity ->
            clearWakeFlags(activity)
        }
        KioskRuntimeRegistry.setManualSleepActive(true)
        val locked = KioskAccessibilityService.requestLockScreen()
        if (!locked) {
            KioskRuntimeRegistry.setManualSleepActive(false)
            currentActivityRef?.get()?.let { activity ->
                controllerScope.launch { applyRuntimeState(activity) }
            }
        }
        return locked
    }

    fun isManualSleepActive(): Boolean = KioskRuntimeRegistry.manualSleepActive.value

    fun onScreenTurnedOn() {
        if (KioskRuntimeRegistry.manualSleepActive.value) {
            KioskRuntimeRegistry.setManualSleepActive(false)
            currentActivityRef?.get()?.let { activity ->
                controllerScope.launch {
                    applyRuntimeState(activity)
                }
            }
        }
    }

    fun lastKnownPermissionState(): KioskPermissionState = _lastKnownPermissionState.value

    fun shouldBlockBack(): Boolean {
        val state = runtimeState.value
        val route = KioskRuntimeRegistry.currentRoute.value
        return state.active && !route.startsWith("admin")
    }

    fun shouldConsumeVolume(): Boolean = runtimeState.value.active

    fun isForegroundPackageAllowed(packageName: String?): Boolean {
        val candidate = packageName?.takeIf { it.isNotBlank() } ?: return true
        if (candidate == appContext.packageName) return true
        if (candidate == "com.android.systemui" || candidate == "android") return true
        val state = runtimeState.value
        if (!state.active) return true
        if (state.grantSession || state.adminMaintenance) {
            return candidate in maintenanceAllowedPackages
        }
        return false
    }

    fun handleSystemUiIntrusion() {
        if (isManualSleepActive()) return
        currentActivityRef?.get()?.let { activity ->
            controllerScope.launch {
                if (!currentRuntimeState().enforcingClientLockdown) return@launch
                forceHideBars(activity)
                installEdgeShields(activity)
                startRapidRehide(activity)
            }
        }
    }

    private fun applyRuntimeState(
        activity: Activity,
        desiredOverride: Boolean? = null,
    ) {
        val state = currentRuntimeState(desiredOverride)
        if (!state.active) {
            stopRuntime(activity)
            return
        }

        applyWakeAndFullscreenFlags(activity)
        startWatchdogService()
        registerScreenOffReceiver()
        if (isManualSleepActive()) {
            cancelSelfHeal()
        } else {
            scheduleSelfHeal()
        }

        if (state.enforcingClientLockdown) {
            applyImmersiveMode(activity)
            installSystemUiGuard(activity)
            installInsetsGuard(activity)
            installEdgeShields(activity)
            startHeartbeat()
        } else {
            stopHeartbeat()
            removeSystemUiGuard(activity)
            overlayManager.remove()
            showSystemBars(activity)
        }
    }

    private fun stopRuntime(activity: Activity) {
        KioskRuntimeRegistry.setManualSleepActive(false)
        stopRapidRehide()
        stopHeartbeat()
        removeSystemUiGuard(activity)
        removeInsetsGuard(activity)
        overlayManager.remove()
        unregisterScreenOffReceiver()
        cancelSelfHeal()
        stopWatchdogService()
        clearWakeFlags(activity)
        showSystemBars(activity)
    }

    private fun applyWakeAndFullscreenFlags(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        }
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }

    private fun clearWakeFlags(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(false)
            activity.setTurnScreenOn(false)
        }
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveMode(activity: Activity) {
        val window = activity.window
        val decorView = window.decorView
        // FLAG_LAYOUT_NO_LIMITS lets us draw edge-to-edge so system bars cannot reclaim
        // any layout slot even if they transiently appear.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
        )
        WindowCompat.getInsetsController(window, decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        @Suppress("DEPRECATION")
        run {
            decorView.systemUiVisibility = immersiveFlags()
        }
        // Re-hide twice on a short delay — some OEMs reveal bars for the first
        // ~200ms of a new window focus. Catching them early prevents the "sticky
        // peek" behaviour the user reported.
        decorView.postDelayed({ forceHideBars(activity) }, 100L)
        decorView.postDelayed({ forceHideBars(activity) }, 400L)
    }

    private fun forceHideBars(activity: Activity) {
        val window = activity.window ?: return
        @Suppress("DEPRECATION")
        run {
            window.decorView.systemUiVisibility = immersiveFlags()
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        // Lenovo still lets bars peek through during edge drags. Re-assert twice
        // on the next frames so transient bars are dismissed immediately.
        window.decorView.postDelayed({
            @Suppress("DEPRECATION")
            run { window.decorView.systemUiVisibility = immersiveFlags() }
            WindowCompat.getInsetsController(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }, 16L)
        window.decorView.postDelayed({
            @Suppress("DEPRECATION")
            run { window.decorView.systemUiVisibility = immersiveFlags() }
            WindowCompat.getInsetsController(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }, 80L)
    }

    private fun installEdgeShields(activity: Activity) {
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val topInset = insets?.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
        )?.top ?: 0
        val bottomInset = listOf(
            insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0,
            insets?.getInsets(WindowInsetsCompat.Type.systemGestures())?.bottom ?: 0,
            insets?.getInsets(WindowInsetsCompat.Type.tappableElement())?.bottom ?: 0,
        ).maxOrNull() ?: 0

        // Keep the top shield large enough to eat shade/icon taps once SystemUI peeks in,
        // but still below the app's top-left controls on most screens.
        val topHeightPx = ((if (topInset > 0) topInset else dp(28)) + dp(12))
            .coerceIn(dp(32), dp(52))
        // Bottom shield is intentionally larger: if Android's nav bar appears, the
        // user should hit the shield first rather than Home / Recents / Back.
        val bottomHeightPx = ((if (bottomInset > 0) bottomInset else dp(32)) + dp(36))
            .coerceIn(dp(56), dp(88))

        overlayManager.install(
            topHeightPx = topHeightPx,
            bottomHeightPx = bottomHeightPx,
        )
    }

    private fun installSystemUiGuard(activity: Activity) {
        if (systemUiGuardInstalled) return
        val decorView = activity.window.decorView
        @Suppress("DEPRECATION")
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (!currentRuntimeState().enforcingClientLockdown) return@setOnSystemUiVisibilityChangeListener
            val navVisible = visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
            val statusVisible = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0
            if (navVisible || statusVisible) {
                forceHideBars(activity)
                installEdgeShields(activity)
                startRapidRehide(activity)
            }
        }
        systemUiGuardInstalled = true
    }

    /**
     * Installs a WindowInsets listener on the activity's decor view. Whenever the
     * system attempts to reveal the status or navigation bars (via swipe, transient
     * display, or OEM gesture), this listener immediately re-hides them on the
     * same frame — so the user sees at most a single-frame flicker.
     */
    private fun installInsetsGuard(activity: Activity) {
        val decorView = activity.window.decorView
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            if (currentRuntimeState().enforcingClientLockdown) {
                val systemBarsVisible = insets.isVisible(WindowInsetsCompat.Type.systemBars())
                val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                if (systemBarsVisible && !imeVisible) {
                    forceHideBars(activity)
                }
            }
            insets
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            decorView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    if (currentRuntimeState().enforcingClientLockdown) {
                        val systemBarsVisible = insets.isVisible(WindowInsetsCompat.Type.systemBars())
                        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                        if (systemBarsVisible && !imeVisible) {
                            decorView.post {
                                forceHideBars(activity)
                                installEdgeShields(activity)
                                startRapidRehide(activity)
                            }
                        }
                    }
                    return insets
                }
            },
        )
        insetsListenerInstalled = true
    }

    private fun removeInsetsGuard(activity: Activity) {
        if (!insetsListenerInstalled) return
        ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView, null)
        ViewCompat.setWindowInsetsAnimationCallback(activity.window.decorView, null)
        insetsListenerInstalled = false
    }

    private fun removeSystemUiGuard(activity: Activity) {
        if (!systemUiGuardInstalled) return
        @Suppress("DEPRECATION")
        activity.window.decorView.setOnSystemUiVisibilityChangeListener(null)
        systemUiGuardInstalled = false
    }

    private fun showSystemBars(activity: Activity) {
        val decorView = activity.window.decorView
        WindowCompat.getInsetsController(activity.window, decorView)
            .show(WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    /**
     * Kiosk heartbeat — every 1.5s while active, re-assert immersive mode and
     * make sure the current edge shields still match the live inset sizes.
     */
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = controllerScope.launch {
            while (isActive) {
                val activity = currentActivityRef?.get()
                if (activity != null && currentRuntimeState().enforcingClientLockdown) {
                    forceHideBars(activity)
                    installEdgeShields(activity)
                }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun startRapidRehide(activity: Activity) {
        rapidRehideJob?.cancel()
        rapidRehideJob = controllerScope.launch {
            repeat(RAPID_REHIDE_PULSES) {
                if (!currentRuntimeState().enforcingClientLockdown || isManualSleepActive()) return@launch
                forceHideBars(activity)
                installEdgeShields(activity)
                delay(RAPID_REHIDE_INTERVAL_MS)
            }
        }
    }

    private fun stopRapidRehide() {
        rapidRehideJob?.cancel()
        rapidRehideJob = null
    }

    private fun registerScreenOffReceiver() {
        if (screenOffRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            appContext,
            screenOffReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        screenOffRegistered = true
    }

    private fun unregisterScreenOffReceiver() {
        if (!screenOffRegistered) return
        runCatching { appContext.unregisterReceiver(screenOffReceiver) }
        screenOffRegistered = false
    }

    private fun startWatchdogService() {
        val intent = Intent(appContext, KioskWatchdogService::class.java)
        ContextCompat.startForegroundService(appContext, intent)
    }

    private fun stopWatchdogService() {
        appContext.stopService(Intent(appContext, KioskWatchdogService::class.java))
    }

    private fun scheduleSelfHeal() {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !permissionsHelper.currentState().exactAlarms) {
            return
        }
        val triggerAt = System.currentTimeMillis() + SELF_HEAL_INTERVAL_MS
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            restartPendingIntent(appContext),
        )
    }

    private fun cancelSelfHeal() {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(restartPendingIntent(appContext))
    }

    private fun currentRuntimeState(desiredOverride: Boolean? = null): KioskRuntimeState {
        val desired = desiredOverride ?: desiredEnabled.value
        val permissions = permissionState.value
        val adminMaintenance = KioskRuntimeRegistry.adminMaintenance.value
        val grantSession = KioskRuntimeRegistry.grantSession.value
        return KioskRuntimeState(
            desiredEnabled = desired,
            permissionState = permissions,
            active = desired && permissions.allGranted,
            adminMaintenance = adminMaintenance,
            grantSession = grantSession,
        )
    }

    companion object {
        private const val SELF_HEAL_INTERVAL_MS = 15 * 60 * 1000L
        private const val HEARTBEAT_INTERVAL_MS = 250L
        private const val RAPID_REHIDE_INTERVAL_MS = 16L
        private const val RAPID_REHIDE_PULSES = 40
        internal const val RESTART_ALARM_REQUEST_CODE = 44012
        internal const val RESTART_ALARM_ACTION = "io.gelio.app.kiosk.RESTART_ALARM"
        private val maintenanceAllowedPackages = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.google.android.gms",
        )

        internal fun restartPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                RESTART_ALARM_REQUEST_CODE,
                Intent(context, KioskRestartReceiver::class.java).setAction(RESTART_ALARM_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        @Suppress("DEPRECATION")
        private fun immersiveFlags(): Int =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}
