package io.gelio.app.media.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.gelio.app.core.ui.GalleryLightbox
import io.gelio.app.core.ui.GalleryPreviewImage
import io.gelio.app.core.ui.GalleryPreviewViewport
import io.gelio.app.core.ui.GearLinkedThumbnailCard
import io.gelio.app.core.ui.LiveLinkedGalleryPreview
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.ui.rememberLinkedGalleryScrubberState
import io.gelio.app.data.model.FeaturedProject

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProjectGalleryScreen(
    project: FeaturedProject?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    ShowcaseBackground {
        if (project == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Project is not available.",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            return@ShowcaseBackground
        }

        var lightboxIndex by rememberSaveable(project.id) { mutableStateOf<Int?>(null) }
        val pagerState = rememberPagerState(pageCount = { project.galleryImages.size })
        val thumbnailCarouselState = rememberCarouselState { project.galleryImages.count() }
        val linkedScrubberState = rememberLinkedGalleryScrubberState(
            pagerState = pagerState,
            carouselState = thumbnailCarouselState,
            itemCount = project.galleryImages.size,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ViewerTopBar(
                    title = project.projectName,
                    subtitle = project.galleryImages.size.let { "$it views" },
                    onBack = onBack,
                    onHome = onHome,
                    onClose = onClose,
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                ) {
                    GalleryPreviewViewport(modifier = Modifier.fillMaxSize()) {
                        if (linkedScrubberState.isCarouselDragging) {
                            LiveLinkedGalleryPreview(
                                images = project.galleryImages,
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
                                        model = project.galleryImages[page],
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
                    val imageUri = project.galleryImages[index]
                    GearLinkedThumbnailCard(
                        imageUri = imageUri,
                        selected = index == linkedScrubberState.selectedIndex,
                        onClick = { linkedScrubberState.onThumbnailTap(index) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 2.dp)
                            .maskClip(MaterialTheme.shapes.extraLarge),
                    )
                }
            }

            lightboxIndex?.let { selectedIndex ->
                GalleryLightbox(
                    images = project.galleryImages,
                    initialIndex = selectedIndex,
                    title = project.projectName,
                    onDismiss = { lightboxIndex = null },
                )
            }
        }
    }
}
