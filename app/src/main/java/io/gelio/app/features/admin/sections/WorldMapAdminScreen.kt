package io.gelio.app.features.admin.sections

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.data.model.WorldMapPin
import io.gelio.app.data.model.WorldMapSection
import io.gelio.app.features.map.MapAssetRegistry
import io.gelio.app.features.map.MapEditorViewport
import io.gelio.app.features.map.MapPosterPlaceholder
import io.gelio.app.features.map.MapRendererMode
import io.gelio.app.features.map.VectorWorldMapAssetData
import io.gelio.app.features.map.WorldMapCountryShape
import io.gelio.app.features.map.WorldMapViewportState
import io.gelio.app.features.map.rememberMapAsset
import java.util.UUID
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldMapAdminScreen(
    section: ShowcaseSection?,
    mapSection: WorldMapSection?,
    pins: List<WorldMapPin>,
    onSave: (WorldMapSection, List<WorldMapPin>) -> Unit,
    onRenameSection: (ShowcaseSection, String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    var sectionTitle by remember(section?.id) { mutableStateOf(section?.title.orEmpty()) }
    var selectedAssetName by remember(section?.id, mapSection) { mutableStateOf(mapSection?.assetName.orEmpty()) }
    var countryLabel by remember(section?.id, mapSection) { mutableStateOf(mapSection?.countryLabel.orEmpty()) }
    var cityLabel by remember(section?.id, mapSection) { mutableStateOf(mapSection?.cityLabel.orEmpty()) }
    var searchQuery by remember(section?.id) { mutableStateOf("") }
    var statusNote by remember(section?.id) { mutableStateOf("Choose a map, frame it, drop pins, then save the preview.") }
    var pinDropMode by remember(section?.id) { mutableStateOf(false) }
    var selectedPinId by remember(section?.id) { mutableStateOf<String?>(null) }
    var assetMenuExpanded by remember(section?.id) { mutableStateOf(false) }
    var viewport by remember(section?.id, mapSection) {
        mutableStateOf(
            normalizeSavedViewport(
                assetName = mapSection?.assetName.orEmpty(),
                viewport = WorldMapViewportState(
                    centerX = mapSection?.viewportCenterX ?: 0.5f,
                    centerY = mapSection?.viewportCenterY ?: 0.5f,
                    zoomScale = mapSection?.zoomScale ?: 1f,
                ),
            ),
        )
    }
    val highlightedCodes = remember(section?.id, mapSection) {
        mutableStateListOf<String>().apply {
            addAll(mapSection?.highlightedCountryCodes.orEmpty())
        }
    }
    val draftPins = remember(section?.id, pins) {
        mutableStateListOf<WorldMapPin>().apply {
            addAll(pins)
        }
    }
    val asset = rememberMapAsset(selectedAssetName)
    val selectedPin = draftPins.firstOrNull { it.id == selectedPinId }
    val selectedAssetDefinition = MapAssetRegistry.resolve(selectedAssetName)

    LaunchedEffect(mapSection?.sectionId) {
        if (mapSection != null) {
            sectionTitle = section?.title.orEmpty()
            selectedAssetName = mapSection.assetName
            countryLabel = mapSection.countryLabel
            cityLabel = mapSection.cityLabel
            viewport = normalizeSavedViewport(
                assetName = mapSection.assetName,
                viewport = WorldMapViewportState(
                    centerX = mapSection.viewportCenterX,
                    centerY = mapSection.viewportCenterY,
                    zoomScale = mapSection.zoomScale,
                ),
            )
            highlightedCodes.clear()
            highlightedCodes.addAll(mapSection.highlightedCountryCodes)
            draftPins.clear()
            draftPins.addAll(pins)
            selectedPinId = draftPins.firstOrNull()?.id
        }
    }

    fun onAssetSelected(assetName: String) {
        if (assetName == selectedAssetName) return
        selectedAssetName = assetName
        viewport = MapAssetRegistry.resolve(assetName)?.defaultViewport ?: WorldMapViewportState(0.5f, 0.5f, 1f)
        highlightedCodes.clear()
        draftPins.clear()
        selectedPinId = null
        pinDropMode = false
        searchQuery = ""
        statusNote = "Selected ${MapAssetRegistry.resolve(assetName)?.displayName ?: "map"}. Previous pins were cleared."
    }

    fun onPinLabelChanged(label: String) {
        val selectedId = selectedPinId ?: return
        val index = draftPins.indexOfFirst { it.id == selectedId }
        if (index >= 0) {
            draftPins[index] = draftPins[index].copy(label = label)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ViewerTopBar(
            title = sectionTitle.ifBlank { section?.title ?: "Map" },
            subtitle = "Offline map section editor",
            onBack = onBack,
            onHome = onHome,
            onClose = onClose,
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = adaptive.contentPaddingHorizontal, vertical = adaptive.contentPaddingVertical),
            horizontalArrangement = Arrangement.spacedBy(tokens.panelGap),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = tokens.adminSidebarWidth)
                    .fillMaxHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 10.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(tokens.headerPadding + adaptive.heroSpacing * 0.5f),
                    verticalArrangement = Arrangement.spacedBy(tokens.panelGap * 0.75f),
                ) {
                    Text(
                        text = "Map",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose the asset, frame the poster, place pins, then save the client preview.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = sectionTitle,
                        onValueChange = { sectionTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Section Title / Client Tab") },
                    )
                    ExposedDropdownMenuBox(
                        expanded = assetMenuExpanded,
                        onExpandedChange = { assetMenuExpanded = !assetMenuExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedAssetDefinition?.displayName.orEmpty(),
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true,
                                )
                                .fillMaxWidth(),
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Choose Map") },
                            placeholder = { Text("Choose map") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetMenuExpanded) },
                        )
                        ExposedDropdownMenu(
                            expanded = assetMenuExpanded,
                            onDismissRequest = { assetMenuExpanded = false },
                        ) {
                            MapAssetRegistry.all.forEach { definition ->
                                DropdownMenuItem(
                                    text = { Text(definition.displayName) },
                                    onClick = {
                                        assetMenuExpanded = false
                                        onAssetSelected(definition.assetName)
                                    },
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = countryLabel,
                            onValueChange = { countryLabel = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Country") },
                        )
                        OutlinedTextField(
                            value = cityLabel,
                            onValueChange = { cityLabel = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("City") },
                        )
                    }
                    if (selectedAssetDefinition?.rendererMode == MapRendererMode.VECTOR_WORLD) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Find Country") },
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    val vectorAsset = asset as? VectorWorldMapAssetData
                                    val country = vectorAsset?.findCountry(searchQuery)
                                    if (country == null) {
                                        statusNote = "No world-map country matched \"$searchQuery\"."
                                    } else {
                                        viewport = vectorAsset.focusFor(country)
                                        toggleCountry(country, highlightedCodes)
                                        statusNote = "${country.name} focused."
                                    }
                                },
                                enabled = asset is VectorWorldMapAssetData && searchQuery.isNotBlank(),
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = "Find country")
                            }
                        }
                    } else {
                        Text(
                            text = "Country search and highlight only work on the current world map. Dubai and Sharjah use framing plus free pins.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selectedPin == null) {
                        Text(
                            text = "Use Add Pin, tap the map once, then tap the dropped pin to rename it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = selectedPin.label,
                                onValueChange = ::onPinLabelChanged,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Pin Label") },
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    draftPins.removeAll { it.id == selectedPin.id }
                                    selectedPinId = draftPins.firstOrNull()?.id
                                    statusNote = "Pin removed."
                                },
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete pin")
                            }
                        }
                    }
                    Text(
                        text = statusNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(adaptive.heroSpacing))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { pinDropMode = !pinDropMode },
                            enabled = asset != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.AddLocationAlt, contentDescription = null)
                            Text(
                                text = if (pinDropMode) "Pin Mode On" else "Add Pin",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Button(
                            onClick = {
                                viewport = MapAssetRegistry.resolve(selectedAssetName)?.defaultViewport
                                    ?: WorldMapViewportState(0.5f, 0.5f, 1f)
                                selectedPinId = null
                                pinDropMode = false
                                statusNote = "Viewport reset."
                            },
                            enabled = asset != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Text("Reset", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Button(
                        onClick = {
                            val resolvedSectionId = section?.id ?: mapSection?.sectionId ?: return@Button
                            val trimmedTitle = sectionTitle.trim()
                            if (section != null && trimmedTitle.isNotBlank() && trimmedTitle != section.title) {
                                onRenameSection(section, trimmedTitle)
                            }
                            onSave(
                                WorldMapSection(
                                    sectionId = resolvedSectionId,
                                    assetName = selectedAssetName,
                                    subtitle = "",
                                    countryLabel = countryLabel.trim(),
                                    cityLabel = cityLabel.trim(),
                                    viewportCenterX = viewport.centerX,
                                    viewportCenterY = viewport.centerY,
                                    zoomScale = viewport.zoomScale,
                                    highlightedCountryCodes = highlightedCodes.toList(),
                                ),
                                draftPins.toList(),
                            )
                            pinDropMode = false
                            statusNote = "Map saved. Client preview updated."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = sectionTitle.trim().isNotBlank() && selectedAssetName.isNotBlank() && asset != null,
                    ) {
                        Text("Save Map")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                when {
                    selectedAssetName.isBlank() -> {
                        MapPosterPlaceholder(
                            countryLabel = countryLabel,
                            cityLabel = cityLabel,
                            message = "Choose a map to start editing.",
                            compactLandscape = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    asset == null -> {
                        MapPosterPlaceholder(
                            countryLabel = countryLabel,
                            cityLabel = cityLabel,
                            message = "Loading selected map...",
                            compactLandscape = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    else -> {
                        MapEditorViewport(
                            asset = asset,
                            highlightedCountryCodes = highlightedCodes.toSet(),
                            pins = draftPins,
                            centerX = viewport.centerX,
                            centerY = viewport.centerY,
                            zoomScale = viewport.zoomScale,
                            countryLabel = countryLabel,
                            cityLabel = cityLabel,
                            compactLandscape = false,
                            modifier = Modifier.fillMaxSize(),
                            interactive = true,
                            pinDropMode = pinDropMode,
                            selectedPinId = selectedPinId,
                            onViewportChange = { viewport = it },
                            onCountryTapped = { country ->
                                toggleCountry(country, highlightedCodes)
                                statusNote = "${country.name} ${if (country.code in highlightedCodes) "highlighted" else "removed"}."
                            },
                            onPinDropped = { x, y ->
                                val pin = WorldMapPin(
                                    id = UUID.randomUUID().toString(),
                                    sectionId = section?.id ?: mapSection?.sectionId.orEmpty(),
                                    xNorm = x,
                                    yNorm = y,
                                    label = "Pin ${draftPins.size + 1}",
                                )
                                draftPins += pin
                                selectedPinId = pin.id
                                pinDropMode = false
                                statusNote = "Pin placed. Edit its label on the left."
                            },
                            onPinTapped = { selectedPinId = it },
                        )
                    }
                }
            }
        }
    }
}

private fun normalizeSavedViewport(
    assetName: String,
    viewport: WorldMapViewportState,
): WorldMapViewportState {
    val definition = MapAssetRegistry.resolve(assetName) ?: return viewport
    val looksLikeLegacyDefault =
        when (assetName) {
            io.gelio.app.features.map.MapAssetPaths.DUBAI ->
                viewport.isNear(centerX = 0.73f, centerY = 0.46f, zoomScale = 1.18f)

            io.gelio.app.features.map.MapAssetPaths.SHARJAH ->
                viewport.isNear(centerX = 0.70f, centerY = 0.46f, zoomScale = 1.16f)

            else -> false
        }
    return if (looksLikeLegacyDefault) definition.defaultViewport else viewport
}

private fun WorldMapViewportState.isNear(
    centerX: Float,
    centerY: Float,
    zoomScale: Float,
): Boolean =
    abs(this.centerX - centerX) <= 0.03f &&
        abs(this.centerY - centerY) <= 0.03f &&
        abs(this.zoomScale - zoomScale) <= 0.06f

private fun toggleCountry(
    country: WorldMapCountryShape,
    highlightedCodes: MutableList<String>,
) {
    if (country.code in highlightedCodes) {
        highlightedCodes.remove(country.code)
    } else {
        highlightedCodes += country.code
    }
}
