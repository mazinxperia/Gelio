package io.gelio.app.features.admin.webimport

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.gelio.app.app.LocalAppContainer
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.data.repository.PexelsPhoto
import kotlinx.coroutines.launch

data class WebImageImportSpec(
    val folderName: String,
    val maxLongSide: Int = 1440,
    val quality: Int = 82,
)

@Composable
fun WebImagePickerDialog(
    title: String,
    spec: WebImageImportSpec,
    onDismiss: () -> Unit,
    onImageImported: (String) -> Unit,
    onUseLocalFile: () -> Unit,
) {
    val appContainer = LocalAppContainer.current
    val settings by appContainer.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = io.gelio.app.data.model.AppSettings())
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<PexelsPhoto>()) }
    var selectedPhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    var nextPageUrl by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val selectedPhoto = remember(results, selectedPhotoId) {
        results.firstOrNull { it.id == selectedPhotoId }
    }

    fun clearSelectionIfMissing() {
        if (selectedPhotoId != null && results.none { it.id == selectedPhotoId }) {
            selectedPhotoId = null
        }
    }

    suspend fun runSearch(loadMore: Boolean) {
        val apiKey = settings.pexelsApiKey.trim()
        if (apiKey.isBlank()) {
            message = "Add and verify the Pexels API key first in App Settings."
            return
        }
        if (!loadMore && query.isBlank()) {
            message = "Enter a search term first."
            return
        }
        isSearching = true
        message = if (loadMore) "Loading more images..." else "Searching Pexels..."
        val result = if (loadMore) {
            val next = nextPageUrl.orEmpty()
            appContainer.pexelsRepository.loadNextPage(apiKey, next)
        } else {
            appContainer.pexelsRepository.searchPhotos(apiKey, query)
        }
        result
            .onSuccess { page ->
                if (loadMore) {
                    results = (results + page.photos).distinctBy { it.id }
                } else {
                    results = page.photos
                }
                nextPageUrl = page.nextPageUrl
                clearSelectionIfMissing()
                message = when {
                    results.isEmpty() -> "No images found for \"$query\"."
                    loadMore -> "Loaded ${page.photos.size} more images."
                    else -> "Found ${results.size} images."
                }
            }
            .onFailure { error ->
                message = error.message ?: "Unable to search Pexels right now."
            }
        isSearching = false
    }

    LaunchedEffect(Unit) {
        message = "Search Pexels and import a lightweight app-owned image."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            shadowElevation = 18.dp,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val compactWidth = maxWidth < 1200.dp
                val compactHeight = maxHeight < 820.dp
                val contentSpacing = if (compactHeight) 12.dp else 18.dp
                val previewHeight = if (compactHeight) 180.dp else 260.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (compactHeight) 18.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(contentSpacing),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Search Pexels, select one image, then import it into app storage.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalIconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    if (compactWidth) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search Pexels") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Rounded.Search, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Search,
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        scope.launch { runSearch(loadMore = false) }
                                    },
                                ),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        scope.launch { runSearch(loadMore = false) }
                                    },
                                    enabled = !isSearching && !isImporting,
                                    modifier = Modifier.weight(0.9f),
                                ) {
                                    Icon(Icons.Rounded.ImageSearch, contentDescription = null)
                                    Text("Search", modifier = Modifier.padding(start = 8.dp))
                                }
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onUseLocalFile()
                                    },
                                    enabled = !isSearching && !isImporting,
                                    modifier = Modifier.weight(1.1f),
                                ) {
                                    Icon(Icons.Rounded.Language, contentDescription = null)
                                    Text("Use local file instead", modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Search Pexels") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Rounded.Search, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Search,
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        scope.launch { runSearch(loadMore = false) }
                                    },
                                ),
                            )
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    scope.launch { runSearch(loadMore = false) }
                                },
                                enabled = !isSearching && !isImporting,
                            ) {
                                Icon(Icons.Rounded.ImageSearch, contentDescription = null)
                                Text("Search", modifier = Modifier.padding(start = 8.dp))
                            }
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onUseLocalFile()
                                },
                                enabled = !isSearching && !isImporting,
                            ) {
                                Icon(Icons.Rounded.Language, contentDescription = null)
                                Text("Use local file instead", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(if (compactHeight) 12.dp else 18.dp),
                    ) {
                    Surface(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (results.isEmpty() && !isSearching) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        Icons.Rounded.ImageSearch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(42.dp),
                                    )
                                    Text(
                                        text = "Search results will appear here.",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(top = 12.dp),
                                    )
                                    Text(
                                        text = "Photos remain online until you confirm Use Selected Image.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 180.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(results, key = { it.id }) { photo ->
                                        PexelsPhotoCard(
                                            photo = photo,
                                            selected = photo.id == selectedPhotoId,
                                            onSelect = { selectedPhotoId = photo.id },
                                        )
                                    }
                                }
                            }

                            if (isSearching) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                text = "Selection",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(previewHeight),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                if (selectedPhoto == null) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Tap any photo card to select it.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    OptimizedAsyncImage(
                                        model = selectedPhoto.previewUrl,
                                        contentDescription = selectedPhoto.title.ifBlank { selectedPhoto.photographerName },
                                        maxWidth = 900.dp,
                                        maxHeight = 600.dp,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.extraLarge),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = selectedPhoto?.title?.takeIf { it.isNotBlank() } ?: "No image selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = selectedPhoto?.let { "Photographer: ${it.photographerName}" }
                                        ?: "Selected image details will show here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Metadata saved internally: local path, original Pexels link, photographer, and Pexels id.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                message?.let { info ->
                                    Text(
                                        text = info,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            if (nextPageUrl != null) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch { runSearch(loadMore = true) }
                                    },
                                    enabled = !isSearching && !isImporting,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Load more")
                                }
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val photo = selectedPhoto ?: return@launch
                                        if (isSearching || isImporting) return@launch
                                        isImporting = true
                                        message = "Downloading and converting the selected image..."
                                        appContainer.pexelsRepository.importPhotoToAppStorage(
                                            photo = photo,
                                            folderName = spec.folderName,
                                            maxLongSide = spec.maxLongSide,
                                            quality = spec.quality,
                                        ).onSuccess { imported ->
                                            isImporting = false
                                            onImageImported(imported.localPath)
                                            onDismiss()
                                        }.onFailure { error ->
                                            isImporting = false
                                            message = error.message ?: "Unable to import the selected image."
                                        }
                                    }
                                },
                                enabled = selectedPhoto != null && !isSearching && !isImporting,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (isImporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                }
                                Text(
                                    text = if (isImporting) "Importing..." else "Use Selected Image",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            Text(
                                text = "Powered by Pexels",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                }
            }
        }
    }

    LaunchedEffect(settings.pexelsApiKey) {
        if (settings.pexelsApiKey.isBlank()) {
            results = emptyList()
            selectedPhotoId = null
            nextPageUrl = null
        }
    }
}

@Composable
private fun PexelsPhotoCard(
    photo: PexelsPhoto,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onSelect,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box {
                OptimizedAsyncImage(
                    model = photo.previewUrl,
                    contentDescription = photo.title.ifBlank { photo.photographerName },
                    maxWidth = 560.dp,
                    maxHeight = 420.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                    contentScale = ContentScale.Crop,
                )
                if (selected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = photo.title.ifBlank { "Pexels photo" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                Text(
                    text = photo.photographerName.ifBlank { "Unknown photographer" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Surface(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Text(
                    text = if (selected) "Selected" else "Tap to select",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
