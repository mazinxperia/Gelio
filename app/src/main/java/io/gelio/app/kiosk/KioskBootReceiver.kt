package io.gelio.app.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KioskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in handledActions) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val appContainer = context.appContainer()
                val desiredEnabled = appContainer.settingsRepository.settings.first().kioskModeEnabled
                appContainer.kioskController.refreshPermissionState()
                if (desiredEnabled && appContainer.kioskController.lastKnownPermissionState().allGranted) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, KioskWatchdogService::class.java),
                    )
                    context.launchGelioToFront()
                }
            }
            pendingResult.finish()
        }
    }

    companion object {
        private val handledActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
        )
    }
}
