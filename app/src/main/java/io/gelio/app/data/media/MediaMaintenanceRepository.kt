package io.gelio.app.data.media

import android.content.Context
import android.os.SystemClock
import io.gelio.app.core.util.generateImageThumbnailIfSupported
import io.gelio.app.data.local.dao.ShowcaseDao
import io.gelio.app.data.model.Brand
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.StateFlow

class MediaMaintenanceRepository(
    private val context: Context,
    private val dao: ShowcaseDao,
    private val applicationScope: CoroutineScope,
    private val lastInteractionMillis: StateFlow<Long>,
) {
    fun warmUp() {
        applicationScope.launch {
            delay(12_000L)
            while (SystemClock.elapsedRealtime() - lastInteractionMillis.value < USER_IDLE_BEFORE_REPAIR_MS) {
                delay(5_000L)
            }
            generateMissingImageThumbnails()
        }
    }

    suspend fun generateMissingImageThumbnails() = withContext(Dispatchers.IO) {
        val imagePaths = buildSet {
            dao.getAllFeaturedProjectsSnapshot().forEach { project ->
                add(project.thumbnailUri)
                addAll(project.galleryImages)
            }
            dao.getAllVirtualToursSnapshot().forEach { add(it.thumbnailUri) }
            dao.getAllVideosSnapshot().forEach { video ->
                video.thumbnailUri?.let { add(it) }
            }
            (dao.getAllBrochuresSnapshot(Brand.DESIGN.name) + dao.getAllBrochuresSnapshot(Brand.TOURISM.name)).forEach { brochure ->
                add(brochure.coverThumbnailUri)
            }
            dao.getAllDestinationsSnapshot().forEach { add(it.imageUri) }
            dao.getAllServicesSnapshot().forEach { add(it.imageUri) }
        }

        imagePaths.forEachIndexed { index, path ->
            val file = path.toLocalFileOrNull() ?: return@forEachIndexed
            generateImageThumbnailIfSupported(context, file)
            if (index % THUMBNAIL_REPAIR_BATCH_SIZE == 0) {
                delay(THUMBNAIL_REPAIR_BATCH_PAUSE_MS)
                yield()
            }
        }
    }

    private fun String.toLocalFileOrNull(): File? {
        if (isBlank() || startsWith("content://") || startsWith("http://") || startsWith("https://")) return null
        val file = when {
            startsWith("file://") -> File(removePrefix("file://"))
            else -> File(this)
        }
        return file.takeIf { it.exists() && it.isFile }
    }

    private companion object {
        const val THUMBNAIL_REPAIR_BATCH_SIZE = 8
        const val THUMBNAIL_REPAIR_BATCH_PAUSE_MS = 60L
        const val USER_IDLE_BEFORE_REPAIR_MS = 15_000L
    }
}
