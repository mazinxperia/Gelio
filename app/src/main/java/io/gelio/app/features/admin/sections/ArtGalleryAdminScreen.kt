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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.BrowseGallery
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
import io.gelio.app.data.model.ArtGalleryCard
import io.gelio.app.data.model.ArtGalleryHero
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
fun ArtGalleryAdminScreen(
    section: ShowcaseSection,
    heroes: List<ArtGalleryHero>,
    onSaveHero: (ArtGalleryHero) -> Unit,
    onDeleteHero: (String) -> Unit,
    onToggleHeroVisibility: (ArtGalleryHero) -> Unit,
    onMoveHero: (String, Int) -> Unit,
    onOpenHeroItems: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    var selectedHeroId by rememberSaveable(section.id) { mutableStateOf<String?>(null) }
    var draft by remember(section.id) { mutableStateOf(ArtGalleryHeroDraft.newHero(heroes.size)) }

    LaunchedEffect(heroes, selectedHeroId) {
        val selected = heroes.firstOrNull { it.id == selectedHeroId }
        if (selectedHeroId != null && selected == null) {
            selectedHeroId = null
            draft = ArtGalleryHeroDraft.newHero(heroes.size)
        } else if (selected != null && !draft.isDirtyAgainst(selected)) {
            draft = ArtGalleryHeroDraft.from(selected)
        }
    }

    fun selectHero(hero: ArtGalleryHero) {
        selectedHeroId = hero.id
        draft = ArtGalleryHeroDraft.from(hero)
    }

    fun beginNewHero() {
        selectedHeroId = null
        draft = ArtGalleryHeroDraft.newHero(heroes.size)
    }

    ShowcaseBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = section.title,
                    subtitle = "Art Gallery hero manager",
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
                            ArtGalleryHeroEditorPane(
                                modifier = paneModifier,
                                sectionTitle = section.title,
                                draft = draft,
                                isEditing = selectedHeroId != null,
                                onDraftChange = { draft = it },
                                onReset = ::beginNewHero,
                                onSave = {
                                    val hero = draft.toHero(
                                        sectionId = section.id,
                                        existingId = selectedHeroId,
                                        sortOrder = heroes.size,
                                    )
                                    onSaveHero(hero)
                                    selectedHeroId = hero.id
                                    draft = ArtGalleryHeroDraft.from(hero)
                                },
                            )
                        }
                        val listPane: @Composable (Modifier) -> Unit = { paneModifier ->
                            ArtGalleryHeroListPane(
                                modifier = paneModifier,
                                items = heroes,
                                selectedHeroId = selectedHeroId,
                                onSelect = ::selectHero,
                                onOpenHeroItems = onOpenHeroItems,
                                onToggleVisibility = onToggleHeroVisibility,
                                onMoveHero = onMoveHero,
                                onDeleteHero = { id ->
                                    onDeleteHero(id)
                                    if (selectedHeroId == id) beginNewHero()
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
                isAdding = selectedHeroId == null,
                contentDescription = "Add hero group",
                icon = Icons.Rounded.ViewAgenda,
                onClick = ::beginNewHero,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(adaptive.outerMargin + adaptive.heroSpacing),
            )
        }
    }
}

@Composable
fun ArtGalleryHeroItemsAdminScreen(
    section: ShowcaseSection,
    hero: ArtGalleryHero,
    cards: List<ArtGalleryCard>,
    onSaveCard: (ArtGalleryCard) -> Unit,
    onDeleteCard: (String) -> Unit,
    onToggleCardVisibility: (ArtGalleryCard) -> Unit,
    onMoveCard: (String, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCardId by rememberSaveable(hero.id) { mutableStateOf<String?>(null) }
    var draft by remember(hero.id) { mutableStateOf(ArtGalleryCardDraft.newCard(cards.size)) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<AdminImportProgress?>(null) }
    var showWebPicker by remember { mutableStateOf(false) }

    LaunchedEffect(cards, selectedCardId) {
        val selected = cards.firstOrNull { it.id == selectedCardId }
        if (selectedCardId != null && selected == null) {
            selectedCardId = null
            draft = ArtGalleryCardDraft.newCard(cards.size)
        } else if (selected != null && !draft.isDirtyAgainst(selected)) {
            draft = ArtGalleryCardDraft.from(selected)
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
                    folderName = "art_gallery/cards/images",
                    maxLongSide = 1280,
                    quality = 82,
                )
                draft = draft.copy(imagePath = path)
                importProgress = AdminImportProgress(completed = 1, total = 1)
            } finally {
                isImporting = false
                importProgress = null
            }
        }
    }

    fun selectCard(card: ArtGalleryCard) {
        selectedCardId = card.id
        draft = ArtGalleryCardDraft.from(card)
    }

    fun beginNewCard() {
        selectedCardId = null
        draft = ArtGalleryCardDraft.newCard(cards.size)
    }

    ShowcaseBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = hero.title,
                    subtitle = "Art Gallery item editor",
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
                            ArtGalleryCardEditorPane(
                                modifier = paneModifier,
                                heroTitle = hero.title,
                                draft = draft,
                                isEditing = selectedCardId != null,
                                isImporting = isImporting,
                                onDraftChange = { draft = it },
                                onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                                onPickImageFromWeb = { showWebPicker = true },
                                onReset = ::beginNewCard,
                                onSave = {
                                    val card = draft.toCard(
                                        heroId = hero.id,
                                        existingId = selectedCardId,
                                        sortOrder = cards.size,
                                    )
                                    onSaveCard(card)
                                    selectedCardId = card.id
                                    draft = ArtGalleryCardDraft.from(card)
                                },
                            )
                        }
                        val listPane: @Composable (Modifier) -> Unit = { paneModifier ->
                            ArtGalleryCardListPane(
                                modifier = paneModifier,
                                items = cards,
                                selectedCardId = selectedCardId,
                                onSelect = ::selectCard,
                                onToggleVisibility = onToggleCardVisibility,
                                onMoveCard = onMoveCard,
                                onDeleteCard = { id ->
                                    onDeleteCard(id)
                                    if (selectedCardId == id) beginNewCard()
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
                contentDescription = "Add art gallery item",
                icon = Icons.AutoMirrored.Rounded.Article,
                onClick = ::beginNewCard,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(adaptive.outerMargin + adaptive.heroSpacing),
            )

            AdminImportingOverlay(
                visible = isImporting,
                message = "Converting image into lightweight art gallery media...",
                progress = importProgress,
            )

            if (showWebPicker) {
                WebImagePickerDialog(
                    title = "Search Art Gallery Image",
                    spec = WebImageImportSpec(
                        folderName = "art_gallery/cards/images/web_import",
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
private fun ArtGalleryHeroEditorPane(
    modifier: Modifier,
    sectionTitle: String,
    draft: ArtGalleryHeroDraft,
    isEditing: Boolean,
    onDraftChange: (ArtGalleryHeroDraft) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AdminLibraryHeader(
                title = if (isEditing) "Edit Hero" else "New Hero",
                subtitle = "Create editorial hero groups for $sectionTitle. Each hero holds a title and description only.",
                count = if (isEditing) 1 else 0,
                itemLabel = if (isEditing) "selected" else "drafts",
            )

            OutlinedTextField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hero Title") },
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.description,
                onValueChange = { onDraftChange(draft.copy(description = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Hero Description") },
                minLines = 10,
                maxLines = 12,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.ViewAgenda, contentDescription = null)
                    Text(
                        text = if (isEditing) "New Hero" else "Clear",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Button(
                    onClick = onSave,
                    enabled = draft.title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isEditing) "Update Hero" else "Save Hero")
                }
            }
        }
    }
}

@Composable
private fun ArtGalleryHeroListPane(
    modifier: Modifier,
    items: List<ArtGalleryHero>,
    selectedHeroId: String?,
    onSelect: (ArtGalleryHero) -> Unit,
    onOpenHeroItems: (String) -> Unit,
    onToggleVisibility: (ArtGalleryHero) -> Unit,
    onMoveHero: (String, Int) -> Unit,
    onDeleteHero: (String) -> Unit,
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
                title = "Saved Heroes",
                subtitle = "Edit, reorder, hide, delete, or open a hero to manage its inner cards. This is the only scrolling pane.",
                count = items.size,
                itemLabel = "heroes",
            )
            if (items.isEmpty()) {
                AdminEmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ViewAgenda,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    },
                    title = "No heroes yet",
                    subtitle = "Create the first editorial hero from the editor on the left.",
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
                            ArtGalleryHeroListItem(
                                item = item,
                                index = index,
                                total = items.size,
                                selected = item.id == selectedHeroId,
                                onSelect = { onSelect(item) },
                                onOpenHeroItems = { onOpenHeroItems(item.id) },
                                onToggleVisibility = { onToggleVisibility(item) },
                                onMoveUp = { onMoveHero(item.id, -1) },
                                onMoveDown = { onMoveHero(item.id, 1) },
                                onDelete = { onDeleteHero(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtGalleryHeroListItem(
    item: ArtGalleryHero,
    index: Int,
    total: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenHeroItems: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.title.ifBlank { "Untitled hero" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.description.ifBlank { "No hero description added yet." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (item.hidden) "Hidden on client" else "Visible on client",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(onClick = onOpenHeroItems) {
                    Text("Open Items")
                }
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

@Composable
private fun ArtGalleryCardEditorPane(
    modifier: Modifier,
    heroTitle: String,
    draft: ArtGalleryCardDraft,
    isEditing: Boolean,
    isImporting: Boolean,
    onDraftChange: (ArtGalleryCardDraft) -> Unit,
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AdminLibraryHeader(
                title = if (isEditing) "Edit Item" else "New Item",
                subtitle = "Build image + title + text cards inside $heroTitle.",
                count = if (isEditing) 1 else 0,
                itemLabel = if (isEditing) "selected" else "drafts",
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
                            text = "Pick a lightweight art card image.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    OptimizedAsyncImage(
                        model = draft.imagePath,
                        contentDescription = draft.title.ifBlank { "Art card image" },
                        maxWidth = 720.dp,
                        maxHeight = 360.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            OutlinedTextField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Card Title") },
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.bodyText,
                onValueChange = { onDraftChange(draft.copy(bodyText = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                label = { Text("Text") },
                minLines = 6,
                maxLines = 8,
            )

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
                    contentDescription = "Search art gallery image from web",
                )
                OutlinedButton(
                    onClick = onReset,
                    enabled = !isImporting,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.BrowseGallery, contentDescription = null)
                    Text(
                        text = if (isEditing) "New Item" else "Clear",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = !isImporting &&
                    draft.imagePath.isNotBlank() &&
                    draft.title.isNotBlank() &&
                    draft.bodyText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditing) "Update Item" else "Save Item")
            }
        }
    }
}

@Composable
private fun ArtGalleryCardListPane(
    modifier: Modifier,
    items: List<ArtGalleryCard>,
    selectedCardId: String?,
    onSelect: (ArtGalleryCard) -> Unit,
    onToggleVisibility: (ArtGalleryCard) -> Unit,
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
                title = "Saved Items",
                subtitle = "Edit, hide, reorder, and delete from this rail. This is the only scrolling pane.",
                count = items.size,
                itemLabel = "items",
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
                    title = "No hero items yet",
                    subtitle = "Create the first image + title + text card from the editor on the left.",
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
                            ArtGalleryCardListItem(
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
private fun ArtGalleryCardListItem(
    item: ArtGalleryCard,
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
                    text = item.title.ifBlank { "Untitled item" },
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

private data class ArtGalleryHeroDraft(
    val title: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toHero(
        sectionId: String,
        existingId: String?,
        sortOrder: Int,
    ): ArtGalleryHero {
        val now = System.currentTimeMillis()
        return ArtGalleryHero(
            id = existingId ?: UUID.randomUUID().toString(),
            sectionId = sectionId,
            title = title.trim(),
            description = description.trim(),
            hidden = hidden,
            sortOrder = if (existingId == null) sortOrder else this.sortOrder,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun isDirtyAgainst(hero: ArtGalleryHero): Boolean =
        title != hero.title ||
            description != hero.description ||
            hidden != hero.hidden ||
            sortOrder != hero.sortOrder

    companion object {
        fun from(hero: ArtGalleryHero): ArtGalleryHeroDraft =
            ArtGalleryHeroDraft(
                title = hero.title,
                description = hero.description,
                hidden = hero.hidden,
                sortOrder = hero.sortOrder,
            )

        fun newHero(sortOrder: Int): ArtGalleryHeroDraft =
            ArtGalleryHeroDraft(
                title = "",
                description = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}

private data class ArtGalleryCardDraft(
    val title: String,
    val bodyText: String,
    val imagePath: String,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    fun toCard(
        heroId: String,
        existingId: String?,
        sortOrder: Int,
    ): ArtGalleryCard {
        val now = System.currentTimeMillis()
        return ArtGalleryCard(
            id = existingId ?: UUID.randomUUID().toString(),
            heroId = heroId,
            title = title.trim(),
            bodyText = bodyText.trim(),
            imagePath = imagePath,
            hidden = hidden,
            sortOrder = if (existingId == null) sortOrder else this.sortOrder,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun isDirtyAgainst(card: ArtGalleryCard): Boolean =
        title != card.title ||
            bodyText != card.bodyText ||
            imagePath != card.imagePath ||
            hidden != card.hidden ||
            sortOrder != card.sortOrder

    companion object {
        fun from(card: ArtGalleryCard): ArtGalleryCardDraft =
            ArtGalleryCardDraft(
                title = card.title,
                bodyText = card.bodyText,
                imagePath = card.imagePath,
                hidden = card.hidden,
                sortOrder = card.sortOrder,
            )

        fun newCard(sortOrder: Int): ArtGalleryCardDraft =
            ArtGalleryCardDraft(
                title = "",
                bodyText = "",
                imagePath = "",
                hidden = false,
                sortOrder = sortOrder.coerceAtLeast(0),
            )
    }
}
