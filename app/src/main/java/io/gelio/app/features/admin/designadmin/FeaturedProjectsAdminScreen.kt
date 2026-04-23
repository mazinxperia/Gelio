package io.gelio.app.features.admin.designadmin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.util.copyContentUriToAppStorage
import io.gelio.app.data.model.FeaturedProject
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
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FeaturedProjectsAdminScreen(
    projects: List<FeaturedProject>,
    allItemsLink: String,
    onAllItemsLinkSave: (String) -> Unit,
    onSaveProject: (FeaturedProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onToggleVisibility: (FeaturedProject) -> Unit,
    onMoveProject: (String, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<FeaturedProjectEditorSession?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<AdminImportProgress?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var webImportTarget by remember { mutableStateOf<FeaturedProjectWebImportTarget?>(null) }
    var allItemsLinkInput by remember(allItemsLink) { mutableStateOf(allItemsLink) }
    val openAdd = {
        editor = FeaturedProjectEditorSession(
            mode = FeaturedProjectEditorMode.Add,
            draft = FeaturedProjectEditorState.newItem(projects.size),
        )
    }

    val galleryPicker = rememberLauncherForActivityResult(OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isImporting = true
            importProgress = AdminImportProgress(completed = 0, total = uris.size)
            importError = null
            try {
                val copiedImages = mutableListOf<String>()
                uris.forEachIndexed { index, uri ->
                    copiedImages += copyContentUriToAppStorage(
                        context = context,
                        uri = uri,
                        folderName = "design/projects/gallery",
                        fallbackExtension = ".jpg",
                    )
                    importProgress = AdminImportProgress(completed = index + 1, total = uris.size)
                }
                editor = editor?.let { active ->
                    val images = (active.draft.galleryImages + copiedImages).distinct()
                    active.copy(
                        draft = active.draft.copy(
                            galleryImages = images,
                            thumbnailUri = active.draft.thumbnailUri.ifBlank { images.firstOrNull().orEmpty() },
                        ),
                    )
                }
            } catch (error: Throwable) {
                importError = error.message ?: "Unable to import selected images."
            } finally {
                isImporting = false
                importProgress = null
            }
        }
    }
    val thumbnailPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isImporting = true
            importProgress = AdminImportProgress(completed = 0, total = 1)
            importError = null
            try {
                val path = copyContentUriToAppStorage(
                    context = context,
                    uri = uri,
                    folderName = "design/projects/thumbnails",
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = "Featured Projects",
                    subtitle = "Design gallery management",
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
                            title = "Featured Projects",
                            subtitle = "Upload galleries, choose thumbnails, control order, and set the optional external collection link.",
                            count = projects.size,
                            itemLabel = "projects",
                        )
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "All Items Link",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Optional external link for the client-side button below the gallery. Use any Drive, SharePoint, or web collection URL.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedTextField(
                                        value = allItemsLinkInput,
                                        onValueChange = { allItemsLinkInput = it },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("External Collection URL") },
                                        singleLine = true,
                                    )
                                    Button(onClick = { onAllItemsLinkSave(allItemsLinkInput) }) {
                                        Text("Save Link")
                                    }
                                }
                            }
                        }
                        if (projects.isEmpty()) {
                            AdminEmptyState(
                                icon = {
                                    Icon(
                                        Icons.Rounded.PhotoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                },
                                title = "No featured projects yet",
                                subtitle = "Use the plus button to create the first project gallery.",
                                actionLabel = "Add Project",
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
                                itemsIndexed(projects, key = { _, project -> project.id }) { index, project ->
                                    AdminStaggeredEntrance(index = index) {
                                        FeaturedProjectLibraryItem(
                                            project = project,
                                            index = index,
                                            total = projects.size,
                                            onEdit = {
                                                editor = FeaturedProjectEditorSession(
                                                    mode = FeaturedProjectEditorMode.Edit(project.id),
                                                    draft = FeaturedProjectEditorState.from(project),
                                                )
                                            },
                                            onMoveUp = { onMoveProject(project.id, -1) },
                                            onMoveDown = { onMoveProject(project.id, 1) },
                                            onToggleVisibility = { onToggleVisibility(project) },
                                            onDelete = { onDeleteProject(project.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            AdminAddFab(
                isAdding = editor?.mode is FeaturedProjectEditorMode.Add,
                contentDescription = "Add featured project",
                onClick = openAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(44.dp),
            )
        }

        editor?.let { session ->
            FeaturedProjectEditorDialog(
                session = session,
                projects = projects,
                isImporting = isImporting,
                importProgress = importProgress,
                importError = importError,
                onDraftChange = { editor = session.copy(draft = it) },
                onPickGallery = { galleryPicker.launch(arrayOf("image/*")) },
                onPickGalleryFromWeb = { webImportTarget = FeaturedProjectWebImportTarget.Gallery },
                onPickThumbnail = { thumbnailPicker.launch(arrayOf("image/*")) },
                onPickThumbnailFromWeb = { webImportTarget = FeaturedProjectWebImportTarget.Thumbnail },
                onDismiss = { editor = null },
                onDelete = {
                    val id = (session.mode as? FeaturedProjectEditorMode.Edit)?.id ?: return@FeaturedProjectEditorDialog
                    onDeleteProject(id)
                    editor = null
                },
                onSave = {
                    val thumbnail = session.draft.thumbnailUri.ifBlank { session.draft.galleryImages.firstOrNull().orEmpty() }
                    val project = when (val mode = session.mode) {
                        FeaturedProjectEditorMode.Add -> session.draft.copy(
                            id = UUID.randomUUID().toString(),
                            thumbnailUri = thumbnail,
                            sortOrder = projects.size,
                        ).toProject()
                        is FeaturedProjectEditorMode.Edit -> session.draft.copy(
                            id = mode.id,
                            thumbnailUri = thumbnail,
                        ).toProject()
                    }
                    onSaveProject(project)
                    editor = null
                },
            )
        }

        webImportTarget?.let { target ->
            val (dialogTitle, spec) = when (target) {
                FeaturedProjectWebImportTarget.Gallery -> {
                    "Search Project Gallery Image" to WebImageImportSpec(
                        folderName = "design/projects/gallery/web_import",
                        maxLongSide = 1440,
                        quality = 82,
                    )
                }

                FeaturedProjectWebImportTarget.Thumbnail -> {
                    "Search Project Thumbnail" to WebImageImportSpec(
                        folderName = "design/projects/thumbnails/web_import",
                        maxLongSide = 1280,
                        quality = 82,
                    )
                }
            }
            WebImagePickerDialog(
                title = dialogTitle,
                spec = spec,
                onDismiss = { webImportTarget = null },
                onImageImported = { path ->
                    editor = editor?.let { active ->
                        when (target) {
                            FeaturedProjectWebImportTarget.Gallery -> {
                                val images = (active.draft.galleryImages + path).distinct()
                                active.copy(
                                    draft = active.draft.copy(
                                        galleryImages = images,
                                        thumbnailUri = active.draft.thumbnailUri.ifBlank { images.firstOrNull().orEmpty() },
                                    ),
                                )
                            }

                            FeaturedProjectWebImportTarget.Thumbnail -> {
                                active.copy(draft = active.draft.copy(thumbnailUri = path))
                            }
                        }
                    }
                },
                onUseLocalFile = {
                    when (target) {
                        FeaturedProjectWebImportTarget.Gallery -> galleryPicker.launch(arrayOf("image/*"))
                        FeaturedProjectWebImportTarget.Thumbnail -> thumbnailPicker.launch(arrayOf("image/*"))
                    }
                },
            )
        }
    }
}

@Composable
private fun FeaturedProjectLibraryItem(
    project: FeaturedProject,
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
                if (project.thumbnailUri.isNotBlank()) {
                    OptimizedAsyncImage(
                        model = project.thumbnailUri,
                        contentDescription = project.projectName,
                        maxWidth = 320.dp,
                        maxHeight = 200.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = project.projectName.ifBlank { "Untitled project" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${project.galleryImages.size} gallery images",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ProjectStorageUsagePill(project = project)
                }
                Text(
                    text = if (project.hidden) "Hidden from client view" else "Visible in client view",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (project.hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            AdminItemActions(
                index = index,
                total = total,
                hidden = project.hidden,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onToggleVisibility = onToggleVisibility,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun ProjectStorageUsagePill(
    project: FeaturedProject,
) {
    val usage by produceState<ProjectStorageUsage?>(initialValue = null, project.galleryImages, project.thumbnailUri) {
        value = withContext(Dispatchers.IO) {
            calculateProjectStorageUsage(project)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = usage?.let { formatStorageBytes(it.bytes) } ?: "Calculating size",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun FeaturedProjectEditorDialog(
    session: FeaturedProjectEditorSession,
    projects: List<FeaturedProject>,
    isImporting: Boolean,
    importProgress: AdminImportProgress?,
    importError: String?,
    onDraftChange: (FeaturedProjectEditorState) -> Unit,
    onPickGallery: () -> Unit,
    onPickGalleryFromWeb: () -> Unit,
    onPickThumbnail: () -> Unit,
    onPickThumbnailFromWeb: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val draft = session.draft
    val isEdit = session.mode is FeaturedProjectEditorMode.Edit
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.adminDialogSize(maxWidth = 1160.dp),
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
                        title = if (isEdit) "Edit Project" else "Add Project",
                        subtitle = "Upload gallery images, then choose the thumbnail shown in the client view.",
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
                                label = { Text("Project Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = onPickGallery, enabled = !isImporting) {
                                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                                    Text(if (isImporting) "Importing..." else "Upload Images", modifier = Modifier.padding(start = 8.dp))
                                }
                                WebImportTriggerButton(
                                    onClick = onPickGalleryFromWeb,
                                    enabled = !isImporting,
                                    contentDescription = "Search project gallery image from web",
                                )
                                Button(onClick = onPickThumbnail, enabled = !isImporting) {
                                    Icon(Icons.Rounded.Image, contentDescription = null)
                                    Text("Custom Thumbnail", modifier = Modifier.padding(start = 8.dp))
                                }
                                WebImportTriggerButton(
                                    onClick = onPickThumbnailFromWeb,
                                    enabled = !isImporting,
                                    contentDescription = "Search project thumbnail from web",
                                )
                            }
                            importError?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Text("Uploaded Gallery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (draft.galleryImages.isEmpty()) {
                                EmptyPreviewCard("No gallery images added yet.")
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    itemsIndexed(draft.galleryImages, key = { _, uri -> uri }) { index, imageUri ->
                                        GalleryImageEditorCard(
                                            imageUri = imageUri,
                                            selected = imageUri == draft.thumbnailUri,
                                            onSelectAsThumbnail = { onDraftChange(draft.copy(thumbnailUri = imageUri)) },
                                            onRemove = {
                                                val updated = draft.galleryImages.toMutableList().apply { removeAt(index) }
                                                onDraftChange(
                                                    draft.copy(
                                                        galleryImages = updated,
                                                        thumbnailUri = when {
                                                            draft.thumbnailUri != imageUri -> draft.thumbnailUri
                                                            updated.isNotEmpty() -> updated.first()
                                                            else -> ""
                                                        },
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        AdminPreviewPanel(
                            title = "Card Thumbnail",
                            note = if (isEdit) "Editing keeps this project in its current library position." else "This creates project ${projects.size + 1}.",
                            imageUri = draft.thumbnailUri,
                            emptyLabel = "Pick gallery images or upload a custom thumbnail.",
                            onDelete = if (isEdit) onDelete else null,
                            deleteLabel = "Delete Project",
                        )
                    }
                    AdminDialogActions(
                        saveLabel = if (isEdit) "Update Project" else "Add Project",
                        saveEnabled = !isImporting && draft.projectName.isNotBlank() && draft.galleryImages.isNotEmpty(),
                        onDismiss = onDismiss,
                        onSave = onSave,
                        isLoading = isImporting,
                    )
                }
                AdminImportingOverlay(
                    visible = isImporting,
                    message = "Copying selected project media into app storage...",
                    progress = importProgress,
                )
            }
        }
    }
}

@Composable
private fun GalleryImageEditorCard(
    imageUri: String,
    selected: Boolean,
    onSelectAsThumbnail: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(184.dp),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OptimizedAsyncImage(
                model = imageUri,
                contentDescription = "Gallery image",
                maxWidth = 280.dp,
                maxHeight = 200.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onSelectAsThumbnail),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = if (selected) "Current thumbnail" else "Tap to use as thumbnail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Rounded.HideImage, contentDescription = null)
                Text("Remove", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun EmptyPreviewCard(label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private sealed interface FeaturedProjectEditorMode {
    data object Add : FeaturedProjectEditorMode
    data class Edit(val id: String) : FeaturedProjectEditorMode
}

private data class FeaturedProjectEditorSession(
    val mode: FeaturedProjectEditorMode,
    val draft: FeaturedProjectEditorState,
)

private data class FeaturedProjectEditorState(
    val id: String,
    val projectName: String,
    val galleryImages: List<String>,
    val thumbnailUri: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toProject(): FeaturedProject =
        FeaturedProject(
            id = id,
            projectName = projectName,
            galleryImages = galleryImages,
            thumbnailUri = thumbnailUri,
            featured = true,
            hidden = hidden,
            sortOrder = sortOrder,
        )

    companion object {
        fun from(project: FeaturedProject): FeaturedProjectEditorState =
            FeaturedProjectEditorState(
                id = project.id,
                projectName = project.projectName,
                galleryImages = project.galleryImages,
                thumbnailUri = project.thumbnailUri,
                hidden = project.hidden,
                sortOrder = project.sortOrder,
            )

        fun newItem(sortOrder: Int): FeaturedProjectEditorState =
            FeaturedProjectEditorState(
                id = UUID.randomUUID().toString(),
                projectName = "",
                galleryImages = emptyList(),
                thumbnailUri = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}

private data class ProjectStorageUsage(
    val bytes: Long,
)

private enum class FeaturedProjectWebImportTarget {
    Gallery,
    Thumbnail,
}

private fun calculateProjectStorageUsage(project: FeaturedProject): ProjectStorageUsage {
    val bytes = buildList {
        add(project.thumbnailUri)
        addAll(project.galleryImages)
    }
        .filter { it.isNotBlank() }
        .distinct()
        .sumOf { path ->
            path.toLocalFileOrNull()?.length() ?: 0L
        }

    return ProjectStorageUsage(bytes = bytes)
}

private fun String.toLocalFileOrNull(): File? {
    val localPath = when {
        startsWith("file://") -> toUri().path
        startsWith("content://") -> null
        startsWith("http://") || startsWith("https://") -> null
        else -> this
    } ?: return null

    return File(localPath).takeIf { it.isFile }
}

private fun formatStorageBytes(bytes: Long): String {
    val gib = 1024.0 * 1024.0 * 1024.0
    val mib = 1024.0 * 1024.0
    val kib = 1024.0
    return when {
        bytes >= gib -> String.format(Locale.US, "%.2f GB", bytes / gib)
        bytes >= mib -> String.format(Locale.US, "%.1f MB", bytes / mib)
        bytes >= kib -> String.format(Locale.US, "%.1f KB", bytes / kib)
        else -> "$bytes B"
    }
}
