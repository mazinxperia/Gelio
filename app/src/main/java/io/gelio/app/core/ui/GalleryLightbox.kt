package io.gelio.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.core.performance.DevicePerformanceClass
import io.gelio.app.core.performance.LocalDevicePerformanceProfile
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryLightbox(
    images: List<String>,
    initialIndex: Int,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) return

    BackHandler(onBack = onDismiss)

    val startIndex = initialIndex.coerceIn(0, images.lastIndex)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { images.size })
    val dismissInteraction = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp().value.roundToInt() }
    val screenHeightDp = with(density) { containerSize.height.toDp().value.roundToInt() }
    val performanceProfile = LocalDevicePerformanceProfile.current
    val (decodeWidth, decodeHeight) = remember(
        screenWidthDp,
        screenHeightDp,
        performanceProfile.performanceClass,
    ) {
        lightboxDecodeSize(
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            performanceClass = performanceProfile.performanceClass,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.90f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = dismissInteraction,
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close full image",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            ) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp),
                    pageSpacing = 24.dp,
                    beyondViewportPageCount = 0,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        OptimizedAsyncImage(
                            model = images[page],
                            contentDescription = title,
                            maxWidth = decodeWidth,
                            maxHeight = decodeHeight,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1700.dp),
                            contentScale = ContentScale.Fit,
                            preferLocalThumbnail = false,
                        )
                    }
                }
            }

            Text(
                text = "Swipe for full-resolution photos. Tap outside to close.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun lightboxDecodeSize(
    screenWidthDp: Int,
    screenHeightDp: Int,
    performanceClass: DevicePerformanceClass,
): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> {
    val longestEdge = maxOf(screenWidthDp, screenHeightDp)
    val shortestEdge = minOf(screenWidthDp, screenHeightDp)
    val widthDp = when (performanceClass) {
        DevicePerformanceClass.Compact -> longestEdge
        DevicePerformanceClass.Balanced -> (longestEdge * 1.1f).toInt()
        DevicePerformanceClass.High -> (longestEdge * 1.2f).toInt()
    }.coerceIn(960, 2200)
    val heightDp = when (performanceClass) {
        DevicePerformanceClass.Compact -> shortestEdge
        DevicePerformanceClass.Balanced -> (shortestEdge * 1.08f).toInt()
        DevicePerformanceClass.High -> (shortestEdge * 1.16f).toInt()
    }.coerceIn(720, 1600)
    return widthDp.dp to heightDp.dp
}
