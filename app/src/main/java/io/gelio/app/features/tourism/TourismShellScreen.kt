@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.tourism

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
import io.gelio.app.core.navigation.TourismSection
import io.gelio.app.core.theme.LocalSharedTransitionScope
import io.gelio.app.core.theme.directionalTabTransitionSpec
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ShowcaseHomeButton
import io.gelio.app.core.ui.ShowcasePrimaryActionButton
import io.gelio.app.core.ui.ShowcaseSectionToggleButton
import io.gelio.app.core.ui.ShowcaseTonalIconButton
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
import io.gelio.app.features.design.DesignProjectGalleryDetail
import io.gelio.app.features.design.DesignStripItem
import io.gelio.app.features.design.DesignStripRail
import io.gelio.app.features.design.DesignVideosEditorialList
import io.gelio.app.features.design.DesignVirtualTourDetail
import io.gelio.app.features.design.EmptyStripState
import io.gelio.app.features.design.ProjectImagesCarousel
import io.gelio.app.features.map.MapAssetPaths
import io.gelio.app.features.map.WorldMapSectionViewer
import io.gelio.app.features.map.rememberWorldMapAsset
import io.gelio.app.features.reviews.RatingsSectionViewer
import io.gelio.app.features.sections.rememberClientSectionScopedState
import io.gelio.app.core.util.cleanVirtualTourThumbnailUrl
import io.gelio.app.core.util.resolveVirtualTourThumbnailUrl
import io.gelio.app.core.util.youtubeThumbnail
import kotlinx.coroutines.launch

@Composable
fun TourismShellScreen(
    selectedSection: TourismSection,
    destinations: List<Destination>,
    services: List<Service>,
    brochures: List<Brochure>,
    onSectionSelected: (TourismSection) -> Unit,
    onWelcomeClick: () -> Unit,
    onBrochureClick: (String) -> Unit,
    embeddedInClientStage: Boolean = false,
) {
    val tabSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val tabEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    val content: @Composable () -> Unit = {
        val adaptive = LocalAdaptiveProfile.current
        val tokens = LocalLayoutTokens.current
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compactLandscape = adaptive.compactLandscape || maxWidth < 820.dp || maxHeight < 560.dp
            val tabTopPadding = if (embeddedInClientStage) {
                adaptive.topChromeReserve + adaptive.heroSpacing
            } else {
                adaptive.contentPaddingVertical + adaptive.minTouchTarget
            }
            val contentTopPadding = tabTopPadding + adaptive.minTouchTarget + adaptive.sectionSpacing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = adaptive.contentPaddingHorizontal,
                    vertical = adaptive.contentPaddingVertical,
                ),
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
                    horizontalArrangement = Arrangement.spacedBy(adaptive.tabSpacing, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TourismSection.entries.forEach { section ->
                        ShowcaseSectionToggleButton(
                            label = section.label,
                            selected = section == selectedSection,
                            onClick = { onSectionSelected(section) },
                        )
                    }
                }
            }

            if (!embeddedInClientStage) {
                ShowcaseHomeButton(
                    onClick = onWelcomeClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = adaptive.homeButtonInsetTop, start = adaptive.homeButtonInsetStart),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(top = contentTopPadding)
                    .padding(horizontal = adaptive.heroSpacing * 0.5f),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = selectedSection,
                    transitionSpec = directionalTabTransitionSpec(tabSpatial, tabEffects) { it.ordinal },
                    label = "tourism_strip_content",
                ) { section ->
                    val sectionItems = when (section) {
                        TourismSection.DESTINATIONS -> destinations.map {
                            TourismStripItem(
                                id = it.id,
                                title = it.destinationName,
                                subtitle = it.subtitle,
                                imageUri = it.imageUri,
                                onClick = {},
                            )
                        }

                        TourismSection.SERVICES -> services.map {
                            TourismStripItem(
                                id = it.id,
                                title = it.serviceTitle,
                                subtitle = it.description,
                                imageUri = it.imageUri,
                                onClick = {},
                            )
                        }

                        TourismSection.BROCHURES -> brochures.map {
                            TourismStripItem(
                                id = it.id,
                                title = it.title,
                                subtitle = "PDF brochure",
                                imageUri = it.coverThumbnailUri,
                                onClick = { onBrochureClick(it) },
                            )
                        }
                    }

                    if (section == TourismSection.BROCHURES) {
                        BrochureCarousel(
                            brochures = brochures,
                            onBrochureClick = onBrochureClick,
                        )
                    } else {
                        TourismFullWidthStrip(
                            items = sectionItems,
                            emptyLabel = section.label,
                            compactLandscape = compactLandscape,
                        )
                    }
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
fun DynamicTourismCompanyShellScreen(
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
        sections.firstOrNull { it.id == selectedSectionId }
            ?: sections.firstOrNull { it.id == lastResolvedSection?.id }
            ?: if (sections.isEmpty()) lastResolvedSection else null
            ?: sections.firstOrNull()
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
                ?.let(TourismDetailTarget::Project)

            detailTourId != null -> activeSectionState.virtualTours
                .firstOrNull { it.id == detailTourId }
                ?.let(TourismDetailTarget::Tour)

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
            "DynamicTourismCompanyShellScreen requires the app-level SharedTransitionLayout"
        }
        val detailSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val detailEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val adaptive = LocalAdaptiveProfile.current

        AnimatedContent(
            targetState = detailTarget,
            transitionSpec = directionalTabTransitionSpec(detailSpatial, detailEffects) { if (it == null) 0 else 1 },
            label = "dynamic_tourism_detail_transition",
        ) { target ->
            val contentAnimatedVisibilityScope = this
            when {
                target is TourismDetailTarget.Project -> {
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

                target is TourismDetailTarget.Tour -> {
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
                        DynamicTourismSectionStage(
                            company = company,
                            sections = sections,
                            selectedSection = activeSection,
                            showcaseViewModel = showcaseViewModel,
                            onSectionSelected = onSectionSelected,
                            onWelcomeClick = onWelcomeClick,
                            onAllProjectsClick = onAllProjectsClick,
                            onProjectOpen = {
                                detailProjectId = it.id
                                detailTourId = null
                            },
                            onTourOpen = {
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
private fun DynamicTourismSectionStage(
    company: ShowcaseCompany?,
    sections: List<ShowcaseSection>,
    selectedSection: ShowcaseSection?,
    showcaseViewModel: ShowcaseViewModel,
    onSectionSelected: (String) -> Unit,
    onWelcomeClick: () -> Unit,
    onAllProjectsClick: (String) -> Unit,
    onProjectOpen: (FeaturedProject) -> Unit,
    onTourOpen: (VirtualTour) -> Unit,
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
    val tabSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val tabEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val itemSpacing = adaptive.gutter
    val posterWidth = if (compactLandscape) 178.dp else 248.dp
    val posterHeight = if (compactLandscape) adaptive.cardHeightSmall else adaptive.cardHeightMedium
    val posterTextVerticalPadding = adaptive.heroSpacing
    val contentBottomPadding = adaptive.sectionSpacing + adaptive.heroSpacing
    val selectedSectionState = rememberClientSectionScopedState(selectedSection, showcaseViewModel)

    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = adaptive.contentPaddingHorizontal,
                    vertical = adaptive.contentPaddingVertical,
                ),
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
                    horizontalArrangement = Arrangement.spacedBy(adaptive.tabSpacing, Alignment.CenterHorizontally),
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
                    transitionSpec = directionalTabTransitionSpec(tabSpatial, tabEffects) { section ->
                        sections.indexOfFirst { it.id == section?.id }.coerceAtLeast(0)
                    },
                    label = "tourism_dynamic_strip_content",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = adaptive.heroSpacing * 0.5f),
                ) { section ->
                    val scopedState = rememberClientSectionScopedState(section, showcaseViewModel)
                    when {
                        section == null -> {
                            EmptyStripState(company?.name ?: "Company")
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

                        section.type == SectionType.PDF_VIEWER -> {
                            if (scopedState.brochures.isEmpty()) {
                                EmptyStripState(section.title)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = contentBottomPadding),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    BrochureCarousel(
                                        brochures = scopedState.brochures,
                                        onBrochureClick = onBrochureClick,
                                    )
                                }
                            }
                        }

                        section.type == SectionType.IMAGE_GALLERY -> {
                            if (scopedState.stripItems.isEmpty()) {
                                EmptyStripState(section.title)
                            } else {
                                Column(
                                    modifier = Modifier.padding(bottom = contentBottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 18.dp else 24.dp),
                                ) {
                                    ProjectImagesCarousel(
                                        items = scopedState.stripItems,
                                        compactLandscape = compactLandscape,
                                        onFeatureProjectOpen = onProjectOpen,
                                    )
                                    if (selectedSectionState.allProjectsLink.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
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
                        }

                        section.type == SectionType.TOUR_360 -> {
                            if (scopedState.stripItems.isEmpty()) {
                                EmptyStripState(section.title)
                            } else {
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
                                        onFeatureProjectOpen = onProjectOpen,
                                        onVirtualTourOpen = onTourOpen,
                                        onVideoClick = onVideoClick,
                                        onBrochureClick = onBrochureClick,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    )
                                }
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

                        else -> {
                            EmptyStripState(section.title)
                        }
                    }
                }
            }
        }
    }

    content()
}

@Composable
internal fun TourismFullWidthStrip(
    items: List<TourismStripItem>,
    emptyLabel: String,
    compactLandscape: Boolean,
) {
    val adaptive = LocalAdaptiveProfile.current
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(adaptive.previewFrameHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showControls = items.size > 4
    val cardWidth = if (compactLandscape) 200.dp else 286.dp
    val cardHeight = if (compactLandscape) adaptive.cardHeightSmall else adaptive.cardHeightMedium
    val itemSpacing = adaptive.gutter

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = adaptive.heroSpacing * 0.25f),
        horizontalArrangement = Arrangement.spacedBy(adaptive.heroSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showControls) {
            ShowcaseTonalIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Scroll left",
                onClick = {
                    val target = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                    scope.launch { listState.animateScrollToItem(target) }
                },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (items.size <= 4) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.Top,
                ) {
                    items.forEach { item ->
                        TourismPosterCard(item = item, cardWidth = cardWidth, imageHeight = cardHeight)
                    }
                }
            } else {
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.Top,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = adaptive.heroSpacing),
                ) {
                    items(
                        items = items,
                        key = { it.id },
                        contentType = { "tourism_poster" },
                    ) { item ->
                        TourismPosterCard(item = item, cardWidth = cardWidth, imageHeight = cardHeight)
                    }
                }
            }
        }

        if (showControls) {
            ShowcaseTonalIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Scroll right",
                onClick = {
                    val target = (listState.firstVisibleItemIndex + 1).coerceAtMost(items.lastIndex)
                    scope.launch { listState.animateScrollToItem(target) }
                },
            )
        }
    }
}

@Composable
private fun TourismPosterCard(
    item: TourismStripItem,
    cardWidth: Dp,
    imageHeight: Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier.width(cardWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (cardWidth < 240.dp) 8.dp else 12.dp),
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .expressivePressScale(interactionSource, pressedScale = 0.985f),
            onClick = { item.onClick(item.id) },
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 6.dp,
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                OptimizedAsyncImage(
                    model = item.imageUri,
                    contentDescription = item.title,
                    maxWidth = 460.dp,
                    maxHeight = 280.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

internal data class TourismStripItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUri: String,
    val onClick: (String) -> Unit,
)

private sealed interface TourismDetailTarget {
    data class Project(val project: FeaturedProject) : TourismDetailTarget
    data class Tour(val tour: VirtualTour) : TourismDetailTarget
}
