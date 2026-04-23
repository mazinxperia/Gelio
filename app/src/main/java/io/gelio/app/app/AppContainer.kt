package io.gelio.app.app

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.RoomDatabase
import io.gelio.app.data.backup.BackupRepository
import io.gelio.app.data.cleardata.ClearDataRepository
import io.gelio.app.data.local.db.ShowcaseDatabase
import io.gelio.app.data.media.MediaMaintenanceRepository
import io.gelio.app.kiosk.KioskController
import io.gelio.app.data.repository.PexelsRepository
import io.gelio.app.data.repository.SettingsRepository
import io.gelio.app.data.repository.ShowcaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val Context.dataStore by preferencesDataStore(name = "gelio_settings")

class AppContainer(
    application: Application,
) {
    val applicationContext: Context = application.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _lastInteractionMillis = MutableStateFlow(SystemClock.elapsedRealtime())

    private val database: ShowcaseDatabase by lazy {
        Room.databaseBuilder(
            application,
            ShowcaseDatabase::class.java,
            "gelio.db",
        ).addMigrations(
            ShowcaseDatabase.MIGRATION_1_2,
            ShowcaseDatabase.MIGRATION_2_3,
            ShowcaseDatabase.MIGRATION_3_4,
            ShowcaseDatabase.MIGRATION_4_5,
            ShowcaseDatabase.MIGRATION_5_6,
            ShowcaseDatabase.MIGRATION_6_7,
            ShowcaseDatabase.MIGRATION_7_8,
            ShowcaseDatabase.MIGRATION_8_9,
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(application.dataStore)
    }

    val showcaseRepository: ShowcaseRepository by lazy {
        ShowcaseRepository(
            context = application,
            dao = database.showcaseDao(),
            applicationScope = applicationScope,
        )
    }

    val pexelsRepository: PexelsRepository by lazy {
        PexelsRepository(
            context = application,
            dao = database.showcaseDao(),
        )
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            context = application,
            dao = database.showcaseDao(),
            settingsRepository = settingsRepository,
        )
    }

    val clearDataRepository: ClearDataRepository by lazy {
        ClearDataRepository(
            context = application,
            dao = database.showcaseDao(),
            settingsRepository = settingsRepository,
            database = database,
        )
    }

    val mediaMaintenanceRepository: MediaMaintenanceRepository by lazy {
        MediaMaintenanceRepository(
            context = application,
            dao = database.showcaseDao(),
            applicationScope = applicationScope,
            lastInteractionMillis = _lastInteractionMillis,
        )
    }

    val kioskController: KioskController by lazy {
        KioskController(this)
    }

    val showcaseInitialized: StateFlow<Boolean>
        get() = showcaseRepository.initialized

    val lastInteractionMillis: StateFlow<Long>
        get() = _lastInteractionMillis

    fun warmUp() {
        showcaseRepository.warmUp()
        mediaMaintenanceRepository.warmUp()
    }

    fun recordUserInteraction(now: Long = SystemClock.elapsedRealtime()) {
        if (now - _lastInteractionMillis.value >= USER_INTERACTION_THROTTLE_MS) {
            _lastInteractionMillis.value = now
        }
    }

    private companion object {
        const val USER_INTERACTION_THROTTLE_MS = 250L
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not available.")
}
