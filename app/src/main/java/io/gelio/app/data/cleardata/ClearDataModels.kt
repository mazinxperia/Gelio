package io.gelio.app.data.cleardata

enum class ClearDataPhase {
    Idle,
    Scan,
    Ready,
    Clearing,
    Complete,
    Error,
}

enum class ClearDataBucketKey {
    Database,
    Media,
    Cache,
    Settings,
    Total,
}

data class ClearDataBucket(
    val key: ClearDataBucketKey,
    val label: String,
    val bytes: Long,
    val files: Int,
)

data class ClearDataContentCounts(
    val companies: Int,
    val sections: Int,
    val featuredProjects: Int,
    val virtualTours: Int,
    val videos: Int,
    val brochures: Int,
    val destinations: Int,
    val services: Int,
    val globalLinks: Int,
    val worldMapSections: Int,
    val worldMapPins: Int,
    val reviewCards: Int,
    val contentPageCards: Int,
    val artGalleryHeroes: Int,
    val artGalleryCards: Int,
    val remoteImageAssets: Int,
) {
    val totalRows: Int
        get() = companies +
            sections +
            featuredProjects +
            virtualTours +
            videos +
            brochures +
            destinations +
            services +
            globalLinks +
            worldMapSections +
            worldMapPins +
            reviewCards +
            contentPageCards +
            artGalleryHeroes +
            artGalleryCards +
            remoteImageAssets
}

data class ClearDataScanResult(
    val buckets: List<ClearDataBucket>,
    val counts: ClearDataContentCounts,
    val warnings: List<String> = emptyList(),
) {
    val totalBytes: Long
        get() = buckets.firstOrNull { it.key == ClearDataBucketKey.Total }?.bytes ?: buckets.sumOf { it.bytes }

    val totalFiles: Int
        get() = buckets.firstOrNull { it.key == ClearDataBucketKey.Total }?.files ?: buckets.sumOf { it.files }
}

data class ClearDataProgress(
    val phase: ClearDataPhase,
    val message: String,
    val progress: Float,
    val warning: Boolean = false,
)

data class ClearDataSummary(
    val deletedEntries: Int,
    val deletedBytes: Long,
    val warnings: List<String>,
    val contentRowsCleared: Int,
)
