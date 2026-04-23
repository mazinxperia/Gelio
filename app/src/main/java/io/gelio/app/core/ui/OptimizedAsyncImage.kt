package io.gelio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.gelio.app.core.performance.LocalDevicePerformanceProfile
import io.gelio.app.core.util.localThumbnailForPath

@Composable
fun OptimizedAsyncImage(
    model: Any?,
    contentDescription: String?,
    maxWidth: Dp,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    preferLocalThumbnail: Boolean = maxWidth <= 800.dp && maxHeight <= 800.dp,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val performanceProfile = LocalDevicePerformanceProfile.current
    val decodeScale = performanceProfile.imageDecodeScale.coerceIn(0.5f, 1f)
    val widthPx = bucketDecodeSizePx(with(density) { (maxWidth.toPx() * decodeScale).toInt().coerceAtLeast(1) })
    val heightPx = bucketDecodeSizePx(with(density) { (maxHeight.toPx() * decodeScale).toInt().coerceAtLeast(1) })
    val resolvedModel = remember(model, context, preferLocalThumbnail) {
        if (preferLocalThumbnail && model is String) {
            localThumbnailForPath(context, model) ?: model
        } else {
            model
        }
    }
    val request = remember(resolvedModel, context, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(resolvedModel)
            .size(widthPx, heightPx)
            .allowHardware(false)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}

private fun bucketDecodeSizePx(sizePx: Int): Int {
    val requested = sizePx.coerceAtLeast(1)
    val bucket = DECODE_SIZE_BUCKETS.firstOrNull { requested <= it }
    return bucket ?: (((requested + 255) / 256) * 256)
}

private val DECODE_SIZE_BUCKETS = intArrayOf(
    96,
    128,
    160,
    192,
    240,
    288,
    320,
    384,
    448,
    512,
    640,
    768,
    960,
    1152,
    1280,
    1536,
    1792,
    2048,
    2560,
)

@Composable
fun OptimizedThumbnailImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    OptimizedAsyncImage(
        model = model,
        contentDescription = contentDescription,
        maxWidth = LocalDevicePerformanceProfile.current.thumbnailDecodePx.dp,
        maxHeight = LocalDevicePerformanceProfile.current.thumbnailDecodePx.dp,
        modifier = modifier,
        contentScale = contentScale,
    )
}
