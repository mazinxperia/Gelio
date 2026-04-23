package io.gelio.app.core.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.gelio.app.core.util.localThumbnailForPath
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun warmGalleryPreviewAspectRatio(
    context: android.content.Context,
    model: Any?,
    preferLocalThumbnail: Boolean = true,
) {
    val localPath = resolvePreviewLocalPath(context, model, preferLocalThumbnail) ?: return
    if (previewAspectRatioCache.containsKey(localPath)) return
    val computed = withContext(Dispatchers.IO) {
        resolveImageAspectRatio(localPath)
    }
    if (computed > 0f) {
        previewAspectRatioCache[localPath] = computed
    }
}

@Composable
fun GalleryPreviewImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    preferLocalThumbnail: Boolean = true,
    decodeMaxWidth: Dp = 1280.dp,
    decodeMaxHeight: Dp = 880.dp,
) {
    val context = LocalContext.current
    val resolvedLocalPath = remember(model, context, preferLocalThumbnail) {
        resolvePreviewLocalPath(context, model, preferLocalThumbnail)
    }
    val aspectRatio by produceState(
        initialValue = resolvedLocalPath?.let { previewAspectRatioCache[it] } ?: 0f,
        key1 = resolvedLocalPath,
    ) {
        val localPath = resolvedLocalPath ?: return@produceState
        previewAspectRatioCache[localPath]?.let {
            value = it
            return@produceState
        }
        val computed = withContext(Dispatchers.IO) {
            resolveImageAspectRatio(localPath)
        }
        if (computed > 0f) {
            previewAspectRatioCache[localPath] = computed
            value = computed
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val targetWidth: Dp
        val targetHeight: Dp
        if (aspectRatio > 0f && maxWidth > 0.dp && maxHeight > 0.dp) {
            val containerAspect = maxWidth.value / maxHeight.value
            if (containerAspect > aspectRatio) {
                targetHeight = maxHeight
                targetWidth = maxHeight * aspectRatio
            } else {
                targetWidth = maxWidth
                targetHeight = maxWidth / aspectRatio
            }
        } else {
            targetWidth = maxWidth
            targetHeight = maxHeight
        }

        Box(
            modifier = Modifier
                .size(targetWidth, targetHeight)
                .clip(MaterialTheme.shapes.extraLarge),
        ) {
            OptimizedAsyncImage(
                model = model,
                contentDescription = contentDescription,
                maxWidth = decodeMaxWidth,
                maxHeight = decodeMaxHeight,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                preferLocalThumbnail = preferLocalThumbnail,
            )
        }
    }
}

private fun resolvePreviewLocalPath(
    context: android.content.Context,
    model: Any?,
    preferLocalThumbnail: Boolean,
): String? {
    val path = when (model) {
        is String -> {
            if (preferLocalThumbnail) {
                localThumbnailForPath(context, model) ?: model
            } else {
                model
            }
        }

        else -> return null
    }

    val localPath = when {
        path.startsWith("content://") || path.startsWith("http://") || path.startsWith("https://") -> return null
        path.startsWith("file://") -> path.toUri().path.orEmpty()
        else -> path
    }
    return localPath.takeIf { it.isNotBlank() }
}

private fun resolveImageAspectRatio(localPath: String): Float {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(localPath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return 0f
    return bounds.outWidth.toFloat() / bounds.outHeight.toFloat()
}

private val previewAspectRatioCache = ConcurrentHashMap<String, Float>()
