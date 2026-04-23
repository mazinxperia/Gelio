package io.gelio.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "featured_projects")
data class FeaturedProjectEntity(
    @PrimaryKey val id: String,
    val projectName: String,
    val galleryImages: List<String>,
    val thumbnailUri: String,
    val featured: Boolean,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Entity(tableName = "virtual_tours")
data class VirtualTourEntity(
    @PrimaryKey val id: String,
    val projectName: String,
    val embedUrl: String,
    val thumbnailUri: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Entity(tableName = "videos")
data class ShowcaseVideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val youtubeLink: String,
    val description: String,
    val thumbnailUri: String?,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Entity(tableName = "brochures")
data class BrochureEntity(
    @PrimaryKey val id: String,
    val brand: String,
    val title: String,
    val pdfUri: String,
    val coverThumbnailUri: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val id: String,
    val destinationName: String,
    val imageUri: String,
    val subtitle: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val serviceTitle: String,
    val imageUri: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Entity(tableName = "content_page_cards")
data class ContentPageCardEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val title: String,
    val bodyText: String,
    val imagePath: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "art_gallery_heroes")
data class ArtGalleryHeroEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val title: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "art_gallery_cards")
data class ArtGalleryCardEntity(
    @PrimaryKey val id: String,
    val heroId: String,
    val title: String,
    val bodyText: String,
    val imagePath: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "remote_image_assets")
data class RemoteImageAssetEntity(
    @PrimaryKey val localPath: String,
    val provider: String,
    val providerAssetId: String,
    val sourcePageUrl: String,
    val downloadUrl: String,
    val photographerName: String,
    val photographerUrl: String,
    val importedAt: Long,
)

@Entity(tableName = "global_links")
data class GlobalLinkEntity(
    @PrimaryKey val id: String,
    val brand: String,
    val label: String,
    val url: String,
    val sectionId: String = "",
)

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val paletteContextKey: String,
    val logoPath: String,
    val brandSeedColor: String,
    val hidden: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val type: String,
    val title: String,
    val hidden: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "world_map_sections")
data class WorldMapSectionEntity(
    @PrimaryKey val sectionId: String,
    val assetName: String,
    val subtitle: String,
    val countryLabel: String,
    val cityLabel: String,
    val viewportCenterX: Float,
    val viewportCenterY: Float,
    val zoomScale: Float,
    val highlightedCountryCodes: List<String>,
)

@Entity(tableName = "world_map_pins")
data class WorldMapPinEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val xNorm: Float,
    val yNorm: Float,
    val label: String,
)

@Entity(tableName = "review_cards")
data class ReviewCardEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val reviewerName: String,
    val sourceType: String,
    val subHeading: String,
    val comment: String,
    val rating: Int,
    val hidden: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
