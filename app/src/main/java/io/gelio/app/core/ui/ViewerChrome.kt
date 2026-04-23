@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens

@Composable
fun ViewerTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.touchTarget + adaptive.heroSpacing)
            .padding(
                horizontal = adaptive.contentPaddingHorizontal,
                vertical = adaptive.heroSpacing * 0.6f,
            ),
    ) {
        val titleSideReserve = when {
            maxWidth < 780.dp -> 88.dp
            maxWidth < 1080.dp -> 116.dp
            else -> 152.dp
        }

        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(tokens.gutter * 0.5f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShowcaseHomeButton(onClick = onHome)
            IconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Text(
            text = subtitle?.let { "$title - $it" } ?: title,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = titleSideReserve),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
