package io.gelio.app.features.admin.brochures

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.core.net.toUri
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
import androidx.compose.material.icons.rounded.Preview
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
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.util.copyContentUriToAppStorage
import io.gelio.app.core.util.generatePdfCoverThumbnailToAppStorage
import io.gelio.app.core.util.isGeneratedPdfCoverThumbnail
import io.gelio.app.data.model.Brand
import io.gelio.app.data.model.Brochure
import io.gelio.app.features.admin.AdminAddFab
import io.gelio.app.features.admin.AdminDialogActions
import io.gelio.app.features.admin.AdminDialogHeader
import io.gelio.app.features.admin.AdminEmptyState
import io.gelio.app.features.admin.AdminImportProgress
import io.gelio.app.features.admin.AdminImportingOverlay
import io.gelio.app.features.admin.AdminItemActions
import io.gelio.app.features.admin.AdminLibraryHeader
import io.gelio.app.features.admin.AdminPreviewPanel
import io.gelio.app.features.admin.adminDialogSize
import io.gelio.app.features.admin.webimport.WebImageImportSpec
import io.gelio.app.features.admin.webimport.WebImagePickerDialog
import io.gelio.app.features.admin.webimport.WebImportTriggerButton
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun BrochuresAdminScreen(
    brand: Brand,
    brochures: List<Brochure>,
    onSaveBrochure: (Brochure) -> Unit,
    onDeleteBrochure: (String, Brand) -> Unit,
    onToggleVisibility: (Brochure) -> Unit,
    onMoveBrochure: (String, Brand, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<BrochureEditorSession?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<AdminImportProgress?>(null) }
    var showWebPicker by remember { mutableStateOf(false) }
    val openAdd = {
        editor = BrochureEditorSession(
            mode = BrochureEditorMode.Add,
            draft = BrochureEditorState.newItem(brand = brand, sortOrder = brochures.size),
        )
    }

    val pdfPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val shouldAutoGenerateCover = editor?.draft?.let { draft ->
            draft.coverThumbnailUri.isBlank() || isGeneratedPdfCoverThumbnail(draft.coverThumbnailUri)
        } == true
        isImporting = true
        importProgress = AdminImportProgress(completed = 0, total = if (shouldAutoGenerateCover) 2 else 1)
        scope.launch {
            try {
                runCatching {
                    copyContentUriToAppStorage(
                        context = context,
                        uri = uri,
                        folderName = "brochures/pdfs",
                        fallbackExtension = ".pdf",
                    )
                }.onSuccess { path ->
                    val generatedCover = if (shouldAutoGenerateCover) {
                        importProgress = AdminImportProgress(completed = 1, total = 2)
                        generatePdfCoverThumbnailToAppStorage(
                            context = context,
                            pdfPath = path,
                        ).also {
                            importProgress = AdminImportProgress(completed = 2, total = 2)
                        }
                    } else {
                        importProgress = AdminImportProgress(completed = 1, total = 1)
                        null
                    }
                    editor = editor?.let { session ->
                        session.copy(
                            draft = session.draft.copy(
                                pdfUri = path,
                                coverThumbnailUri = if (shouldAutoGenerateCover) {
                                    generatedCover ?: session.draft.coverThumbnailUri
                                } else {
                                    session.draft.coverThumbnailUri
                                },
                            ),
                        )
                    }
                }
            } finally {
                isImporting = false
                importProgress = null
            }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isImporting = true
        importProgress = AdminImportProgress(completed = 0, total = 1)
        scope.launch {
            try {
                runCatching {
                    copyContentUriToAppStorage(
                        context = context,
                        uri = uri,
                        folderName = "brochures/covers",
                        fallbackExtension = ".jpg",
                    )
                }.onSuccess { path ->
                    importProgress = AdminImportProgress(completed = 1, total = 1)
                    editor = editor?.let { it.copy(draft = it.draft.copy(coverThumbnailUri = path)) }
                }
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
                    title = "${brand.shortLabel} Brochures",
                    subtitle = "PDF library",
                    onBack = onBack,
                    onHome = onHome,
                    onClose = onClose,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 28.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        AdminLibraryHeader(
                            title = "Brochure Library",
                            subtitle = "Store local PDFs and cover thumbnails inside the app.",
                            count = brochures.size,
                            itemLabel = "brochures",
                        )
                        if (brochures.isEmpty()) {
                            AdminEmptyState(
                                icon = {
                                    Icon(
                                        Icons.Rounded.Preview,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                },
                                title = "No brochures yet",
                                subtitle = "Use the plus button to add the first PDF brochure.",
                                actionLabel = "Add Brochure",
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
                                itemsIndexed(brochures, key = { _, brochure -> brochure.id }) { index, brochure ->
                                    BrochureLibraryItem(
                                        brochure = brochure,
                                        index = index,
                                        total = brochures.size,
                                        onEdit = {
                                            editor = BrochureEditorSession(
                                                mode = BrochureEditorMode.Edit(brochure.id),
                                                draft = BrochureEditorState.from(brochure),
                                            )
                                        },
                                        onMoveUp = { onMoveBrochure(brochure.id, brand, -1) },
                                        onMoveDown = { onMoveBrochure(brochure.id, brand, 1) },
                                        onToggleVisibility = { onToggleVisibility(brochure) },
                                        onDelete = { onDeleteBrochure(brochure.id, brand) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AdminAddFab(
                isAdding = editor?.mode is BrochureEditorMode.Add,
                contentDescription = "Add brochure",
                onClick = openAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(44.dp),
            )
        }

        editor?.let { session ->
            BrochureEditorDialog(
                session = session,
                brochures = brochures,
                isImporting = isImporting,
                importProgress = importProgress,
                onDraftChange = { editor = session.copy(draft = it) },
                onPickPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
                onPickCover = { coverPicker.launch(arrayOf("image/*")) },
                onPickCoverFromWeb = { showWebPicker = true },
                onDismiss = { editor = null },
                onDelete = {
                    val id = (session.mode as? BrochureEditorMode.Edit)?.id ?: return@BrochureEditorDialog
                    onDeleteBrochure(id, brand)
                    editor = null
                },
                onSave = {
                    val brochure = when (val mode = session.mode) {
                        BrochureEditorMode.Add -> session.draft.copy(
                            id = UUID.randomUUID().toString(),
                            sortOrder = brochures.size,
                        ).toBrochure()

                        is BrochureEditorMode.Edit -> session.draft.copy(id = mode.id).toBrochure()
                    }
                    onSaveBrochure(brochure)
                    editor = null
                },
            )
        }

        if (showWebPicker) {
            WebImagePickerDialog(
                title = "Search Brochure Cover",
                spec = WebImageImportSpec(
                    folderName = "brochures/covers/web_import",
                    maxLongSide = 1280,
                    quality = 82,
                ),
                onDismiss = { showWebPicker = false },
                onImageImported = { path ->
                    editor = editor?.let { it.copy(draft = it.draft.copy(coverThumbnailUri = path)) }
                },
                onUseLocalFile = { coverPicker.launch(arrayOf("image/*")) },
            )
        }
    }
}

@Composable
private fun BrochureLibraryItem(
    brochure: Brochure,
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
                modifier = Modifier.size(width = 144.dp, height = 190.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (brochure.coverThumbnailUri.isNotBlank()) {
                    OptimizedAsyncImage(
                        model = brochure.coverThumbnailUri,
                        contentDescription = brochure.title,
                        maxWidth = 240.dp,
                        maxHeight = 320.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Preview, contentDescription = null)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = brochure.title.ifBlank { "Untitled brochure" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = brochure.pdfUri.toUri().lastPathSegment ?: brochure.pdfUri.substringAfterLast('\\').substringAfterLast('/'),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (brochure.hidden) "Hidden from client view" else "Visible in client view",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (brochure.hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            AdminItemActions(
                index = index,
                total = total,
                hidden = brochure.hidden,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onToggleVisibility = onToggleVisibility,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun BrochureEditorDialog(
    session: BrochureEditorSession,
    brochures: List<Brochure>,
    isImporting: Boolean,
    importProgress: AdminImportProgress?,
    onDraftChange: (BrochureEditorState) -> Unit,
    onPickPdf: () -> Unit,
    onPickCover: () -> Unit,
    onPickCoverFromWeb: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val draft = session.draft
    val isEdit = session.mode is BrochureEditorMode.Edit

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
                    title = if (isEdit) "Edit Brochure" else "Add Brochure",
                    subtitle = "Pick a PDF. The first page becomes the cover unless you replace it with a custom image.",
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
                            value = draft.title,
                            onValueChange = { onDraftChange(draft.copy(title = it)) },
                            label = { Text("Brochure Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Button(onClick = onPickPdf, enabled = !isImporting) {
                            Icon(Icons.Rounded.Preview, contentDescription = null)
                            Text(
                                text = if (draft.pdfUri.isBlank()) "Pick PDF" else "Replace PDF",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Button(onClick = onPickCover, enabled = !isImporting) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Text(
                                text = if (draft.coverThumbnailUri.isBlank()) "Pick Custom Cover" else "Replace Cover",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        WebImportTriggerButton(
                            onClick = onPickCoverFromWeb,
                            enabled = !isImporting,
                            contentDescription = "Search brochure cover from web",
                        )
                        Text(
                            text = when {
                                isImporting -> "Importing selected file into app storage..."
                                draft.pdfUri.isBlank() -> "No PDF selected yet."
                                else -> "PDF stored in app storage: ${draft.pdfUri.substringAfterLast('/')}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AdminPreviewPanel(
                        title = "Brochure Cover",
                        note = if (isEdit) "Editing keeps this brochure in its current library position." else "This creates brochure ${brochures.size + 1}.",
                        imageUri = draft.coverThumbnailUri,
                        emptyLabel = "The first PDF page is used automatically unless you add a custom cover.",
                        onDelete = if (isEdit) onDelete else null,
                        deleteLabel = "Delete Brochure",
                    )
                }
                    AdminDialogActions(
                        saveLabel = if (isEdit) "Update Brochure" else "Add Brochure",
                        saveEnabled = !isImporting &&
                            draft.title.isNotBlank() &&
                            draft.pdfUri.isNotBlank(),
                        onDismiss = onDismiss,
                        onSave = onSave,
                        isLoading = isImporting,
                    )
                }
                AdminImportingOverlay(
                    visible = isImporting,
                    message = "Copying selected brochure files into app storage...",
                    progress = importProgress,
                )
            }
        }
    }
}

private sealed interface BrochureEditorMode {
    data object Add : BrochureEditorMode
    data class Edit(val id: String) : BrochureEditorMode
}

private data class BrochureEditorSession(
    val mode: BrochureEditorMode,
    val draft: BrochureEditorState,
)

private data class BrochureEditorState(
    val id: String,
    val brand: Brand,
    val title: String,
    val pdfUri: String,
    val coverThumbnailUri: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toBrochure(): Brochure =
        Brochure(
            id = id,
            brand = brand,
            title = title,
            pdfUri = pdfUri,
            coverThumbnailUri = coverThumbnailUri,
            hidden = hidden,
            sortOrder = sortOrder,
        )

    companion object {
        fun from(brochure: Brochure): BrochureEditorState =
            BrochureEditorState(
                id = brochure.id,
                brand = brochure.brand,
                title = brochure.title,
                pdfUri = brochure.pdfUri,
                coverThumbnailUri = brochure.coverThumbnailUri,
                hidden = brochure.hidden,
                sortOrder = brochure.sortOrder,
            )

        fun newItem(brand: Brand, sortOrder: Int): BrochureEditorState =
            BrochureEditorState(
                id = UUID.randomUUID().toString(),
                brand = brand,
                title = "",
                pdfUri = "",
                coverThumbnailUri = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}
