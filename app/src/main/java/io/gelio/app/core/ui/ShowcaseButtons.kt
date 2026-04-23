@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.gelio.app.core.theme.LocalShowcaseRadius
import io.gelio.app.core.theme.expressivePressScale

@Composable
fun ShowcaseSectionToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = modifier
            .expressivePressScale(interactionSource, pressedScale = 0.96f)
            .defaultMinSize(minHeight = 48.dp),
        interactionSource = interactionSource,
        shapes = ToggleButtonDefaults.shapes(
            shape = CircleShape,
            pressedShape = MaterialTheme.shapes.medium,
            checkedShape = CircleShape,
        ),
        colors = ToggleButtonDefaults.outlinedToggleButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f))
        },
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun ShowcasePrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    ElevatedButton(
        onClick = onClick,
        modifier = modifier
            .expressivePressScale(interactionSource)
            .defaultMinSize(minHeight = 52.dp),
        interactionSource = interactionSource,
        shapes = ButtonDefaults.shapes(
            shape = CircleShape,
            pressedShape = MaterialTheme.shapes.large,
        ),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp,
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun ShowcaseSecondaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .expressivePressScale(interactionSource)
            .defaultMinSize(minHeight = 48.dp),
        interactionSource = interactionSource,
        shapes = ButtonDefaults.shapes(
            shape = CircleShape,
            pressedShape = MaterialTheme.shapes.large,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun ShowcaseTonalIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier
            .expressivePressScale(interactionSource)
            .size(64.dp),
        shapes = IconButtonDefaults.shapes(
            shape = MaterialTheme.shapes.large,
            pressedShape = MaterialTheme.shapes.medium,
        ),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun ShowcaseHomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier
            .expressivePressScale(interactionSource)
            .size(64.dp),
        shapes = IconButtonDefaults.shapes(
            shape = MaterialTheme.shapes.large,
            pressedShape = MaterialTheme.shapes.medium,
        ),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = Icons.Rounded.Home,
            contentDescription = "Home",
        )
    }
}

@Composable
fun RowScope.ShowcaseSectionToggleButtonWeighted(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ShowcaseSectionToggleButton(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.weight(1f, fill = false),
    )
}
