@file:OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.gelio.app.features.brochures

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.core.theme.LocalNavAnimatedVisibilityScope
import io.gelio.app.core.theme.LocalSharedTransitionScope
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.data.model.Brochure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrochureCarousel(
    brochures: List<Brochure>,
    onBrochureClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (brochures.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(320.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 720.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = "No brochures added yet.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 48.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    BoxWithConstraints(modifier = modifier.widthIn(max = 1260.dp)) {
        val compactLandscape = maxHeight < 360.dp || maxWidth < 1000.dp
        val carouselHeight = if (compactLandscape) 218.dp else 332.dp
        val cardHeight = if (compactLandscape) 204.dp else 316.dp
        val preferredItemWidth = if (compactLandscape) 156.dp else 214.dp

        Column(
            verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 10.dp else 16.dp),
        ) {
        Text(
            text = "Brochure Library",
            modifier = Modifier.padding(start = 24.dp),
            style = if (compactLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { brochures.count() },
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight),
            preferredItemWidth = preferredItemWidth,
            itemSpacing = if (compactLandscape) 10.dp else 14.dp,
            contentPadding = PaddingValues(horizontal = if (compactLandscape) 12.dp else 24.dp),
        ) { index ->
            val brochure = brochures[index]
            val interactionSource = remember(brochure.id) { MutableInteractionSource() }
            val sharedTransitionScope = LocalSharedTransitionScope.current
            val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
            val sharedBoundsSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
            val cardModifier = Modifier
                .height(cardHeight)
                .fillMaxWidth()
                .expressivePressScale(interactionSource, pressedScale = 0.97f)
                .maskClip(MaterialTheme.shapes.extraLarge)
                .let { baseModifier ->
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            baseModifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "brochure-cover-${brochure.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> sharedBoundsSpec },
                            )
                        }
                    } else {
                        baseModifier
                    }
                }

            Surface(
                onClick = { onBrochureClick(brochure.id) },
                modifier = cardModifier,
                interactionSource = interactionSource,
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OptimizedAsyncImage(
                        model = brochure.coverThumbnailUri,
                        contentDescription = brochure.title,
                        maxWidth = 360.dp,
                        maxHeight = 520.dp,
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
                                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                    ),
                                ),
                            ),
                    )
                    Text(
                        text = brochure.title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(if (compactLandscape) 12.dp else 18.dp)
                            .let { textModifier ->
                                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        textModifier.sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "brochure-title-${brochure.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            enter = fadeIn(),
                                            exit = fadeOut(),
                                            boundsTransform = { _, _ -> sharedBoundsSpec },
                                            resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.scaleToBounds(),
                                        )
                                    }
                                } else {
                                    textModifier
                                }
                            },
                        style = if (compactLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    }
}
