@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.gelio.app.features.contentpage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.data.model.ContentPageCard

@Composable
fun ContentPageSectionViewer(
    title: String,
    items: List<ContentPageCard>,
    compactLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    val visibleItems = remember(items) { items.filterNot { it.hidden } }
    if (visibleItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(if (compactLandscape) 320.dp else 380.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No $title added yet.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        val columns = when {
            maxWidth >= 1500.dp -> 3
            maxWidth >= 980.dp -> 2
            else -> 1
        }
        val gutter = if (compactLandscape) 18.dp else 28.dp
        val cardWidth = when (columns) {
            3 -> ((maxWidth - gutter * 2) / 3)
            2 -> ((maxWidth - gutter) / 2)
            else -> maxWidth
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(gutter),
            verticalArrangement = Arrangement.spacedBy(gutter),
        ) {
            visibleItems.forEachIndexed { index, item ->
                val staggerPadding = when {
                    columns == 1 -> 0.dp
                    index % columns == 1 -> if (compactLandscape) 18.dp else 28.dp
                    index % columns == 2 -> if (compactLandscape) 8.dp else 14.dp
                    else -> 0.dp
                }
                ContentPageCardSurface(
                    item = item,
                    modifier = Modifier
                        .width(cardWidth)
                        .padding(top = staggerPadding),
                    compactLandscape = compactLandscape,
                )
            }
        }
    }
}

@Composable
private fun ContentPageCardSurface(
    item: ContentPageCard,
    modifier: Modifier,
    compactLandscape: Boolean,
) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 14.dp else 18.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compactLandscape) 190.dp else 230.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                OptimizedAsyncImage(
                    model = item.imagePath,
                    contentDescription = item.title,
                    maxWidth = 720.dp,
                    maxHeight = 420.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.extraLarge),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = item.title,
                    style = if (compactLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
