package io.gelio.app.kiosk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.gelio.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KioskWatchdogService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchdogJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = appContainer().kioskController
        controller.refreshPermissionState()
        if (!controller.runtimeState.value.active) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (watchdogJob == null) {
            watchdogJob = serviceScope.launch {
                val usageStatsManager = getSystemService(UsageStatsManager::class.java)
                while (isActive) {
                    val state = controller.runtimeState.value
                    if (!state.active || state.grantSession || controller.isManualSleepActive()) {
                        delay(POLL_INTERVAL_MS)
                        continue
                    }
                    val foregroundPackage = usageStatsManager?.currentForegroundPackage()
                    if (!controller.isForegroundPackageAllowed(foregroundPackage)) {
                        controller.handleSystemUiIntrusion()
                        launchGelioToFront()
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        watchdogJob?.cancel()
        watchdogJob = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Gelio kiosk protection")
            .setContentText("Keeping Gelio in the foreground.")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gelio kiosk watchdog",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            setShowBadge(false)
            description = "Keeps Gelio in kiosk mode."
        }
        manager.createNotificationChannel(channel)
    }

    private fun UsageStatsManager.currentForegroundPackage(): String? {
        val endTime = System.currentTimeMillis()
        val events = queryEvents(endTime - 2_000L, endTime)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundPackage = event.packageName
            }
        }
        return lastForegroundPackage
    }

    companion object {
        private const val CHANNEL_ID = "gelio_kiosk_watchdog"
        private const val NOTIFICATION_ID = 44011
        private const val POLL_INTERVAL_MS = 500L
    }
}
