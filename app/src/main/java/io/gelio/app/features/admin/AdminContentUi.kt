@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package io.gelio.app.features.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.OptimizedAsyncImage
import kotlin.math.roundToInt
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import kotlinx.coroutines.delay
import io.gelio.app.app.LocalLayoutTokens

data class AdminImportProgress(
    val completed: Int,
    val total: Int,
) {
    val fraction: Float
        get() = if (total <= 0) {
            0f
        } else {
            (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }

    val percent: Int
        get() = (fraction * 100f).roundToInt().coerceIn(0, 100)

    val label: String
        get() = when {
            total == 1 -> "$completed / $total file"
            else -> "$completed / $total files"
        }
}

data class AdminFabMenuAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun AdminStaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    delayStep: Long = 45L,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * delayStep)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()) +
                slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
                ) +
                scaleIn(
                    initialScale = 0.95f,
                    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
                ),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun AdminLibraryHeader(
    title: String,
    subtitle: String,
    count: Int,
    itemLabel: String,
) {
    val tokens = LocalLayoutTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Text(
                text = "$count $itemLabel",
                modifier = Modifier.padding(horizontal = tokens.gutter, vertical = tokens.headerPadding),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AdminItemActions(
    index: Int,
    total: Int,
    hidden: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onMoveUp, enabled = index > 0, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
        }
        FilledTonalIconButton(onClick = onMoveDown, enabled = index < total - 1, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
        }
        FilledTonalIconButton(onClick = onToggleVisibility, shapes = IconButtonDefaults.shapes()) {
            Icon(
                imageVector = if (hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                contentDescription = "Toggle visibility",
            )
        }
        FilledTonalIconButton(onClick = onDelete, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun AdminDialogHeader(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Close, contentDescription = "Close editor")
        }
    }
}

@Composable
fun AdminDialogActions(
    saveLabel: String,
    saveEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismiss, enabled = !isLoading) {
            Text("Cancel")
        }
        val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        Button(
            onClick = onSave,
            enabled = saveEnabled && !isLoading,
            modifier = Modifier.padding(start = 12.dp),
        ) {
            AnimatedContent(
                targetState = isLoading,
                label = "admin_dialog_save_morph",
                transitionSpec = {
                    (fadeIn(animationSpec = effectsSpec) + 
                     scaleIn(initialScale = 0.8f, animationSpec = effectsSpec))
                    .togetherWith(
                        fadeOut(animationSpec = effectsSpec) + 
                        scaleOut(targetScale = 0.8f, animationSpec = effectsSpec)
                    )
                }
            ) { loading ->
                if (loading) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(saveLabel)
                }
            }
        }
    }
}

@Composable
fun AdminPreviewPanel(
    title: String,
    note: String,
    imageUri: String,
    emptyLabel: String,
    onDelete: (() -> Unit)?,
    deleteLabel: String,
    width: Dp? = null,
) {
    val tokens = LocalLayoutTokens.current
    Surface(
        modifier = Modifier
            .width(width ?: tokens.adminPreviewWidth)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.cardHeight),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (imageUri.isNotBlank()) {
                    OptimizedAsyncImage(
                        model = imageUri,
                        contentDescription = title,
                        maxWidth = 520.dp,
                        maxHeight = 320.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emptyLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (onDelete != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Text(
                        text = deleteLabel,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AdminEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(84.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun AdminAddFab(
    isAdding: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isAdding) 45f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "admin_add_fab_rotation",
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.largeIncreased,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
fun AdminExpressiveFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    actions: List<AdminFabMenuAction>,
    modifier: Modifier = Modifier,
) {
    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier.semantics {
                    traversalIndex = -1f
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                    contentDescription = "Toggle menu"
                },
                checked = expanded,
                onCheckedChange = { onToggle() },
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        actions.forEach { action ->
            FloatingActionButtonMenuItem(
                onClick = {
                    action.onClick()
                    onToggle()
                },
                icon = { Icon(action.icon, contentDescription = null) },
                text = { Text(text = action.label) },
            )
        }
    }
}

@Composable
fun AdminImportingOverlay(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    progress: AdminImportProgress? = null,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 12.dp,
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 360.dp, max = LocalLayoutTokens.current.dialogMaxWidth * 0.5f)
                        .padding(horizontal = 26.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ContainedLoadingIndicator(modifier = Modifier.size(42.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            progress?.let {
                                Text(
                                    text = "${it.percent}% - ${it.label}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    progress?.let {
                        val animatedProgress by animateFloatAsState(
                            targetValue = it.fraction,
                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            label = "admin_import_progress",
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.adminDialogSize(maxWidth: Dp = 1080.dp): Modifier =
    this.then(
        Modifier
            .fillMaxWidth()
            .padding(LocalLayoutTokens.current.margin * 0.75f)
            .widthIn(max = minOf(maxWidth, LocalLayoutTokens.current.dialogMaxWidth))
            .fillMaxHeight(0.92f)
    )
