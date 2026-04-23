@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.artgallery

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.data.model.ArtGalleryCard
import io.gelio.app.data.model.ArtGalleryHeroGroup
import io.gelio.app.core.ui.OptimizedAsyncImage

private val ArtGalleryStage = Color(0xFF1C110C)
private val ArtGallerySurface = Color(0xFF291D17)
private val ArtGallerySurfaceRaised = Color(0xFF40322C)
private val ArtGalleryAccent = Color(0xFFF58565)
private val ArtGalleryHeadline = Color(0xFFFFD2C5)
private val ArtGalleryBody = Color(0xFFE5C9BF)
private val ArtGalleryImagePlate = Color(0xFF36251E)

@Composable
fun ArtGallerySectionViewer(
    title: String,
    groups: List<ArtGalleryHeroGroup>,
    compactLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    val visibleGroups = remember(groups) {
        groups.filterNot { it.hero.hidden }.map { group ->
            group.copy(cards = group.cards.filterNot { it.hidden })
        }.filter { it.cards.isNotEmpty() || it.hero.title.isNotBlank() || it.hero.description.isNotBlank() }
    }

    if (visibleGroups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(if (compactLandscape) 360.dp else 420.dp)
                .background(ArtGalleryStage),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No $title added yet.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val horizontalPadding = if (compactLandscape) 18.dp else 32.dp
    val verticalGap = if (compactLandscape) 30.dp else 44.dp

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(ArtGalleryStage),
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = if (compactLandscape) 10.dp else 18.dp,
            bottom = if (compactLandscape) 28.dp else 42.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
    ) {
        itemsIndexed(
            items = visibleGroups,
            key = { _, group -> group.hero.id },
        ) { index, group ->
            ArtGalleryHeroGroupBlock(
                group = group,
                heroIndex = index,
                compactLandscape = compactLandscape,
            )
        }
    }
}

@Composable
private fun ArtGalleryHeroGroupBlock(
    group: ArtGalleryHeroGroup,
    heroIndex: Int,
    compactLandscape: Boolean,
) {
    val alignment = when (heroIndex % 3) {
        1 -> Alignment.CenterHorizontally
        2 -> Alignment.End
        else -> Alignment.Start
    }
    val headlineWidth = when {
        compactLandscape -> 520.dp
        heroIndex % 3 == 1 -> 620.dp
        else -> 560.dp
    }
    val topSpacing = when (heroIndex % 3) {
        1 -> if (compactLandscape) 8.dp else 14.dp
        2 -> if (compactLandscape) 2.dp else 8.dp
        else -> 0.dp
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 18.dp else 26.dp),
        horizontalAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topSpacing)
                .width(headlineWidth),
            verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 10.dp else 12.dp),
            horizontalAlignment = alignment,
        ) {
            Text(
                text = group.hero.title,
                style = if (compactLandscape) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.W600,
                color = ArtGalleryHeadline,
                textAlign = when (alignment) {
                    Alignment.End -> TextAlign.End
                    Alignment.CenterHorizontally -> TextAlign.Center
                    else -> TextAlign.Start
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (group.hero.description.isNotBlank()) {
                Text(
                    text = group.hero.description,
                    style = if (compactLandscape) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = ArtGalleryBody.copy(alpha = 0.88f),
                    textAlign = when (alignment) {
                        Alignment.End -> TextAlign.End
                        Alignment.CenterHorizontally -> TextAlign.Center
                        else -> TextAlign.Start
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compactLandscape) 18.dp else 24.dp),
            contentPadding = PaddingValues(horizontal = if (compactLandscape) 2.dp else 6.dp),
        ) {
            itemsIndexed(
                items = group.cards,
                key = { _, card -> card.id },
            ) { cardIndex, card ->
                ArtGalleryEditorialCard(
                    card = card,
                    heroIndex = heroIndex,
                    cardIndex = cardIndex,
                    compactLandscape = compactLandscape,
                )
            }
        }
    }
}

@Composable
private fun ArtGalleryEditorialCard(
    card: ArtGalleryCard,
    heroIndex: Int,
    cardIndex: Int,
    compactLandscape: Boolean,
) {
    val width = when ((heroIndex + cardIndex) % 4) {
        0 -> if (compactLandscape) 300.dp else 356.dp
        1 -> if (compactLandscape) 248.dp else 304.dp
        2 -> if (compactLandscape) 336.dp else 398.dp
        else -> if (compactLandscape) 272.dp else 332.dp
    }
    val imageHeight = when ((heroIndex + cardIndex) % 3) {
        0 -> if (compactLandscape) 208.dp else 252.dp
        1 -> if (compactLandscape) 172.dp else 214.dp
        else -> if (compactLandscape) 228.dp else 278.dp
    }

    ElevatedCard(
        modifier = Modifier.width(width),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = ArtGallerySurface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 14.dp else 18.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = ArtGalleryImagePlate,
            ) {
                if (card.imagePath.isBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Image",
                            style = MaterialTheme.typography.labelLarge,
                            color = ArtGalleryBody.copy(alpha = 0.7f),
                        )
                    }
                } else {
                    OptimizedAsyncImage(
                        model = card.imagePath,
                        contentDescription = card.title,
                        maxWidth = 900.dp,
                        maxHeight = 540.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compactLandscape) 18.dp else 22.dp)
                    .padding(bottom = if (compactLandscape) 18.dp else 22.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 8.dp else 10.dp),
            ) {
                Text(
                    text = card.title,
                    style = if (compactLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.W600,
                    color = ArtGalleryHeadline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.bodyText,
                    style = if (compactLandscape) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = ArtGalleryBody.copy(alpha = 0.88f),
                    maxLines = if (compactLandscape) 5 else 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
