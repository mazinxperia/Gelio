package io.gelio.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAP_PREVIEW_FOLDER = "maps/previews"

fun mapPreviewFile(
    context: Context,
    sectionId: String,
): File = File(context.filesDir, "showcase_media/$MAP_PREVIEW_FOLDER/$sectionId.png")

fun mapPreviewPath(
    context: Context,
    sectionId: String,
): String? = mapPreviewFile(context, sectionId).takeIf { it.exists() }?.absolutePath

suspend fun writeMapPreviewBitmap(
    context: Context,
    sectionId: String,
    bitmap: Bitmap,
): String? = withContext(Dispatchers.IO) {
    val destination = mapPreviewFile(context, sectionId)
    destination.parentFile?.mkdirs()
    return@withContext runCatching {
        destination.outputStream().use { output ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.flush()
            check(compressed) { "Bitmap compression failed for map preview $sectionId" }
        }
        destination.absolutePath
    }.getOrElse {
        Log.e("MapPreviewStorage", "Failed to write map preview for $sectionId", it)
        null
    }
}

suspend fun deleteMapPreview(
    context: Context,
    sectionId: String,
) = withContext(Dispatchers.IO) {
    runCatching { mapPreviewFile(context, sectionId).delete() }
}
