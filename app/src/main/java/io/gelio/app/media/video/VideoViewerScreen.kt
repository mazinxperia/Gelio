@file:OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.gelio.app.media.video

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.core.theme.LocalNavAnimatedVisibilityScope
import io.gelio.app.core.theme.LocalSharedTransitionScope
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.util.youtubeEmbedUrl
import io.gelio.app.data.model.ShowcaseVideo
import io.gelio.app.media.webview.ShowcaseWebFrame

@Composable
fun VideoViewerScreen(
    video: ShowcaseVideo?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val embedUrl = remember(video?.youtubeLink) {
        youtubeEmbedUrl(video?.youtubeLink.orEmpty())
    }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val sharedBoundsSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()

    ShowcaseBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 42.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ViewerTopBar(
                title = video?.title ?: "Video",
                subtitle = "YouTube player",
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (video == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Video not found.",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    return@BoxWithConstraints
                }

                val playerWidth = (maxWidth * 0.54f).coerceAtLeast(520.dp).coerceAtMost(900.dp)
                val playerHeight = playerWidth * (9f / 16f)
                val infoWidth = (maxWidth * 0.32f).coerceAtLeast(300.dp).coerceAtMost(440.dp)

                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 1480.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(42.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(infoWidth)
                                .height(playerHeight),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 34.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Text(
                                            text = "YouTube Video",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
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
                                                        resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.scaleToBounds(),
                                                    )
                                                }
                                            } else {
                                                baseModifier
                                            }
                                        },
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(190.dp)
                                            .height(3.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                MaterialTheme.shapes.extraSmall,
                                            ),
                                    )
                                    Text(
                                        text = video.description.ifBlank {
                                            "A focused project film prepared for client presentation."
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 6,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Text(
                                        text = "Use the player on the right",
                                        modifier = Modifier.padding(start = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(width = playerWidth, height = playerHeight),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = Color.Black,
                            tonalElevation = 0.dp,
                        ) {
                            ShowcaseWebFrame(
                                url = embedUrl,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}
