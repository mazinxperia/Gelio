package io.gelio.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

suspend fun generatePdfCoverThumbnailToAppStorage(
    context: Context,
    pdfPath: String,
    folderName: String = "brochures/covers",
    maxRenderDimension: Int = 1024,
    jpegQuality: Int = 88,
): String? = withContext(Dispatchers.IO) {
    if (pdfPath.isBlank()) return@withContext null

    val sourceKey = resolvePdfCacheKey(pdfPath) ?: return@withContext null
    val destination = File(
        context.filesDir,
        "showcase_media/$folderName/generated-${sourceKey.sha256().take(24)}.jpg",
    )
    if (destination.exists()) {
        return@withContext destination.absolutePath
    }
    destination.parentFile?.mkdirs()

    val descriptor = openPdfDescriptor(context, pdfPath) ?: return@withContext null
    val renderer = runCatching { PdfRenderer(descriptor) }.getOrElse {
        descriptor.close()
        return@withContext null
    }

    try {
        if (renderer.pageCount <= 0) return@withContext null
        val page = renderer.openPage(0)
        try {
            val scale = minOf(
                maxRenderDimension.toFloat() / page.width.toFloat(),
                maxRenderDimension.toFloat() / page.height.toFloat(),
                2f,
            ).coerceAtLeast(0.1f)
            val bitmap = createBitmap(
                (page.width * scale).roundToInt().coerceAtLeast(1),
                (page.height * scale).roundToInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
            try {
                bitmap.eraseColor(AndroidColor.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                destination.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)
                }
            } finally {
                bitmap.recycle()
            }
        } finally {
            page.close()
        }
        destination.absolutePath
    } catch (_: Throwable) {
        runCatching { destination.delete() }
        null
    } finally {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}

fun isGeneratedPdfCoverThumbnail(path: String): Boolean =
    path.contains("/brochures/covers/generated-", ignoreCase = true) ||
        path.contains("\\brochures\\covers\\generated-", ignoreCase = true)

private fun openPdfDescriptor(
    context: Context,
    value: String,
): ParcelFileDescriptor? {
    return when {
        value.startsWith("content://", ignoreCase = true) ->
            context.contentResolver.openFileDescriptor(value.toUri(), "r")

        value.startsWith("file://", ignoreCase = true) -> {
            val path = value.toUri().path ?: return null
            val file = File(path)
            if (!file.exists()) return null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        else -> {
            val file = File(value)
            if (!file.exists()) return null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
}

private fun resolvePdfCacheKey(pdfPath: String): String? {
    return when {
        pdfPath.startsWith("content://", ignoreCase = true) -> pdfPath
        pdfPath.startsWith("file://", ignoreCase = true) -> {
            val path = pdfPath.toUri().path ?: return null
            val file = File(path)
            if (!file.exists()) return null
            "file|${file.absolutePath}|${file.length()}|${file.lastModified()}"
        }
        else -> {
            val file = File(pdfPath)
            if (!file.exists()) return null
            "file|${file.absolutePath}|${file.length()}|${file.lastModified()}"
        }
    }
}

private fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
