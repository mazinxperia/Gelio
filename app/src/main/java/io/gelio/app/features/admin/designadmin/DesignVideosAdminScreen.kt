@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.admin.designadmin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.features.admin.AdminAddFab
import io.gelio.app.features.admin.AdminDialogActions
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.util.copyContentUriToAppStorage
import io.gelio.app.core.util.extractYouTubeId
import io.gelio.app.core.util.youtubeThumbnail
import io.gelio.app.data.model.ShowcaseVideo
import io.gelio.app.features.admin.AdminImportProgress
import io.gelio.app.features.admin.AdminImportingOverlay
import io.gelio.app.features.admin.webimport.WebImageImportSpec
import io.gelio.app.features.admin.webimport.WebImagePickerDialog
import io.gelio.app.features.admin.webimport.WebImportTriggerButton
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun DesignVideosAdminScreen(
    videos: List<ShowcaseVideo>,
    onSaveVideo: (ShowcaseVideo) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onToggleVisibility: (ShowcaseVideo) -> Unit,
    onMoveVideo: (String, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<VideoEditorSession?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<AdminImportProgress?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showWebPicker by remember { mutableStateOf(false) }

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
                    folderName = "design/videos/thumbnails",
                    fallbackExtension = ".jpg",
                )
                importProgress = AdminImportProgress(completed = 1, total = 1)
                editor = editor?.let { active ->
                    active.copy(draft = active.draft.copy(thumbnailUri = path))
                }
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
                    title = "YouTube Videos",
                    subtitle = "Video library",
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Video Library",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Manage YouTube links, thumbnails, visibility, and ordering.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                Text(
                                    text = "${videos.size} videos",
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (videos.isEmpty()) {
                            EmptyVideoLibrary(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 64.dp),
                                onAdd = {
                                    editor = VideoEditorSession(
                                        mode = VideoEditorMode.Add,
                                        draft = VideoEditorState.newItem(sortOrder = videos.size),
                                    )
                                },
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                                    VideoLibraryItem(
                                        video = video,
                                        index = index,
                                        total = videos.size,
                                        onEdit = {
                                            editor = VideoEditorSession(
                                                mode = VideoEditorMode.Edit(video.id),
                                                draft = VideoEditorState.from(video),
                                            )
                                        },
                                        onMoveUp = { onMoveVideo(video.id, -1) },
                                        onMoveDown = { onMoveVideo(video.id, 1) },
                                        onToggleVisibility = { onToggleVisibility(video) },
                                        onDelete = { onDeleteVideo(video.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AdminAddFab(
                isAdding = editor?.mode is VideoEditorMode.Add,
                contentDescription = "Add video",
                onClick = {
                    editor = VideoEditorSession(
                        mode = VideoEditorMode.Add,
                        draft = VideoEditorState.newItem(sortOrder = videos.size),
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(44.dp),
            )
        }

        editor?.let { session ->
            VideoEditorDialog(
                session = session,
                videos = videos,
                isImporting = isImporting,
                importProgress = importProgress,
                importError = importError,
                onDraftChange = { editor = session.copy(draft = it) },
                onPickThumbnail = { thumbnailPicker.launch(arrayOf("image/*")) },
                onPickThumbnailFromWeb = { showWebPicker = true },
                onDismiss = { editor = null },
                onDelete = {
                    val editMode = session.mode as? VideoEditorMode.Edit ?: return@VideoEditorDialog
                    onDeleteVideo(editMode.id)
                    editor = null
                },
                onSave = {
                    val videoToSave = when (val mode = session.mode) {
                        VideoEditorMode.Add -> session.draft.copy(
                            id = UUID.randomUUID().toString(),
                            sortOrder = videos.size,
                        ).toVideo()

                        is VideoEditorMode.Edit -> session.draft.copy(id = mode.id).toVideo()
                    }
                    onSaveVideo(videoToSave)
                    editor = null
                },
            )
        }

        if (showWebPicker) {
            WebImagePickerDialog(
                title = "Search Video Thumbnail",
                spec = WebImageImportSpec(
                    folderName = "design/videos/thumbnails/web_import",
                    maxLongSide = 1280,
                    quality = 82,
                ),
                onDismiss = { showWebPicker = false },
                onImageImported = { path ->
                    editor = editor?.let { active ->
                        active.copy(draft = active.draft.copy(thumbnailUri = path))
                    }
                },
                onUseLocalFile = { thumbnailPicker.launch(arrayOf("image/*")) },
            )
        }
    }
}

@Composable
private fun VideoLibraryItem(
    video: ShowcaseVideo,
    index: Int,
    total: Int,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
) {
    val thumbnail = remember(video.thumbnailUri, video.youtubeLink) {
        video.thumbnailUri.orEmpty().ifBlank { youtubeThumbnail(video.youtubeLink).orEmpty() }
    }
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
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (thumbnail.isNotBlank()) {
                    OptimizedAsyncImage(
                        model = thumbnail,
                        contentDescription = video.title,
                        maxWidth = 320.dp,
                        maxHeight = 200.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = video.title.ifBlank { "Untitled video" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = video.description.ifBlank { "No description added." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (video.hidden) "Hidden from client view" else "Visible in client view",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (video.hidden) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                }
                FilledTonalIconButton(
                    onClick = onMoveDown,
                    enabled = index < total - 1,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                }
                FilledTonalIconButton(onClick = onToggleVisibility, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = if (video.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = "Toggle visibility",
                    )
                }
                FilledTonalIconButton(onClick = onDelete, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun EmptyVideoLibrary(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
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
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                text = "No YouTube videos yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Use the plus button to add the first video story.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAdd) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(
                    text = "Add Video",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun VideoEditorDialog(
    session: VideoEditorSession,
    videos: List<ShowcaseVideo>,
    isImporting: Boolean,
    importProgress: AdminImportProgress?,
    importError: String?,
    onDraftChange: (VideoEditorState) -> Unit,
    onPickThumbnail: () -> Unit,
    onPickThumbnailFromWeb: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val draft = session.draft
    val validYouTubeLink = extractYouTubeId(draft.youtubeLink) != null
    val fallbackThumbnail = remember(draft.youtubeLink) { youtubeThumbnail(draft.youtubeLink).orEmpty() }
    val previewThumbnail = draft.thumbnailUri.ifBlank { fallbackThumbnail }
    val isEdit = session.mode is VideoEditorMode.Edit
    val title = if (isEdit) "Edit Video" else "Add Video"
    val saveLabel = if (isEdit) "Update Video" else "Add Video"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .widthIn(max = 1080.dp)
                .fillMaxHeight(0.92f),
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
                                text = "Paste a YouTube link. A thumbnail is generated unless you choose a custom one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close editor")
                        }
                    }

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
                                label = { Text("Project / Video Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = draft.description,
                                onValueChange = { onDraftChange(draft.copy(description = it)) },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                            )
                            OutlinedTextField(
                                value = draft.youtubeLink,
                                onValueChange = { onDraftChange(draft.copy(youtubeLink = it)) },
                                label = { Text("YouTube Video Link") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = draft.youtubeLink.isNotBlank() && !validYouTubeLink,
                                supportingText = {
                                    Text(
                                        if (validYouTubeLink || draft.youtubeLink.isBlank()) {
                                            "Example: https://youtu.be/T9JxZCtwzdE?si=vlbQ7Tt3E-Lo8oiz"
                                        } else {
                                            "Paste a valid YouTube URL."
                                        },
                                    )
                                },
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(onClick = onPickThumbnail, enabled = !isImporting) {
                                    Icon(Icons.Rounded.Image, contentDescription = null)
                                    Text(
                                        text = if (isImporting) "Importing..." else "Pick Custom Thumbnail",
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                                WebImportTriggerButton(
                                    onClick = onPickThumbnailFromWeb,
                                    enabled = !isImporting,
                                    contentDescription = "Search video thumbnail from web",
                                )
                                if (draft.thumbnailUri.isNotBlank()) {
                                    TextButton(
                                        onClick = { onDraftChange(draft.copy(thumbnailUri = "")) },
                                    ) {
                                        Text("Use YouTube Thumbnail")
                                    }
                                }
                            }
                            importError?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .width(360.dp)
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
                                    text = "Thumbnail Preview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp),
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ) {
                                    if (previewThumbnail.isNotBlank()) {
                                        OptimizedAsyncImage(
                                            model = previewThumbnail,
                                            contentDescription = draft.title,
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
                                                text = "Paste a valid YouTube link or choose a custom thumbnail.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isEdit) {
                                        "Editing this video will keep its existing library position."
                                    } else {
                                        "This will create a new library item at position ${videos.size + 1}."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isEdit) {
                                    OutlinedButton(
                                        onClick = onDelete,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = null)
                                        Text(
                                            text = "Delete Video",
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AdminDialogActions(
                        saveLabel = saveLabel,
                        saveEnabled = !isImporting && draft.title.isNotBlank() && validYouTubeLink,
                        onDismiss = onDismiss,
                        onSave = onSave,
                        isLoading = isImporting,
                    )
                }
                AdminImportingOverlay(
                    visible = isImporting,
                    message = "Copying video thumbnail into app storage...",
                    progress = importProgress,
                )
            }
        }
    }
}

private sealed interface VideoEditorMode {
    data object Add : VideoEditorMode

    data class Edit(val id: String) : VideoEditorMode
}

private data class VideoEditorSession(
    val mode: VideoEditorMode,
    val draft: VideoEditorState,
)

private data class VideoEditorState(
    val id: String,
    val title: String,
    val youtubeLink: String,
    val description: String,
    val thumbnailUri: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toVideo(): ShowcaseVideo =
        ShowcaseVideo(
            id = id,
            title = title,
            youtubeLink = youtubeLink,
            description = description,
            thumbnailUri = thumbnailUri.ifBlank { null },
            hidden = hidden,
            sortOrder = sortOrder,
        )

    companion object {
        fun from(video: ShowcaseVideo): VideoEditorState =
            VideoEditorState(
                id = video.id,
                title = video.title,
                youtubeLink = video.youtubeLink,
                description = video.description,
                thumbnailUri = video.thumbnailUri.orEmpty(),
                hidden = video.hidden,
                sortOrder = video.sortOrder,
            )

        fun newItem(sortOrder: Int): VideoEditorState =
            VideoEditorState(
                id = UUID.randomUUID().toString(),
                title = "",
                youtubeLink = "",
                description = "",
                thumbnailUri = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}
