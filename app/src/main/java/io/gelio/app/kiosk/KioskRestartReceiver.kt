package io.gelio.app.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KioskRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != KioskController.RESTART_ALARM_ACTION) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val appContainer = context.appContainer()
                val desiredEnabled = appContainer.settingsRepository.settings.first().kioskModeEnabled
                val kioskController = appContainer.kioskController
                kioskController.refreshPermissionState()
                val powerManager = context.getSystemService(PowerManager::class.java)
                val screenIsOn = powerManager?.isInteractive == true
                if (desiredEnabled && kioskController.lastKnownPermissionState().allGranted) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, KioskWatchdogService::class.java),
                    )
                    if (!kioskController.isManualSleepActive() && screenIsOn) {
                        context.launchGelioToFront()
                        kioskController.reapplyCurrentState()
                    }
                }
            }
            pendingResult.finish()
        }
    }
}
