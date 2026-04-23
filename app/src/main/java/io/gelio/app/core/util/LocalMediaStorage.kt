package io.gelio.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun copyContentUriToAppStorage(
    context: Context,
    uri: Uri,
    folderName: String,
    fallbackExtension: String,
): String = withContext(Dispatchers.IO) {
    val extension = uri.displayName(context)
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.takeIf { it.isNotBlank() && it.length <= 8 }
        ?.let { ".$it" }
        ?: fallbackExtension
    val folder = File(context.filesDir, "showcase_media/$folderName").apply {
        mkdirs()
    }
    val destination = File(folder, "${UUID.randomUUID()}$extension")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open selected file." }
        destination.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    generateImageThumbnailIfSupported(context, destination)
    destination.absolutePath
}

suspend fun importNormalizedImageToAppStorage(
    context: Context,
    uri: Uri,
    folderName: String,
    maxLongSide: Int = 1440,
    quality: Int = 84,
): String = withContext(Dispatchers.IO) {
    val folder = File(context.filesDir, "showcase_media/$folderName").apply { mkdirs() }
    val destination = File(folder, "${UUID.randomUUID()}.jpg")

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open selected image." }
        BitmapFactory.decodeStream(input, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return@withContext copyContentUriToAppStorage(
            context = context,
            uri = uri,
            folderName = folderName,
            fallbackExtension = ".jpg",
        )
    }

    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxLongSide)
    val decoded = context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open selected image." }
        BitmapFactory.decodeStream(
            input,
            null,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            },
        )
    } ?: return@withContext copyContentUriToAppStorage(
        context = context,
        uri = uri,
        folderName = folderName,
        fallbackExtension = ".jpg",
    )

    val orientedBitmap = decoded.applyExifOrientation(uri, context)
    val scale = minOf(1f, maxLongSide.toFloat() / maxOf(orientedBitmap.width, orientedBitmap.height).toFloat())
    val outputBitmap = if (scale < 1f) {
        orientedBitmap.scale(
            (orientedBitmap.width * scale).toInt().coerceAtLeast(1),
            (orientedBitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        ).also {
            if (it != orientedBitmap) orientedBitmap.recycle()
        }
    } else {
        orientedBitmap
    }

    destination.outputStream().use { output ->
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(60, 95), output)
    }
    outputBitmap.recycle()
    generateImageThumbnailIfSupported(context, destination)
    destination.absolutePath
}

suspend fun importNormalizedImageFileToAppStorage(
    context: Context,
    source: File,
    folderName: String,
    maxLongSide: Int = 1440,
    quality: Int = 84,
): String = withContext(Dispatchers.IO) {
    require(source.exists()) { "Downloaded image is missing." }
    val folder = File(context.filesDir, "showcase_media/$folderName").apply { mkdirs() }
    val destination = File(folder, "${UUID.randomUUID()}.jpg")

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        val extension = source.extension.takeIf { it.isNotBlank() && it.length <= 8 }?.let { ".$it" } ?: ".jpg"
        val fallback = File(folder, "${UUID.randomUUID()}$extension")
        source.inputStream().use { input ->
            fallback.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        generateImageThumbnailIfSupported(context, fallback)
        return@withContext fallback.absolutePath
    }

    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxLongSide)
    val decoded = BitmapFactory.decodeFile(
        source.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        },
    )
    if (decoded == null) {
        val extension = source.extension.takeIf { it.isNotBlank() && it.length <= 8 }?.let { ".$it" } ?: ".jpg"
        val fallback = File(folder, "${UUID.randomUUID()}$extension")
        source.inputStream().use { input ->
            fallback.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        generateImageThumbnailIfSupported(context, fallback)
        return@withContext fallback.absolutePath
    }

    val orientedBitmap = decoded.applyExifOrientation(source)
    val scale = minOf(1f, maxLongSide.toFloat() / maxOf(orientedBitmap.width, orientedBitmap.height).toFloat())
    val outputBitmap = if (scale < 1f) {
        orientedBitmap.scale(
            (orientedBitmap.width * scale).toInt().coerceAtLeast(1),
            (orientedBitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        ).also {
            if (it != orientedBitmap) orientedBitmap.recycle()
        }
    } else {
        orientedBitmap
    }

    destination.outputStream().use { output ->
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(60, 95), output)
    }
    outputBitmap.recycle()
    generateImageThumbnailIfSupported(context, destination)
    destination.absolutePath
}

fun localThumbnailForPath(
    context: Context,
    sourcePath: String,
): String? {
    val source = sourcePath.toLocalImageFileOrNull() ?: return null
    val thumbnail = thumbnailFileFor(context, source)
    return thumbnail.takeIf { it.exists() }?.absolutePath
}

fun generateImageThumbnailIfSupported(
    context: Context,
    sourcePath: String,
): String? = generateImageThumbnailIfSupported(context, File(sourcePath))

fun generateImageThumbnailIfSupported(
    context: Context,
    source: File,
    maxSide: Int = 512,
): String? {
    if (!source.isSupportedImageFile() || !source.exists()) return null
    val destination = thumbnailFileFor(context, source)
    if (destination.exists() && destination.lastModified() >= source.lastModified()) {
        return destination.absolutePath
    }
    destination.parentFile?.mkdirs()

    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val isPng = source.extension.equals("png", ignoreCase = true)
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = if (isPng) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, options) ?: return null
        val orientedBitmap = decoded.applyExifOrientation(source)
        val scale = minOf(1f, maxSide.toFloat() / maxOf(orientedBitmap.width, orientedBitmap.height).toFloat())
        val outputBitmap = if (scale < 1f) {
            orientedBitmap.scale(
                (orientedBitmap.width * scale).toInt().coerceAtLeast(1),
                (orientedBitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also {
                if (it != orientedBitmap) orientedBitmap.recycle()
            }
        } else {
            orientedBitmap
        }
        destination.outputStream().use { output ->
            if (isPng) {
                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } else {
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, 84, output)
            }
        }
        outputBitmap.recycle()
        destination.absolutePath
    }.getOrNull()
}

private fun Uri.displayName(context: Context): String? {
    if (scheme == "file") return path?.substringAfterLast('/')
    return context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
}

private fun thumbnailFileFor(context: Context, source: File): File {
    val isPng = source.extension.equals("png", ignoreCase = true)
    val ext = if (isPng) "png" else "jpg"
    val key = "thumb-v2|${source.absolutePath}|${source.length()}|${source.lastModified()}".sha256().take(24)
    return File(context.filesDir, "showcase_media/.thumbs/images/$key.$ext")
}

private fun String.toLocalImageFileOrNull(): File? {
    if (startsWith("content://") || startsWith("http://") || startsWith("https://")) return null
    val file = when {
        startsWith("file://") -> File(toUri().path.orEmpty())
        else -> File(this)
    }
    return file.takeIf { it.isSupportedImageFile() && it.exists() }
}

private fun File.isSupportedImageFile(): Boolean {
    val extension = extension.lowercase()
    return extension in setOf("jpg", "jpeg", "png", "webp")
}

private fun calculateInSampleSize(width: Int, height: Int, maxSide: Int): Int {
    var sampleSize = 1
    val largestSide = maxOf(width, height)
    while (largestSide / sampleSize >= maxSide * 2) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}

private fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

private fun Bitmap.applyExifOrientation(source: File): Bitmap {
    val exifOrientation = runCatching {
        ExifInterface(source.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    if (exifOrientation == ExifInterface.ORIENTATION_NORMAL || exifOrientation == ExifInterface.ORIENTATION_UNDEFINED) {
        return this
    }

    val matrix = Matrix().apply {
        when (exifOrientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                postRotate(180f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
        }
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also { transformed ->
        if (transformed != this) recycle()
    }
}

private fun Bitmap.applyExifOrientation(
    uri: Uri,
    context: Context,
): Bitmap {
    val exifOrientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    if (exifOrientation == ExifInterface.ORIENTATION_NORMAL || exifOrientation == ExifInterface.ORIENTATION_UNDEFINED) {
        return this
    }

    val matrix = Matrix().apply {
        when (exifOrientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                postRotate(180f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
        }
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also { transformed ->
        if (transformed != this) recycle()
    }
}
