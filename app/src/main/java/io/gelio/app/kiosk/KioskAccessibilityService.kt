package io.gelio.app.kiosk

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class KioskAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSystemUiInterventionAt = 0L
    private var topTouchShield: View? = null
    private var bottomTouchShield: View? = null
    private var installedTopShieldHeightPx: Int = -1
    private var installedBottomShieldHeightPx: Int = -1

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceRef = WeakReference(this)
        syncSystemUiTouchShields(
            enabled = applicationContext.appContainer().kioskController.runtimeState.value.enforcingClientLockdown,
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentEvent = event ?: return
        val controller = applicationContext.appContainer().kioskController
        if (controller.isManualSleepActive()) return
        val state = controller.runtimeState.value
        if (!state.enforcingClientLockdown) {
            syncSystemUiTouchShields(enabled = false)
            return
        }
        syncSystemUiTouchShields(enabled = true)

        val sourcePackage = currentEvent.packageName?.toString().orEmpty()
        val className = currentEvent.className?.toString().orEmpty()

        if (sourcePackage == appPackageName()) return

        if (sourcePackage == "com.android.systemui" || sourcePackage == "android") {
            val loweredClass = className.lowercase()
            if (shouldIgnoreSystemUiClass(loweredClass)) return
            interveneAgainstSystemUi(loweredClass, controller)
            return
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        syncSystemUiTouchShields(enabled = false)
        super.onDestroy()
        serviceRef?.get()?.takeIf { it === this }?.let {
            serviceRef = null
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val state = applicationContext.appContainer().kioskController.runtimeState.value
        if (!state.enforcingClientLockdown) return super.onKeyEvent(event)
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_APP_SWITCH -> true
            else -> super.onKeyEvent(event)
        }
    }

    private fun interveneAgainstSystemUi(
        loweredClass: String,
        controller: KioskController,
    ) {
        val now = SystemClock.uptimeMillis()
        if (now - lastSystemUiInterventionAt < SYSTEM_UI_INTERVENTION_COOLDOWN_MS) {
            controller.handleSystemUiIntrusion()
            return
        }
        lastSystemUiInterventionAt = now

        when {
            isNotificationShadeLike(loweredClass) -> dismissNotificationShade()
            isRecentsLike(loweredClass) -> dismissRecents()
            isPowerDialogLike(loweredClass) -> performGlobalAction(GLOBAL_ACTION_BACK)
            else -> performGlobalAction(GLOBAL_ACTION_BACK)
        }

        controller.handleSystemUiIntrusion()

        mainHandler.postDelayed(
            {
                when {
                    isNotificationShadeLike(loweredClass) -> dismissNotificationShade()
                    isRecentsLike(loweredClass) -> dismissRecents()
                    else -> performGlobalAction(GLOBAL_ACTION_BACK)
                }
                controller.handleSystemUiIntrusion()
            },
            FOLLOW_UP_DISMISS_DELAY_MS,
        )
    }

    private fun dismissNotificationShade() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun dismissRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        mainHandler.postDelayed(
            { performGlobalAction(GLOBAL_ACTION_BACK) },
            FOLLOW_UP_RECENTS_BACK_DELAY_MS,
        )
    }

    private fun shouldIgnoreSystemUiClass(loweredClass: String): Boolean =
        loweredClass.contains("inputmethod") ||
            loweredClass.contains("ime") ||
            loweredClass.contains("volume") ||
            loweredClass.contains("biometric")

    private fun isNotificationShadeLike(loweredClass: String): Boolean =
        loweredClass.contains("notification") ||
            loweredClass.contains("shade") ||
            loweredClass.contains("quicksettings") ||
            loweredClass.contains("qs")

    private fun isRecentsLike(loweredClass: String): Boolean =
        loweredClass.contains("recent") ||
            loweredClass.contains("recents") ||
            loweredClass.contains("overview") ||
            loweredClass.contains("fallbackrecents") ||
            loweredClass.contains("taskview")

    private fun isPowerDialogLike(loweredClass: String): Boolean =
        loweredClass.contains("globalactions") ||
            loweredClass.contains("power")

    private fun syncSystemUiTouchShields(enabled: Boolean) {
        if (!enabled) {
            removeTouchShields()
            return
        }

        val topHeightPx = topShieldHeightPx()
        val bottomHeightPx = bottomShieldHeightPx()
        val windowManager = getSystemService(WindowManager::class.java) ?: return

        if (
            topTouchShield?.isAttachedToWindow == true &&
            bottomTouchShield?.isAttachedToWindow == true &&
            installedTopShieldHeightPx == topHeightPx &&
            installedBottomShieldHeightPx == bottomHeightPx
        ) {
            return
        }

        removeTouchShields()

        val topView = buildTouchShield()
        val bottomView = buildTouchShield()

        runCatching {
            windowManager.addView(
                topView,
                shieldLayoutParams(gravity = Gravity.TOP, heightPx = topHeightPx),
            )
            windowManager.addView(
                bottomView,
                shieldLayoutParams(gravity = Gravity.BOTTOM, heightPx = bottomHeightPx),
            )
            topTouchShield = topView
            bottomTouchShield = bottomView
            installedTopShieldHeightPx = topHeightPx
            installedBottomShieldHeightPx = bottomHeightPx
        }.onFailure {
            removeTouchShields()
        }
    }

    private fun topShieldHeightPx(): Int {
        val statusBarHeight = androidSystemDimen("status_bar_height")
        return (statusBarHeight + dp(4)).coerceIn(dp(24), dp(36))
    }

    private fun bottomShieldHeightPx(): Int {
        val navHeight = maxOf(
            androidSystemDimen("navigation_bar_height_landscape"),
            androidSystemDimen("navigation_bar_height"),
            dp(48),
        )
        return (navHeight + dp(8)).coerceIn(dp(52), dp(72))
    }

    @SuppressLint("DiscouragedApi")
    private fun androidSystemDimen(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id != 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildTouchShield(): View =
        View(this).apply {
            isClickable = true
            isFocusable = false
            setBackgroundColor(0x01000000)
            setOnTouchListener { view, motionEvent ->
                if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
                true
            }
        }

    private fun shieldLayoutParams(
        gravity: Int,
        heightPx: Int,
    ): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            title = "GelioKioskA11yShield"
        }

    private fun removeTouchShields() {
        val windowManager = runCatching { getSystemService(WindowManager::class.java) }.getOrNull()
        topTouchShield?.let { view -> runCatching { windowManager?.removeView(view) } }
        bottomTouchShield?.let { view -> runCatching { windowManager?.removeView(view) } }
        topTouchShield = null
        bottomTouchShield = null
        installedTopShieldHeightPx = -1
        installedBottomShieldHeightPx = -1
    }

    private fun appPackageName(): String = applicationContext.packageName

    companion object {
        private const val SYSTEM_UI_INTERVENTION_COOLDOWN_MS = 180L
        private const val FOLLOW_UP_DISMISS_DELAY_MS = 60L
        private const val FOLLOW_UP_RECENTS_BACK_DELAY_MS = 48L

        @Volatile
        private var serviceRef: WeakReference<KioskAccessibilityService>? = null

        fun requestLockScreen(): Boolean {
            val service = serviceRef?.get() ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
