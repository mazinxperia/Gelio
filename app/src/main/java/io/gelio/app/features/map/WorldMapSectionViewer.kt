package io.gelio.app.features.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.gelio.app.core.util.mapPreviewFile
import io.gelio.app.data.model.WorldMapPin
import io.gelio.app.data.model.WorldMapSection

@Composable
fun WorldMapSectionViewer(
    section: WorldMapSection?,
    pins: List<WorldMapPin>,
    sectionTitle: String,
    compactLandscape: Boolean,
) {
    val context = LocalContext.current
    val previewFile = section?.sectionId
        ?.let { mapPreviewFile(context, it) }
        ?.takeIf { it.exists() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compactLandscape) 306.dp else 446.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            section == null -> {
                MapPosterPlaceholder(
                    countryLabel = "",
                    cityLabel = "",
                    message = "Map section not configured yet.",
                    compactLandscape = compactLandscape,
                )
            }

            previewFile != null -> {
                SavedMapPreviewImage(
                    previewFile = previewFile,
                    countryLabel = section.countryLabel,
                    cityLabel = section.cityLabel,
                    compactLandscape = compactLandscape,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
                MapPosterPlaceholder(
                    countryLabel = section.countryLabel,
                    cityLabel = section.cityLabel,
                    message = if (section.assetName.isBlank()) {
                        "Choose a map in admin and save the preview."
                    } else {
                        "Saved preview unavailable. Open admin and save this map."
                    },
                    compactLandscape = compactLandscape,
                )
            }
        }
    }
}
