package io.gelio.app.features.admin.designadmin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.util.cleanVirtualTourThumbnailUrl
import io.gelio.app.core.util.copyContentUriToAppStorage
import io.gelio.app.core.util.isRemoteUrl
import io.gelio.app.core.util.resolveVirtualTourThumbnailUrl
import io.gelio.app.data.model.VirtualTour
import io.gelio.app.features.admin.AdminAddFab
import io.gelio.app.features.admin.AdminDialogActions
import io.gelio.app.features.admin.AdminDialogHeader
import io.gelio.app.features.admin.AdminEmptyState
import io.gelio.app.features.admin.AdminImportProgress
import io.gelio.app.features.admin.AdminImportingOverlay
import io.gelio.app.features.admin.AdminItemActions
import io.gelio.app.features.admin.AdminLibraryHeader
import io.gelio.app.features.admin.AdminPreviewPanel
import io.gelio.app.features.admin.AdminStaggeredEntrance
import io.gelio.app.features.admin.adminDialogSize
import io.gelio.app.features.admin.webimport.WebImageImportSpec
import io.gelio.app.features.admin.webimport.WebImagePickerDialog
import io.gelio.app.features.admin.webimport.WebImportTriggerButton
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun VirtualToursAdminScreen(
    tours: List<VirtualTour>,
    onSaveTour: (VirtualTour) -> Unit,
    onDeleteTour: (String) -> Unit,
    onToggleVisibility: (VirtualTour) -> Unit,
    onMoveTour: (String, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<VirtualTourEditorSession?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf("Copying tour thumbnail into app storage...") }
    var importProgress by remember { mutableStateOf<AdminImportProgress?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showWebPicker by remember { mutableStateOf(false) }
    val openAdd = {
        editor = VirtualTourEditorSession(
            mode = VirtualTourEditorMode.Add,
            draft = VirtualTourEditorState.newItem(tours.size),
        )
    }

    val thumbnailPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isImporting = true
            importMessage = "Copying tour thumbnail into app storage..."
            importProgress = AdminImportProgress(completed = 0, total = 1)
            importError = null
            try {
                val path = copyContentUriToAppStorage(
                    context = context,
                    uri = uri,
                    folderName = "design/tours/thumbnails",
                    fallbackExtension = ".jpg",
                )
                importProgress = AdminImportProgress(completed = 1, total = 1)
                editor = editor?.let { it.copy(draft = it.draft.copy(thumbnailUri = path)) }
            } catch (error: Throwable) {
                importError = error.message ?: "Unable to import selected thumbnail."
            } finally {
                isImporting = false
                importProgress = null
            }
        }
    }

    ShowcaseBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = "Virtual Tours",
                    subtitle = "Iframe embed management",
                    onBack = onBack,
                    onHome = onHome,
                    onClose = onClose,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        AdminLibraryHeader(
                            title = "Virtual Tours",
                            subtitle = "Paste iframe embed HTML and upload the thumbnail shown to clients.",
                            count = tours.size,
                            itemLabel = "tours",
                        )
                        if (tours.isEmpty()) {
                            AdminEmptyState(
                                icon = {
                                    Icon(
                                        Icons.Rounded.TravelExplore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                },
                                title = "No virtual tours yet",
                                subtitle = "Use the plus button to add the first 360 tour.",
                                actionLabel = "Add Tour",
                                onAction = openAdd,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 64.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                itemsIndexed(tours, key = { _, tour -> tour.id }) { index, tour ->
                                    AdminStaggeredEntrance(index = index) {
                                        VirtualTourLibraryItem(
                                            tour = tour,
                                            index = index,
                                            total = tours.size,
                                            onEdit = {
                                                editor = VirtualTourEditorSession(
                                                    mode = VirtualTourEditorMode.Edit(tour.id),
                                                    draft = VirtualTourEditorState.from(tour),
                                                )
                                            },
                                            onMoveUp = { onMoveTour(tour.id, -1) },
                                            onMoveDown = { onMoveTour(tour.id, 1) },
                                            onToggleVisibility = { onToggleVisibility(tour) },
                                            onDelete = { onDeleteTour(tour.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminAddFab(
                isAdding = editor?.mode is VirtualTourEditorMode.Add,
                contentDescription = "Add virtual tour",
                onClick = openAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(44.dp),
            )
        }

        editor?.let { session ->
            VirtualTourEditorDialog(
                session = session,
                tours = tours,
                isImporting = isImporting,
                importMessage = importMessage,
                importProgress = importProgress,
                importError = importError,
                onDraftChange = { editor = session.copy(draft = it) },
                onPickThumbnail = { thumbnailPicker.launch(arrayOf("image/*")) },
                onPickThumbnailFromWeb = { showWebPicker = true },
                onDismiss = { editor = null },
                onDelete = {
                    val id = (session.mode as? VirtualTourEditorMode.Edit)?.id ?: return@VirtualTourEditorDialog
                    onDeleteTour(id)
                    editor = null
                },
                onSave = {
                    coroutineScope.launch {
                        var draft = session.draft
                        if (draft.thumbnailUri.isBlank()) {
                            isImporting = true
                            importMessage = "Generating virtual tour thumbnail from the 360 link..."
                            importProgress = null
                            importError = null
                            val autoThumbnail = resolveVirtualTourThumbnailUrl(draft.embedHtml).orEmpty()
                            if (autoThumbnail.isNotBlank()) {
                                draft = draft.copy(thumbnailUri = autoThumbnail)
                            }
                            isImporting = false
                        }

                        val tour = when (val mode = session.mode) {
                            VirtualTourEditorMode.Add -> draft.copy(
                                id = UUID.randomUUID().toString(),
                                sortOrder = tours.size,
                            ).toTour()
                            is VirtualTourEditorMode.Edit -> draft.copy(id = mode.id).toTour()
                        }
                        onSaveTour(tour)
                        editor = null
                    }
                },
            )
        }

        if (showWebPicker) {
            WebImagePickerDialog(
                title = "Search Tour Thumbnail",
                spec = WebImageImportSpec(
                    folderName = "design/tours/thumbnails/web_import",
                    maxLongSide = 1280,
                    quality = 82,
                ),
                onDismiss = { showWebPicker = false },
                onImageImported = { path ->
                    editor = editor?.let { it.copy(draft = it.draft.copy(thumbnailUri = path)) }
                },
                onUseLocalFile = { thumbnailPicker.launch(arrayOf("image/*")) },
            )
        }
    }
}

@Composable
private fun VirtualTourLibraryItem(
    tour: VirtualTour,
    index: Int,
    total: Int,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val thumbnail by androidx.compose.runtime.produceState(initialValue = tour.thumbnailUri, tour.thumbnailUri, tour.embedUrl) {
        value = tour.thumbnailUri
            .ifBlank { resolveVirtualTourThumbnailUrl(tour.embedUrl).orEmpty() }
            .let(::cleanVirtualTourThumbnailUrl)
    }

    ElevatedCard(
        onClick = onEdit,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressScale(interactionSource, pressedScale = 0.985f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(width = 184.dp, height = 104.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (thumbnail.isNotBlank()) {
                    OptimizedAsyncImage(
                        model = thumbnail,
                        contentDescription = tour.projectName,
                        maxWidth = 320.dp,
                        maxHeight = 200.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tour.projectName.ifBlank { "Untitled tour" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tour.description.ifBlank { "No description added." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (tour.hidden) "Hidden from client view" else "Visible in client view",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (tour.hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            AdminItemActions(
                index = index,
                total = total,
                hidden = tour.hidden,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onToggleVisibility = onToggleVisibility,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun VirtualTourEditorDialog(
    session: VirtualTourEditorSession,
    tours: List<VirtualTour>,
    isImporting: Boolean,
    importMessage: String,
    importProgress: AdminImportProgress?,
    importError: String?,
    onDraftChange: (VirtualTourEditorState) -> Unit,
    onPickThumbnail: () -> Unit,
    onPickThumbnailFromWeb: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val draft = session.draft
    val isEdit = session.mode is VirtualTourEditorMode.Edit
    val validIframe = draft.embedHtml.contains("<iframe", ignoreCase = true)
    val autoThumbnail by androidx.compose.runtime.produceState(
        initialValue = "",
        draft.embedHtml,
        draft.thumbnailUri,
    ) {
        value = if (draft.thumbnailUri.isBlank() && validIframe) {
            resolveVirtualTourThumbnailUrl(draft.embedHtml).orEmpty()
        } else {
            ""
        }
    }
    val previewThumbnail = draft.thumbnailUri.ifBlank { autoThumbnail }.let(::cleanVirtualTourThumbnailUrl)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.adminDialogSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            shadowElevation = 20.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    AdminDialogHeader(
                        title = if (isEdit) "Edit Tour" else "Add Tour",
                        subtitle = "Use the full iframe embed HTML. The client screen renders it inside the app viewer.",
                        onDismiss = onDismiss,
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            OutlinedTextField(
                                value = draft.projectName,
                                onValueChange = { onDraftChange(draft.copy(projectName = it)) },
                                label = { Text("Tour Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = draft.description,
                                onValueChange = { onDraftChange(draft.copy(description = it)) },
                                label = { Text("Optional Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                            )
                            OutlinedTextField(
                                value = draft.embedHtml,
                                onValueChange = { newEmbedHtml ->
                                    onDraftChange(
                                        draft.copy(
                                            embedHtml = newEmbedHtml,
                                            thumbnailUri = draft.thumbnailUri.takeUnless { isRemoteUrl(it) }.orEmpty(),
                                        ),
                                    )
                                },
                                label = { Text("Iframe Embed HTML") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 8,
                                isError = draft.embedHtml.isNotBlank() && !validIframe,
                                supportingText = {
                                    Text(if (validIframe || draft.embedHtml.isBlank()) "Paste the full iframe embed HTML." else "This must include an iframe tag.")
                                },
                            )
                            Button(onClick = onPickThumbnail, enabled = !isImporting) {
                                Icon(Icons.Rounded.Image, contentDescription = null)
                                Text(if (isImporting) "Importing..." else "Pick Thumbnail", modifier = Modifier.padding(start = 8.dp))
                            }
                            WebImportTriggerButton(
                                onClick = onPickThumbnailFromWeb,
                                enabled = !isImporting,
                                contentDescription = "Search virtual tour thumbnail from web",
                            )
                            importError?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        AdminPreviewPanel(
                            title = "Tour Thumbnail",
                            note = when {
                                draft.thumbnailUri.isNotBlank() && !isRemoteUrl(draft.thumbnailUri) -> "Custom thumbnail selected. It will override the generated preview."
                                previewThumbnail.isNotBlank() -> "Auto thumbnail generated from the 360 tour link."
                                isEdit -> "Editing keeps this tour in its current library position."
                                else -> "This creates tour ${tours.size + 1}."
                            },
                            imageUri = previewThumbnail,
                            emptyLabel = "Paste a valid 360 iframe to auto-generate a thumbnail, or pick a custom thumbnail.",
                            onDelete = if (isEdit) onDelete else null,
                            deleteLabel = "Delete Tour",
                        )
                    }
                    AdminDialogActions(
                        saveLabel = if (isEdit) "Update Tour" else "Add Tour",
                        saveEnabled = !isImporting && draft.projectName.isNotBlank() && validIframe,
                        onDismiss = onDismiss,
                        onSave = onSave,
                        isLoading = isImporting,
                    )
                }
                AdminImportingOverlay(
                    visible = isImporting,
                    message = importMessage,
                    progress = importProgress,
                )
            }
        }
    }
}

private sealed interface VirtualTourEditorMode {
    data object Add : VirtualTourEditorMode
    data class Edit(val id: String) : VirtualTourEditorMode
}

private data class VirtualTourEditorSession(
    val mode: VirtualTourEditorMode,
    val draft: VirtualTourEditorState,
)

private data class VirtualTourEditorState(
    val id: String,
    val projectName: String,
    val embedHtml: String,
    val thumbnailUri: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toTour(): VirtualTour =
        VirtualTour(
            id = id,
            projectName = projectName,
            embedUrl = embedHtml,
            thumbnailUri = thumbnailUri,
            description = description,
            hidden = hidden,
            sortOrder = sortOrder,
        )

    companion object {
        fun from(tour: VirtualTour): VirtualTourEditorState =
            VirtualTourEditorState(
                id = tour.id,
                projectName = tour.projectName,
                embedHtml = tour.embedUrl,
                thumbnailUri = tour.thumbnailUri,
                description = tour.description,
                hidden = tour.hidden,
                sortOrder = tour.sortOrder,
            )

        fun newItem(sortOrder: Int): VirtualTourEditorState =
            VirtualTourEditorState(
                id = UUID.randomUUID().toString(),
                projectName = "",
                embedHtml = "",
                thumbnailUri = "",
                description = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}
