@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.admin.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.ui.rememberAdaptivePanePolicy
import io.gelio.app.core.util.importNormalizedImageToAppStorage
import io.gelio.app.data.model.ContentPageCard
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.features.admin.AdminAddFab
import io.gelio.app.features.admin.AdminEmptyState
import io.gelio.app.features.admin.AdminImportProgress
import io.gelio.app.features.admin.AdminImportingOverlay
import io.gelio.app.features.admin.AdminItemActions
import io.gelio.app.features.admin.AdminLibraryHeader
import io.gelio.app.features.admin.AdminStaggeredEntrance
import io.gelio.app.features.admin.webimport.WebImageImportSpec
import io.gelio.app.features.admin.webimport.WebImagePickerDialog
import io.gelio.app.features.admin.webimport.WebImportTriggerButton
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ContentPageAdminScreen(
    section: ShowcaseSection,
    items: List<ContentPageCard>,
    onSaveCard: (ContentPageCard) -> Unit,
    onDeleteCard: (String) -> Unit,
    onToggleVisibility: (ContentPageCard) -> Unit,
    onMoveCard: (String, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCardId by rememberSaveable(section.id) { mutableStateOf<String?>(null) }
    var draft by remember(section.id) { mutableStateOf(ContentPageDraft.newCard(items.size)) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<AdminImportProgress?>(null) }
    var showWebPicker by remember { mutableStateOf(false) }

    LaunchedEffect(items, selectedCardId) {
        val selected = items.firstOrNull { it.id == selectedCardId }
        if (selectedCardId != null && selected == null) {
            selectedCardId = null
            draft = ContentPageDraft.newCard(items.size)
        } else if (selected != null && !draft.isDirtyAgainst(selected)) {
            draft = ContentPageDraft.from(selected)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isImporting = true
        importProgress = AdminImportProgress(completed = 0, total = 1)
        scope.launch {
            try {
                val path = importNormalizedImageToAppStorage(
                    context = context,
                    uri = uri,
                    folderName = "content/pages/images",
                    maxLongSide = 1440,
                    quality = 84,
                )
                draft = draft.copy(imagePath = path)
                importProgress = AdminImportProgress(completed = 1, total = 1)
            } finally {
                isImporting = false
                importProgress = null
            }
        }
    }

    fun selectCard(card: ContentPageCard) {
        selectedCardId = card.id
        draft = ContentPageDraft.from(card)
    }

    fun beginNewCard() {
        selectedCardId = null
        draft = ContentPageDraft.newCard(items.size)
    }

    ShowcaseBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = section.title,
                    subtitle = "Content Page editor",
                    onBack = onBack,
                    onHome = onHome,
                    onClose = onClose,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = adaptive.contentPaddingHorizontal, vertical = adaptive.contentPaddingVertical),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 4.dp,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(tokens.headerPadding + adaptive.heroSpacing * 0.5f),
                    ) {
                        val panePolicy = rememberAdaptivePanePolicy(paneCount = 2)
                        val stackPanels = adaptive.compactLandscape && maxWidth < 1180.dp
                        val editorPane: @Composable (Modifier) -> Unit = { paneModifier ->
                            ContentPageEditorPane(
                                modifier = paneModifier,
                                sectionTitle = section.title,
                                draft = draft,
                                isEditing = selectedCardId != null,
                                isImporting = isImporting,
                                onDraftChange = { draft = it },
                                onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                                onPickImageFromWeb = { showWebPicker = true },
                                onReset = ::beginNewCard,
                                onSave = {
                                    val card = draft.toCard(
                                        sectionId = section.id,
                                        existingId = selectedCardId,
                                        sortOrder = items.size,
                                    )
                                    onSaveCard(card)
                                    selectedCardId = card.id
                                    draft = ContentPageDraft.from(card)
                                },
                            )
                        }
                        val listPane: @Composable (Modifier) -> Unit = { paneModifier ->
                            ContentPageSavedListPane(
                                modifier = paneModifier,
                                items = items,
                                selectedCardId = selectedCardId,
                                onSelect = ::selectCard,
                                onToggleVisibility = onToggleVisibility,
                                onMoveCard = onMoveCard,
                                onDeleteCard = { id ->
                                    onDeleteCard(id)
                                    if (selectedCardId == id) {
                                        beginNewCard()
                                    }
                                },
                            )
                        }

                        if (stackPanels) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(tokens.panelGap),
                            ) {
                                editorPane(Modifier.weight(1f).fillMaxWidth())
                                listPane(Modifier.weight(1f).fillMaxWidth())
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(tokens.panelGap),
                            ) {
                                editorPane(Modifier.weight(panePolicy.primaryWeight).fillMaxHeight())
                                listPane(Modifier.weight(panePolicy.secondaryWeight).fillMaxHeight())
                            }
                        }
                    }
                }
            }

            AdminAddFab(
                isAdding = selectedCardId == null,
                contentDescription = "Add content card",
                icon = Icons.AutoMirrored.Rounded.Article,
                onClick = ::beginNewCard,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(adaptive.outerMargin + adaptive.heroSpacing),
            )

            AdminImportingOverlay(
                visible = isImporting,
                message = "Converting image into lightweight app storage...",
                progress = importProgress,
            )

            if (showWebPicker) {
                WebImagePickerDialog(
                    title = "Search Content Page Image",
                    spec = WebImageImportSpec(
                        folderName = "content/pages/images/web_import",
                        maxLongSide = 1280,
                        quality = 82,
                    ),
                    onDismiss = { showWebPicker = false },
                    onImageImported = { path -> draft = draft.copy(imagePath = path) },
                    onUseLocalFile = { imagePicker.launch(arrayOf("image/*")) },
                )
            }
        }
    }
}

@Composable
private fun ContentPageEditorPane(
    modifier: Modifier,
    sectionTitle: String,
    draft: ContentPageDraft,
    isEditing: Boolean,
    isImporting: Boolean,
    onDraftChange: (ContentPageDraft) -> Unit,
    onPickImage: () -> Unit,
    onPickImageFromWeb: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AdminLibraryHeader(
                title = if (isEditing) "Edit Card" else "New Card",
                subtitle = "Build static $sectionTitle cards with image, name, and text only.",
                count = if (isEditing) 1 else 0,
                itemLabel = if (isEditing) "selected" else "drafts",
            )

            OutlinedTextField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Card Name") },
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.bodyText,
                onValueChange = { onDraftChange(draft.copy(bodyText = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Text") },
                minLines = 8,
                maxLines = 10,
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                if (draft.imagePath.isBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Pick a lightweight card image.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    OptimizedAsyncImage(
                        model = draft.imagePath,
                        contentDescription = draft.title.ifBlank { "Content card image" },
                        maxWidth = 720.dp,
                        maxHeight = 360.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPickImage,
                    enabled = !isImporting,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Text(
                        text = if (draft.imagePath.isBlank()) "Pick Image" else "Replace Image",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                WebImportTriggerButton(
                    onClick = onPickImageFromWeb,
                    enabled = !isImporting,
                    contentDescription = "Search content page image from web",
                )
                OutlinedButton(
                    onClick = onReset,
                    enabled = !isImporting,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.ViewAgenda, contentDescription = null)
                    Text(
                        text = if (isEditing) "New Card" else "Clear",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = !isImporting && draft.title.isNotBlank() && draft.bodyText.isNotBlank() && draft.imagePath.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditing) "Update Card" else "Save Card")
            }
        }
    }
}

@Composable
private fun ContentPageSavedListPane(
    modifier: Modifier,
    items: List<ContentPageCard>,
    selectedCardId: String?,
    onSelect: (ContentPageCard) -> Unit,
    onToggleVisibility: (ContentPageCard) -> Unit,
    onMoveCard: (String, Int) -> Unit,
    onDeleteCard: (String) -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AdminLibraryHeader(
                title = "Saved Cards",
                subtitle = "Edit, hide, reorder, and delete from this rail. This is the only scrolling pane.",
                count = items.size,
                itemLabel = "cards",
            )
            if (items.isEmpty()) {
                AdminEmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    },
                    title = "No content cards yet",
                    subtitle = "Create the first image + name + text card from the editor on the left.",
                    actionLabel = "Ready",
                    onAction = {},
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        AdminStaggeredEntrance(index = index) {
                            ContentPageListItem(
                                item = item,
                                index = index,
                                total = items.size,
                                selected = item.id == selectedCardId,
                                onSelect = { onSelect(item) },
                                onToggleVisibility = { onToggleVisibility(item) },
                                onMoveUp = { onMoveCard(item.id, -1) },
                                onMoveDown = { onMoveCard(item.id, 1) },
                                onDelete = { onDeleteCard(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentPageListItem(
    item: ContentPageCard,
    index: Int,
    total: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    ElevatedCard(
        onClick = onSelect,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressScale(interactionSource, pressedScale = 0.985f),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(width = 124.dp, height = 96.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (item.imagePath.isBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                    }
                } else {
                    OptimizedAsyncImage(
                        model = item.imagePath,
                        contentDescription = item.title,
                        maxWidth = 260.dp,
                        maxHeight = 180.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title.ifBlank { "Untitled card" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.bodyText.ifBlank { "No text added yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.hidden) "Hidden on client" else "Visible on client",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            AdminItemActions(
                index = index,
                total = total,
                hidden = item.hidden,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onToggleVisibility = onToggleVisibility,
                onDelete = onDelete,
            )
        }
    }
}

private data class ContentPageDraft(
    val title: String,
    val bodyText: String,
    val imagePath: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toCard(
        sectionId: String,
        existingId: String?,
        sortOrder: Int,
    ): ContentPageCard {
        val now = System.currentTimeMillis()
        return ContentPageCard(
            id = existingId ?: UUID.randomUUID().toString(),
            sectionId = sectionId,
            title = title.trim(),
            bodyText = bodyText.trim(),
            imagePath = imagePath,
            hidden = hidden,
            sortOrder = if (existingId == null) sortOrder else this.sortOrder,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun isDirtyAgainst(card: ContentPageCard): Boolean =
        title != card.title ||
            bodyText != card.bodyText ||
            imagePath != card.imagePath ||
            hidden != card.hidden ||
            sortOrder != card.sortOrder

    companion object {
        fun from(card: ContentPageCard): ContentPageDraft =
            ContentPageDraft(
                title = card.title,
                bodyText = card.bodyText,
                imagePath = card.imagePath,
                hidden = card.hidden,
                sortOrder = card.sortOrder,
            )

        fun newCard(sortOrder: Int): ContentPageDraft =
            ContentPageDraft(
                title = "",
                bodyText = "",
                imagePath = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}
