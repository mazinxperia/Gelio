@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.admin.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens
import io.gelio.app.app.BackupLogEntry
import io.gelio.app.app.BackupOperation
import io.gelio.app.app.BackupUiState
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.rememberAdaptivePanePolicy
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.data.backup.BackupInspection
import io.gelio.app.data.backup.BackupPhase
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun BackupImportScreen(
    uiState: BackupUiState,
    onExport: (password: String?, compressionLevel: Int) -> Unit,
    onImportFileSelected: (android.net.Uri?) -> Unit,
    onImport: (password: String?) -> Unit,
    onClearSession: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    var protectWithPassword by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var compression by remember { mutableFloatStateOf(5f) }
    var importPassword by remember { mutableStateOf("") }
    var passwordExpanded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onClearSession()
        onDispose { onClearSession() }
    }

    LaunchedEffect(uiState.selectedImportUri, uiState.importInspection?.passwordProtected) {
        importPassword = ""
        passwordExpanded = uiState.importInspection?.passwordProtected == true
    }

    LaunchedEffect(importPassword, uiState.importInspection?.passwordProtected) {
        if (uiState.importInspection?.passwordProtected == true && importPassword.isNotBlank()) {
            delay(500)
            if (importPassword.isNotBlank()) passwordExpanded = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        onImportFileSelected(uri)
    }
    val importEnabled = !uiState.running &&
        !uiState.inspectingImportFile &&
        uiState.selectedImportUri != null &&
        uiState.importInspection != null &&
        (uiState.importInspection?.passwordProtected != true || importPassword.isNotBlank())

    ShowcaseBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ViewerTopBar(
                title = "Backup & Import",
                subtitle = ".kskm full mirror",
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = adaptive.contentPaddingHorizontal, vertical = adaptive.contentPaddingVertical),
                verticalArrangement = Arrangement.spacedBy(tokens.panelGap),
            ) {
                Timeline(uiState)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val panePolicy = rememberAdaptivePanePolicy(paneCount = 3)
                    val verticalLayout = adaptive.compactLandscape && maxHeight >= maxWidth
                    if (verticalLayout) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(tokens.panelGap),
                        ) {
                            ExportPanel(
                                uiState = uiState,
                                protectWithPassword = protectWithPassword,
                                onProtectWithPasswordChange = { protectWithPassword = it },
                                exportPassword = exportPassword,
                                onExportPasswordChange = { exportPassword = it },
                                confirmPassword = confirmPassword,
                                onConfirmPasswordChange = { confirmPassword = it },
                                compression = compression,
                                onCompressionChange = { compression = it },
                                onExport = onExport,
                                modifier = Modifier.weight(1f),
                            )
                            LogPanel(uiState = uiState, modifier = Modifier.weight(1f))
                            ImportPanel(
                                uiState = uiState,
                                importPassword = importPassword,
                                onImportPasswordChange = { importPassword = it },
                                passwordExpanded = passwordExpanded,
                                onPasswordExpandedChange = { passwordExpanded = it },
                                onPickFile = { importLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) },
                                onImport = { onImport(importPassword) },
                                importEnabled = importEnabled,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(tokens.panelGap),
                            verticalAlignment = Alignment.Top,
                        ) {
                            ExportPanel(
                                uiState = uiState,
                                protectWithPassword = protectWithPassword,
                                onProtectWithPasswordChange = { protectWithPassword = it },
                                exportPassword = exportPassword,
                                onExportPasswordChange = { exportPassword = it },
                                confirmPassword = confirmPassword,
                                onConfirmPasswordChange = { confirmPassword = it },
                                compression = compression,
                                onCompressionChange = { compression = it },
                                onExport = onExport,
                                modifier = Modifier.weight(panePolicy.primaryWeight).fillMaxHeight(),
                            )
                            LogPanel(uiState = uiState, modifier = Modifier.weight(panePolicy.secondaryWeight).fillMaxHeight())
                            ImportPanel(
                                uiState = uiState,
                                importPassword = importPassword,
                                onImportPasswordChange = { importPassword = it },
                                passwordExpanded = passwordExpanded,
                                onPasswordExpandedChange = { passwordExpanded = it },
                                onPickFile = { importLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) },
                                onImport = { onImport(importPassword) },
                                importEnabled = importEnabled,
                                modifier = Modifier.weight(panePolicy.tertiaryWeight).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportPanel(
    uiState: BackupUiState,
    protectWithPassword: Boolean,
    onProtectWithPasswordChange: (Boolean) -> Unit,
    exportPassword: String,
    onExportPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    compression: Float,
    onCompressionChange: (Float) -> Unit,
    onExport: (password: String?, compressionLevel: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Panel(modifier, Icons.Rounded.Backup, "Export .kskm", "Auto-saves to Downloads and verifies the archive.") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(
                checked = protectWithPassword,
                onCheckedChange = onProtectWithPasswordChange,
                enabled = !uiState.running,
            )
            Column {
                Text("Protect backup with password", style = MaterialTheme.typography.titleMedium)
                Text("Optional AES encryption.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AnimatedVisibility(
            visible = protectWithPassword,
            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec())
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = exportPassword,
                    onValueChange = onExportPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !uiState.running,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !uiState.running,
                    singleLine = true,
                )
            }
        }
        Text("Compression: ${compressionLabel(compression.toInt())}", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = compression,
            onValueChange = { onCompressionChange(it.coerceIn(0f, 9f)) },
            valueRange = 0f..9f,
            steps = 1,
            enabled = !uiState.running,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Fast", style = MaterialTheme.typography.labelMedium)
            Text("Balanced", style = MaterialTheme.typography.labelMedium)
            Text("Smaller", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { onExport(exportPassword.takeIf { protectWithPassword && exportPassword.isNotBlank() }, compression.toInt()) },
            enabled = !uiState.running && (!protectWithPassword || (exportPassword.isNotBlank() && exportPassword == confirmPassword)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Backup, contentDescription = null)
            Text(
                text = if (uiState.running && uiState.activeOperation == BackupOperation.Export) "Exporting..." else "Start Export",
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

@Composable
private fun ImportPanel(
    uiState: BackupUiState,
    importPassword: String,
    onImportPasswordChange: (String) -> Unit,
    passwordExpanded: Boolean,
    onPasswordExpandedChange: (Boolean) -> Unit,
    onPickFile: () -> Unit,
    onImport: () -> Unit,
    importEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Panel(modifier, Icons.Rounded.Restore, "Import .kskm", "Choose a .kskm archive, inspect it, then restore everything automatically.") {
        OutlinedButton(onClick = onPickFile, enabled = !uiState.running, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.FileOpen, contentDescription = null)
            Text("Choose .kskm file", modifier = Modifier.padding(start = 10.dp))
        }
        AnimatedVisibility(
            visible = uiState.inspectingImportFile,
            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            InlineInspectionStatus(uiState.progressLabel)
        }
        uiState.importInspection?.let { ImportInspectionStrip(it) }
        AnimatedVisibility(
            visible = uiState.importInspection?.passwordProtected == true,
            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                if (passwordExpanded || importPassword.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Password detected", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = importPassword,
                            onValueChange = onImportPasswordChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Enter password") },
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                            enabled = !uiState.running,
                            singleLine = true,
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.running) { onPasswordExpandedChange(true) },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Password entered", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Tap to edit before import.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        BulletNotes(
            notes = listOf(
                "Backup is inspected before import",
                "Protected backups ask for password automatically",
                "Current app data is replaced after verification",
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onImport,
            enabled = importEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Restore, contentDescription = null)
            Text(
                text = if (uiState.running && uiState.activeOperation == BackupOperation.Import) "Importing..." else "Start Import",
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        if (!importEnabled && uiState.selectedImportUri != null && !uiState.running) {
            Text(
                text = when {
                    uiState.inspectingImportFile -> "Wait for backup inspection to finish."
                    uiState.importInspection == null -> "Inspection must finish before import."
                    uiState.importInspection?.passwordProtected == true && importPassword.isBlank() -> "Password is required for this backup."
                    else -> "Ready to import."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogPanel(
    uiState: BackupUiState,
    modifier: Modifier = Modifier,
) {
    val panelTitle = when (uiState.activeOperation) {
        BackupOperation.Export -> "Export log"
        BackupOperation.Import -> "Import log"
        BackupOperation.None -> "Live log"
    }
    val progressPercent = (uiState.progress * 100).roundToInt().coerceIn(0, 100)
    val showProgressMetrics = uiState.activeOperation != BackupOperation.None && (uiState.progress > 0f || uiState.running || uiState.inspectingImportFile)
    val logListState = rememberLazyListState()
    LaunchedEffect(uiState.logs.size) {
        val lastIndex = uiState.logs.lastIndex
        if (lastIndex >= 0) {
            logListState.animateScrollToItem(lastIndex)
        }
    }
    Panel(modifier, Icons.Rounded.Verified, panelTitle, uiState.progressLabel, headerShape = MaterialShapes.Clover4Leaf.toShape()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showProgressMetrics) MetricChip("$progressPercent%", highlight = true)
                inspectionOrTransferMetric(uiState)?.let { MetricChip(it) }
                bytesMetric(uiState)?.let { MetricChip(it) }
            }
            AnimatedVisibility(visible = uiState.running || uiState.inspectingImportFile, enter = fadeIn(), exit = fadeOut()) {
                ContainedLoadingIndicator(modifier = Modifier.size(32.dp))
            }
        }
        LinearProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        uiState.summary?.let { summary ->
            StatusStrip(
                icon = Icons.Rounded.CheckCircle,
                title = "Operation complete",
                subtitle = "${summary.fileCount} files verified | ${summary.totalBytes.formatBytes()}${summary.outputLabel?.let { label -> " | $label" }.orEmpty()}",
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            LazyColumn(
                state = logListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.logs) { entry -> LogRow(entry) }
            }
        }
    }
}

@Composable
private fun Timeline(uiState: BackupUiState) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    val placeholder = uiState.activeOperation == BackupOperation.None &&
        uiState.phase == BackupPhase.Idle &&
        !uiState.inspectingImportFile
    val steps = timelineSteps(uiState)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = tokens.pageMaxWidth)
            .height(tokens.touchTarget + adaptive.heroSpacing + 6.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.gutter, vertical = adaptive.heroSpacing * 0.45f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEach { step ->
                TimelineStep(
                    stage = step.stage,
                    active = step.active,
                    complete = step.complete,
                    placeholder = placeholder,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TimelineStep(
    stage: BackupTimelineStage,
    active: Boolean,
    complete: Boolean,
    placeholder: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = when (stage) {
        BackupTimelineStage.Placeholder -> CircleShape
        BackupTimelineStage.ChooseFile -> MaterialShapes.Cookie9Sided.toShape()
        BackupTimelineStage.Scan -> MaterialShapes.Cookie9Sided.toShape()
        BackupTimelineStage.Repair -> MaterialShapes.Gem.toShape()
        BackupTimelineStage.Package -> MaterialShapes.Clover4Leaf.toShape()
        BackupTimelineStage.Inspect -> MaterialShapes.SoftBurst.toShape()
        BackupTimelineStage.Verify -> MaterialShapes.SoftBurst.toShape()
        BackupTimelineStage.Import -> MaterialShapes.Gem.toShape()
        BackupTimelineStage.Complete -> MaterialShapes.Cookie9Sided.toShape()
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = Modifier
                .size(30.dp)
                .clip(shape),
            shape = shape,
            color = when {
                placeholder -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                active || complete -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (placeholder) {
                    Spacer(modifier = Modifier.size(12.dp))
                } else if (active) {
                    LoadingIndicator(modifier = Modifier.size(22.dp))
                } else {
                    Icon(
                        imageVector = if (complete) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                        contentDescription = null,
                        tint = when {
                            placeholder -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            complete -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Text(
            text = if (placeholder) "—" else stage.label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                placeholder -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                active || complete -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    headerShape: Shape = MaterialShapes.Gem.toShape(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = headerShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ImportInspectionStrip(inspection: BackupInspection) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = inspection.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(inspection.sizeBytes.formatBytes())
                MetricChip(if (inspection.passwordProtected) "Password detected" else "No password", highlight = inspection.passwordProtected)
                inspection.fileCount?.let { MetricChip("$it files") }
            }
        }
    }
}

@Composable
private fun InlineInspectionStatus(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoadingIndicator(modifier = Modifier.size(22.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BulletNotes(notes: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        notes.forEach { note ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.padding(top = 7.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Spacer(modifier = Modifier.size(6.dp))
                }
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusStrip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MetricChip(text: String, highlight: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class BackupTimelineStage(val label: String) {
    Placeholder(""),
    ChooseFile("Choose File"),
    Scan("Scan"),
    Repair("Repair"),
    Package("Package"),
    Inspect("Inspect"),
    Verify("Verify"),
    Import("Import"),
    Complete("Complete"),
}

private data class TimelineStepState(
    val stage: BackupTimelineStage,
    val active: Boolean,
    val complete: Boolean,
)

@Composable
private fun LogRow(entry: BackupLogEntry) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = if (entry.error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Spacer(modifier = Modifier.size(14.dp))
        }
        Column {
            Text(entry.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(entry.message, style = MaterialTheme.typography.bodyMedium, color = if (entry.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun compressionLabel(level: Int): String =
    when {
        level <= 2 -> "Fast"
        level <= 6 -> "Balanced"
        else -> "Smaller file"
    }

private fun timelineSteps(uiState: BackupUiState): List<TimelineStepState> {
    return when (uiState.activeOperation) {
        BackupOperation.Export -> {
            val stages = listOf(
                BackupTimelineStage.Scan,
                BackupTimelineStage.Repair,
                BackupTimelineStage.Package,
                BackupTimelineStage.Verify,
                BackupTimelineStage.Complete,
            )
            val activeStage = when (uiState.phase) {
                BackupPhase.Scan -> BackupTimelineStage.Scan
                BackupPhase.Repair -> BackupTimelineStage.Repair
                BackupPhase.Package -> BackupTimelineStage.Package
                BackupPhase.Verify -> BackupTimelineStage.Verify
                BackupPhase.Complete -> BackupTimelineStage.Complete
                else -> null
            }
            stages.map { stage ->
                TimelineStepState(
                    stage = stage,
                    active = activeStage == stage,
                    complete = when (stage) {
                        BackupTimelineStage.Scan -> uiState.phase !in setOf(BackupPhase.Idle, BackupPhase.Scan, BackupPhase.Error)
                        BackupTimelineStage.Repair -> uiState.phase in setOf(BackupPhase.Package, BackupPhase.Verify, BackupPhase.Complete)
                        BackupTimelineStage.Package -> uiState.phase in setOf(BackupPhase.Verify, BackupPhase.Complete)
                        BackupTimelineStage.Verify -> uiState.phase == BackupPhase.Complete
                        BackupTimelineStage.Complete -> uiState.phase == BackupPhase.Complete
                        else -> false
                    },
                )
            }
        }
        BackupOperation.Import -> {
            val stages = listOf(
                BackupTimelineStage.ChooseFile,
                BackupTimelineStage.Inspect,
                BackupTimelineStage.Verify,
                BackupTimelineStage.Import,
                BackupTimelineStage.Complete,
            )
            val activeStage = when {
                uiState.inspectingImportFile -> BackupTimelineStage.Inspect
                uiState.running && uiState.phase == BackupPhase.Verify -> BackupTimelineStage.Verify
                uiState.running && uiState.phase == BackupPhase.Import -> BackupTimelineStage.Import
                uiState.phase == BackupPhase.Complete -> BackupTimelineStage.Complete
                else -> null
            }
            stages.map { stage ->
                TimelineStepState(
                    stage = stage,
                    active = activeStage == stage,
                    complete = when (stage) {
                        BackupTimelineStage.ChooseFile -> uiState.selectedImportUri != null
                        BackupTimelineStage.Inspect -> !uiState.inspectingImportFile && uiState.importInspection != null
                        BackupTimelineStage.Verify -> uiState.phase in setOf(BackupPhase.Import, BackupPhase.Complete)
                        BackupTimelineStage.Import -> uiState.phase == BackupPhase.Complete
                        BackupTimelineStage.Complete -> uiState.phase == BackupPhase.Complete
                        else -> false
                    },
                )
            }
        }
        BackupOperation.None -> listOf(
            BackupTimelineStage.Placeholder,
            BackupTimelineStage.Placeholder,
            BackupTimelineStage.Placeholder,
            BackupTimelineStage.Placeholder,
            BackupTimelineStage.Placeholder,
            BackupTimelineStage.Placeholder,
        ).map { stage -> TimelineStepState(stage = stage, active = false, complete = false) }
    }
}

private fun inspectionOrTransferMetric(uiState: BackupUiState): String? =
    when {
        uiState.inspectingImportFile && uiState.totalFiles > 0 -> "0/${uiState.totalFiles} verified yet"
        uiState.activeOperation == BackupOperation.Import &&
            !uiState.running &&
            !uiState.inspectingImportFile &&
            uiState.importInspection != null &&
            uiState.totalFiles > 0 -> "0/${uiState.totalFiles} verified yet"
        uiState.phase == BackupPhase.Verify && uiState.totalFiles > 0 -> "${uiState.completedFiles}/${uiState.totalFiles} verified"
        uiState.phase == BackupPhase.Import && uiState.totalFiles > 0 -> "${uiState.completedFiles}/${uiState.totalFiles} imported"
        uiState.totalFiles > 0 -> "${uiState.totalFiles} files"
        else -> null
    }

private fun bytesMetric(uiState: BackupUiState): String? =
    when {
        uiState.totalBytes <= 0L -> null
        uiState.phase in setOf(BackupPhase.Verify, BackupPhase.Import) && uiState.completedBytes > 0L ->
            "${uiState.completedBytes.formatBytes()} / ${uiState.totalBytes.formatBytes()}"
        else -> uiState.totalBytes.formatBytes()
    }

private fun Long.formatBytes(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}
