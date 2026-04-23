package io.gelio.app.data.mapper

import io.gelio.app.data.local.entity.BrochureEntity
import io.gelio.app.data.local.entity.CompanyEntity
import io.gelio.app.data.local.entity.ArtGalleryCardEntity
import io.gelio.app.data.local.entity.ArtGalleryHeroEntity
import io.gelio.app.data.local.entity.ContentPageCardEntity
import io.gelio.app.data.local.entity.DestinationEntity
import io.gelio.app.data.local.entity.FeaturedProjectEntity
import io.gelio.app.data.local.entity.GlobalLinkEntity
import io.gelio.app.data.local.entity.SectionEntity
import io.gelio.app.data.local.entity.ServiceEntity
import io.gelio.app.data.local.entity.ShowcaseVideoEntity
import io.gelio.app.data.local.entity.ReviewCardEntity
import io.gelio.app.data.local.entity.VirtualTourEntity
import io.gelio.app.data.local.entity.WorldMapPinEntity
import io.gelio.app.data.local.entity.WorldMapSectionEntity
import io.gelio.app.data.model.Brand
import io.gelio.app.data.model.BrandLink
import io.gelio.app.data.model.Brochure
import io.gelio.app.data.model.ArtGalleryCard
import io.gelio.app.data.model.ArtGalleryHero
import io.gelio.app.data.model.ContentPageCard
import io.gelio.app.data.model.Destination
import io.gelio.app.data.model.FeaturedProject
import io.gelio.app.data.model.ReviewCard
import io.gelio.app.data.model.ReviewSource
import io.gelio.app.data.model.SectionType
import io.gelio.app.data.model.Service
import io.gelio.app.data.model.ShowcaseVideo
import io.gelio.app.data.model.ShowcaseCompany
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.data.model.VirtualTour
import io.gelio.app.data.model.WorldMapPin
import io.gelio.app.data.model.WorldMapSection

fun FeaturedProjectEntity.toModel(): FeaturedProject =
    FeaturedProject(
        id = id,
        sectionId = sectionId,
        projectName = projectName,
        galleryImages = galleryImages,
        thumbnailUri = thumbnailUri,
        featured = featured,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun FeaturedProject.toEntity(): FeaturedProjectEntity =
    FeaturedProjectEntity(
        id = id,
        sectionId = sectionId,
        projectName = projectName,
        galleryImages = galleryImages,
        thumbnailUri = thumbnailUri,
        featured = featured,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun VirtualTourEntity.toModel(): VirtualTour =
    VirtualTour(
        id = id,
        sectionId = sectionId,
        projectName = projectName,
        embedUrl = embedUrl,
        thumbnailUri = thumbnailUri,
        description = description,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun VirtualTour.toEntity(): VirtualTourEntity =
    VirtualTourEntity(
        id = id,
        sectionId = sectionId,
        projectName = projectName,
        embedUrl = embedUrl,
        thumbnailUri = thumbnailUri,
        description = description,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun ShowcaseVideoEntity.toModel(): ShowcaseVideo =
    ShowcaseVideo(
        id = id,
        sectionId = sectionId,
        title = title,
        youtubeLink = youtubeLink,
        description = description,
        thumbnailUri = thumbnailUri,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun ShowcaseVideo.toEntity(): ShowcaseVideoEntity =
    ShowcaseVideoEntity(
        id = id,
        sectionId = sectionId,
        title = title,
        youtubeLink = youtubeLink,
        description = description,
        thumbnailUri = thumbnailUri,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun BrochureEntity.toModel(): Brochure =
    Brochure(
        id = id,
        sectionId = sectionId,
        brand = Brand.valueOf(brand),
        title = title,
        pdfUri = pdfUri,
        coverThumbnailUri = coverThumbnailUri,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun Brochure.toEntity(): BrochureEntity =
    BrochureEntity(
        id = id,
        sectionId = sectionId,
        brand = brand.name,
        title = title,
        pdfUri = pdfUri,
        coverThumbnailUri = coverThumbnailUri,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun DestinationEntity.toModel(): Destination =
    Destination(
        id = id,
        sectionId = sectionId,
        destinationName = destinationName,
        imageUri = imageUri,
        subtitle = subtitle,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun Destination.toEntity(): DestinationEntity =
    DestinationEntity(
        id = id,
        sectionId = sectionId,
        destinationName = destinationName,
        imageUri = imageUri,
        subtitle = subtitle,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun ServiceEntity.toModel(): Service =
    Service(
        id = id,
        sectionId = sectionId,
        serviceTitle = serviceTitle,
        imageUri = imageUri,
        description = description,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun Service.toEntity(): ServiceEntity =
    ServiceEntity(
        id = id,
        sectionId = sectionId,
        serviceTitle = serviceTitle,
        imageUri = imageUri,
        description = description,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun ContentPageCardEntity.toModel(): ContentPageCard =
    ContentPageCard(
        id = id,
        sectionId = sectionId,
        title = title,
        bodyText = bodyText,
        imagePath = imagePath,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ContentPageCard.toEntity(): ContentPageCardEntity =
    ContentPageCardEntity(
        id = id,
        sectionId = sectionId,
        title = title,
        bodyText = bodyText,
        imagePath = imagePath,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ArtGalleryHeroEntity.toModel(): ArtGalleryHero =
    ArtGalleryHero(
        id = id,
        sectionId = sectionId,
        title = title,
        description = description,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ArtGalleryHero.toEntity(): ArtGalleryHeroEntity =
    ArtGalleryHeroEntity(
        id = id,
        sectionId = sectionId,
        title = title,
        description = description,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ArtGalleryCardEntity.toModel(): ArtGalleryCard =
    ArtGalleryCard(
        id = id,
        heroId = heroId,
        title = title,
        bodyText = bodyText,
        imagePath = imagePath,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ArtGalleryCard.toEntity(): ArtGalleryCardEntity =
    ArtGalleryCardEntity(
        id = id,
        heroId = heroId,
        title = title,
        bodyText = bodyText,
        imagePath = imagePath,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun GlobalLinkEntity.toModel(): BrandLink =
    BrandLink(
        id = id,
        sectionId = sectionId,
        brand = Brand.valueOf(brand),
        label = label,
        url = url,
    )

fun BrandLink.toEntity(): GlobalLinkEntity =
    GlobalLinkEntity(
        id = id,
        sectionId = sectionId,
        brand = brand.name,
        label = label,
        url = url,
    )

fun CompanyEntity.toModel(): ShowcaseCompany =
    ShowcaseCompany(
        id = id,
        name = name,
        logoPath = logoPath,
        brandSeedColor = brandSeedColor,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun ShowcaseCompany.toEntity(): CompanyEntity =
    CompanyEntity(
        id = id,
        name = name,
        paletteContextKey = "COMPANY",
        logoPath = logoPath,
        brandSeedColor = brandSeedColor,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun SectionEntity.toModel(): ShowcaseSection =
    ShowcaseSection(
        id = id,
        companyId = companyId,
        type = SectionType.fromStorageKey(type),
        title = title,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun ShowcaseSection.toEntity(): SectionEntity =
    SectionEntity(
        id = id,
        companyId = companyId,
        type = type.storageKey,
        title = title,
        hidden = hidden,
        sortOrder = sortOrder,
    )

fun WorldMapSectionEntity.toModel(): WorldMapSection =
    WorldMapSection(
        sectionId = sectionId,
        assetName = assetName,
        subtitle = subtitle,
        countryLabel = countryLabel,
        cityLabel = cityLabel,
        viewportCenterX = viewportCenterX,
        viewportCenterY = viewportCenterY,
        zoomScale = zoomScale,
        highlightedCountryCodes = highlightedCountryCodes,
    )

fun WorldMapSection.toEntity(): WorldMapSectionEntity =
    WorldMapSectionEntity(
        sectionId = sectionId,
        assetName = assetName,
        subtitle = subtitle,
        countryLabel = countryLabel,
        cityLabel = cityLabel,
        viewportCenterX = viewportCenterX,
        viewportCenterY = viewportCenterY,
        zoomScale = zoomScale,
        highlightedCountryCodes = highlightedCountryCodes,
    )

fun WorldMapPinEntity.toModel(): WorldMapPin =
    WorldMapPin(
        id = id,
        sectionId = sectionId,
        xNorm = xNorm,
        yNorm = yNorm,
        label = label,
    )

fun WorldMapPin.toEntity(): WorldMapPinEntity =
    WorldMapPinEntity(
        id = id,
        sectionId = sectionId,
        xNorm = xNorm,
        yNorm = yNorm,
        label = label,
    )

fun ReviewCardEntity.toModel(): ReviewCard =
    ReviewCard(
        id = id,
        sectionId = sectionId,
        reviewerName = reviewerName,
        sourceType = ReviewSource.fromStorageKey(sourceType),
        subHeading = subHeading,
        comment = comment,
        rating = rating,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ReviewCard.toEntity(): ReviewCardEntity =
    ReviewCardEntity(
        id = id,
        sectionId = sectionId,
        reviewerName = reviewerName,
        sourceType = sourceType.storageKey,
        subHeading = subHeading,
        comment = comment,
        rating = rating,
        hidden = hidden,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
