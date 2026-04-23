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

internal sealed interface DesignDetailTarget {
    data class Project(val project: FeaturedProject) : DesignDetailTarget
    data class Tour(val tour: VirtualTour) : DesignDetailTarget
}

internal data class DesignStripItem(
    val id: String,
    val title: String,
    val imageUri: String,
    val source: Any,
)

internal data class DesignSectionBodyState(
    val section: DesignSection,
    val stripPages: List<List<DesignStripItem>>,
    val pageIndex: Int,
    val videos: List<ShowcaseVideo>,
    val brochures: List<Brochure>,
)

internal data class DynamicDesignSectionBodyState(
    val section: ShowcaseSection?,
    val stripItems: List<DesignStripItem>,
    val videos: List<ShowcaseVideo>,
    val brochures: List<Brochure>,
    val destinations: List<Destination>,
    val services: List<Service>,
    val allProjectsLink: String,
    val worldMapSection: io.gelio.app.data.model.WorldMapSection?,
    val worldMapPins: List<io.gelio.app.data.model.WorldMapPin>,
    val reviewCards: List<io.gelio.app.data.model.ReviewCard>,
)
