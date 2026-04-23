package io.gelio.app.features.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.gelio.app.R
import io.gelio.app.data.model.ReviewCard
import io.gelio.app.data.model.ReviewSource

private val ReviewCardWhite = Color(0xFFFFFFFF)
private val ReviewCardText = Color(0xFF203040)
private val ReviewCardMuted = Color(0xFF7A889A)
private val ReviewCardBorder = Color(0xFFDDE3EB)
private val ReviewStarTint = Color(0xFFFFB400)

@Composable
fun RatingsSectionViewer(
    title: String,
    items: List<ReviewCard>,
    compactLandscape: Boolean,
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactLandscape) 300.dp else 380.dp),
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

    val visibleItems = remember(items) { items.filterNot { it.hidden } }
    if (visibleItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactLandscape) 300.dp else 380.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No visible reviews yet.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    var expandedReview by remember(visibleItems) { mutableStateOf<ReviewCard?>(null) }

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { visibleItems.size },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compactLandscape) 298.dp else 376.dp),
        preferredItemWidth = if (compactLandscape) 248.dp else 296.dp,
        itemSpacing = if (compactLandscape) 16.dp else 20.dp,
        contentPadding = PaddingValues(
            horizontal = if (compactLandscape) 18.dp else 30.dp,
            vertical = 6.dp,
        ),
    ) { index ->
        val item = visibleItems[index]
        val staggerTop = if (index % 2 == 0) 6.dp else 26.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = staggerTop, bottom = 6.dp),
        ) {
            ReviewCardSurface(
                item = item,
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge),
                expanded = false,
                onClick = { expandedReview = item },
            )
        }
    }

    expandedReview?.let { item ->
        ReviewDetailDialog(
            item = item,
            onDismiss = { expandedReview = null },
        )
    }
}

@Composable
fun ReviewCardSurface(
    item: ReviewCard,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = ReviewCardWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (expanded) 10.dp else 5.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ReviewCardWhite)
                .padding(horizontal = if (expanded) 22.dp else 14.dp, vertical = if (expanded) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (expanded) 14.dp else 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ReviewAvatar(
                        name = item.reviewerName,
                        size = if (expanded) 42.dp else 32.dp,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = item.reviewerName,
                            style = if (expanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ReviewCardText,
                            maxLines = if (expanded) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.subHeading.isNotBlank()) {
                            Text(
                                text = item.subHeading,
                                style = if (expanded) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
                                color = ReviewCardMuted,
                                maxLines = if (expanded) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                ReviewSourceBadge(
                    source = item.sourceType,
                    compact = !expanded,
                )
            }

            ReviewStarsRow(
                rating = item.rating,
                iconSize = if (expanded) 16.dp else 12.dp,
                spacing = if (expanded) 4.dp else 2.dp,
            )

            Text(
                text = item.comment,
                style = if (expanded) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                color = ReviewCardText,
                maxLines = if (expanded) Int.MAX_VALUE else 8,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReviewDetailDialog(
    item: ReviewCard,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .padding(horizontal = 36.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
            ) {
                val dialogMaxHeight = maxHeight
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.elevatedCardColors(containerColor = ReviewCardWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = dialogMaxHeight * 0.86f)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close review",
                                    tint = ReviewCardMuted,
                                )
                            }
                        }

                        ReviewCardSurface(
                            item = item,
                            expanded = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp,
) {
    val palette = listOf(
        Color(0xFFD8E7FF),
        Color(0xFFFFE0D6),
        Color(0xFFE3F6E3),
        Color(0xFFF0E1FF),
        Color(0xFFFFE1EE),
        Color(0xFFE0F4F7),
    )
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "R"
    val background = remember(name) {
        palette[(name.hashCode().absoluteValue % palette.size)]
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = if (size > 36.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ReviewCardText,
        )
    }
}

@Composable
private fun ReviewStarsRow(
    rating: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(rating.coerceIn(1, 5)) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = ReviewStarTint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun ReviewSourceBadge(
    source: ReviewSource,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = if (compact) CircleShape else MaterialTheme.shapes.large,
        color = if (compact) Color.Transparent else Color(0xFFF6F8FB),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 0.dp else 8.dp,
                vertical = if (compact) 0.dp else 6.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReviewSourceLogo(
                source = source,
                size = if (compact) 18.dp else 20.dp,
            )
            if (!compact && source == ReviewSource.GENERIC) {
                Text(
                    text = source.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ReviewCardMuted,
                )
            }
        }
    }
}

@Composable
private fun ReviewSourceLogo(
    source: ReviewSource,
    size: androidx.compose.ui.unit.Dp,
) {
    val drawable = when (source) {
        ReviewSource.GOOGLE -> R.drawable.review_source_google
        ReviewSource.TRIPADVISOR -> R.drawable.review_source_tripadvisor
        ReviewSource.FACEBOOK -> R.drawable.review_source_facebook
        ReviewSource.GENERIC -> null
    }
    if (drawable != null) {
        Icon(
            painter = painterResource(drawable),
            contentDescription = source.label,
            tint = Color.Unspecified,
            modifier = Modifier.size(size),
        )
    } else {
        Surface(
            shape = CircleShape,
            color = Color(0xFFF0F3F7),
        ) {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ThumbUp,
                    contentDescription = source.label,
                    tint = ReviewCardMuted,
                    modifier = Modifier.size(size * 0.62f),
                )
            }
        }
    }
}

private val Int.absoluteValue: Int
    get() = if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this)
