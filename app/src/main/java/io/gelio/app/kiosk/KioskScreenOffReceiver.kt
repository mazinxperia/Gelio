package io.gelio.app.kiosk

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class KioskScreenOffReceiver : BroadcastReceiver() {
    @SuppressLint("Wakelock")
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val kioskController = context.appContainer().kioskController
        if (action == Intent.ACTION_SCREEN_ON) {
            kioskController.onScreenTurnedOn()
            return
        }
        if (action != Intent.ACTION_SCREEN_OFF) return

        val state = kioskController.runtimeState.value
        if (!state.active || state.grantSession) return
        if (kioskController.isManualSleepActive()) return

        val powerManager = context.getSystemService(PowerManager::class.java)
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "gelio:kiosk-screen-off",
        )
        wakeLock?.acquire(2_500L)
        context.launchGelioToFront()
    }
}
