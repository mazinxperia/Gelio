package io.gelio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GelioLogoMark(
    modifier: Modifier = Modifier,
    highlight: Color = MaterialTheme.colorScheme.primary,
    largeText: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (largeText) 28.dp else 16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        highlight.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "G",
            style = if (largeText) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.W700,
        )
    }
}

@Composable
fun GelioLogoLockup(
    modifier: Modifier = Modifier,
    title: String = "Gelio",
    subtitle: String? = null,
    vertical: Boolean = false,
    highlight: Color = MaterialTheme.colorScheme.primary,
) {
    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
            ) {
                GelioLogoMark(
                    modifier = Modifier
                        .padding(14.dp)
                        .size(116.dp),
                    highlight = highlight,
                    largeText = true
                )
            }
            LogoTextBlock(title = title, subtitle = subtitle, centered = true, largeType = true)
        }
    } else {
        Row(
            modifier = modifier.defaultMinSize(minHeight = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GelioLogoMark(
                modifier = Modifier.size(56.dp),
                highlight = highlight,
                largeText = false
            )
            LogoTextBlock(title = title, subtitle = subtitle, centered = false, largeType = false)
        }
    }
}

@Composable
private fun LogoTextBlock(
    title: String,
    subtitle: String?,
    centered: Boolean,
    largeType: Boolean
) {
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = if (largeType) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = if (largeType) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
