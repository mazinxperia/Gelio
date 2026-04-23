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
import io.gelio.app.features.artgallery.ArtGallerySectionViewer
import io.gelio.app.features.contentpage.ContentPageSectionViewer
import io.gelio.app.features.map.MapAssetPaths
import io.gelio.app.features.map.WorldMapSectionViewer
import io.gelio.app.features.map.rememberWorldMapAsset
import io.gelio.app.features.reviews.RatingsSectionViewer
import io.gelio.app.features.sections.rememberClientSectionScopedState
import io.gelio.app.features.tourism.TourismFullWidthStrip
import io.gelio.app.features.tourism.TourismStripItem
import io.gelio.app.media.webview.ShowcaseWebFrame


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DesignShellScreen(
    selectedSection: DesignSection,
    featuredProjects: List<FeaturedProject>,
    virtualTours: List<VirtualTour>,
    videos: List<ShowcaseVideo>,
    brochures: List<Brochure>,
    allProjectsLink: String,
    onSectionSelected: (DesignSection) -> Unit,
    onWelcomeClick: () -> Unit,
    onAllProjectsClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBrochureClick: (String) -> Unit,
    embeddedInClientStage: Boolean = false,
    onDetailModeChange: (Boolean) -> Unit = {},
) {
    val autoTourThumbnails by produceState<Map<String, String>>(initialValue = emptyMap(), virtualTours) {
        val resolved = mutableMapOf<String, String>()
        virtualTours
            .filter { it.thumbnailUri.isBlank() }
            .forEach { tour ->
                val thumbnail = resolveVirtualTourThumbnailUrl(tour.embedUrl).orEmpty()
                if (thumbnail.isNotBlank()) {
                    resolved[tour.id] = thumbnail
                    value = resolved.toMap()
                }
            }
    }
    val stripItems = remember(selectedSection, featuredProjects, virtualTours, videos, brochures, autoTourThumbnails) {
        when (selectedSection) {
            DesignSection.IMAGES -> featuredProjects.map {
                DesignStripItem(
                    id = it.id,
                    title = it.projectName,
                    imageUri = it.thumbnailUri,
                    source = it,
                )
            }

            DesignSection.TOURS -> virtualTours.map {
                DesignStripItem(
                    id = it.id,
                    title = it.projectName,
                    imageUri = it.thumbnailUri
                        .ifBlank { autoTourThumbnails[it.id].orEmpty() }
                        .let(::cleanVirtualTourThumbnailUrl),
                    source = it,
                )
            }

            DesignSection.VIDEOS -> videos.map {
                DesignStripItem(
                    id = it.id,
                    title = it.title,
                    imageUri = it.thumbnailUri ?: youtubeThumbnail(it.youtubeLink).orEmpty(),
                    source = it,
                )
            }

            DesignSection.BROCHURES -> brochures.map {
                DesignStripItem(
                    id = it.id,
                    title = it.title,
                    imageUri = it.coverThumbnailUri,
                    source = it,
                )
            }
        }
    }
    val stripPages = remember(stripItems) { stripItems.chunked(4) }
    var pageIndex by remember(selectedSection, stripItems.size) { mutableIntStateOf(0) }
    var detailProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailTourId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailTarget = remember(detailProjectId, detailTourId, featuredProjects, virtualTours) {
        when {
            detailProjectId != null -> featuredProjects
                .firstOrNull { it.id == detailProjectId }
                ?.let(DesignDetailTarget::Project)

            detailTourId != null -> virtualTours
                .firstOrNull { it.id == detailTourId }
                ?.let(DesignDetailTarget::Tour)

            else -> null
        }
    }
    fun clearDetailTarget() {
        detailProjectId = null
        detailTourId = null
    }

    LaunchedEffect(selectedSection) {
        pageIndex = 0
        clearDetailTarget()
    }

    LaunchedEffect(detailTarget != null) {
        onDetailModeChange(detailTarget != null)
    }

    BackHandler(enabled = detailTarget != null) {
        clearDetailTarget()
    }

    val content: @Composable () -> Unit = {
        val sharedTransitionScope = checkNotNull(LocalSharedTransitionScope.current) {
            "DesignShellScreen requires the app-level SharedTransitionLayout"
        }
        val detailSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val detailEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val adaptive = LocalAdaptiveProfile.current

        AnimatedContent(
            targetState = detailTarget,
            transitionSpec = directionalTabTransitionSpec(detailSpatial, detailEffects) { if (it == null) 0 else 1 },
            label = "design_detail_transition",
        ) { target ->
            val contentAnimatedVisibilityScope = this
            when {
                target is DesignDetailTarget.Project -> {
                    DesignProjectGalleryDetail(
                        project = target.project,
                        onBack = {
                            clearDetailTarget()
                        },
                        onHome = {
                            clearDetailTarget()
                            onWelcomeClick()
                        },
                        onClose = onWelcomeClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    )
                }

                target is DesignDetailTarget.Tour -> {
                    DesignVirtualTourDetail(
                        tour = target.tour,
                        onBack = {
                            clearDetailTarget()
                        },
                        onHome = {
                            clearDetailTarget()
                            onWelcomeClick()
                        },
                        onClose = onWelcomeClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    )
                }

                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val compactLandscape = adaptive.compactLandscape || maxWidth < 820.dp || maxHeight < 560.dp
                        val tabTopPadding = if (embeddedInClientStage) {
                            adaptive.topChromeReserve + adaptive.heroSpacing
                        } else {
                            adaptive.contentPaddingVertical + adaptive.minTouchTarget
                        }
                        val floatingContentTopPadding = tabTopPadding + adaptive.minTouchTarget + adaptive.sectionSpacing
                        DesignSectionStage(
                            selectedSection = selectedSection,
                            stripPages = stripPages,
                            videos = videos,
                            brochures = brochures,
                            pageIndex = pageIndex,
                            onPageIndexChange = { pageIndex = it },
                            onSectionSelected = onSectionSelected,
                            onWelcomeClick = onWelcomeClick,
                            allProjectsLink = allProjectsLink,
                            onAllProjectsClick = onAllProjectsClick,
                            onFeatureProjectOpen = {
                                detailProjectId = it.id
                                detailTourId = null
                            },
                            onVirtualTourOpen = {
                                detailProjectId = null
                                detailTourId = it.id
                            },
                            onVideoClick = onVideoClick,
                            onBrochureClick = onBrochureClick,
                            showHomeButton = !embeddedInClientStage,
                            tabTopPadding = tabTopPadding,
                            floatingContentTopPadding = floatingContentTopPadding,
                            compactLandscape = compactLandscape,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = contentAnimatedVisibilityScope,
                        )
                    }
                }
            }
        }
    }

    if (embeddedInClientStage) {
        content()
    } else {
        ShowcaseBackground { content() }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DynamicDesignCompanyShellScreen(
    company: ShowcaseCompany?,
    sections: List<ShowcaseSection>,
    selectedSectionId: String?,
    showcaseViewModel: ShowcaseViewModel,
    onSectionSelected: (String) -> Unit,
    onWelcomeClick: () -> Unit,
    onAllProjectsClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBrochureClick: (String) -> Unit,
    embeddedInClientStage: Boolean = false,
    onDetailModeChange: (Boolean) -> Unit = {},
) {
    // Keep the last resolved section across recompositions so a transient empty/null
    // selectedSectionId (e.g. returning from admin) does not fall through to an unwarmed section.
    var lastResolvedSection by remember { mutableStateOf<ShowcaseSection?>(null) }
    val activeSection = remember(sections.map { it.id }, selectedSectionId) {
        val resolved = sections.firstOrNull { it.id == selectedSectionId }
            ?: sections.firstOrNull { it.id == lastResolvedSection?.id }
            ?: if (sections.isEmpty()) lastResolvedSection else null
            ?: sections.firstOrNull()
        resolved
    }.also { if (it != null) lastResolvedSection = it }
    val activeSectionState = rememberClientSectionScopedState(activeSection, showcaseViewModel)

    var detailProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailTourId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailTarget = remember(
        detailProjectId,
        detailTourId,
        activeSectionState.featuredProjects,
        activeSectionState.virtualTours,
    ) {
        when {
            detailProjectId != null -> activeSectionState.featuredProjects
                .firstOrNull { it.id == detailProjectId }
                ?.let(DesignDetailTarget::Project)

            detailTourId != null -> activeSectionState.virtualTours
                .firstOrNull { it.id == detailTourId }
                ?.let(DesignDetailTarget::Tour)

            else -> null
        }
    }

    fun clearDetailTarget() {
        detailProjectId = null
        detailTourId = null
    }

    LaunchedEffect(activeSection?.id) {
        clearDetailTarget()
    }

    LaunchedEffect(detailTarget != null) {
        onDetailModeChange(detailTarget != null)
    }

    BackHandler(enabled = detailTarget != null) {
        clearDetailTarget()
    }

    val content: @Composable () -> Unit = {
        val sharedTransitionScope = checkNotNull(LocalSharedTransitionScope.current) {
            "DynamicDesignCompanyShellScreen requires the app-level SharedTransitionLayout"
        }
        val detailSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val detailEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val adaptive = LocalAdaptiveProfile.current

        AnimatedContent(
            targetState = detailTarget,
            transitionSpec = directionalTabTransitionSpec(detailSpatial, detailEffects) { if (it == null) 0 else 1 },
            label = "dynamic_design_detail_transition",
        ) { target ->
            val contentAnimatedVisibilityScope = this
            when {
                target is DesignDetailTarget.Project -> {
                    DesignProjectGalleryDetail(
                        project = target.project,
                        onBack = ::clearDetailTarget,
                        onHome = {
                            clearDetailTarget()
                            onWelcomeClick()
                        },
                        onClose = onWelcomeClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    )
                }

                target is DesignDetailTarget.Tour -> {
                    DesignVirtualTourDetail(
                        tour = target.tour,
                        onBack = ::clearDetailTarget,
                        onHome = {
                            clearDetailTarget()
                            onWelcomeClick()
                        },
                        onClose = onWelcomeClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    )
                }

                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val compactLandscape = adaptive.compactLandscape || maxWidth < 820.dp || maxHeight < 560.dp
                        val tabTopPadding = if (embeddedInClientStage) {
                            adaptive.topChromeReserve + adaptive.heroSpacing
                        } else {
                            adaptive.contentPaddingVertical + adaptive.minTouchTarget
                        }
                        val floatingContentTopPadding = tabTopPadding + adaptive.minTouchTarget + adaptive.sectionSpacing
                        DynamicDesignSectionStage(
                            company = company,
                            sections = sections,
                            selectedSection = activeSection,
                            showcaseViewModel = showcaseViewModel,
                            onSectionSelected = onSectionSelected,
                            onWelcomeClick = onWelcomeClick,
                            onAllProjectsClick = onAllProjectsClick,
                            onFeatureProjectOpen = {
                                detailProjectId = it.id
                                detailTourId = null
                            },
                            onVirtualTourOpen = {
                                detailProjectId = null
                                detailTourId = it.id
                            },
                            onVideoClick = onVideoClick,
                            onBrochureClick = onBrochureClick,
                            showHomeButton = !embeddedInClientStage,
                            tabTopPadding = tabTopPadding,
                            floatingContentTopPadding = floatingContentTopPadding,
                            compactLandscape = compactLandscape,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = contentAnimatedVisibilityScope,
                        )
                    }
                }
            }
        }
    }

    if (embeddedInClientStage) {
        content()
    } else {
        ShowcaseBackground { content() }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DynamicDesignSectionStage(
    company: ShowcaseCompany?,
    sections: List<ShowcaseSection>,
    selectedSection: ShowcaseSection?,
    showcaseViewModel: ShowcaseViewModel,
    onSectionSelected: (String) -> Unit,
    onWelcomeClick: () -> Unit,
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
    val selectedSectionState = rememberClientSectionScopedState(selectedSection, showcaseViewModel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = tabTopPadding)
                .zIndex(2f),
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
                sections.forEach { section ->
                    ShowcaseSectionToggleButton(
                        label = section.title,
                        selected = section.id == selectedSection?.id,
                        onClick = { onSectionSelected(section.id) },
                    )
                }
            }
        }

            if (showHomeButton) {
                ShowcaseHomeButton(
                    onClick = onWelcomeClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = adaptive.homeButtonInsetTop, start = adaptive.homeButtonInsetStart)
                        .zIndex(2f),
                )
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .padding(top = floatingContentTopPadding),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = selectedSection,
                transitionSpec = directionalTabTransitionSpec(pageSpatial, pageEffects) { state ->
                    sections.indexOfFirst { it.id == state?.id }.coerceAtLeast(0)
                },
                    label = "dynamic_design_section_body",
                    modifier = Modifier.fillMaxWidth(),
                ) { section ->
                val scopedState = rememberClientSectionScopedState(section, showcaseViewModel)
                when {
                    section == null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyStripState(label = company?.name ?: "Company")
                        }
                    }

                    section.type == SectionType.YOUTUBE_VIDEOS -> {
                        DesignVideosEditorialList(
                            videos = scopedState.videos,
                            onVideoClick = onVideoClick,
                            compactLandscape = compactLandscape,
                            modifier = Modifier.padding(bottom = contentBottomPadding),
                        )
                    }

                    section.type == SectionType.PDF_VIEWER -> {
                        if (scopedState.brochures.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                EmptyStripState(label = section.title)
                            }
                        } else {
                            BrochureCarousel(
                                brochures = scopedState.brochures,
                                onBrochureClick = onBrochureClick,
                                modifier = Modifier.padding(bottom = contentBottomPadding),
                            )
                        }
                    }

                    section.type == SectionType.WORLD_MAP -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = contentBottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            WorldMapSectionViewer(
                                section = scopedState.worldMapSection,
                                pins = scopedState.worldMapPins,
                                sectionTitle = section.title,
                                compactLandscape = compactLandscape,
                            )
                        }
                    }

                    section.type == SectionType.GOOGLE_REVIEWS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = contentBottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            RatingsSectionViewer(
                                title = section.title,
                                items = scopedState.reviewCards,
                                compactLandscape = compactLandscape,
                            )
                        }
                    }

                    section.type == SectionType.CONTENT_PAGE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = contentBottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            ContentPageSectionViewer(
                                title = section.title,
                                items = scopedState.contentPageCards,
                                compactLandscape = compactLandscape,
                            )
                        }
                    }

                    section.type == SectionType.ART_GALLERY -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = contentBottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            ArtGallerySectionViewer(
                                title = section.title,
                                groups = scopedState.artGalleryGroups,
                                compactLandscape = compactLandscape,
                            )
                        }
                    }

                    section.type == SectionType.DESTINATIONS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = contentBottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            TourismFullWidthStrip(
                                items = scopedState.destinations.map {
                                    TourismStripItem(
                                        id = it.id,
                                        title = it.destinationName,
                                        subtitle = it.subtitle,
                                        imageUri = it.imageUri,
                                        onClick = {},
                                    )
                                },
                                emptyLabel = section.title,
                                compactLandscape = compactLandscape,
                            )
                        }
                    }

                    section.type == SectionType.SERVICES -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = contentBottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            TourismFullWidthStrip(
                                items = scopedState.services.map {
                                    TourismStripItem(
                                        id = it.id,
                                        title = it.serviceTitle,
                                        subtitle = it.description,
                                        imageUri = it.imageUri,
                                        onClick = {},
                                    )
                                },
                                emptyLabel = section.title,
                                compactLandscape = compactLandscape,
                            )
                        }
                    }

                    scopedState.stripItems.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (section.type == SectionType.IMAGE_GALLERY && !compactLandscape) 64.dp else 0.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyStripState(label = section.title)
                        }
                    }

                    section.type == SectionType.IMAGE_GALLERY -> {
                        ProjectImagesCarousel(
                            items = scopedState.stripItems,
                            compactLandscape = compactLandscape,
                            onFeatureProjectOpen = onFeatureProjectOpen,
                            modifier = Modifier.padding(bottom = contentBottomPadding),
                        )
                    }

                    section.type == SectionType.TOUR_360 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = contentBottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            DesignStripRail(
                                section = DesignSection.TOURS,
                                items = scopedState.stripItems,
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

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyStripState(label = section.title)
                        }
                    }
                }
            }
        }

        if (selectedSection?.type == SectionType.IMAGE_GALLERY && selectedSectionState.allProjectsLink.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (compactLandscape) 16.dp else 90.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                ShowcasePrimaryActionButton(
                    label = "View All Projects",
                    onClick = { onAllProjectsClick(selectedSectionState.allProjectsLink) },
                )
            }
        }
    }
}

