@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.company

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.gelio.app.app.ShowcaseViewModel
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens
import io.gelio.app.core.theme.directionalTabTransitionSpec
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ShowcaseHomeButton
import io.gelio.app.core.ui.ShowcasePrimaryActionButton
import io.gelio.app.core.ui.ShowcaseSectionToggleButton
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
import io.gelio.app.features.map.WorldMapSectionViewer
import io.gelio.app.features.reviews.RatingsSectionViewer
import io.gelio.app.features.sections.rememberClientSectionScopedState
import io.gelio.app.app.LocalScreenConfig
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.foundation.layout.widthIn

@Composable
fun CompanyShellScreen(
    company: ShowcaseCompany?,
    sections: List<ShowcaseSection>,
    selectedSectionId: String?,
    showcaseViewModel: ShowcaseViewModel,
    onSectionSelected: (String) -> Unit,
    onWelcomeClick: () -> Unit,
    onProjectClick: (String) -> Unit,
    onTourClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBrochureClick: (String) -> Unit,
    onAllProjectsClick: (String) -> Unit,
    embeddedInClientStage: Boolean = false,
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
            val floatingContentTopPadding = tabTopPadding + adaptive.minTouchTarget + adaptive.sectionSpacing

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
                                selected = section.id == activeSection?.id,
                                onClick = { onSectionSelected(section.id) },
                            )
                        }
                    }
                }

                if (!embeddedInClientStage) {
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
                        targetState = activeSection,
                        transitionSpec = directionalTabTransitionSpec(tabSpatial, tabEffects) { targetSection ->
                            sections.indexOfFirst { section -> section.id == targetSection?.id }.coerceAtLeast(0)
                        },
                        label = "dynamic_company_section",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = adaptive.heroSpacing * 0.5f),
                    ) { targetSection ->
                        if (targetSection == null) {
                            EmptySectionState(
                                label = company?.name ?: "Company",
                            )
                        } else {
                            DynamicSectionContent(
                                section = targetSection,
                                showcaseViewModel = showcaseViewModel,
                                compactLandscape = compactLandscape,
                                onProjectClick = onProjectClick,
                                onTourClick = onTourClick,
                                onVideoClick = onVideoClick,
                                onBrochureClick = onBrochureClick,
                                onAllProjectsClick = onAllProjectsClick,
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

@Composable
private fun DynamicSectionContent(
    section: ShowcaseSection,
    showcaseViewModel: ShowcaseViewModel,
    compactLandscape: Boolean,
    onProjectClick: (String) -> Unit,
    onTourClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBrochureClick: (String) -> Unit,
    onAllProjectsClick: (String) -> Unit,
) {
    val sectionState = rememberClientSectionScopedState(section, showcaseViewModel)
    when (section.type) {
        SectionType.IMAGE_GALLERY -> {
            ImageGallerySection(
                title = section.title,
                items = sectionState.featuredProjects,
                allProjectsUrl = sectionState.allProjectsLink,
                compactLandscape = compactLandscape,
                onProjectClick = onProjectClick,
                onAllProjectsClick = onAllProjectsClick,
            )
        }

        SectionType.TOUR_360 -> {
            MediaCarouselSection(
                title = section.title,
                items = sectionState.stripItems.map {
                    GenericSectionItem(it.id, it.title, it.imageUri)
                },
                emptyLabel = section.title,
                compactLandscape = compactLandscape,
                onItemClick = onTourClick,
            )
        }

        SectionType.YOUTUBE_VIDEOS -> {
            MediaCarouselSection(
                title = section.title,
                items = sectionState.stripItems.map {
                    GenericSectionItem(it.id, it.title, it.imageUri)
                },
                emptyLabel = section.title,
                compactLandscape = compactLandscape,
                onItemClick = onVideoClick,
            )
        }

        SectionType.PDF_VIEWER -> {
            if (sectionState.brochures.isEmpty()) {
                EmptySectionState(section.title)
            } else {
                BrochureCarousel(
                    brochures = sectionState.brochures,
                    onBrochureClick = onBrochureClick,
                )
            }
        }

        SectionType.DESTINATIONS -> {
            MediaCarouselSection(
                title = section.title,
                items = sectionState.destinations.map {
                    GenericSectionItem(it.id, it.destinationName, it.imageUri)
                },
                emptyLabel = section.title,
                compactLandscape = compactLandscape,
                onItemClick = {},
            )
        }

        SectionType.SERVICES -> {
            MediaCarouselSection(
                title = section.title,
                items = sectionState.services.map {
                    GenericSectionItem(it.id, it.serviceTitle, it.imageUri)
                },
                emptyLabel = section.title,
                compactLandscape = compactLandscape,
                onItemClick = {},
            )
        }

        SectionType.WORLD_MAP -> {
            WorldMapSectionViewer(
                section = sectionState.worldMapSection,
                pins = sectionState.worldMapPins,
                sectionTitle = section.title,
                compactLandscape = compactLandscape,
            )
        }

        SectionType.GOOGLE_REVIEWS -> {
            RatingsSectionViewer(
                title = section.title,
                items = sectionState.reviewCards,
                compactLandscape = compactLandscape,
            )
        }

        SectionType.CONTENT_PAGE -> {
            ContentPageSectionViewer(
                title = section.title,
                items = sectionState.contentPageCards,
                compactLandscape = compactLandscape,
            )
        }

        SectionType.ART_GALLERY -> {
            ArtGallerySectionViewer(
                title = section.title,
                groups = sectionState.artGalleryGroups,
                compactLandscape = compactLandscape,
            )
        }

        else -> EmptySectionState(section.title)
    }
}

@Composable
private fun ImageGallerySection(
    title: String,
    items: List<FeaturedProject>,
    allProjectsUrl: String,
    compactLandscape: Boolean,
    onProjectClick: (String) -> Unit,
    onAllProjectsClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 18.dp else 24.dp),
    ) {
        MediaCarouselSection(
            title = title,
            items = items.map { GenericSectionItem(it.id, it.projectName, it.thumbnailUri) },
            emptyLabel = title,
            compactLandscape = compactLandscape,
            onItemClick = onProjectClick,
        )
        if (allProjectsUrl.isNotBlank()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ShowcasePrimaryActionButton(
                    label = "View All Items",
                    onClick = { onAllProjectsClick(allProjectsUrl) },
                )
            }
        }
    }
}

@Composable
private fun MediaCarouselSection(
    title: String,
    items: List<GenericSectionItem>,
    emptyLabel: String,
    compactLandscape: Boolean,
    onItemClick: (String) -> Unit,
) {
    if (items.isEmpty()) {
        EmptySectionState(emptyLabel)
        return
    }

    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    val config = LocalScreenConfig.current
    val isExpanded = config.widthClass == WindowWidthSizeClass.Expanded

    val cardWidth = when {
        isExpanded && adaptive.widthDp >= 1400 -> 340.dp
        compactLandscape -> 220.dp
        else -> 286.dp
    }
    val imageHeight = when {
        isExpanded -> adaptive.cardHeightMedium
        compactLandscape -> adaptive.cardHeightSmall
        else -> adaptive.cardHeightMedium
    }
    val carouselHeight = imageHeight + tokens.touchTarget + adaptive.heroSpacing * 1.6f

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.size },
        modifier = Modifier
            .fillMaxWidth()
            .height(carouselHeight),
        preferredItemWidth = cardWidth,
        itemSpacing = adaptive.gutter,
        contentPadding = PaddingValues(
            horizontal = adaptive.contentPaddingHorizontal + adaptive.heroSpacing,
            vertical = adaptive.heroSpacing * 0.5f,
        ),
    ) { index ->
        val item = items[index]
        CompanyPosterCard(
            item = item,
            imageHeight = imageHeight,
            onClick = { onItemClick(item.id) },
            modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
        )
    }
}

@Composable
private fun EmptySectionState(
    label: String,
) {
    val adaptive = LocalAdaptiveProfile.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(adaptive.previewFrameHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No $label added yet.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private data class GenericSectionItem(
    val id: String,
    val title: String,
    val imageUri: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyPosterCard(
    item: GenericSectionItem,
    imageHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val adaptive = LocalAdaptiveProfile.current
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    ElevatedCard(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .expressivePressScale(interactionSource, pressedScale = 0.985f)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(adaptive.heroSpacing * 0.75f),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (item.imageUri.isBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Map, contentDescription = null)
                    }
                } else {
                    OptimizedAsyncImage(
                        model = item.imageUri,
                        contentDescription = item.title,
                        maxWidth = 480.dp,
                        maxHeight = 280.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptive.heroSpacing, vertical = adaptive.heroSpacing * 0.25f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f),
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.padding(horizontal = adaptive.gutter, vertical = adaptive.heroSpacing),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
