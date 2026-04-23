package io.gelio.app.data.model

import androidx.compose.runtime.Immutable
@Immutable
data class ShowcaseCompany(
    val id: String,
    val name: String,
    val logoPath: String,
    val brandSeedColor: String,
    val hidden: Boolean,
    val sortOrder: Int,
)

enum class SectionType(
    val storageKey: String,
    val displayName: String,
) {
    IMAGE_GALLERY("image_gallery", "Image Gallery"),
    CONTENT_PAGE("content_page", "Content Page"),
    ART_GALLERY("art_gallery", "Art Gallery"),
    TOUR_360("tour_360", "360 Tours"),
    YOUTUBE_VIDEOS("youtube_videos", "YouTube Videos"),
    PDF_VIEWER("pdf_viewer", "PDF Viewer"),
    DESTINATIONS("destinations", "Destinations"),
    SERVICES("services", "Services"),
    WORLD_MAP("world_map", "Map"),
    // Reserved extension points for future open-source/custom deployments.
    GOOGLE_REVIEWS("google_reviews", "Ratings"),
    REGION_MAP("region_map", "Region Map");

    companion object {
        fun fromStorageKey(value: String): SectionType =
            entries.firstOrNull { it.storageKey == value } ?: IMAGE_GALLERY
    }
}

@Immutable
data class ShowcaseSection(
    val id: String,
    val companyId: String,
    val type: SectionType,
    val title: String,
    val hidden: Boolean,
    val sortOrder: Int,
)

@Immutable
data class WorldMapSection(
    val sectionId: String,
    val assetName: String,
    val subtitle: String,
    val countryLabel: String,
    val cityLabel: String,
    val viewportCenterX: Float,
    val viewportCenterY: Float,
    val zoomScale: Float,
    val highlightedCountryCodes: List<String>,
)

@Immutable
data class WorldMapPin(
    val id: String,
    val sectionId: String,
    val xNorm: Float,
    val yNorm: Float,
    val label: String,
)
