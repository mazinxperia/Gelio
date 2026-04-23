package io.gelio.app

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import io.gelio.app.app.GelioApp
import io.gelio.app.app.GelioRoot
import io.gelio.app.core.performance.FrameTracker
import io.gelio.app.core.performance.PerfLog

class MainActivity : ComponentActivity() {
    private lateinit var kioskBackCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        val tCreate = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)
        window.decorView.isHapticFeedbackEnabled = false
        val app = application as GelioApp
        kioskBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (app.appContainer.kioskController.shouldBlockBack()) return
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
        onBackPressedDispatcher.addCallback(this, kioskBackCallback)

        FrameTracker.attach(this)
        // Kiosk: keep CPU/GPU clocks stable over long idle runs.
        runCatching { window.setSustainedPerformanceMode(true) }
        PerfLog.d("LIFE", "MainActivity.onCreate start")

        setContent {
            GelioRoot(appContainer = app.appContainer)
        }
        PerfLog.d("LIFE", "MainActivity.onCreate done in ${SystemClock.elapsedRealtime() - tCreate}ms")
    }

    override fun onResume() {
        super.onResume()
        (application as? GelioApp)?.appContainer?.let { appContainer ->
            appContainer.recordUserInteraction()
            appContainer.kioskController.reapply(this)
        }
        PerfLog.d("LIFE", "MainActivity.onResume")
    }

    override fun onPause() {
        super.onPause()
        PerfLog.d("LIFE", "MainActivity.onPause")
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        (application as? GelioApp)?.appContainer?.recordUserInteraction()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (shouldConsumeKioskKey(keyCode)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (shouldConsumeKioskKey(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            (application as? GelioApp)?.appContainer?.kioskController?.reapply(this)
        }
    }

    private fun shouldConsumeKioskKey(keyCode: Int): Boolean {
        val kioskController = (application as? GelioApp)?.appContainer?.kioskController
        if (kioskController?.shouldConsumeVolume() != true) return false
        return keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
    }
}
