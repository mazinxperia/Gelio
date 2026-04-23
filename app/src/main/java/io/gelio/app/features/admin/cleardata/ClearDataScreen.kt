@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
)

package io.gelio.app.features.admin.cleardata

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.app.ClearDataLogEntry
import io.gelio.app.app.ClearDataUiState
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.data.cleardata.ClearDataBucket
import io.gelio.app.data.cleardata.ClearDataBucketKey
import io.gelio.app.data.cleardata.ClearDataPhase

@Composable
fun ClearDataScreen(
    uiState: ClearDataUiState,
    onScan: () -> Unit,
    onClearEverything: () -> Unit,
    onClearSession: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    var confirmation by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onClearSession()
        onDispose { onClearSession() }
    }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == ClearDataPhase.Idle || uiState.phase == ClearDataPhase.Complete) {
            confirmation = ""
        }
    }

    ShowcaseBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ViewerTopBar(
                title = "Clear Data",
                subtitle = "Factory reset",
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TopStorageSection(
                    uiState = uiState,
                    modifier = Modifier.widthIn(max = 1_680.dp),
                )
                MorphingDataPanel(
                    uiState = uiState,
                    modifier = Modifier
                        .widthIn(max = 1_680.dp)
                        .weight(1f),
                )
                ResetActionPanel(
                    uiState = uiState,
                    confirmation = confirmation,
                    onConfirmationChange = { confirmation = it },
                    onScan = onScan,
                    onClearEverything = onClearEverything,
                    modifier = Modifier
                        .widthIn(max = 1_680.dp)
                        .heightIn(min = 118.dp),
                )
            }
        }
    }
}

@Composable
private fun TopStorageSection(
    uiState: ClearDataUiState,
    modifier: Modifier = Modifier,
) {
    val buckets = uiState.scanResult?.buckets ?: defaultBuckets()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WarningStrip()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            buckets.forEach { bucket ->
                StorageBucketCard(
                    bucket = bucket,
                    scanned = uiState.scanResult != null,
                    modifier = Modifier.weight(if (bucket.key == ClearDataBucketKey.Total) 1.16f else 1f),
                )
            }
        }
    }
}

@Composable
private fun WarningStrip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.84f),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = MaterialShapes.Burst.toShape(), color = MaterialTheme.colorScheme.error) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                )
            }
            Text(
                text = "This resets only Gelio private data. Exported .kskm files in Downloads and user Gallery/Files data will not be deleted.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StorageBucketCard(
    bucket: ClearDataBucket,
    scanned: Boolean,
    modifier: Modifier = Modifier,
) {
    val icon = when (bucket.key) {
        ClearDataBucketKey.Database -> Icons.Rounded.Storage
        ClearDataBucketKey.Media -> Icons.Rounded.PhotoLibrary
        ClearDataBucketKey.Cache -> Icons.Rounded.Cached
        ClearDataBucketKey.Settings -> Icons.Rounded.Settings
        ClearDataBucketKey.Total -> Icons.Rounded.DeleteSweep
    }
    val shape = when (bucket.key) {
        ClearDataBucketKey.Database -> MaterialShapes.Gem.toShape()
        ClearDataBucketKey.Media -> MaterialShapes.Cookie9Sided.toShape()
        ClearDataBucketKey.Cache -> MaterialShapes.Clover4Leaf.toShape()
        ClearDataBucketKey.Settings -> MaterialShapes.SoftBurst.toShape()
        ClearDataBucketKey.Total -> MaterialShapes.Burst.toShape()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (bucket.key == ClearDataBucketKey.Total) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                        tint = if (bucket.key == ClearDataBucketKey.Total) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (bucket.key == ClearDataBucketKey.Total) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            Column {
                Text(
                    text = if (scanned) bucket.bytes.formatBytes() else "Not scanned",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (bucket.key == ClearDataBucketKey.Total) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = if (scanned) "${bucket.files} private entries" else "Scan required",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (bucket.key == ClearDataBucketKey.Total) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MorphingDataPanel(
    uiState: ClearDataUiState,
    modifier: Modifier = Modifier,
) {
    val panelState = when (uiState.phase) {
        ClearDataPhase.Idle -> DataPanelState.Intro
        ClearDataPhase.Scan -> DataPanelState.Running
        ClearDataPhase.Ready -> DataPanelState.Found
        ClearDataPhase.Clearing -> DataPanelState.Running
        ClearDataPhase.Complete -> DataPanelState.Complete
        ClearDataPhase.Error -> DataPanelState.Error
    }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val defaultFloatSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fastFloatSpatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraExtraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        AnimatedContent(
            targetState = panelState,
            transitionSpec = {
                (
                    slideInVertically(animationSpec = spatial) { it / 5 } +
                        fadeIn(animationSpec = effects) +
                        scaleIn(initialScale = 0.96f, animationSpec = defaultFloatSpatial)
                    ) togetherWith (
                    slideOutVertically(animationSpec = spatial) { -it / 5 } +
                        fadeOut(animationSpec = effects) +
                        scaleOut(targetScale = 0.96f, animationSpec = fastFloatSpatial)
                    ) using SizeTransform(clip = false)
            },
            label = "clearDataPanel",
        ) { state ->
            when (state) {
                DataPanelState.Intro -> IntroPanel()
                DataPanelState.Found -> FoundPanel(uiState)
                DataPanelState.Running -> LogPanel(uiState)
                DataPanelState.Complete -> CompletePanel(uiState)
                DataPanelState.Error -> ErrorPanel(uiState)
            }
        }
    }
}

@Composable
private fun IntroPanel() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(
            icon = Icons.Rounded.Search,
            shapeColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(92.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Scan first, then reset.", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "The scan measures Room rows, app-private media, cache, settings, and database storage. Nothing is deleted until you type RESET and press the final action.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FoundPanel(uiState: ClearDataUiState) {
    val scan = uiState.scanResult
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(
            icon = Icons.Rounded.FolderOff,
            shapeColor = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(84.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Found app-private data", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "${scan?.counts?.totalRows ?: 0} content rows, ${scan?.totalFiles ?: 0} private entries, ${scan?.totalBytes?.formatBytes() ?: "0 B"} measured.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            scan?.counts?.let {
                Text(
                    text = "Projects ${it.featuredProjects} | Tours ${it.virtualTours} | Videos ${it.videos} | Brochures ${it.brochures} | Destinations ${it.destinations} | Services ${it.services} | Links ${it.globalLinks}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LogPreview(logs = uiState.logs.takeLast(5), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LogPanel(uiState: ClearDataUiState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
            ContainedLoadingIndicator(modifier = Modifier.size(74.dp))
        }
        Column(
            modifier = Modifier.weight(0.9f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(uiState.phase.name.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.headlineLarge)
            Text(uiState.progressLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        }
        LogPreview(logs = uiState.logs.takeLast(7), modifier = Modifier.weight(1.1f))
    }
}

@Composable
private fun CompletePanel(uiState: ClearDataUiState) {
    val summary = uiState.summary
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(
            icon = Icons.Rounded.CheckCircle,
            shapeColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Factory reset complete", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Cleared ${summary?.contentRowsCleared ?: 0} content rows and deleted ${summary?.deletedEntries ?: 0} private media/cache entries (${summary?.deletedBytes?.formatBytes() ?: "0 B"}).",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Downloads backups were left untouched. The app is now empty and settings will fall back to defaults.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LogPreview(logs = uiState.logs.takeLast(6), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ErrorPanel(uiState: ClearDataUiState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(
            icon = Icons.Rounded.Error,
            shapeColor = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(92.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Reset stopped", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.error)
            Text(uiState.progressLabel, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LogPreview(logs = uiState.logs.takeLast(7), modifier = Modifier.weight(1.2f))
    }
}

@Composable
private fun LogPreview(
    logs: List<ClearDataLogEntry>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Live log", style = MaterialTheme.typography.titleMedium)
            if (logs.isEmpty()) {
                Text("No current-session log yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                logs.forEach { entry ->
                    LogRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: ClearDataLogEntry) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = when {
                entry.error -> MaterialTheme.colorScheme.errorContainer
                entry.warning -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
        ) {
            Spacer(modifier = Modifier.size(16.dp))
        }
        Text(entry.timestamp, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (entry.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ResetActionPanel(
    uiState: ClearDataUiState,
    confirmation: String,
    onConfirmationChange: (String) -> Unit,
    onScan: () -> Unit,
    onClearEverything: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canClear = uiState.phase == ClearDataPhase.Ready && confirmation == "RESET"
    val action = when (uiState.phase) {
        ClearDataPhase.Idle -> ResetAction.Scan
        ClearDataPhase.Scan, ClearDataPhase.Clearing -> ResetAction.Running
        ClearDataPhase.Ready -> if (canClear) ResetAction.Clear else ResetAction.Confirm
        ClearDataPhase.Complete -> ResetAction.Done
        ClearDataPhase.Error -> ResetAction.ScanAgain
    }
    val surfaceColor by animateFloatAsState(
        targetValue = if (action == ResetAction.Clear) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "dangerBlend",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraExtraLarge,
        color = lerp(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.errorContainer, surfaceColor),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = uiState.phase == ClearDataPhase.Ready) {
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { onConfirmationChange(it.uppercase().take(5)) },
                    modifier = Modifier.width(260.dp),
                    singleLine = true,
                    label = { Text("Type RESET") },
                    enabled = !uiState.running,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = when (action) {
                        ResetAction.Scan -> "Scan before clearing"
                        ResetAction.Confirm -> "Final confirmation required"
                        ResetAction.Clear -> "Ready to factory reset"
                        ResetAction.Running -> uiState.progressLabel
                        ResetAction.Done -> "Factory reset complete"
                        ResetAction.ScanAgain -> "Scan again or retry after fixing the error"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (action == ResetAction.Clear) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (uiState.phase == ClearDataPhase.Ready) {
                        "No automatic backup will be created. Export a .kskm backup first if this data matters."
                    } else {
                        "Only this app's private data is touched. Downloads backups stay outside this reset."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MorphingActionButton(
                action = action,
                progress = uiState.progress,
                onClick = when (action) {
                    ResetAction.Scan, ResetAction.ScanAgain -> onScan
                    ResetAction.Clear -> onClearEverything
                    else -> null
                },
            )
        }
    }
}

@Composable
private fun MorphingActionButton(
    action: ResetAction,
    progress: Float,
    onClick: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val enabled = onClick != null
    val shape = when (action) {
        ResetAction.Done -> MaterialShapes.Clover4Leaf.toShape()
        ResetAction.Running -> MaterialShapes.SoftBurst.toShape()
        ResetAction.Clear -> MaterialShapes.Burst.toShape()
        else -> MaterialTheme.shapes.extraLarge
    }
    val color = when (action) {
        ResetAction.Clear -> MaterialTheme.colorScheme.error
        ResetAction.Done -> MaterialTheme.colorScheme.primary
        ResetAction.Running -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (action) {
        ResetAction.Clear -> MaterialTheme.colorScheme.onError
        ResetAction.Done -> MaterialTheme.colorScheme.onPrimary
        ResetAction.Running -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = Modifier
            .width(if (action == ResetAction.Running) 240.dp else 210.dp)
            .height(70.dp)
            .expressivePressScale(interactionSource, pressedScale = 0.97f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick?.invoke() },
            ),
        shape = shape,
        color = color,
        tonalElevation = 6.dp,
    ) {
        AnimatedContent(
            targetState = action,
            label = "resetActionButton",
        ) { target ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (target) {
                    ResetAction.Running -> {
                        ContainedLoadingIndicator(modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Working", style = MaterialTheme.typography.titleMedium, color = contentColor)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(MaterialTheme.shapes.extraSmall),
                            )
                        }
                    }
                    ResetAction.Done -> {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = contentColor)
                        Text("Done", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleMedium, color = contentColor)
                    }
                    ResetAction.Clear -> {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = contentColor)
                        Text("Clear Everything", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleMedium, color = contentColor)
                    }
                    ResetAction.ScanAgain -> {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = contentColor)
                        Text("Scan Again", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleMedium, color = contentColor)
                    }
                    ResetAction.Scan, ResetAction.Confirm -> {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = contentColor)
                        Text("Scan App Data", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleMedium, color = contentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    icon: ImageVector,
    shapeColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialShapes.SoftBurst.toShape(),
        color = shapeColor,
        tonalElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(42.dp),
            )
        }
    }
}

private enum class DataPanelState {
    Intro,
    Found,
    Running,
    Complete,
    Error,
}

private enum class ResetAction {
    Scan,
    Confirm,
    Clear,
    Running,
    Done,
    ScanAgain,
}

private fun defaultBuckets(): List<ClearDataBucket> =
    listOf(
        ClearDataBucket(ClearDataBucketKey.Database, "Database", 0, 0),
        ClearDataBucket(ClearDataBucketKey.Media, "App media", 0, 0),
        ClearDataBucket(ClearDataBucketKey.Cache, "Cache", 0, 0),
        ClearDataBucket(ClearDataBucketKey.Settings, "Settings", 0, 0),
        ClearDataBucket(ClearDataBucketKey.Total, "Total", 0, 0),
    )

private fun Long.formatBytes(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}
