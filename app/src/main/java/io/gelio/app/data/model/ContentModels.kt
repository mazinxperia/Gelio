package io.gelio.app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class FeaturedProject(
    val id: String,
    val projectName: String,
    val galleryImages: List<String>,
    val thumbnailUri: String,
    val featured: Boolean,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Immutable
data class VirtualTour(
    val id: String,
    val projectName: String,
    val embedUrl: String,
    val thumbnailUri: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Immutable
data class ShowcaseVideo(
    val id: String,
    val title: String,
    val youtubeLink: String,
    val description: String,
    val thumbnailUri: String?,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Immutable
data class Brochure(
    val id: String,
    val brand: Brand,
    val title: String,
    val pdfUri: String,
    val coverThumbnailUri: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Immutable
data class Destination(
    val id: String,
    val destinationName: String,
    val imageUri: String,
    val subtitle: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Immutable
data class Service(
    val id: String,
    val serviceTitle: String,
    val imageUri: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val sectionId: String = "",
)

@Immutable
data class ContentPageCard(
    val id: String,
    val sectionId: String,
    val title: String,
    val bodyText: String,
    val imagePath: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Immutable
data class ArtGalleryHero(
    val id: String,
    val sectionId: String,
    val title: String,
    val description: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Immutable
data class ArtGalleryCard(
    val id: String,
    val heroId: String,
    val title: String,
    val bodyText: String,
    val imagePath: String,
    val hidden: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Immutable
data class ArtGalleryHeroGroup(
    val hero: ArtGalleryHero,
    val cards: List<ArtGalleryCard>,
)

@Immutable
data class BrandLink(
    val id: String,
    val brand: Brand,
    val label: String,
    val url: String,
    val sectionId: String = "",
)
