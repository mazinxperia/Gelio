package io.gelio.app.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

private enum class GallerySyncSource {
    Idle,
    CarouselDrag,
    CarouselTap,
    PagerSwipe,
}

data class LinkedGalleryScrubberState(
    val selectedIndex: Int,
    val livePreviewOffsetFraction: Float,
    val isCarouselDragging: Boolean,
    val onCarouselViewportCenterChanged: (Float) -> Unit,
    val onThumbnailCenterChanged: (Int, Float) -> Unit,
    val onThumbnailTap: (Int) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun rememberLinkedGalleryScrubberState(
    pagerState: PagerState,
    carouselState: CarouselState,
    itemCount: Int,
    initialIndex: Int = 0,
): LinkedGalleryScrubberState {
    val scope = rememberCoroutineScope()
    val carouselPagerState = remember(carouselState) { carouselState.rawPagerState() }
    var selectedIndex by remember(pagerState, carouselState, itemCount, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0)))
    }
    var syncSource by remember(pagerState, carouselState, itemCount) {
        mutableStateOf(GallerySyncSource.Idle)
    }
    var livePreviewOffsetFraction by remember(pagerState, carouselState, itemCount) { mutableFloatStateOf(0f) }
    var isCarouselDragging by remember(pagerState, carouselState, itemCount) { mutableStateOf(false) }
    var previewSyncJob by remember(pagerState, carouselState, itemCount) {
        mutableStateOf<Job?>(null)
    }

    fun syncPreview(
        targetIndex: Int,
        targetOffsetFraction: Float = 0f,
        animated: Boolean,
    ) {
        if (itemCount <= 0) return
        val clampedIndex = targetIndex.coerceIn(0, itemCount - 1)
        val clampedOffset = targetOffsetFraction.coerceIn(-0.5f, 0.5f)
        previewSyncJob?.cancel()
        previewSyncJob = scope.launch {
            if (animated) {
                pagerState.animateScrollToPage(clampedIndex, clampedOffset)
            } else {
                pagerState.scrollToPage(clampedIndex, clampedOffset)
            }
        }
    }

    LaunchedEffect(itemCount, initialIndex, pagerState, carouselState) {
        if (itemCount <= 0) return@LaunchedEffect
        val clampedIndex = initialIndex.coerceIn(0, itemCount - 1)
        selectedIndex = clampedIndex
        if (pagerState.currentPage != clampedIndex) {
            pagerState.scrollToPage(clampedIndex)
        }
        if (carouselState.currentItem != clampedIndex) {
            carouselState.scrollToItem(clampedIndex)
        }
        syncSource = GallerySyncSource.Idle
        isCarouselDragging = false
        livePreviewOffsetFraction = 0f
    }

    LaunchedEffect(carouselState, carouselPagerState, pagerState, itemCount) {
        if (itemCount <= 0) return@LaunchedEffect
        snapshotFlow {
            Triple(
                carouselPagerState.currentPage,
                carouselPagerState.currentPageOffsetFraction,
                carouselState.isScrollInProgress,
            )
        }
            .distinctUntilChanged()
            .collect { (currentItem, currentOffsetFraction, isScrolling) ->
                val targetIndex = currentItem.coerceIn(0, itemCount - 1)
                val targetOffsetFraction = currentOffsetFraction.coerceIn(-0.5f, 0.5f)
                when (syncSource) {
                    GallerySyncSource.CarouselTap -> {
                        if (!isScrolling) {
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                            if (pagerState.currentPage != targetIndex) {
                                syncPreview(targetIndex, animated = false)
                            }
                            syncSource = GallerySyncSource.Idle
                        }
                    }

                    GallerySyncSource.PagerSwipe -> {
                        if (!isScrolling && carouselState.currentItem == pagerState.currentPage) {
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                            syncSource = GallerySyncSource.Idle
                        }
                    }

                    GallerySyncSource.Idle,
                    GallerySyncSource.CarouselDrag,
                    -> {
                        if (isScrolling) {
                            syncSource = GallerySyncSource.CarouselDrag
                            isCarouselDragging = true
                            selectedIndex = targetIndex
                            livePreviewOffsetFraction = targetOffsetFraction
                        } else if (syncSource == GallerySyncSource.CarouselDrag) {
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                            if (
                                pagerState.currentPage != targetIndex ||
                                pagerState.currentPageOffsetFraction != 0f
                            ) {
                                syncPreview(
                                    targetIndex = targetIndex,
                                    targetOffsetFraction = 0f,
                                    animated = false,
                                )
                            }
                            syncSource = GallerySyncSource.Idle
                        }
                    }
                }
            }
    }

    LaunchedEffect(pagerState, carouselState, itemCount) {
        if (itemCount <= 0) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (currentPage, isScrolling) ->
                val targetIndex = currentPage.coerceIn(0, itemCount - 1)
                when (syncSource) {
                    GallerySyncSource.CarouselDrag -> {
                        if (!isScrolling) {
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                        }
                    }

                    GallerySyncSource.CarouselTap -> {
                        if (!isScrolling) {
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                        }
                    }

                    GallerySyncSource.PagerSwipe -> {
                        if (isScrolling) {
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                        } else {
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                            if (carouselState.currentItem != targetIndex) {
                                carouselState.animateScrollToItem(targetIndex)
                            }
                            syncSource = GallerySyncSource.Idle
                        }
                    }

                    GallerySyncSource.Idle -> {
                        if (isScrolling) {
                            syncSource = GallerySyncSource.PagerSwipe
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                        } else {
                            selectedIndex = targetIndex
                            isCarouselDragging = false
                            livePreviewOffsetFraction = 0f
                        }
                    }
                }
            }
    }

    val onThumbnailTap: (Int) -> Unit = remember(scope, pagerState, carouselState, itemCount) {
        { tappedIndex ->
            if (itemCount > 0) {
                val targetIndex = tappedIndex.coerceIn(0, itemCount - 1)
                selectedIndex = targetIndex
                isCarouselDragging = false
                livePreviewOffsetFraction = 0f
                syncSource = GallerySyncSource.CarouselTap
                scope.launch {
                    coroutineScope {
                        launch {
                            if (carouselState.currentItem != targetIndex) {
                                carouselState.animateScrollToItem(targetIndex)
                            }
                        }
                        launch {
                            if (pagerState.currentPage != targetIndex) {
                                pagerState.animateScrollToPage(targetIndex)
                            }
                        }
                    }
                    if (carouselState.currentItem != targetIndex) {
                        carouselState.scrollToItem(targetIndex)
                    }
                    if (pagerState.currentPage != targetIndex) {
                        pagerState.scrollToPage(targetIndex)
                    }
                    syncSource = GallerySyncSource.Idle
                }
            }
        }
    }

    return LinkedGalleryScrubberState(
        selectedIndex = selectedIndex,
        livePreviewOffsetFraction = livePreviewOffsetFraction,
        isCarouselDragging = isCarouselDragging,
        onCarouselViewportCenterChanged = {},
        onThumbnailCenterChanged = { _, _ -> },
        onThumbnailTap = onThumbnailTap,
    )
}

private fun CarouselState.rawPagerState(): PagerState {
    val getter = carouselPagerGetterCache.getOrPut(javaClass) {
        javaClass.getMethod("getPagerState\$material3")
    }
    return getter.invoke(this) as PagerState
}

@Composable
fun GalleryPreviewViewport(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val edgeColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.98f)
    BoxWithConstraints(modifier = modifier) {
        val viewportWidth = (maxWidth * 0.84f).coerceIn(260.dp, 980.dp)
        val maskWidth = (viewportWidth * 0.11f).coerceIn(32.dp, 104.dp)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(viewportWidth)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraLarge),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val maskWidthPx = maskWidth.toPx().coerceAtLeast(1f)
                            val leftBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    edgeColor,
                                    edgeColor.copy(alpha = 0.92f),
                                    edgeColor.copy(alpha = 0.55f),
                                    Color.Transparent,
                                ),
                                startX = 0f,
                                endX = maskWidthPx,
                            )
                            val rightBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    edgeColor.copy(alpha = 0.55f),
                                    edgeColor.copy(alpha = 0.92f),
                                    edgeColor,
                                ),
                                startX = size.width - maskWidthPx,
                                endX = size.width,
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(
                                    brush = leftBrush,
                                    topLeft = Offset.Zero,
                                    size = Size(maskWidthPx, size.height),
                                )
                                drawRect(
                                    brush = rightBrush,
                                    topLeft = Offset(size.width - maskWidthPx, 0f),
                                    size = Size(maskWidthPx, size.height),
                                )
                            }
                        },
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun LiveLinkedGalleryPreview(
    images: List<String>,
    selectedIndex: Int,
    offsetFraction: Float,
    contentDescription: String,
    maxImageWidth: androidx.compose.ui.unit.Dp,
    maxImageHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val viewportWidth = maxWidth
        val travelPx = with(density) { (viewportWidth * 0.92f).toPx() }
        val previewShiftFraction = (-offsetFraction).coerceIn(-0.5f, 0.5f)
        val currentTranslation = previewShiftFraction * travelPx
        val adjacentIndex = when {
            previewShiftFraction > 0f && selectedIndex > 0 -> selectedIndex - 1
            previewShiftFraction < 0f && selectedIndex < images.lastIndex -> selectedIndex + 1
            else -> null
        }
        val adjacentTranslation = when {
            adjacentIndex == null -> null
            previewShiftFraction > 0f -> currentTranslation - travelPx
            else -> currentTranslation + travelPx
        }
        val adjacentAlpha = abs(previewShiftFraction).coerceIn(0f, 1f)

        Box(modifier = Modifier.fillMaxSize()) {
            adjacentIndex?.let { index ->
                PreviewFrame(
                    imageUri = images[index],
                    contentDescription = contentDescription,
                    translationX = adjacentTranslation ?: 0f,
                    alpha = adjacentAlpha,
                    maxImageWidth = maxImageWidth,
                    maxImageHeight = maxImageHeight,
                )
            }
            PreviewFrame(
                imageUri = images[selectedIndex.coerceIn(0, images.lastIndex)],
                contentDescription = contentDescription,
                translationX = currentTranslation,
                alpha = 1f,
                maxImageWidth = maxImageWidth,
                maxImageHeight = maxImageHeight,
            )
        }
    }
}

@Composable
private fun PreviewFrame(
    imageUri: String,
    contentDescription: String,
    translationX: Float,
    alpha: Float,
    maxImageWidth: androidx.compose.ui.unit.Dp,
    maxImageHeight: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.translationX = translationX
                    this.alpha = alpha
                },
        ) {
            GalleryPreviewImage(
                model = imageUri,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                preferLocalThumbnail = true,
                decodeMaxWidth = maxImageWidth.coerceAtMost(1280.dp),
                decodeMaxHeight = maxImageHeight.coerceAtMost(880.dp),
            )
        }
    }
}

@Composable
fun GearLinkedThumbnailCard(
    imageUri: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (selected) 12.dp else 3.dp,
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        OptimizedAsyncImage(
            model = imageUri,
            contentDescription = null,
            maxWidth = 260.dp,
            maxHeight = 180.dp,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

private val carouselPagerGetterCache = ConcurrentHashMap<Class<*>, Method>()
