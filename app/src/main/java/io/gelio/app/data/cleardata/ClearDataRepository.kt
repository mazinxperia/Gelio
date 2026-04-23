package io.gelio.app.data.cleardata

import android.content.Context
import android.os.storage.StorageManager
import io.gelio.app.data.local.dao.ShowcaseDao
import io.gelio.app.data.local.db.ShowcaseDatabase
import io.gelio.app.data.repository.SettingsRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClearDataRepository(
    private val context: Context,
    private val dao: ShowcaseDao,
    private val settingsRepository: SettingsRepository,
    private val database: ShowcaseDatabase,
) {
    suspend fun scanAppData(
        onProgress: (ClearDataProgress) -> Unit,
    ): ClearDataScanResult = withContext(Dispatchers.IO) {
        onProgress(ClearDataProgress(ClearDataPhase.Scan, "Scanning database rows.", 0.12f))
        val counts = readContentCounts()

        onProgress(ClearDataProgress(ClearDataPhase.Scan, "Measuring database files.", 0.28f))
        val databaseBucket = fileBucket(
            key = ClearDataBucketKey.Database,
            label = "Database",
            roots = databaseFiles(),
        )
        if (databaseBucket.bytes > 0L && availableBytes(context.filesDir) < databaseBucket.bytes) {
            onProgress(
                ClearDataProgress(
                    phase = ClearDataPhase.Scan,
                    message = "Low free space: database compacting may be skipped, but reset can still clear content.",
                    progress = 0.32f,
                    warning = true,
                ),
            )
        }

        onProgress(ClearDataProgress(ClearDataPhase.Scan, "Measuring app-private media.", 0.46f))
        val mediaBucket = fileBucket(
            key = ClearDataBucketKey.Media,
            label = "App media",
            roots = listOf(mediaRoot()),
        )

        onProgress(ClearDataProgress(ClearDataPhase.Scan, "Measuring cache.", 0.64f))
        val cacheBucket = fileBucket(
            key = ClearDataBucketKey.Cache,
            label = "Cache",
            roots = listOf(context.cacheDir),
        )

        onProgress(ClearDataProgress(ClearDataPhase.Scan, "Measuring settings.", 0.82f))
        val settingsBucket = fileBucket(
            key = ClearDataBucketKey.Settings,
            label = "Settings",
            roots = listOf(settingsFile()),
        )

        val measuredBuckets = listOf(databaseBucket, mediaBucket, cacheBucket, settingsBucket)
        val totalBucket = ClearDataBucket(
            key = ClearDataBucketKey.Total,
            label = "Total",
            bytes = measuredBuckets.sumOf { it.bytes },
            files = measuredBuckets.sumOf { it.files },
        )
        onProgress(ClearDataProgress(ClearDataPhase.Ready, "Scan complete. Review before clearing.", 1f))
        ClearDataScanResult(
            buckets = measuredBuckets + totalBucket,
            counts = counts,
        )
    }

    suspend fun clearAllAppData(
        scanResult: ClearDataScanResult,
        onProgress: (ClearDataProgress) -> Unit,
    ): ClearDataSummary = withContext(Dispatchers.IO) {
        val warnings = mutableListOf<String>()
        var deletedEntries = 0
        var deletedBytes = 0L

        onProgress(ClearDataProgress(ClearDataPhase.Clearing, "Clearing Room content tables.", 0.12f))
        dao.replaceAllContent(
            featuredProjects = emptyList(),
            virtualTours = emptyList(),
            videos = emptyList(),
            brochures = emptyList(),
            destinations = emptyList(),
            services = emptyList(),
            globalLinks = emptyList(),
        )

        onProgress(ClearDataProgress(ClearDataPhase.Clearing, "Compacting database storage.", 0.24f))
        compactDatabase(warnings)

        onProgress(ClearDataProgress(ClearDataPhase.Clearing, "Resetting app settings.", 0.36f))
        settingsRepository.clearAllSettings()

        onProgress(ClearDataProgress(ClearDataPhase.Clearing, "Deleting app-private media.", 0.58f))
        deleteTree(mediaRoot(), deleteRoot = true, warnings = warnings).also {
            deletedEntries += it.entries
            deletedBytes += it.bytes
        }

        onProgress(ClearDataProgress(ClearDataPhase.Clearing, "Deleting cache contents.", 0.78f))
        deleteTree(context.cacheDir, deleteRoot = false, warnings = warnings).also {
            deletedEntries += it.entries
            deletedBytes += it.bytes
        }

        onProgress(ClearDataProgress(ClearDataPhase.Complete, "Factory reset complete.", 1f))
        ClearDataSummary(
            deletedEntries = deletedEntries,
            deletedBytes = deletedBytes,
            warnings = warnings,
            contentRowsCleared = scanResult.counts.totalRows,
        )
    }

    suspend fun wipeAllAppData(
        onProgress: (ClearDataProgress) -> Unit = {},
    ): ClearDataSummary = withContext(Dispatchers.IO) {
        val scan = scanAppData(onProgress)
        clearAllAppData(scan, onProgress)
    }

    private suspend fun readContentCounts(): ClearDataContentCounts =
        ClearDataContentCounts(
            companies = dao.getAllCompaniesSnapshot().size,
            sections = dao.getAllSectionsSnapshot().size,
            featuredProjects = dao.getAllFeaturedProjectsSnapshot().size,
            virtualTours = dao.getAllVirtualToursSnapshot().size,
            videos = dao.getAllVideosSnapshot().size,
            brochures = dao.getAllBrochuresSnapshot().size,
            destinations = dao.getAllDestinationsSnapshot().size,
            services = dao.getAllServicesSnapshot().size,
            globalLinks = dao.getAllGlobalLinksSnapshot().size,
            worldMapSections = dao.getAllWorldMapSectionsSnapshot().size,
            worldMapPins = dao.getAllWorldMapPinsSnapshot().size,
            reviewCards = dao.getAllReviewCardsSnapshot().size,
            contentPageCards = dao.getAllContentPageCardsSnapshot().size,
            artGalleryHeroes = dao.getAllArtGalleryHeroesSnapshot().size,
            artGalleryCards = dao.getAllArtGalleryCardsSnapshot().size,
            remoteImageAssets = dao.getAllRemoteImageAssetsSnapshot().size,
        )

    private fun compactDatabase(warnings: MutableList<String>) {
        runCatching {
            val writableDatabase = database.openHelper.writableDatabase
            writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            writableDatabase.execSQL("VACUUM")
        }.onFailure {
            warnings += "Database content was cleared, but compacting storage failed: ${it.message ?: it::class.java.simpleName}"
        }
    }

    private fun fileBucket(
        key: ClearDataBucketKey,
        label: String,
        roots: List<File>,
    ): ClearDataBucket {
        val stats = roots.fold(FileStats()) { acc, root ->
            acc + measureTree(root)
        }
        return ClearDataBucket(
            key = key,
            label = label,
            bytes = stats.bytes,
            files = stats.entries,
        )
    }

    private fun measureTree(root: File): FileStats {
        if (!root.exists()) return FileStats()
        if (root.isFile) return FileStats(entries = 1, bytes = root.length())
        var entries = 0
        var bytes = 0L
        root.walkTopDown().forEach { file ->
            if (file != root) entries += 1
            if (file.isFile) bytes += file.length()
        }
        return FileStats(entries, bytes)
    }

    private fun availableBytes(root: File): Long =
        runCatching {
            val storageManager = context.getSystemService(StorageManager::class.java)
            val uuid = storageManager?.getUuidForPath(root)
            if (storageManager != null && uuid != null) storageManager.getAllocatableBytes(uuid) else root.usableSpace
        }.getOrDefault(root.usableSpace)

    private fun deleteTree(
        root: File,
        deleteRoot: Boolean,
        warnings: MutableList<String>,
    ): FileStats {
        if (!root.exists()) return FileStats()
        var entries = 0
        var bytes = 0L
        val files = root.walkBottomUp().toList()
        files.forEach { file ->
            if (!deleteRoot && file == root) return@forEach
            val fileBytes = if (file.isFile) file.length() else 0L
            val deleted = runCatching { file.delete() }.getOrDefault(false)
            if (deleted) {
                entries += 1
                bytes += fileBytes
            } else if (file.exists()) {
                warnings += "Could not delete ${file.absolutePath}"
            }
        }
        return FileStats(entries, bytes)
    }

    private fun databaseFiles(): List<File> {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        return listOf(
            databaseFile,
            File("${databaseFile.absolutePath}-wal"),
            File("${databaseFile.absolutePath}-shm"),
        )
    }

    private fun mediaRoot(): File = File(context.filesDir, "showcase_media")

    private fun settingsFile(): File = File(context.filesDir, "datastore/$SETTINGS_DATASTORE_FILE")

    private data class FileStats(
        val entries: Int = 0,
        val bytes: Long = 0L,
    ) {
        operator fun plus(other: FileStats): FileStats =
            FileStats(entries = entries + other.entries, bytes = bytes + other.bytes)
    }

    private companion object {
        const val DATABASE_NAME = "gelio.db"
        const val SETTINGS_DATASTORE_FILE = "gelio_settings.preferences_pb"
    }
}
