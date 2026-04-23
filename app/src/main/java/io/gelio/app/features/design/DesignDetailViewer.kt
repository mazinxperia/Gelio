@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.design
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.gelio.app.app.ShowcaseViewModel
import io.gelio.app.core.navigation.DesignSection
import io.gelio.app.core.theme.LocalNavAnimatedVisibilityScope
import io.gelio.app.core.theme.LocalSharedTransitionScope
import io.gelio.app.core.theme.directionalTabTransitionSpec
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.GalleryLightbox
import io.gelio.app.core.ui.GalleryPreviewImage
import io.gelio.app.core.ui.GalleryPreviewViewport
import io.gelio.app.core.ui.GearLinkedThumbnailCard
import io.gelio.app.core.ui.LiveLinkedGalleryPreview
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ShowcaseHomeButton
import io.gelio.app.core.ui.ShowcasePrimaryActionButton
import io.gelio.app.core.ui.ShowcaseSectionToggleButton
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.ui.rememberLinkedGalleryScrubberState
import io.gelio.app.core.util.cleanVirtualTourThumbnailUrl
import io.gelio.app.core.util.resolveVirtualTourThumbnailUrl
import io.gelio.app.core.util.youtubeThumbnail
import io.gelio.app.data.model.Brochure
import io.gelio.app.data.model.Destination
import io.gelio.app.data.model.FeaturedProject
import io.gelio.app.data.model.SectionType
import io.gelio.app.data.model.Service
import io.gelio.app.data.model.ShowcaseCompany
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.data.model.ShowcaseVideo
import io.gelio.app.data.model.VirtualTour
import io.gelio.app.features.brochures.BrochureCarousel
import io.gelio.app.features.map.MapAssetPaths
import io.gelio.app.features.map.WorldMapSectionViewer
import io.gelio.app.features.map.rememberWorldMapAsset
import io.gelio.app.features.reviews.RatingsSectionViewer
import io.gelio.app.features.tourism.TourismFullWidthStrip
import io.gelio.app.features.tourism.TourismStripItem
import io.gelio.app.media.webview.ShowcaseWebFrame

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun DesignProjectGalleryDetail(
    project: FeaturedProject,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val images = project.galleryImages.ifEmpty { listOf(project.thumbnailUri).filter { it.isNotBlank() } }
    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No gallery images available.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        return
    }

    val initialPage = remember(project.id) {
        images.indexOf(project.thumbnailUri).takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { images.size })
    val thumbnailCarouselState = rememberCarouselState(initialItem = initialPage) { images.count() }
    var lightboxIndex by rememberSaveable(project.id) { mutableStateOf<Int?>(null) }
    val linkedScrubberState = rememberLinkedGalleryScrubberState(
        pagerState = pagerState,
        carouselState = thumbnailCarouselState,
        itemCount = images.size,
        initialIndex = initialPage,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ViewerTopBar(
            title = project.projectName,
            subtitle = "${images.size} images",
            onBack = onBack,
            onHome = onHome,
            onClose = onClose,
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            GalleryPreviewViewport(modifier = Modifier.fillMaxSize()) {
                if (linkedScrubberState.isCarouselDragging) {
                    LiveLinkedGalleryPreview(
                        images = images,
                        selectedIndex = linkedScrubberState.selectedIndex,
                        offsetFraction = linkedScrubberState.livePreviewOffsetFraction,
                        contentDescription = project.projectName,
                        maxImageWidth = 1280.dp,
                        maxImageHeight = 880.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        pageSpacing = 16.dp,
                        beyondViewportPageCount = 0,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            GalleryPreviewImage(
                                model = images[page],
                                contentDescription = project.projectName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        lightboxIndex = page
                                    },
                                preferLocalThumbnail = true,
                                decodeMaxWidth = 1280.dp,
                                decodeMaxHeight = 880.dp,
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = project.projectName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }

        HorizontalCenteredHeroCarousel(
            state = thumbnailCarouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp),
            maxItemWidth = 272.dp,
            itemSpacing = 10.dp,
            minSmallItemWidth = 36.dp,
            maxSmallItemWidth = 72.dp,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        ) { index ->
            val imageUri = images[index]
            GearLinkedThumbnailCard(
                imageUri = imageUri,
                selected = index == linkedScrubberState.selectedIndex,
                onClick = { linkedScrubberState.onThumbnailTap(index) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp)
                    .maskClip(MaterialTheme.shapes.large),
            )
            }
        }

    lightboxIndex?.let { selectedIndex ->
        GalleryLightbox(
            images = images,
            initialIndex = selectedIndex,
            title = project.projectName,
            onDismiss = { lightboxIndex = null },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun DesignVirtualTourDetail(
    tour: VirtualTour,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val thumbnail = remember(tour.thumbnailUri) { cleanVirtualTourThumbnailUrl(tour.thumbnailUri) }
    val descriptionText = remember(tour.description) {
        tour.description.ifBlank { "Interactive 360 tour preview inside the showroom app." }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        val compactLandscape = maxWidth < 840.dp || maxHeight < 560.dp
        val viewerMaxWidth = (maxWidth - 112.dp).coerceAtLeast(360.dp)
        val viewerMaxHeight = (maxHeight - if (compactLandscape) 168.dp else 232.dp)
            .coerceAtLeast(if (compactLandscape) 150.dp else 260.dp)
        val viewerWidth = minOf(
            maxWidth * if (compactLandscape) 0.76f else 0.68f,
            viewerMaxWidth,
            viewerMaxHeight * (16f / 9f),
        )
        val viewerHeight = viewerWidth * (9f / 16f)
        val descriptionWidth = viewerWidth.coerceAtLeast(if (compactLandscape) 360.dp else 440.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ViewerTopBar(
                title = tour.projectName,
                subtitle = "Virtual tour",
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )

            ElevatedCard(
                modifier = Modifier
                    .width(if (compactLandscape) 148.dp else 160.dp)
                    .height(if (compactLandscape) 66.dp else 88.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                PosterImage(
                    imageUri = thumbnail,
                    title = tour.projectName,
                )
            }

            Surface(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 460.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = tour.projectName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(width = viewerWidth, height = viewerHeight),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                ) {
                    ShowcaseWebFrame(
                        url = tour.embedUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compactLandscape) 10.dp else 14.dp))

            Surface(
                modifier = Modifier.width(descriptionWidth),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (compactLandscape) 18.dp else 22.dp,
                            vertical = if (compactLandscape) 14.dp else 18.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = if (compactLandscape) 3 else 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
