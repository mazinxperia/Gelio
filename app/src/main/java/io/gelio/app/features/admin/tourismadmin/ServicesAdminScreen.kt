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
import androidx.compose.material.icons.rounded.Workspaces
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
import io.gelio.app.data.model.Service
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
fun ServicesAdminScreen(
    services: List<Service>,
    onSaveService: (Service) -> Unit,
    onDeleteService: (String) -> Unit,
    onToggleVisibility: (Service) -> Unit,
    onMoveService: (String, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<ServiceEditorSession?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<AdminImportProgress?>(null) }
    var showWebPicker by remember { mutableStateOf(false) }
    val openAdd = {
        editor = ServiceEditorSession(
            mode = ServiceEditorMode.Add,
            draft = ServiceEditorState.newItem(services.size),
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
                        folderName = "tourism/services",
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
                    title = "Tourism Services",
                    subtitle = "Local service library",
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
                            title = "Services Library",
                            subtitle = "Add, edit, hide, delete, and reorder tourism service cards.",
                            count = services.size,
                            itemLabel = "services",
                        )
                        if (services.isEmpty()) {
                            AdminEmptyState(
                                icon = {
                                    Icon(
                                        Icons.Rounded.Workspaces,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                },
                                title = "No services yet",
                                subtitle = "Use the plus button to add the first tourism service.",
                                actionLabel = "Add Service",
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
                                itemsIndexed(services, key = { _, service -> service.id }) { index, service ->
                                    AdminStaggeredEntrance(index = index) {
                                        ServiceLibraryItem(
                                            service = service,
                                            index = index,
                                            total = services.size,
                                            onEdit = {
                                                editor = ServiceEditorSession(
                                                    mode = ServiceEditorMode.Edit(service.id),
                                                    draft = ServiceEditorState.from(service),
                                                )
                                            },
                                            onMoveUp = { onMoveService(service.id, -1) },
                                            onMoveDown = { onMoveService(service.id, 1) },
                                            onToggleVisibility = { onToggleVisibility(service) },
                                            onDelete = { onDeleteService(service.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminAddFab(
                isAdding = editor?.mode is ServiceEditorMode.Add,
                contentDescription = "Add service",
                onClick = openAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(44.dp),
            )
        }

        editor?.let { session ->
            ServiceEditorDialog(
                session = session,
                services = services,
                isImporting = isImporting,
                importProgress = importProgress,
                onDraftChange = { editor = session.copy(draft = it) },
                onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                onPickImageFromWeb = { showWebPicker = true },
                onDismiss = { editor = null },
                onDelete = {
                    val id = (session.mode as? ServiceEditorMode.Edit)?.id ?: return@ServiceEditorDialog
                    onDeleteService(id)
                    editor = null
                },
                onSave = {
                    val service = when (val mode = session.mode) {
                        ServiceEditorMode.Add -> session.draft.copy(
                            id = UUID.randomUUID().toString(),
                            sortOrder = services.size,
                        ).toService()
                        is ServiceEditorMode.Edit -> session.draft.copy(id = mode.id).toService()
                    }
                    onSaveService(service)
                    editor = null
                },
            )
        }

        if (showWebPicker) {
            WebImagePickerDialog(
                title = "Search Service Image",
                spec = WebImageImportSpec(
                    folderName = "tourism/services/web_import",
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
private fun ServiceLibraryItem(
    service: Service,
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
                if (service.imageUri.isNotBlank()) {
                    OptimizedAsyncImage(
                        model = service.imageUri,
                        contentDescription = service.serviceTitle,
                        maxWidth = 320.dp,
                        maxHeight = 200.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Workspaces, contentDescription = null)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = service.serviceTitle.ifBlank { "Untitled service" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = service.description.ifBlank { "No description added." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (service.hidden) "Hidden from client view" else "Visible in client view",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (service.hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            AdminItemActions(
                index = index,
                total = total,
                hidden = service.hidden,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onToggleVisibility = onToggleVisibility,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun ServiceEditorDialog(
    session: ServiceEditorSession,
    services: List<Service>,
    isImporting: Boolean,
    importProgress: AdminImportProgress?,
    onDraftChange: (ServiceEditorState) -> Unit,
    onPickImage: () -> Unit,
    onPickImageFromWeb: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val draft = session.draft
    val isEdit = session.mode is ServiceEditorMode.Edit

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
                    title = if (isEdit) "Edit Service" else "Add Service",
                    subtitle = "Create clean service cards for the tourism client view.",
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
                            value = draft.serviceTitle,
                            onValueChange = { onDraftChange(draft.copy(serviceTitle = it)) },
                            label = { Text("Service Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = draft.description,
                            onValueChange = { onDraftChange(draft.copy(description = it)) },
                            label = { Text("Short Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                        )
                        OutlinedTextField(
                            value = draft.imageUri,
                            onValueChange = { onDraftChange(draft.copy(imageUri = it)) },
                            label = { Text("Image URI") },
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
                            contentDescription = "Search service image from web",
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
                        title = "Service Image",
                        note = if (isEdit) "Editing keeps this service in its current library position." else "This creates service ${services.size + 1}.",
                        imageUri = draft.imageUri,
                        emptyLabel = "Pick an image or paste an image URI.",
                        onDelete = if (isEdit) onDelete else null,
                        deleteLabel = "Delete Service",
                    )
                }
                    AdminDialogActions(
                        saveLabel = if (isEdit) "Update Service" else "Add Service",
                        saveEnabled = !isImporting && draft.serviceTitle.isNotBlank(),
                        onDismiss = onDismiss,
                        onSave = onSave,
                        isLoading = isImporting,
                    )
                }
                AdminImportingOverlay(
                    visible = isImporting,
                    message = "Copying service image into app storage...",
                    progress = importProgress,
                )
            }
        }
    }
}

private sealed interface ServiceEditorMode {
    data object Add : ServiceEditorMode
    data class Edit(val id: String) : ServiceEditorMode
}

private data class ServiceEditorSession(
    val mode: ServiceEditorMode,
    val draft: ServiceEditorState,
)

private data class ServiceEditorState(
    val id: String,
    val serviceTitle: String,
    val imageUri: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toService(): Service =
        Service(
            id = id,
            serviceTitle = serviceTitle,
            imageUri = imageUri,
            description = description,
            hidden = hidden,
            sortOrder = sortOrder,
        )

    companion object {
        fun from(service: Service): ServiceEditorState =
            ServiceEditorState(
                id = service.id,
                serviceTitle = service.serviceTitle,
                imageUri = service.imageUri,
                description = service.description,
                hidden = service.hidden,
                sortOrder = service.sortOrder,
            )

        fun newItem(sortOrder: Int): ServiceEditorState =
            ServiceEditorState(
                id = UUID.randomUUID().toString(),
                serviceTitle = "",
                imageUri = "",
                description = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}
