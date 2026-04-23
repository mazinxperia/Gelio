@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.admin.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.BrowseGallery
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RoomService
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ShowcaseHomeButton
import io.gelio.app.core.ui.ShowcasePageFrame
import io.gelio.app.core.ui.ShowcasePrimaryActionButton
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.util.copyContentUriToAppStorage
import io.gelio.app.data.model.SectionType
import io.gelio.app.data.model.ShowcaseCompany
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.features.admin.*
import io.gelio.app.data.model.AppSettings
import kotlinx.coroutines.launch

@Composable
fun SectionsAdminScreen(
    companies: List<ShowcaseCompany>,
    settings: AppSettings,
    onUpdateHomescreenLogo: (String) -> Unit,
    onCompanyClick: (String) -> Unit,
    onCreateCompany: (String, String, String) -> Unit,
    onUpdateCompany: (ShowcaseCompany) -> Unit,
    onToggleCompanyVisibility: (ShowcaseCompany) -> Unit,
    onMoveCompany: (String, Int) -> Unit,
    onDeleteCompany: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    var editorTarget by remember { mutableStateOf<ShowcaseCompany?>(null) }
    var createDialogVisible by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ShowcaseCompany?>(null) }
    var showHomescreenLogoPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val homescreenLogoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                copyContentUriToAppStorage(
                    context = context,
                    uri = uri,
                    folderName = "branding",
                    fallbackExtension = ".png",
                )
            }.onSuccess { importedPath ->
                onUpdateHomescreenLogo(importedPath)
                showHomescreenLogoPicker = false
            }
        }
    }

    // Auto-prompt for homescreen logo if we have 2+ companies and no logo set
    androidx.compose.runtime.LaunchedEffect(companies.size) {
        if (companies.size >= 2 && settings.homescreenLogoPath.isBlank()) {
            showHomescreenLogoPicker = true
        }
    }

    ShowcaseBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ViewerTopBar(
                title = "Sections",
                subtitle = "Dynamic company section management",
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )
            ShowcasePageFrame(
                maxWidth = 1680.dp,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
            ) {
                AdminLibraryHeader(
                    title = "Companies",
                    subtitle = "Add, brand, reorder, hide, and manage every company from one place.",
                    count = companies.size,
                    itemLabel = "companies",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (companies.size >= 2) {
                        ShowcasePrimaryActionButton(
                            label = if (settings.homescreenLogoPath.isBlank()) "Set Homescreen Logo" else "Change Homescreen Logo",
                            onClick = { showHomescreenLogoPicker = true },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    ShowcasePrimaryActionButton(
                        label = "Add Company",
                        onClick = { createDialogVisible = true },
                    )
                }
                if (companies.isEmpty()) {
                    AdminEmptyState(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        },
                        title = "No companies available",
                        subtitle = "Gelio starts blank. Add the first company with a logo and seed color to begin building sections.",
                        actionLabel = "Add Company",
                        onAction = { createDialogVisible = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        companies.forEachIndexed { index, company ->
                            AdminStaggeredEntrance(index = index) {
                                CompanyCard(
                                    company = company,
                                    index = index,
                                    total = companies.size,
                                    onClick = { onCompanyClick(company.id) },
                                    onEdit = { editorTarget = company },
                                    onToggleVisibility = { onToggleCompanyVisibility(company) },
                                    onMoveLeft = { onMoveCompany(company.id, -1) },
                                    onMoveRight = { onMoveCompany(company.id, 1) },
                                    onDelete = { deleteTarget = company },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (createDialogVisible) {
        CompanyEditorDialog(
            company = null,
            onDismiss = { createDialogVisible = false },
            onSave = { name, logoPath, brandSeedColor ->
                onCreateCompany(name, logoPath, brandSeedColor)
                createDialogVisible = false
            },
        )
    }

    editorTarget?.let { company ->
        CompanyEditorDialog(
            company = company,
            onDismiss = { editorTarget = null },
            onSave = { name, logoPath, brandSeedColor ->
                onUpdateCompany(
                    company.copy(
                        name = name,
                        logoPath = logoPath,
                        brandSeedColor = brandSeedColor,
                    ),
                )
                editorTarget = null
            },
        )
    }

    deleteTarget?.let { company ->
        DeleteCompanyDialog(
            company = company,
            onDismiss = { deleteTarget = null },
            onDelete = {
                onDeleteCompany(company.id)
                deleteTarget = null
            },
        )
    }

    if (showHomescreenLogoPicker) {
        Dialog(
            onDismissRequest = { showHomescreenLogoPicker = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.width(440.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Multi-Company Logo",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Since you have multiple companies, Gelio needs a parent logo for the homescreen. This logo will appear when users first see the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (settings.homescreenLogoPath.isNotBlank()) {
                            OptimizedAsyncImage(
                                model = settings.homescreenLogoPath,
                                contentDescription = "Homescreen Logo",
                                maxWidth = 200.dp,
                                maxHeight = 200.dp,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("No Logo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = { showHomescreenLogoPicker = false },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }
                        ShowcasePrimaryActionButton(
                            label = "Select Image",
                            onClick = { homescreenLogoPicker.launch("image/*") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompanySectionsAdminScreen(
    company: ShowcaseCompany?,
    sections: List<ShowcaseSection>,
    onOpenSection: (String) -> Unit,
    onCreateSection: (SectionType, String) -> Unit,
    onRenameSection: (ShowcaseSection, String) -> Unit,
    onToggleSectionVisibility: (ShowcaseSection) -> Unit,
    onMoveSection: (String, Int) -> Unit,
    onDeleteSection: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    var fabExpanded by remember { mutableStateOf(false) }
    var sectionTypeToCreate by remember { mutableStateOf<SectionType?>(null) }
    var renameTarget by remember { mutableStateOf<ShowcaseSection?>(null) }
    var deleteTarget by remember { mutableStateOf<ShowcaseSection?>(null) }

    ShowcaseBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = company?.name ?: "Company Sections",
                    subtitle = "Manage tab order, visibility, naming, and section creation.",
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
                            title = company?.name ?: "Sections",
                            subtitle = "Every section can be renamed, hidden, reordered, deleted, or expanded with future types.",
                            count = sections.size,
                            itemLabel = "sections",
                        )
                        if (sections.isEmpty()) {
                            AdminEmptyState(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.AddLocationAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                },
                                title = "No sections yet",
                                subtitle = "Open the add menu below to create your first client section.",
                                actionLabel = "Add Section",
                                onAction = { fabExpanded = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                itemsIndexed(sections, key = { _, section -> section.id }) { index, section ->
                                    AdminStaggeredEntrance(index = index) {
                                        CompanySectionItem(
                                            section = section,
                                            index = index,
                                            total = sections.size,
                                            onOpen = { onOpenSection(section.id) },
                                            onRename = { renameTarget = section },
                                            onToggleVisibility = { onToggleSectionVisibility(section) },
                                            onMoveUp = { onMoveSection(section.id, -1) },
                                            onMoveDown = { onMoveSection(section.id, 1) },
                                            onDelete = { deleteTarget = section },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminExpressiveFabMenu(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                actions = V1SectionTypes.map { type ->
                    AdminFabMenuAction(
                        label = type.displayName,
                        icon = type.icon(),
                        onClick = { sectionTypeToCreate = type }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(44.dp),
            )
        }
    }

    sectionTypeToCreate?.let { type ->
        AddSectionNameDialog(
            type = type,
            onDismiss = { sectionTypeToCreate = null },
            onCreate = { title ->
                onCreateSection(type, title)
                sectionTypeToCreate = null
            },
        )
    }

    renameTarget?.let { section ->
        RenameSectionDialog(
            section = section,
            onDismiss = { renameTarget = null },
            onSave = { title ->
                onRenameSection(section, title)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { section ->
        DeleteSectionDialog(
            section = section,
            onDismiss = { deleteTarget = null },
            onDelete = {
                onDeleteSection(section.id)
                deleteTarget = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyCard(
    company: ShowcaseCompany,
    index: Int,
    total: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleVisibility: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.48f)
            .height(204.dp)
            .expressivePressScale(interactionSource, pressedScale = 0.98f),
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color(runCatching { android.graphics.Color.parseColor(company.brandSeedColor) }.getOrDefault(MaterialTheme.colorScheme.primary.toArgb())),
            ) {
                OptimizedAsyncImage(
                    model = company.logoPath.ifBlank { io.gelio.app.R.drawable.gelio_logo },
                    contentDescription = company.name,
                    maxWidth = 56.dp,
                    maxHeight = 56.dp,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(56.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = company.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = onEdit,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit company")
                        }
                        FilledTonalIconButton(
                            onClick = onMoveLeft,
                            enabled = index > 0,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Move company left",
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onMoveRight,
                            enabled = index < total - 1,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Move company right",
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onToggleVisibility,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                imageVector = if (company.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = "Toggle company visibility",
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onDelete,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete company")
                        }
                    }
                }
                Text(
                    text = if (company.hidden) {
                        "Hidden on client. Open this company to manage its sections and content safely."
                    } else {
                        "Visible on client. Open this company to manage its sections, content, and ordering."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Position ${index + 1} • ${company.brandSeedColor}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CompanySectionItem(
    section: ShowcaseSection,
    index: Int,
    total: Int,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onToggleVisibility: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    ElevatedCard(
        onClick = onOpen,
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
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = section.type.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = section.type.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (section.hidden) "Hidden on client" else "Visible on client",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (section.hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onRename, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Rename section")
                }
                FilledTonalIconButton(onClick = onMoveUp, enabled = index > 0, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = "Move section up", modifier = Modifier.size(20.dp))
                }
                FilledTonalIconButton(onClick = onMoveDown, enabled = index < total - 1, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = "Move section down", modifier = Modifier.size(20.dp).padding(0.dp))
                }
                FilledTonalIconButton(onClick = onToggleVisibility, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = if (section.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = "Toggle visibility",
                    )
                }
                FilledTonalIconButton(onClick = onDelete, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete section")
                }
            }
        }
    }
}

@Composable
private fun CompanyEditorDialog(
    company: ShowcaseCompany?,
    onDismiss: () -> Unit,
    onSave: (name: String, logoPath: String, brandSeedColor: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(company?.id) { mutableStateOf(company?.name.orEmpty()) }
    var logoPath by remember(company?.id) { mutableStateOf(company?.logoPath.orEmpty()) }
    var brandSeedColor by remember(company?.id) { mutableStateOf(company?.brandSeedColor ?: "#8D4B68") }
    var importingLogo by remember { mutableStateOf(false) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            importingLogo = true
            runCatching {
                copyContentUriToAppStorage(
                    context = context,
                    uri = uri,
                    folderName = "companies/logos",
                    fallbackExtension = ".png",
                )
            }.onSuccess { importedPath ->
                logoPath = importedPath
            }
            importingLogo = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AdminDialogHeader(
                    title = if (company == null) "Add Company" else "Edit Company",
                    subtitle = "Each company needs a name, a transparent PNG logo, and one seed color. Gelio derives the rest of the palette.",
                    onDismiss = onDismiss,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Company Name") },
                    singleLine = true,
                )
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(92.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                OptimizedAsyncImage(
                                    model = logoPath.ifBlank { io.gelio.app.R.drawable.gelio_logo },
                                    contentDescription = "Company logo preview",
                                    maxWidth = 84.dp,
                                    maxHeight = 84.dp,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(84.dp),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = if (logoPath.isBlank()) "No logo selected yet." else "PNG logo ready.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ShowcasePrimaryActionButton(
                                label = if (importingLogo) "Importing..." else "Pick Transparent PNG",
                                onClick = { if (!importingLogo) logoPicker.launch("image/png") },
                            )
                        }
                    }
                }
                RgbColorPicker(
                    hexColor = brandSeedColor,
                    onColorChange = { brandSeedColor = it }
                )
                AdminDialogActions(
                    saveLabel = if (company == null) "Create Company" else "Save Company",
                    saveEnabled = name.trim().isNotBlank() && logoPath.isNotBlank() && brandSeedColor.trim().isNotBlank() && !importingLogo,
                    onDismiss = onDismiss,
                    onSave = {
                        onSave(
                            name.trim(),
                            logoPath.trim(),
                            brandSeedColor.trim(),
                        )
                    },
                    isLoading = importingLogo,
                )
            }
        }
    }
}

@Composable
private fun DeleteCompanyDialog(
    company: ShowcaseCompany,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.46f)
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AdminDialogHeader(
                    title = "Delete ${company.name}?",
                    subtitle = "This removes the company, all of its sections, and all media owned only by those sections.",
                    onDismiss = onDismiss,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = onDelete) {
                        Text("Delete Permanently")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSectionNameDialog(
    type: SectionType,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.64f)
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AdminDialogHeader(
                    title = "Name your ${type.displayName} Section",
                    subtitle = "Give this new tab a name that the client will see.",
                    onDismiss = onDismiss,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Section Name") },
                    singleLine = true,
                )
                AdminDialogActions(
                    saveLabel = "Create Section",
                    saveEnabled = title.trim().isNotBlank(),
                    onDismiss = onDismiss,
                    onSave = { onCreate(title.trim()) },
                )
            }
        }
    }
}

@Composable
private fun RenameSectionDialog(
    section: ShowcaseSection,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember(section.id) { mutableStateOf(section.title) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AdminDialogHeader(
                    title = "Rename Section",
                    subtitle = "This updates the client tab label and the admin section title.",
                    onDismiss = onDismiss,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Section Name") },
                    singleLine = true,
                )
                AdminDialogActions(
                    saveLabel = "Save",
                    saveEnabled = title.trim().isNotBlank() && title.trim() != section.title,
                    onDismiss = onDismiss,
                    onSave = { onSave(title.trim()) },
                )
            }
        }
    }
}

@Composable
private fun DeleteSectionDialog(
    section: ShowcaseSection,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AdminDialogHeader(
                    title = "Delete ${section.title}?",
                    subtitle = "This permanently deletes the section and everything inside it, including app-private media owned by that section.",
                    onDismiss = onDismiss,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = onDelete) {
                        Text("Delete Permanently")
                    }
                }
            }
        }
    }
}

private val V1SectionTypes = listOf(
    SectionType.IMAGE_GALLERY,
    SectionType.PDF_VIEWER,
    SectionType.YOUTUBE_VIDEOS,
    SectionType.TOUR_360,
    SectionType.DESTINATIONS,
    SectionType.SERVICES,
    SectionType.WORLD_MAP,
    SectionType.GOOGLE_REVIEWS,
    SectionType.CONTENT_PAGE,
    SectionType.ART_GALLERY,
)

private fun SectionType.icon(): ImageVector =
    when (this) {
        SectionType.IMAGE_GALLERY -> Icons.Rounded.BrowseGallery
        SectionType.PDF_VIEWER -> Icons.Rounded.Description
        SectionType.YOUTUBE_VIDEOS -> Icons.Rounded.PlayCircle
        SectionType.TOUR_360 -> Icons.Rounded.TravelExplore
        SectionType.DESTINATIONS -> Icons.Rounded.Map
        SectionType.SERVICES -> Icons.Rounded.RoomService
        SectionType.WORLD_MAP -> Icons.Rounded.Public
        SectionType.GOOGLE_REVIEWS -> Icons.Rounded.StarRate
        SectionType.CONTENT_PAGE -> Icons.Rounded.ViewAgenda
        SectionType.ART_GALLERY -> Icons.Rounded.BrowseGallery
        else -> Icons.Rounded.AddLocationAlt
    }

@Composable
private fun RgbColorPicker(
    hexColor: String,
    onColorChange: (String) -> Unit,
) {
    val fallbackColor = Color(0xFF8D4B68)
    val color = remember(hexColor) {
        try {
            val c = android.graphics.Color.parseColor(if (!hexColor.startsWith("#")) "#$hexColor" else hexColor)
            Color(c)
        } catch (e: Exception) {
            fallbackColor
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = color,
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {}
            OutlinedTextField(
                value = hexColor,
                onValueChange = { 
                    if (it.length <= 9) onColorChange(it.uppercase()) 
                },
                modifier = Modifier.weight(1f),
                label = { Text("Hex Color Code") },
                singleLine = true,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RgbSlider("Red", color.red, Color(0xFFE53935)) { r ->
                val c = Color(r, color.green, color.blue)
                onColorChange(String.format("#%02X%02X%02X", (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()))
            }
            RgbSlider("Green", color.green, Color(0xFF43A047)) { g ->
                val c = Color(color.red, g, color.blue)
                onColorChange(String.format("#%02X%02X%02X", (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()))
            }
            RgbSlider("Blue", color.blue, Color(0xFF1E88E5)) { b ->
                val c = Color(color.red, color.green, b)
                onColorChange(String.format("#%02X%02X%02X", (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()))
            }
        }
    }
}

@Composable
private fun RgbSlider(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = label, modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelMedium)
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.7f),
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}
