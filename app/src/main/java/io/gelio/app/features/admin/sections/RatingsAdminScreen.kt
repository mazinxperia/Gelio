package io.gelio.app.features.admin.sections

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.ui.rememberAdaptivePanePolicy
import io.gelio.app.data.model.ReviewCard
import io.gelio.app.data.model.ReviewSource
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.features.admin.AdminDialogHeader
import io.gelio.app.features.admin.AdminItemActions
import io.gelio.app.features.admin.AdminStaggeredEntrance
import io.gelio.app.features.reviews.ReviewCardSurface
import io.gelio.app.features.reviews.ReviewOcrResult
import io.gelio.app.features.reviews.ReviewSourceBadge
import io.gelio.app.features.reviews.extractReviewFromScreenshot
import java.util.UUID

private data class ReviewDraft(
    val id: String = "",
    val reviewerName: String = "",
    val sourceType: ReviewSource = ReviewSource.GENERIC,
    val subHeading: String = "",
    val comment: String = "",
    val rating: Int = 0,
    val hidden: Boolean = false,
    val sortOrder: Int = -1,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingsAdminScreen(
    section: ShowcaseSection,
    items: List<ReviewCard>,
    onSaveReview: (ReviewCard) -> Unit,
    onDeleteReview: (String) -> Unit,
    onToggleVisibility: (ReviewCard) -> Unit,
    onMoveReview: (String, Int) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    val context = LocalContext.current
    var draft by remember(section.id) { mutableStateOf(ReviewDraft()) }
    var selectedReviewId by rememberSaveable(section.id) { mutableStateOf<String?>(null) }
    var screenshotUri by remember(section.id) { mutableStateOf<Uri?>(null) }
    var ocrState by remember(section.id) { mutableStateOf<ReviewOcrResult?>(null) }
    var ocrBusy by remember(section.id) { mutableStateOf(false) }
    var ocrNote by remember(section.id) { mutableStateOf("Upload a review screenshot to auto-fill the form.") }
    var sourceMenuExpanded by remember { mutableStateOf(false) }

    val screenshotPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        screenshotUri = uri
    }

    DisposableEffect(section.id) {
        onDispose {
            screenshotUri = null
            ocrState = null
        }
    }

    LaunchedEffect(selectedReviewId, items) {
        val selected = items.firstOrNull { it.id == selectedReviewId }
        if (selected != null) {
            draft = selected.toDraft()
        } else if (selectedReviewId == null && draft.id.isBlank()) {
            draft = ReviewDraft()
        }
    }

    LaunchedEffect(screenshotUri) {
        val uri = screenshotUri ?: return@LaunchedEffect
        ocrBusy = true
        ocrNote = "Reading screenshot offline..."
        runCatching { extractReviewFromScreenshot(context, uri) }
            .onSuccess { result ->
                ocrState = result
                draft = draft.copy(
                    reviewerName = result.reviewerName.ifBlank { draft.reviewerName },
                    sourceType = result.sourceType,
                    subHeading = result.subHeading.ifBlank { draft.subHeading },
                    comment = result.comment.ifBlank { draft.comment },
                    rating = result.rating ?: draft.rating,
                )
                ocrNote = when {
                    result.rawText.isBlank() -> "OCR finished, but no readable text was found."
                    else -> "OCR finished. Review the extracted fields before saving."
                }
            }
            .onFailure { error ->
                ocrState = null
                ocrNote = error.message ?: "Unable to read screenshot."
            }
        ocrBusy = false
    }

    fun resetDraft() {
        selectedReviewId = null
        draft = ReviewDraft()
        screenshotUri = null
        ocrState = null
        ocrNote = "Upload a review screenshot to auto-fill the form."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ViewerTopBar(
            title = section.title,
            subtitle = "Ratings editor",
            onBack = onBack,
            onHome = onHome,
            onClose = onClose,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = adaptive.contentPaddingHorizontal, vertical = adaptive.contentPaddingVertical)
        ) {
            val panePolicy = rememberAdaptivePanePolicy(paneCount = 3)
            val showOcrPane = maxWidth >= 1180.dp && !adaptive.compactLandscape
            val listWeight = if (showOcrPane) panePolicy.primaryWeight else 0.42f
            val editorWeight = if (showOcrPane) panePolicy.secondaryWeight else 0.58f

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(tokens.panelGap),
            ) {
            Surface(
                modifier = Modifier
                    .weight(listWeight)
                    .fillMaxHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Saved Reviews", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("${items.size} reviews", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalIconButton(onClick = ::resetDraft) {
                            Icon(Icons.Rounded.Add, contentDescription = "New review")
                        }
                    }

                    if (items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No reviews saved yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                                AdminStaggeredEntrance(index = index) {
                                    Surface(
                                        onClick = { selectedReviewId = item.id },
                                        shape = MaterialTheme.shapes.large,
                                        color = if (selectedReviewId == item.id) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Text(
                                                text = item.reviewerName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = item.comment,
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
                                                ReviewSourceBadge(source = item.sourceType)
                                                AdminItemActions(
                                                    index = index,
                                                    total = items.size,
                                                    hidden = item.hidden,
                                                    onMoveUp = { onMoveReview(item.id, -1) },
                                                    onMoveDown = { onMoveReview(item.id, 1) },
                                                    onToggleVisibility = { onToggleVisibility(item) },
                                                    onDelete = {
                                                        onDeleteReview(item.id)
                                                        if (selectedReviewId == item.id) resetDraft()
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(editorWeight)
                    .fillMaxHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Review Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = draft.reviewerName,
                        onValueChange = { draft = draft.copy(reviewerName = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reviewer Name") },
                        singleLine = true,
                    )
                    ExposedDropdownMenuBox(
                        expanded = sourceMenuExpanded,
                        onExpandedChange = { sourceMenuExpanded = !sourceMenuExpanded },
                    ) {
                        OutlinedTextField(
                            value = draft.sourceType.label,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true,
                                )
                                .fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Source") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceMenuExpanded) },
                        )
                        ExposedDropdownMenu(
                            expanded = sourceMenuExpanded,
                            onDismissRequest = { sourceMenuExpanded = false },
                        ) {
                            ReviewSource.entries.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(source.label) },
                                    onClick = {
                                        draft = draft.copy(sourceType = source)
                                        sourceMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = draft.subHeading,
                        onValueChange = { draft = draft.copy(subHeading = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Sub Heading") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = draft.comment,
                        onValueChange = { draft = draft.copy(comment = it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        label = { Text("Comment") },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rating", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            (1..5).forEach { value ->
                                FilledTonalIconButton(onClick = { draft = draft.copy(rating = value) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = "$value stars",
                                        tint = if (value <= draft.rating) {
                                            androidx.compose.ui.graphics.Color(0xFFFFC24B)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            onSaveReview(
                                ReviewCard(
                                    id = draft.id,
                                    sectionId = section.id,
                                    reviewerName = draft.reviewerName.trim(),
                                    sourceType = draft.sourceType,
                                    subHeading = draft.subHeading.trim(),
                                    comment = draft.comment.trim(),
                                    rating = draft.rating.coerceIn(1, 5),
                                    hidden = draft.hidden,
                                    sortOrder = draft.sortOrder,
                                    createdAt = draft.createdAt,
                                    updatedAt = draft.updatedAt,
                                ),
                            )
                            resetDraft()
                        },
                        enabled = draft.reviewerName.trim().isNotBlank() &&
                            draft.comment.trim().isNotBlank() &&
                            draft.rating in 1..5,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (draft.id.isBlank()) "Save Review" else "Update Review")
                    }
                }
            }

            if (showOcrPane) {
                Surface(
                    modifier = Modifier
                        .weight(panePolicy.tertiaryWeight)
                        .fillMaxHeight(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("OCR Assist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { screenshotPicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                        Text("Upload Screenshot", modifier = Modifier.padding(start = 8.dp))
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        when {
                            screenshotUri != null -> {
                                OptimizedAsyncImage(
                                    model = screenshotUri,
                                    contentDescription = "Review screenshot",
                                    maxWidth = 720.dp,
                                    maxHeight = 520.dp,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            else -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Screenshot is temporary and used only for OCR.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                Text(
                                    text = if (ocrBusy) "Reading review screenshot" else "OCR status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (ocrBusy) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.width(22.dp).height(22.dp), strokeWidth = 2.6.dp)
                                    Text(ocrNote, style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                Text(ocrNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ocrState?.rawText?.takeIf { it.isNotBlank() }?.let { raw ->
                                Text(
                                    text = raw,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 10,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Client Card Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            ReviewCardSurface(
                                item = ReviewCard(
                                    id = draft.id.ifBlank { UUID.randomUUID().toString() },
                                    sectionId = section.id,
                                    reviewerName = draft.reviewerName.ifBlank { "Reviewer Name" },
                                    sourceType = draft.sourceType,
                                    subHeading = draft.subHeading,
                                    comment = draft.comment.ifBlank { "Review comment will appear here after OCR or manual entry." },
                                    rating = draft.rating.coerceAtLeast(1),
                                    hidden = draft.hidden,
                                    sortOrder = draft.sortOrder,
                                    createdAt = draft.createdAt,
                                    updatedAt = draft.updatedAt,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
}
}

private fun ReviewCard.toDraft(): ReviewDraft =
    ReviewDraft(
        id = id,
        reviewerName = reviewerName,
        sourceType = sourceType,
        subHeading = subHeading,
        comment = comment,
        rating = rating,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
