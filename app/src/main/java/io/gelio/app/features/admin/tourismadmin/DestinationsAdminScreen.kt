package io.gelio.app.features.admin.tourismadmin

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Map
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
import io.gelio.app.core.util.copyContentUriToAppStorage
import io.gelio.app.data.model.Destination
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
import io.gelio.app.app.LocalLayoutTokens
import io.gelio.app.features.admin.webimport.WebImageImportSpec
import io.gelio.app.features.admin.webimport.WebImagePickerDialog
import io.gelio.app.features.admin.webimport.WebImportTriggerButton
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun DestinationsAdminScreen(
    destinations: List<Destination>,
    onSaveDestination: (Destination) -> Unit,
    onDeleteDestination: (String) -> Unit,
    onToggleVisibility: (Destination) -> Unit,
    onMoveDestination: (String, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<DestinationEditorSession?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<AdminImportProgress?>(null) }
    var showWebPicker by remember { mutableStateOf(false) }
    val openAdd = {
        editor = DestinationEditorSession(
            mode = DestinationEditorMode.Add,
            draft = DestinationEditorState.newItem(destinations.size),
        )
    }

    val imagePicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isImporting = true
        importProgress = AdminImportProgress(completed = 0, total = 1)
        scope.launch {
            try {
                runCatching {
                    copyContentUriToAppStorage(
                        context = context,
                        uri = uri,
                        folderName = "tourism/destinations",
                        fallbackExtension = ".jpg",
                    )
                }.onSuccess { path ->
                    importProgress = AdminImportProgress(completed = 1, total = 1)
                    editor = editor?.let { it.copy(draft = it.draft.copy(imageUri = path)) }
                }
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
                    title = "Tourism Destinations",
                    subtitle = "Local destination library",
                    onBack = onBack,
                    onHome = onHome,
                    onClose = onClose,
                )
                val tokens = LocalLayoutTokens.current
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = tokens.gutter, vertical = tokens.gutter / 2),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = tokens.gutter, vertical = tokens.gutter * 0.75f),
                        verticalArrangement = Arrangement.spacedBy(tokens.gutter),
                    ) {
                        AdminLibraryHeader(
                            title = "Destinations Library",
                            subtitle = "Add, edit, hide, delete, and reorder tourism destination cards.",
                            count = destinations.size,
                            itemLabel = "destinations",
                        )
                        if (destinations.isEmpty()) {
                            AdminEmptyState(
                                icon = {
                                    Icon(
                                        Icons.Rounded.TravelExplore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                },
                                title = "No destinations yet",
                                subtitle = "Use the plus button to add the first tourism destination.",
                                actionLabel = "Add Destination",
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
                                itemsIndexed(destinations, key = { _, destination -> destination.id }) { index, destination ->
                                    AdminStaggeredEntrance(index = index) {
                                        DestinationLibraryItem(
                                            destination = destination,
                                            index = index,
                                            total = destinations.size,
                                            onEdit = {
                                                editor = DestinationEditorSession(
                                                    mode = DestinationEditorMode.Edit(destination.id),
                                                    draft = DestinationEditorState.from(destination),
                                                )
                                            },
                                            onMoveUp = { onMoveDestination(destination.id, -1) },
                                            onMoveDown = { onMoveDestination(destination.id, 1) },
                                            onToggleVisibility = { onToggleVisibility(destination) },
                                            onDelete = { onDeleteDestination(destination.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminAddFab(
                isAdding = editor?.mode is DestinationEditorMode.Add,
                contentDescription = "Add destination",
                onClick = openAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(44.dp),
            )
        }

        editor?.let { session ->
            DestinationEditorDialog(
                session = session,
                destinations = destinations,
                isImporting = isImporting,
                importProgress = importProgress,
                onDraftChange = { editor = session.copy(draft = it) },
                onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                onPickImageFromWeb = { showWebPicker = true },
                onDismiss = { editor = null },
                onDelete = {
                    val id = (session.mode as? DestinationEditorMode.Edit)?.id ?: return@DestinationEditorDialog
                    onDeleteDestination(id)
                    editor = null
                },
                onSave = {
                    val destination = when (val mode = session.mode) {
                        DestinationEditorMode.Add -> session.draft.copy(
                            id = UUID.randomUUID().toString(),
                            sortOrder = destinations.size,
                        ).toDestination()

                        is DestinationEditorMode.Edit -> session.draft.copy(id = mode.id).toDestination()
                    }
                    onSaveDestination(destination)
                    editor = null
                },
            )
        }

        if (showWebPicker) {
            WebImagePickerDialog(
                title = "Search Destination Image",
                spec = WebImageImportSpec(
                    folderName = "tourism/destinations/web_import",
                    maxLongSide = 1280,
                    quality = 82,
                ),
                onDismiss = { showWebPicker = false },
                onImageImported = { path ->
                    editor = editor?.let { it.copy(draft = it.draft.copy(imageUri = path)) }
                },
                onUseLocalFile = { imagePicker.launch(arrayOf("image/*")) },
            )
        }
    }
}

@Composable
private fun DestinationLibraryItem(
    destination: Destination,
    index: Int,
    total: Int,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

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
                if (destination.imageUri.isNotBlank()) {
                    OptimizedAsyncImage(
                        model = destination.imageUri,
                        contentDescription = destination.destinationName,
                        maxWidth = 320.dp,
                        maxHeight = 200.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Map, contentDescription = null)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = destination.destinationName.ifBlank { "Untitled destination" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = destination.subtitle.ifBlank { "No subtitle added." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (destination.hidden) "Hidden from client view" else "Visible in client view",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (destination.hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            AdminItemActions(
                index = index,
                total = total,
                hidden = destination.hidden,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onToggleVisibility = onToggleVisibility,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun DestinationEditorDialog(
    session: DestinationEditorSession,
    destinations: List<Destination>,
    isImporting: Boolean,
    importProgress: AdminImportProgress?,
    onDraftChange: (DestinationEditorState) -> Unit,
    onPickImage: () -> Unit,
    onPickImageFromWeb: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val draft = session.draft
    val isEdit = session.mode is DestinationEditorMode.Edit

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
                    title = if (isEdit) "Edit Destination" else "Add Destination",
                    subtitle = "Build clean destination cards for the tourism showcase.",
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
                            value = draft.destinationName,
                            onValueChange = { onDraftChange(draft.copy(destinationName = it)) },
                            label = { Text("Destination Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = draft.subtitle,
                            onValueChange = { onDraftChange(draft.copy(subtitle = it)) },
                            label = { Text("Subtitle / Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                        )
                        OutlinedTextField(
                            value = draft.imageUri,
                            onValueChange = { onDraftChange(draft.copy(imageUri = it)) },
                            label = { Text("Image path") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        Button(onClick = onPickImage, enabled = !isImporting) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Text(
                                text = if (draft.imageUri.isBlank()) "Pick Image" else "Replace Image",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        WebImportTriggerButton(
                            onClick = onPickImageFromWeb,
                            enabled = !isImporting,
                            contentDescription = "Search destination image from web",
                        )
                        Text(
                            text = when {
                                isImporting -> "Importing selected image into app storage..."
                                draft.imageUri.isBlank() -> "No image selected yet."
                                else -> "Image stored in app storage: ${draft.imageUri.substringAfterLast('/')}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AdminPreviewPanel(
                        title = "Destination Image",
                        note = if (isEdit) "Editing keeps this destination in its current library position." else "This creates destination ${destinations.size + 1}.",
                        imageUri = draft.imageUri,
                        emptyLabel = "Pick a scenic thumbnail for the destination carousel.",
                        onDelete = if (isEdit) onDelete else null,
                        deleteLabel = "Delete Destination",
                    )
                }
                    AdminDialogActions(
                        saveLabel = if (isEdit) "Update Destination" else "Add Destination",
                        saveEnabled = !isImporting && draft.destinationName.isNotBlank() && draft.imageUri.isNotBlank(),
                        onDismiss = onDismiss,
                        onSave = onSave,
                        isLoading = isImporting,
                    )
                }
                AdminImportingOverlay(
                    visible = isImporting,
                    message = "Copying destination image into app storage...",
                    progress = importProgress,
                )
            }
        }
    }
}

private sealed interface DestinationEditorMode {
    data object Add : DestinationEditorMode
    data class Edit(val id: String) : DestinationEditorMode
}

private data class DestinationEditorSession(
    val mode: DestinationEditorMode,
    val draft: DestinationEditorState,
)

private data class DestinationEditorState(
    val id: String,
    val destinationName: String,
    val imageUri: String,
    val subtitle: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toDestination(): Destination =
        Destination(
            id = id,
            destinationName = destinationName,
            imageUri = imageUri,
            subtitle = subtitle,
            hidden = hidden,
            sortOrder = sortOrder,
        )

    companion object {
        fun from(destination: Destination): DestinationEditorState =
            DestinationEditorState(
                id = destination.id,
                destinationName = destination.destinationName,
                imageUri = destination.imageUri,
                subtitle = destination.subtitle,
                hidden = destination.hidden,
                sortOrder = destination.sortOrder,
            )

        fun newItem(sortOrder: Int): DestinationEditorState =
            DestinationEditorState(
                id = UUID.randomUUID().toString(),
                destinationName = "",
                imageUri = "",
                subtitle = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}
