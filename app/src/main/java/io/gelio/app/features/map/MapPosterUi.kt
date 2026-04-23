package io.gelio.app.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

data class WorldMapPosterPalette(
    val background: Color,
    val worldPlane: Color,
    val countryFill: Color,
    val countryStroke: Color,
    val highlightedFill: Color,
    val highlightedStroke: Color,
    val pinOuter: Color,
    val pinInner: Color,
    val pinSelectedRing: Color,
    val title: Color,
    val subtitle: Color,
    val placeholder: Color,
)

@Composable
fun SavedMapPreviewImage(
    previewFile: File,
    countryLabel: String,
    cityLabel: String,
    compactLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val request = remember(previewFile.absolutePath, previewFile.lastModified()) {
        ImageRequest.Builder(context)
            .data(previewFile)
            .memoryCacheKey("${previewFile.absolutePath}|${previewFile.lastModified()}")
            .diskCacheKey("${previewFile.absolutePath}|${previewFile.lastModified()}")
            .crossfade(false)
            .allowHardware(true)
            .build()
    }
    val palette = rememberWorldMapPosterPalette()
    MapPosterScaffold(
        countryLabel = countryLabel,
        cityLabel = cityLabel,
        compactLandscape = compactLandscape,
        palette = palette,
        showPosterChromeOverlay = false,
        modifier = modifier,
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            alignment = Alignment.Center,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
    }
}

@Composable
fun MapPosterPlaceholder(
    countryLabel: String,
    cityLabel: String,
    message: String,
    compactLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = rememberWorldMapPosterPalette()
    MapPosterScaffold(
        countryLabel = countryLabel,
        cityLabel = cityLabel,
        compactLandscape = compactLandscape,
        palette = palette,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (compactLandscape) 64.dp else 78.dp)
                    .clip(CircleShape)
                    .background(palette.pinOuter.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = null,
                    tint = palette.pinOuter,
                    modifier = Modifier.size(if (compactLandscape) 28.dp else 34.dp),
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = palette.placeholder,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MapPosterScaffold(
    countryLabel: String,
    cityLabel: String,
    compactLandscape: Boolean,
    palette: WorldMapPosterPalette,
    modifier: Modifier = Modifier,
    showPosterChromeOverlay: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.background),
    ) {
        content()
        if (showPosterChromeOverlay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .worldMapViewfinderMaskOverlay(palette.background),
            )
            MapPosterTitle(
                countryLabel = countryLabel,
                cityLabel = cityLabel,
                compactLandscape = compactLandscape,
                palette = palette,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = if (compactLandscape) 24.dp else 30.dp,
                        start = 24.dp,
                        end = 24.dp,
                    ),
            )
        }
    }
}

@Composable
private fun MapPosterTitle(
    countryLabel: String,
    cityLabel: String,
    compactLandscape: Boolean,
    palette: WorldMapPosterPalette,
    modifier: Modifier = Modifier,
) {
    val resolvedCountry = countryLabel.trim().ifBlank { "MAP" }.uppercase()
    val resolvedCity = cityLabel.trim().uppercase()
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 6.dp else 8.dp),
    ) {
        Text(
            text = resolvedCountry,
            color = palette.title,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = if (compactLandscape) 6.sp else 9.sp,
                fontSize = if (compactLandscape) 26.sp else 38.sp,
                lineHeight = if (compactLandscape) 30.sp else 42.sp,
            ),
        )
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth(if (compactLandscape) 0.24f else 0.18f)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            palette.title.copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        if (resolvedCity.isNotBlank()) {
            Text(
                text = resolvedCity,
                color = palette.subtitle,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = if (compactLandscape) 1.4.sp else 2.2.sp,
                    fontSize = if (compactLandscape) 11.sp else 14.sp,
                ),
            )
        }
    }
}

fun Modifier.worldMapViewfinderMaskOverlay(backgroundColor: Color): Modifier =
    drawWithCache {
        val edgeWidth = size.width * 0.18f
        val topFadeHeight = size.height * 0.14f
        val bottomFadeHeight = size.height * 0.34f
        val cornerRadius = size.minDimension * 0.34f
        val strongBackground = backgroundColor.copy(alpha = 0.96f)
        val mediumBackground = backgroundColor.copy(alpha = 0.62f)
        val lightBackground = backgroundColor.copy(alpha = 0.24f)

        onDrawBehind {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(strongBackground, mediumBackground, Color.Transparent),
                    startX = 0f,
                    endX = edgeWidth,
                ),
                size = Size(edgeWidth, size.height),
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, mediumBackground, strongBackground),
                    startX = size.width - edgeWidth,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - edgeWidth, 0f),
                size = Size(edgeWidth, size.height),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(strongBackground, lightBackground, Color.Transparent),
                    startY = 0f,
                    endY = topFadeHeight,
                ),
                size = Size(size.width, topFadeHeight),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, lightBackground, mediumBackground, strongBackground),
                    startY = size.height - bottomFadeHeight,
                    endY = size.height,
                ),
                topLeft = Offset(0f, size.height - bottomFadeHeight),
                size = Size(size.width, bottomFadeHeight),
            )
            listOf(
                Offset(0f, 0f),
                Offset(size.width, 0f),
                Offset(0f, size.height),
                Offset(size.width, size.height),
            ).forEach { center ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(strongBackground, mediumBackground, Color.Transparent),
                        center = center,
                        radius = cornerRadius,
                    ),
                    radius = cornerRadius,
                    center = center,
                )
            }
        }
    }

@Composable
fun rememberWorldMapPosterPalette(): WorldMapPosterPalette =
    remember {
        WorldMapPosterPalette(
            background = Color(0xFF061020),
            worldPlane = Color(0xFF061020),
            countryFill = Color(0xFF0F2235),
            countryStroke = Color(0xFFBF9637),
            highlightedFill = Color(0x66D5A84B),
            highlightedStroke = Color(0xFFDDB45C),
            pinOuter = Color(0xFFDDB45C),
            pinInner = Color(0xFF061020),
            pinSelectedRing = Color(0x55F7D98F),
            title = Color(0xFFE0B457),
            subtitle = Color(0xFFF0CF7B),
            placeholder = Color(0xFF9EB0C9),
        )
    }
