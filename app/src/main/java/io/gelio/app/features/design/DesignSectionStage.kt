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
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun DesignSectionStage(
    selectedSection: DesignSection,
    stripPages: List<List<DesignStripItem>>,
    videos: List<ShowcaseVideo>,
    brochures: List<Brochure>,
    pageIndex: Int,
    onPageIndexChange: (Int) -> Unit,
    onSectionSelected: (DesignSection) -> Unit,
    onWelcomeClick: () -> Unit,
    allProjectsLink: String,
    onAllProjectsClick: (String) -> Unit,
    onFeatureProjectOpen: (FeaturedProject) -> Unit,
    onVirtualTourOpen: (VirtualTour) -> Unit,
    onVideoClick: (String) -> Unit,
    onBrochureClick: (String) -> Unit,
    showHomeButton: Boolean,
    tabTopPadding: Dp,
    floatingContentTopPadding: Dp,
    compactLandscape: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    val pageSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val pageEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val horizontalPadding = adaptive.contentPaddingHorizontal
    val verticalPadding = adaptive.contentPaddingVertical
    val itemSpacing = adaptive.gutter
    val posterWidth = if (compactLandscape) 178.dp else 248.dp
    val posterHeight = if (compactLandscape) adaptive.cardHeightSmall else adaptive.cardHeightMedium
    val posterTextVerticalPadding = adaptive.heroSpacing
    val contentBottomPadding = adaptive.sectionSpacing + adaptive.heroSpacing
    val sectionBodyState = remember(selectedSection, stripPages, pageIndex, videos, brochures) {
        DesignSectionBodyState(
            section = selectedSection,
            stripPages = stripPages,
            pageIndex = pageIndex,
            videos = videos,
            brochures = brochures,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = tabTopPadding),
            horizontalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier.widthIn(max = tokens.contentMaxWidth),
                    horizontalArrangement = Arrangement.spacedBy(
                        adaptive.tabSpacing,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                DesignSection.entries.forEach { section ->
                    ShowcaseSectionToggleButton(
                        label = section.label,
                        selected = section == selectedSection,
                        onClick = { onSectionSelected(section) },
                    )
                }
            }
        }

        if (showHomeButton) {
            ShowcaseHomeButton(
                onClick = onWelcomeClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = adaptive.homeButtonInsetTop, start = adaptive.homeButtonInsetStart),
            )
        }

        AnimatedContent(
            targetState = sectionBodyState,
            transitionSpec = directionalTabTransitionSpec(pageSpatial, pageEffects) { state ->
                state.section.ordinal * 100 + state.pageIndex
            },
            label = "design_section_body",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
        ) { bodyState ->
            if (bodyState.section == DesignSection.VIDEOS) {
                DesignVideosEditorialList(
                    videos = bodyState.videos,
                    onVideoClick = onVideoClick,
                    compactLandscape = compactLandscape,
                    modifier = Modifier
                        .padding(top = floatingContentTopPadding, bottom = contentBottomPadding),
                )
            } else if (bodyState.section == DesignSection.BROCHURES) {
                BrochureCarousel(
                    brochures = bodyState.brochures,
                    onBrochureClick = onBrochureClick,
                    modifier = Modifier
                        .padding(top = floatingContentTopPadding, bottom = contentBottomPadding),
                )
            } else if (bodyState.stripPages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (bodyState.section == DesignSection.IMAGES) adaptive.sectionSpacing * 2f else 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyStripState(label = bodyState.section.label)
                }
            } else if (bodyState.section == DesignSection.IMAGES) {
                ProjectImagesCarousel(
                    items = bodyState.stripPages.flatten(),
                    compactLandscape = compactLandscape,
                    onFeatureProjectOpen = onFeatureProjectOpen,
                    modifier = Modifier
                        .padding(top = floatingContentTopPadding, bottom = contentBottomPadding),
                )
            } else {
                DesignStripRail(
                    section = bodyState.section,
                    items = bodyState.stripPages.flatten(),
                    itemSpacing = itemSpacing,
                    posterWidth = posterWidth,
                    posterHeight = posterHeight,
                    posterTextVerticalPadding = posterTextVerticalPadding,
                    compactLandscape = compactLandscape,
                    onFeatureProjectOpen = onFeatureProjectOpen,
                    onVirtualTourOpen = onVirtualTourOpen,
                    onVideoClick = onVideoClick,
                    onBrochureClick = onBrochureClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }

        if (selectedSection == DesignSection.IMAGES && allProjectsLink.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = adaptive.sectionSpacing * 2f + adaptive.heroSpacing),
                horizontalArrangement = Arrangement.Center,
            ) {
                ShowcasePrimaryActionButton(
                    label = "View All Projects",
                    onClick = { onAllProjectsClick(allProjectsLink) },
                )
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectImagesCarousel(
    items: List<DesignStripItem>,
    compactLandscape: Boolean,
    onFeatureProjectOpen: (FeaturedProject) -> Unit,
    modifier: Modifier = Modifier,
) {
    val adaptive = LocalAdaptiveProfile.current
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStripState(label = DesignSection.IMAGES.label)
        }
        return
    }

    val preferredItemWidth = if (compactLandscape) 230.dp else 286.dp
    val imageHeight = if (compactLandscape) adaptive.cardHeightSmall else adaptive.cardHeightLarge
    val carouselHeight = imageHeight + adaptive.minTouchTarget + adaptive.heroSpacing * 1.8f

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.count() },
        modifier = modifier
            .fillMaxWidth()
            .height(carouselHeight),
        preferredItemWidth = preferredItemWidth,
        itemSpacing = adaptive.gutter,
        contentPadding = PaddingValues(horizontal = adaptive.contentPaddingHorizontal + adaptive.heroSpacing),
    ) { index ->
        val item = items[index]
        val project = item.source as? FeaturedProject
        val interactionSource = remember(item.id) { MutableInteractionSource() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = project != null,
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    project?.let(onFeatureProjectOpen)
                }
                .expressivePressScale(interactionSource, pressedScale = 0.97f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 10.dp else 14.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .maskClip(MaterialTheme.shapes.extraLarge),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
            ) {
                PosterImage(
                    imageUri = item.imageUri,
                    title = item.title,
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .maskClip(MaterialTheme.shapes.medium),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 2.dp,
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = if (compactLandscape) 9.dp else 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun DesignStripRail(
    section: DesignSection,
    items: List<DesignStripItem>,
    itemSpacing: Dp,
    posterWidth: Dp,
    posterHeight: Dp,
    posterTextVerticalPadding: Dp,
    compactLandscape: Boolean,
    onFeatureProjectOpen: (FeaturedProject) -> Unit,
    onVirtualTourOpen: (VirtualTour) -> Unit,
    onVideoClick: (String) -> Unit,
    onBrochureClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val openItem: (DesignStripItem) -> Unit = { item ->
        when (section) {
            DesignSection.IMAGES -> (item.source as? FeaturedProject)?.let(onFeatureProjectOpen)
            DesignSection.TOURS -> (item.source as? VirtualTour)?.let(onVirtualTourOpen)
            DesignSection.VIDEOS -> onVideoClick(item.id)
            DesignSection.BROCHURES -> onBrochureClick(item.id)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val effectivePosterWidth = if (items.size <= 5) {
            val fittedWidth = ((maxWidth - itemSpacing * (items.size - 1).toFloat()) / items.size.toFloat())
                .coerceAtLeast(132.dp)
            if (fittedWidth < posterWidth) fittedWidth else posterWidth
        } else {
            posterWidth
        }
        val card: @Composable (DesignStripItem) -> Unit = { item ->
            StandardPosterCard(
                item = item,
                onClick = { openItem(item) },
                cardWidth = effectivePosterWidth,
                imageHeight = posterHeight,
                textVerticalPadding = posterTextVerticalPadding,
            )
        }

        if (items.size <= 5) {
            val density = LocalDensity.current
            val tapPitchPx = with(density) { (effectivePosterWidth + itemSpacing).toPx() }
            val posterWidthPx = with(density) { effectivePosterWidth.toPx() }
            val posterHeightPx = with(density) { posterHeight.toPx() }
            val labelTopPx = with(density) {
                posterHeight.toPx() + (if (effectivePosterWidth < 220.dp) 8.dp else 12.dp).toPx()
            }
            val labelBottomPx = labelTopPx + with(density) {
                (MaterialTheme.typography.titleMedium.lineHeight.value.dp + (posterTextVerticalPadding * 2)).toPx()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(items, section, effectivePosterWidth, itemSpacing, posterHeight, posterTextVerticalPadding) {
                        detectTapGestures { offset ->
                            val totalWidth = (items.size * posterWidthPx) + ((items.size - 1) * with(density) { itemSpacing.toPx() })
                            val startX = (size.width - totalWidth) / 2f
                            val localX = offset.x - startX
                            if (localX < 0f || offset.y < 0f) return@detectTapGestures
                            val index = (localX / tapPitchPx).toInt()
                            val itemX = localX - (index * tapPitchPx)
                            val inPoster = offset.y <= posterHeightPx
                            val inLabel = offset.y in labelTopPx..labelBottomPx
                            if (index in items.indices && itemX <= posterWidthPx && (inPoster || inLabel)) {
                                openItem(items[index])
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Top,
                ) {
                    items.forEachIndexed { index, item ->
                        key("${section.routeKey}-${item.id}-$index") {
                            card(item)
                        }
                    }
                }
            }
        } else {
            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            val horizontalPadding = if (compactLandscape) 28.dp else 56.dp
            val tapPitchPx = with(density) { (effectivePosterWidth + itemSpacing).toPx() }
            val posterWidthPx = with(density) { effectivePosterWidth.toPx() }
            val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
            val verticalPaddingPx = with(density) { 8.dp.toPx() }
            val posterHeightPx = with(density) { posterHeight.toPx() }
            val labelTopPx = with(density) {
                8.dp.toPx() + posterHeight.toPx() + (if (posterWidth < 220.dp) 8.dp else 12.dp).toPx()
            }
            val labelBottomPx = labelTopPx + with(density) {
                (MaterialTheme.typography.titleMedium.lineHeight.value.dp + (posterTextVerticalPadding * 2)).toPx()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(items, section, posterWidth, itemSpacing, compactLandscape) {
                        detectTapGestures { offset ->
                            val localX = offset.x + scrollState.value - horizontalPaddingPx
                            val localY = offset.y - verticalPaddingPx
                            if (localX < 0f || localY < 0f) return@detectTapGestures
                            val index = (localX / tapPitchPx).toInt()
                            val itemX = localX - (index * tapPitchPx)
                            val inPoster = localY <= posterHeightPx
                            val inLabel = localY in labelTopPx..labelBottomPx
                            if (index in items.indices && itemX <= posterWidthPx && (inPoster || inLabel)) {
                                openItem(items[index])
                            }
                        }
                    }
                    .horizontalScroll(scrollState)
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.Top,
            ) {
                items.forEachIndexed { index, item ->
                    key("${section.routeKey}-${item.id}-$index") {
                        card(item)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyStripState(
    label: String,
) {
    Surface(
        modifier = Modifier.widthIn(max = 720.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            text = "No $label added yet.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesignVideosEditorialList(
    videos: List<ShowcaseVideo>,
    onVideoClick: (String) -> Unit,
    compactLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStripState(label = DesignSection.VIDEOS.label)
        }
        return
    }

    Column(
        modifier = modifier
            .widthIn(max = 1240.dp)
            .padding(horizontal = if (compactLandscape) 12.dp else 28.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 12.dp else 26.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Video Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Tap a film to open the client player.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                    maxLines = if (compactLandscape) 1 else Int.MAX_VALUE,
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = "${videos.size} videos",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { videos.count() },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactLandscape) 218.dp else 332.dp),
            preferredItemWidth = if (compactLandscape) 270.dp else 382.dp,
            itemSpacing = if (compactLandscape) 12.dp else 18.dp,
            contentPadding = PaddingValues(
                horizontal = if (compactLandscape) 14.dp else 26.dp,
                vertical = 6.dp,
            ),
        ) { index ->
            VideoShowcaseCard(
                video = videos[index],
                onClick = { onVideoClick(videos[index].id) },
                compactLandscape = compactLandscape,
                modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
            )
        }
    }
}

@Composable
internal fun VideoShowcaseCard(
    video: ShowcaseVideo,
    onClick: () -> Unit,
    compactLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    val thumbnail = video.thumbnailUri ?: youtubeThumbnail(video.youtubeLink).orEmpty()
    val interactionSource = remember { MutableInteractionSource() }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val sharedBoundsSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()

    ElevatedCard(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(if (compactLandscape) 204.dp else 300.dp)
            .fillMaxWidth()
            .expressivePressScale(interactionSource, pressedScale = 0.97f)
            .clip(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compactLandscape) 132.dp else 204.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .let { baseModifier ->
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    baseModifier.sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "video-thumb-${video.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = { _, _ -> sharedBoundsSpec },
                                    )
                                }
                            } else {
                                baseModifier
                            }
                        },
                ) {
                    OptimizedAsyncImage(
                        model = thumbnail,
                        contentDescription = video.title,
                        maxWidth = 720.dp,
                        maxHeight = 420.dp,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(
                            horizontal = if (compactLandscape) 12.dp else 18.dp,
                            vertical = if (compactLandscape) 8.dp else 14.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(if (compactLandscape) 8.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = video.title.ifBlank { "Untitled Video" },
                            modifier = Modifier.let { baseModifier ->
                                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        baseModifier.sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "video-title-${video.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            enter = fadeIn(),
                                            exit = fadeOut(),
                                            boundsTransform = { _, _ -> sharedBoundsSpec },
                                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                        )
                                    }
                                } else {
                                    baseModifier
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = video.description.ifBlank { "Client presentation film" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedPosterCard(
    item: DesignStripItem,
    sharedKeyPrefix: String,
    onClick: () -> Unit,
    cardWidth: Dp,
    imageHeight: Dp,
    textVerticalPadding: Dp,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val imageKeyPrefix = when (sharedKeyPrefix) {
        "project" -> "project-image"
        "tour" -> "tour-thumb"
        else -> "$sharedKeyPrefix-image"
    }
    val titleKeyPrefix = when (sharedKeyPrefix) {
        "project" -> "project-title"
        "tour" -> "tour-title"
        else -> "$sharedKeyPrefix-title"
    }
    val sharedBoundsSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()

    Column(
        modifier = Modifier.width(cardWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (cardWidth < 220.dp) 8.dp else 12.dp),
    ) {
        with(sharedTransitionScope) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .expressivePressScale(interactionSource, pressedScale = 0.988f)
                    .clip(MaterialTheme.shapes.large)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "$imageKeyPrefix-${item.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> sharedBoundsSpec },
                    ),
                onClick = onClick,
                interactionSource = interactionSource,
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 6.dp,
                ),
            ) {
                PosterImage(
                    imageUri = item.imageUri,
                    title = item.title,
                )
            }

            Surface(
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "$titleKeyPrefix-${item.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        boundsTransform = { _, _ -> sharedBoundsSpec },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                    )
                    .clickable(onClick = onClick),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = textVerticalPadding),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun StandardPosterCard(
    item: DesignStripItem,
    onClick: () -> Unit,
    cardWidth: Dp,
    imageHeight: Dp,
    textVerticalPadding: Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val openCard = {
        onClick()
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .expressivePressScale(interactionSource, pressedScale = 0.988f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (cardWidth < 220.dp) 8.dp else 12.dp),
    ) {
        ElevatedCard(
            onClick = openCard,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(MaterialTheme.shapes.large),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 6.dp,
            ),
        ) {
            PosterImage(
                imageUri = item.imageUri,
                title = item.title,
            )
        }

        Surface(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = openCard,
            ),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Text(
                text = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = textVerticalPadding),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PosterImage(
    imageUri: String,
    title: String,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OptimizedAsyncImage(
            model = imageUri,
            contentDescription = title,
            maxWidth = 520.dp,
            maxHeight = 360.dp,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.34f),
                        ),
                    ),
                ),
        )
    }
}
