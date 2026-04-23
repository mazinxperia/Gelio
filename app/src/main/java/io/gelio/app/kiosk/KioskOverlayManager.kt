package io.gelio.app.kiosk

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible

class KioskOverlayManager(
    private val context: Context,
) {
    private val windowManager by lazy {
        context.getSystemService(WindowManager::class.java)
    }

    private var topShield: View? = null
    private var bottomShield: View? = null
    private var installedTopHeightPx: Int = -1
    private var installedBottomHeightPx: Int = -1

    val isInstalled: Boolean
        get() = topShield?.isAttachedToWindow == true && bottomShield?.isAttachedToWindow == true

    fun install(
        topHeightPx: Int,
        bottomHeightPx: Int,
    ) {
        if (!Settings.canDrawOverlays(context)) return
        if (isInstalled && installedTopHeightPx == topHeightPx && installedBottomHeightPx == bottomHeightPx) return
        remove()
        val topView = buildShieldView()
        val bottomView = buildShieldView()
        windowManager?.addView(
            topView,
            shieldLayoutParams(gravity = Gravity.TOP, heightPx = topHeightPx),
        )
        windowManager?.addView(
            bottomView,
            shieldLayoutParams(gravity = Gravity.BOTTOM, heightPx = bottomHeightPx),
        )
        topShield = topView
        bottomShield = bottomView
        installedTopHeightPx = topHeightPx
        installedBottomHeightPx = bottomHeightPx
    }

    fun remove() {
        topShield?.removeFromWindow()
        bottomShield?.removeFromWindow()
        topShield = null
        bottomShield = null
        installedTopHeightPx = -1
        installedBottomHeightPx = -1
    }

    private fun buildShieldView(): View =
        View(context).apply {
            isClickable = true
            isFocusable = false
            isVisible = true
            setBackgroundColor(0x01000000)
            setOnTouchListener { view, motionEvent ->
                if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
                true
            }
        }

    private fun shieldLayoutParams(gravity: Int, heightPx: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            title = "GelioKioskShield"
        }

    private fun View.removeFromWindow() {
        runCatching { windowManager?.removeView(this) }
    }
}
