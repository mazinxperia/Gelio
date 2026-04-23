package io.gelio.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalScreenConfig
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.gelio.app.core.theme.LocalShowcaseRadius

data class RailItemSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun TabletBrandShell(
    brandTitle: String,
    brandSubtitle: String,
    railItems: List<RailItemSpec>,
    selectedKey: String,
    onItemSelected: (String) -> Unit,
    onBrandsClick: () -> Unit,
    onWelcomeClick: () -> Unit,
    heroContent: @Composable () -> Unit,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    val config = LocalScreenConfig.current
    val adaptive = LocalAdaptiveProfile.current
    val isExpanded = config.widthClass == WindowWidthSizeClass.Expanded
    val tokens = io.gelio.app.app.LocalLayoutTokens.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val outerPadding = adaptive.outerMargin
        val internalSpacing = tokens.panelGap
        val railWidth = tokens.railWidth
        val railInnerPadding = adaptive.heroSpacing

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = outerPadding, vertical = adaptive.contentPaddingVertical),
            horizontalArrangement = Arrangement.spacedBy(internalSpacing),
        ) {
            Surface(
                modifier = Modifier
                    .width(railWidth)
                    .fillMaxHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(railInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                    ) {
                        GelioLogoLockup(
                            modifier = Modifier.padding(18.dp),
                            title = brandTitle,
                            subtitle = brandSubtitle,
                            highlight = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    railItems.forEach { item ->
                        RailItem(
                            item = item,
                            selected = item.key == selectedKey,
                            onClick = { onItemSelected(item.key) },
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    UtilityButton(
                        label = "Home",
                        onClick = onWelcomeClick,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                ShowcaseCenteredContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = adaptive.contentPaddingHorizontal, vertical = adaptive.contentPaddingVertical),
                    maxWidth = tokens.contentMaxWidth,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(adaptive.sectionSpacing),
                    ) {
                        heroContent()
                        Box(modifier = Modifier.fillMaxSize()) {
                            content(PaddingValues(bottom = adaptive.heroSpacing))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RailItem(
    item: RailItemSpec,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f)
        },
        label = "rail_item_container",
    )

    val contentColor = animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "rail_item_content",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = io.gelio.app.app.LocalLayoutTokens.current.touchTarget)
            .bounceClickable(interactionSource = interactionSource, onClick = onClick),
        shape = CircleShape,
        color = containerColor.value,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = contentColor.value,
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor.value,
            )
        }
    }
}

@Composable
private fun UtilityButton(
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier.bounceClickable(interactionSource = interactionSource, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
