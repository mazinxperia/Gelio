package io.gelio.app.data.repository

import android.content.Context
import io.gelio.app.core.util.generatePdfCoverThumbnailToAppStorage
import io.gelio.app.core.util.isGeneratedPdfCoverThumbnail
import io.gelio.app.core.util.deleteMapPreview
import io.gelio.app.core.util.localThumbnailForPath
import io.gelio.app.data.local.dao.ShowcaseDao
import io.gelio.app.data.local.entity.ArtGalleryCardEntity
import io.gelio.app.data.local.entity.ArtGalleryHeroEntity
import io.gelio.app.data.local.entity.CompanyEntity
import io.gelio.app.data.local.entity.SectionEntity
import io.gelio.app.data.local.entity.WorldMapPinEntity
import io.gelio.app.data.mapper.toEntity
import io.gelio.app.data.mapper.toModel
import io.gelio.app.data.model.ArtGalleryCard
import io.gelio.app.data.model.ArtGalleryHero
import io.gelio.app.data.model.ArtGalleryHeroGroup
import io.gelio.app.data.model.Brand
import io.gelio.app.data.model.BrandLink
import io.gelio.app.data.model.Brochure
import io.gelio.app.data.model.ContentPageCard
import io.gelio.app.data.model.Destination
import io.gelio.app.data.model.FeaturedProject
import io.gelio.app.data.model.ReviewCard
import io.gelio.app.data.model.SectionType
import io.gelio.app.data.model.Service
import io.gelio.app.data.model.ShowcaseCompany
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.data.model.ShowcaseVideo
import io.gelio.app.data.model.VirtualTour
import io.gelio.app.data.model.WorldMapPin
import io.gelio.app.data.model.WorldMapSection
import io.gelio.app.features.map.MapAssetPaths
import io.gelio.app.features.map.saveMapPreviewToStorage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ShowcaseRepository(
    private val context: Context,
    private val dao: ShowcaseDao,
    private val applicationScope: CoroutineScope,
) {
    private val initializationComplete = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = initializationComplete.asStateFlow()
    private var warmUpJob: Job? = null

    val companies: Flow<List<ShowcaseCompany>> =
        dao.observeVisibleCompanies().map { items -> items.map { it.toModel() } }

    val adminCompanies: Flow<List<ShowcaseCompany>> =
        dao.observeAllCompanies().map { items -> items.map { it.toModel() } }

    // Compatibility helpers retained while the UI transitions to dynamic sections.
    val featuredProjects: Flow<List<FeaturedProject>> = visibleFeaturedProjects(SectionSeed.DESIGN_FEATURED_PROJECTS_ID)
    val adminFeaturedProjects: Flow<List<FeaturedProject>> = adminFeaturedProjects(SectionSeed.DESIGN_FEATURED_PROJECTS_ID)
    val virtualTours: Flow<List<VirtualTour>> = visibleVirtualTours(SectionSeed.DESIGN_VIRTUAL_TOURS_ID)
    val adminVirtualTours: Flow<List<VirtualTour>> = adminVirtualTours(SectionSeed.DESIGN_VIRTUAL_TOURS_ID)
    val videos: Flow<List<ShowcaseVideo>> = visibleVideos(SectionSeed.DESIGN_VIDEOS_ID)
    val adminVideos: Flow<List<ShowcaseVideo>> = adminVideos(SectionSeed.DESIGN_VIDEOS_ID)
    val tourismDestinations: Flow<List<Destination>> = visibleDestinations(SectionSeed.TOURISM_DESTINATIONS_ID)
    val adminDestinations: Flow<List<Destination>> = adminDestinations(SectionSeed.TOURISM_DESTINATIONS_ID)
    val tourismServices: Flow<List<Service>> = visibleServices(SectionSeed.TOURISM_SERVICES_ID)
    val adminServices: Flow<List<Service>> = adminServices(SectionSeed.TOURISM_SERVICES_ID)

    fun designBrochures(): Flow<List<Brochure>> = visibleBrochures(SectionSeed.DESIGN_BROCHURES_ID)
    fun adminDesignBrochures(): Flow<List<Brochure>> = adminBrochures(SectionSeed.DESIGN_BROCHURES_ID)
    fun tourismBrochures(): Flow<List<Brochure>> = visibleBrochures(SectionSeed.TOURISM_BROCHURES_ID)
    fun adminTourismBrochures(): Flow<List<Brochure>> = adminBrochures(SectionSeed.TOURISM_BROCHURES_ID)

    fun sections(companyId: String): Flow<List<ShowcaseSection>> =
        dao.observeVisibleSections(companyId).map { items -> items.map { it.toModel() } }

    fun adminSections(companyId: String): Flow<List<ShowcaseSection>> =
        dao.observeAllSections(companyId).map { items -> items.map { it.toModel() } }

    fun section(sectionId: String): Flow<ShowcaseSection?> =
        dao.observeSection(sectionId).map { it?.toModel() }

    fun company(companyId: String): Flow<ShowcaseCompany?> =
        dao.observeCompany(companyId).map { it?.toModel() }

    suspend fun adminCompanySnapshot(companyId: String): ShowcaseCompany? =
        dao.getAllCompaniesSnapshot().firstOrNull { it.id == companyId }?.toModel()

    suspend fun visibleCompaniesSnapshot(): List<ShowcaseCompany> =
        dao.getAllCompaniesSnapshot()
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminCompaniesSnapshot(): List<ShowcaseCompany> =
        dao.getAllCompaniesSnapshot().map { it.toModel() }

    suspend fun moveCompany(companyId: String, direction: Int) {
        val current = dao.getAllCompaniesSnapshot()
        val sourceIndex = current.indexOfFirst { it.id == companyId }
        if (sourceIndex == -1) return
        val targetIndex = (sourceIndex + direction).coerceIn(0, current.lastIndex)
        if (sourceIndex == targetIndex) return
        val reordered = current.toMutableList().apply { add(targetIndex, removeAt(sourceIndex)) }
        dao.upsertCompanies(reordered.mapIndexed { index, item -> item.copy(sortOrder = index) })
    }

    suspend fun createCompany(
        name: String,
        logoPath: String,
        brandSeedColor: String,
    ): ShowcaseCompany {
        val company = ShowcaseCompany(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Untitled Company" },
            logoPath = logoPath.trim(),
            brandSeedColor = normalizeColorHex(brandSeedColor),
            hidden = false,
            sortOrder = dao.getAllCompaniesSnapshot().size,
        )
        dao.upsertCompany(company.toEntity())
        return company
    }

    suspend fun updateCompany(company: ShowcaseCompany) {
        val existing = dao.getAllCompaniesSnapshot().firstOrNull { it.id == company.id }
        val normalized = company.copy(
            name = company.name.trim().ifBlank { "Untitled Company" },
            logoPath = company.logoPath.trim(),
            brandSeedColor = normalizeColorHex(company.brandSeedColor),
        )
        dao.upsertCompany(normalized.toEntity())
        existing
            ?.logoPath
            ?.takeIf { it.isNotBlank() && it != normalized.logoPath }
            ?.let { deleteUnreferencedMedia(setOf(it)) }
    }

    suspend fun toggleCompanyVisibility(company: ShowcaseCompany) {
        dao.upsertCompany(company.copy(hidden = !company.hidden).toEntity())
    }

    suspend fun deleteCompany(companyId: String) {
        val company = dao.getAllCompaniesSnapshot().firstOrNull { it.id == companyId } ?: return
        val sectionIds = dao.getSectionsSnapshot(companyId).map { it.id }
        for (sectionId in sectionIds) {
            deleteSection(sectionId)
        }
        dao.deleteCompany(companyId)
        normalizeCompanySortOrder()
        company.logoPath.takeIf(String::isNotBlank)?.let { deleteUnreferencedMedia(setOf(it)) }
    }

    suspend fun adminSectionsSnapshot(companyId: String): List<ShowcaseSection> =
        dao.getSectionsSnapshot(companyId).map { it.toModel() }

    suspend fun visibleSectionsSnapshot(companyId: String): List<ShowcaseSection> =
        dao.getSectionsSnapshot(companyId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun sectionSnapshot(sectionId: String): ShowcaseSection? =
        dao.getSectionSnapshot(sectionId)?.toModel()

    fun visibleFeaturedProjects(sectionId: String): Flow<List<FeaturedProject>> =
        dao.observeVisibleFeaturedProjects(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleFeaturedProjectsSnapshot(sectionId: String): List<FeaturedProject> =
        dao.getFeaturedProjectsSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminFeaturedProjectsSnapshot(sectionId: String): List<FeaturedProject> =
        dao.getFeaturedProjectsSnapshot(sectionId).map { it.toModel() }

    fun adminFeaturedProjects(sectionId: String): Flow<List<FeaturedProject>> =
        dao.observeAllFeaturedProjects(sectionId).map { items -> items.map { it.toModel() } }

    fun visibleVirtualTours(sectionId: String): Flow<List<VirtualTour>> =
        dao.observeVisibleVirtualTours(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleVirtualToursSnapshot(sectionId: String): List<VirtualTour> =
        dao.getVirtualToursSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminVirtualToursSnapshot(sectionId: String): List<VirtualTour> =
        dao.getVirtualToursSnapshot(sectionId).map { it.toModel() }

    fun adminVirtualTours(sectionId: String): Flow<List<VirtualTour>> =
        dao.observeAllVirtualTours(sectionId).map { items -> items.map { it.toModel() } }

    fun visibleVideos(sectionId: String): Flow<List<ShowcaseVideo>> =
        dao.observeVisibleVideos(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleVideosSnapshot(sectionId: String): List<ShowcaseVideo> =
        dao.getVideosSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminVideosSnapshot(sectionId: String): List<ShowcaseVideo> =
        dao.getVideosSnapshot(sectionId).map { it.toModel() }

    fun adminVideos(sectionId: String): Flow<List<ShowcaseVideo>> =
        dao.observeAllVideos(sectionId).map { items -> items.map { it.toModel() } }

    fun visibleBrochures(sectionId: String): Flow<List<Brochure>> =
        dao.observeVisibleBrochures(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleBrochuresSnapshot(sectionId: String): List<Brochure> =
        dao.getBrochuresSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminBrochuresSnapshot(sectionId: String): List<Brochure> =
        dao.getBrochuresSnapshot(sectionId).map { it.toModel() }

    fun adminBrochures(sectionId: String): Flow<List<Brochure>> =
        dao.observeAllBrochures(sectionId).map { items -> items.map { it.toModel() } }

    fun visibleDestinations(sectionId: String): Flow<List<Destination>> =
        dao.observeVisibleDestinations(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleDestinationsSnapshot(sectionId: String): List<Destination> =
        dao.getDestinationsSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminDestinationsSnapshot(sectionId: String): List<Destination> =
        dao.getDestinationsSnapshot(sectionId).map { it.toModel() }

    fun adminDestinations(sectionId: String): Flow<List<Destination>> =
        dao.observeAllDestinations(sectionId).map { items -> items.map { it.toModel() } }

    fun visibleServices(sectionId: String): Flow<List<Service>> =
        dao.observeVisibleServices(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleServicesSnapshot(sectionId: String): List<Service> =
        dao.getServicesSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminServicesSnapshot(sectionId: String): List<Service> =
        dao.getServicesSnapshot(sectionId).map { it.toModel() }

    fun adminServices(sectionId: String): Flow<List<Service>> =
        dao.observeAllServices(sectionId).map { items -> items.map { it.toModel() } }

    fun sectionLinks(sectionId: String): Flow<List<BrandLink>> =
        dao.observeGlobalLinks(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun sectionLinksSnapshot(sectionId: String): List<BrandLink> =
        dao.getGlobalLinksSnapshot(sectionId).map { it.toModel() }

    fun allProjectsLink(sectionId: String): Flow<BrandLink?> =
        dao.observeGlobalLinks(sectionId).map { links ->
            links.firstOrNull {
                it.label.equals("All Items", ignoreCase = true) ||
                    it.label.equals("All Projects", ignoreCase = true)
            }?.toModel()
        }

    fun worldMapSection(sectionId: String): Flow<WorldMapSection?> =
        dao.observeWorldMapSection(sectionId).map { it?.toModel() }

    suspend fun worldMapSectionSnapshot(sectionId: String): WorldMapSection? =
        dao.getWorldMapSectionSnapshot(sectionId)?.toModel()

    fun worldMapPins(sectionId: String): Flow<List<WorldMapPin>> =
        dao.observeWorldMapPins(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun worldMapPinsSnapshot(sectionId: String): List<WorldMapPin> =
        dao.getWorldMapPinsSnapshot(sectionId).map { it.toModel() }

    fun visibleReviewCards(sectionId: String): Flow<List<ReviewCard>> =
        dao.observeVisibleReviewCards(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleReviewCardsSnapshot(sectionId: String): List<ReviewCard> =
        dao.getReviewCardsSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminReviewCardsSnapshot(sectionId: String): List<ReviewCard> =
        dao.getReviewCardsSnapshot(sectionId).map { it.toModel() }

    fun adminReviewCards(sectionId: String): Flow<List<ReviewCard>> =
        dao.observeAllReviewCards(sectionId).map { items -> items.map { it.toModel() } }

    fun visibleContentPageCards(sectionId: String): Flow<List<ContentPageCard>> =
        dao.observeVisibleContentPageCards(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleContentPageCardsSnapshot(sectionId: String): List<ContentPageCard> =
        dao.getContentPageCardsSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminContentPageCardsSnapshot(sectionId: String): List<ContentPageCard> =
        dao.getContentPageCardsSnapshot(sectionId).map { it.toModel() }

    fun adminContentPageCards(sectionId: String): Flow<List<ContentPageCard>> =
        dao.observeAllContentPageCards(sectionId).map { items -> items.map { it.toModel() } }

    fun visibleArtGalleryHeroes(sectionId: String): Flow<List<ArtGalleryHero>> =
        dao.observeVisibleArtGalleryHeroes(sectionId).map { items -> items.map { it.toModel() } }

    suspend fun visibleArtGalleryHeroesSnapshot(sectionId: String): List<ArtGalleryHero> =
        dao.getArtGalleryHeroesSnapshot(sectionId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminArtGalleryHeroesSnapshot(sectionId: String): List<ArtGalleryHero> =
        dao.getArtGalleryHeroesSnapshot(sectionId).map { it.toModel() }

    fun adminArtGalleryHeroes(sectionId: String): Flow<List<ArtGalleryHero>> =
        dao.observeAllArtGalleryHeroes(sectionId).map { items -> items.map { it.toModel() } }

    fun artGalleryHero(heroId: String): Flow<ArtGalleryHero?> =
        dao.observeArtGalleryHero(heroId).map { it?.toModel() }

    suspend fun artGalleryHeroSnapshot(heroId: String): ArtGalleryHero? =
        dao.getArtGalleryHeroSnapshot(heroId)?.toModel()

    fun visibleArtGalleryCards(heroId: String): Flow<List<ArtGalleryCard>> =
        dao.observeVisibleArtGalleryCards(heroId).map { items -> items.map { it.toModel() } }

    suspend fun visibleArtGalleryCardsSnapshot(heroId: String): List<ArtGalleryCard> =
        dao.getArtGalleryCardsSnapshot(heroId)
            .filterNot { it.hidden }
            .map { it.toModel() }

    suspend fun adminArtGalleryCardsSnapshot(heroId: String): List<ArtGalleryCard> =
        dao.getArtGalleryCardsSnapshot(heroId).map { it.toModel() }

    fun adminArtGalleryCards(heroId: String): Flow<List<ArtGalleryCard>> =
        dao.observeAllArtGalleryCards(heroId).map { items -> items.map { it.toModel() } }

    fun visibleArtGalleryGroups(sectionId: String): Flow<List<ArtGalleryHeroGroup>> =
        combine(
            dao.observeVisibleArtGalleryHeroes(sectionId),
            dao.observeVisibleArtGalleryCardsForSection(sectionId),
        ) { heroes, cards ->
            heroes.map { hero ->
                ArtGalleryHeroGroup(
                    hero = hero.toModel(),
                    cards = cards.filter { it.heroId == hero.id }.map { it.toModel() },
                )
            }
        }

    suspend fun visibleArtGalleryGroupsSnapshot(sectionId: String): List<ArtGalleryHeroGroup> {
        val heroes = dao.getArtGalleryHeroesSnapshot(sectionId)
            .filterNot { it.hidden }
        val cardsByHero = dao.getVisibleArtGalleryCardsForSectionSnapshot(sectionId)
            .groupBy { it.heroId }
        return heroes.map { hero ->
            ArtGalleryHeroGroup(
                hero = hero.toModel(),
                cards = cardsByHero[hero.id].orEmpty().map { it.toModel() },
            )
        }
    }

    fun featuredProject(id: String): Flow<FeaturedProject?> = dao.observeFeaturedProject(id).map { it?.toModel() }
    fun virtualTour(id: String): Flow<VirtualTour?> = dao.observeVirtualTour(id).map { it?.toModel() }
    fun video(id: String): Flow<ShowcaseVideo?> = dao.observeVideo(id).map { it?.toModel() }
    fun brochure(id: String): Flow<Brochure?> = dao.observeBrochure(id).map { it?.toModel() }

    suspend fun featuredProjectSnapshot(id: String): FeaturedProject? =
        dao.getAllFeaturedProjectsSnapshot().firstOrNull { it.id == id }?.toModel()

    suspend fun virtualTourSnapshot(id: String): VirtualTour? =
        dao.getAllVirtualToursSnapshot().firstOrNull { it.id == id }?.toModel()

    suspend fun videoSnapshot(id: String): ShowcaseVideo? =
        dao.getAllVideosSnapshot().firstOrNull { it.id == id }?.toModel()

    suspend fun brochureSnapshot(id: String): Brochure? =
        dao.getAllBrochuresSnapshot().firstOrNull { it.id == id }?.toModel()

    fun warmUp() {
        if (initializationComplete.value) return
        if (warmUpJob?.isActive == true) return
        warmUpJob = applicationScope.launch {
            runCatching { /* Gelio starts blank; no branded seed data. */ }
            initializationComplete.value = true
        }
    }

    suspend fun seedIfEmpty() = Unit

    suspend fun ensureDefaultCompaniesPresent() = Unit

    suspend fun createSection(companyId: String, type: SectionType, title: String): ShowcaseSection {
        val nextSortOrder = dao.getSectionsSnapshot(companyId).size
        val section = ShowcaseSection(
            id = UUID.randomUUID().toString(),
            companyId = companyId,
            type = type,
            title = title,
            hidden = false,
            sortOrder = nextSortOrder,
        )
        dao.upsertSection(section.toEntity())
        if (type == SectionType.WORLD_MAP) {
            dao.upsertWorldMapSection(
                WorldMapSection(
                    sectionId = section.id,
                    assetName = "",
                    subtitle = "",
                    countryLabel = "",
                    cityLabel = "",
                    viewportCenterX = 0.5f,
                    viewportCenterY = 0.5f,
                    zoomScale = 1f,
                    highlightedCountryCodes = emptyList(),
                ).toEntity(),
            )
        }
        return section
    }

    suspend fun renameSection(section: ShowcaseSection, newTitle: String) {
        dao.upsertSection(section.copy(title = newTitle.trim()).toEntity())
    }

    suspend fun toggleSectionVisibility(section: ShowcaseSection) {
        dao.upsertSection(section.copy(hidden = !section.hidden).toEntity())
    }

    suspend fun moveSection(sectionId: String, direction: Int) {
        val section = dao.getSectionSnapshot(sectionId) ?: return
        val current = dao.getSectionsSnapshot(section.companyId)
        val sourceIndex = current.indexOfFirst { it.id == sectionId }
        if (sourceIndex == -1) return
        val targetIndex = (sourceIndex + direction).coerceIn(0, current.lastIndex)
        if (sourceIndex == targetIndex) return
        val reordered = current.toMutableList().apply { add(targetIndex, removeAt(sourceIndex)) }
        dao.upsertSections(reordered.mapIndexed { index, item -> item.copy(sortOrder = index) })
    }

    suspend fun deleteSection(sectionId: String) {
        val companyId = dao.getSectionSnapshot(sectionId)?.companyId
        val deletedMedia = mutableSetOf<String>()
        deletedMedia += dao.getFeaturedProjectsSnapshot(sectionId).flatMap { it.galleryImages + listOf(it.thumbnailUri) }
        deletedMedia += dao.getVirtualToursSnapshot(sectionId).map { it.thumbnailUri }
        deletedMedia += dao.getVideosSnapshot(sectionId).mapNotNull { it.thumbnailUri }
        deletedMedia += dao.getBrochuresSnapshot(sectionId).flatMap { listOf(it.pdfUri, it.coverThumbnailUri) }
        deletedMedia += dao.getDestinationsSnapshot(sectionId).map { it.imageUri }
        deletedMedia += dao.getServicesSnapshot(sectionId).map { it.imageUri }
        deletedMedia += dao.getContentPageCardsSnapshot(sectionId).map { it.imagePath }
        deletedMedia += dao.getArtGalleryCardsForSectionSnapshot(sectionId).map { it.imagePath }

        dao.deleteWorldMapPinsForSection(sectionId)
        dao.deleteWorldMapSection(sectionId)
        dao.deleteReviewCardsForSection(sectionId)
        dao.deleteContentPageCardsForSection(sectionId)
        dao.deleteArtGalleryCardsForSection(sectionId)
        dao.deleteArtGalleryHeroesForSection(sectionId)
        dao.deleteFeaturedProjectsForSection(sectionId)
        dao.deleteVirtualToursForSection(sectionId)
        dao.deleteVideosForSection(sectionId)
        dao.deleteBrochuresForSection(sectionId)
        dao.deleteDestinationsForSection(sectionId)
        dao.deleteServicesForSection(sectionId)
        dao.deleteGlobalLinksForSection(sectionId)
        dao.deleteSection(sectionId)
        companyId?.let { normalizeSectionSortOrder(it) }
        deleteMapPreview(context, sectionId)
        deleteUnreferencedMedia(deletedMedia)
    }

    suspend fun saveService(service: Service) {
        val nextSortOrder = service.sortOrder.takeIf { it >= 0 } ?: dao.getServicesSnapshot(service.sectionId).size
        dao.upsertService(
            service.copy(
                id = service.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
            ).toEntity(),
        )
    }

    suspend fun saveDestination(destination: Destination) {
        val nextSortOrder = destination.sortOrder.takeIf { it >= 0 } ?: dao.getDestinationsSnapshot(destination.sectionId).size
        dao.upsertDestination(
            destination.copy(
                id = destination.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
            ).toEntity(),
        )
    }

    suspend fun deleteDestination(id: String) {
        val sectionId = findDestinationSectionId(id)
        dao.deleteDestination(id)
        if (sectionId != null) normalizeDestinationSortOrder(sectionId)
    }

    suspend fun toggleDestinationVisibility(destination: Destination) {
        dao.upsertDestination(destination.copy(hidden = !destination.hidden).toEntity())
    }

    suspend fun moveDestination(id: String, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllDestinationsSnapshot(), dao::upsertDestinations)
    }

    suspend fun saveFeaturedProject(project: FeaturedProject) {
        val nextSortOrder = project.sortOrder.takeIf { it >= 0 } ?: dao.getFeaturedProjectsSnapshot(project.sectionId).size
        dao.upsertFeaturedProject(
            project.copy(
                id = project.id.ifBlank { UUID.randomUUID().toString() },
                featured = true,
                sortOrder = nextSortOrder,
            ).toEntity(),
        )
    }

    suspend fun deleteFeaturedProject(id: String) {
        val sectionId = findFeaturedProjectSectionId(id)
        dao.deleteFeaturedProject(id)
        if (sectionId != null) normalizeFeaturedProjectSortOrder(sectionId)
    }

    suspend fun toggleFeaturedProjectVisibility(project: FeaturedProject) {
        dao.upsertFeaturedProject(project.copy(hidden = !project.hidden).toEntity())
    }

    suspend fun moveFeaturedProject(id: String, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllFeaturedProjectsSnapshot(), dao::upsertFeaturedProjects)
    }

    suspend fun saveVirtualTour(tour: VirtualTour) {
        val nextSortOrder = tour.sortOrder.takeIf { it >= 0 } ?: dao.getVirtualToursSnapshot(tour.sectionId).size
        dao.upsertVirtualTour(
            tour.copy(
                id = tour.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
            ).toEntity(),
        )
    }

    suspend fun deleteVirtualTour(id: String) {
        val sectionId = findVirtualTourSectionId(id)
        dao.deleteVirtualTour(id)
        if (sectionId != null) normalizeVirtualTourSortOrder(sectionId)
    }

    suspend fun toggleVirtualTourVisibility(tour: VirtualTour) {
        dao.upsertVirtualTour(tour.copy(hidden = !tour.hidden).toEntity())
    }

    suspend fun moveVirtualTour(id: String, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllVirtualToursSnapshot(), dao::upsertVirtualTours)
    }

    suspend fun saveVideo(video: ShowcaseVideo) {
        val nextSortOrder = video.sortOrder.takeIf { it >= 0 } ?: dao.getVideosSnapshot(video.sectionId).size
        dao.upsertVideo(
            video.copy(
                id = video.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
            ).toEntity(),
        )
    }

    suspend fun deleteVideo(id: String) {
        val sectionId = findVideoSectionId(id)
        dao.deleteVideo(id)
        if (sectionId != null) normalizeVideoSortOrder(sectionId)
    }

    suspend fun toggleVideoVisibility(video: ShowcaseVideo) {
        dao.upsertVideo(video.copy(hidden = !video.hidden).toEntity())
    }

    suspend fun moveVideo(id: String, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllVideosSnapshot(), dao::upsertVideos)
    }

    suspend fun saveBrochure(brochure: Brochure) {
        val nextSortOrder = brochure.sortOrder.takeIf { it >= 0 } ?: dao.getBrochuresSnapshot(brochure.sectionId).size
        val resolvedCoverThumbnail =
            if (brochure.coverThumbnailUri.isBlank()) {
                generatePdfCoverThumbnailToAppStorage(
                    context = context,
                    pdfPath = brochure.pdfUri,
                ).orEmpty()
            } else if (isGeneratedPdfCoverThumbnail(brochure.coverThumbnailUri)) {
                generatePdfCoverThumbnailToAppStorage(
                    context = context,
                    pdfPath = brochure.pdfUri,
                ) ?: brochure.coverThumbnailUri
            } else {
                brochure.coverThumbnailUri
            }
        dao.upsertBrochure(
            brochure.copy(
                id = brochure.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
                brand = brochureBrandForSection(brochure.sectionId),
                coverThumbnailUri = resolvedCoverThumbnail,
            ).toEntity(),
        )
    }

    suspend fun saveSectionLink(
        sectionId: String,
        label: String,
        url: String,
    ) {
        val trimmedLabel = label.trim()
        require(trimmedLabel.isNotBlank()) { "Link label cannot be blank." }
        val trimmedUrl = url.trim()
        val existing = dao.getGlobalLinksSnapshot(sectionId)
            .firstOrNull {
                if (trimmedLabel.equals("All Items", ignoreCase = true)) {
                    it.label.equals("All Items", ignoreCase = true) ||
                        it.label.equals("All Projects", ignoreCase = true)
                } else {
                    it.label.equals(trimmedLabel, ignoreCase = true)
                }
            }
        if (trimmedUrl.isBlank()) {
            existing?.let { dao.deleteGlobalLink(it.id) }
            return
        }
        dao.upsertGlobalLink(
            BrandLink(
                id = existing?.id ?: UUID.randomUUID().toString(),
                sectionId = sectionId,
                brand = Brand.GENERIC,
                label = trimmedLabel,
                url = trimmedUrl,
            ).toEntity(),
        )
    }

    suspend fun deleteSectionLink(id: String) {
        dao.deleteGlobalLink(id)
    }

    suspend fun deleteBrochure(id: String, brand: Brand) {
        val sectionId = findBrochureSectionId(id)
        dao.deleteBrochure(id)
        if (sectionId != null) normalizeBrochureSortOrder(sectionId)
    }

    suspend fun toggleBrochureVisibility(brochure: Brochure) {
        dao.upsertBrochure(brochure.copy(hidden = !brochure.hidden).toEntity())
    }

    suspend fun moveBrochure(id: String, brand: Brand, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllBrochuresSnapshot(), dao::upsertBrochures)
    }

    suspend fun deleteService(id: String) {
        val sectionId = findServiceSectionId(id)
        dao.deleteService(id)
        if (sectionId != null) normalizeServiceSortOrder(sectionId)
    }

    suspend fun toggleServiceVisibility(service: Service) {
        dao.upsertService(service.copy(hidden = !service.hidden).toEntity())
    }

    suspend fun moveService(id: String, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllServicesSnapshot(), dao::upsertServices)
    }

    suspend fun saveWorldMap(section: WorldMapSection, pins: List<WorldMapPin>) {
        saveMapPreviewToStorage(context, section, pins)
        dao.upsertWorldMapSection(section.toEntity())
        dao.deleteWorldMapPinsForSection(section.sectionId)
        dao.upsertWorldMapPins(pins.map { it.toEntity() })
    }

    suspend fun deleteWorldMapPin(sectionId: String, pinId: String) {
        val remaining = dao.getWorldMapPinsSnapshot(sectionId).filterNot { it.id == pinId }
        dao.deleteWorldMapPinsForSection(sectionId)
        dao.upsertWorldMapPins(remaining)
    }

    suspend fun saveReviewCard(card: ReviewCard) {
        val now = System.currentTimeMillis()
        val currentItems = dao.getReviewCardsSnapshot(card.sectionId)
        val existing = currentItems.firstOrNull { it.id == card.id }
        val nextSortOrder = card.sortOrder.takeIf { it >= 0 } ?: currentItems.size
        dao.upsertReviewCard(
            card.copy(
                id = card.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
                createdAt = existing?.createdAt ?: card.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
            ).toEntity(),
        )
    }

    suspend fun deleteReviewCard(id: String) {
        val sectionId = findReviewCardSectionId(id)
        dao.deleteReviewCard(id)
        if (sectionId != null) normalizeReviewCardSortOrder(sectionId)
    }

    suspend fun toggleReviewCardVisibility(card: ReviewCard) {
        dao.upsertReviewCard(card.copy(hidden = !card.hidden, updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun moveReviewCard(id: String, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllReviewCardsSnapshot(), dao::upsertReviewCards)
    }

    suspend fun saveContentPageCard(card: ContentPageCard) {
        val now = System.currentTimeMillis()
        val currentItems = dao.getContentPageCardsSnapshot(card.sectionId)
        val existing = currentItems.firstOrNull { it.id == card.id }
        val nextSortOrder = card.sortOrder.takeIf { it >= 0 } ?: currentItems.size
        dao.upsertContentPageCard(
            card.copy(
                id = card.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
                createdAt = existing?.createdAt ?: card.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
            ).toEntity(),
        )
        if (existing != null && existing.imagePath.isNotBlank() && existing.imagePath != card.imagePath) {
            deleteUnreferencedMedia(setOf(existing.imagePath))
        }
    }

    suspend fun deleteContentPageCard(id: String) {
        val existing = dao.getAllContentPageCardsSnapshot().firstOrNull { it.id == id }
        dao.deleteContentPageCard(id)
        existing?.sectionId?.let { normalizeContentPageCardSortOrder(it) }
        existing?.imagePath?.takeIf(String::isNotBlank)?.let { deleteUnreferencedMedia(setOf(it)) }
    }

    suspend fun toggleContentPageCardVisibility(card: ContentPageCard) {
        dao.upsertContentPageCard(card.copy(hidden = !card.hidden, updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun moveContentPageCard(id: String, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllContentPageCardsSnapshot(), dao::upsertContentPageCards)
    }

    suspend fun saveArtGalleryHero(hero: ArtGalleryHero) {
        val now = System.currentTimeMillis()
        val currentItems = dao.getArtGalleryHeroesSnapshot(hero.sectionId)
        val existing = currentItems.firstOrNull { it.id == hero.id }
        val nextSortOrder = hero.sortOrder.takeIf { it >= 0 } ?: currentItems.size
        dao.upsertArtGalleryHero(
            hero.copy(
                id = hero.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
                createdAt = existing?.createdAt ?: hero.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
            ).toEntity(),
        )
    }

    suspend fun deleteArtGalleryHero(id: String) {
        val existingHero = dao.getAllArtGalleryHeroesSnapshot().firstOrNull { it.id == id } ?: return
        val cardImages = dao.getArtGalleryCardsSnapshot(id).map { it.imagePath }.filter(String::isNotBlank)
        dao.deleteArtGalleryCardsForHero(id)
        dao.deleteArtGalleryHero(id)
        normalizeArtGalleryHeroSortOrder(existingHero.sectionId)
        deleteUnreferencedMedia(cardImages.toSet())
    }

    suspend fun toggleArtGalleryHeroVisibility(hero: ArtGalleryHero) {
        dao.upsertArtGalleryHero(hero.copy(hidden = !hero.hidden, updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun moveArtGalleryHero(id: String, direction: Int) {
        moveSectionScoped(id, direction, dao.getAllArtGalleryHeroesSnapshot(), dao::upsertArtGalleryHeroes)
    }

    suspend fun saveArtGalleryCard(card: ArtGalleryCard) {
        val now = System.currentTimeMillis()
        val currentItems = dao.getArtGalleryCardsSnapshot(card.heroId)
        val existing = currentItems.firstOrNull { it.id == card.id }
        val nextSortOrder = card.sortOrder.takeIf { it >= 0 } ?: currentItems.size
        dao.upsertArtGalleryCard(
            card.copy(
                id = card.id.ifBlank { UUID.randomUUID().toString() },
                sortOrder = nextSortOrder,
                createdAt = existing?.createdAt ?: card.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
            ).toEntity(),
        )
        if (existing != null && existing.imagePath.isNotBlank() && existing.imagePath != card.imagePath) {
            deleteUnreferencedMedia(setOf(existing.imagePath))
        }
    }

    suspend fun deleteArtGalleryCard(id: String) {
        val existing = dao.getAllArtGalleryCardsSnapshot().firstOrNull { it.id == id } ?: return
        dao.deleteArtGalleryCard(id)
        normalizeArtGalleryCardSortOrder(existing.heroId)
        existing.imagePath.takeIf(String::isNotBlank)?.let { deleteUnreferencedMedia(setOf(it)) }
    }

    suspend fun toggleArtGalleryCardVisibility(card: ArtGalleryCard) {
        dao.upsertArtGalleryCard(card.copy(hidden = !card.hidden, updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun moveArtGalleryCard(id: String, direction: Int) {
        val current = dao.getAllArtGalleryCardsSnapshot()
        val target = current.firstOrNull { it.id == id } ?: return
        val scoped = current.filter { it.heroId == target.heroId }
        val sourceIndex = scoped.indexOfFirst { it.id == id }
        if (sourceIndex == -1) return
        val targetIndex = (sourceIndex + direction).coerceIn(0, scoped.lastIndex)
        if (sourceIndex == targetIndex) return
        val reorderedScoped = scoped.toMutableList().apply {
            add(targetIndex, removeAt(sourceIndex))
        }.mapIndexed { index, item -> item.copy(sortOrder = index) }
        val byId = reorderedScoped.associateBy { it.id }
        dao.upsertArtGalleryCards(current.map { item -> byId[item.id] ?: item })
    }

    private suspend fun applyLegacyDefaults() = Unit

    private suspend fun normalizeCompanySortOrder() {
        dao.upsertCompanies(
            dao.getAllCompaniesSnapshot().mapIndexed { index, item ->
                item.copy(sortOrder = index)
            },
        )
    }

    private suspend fun normalizeSectionSortOrder(companyId: String) {
        dao.upsertSections(dao.getSectionsSnapshot(companyId).mapIndexed { index, item -> item.copy(sortOrder = index) })
    }

    private suspend fun normalizeServiceSortOrder(sectionId: String) {
        dao.upsertServices(dao.getServicesSnapshot(sectionId).mapIndexed { index, entity -> entity.copy(sortOrder = index) })
    }

    private suspend fun normalizeDestinationSortOrder(sectionId: String) {
        dao.upsertDestinations(dao.getDestinationsSnapshot(sectionId).mapIndexed { index, entity -> entity.copy(sortOrder = index) })
    }

    private suspend fun normalizeFeaturedProjectSortOrder(sectionId: String) {
        dao.upsertFeaturedProjects(dao.getFeaturedProjectsSnapshot(sectionId).mapIndexed { index, entity -> entity.copy(sortOrder = index) })
    }

    private suspend fun normalizeVirtualTourSortOrder(sectionId: String) {
        dao.upsertVirtualTours(dao.getVirtualToursSnapshot(sectionId).mapIndexed { index, entity -> entity.copy(sortOrder = index) })
    }

    private suspend fun normalizeVideoSortOrder(sectionId: String) {
        dao.upsertVideos(dao.getVideosSnapshot(sectionId).mapIndexed { index, entity -> entity.copy(sortOrder = index) })
    }

    private suspend fun normalizeBrochureSortOrder(sectionId: String) {
        dao.upsertBrochures(dao.getBrochuresSnapshot(sectionId).mapIndexed { index, entity -> entity.copy(sortOrder = index) })
    }

    private suspend fun normalizeReviewCardSortOrder(sectionId: String) {
        dao.upsertReviewCards(
            dao.getReviewCardsSnapshot(sectionId).mapIndexed { index, entity ->
                entity.copy(sortOrder = index)
            },
        )
    }

    private suspend fun normalizeContentPageCardSortOrder(sectionId: String) {
        dao.upsertContentPageCards(
            dao.getContentPageCardsSnapshot(sectionId).mapIndexed { index, entity ->
                entity.copy(sortOrder = index)
            },
        )
    }

    private suspend fun normalizeArtGalleryHeroSortOrder(sectionId: String) {
        dao.upsertArtGalleryHeroes(
            dao.getArtGalleryHeroesSnapshot(sectionId).mapIndexed { index, entity ->
                entity.copy(sortOrder = index)
            },
        )
    }

    private suspend fun normalizeArtGalleryCardSortOrder(heroId: String) {
        dao.upsertArtGalleryCards(
            dao.getArtGalleryCardsSnapshot(heroId).mapIndexed { index, entity ->
                entity.copy(sortOrder = index)
            },
        )
    }

    private suspend fun findFeaturedProjectSectionId(id: String): String? =
        dao.getAllFeaturedProjectsSnapshot().firstOrNull { it.id == id }?.sectionId

    private suspend fun findVirtualTourSectionId(id: String): String? =
        dao.getAllVirtualToursSnapshot().firstOrNull { it.id == id }?.sectionId

    private suspend fun findVideoSectionId(id: String): String? =
        dao.getAllVideosSnapshot().firstOrNull { it.id == id }?.sectionId

    private suspend fun findBrochureSectionId(id: String): String? =
        dao.getAllBrochuresSnapshot().firstOrNull { it.id == id }?.sectionId

    private suspend fun findDestinationSectionId(id: String): String? =
        dao.getAllDestinationsSnapshot().firstOrNull { it.id == id }?.sectionId

    private suspend fun findServiceSectionId(id: String): String? =
        dao.getAllServicesSnapshot().firstOrNull { it.id == id }?.sectionId

    private suspend fun findReviewCardSectionId(id: String): String? =
        dao.getAllReviewCardsSnapshot().firstOrNull { it.id == id }?.sectionId

    private suspend fun findContentPageCardSectionId(id: String): String? =
        dao.getAllContentPageCardsSnapshot().firstOrNull { it.id == id }?.sectionId

    private suspend fun brochureBrandForSection(sectionId: String): Brand = Brand.GENERIC

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> moveSectionScoped(
        id: String,
        direction: Int,
        current: List<T>,
        upsert: suspend (List<T>) -> Unit,
    ) {
        val sourceIndex = current.indexOfFirst { it.stableId == id }
        if (sourceIndex == -1) return
        val sectionId = current[sourceIndex].sectionId
        val scoped = current.filter { it.sectionId == sectionId }
        val scopedSourceIndex = scoped.indexOfFirst { it.stableId == id }
        if (scopedSourceIndex == -1) return
        val targetIndex = (scopedSourceIndex + direction).coerceIn(0, scoped.lastIndex)
        if (scopedSourceIndex == targetIndex) return
        val reorderedScoped = scoped.toMutableList().apply {
            add(targetIndex, removeAt(scopedSourceIndex))
        }.mapIndexed { index, item -> item.withSortOrder(index) as T }

        val byId = reorderedScoped.associateBy { it.stableId }
        upsert(
            current.map { item ->
                byId[item.stableId] ?: item
            },
        )
    }

    private suspend fun deleteUnreferencedMedia(candidatePaths: Set<String>) {
        if (candidatePaths.isEmpty()) return
        val remainingPaths = buildSet {
            dao.getAllCompaniesSnapshot().forEach { add(it.logoPath) }
            dao.getAllFeaturedProjectsSnapshot().forEach { addAll(it.galleryImages); add(it.thumbnailUri) }
            dao.getAllVirtualToursSnapshot().forEach { add(it.thumbnailUri) }
            dao.getAllVideosSnapshot().forEach { it.thumbnailUri?.let(::add) }
            dao.getAllBrochuresSnapshot().forEach { add(it.pdfUri); add(it.coverThumbnailUri) }
            dao.getAllDestinationsSnapshot().forEach { add(it.imageUri) }
            dao.getAllServicesSnapshot().forEach { add(it.imageUri) }
            dao.getAllContentPageCardsSnapshot().forEach { add(it.imagePath) }
            dao.getAllArtGalleryCardsSnapshot().forEach { add(it.imagePath) }
        }
        val removedPaths = candidatePaths
            .filter { it.isNotBlank() && it !in remainingPaths }
        removedPaths
            .map(::File)
            .filter { it.exists() }
            .forEach { file ->
                runCatching { localThumbnailForPath(context, file.absolutePath) }
                    .getOrNull()
                    ?.let(::File)
                    ?.takeIf(File::exists)
                    ?.let { thumbnail -> runCatching { thumbnail.delete() } }
                runCatching { file.delete() }
            }
        if (removedPaths.isNotEmpty()) {
            dao.deleteRemoteImageAssets(removedPaths)
        }
    }

    private fun normalizeColorHex(value: String): String {
        val trimmed = value.trim().removePrefix("#")
        return when {
            trimmed.length == 6 && trimmed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' } ->
                "#${trimmed.uppercase()}"
            else -> "#8D4B68"
        }
    }
}

private val io.gelio.app.data.local.entity.FeaturedProjectEntity.stableId get() = id
private val io.gelio.app.data.local.entity.VirtualTourEntity.stableId get() = id
private val io.gelio.app.data.local.entity.ShowcaseVideoEntity.stableId get() = id
private val io.gelio.app.data.local.entity.BrochureEntity.stableId get() = id
private val io.gelio.app.data.local.entity.DestinationEntity.stableId get() = id
private val io.gelio.app.data.local.entity.ServiceEntity.stableId get() = id
private val io.gelio.app.data.local.entity.ReviewCardEntity.stableId get() = id
private val io.gelio.app.data.local.entity.ContentPageCardEntity.stableId get() = id
private val io.gelio.app.data.local.entity.ArtGalleryHeroEntity.stableId get() = id

private val io.gelio.app.data.local.entity.FeaturedProjectEntity.sectionIdTyped get() = sectionId
private val io.gelio.app.data.local.entity.VirtualTourEntity.sectionIdTyped get() = sectionId
private val io.gelio.app.data.local.entity.ShowcaseVideoEntity.sectionIdTyped get() = sectionId
private val io.gelio.app.data.local.entity.BrochureEntity.sectionIdTyped get() = sectionId
private val io.gelio.app.data.local.entity.DestinationEntity.sectionIdTyped get() = sectionId
private val io.gelio.app.data.local.entity.ServiceEntity.sectionIdTyped get() = sectionId
private val io.gelio.app.data.local.entity.ReviewCardEntity.sectionIdTyped get() = sectionId
private val io.gelio.app.data.local.entity.ContentPageCardEntity.sectionIdTyped get() = sectionId
private val io.gelio.app.data.local.entity.ArtGalleryHeroEntity.sectionIdTyped get() = sectionId

private val io.gelio.app.data.local.entity.FeaturedProjectEntity.sortOrderTyped get() = sortOrder
private val io.gelio.app.data.local.entity.VirtualTourEntity.sortOrderTyped get() = sortOrder
private val io.gelio.app.data.local.entity.ShowcaseVideoEntity.sortOrderTyped get() = sortOrder
private val io.gelio.app.data.local.entity.BrochureEntity.sortOrderTyped get() = sortOrder
private val io.gelio.app.data.local.entity.DestinationEntity.sortOrderTyped get() = sortOrder
private val io.gelio.app.data.local.entity.ServiceEntity.sortOrderTyped get() = sortOrder

private fun io.gelio.app.data.local.entity.FeaturedProjectEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)
private fun io.gelio.app.data.local.entity.VirtualTourEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)
private fun io.gelio.app.data.local.entity.ShowcaseVideoEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)
private fun io.gelio.app.data.local.entity.BrochureEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)
private fun io.gelio.app.data.local.entity.DestinationEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)
private fun io.gelio.app.data.local.entity.ServiceEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)
private fun io.gelio.app.data.local.entity.ReviewCardEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)
private fun io.gelio.app.data.local.entity.ContentPageCardEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)
private fun io.gelio.app.data.local.entity.ArtGalleryHeroEntity.withSortOrderValue(sortOrder: Int) = copy(sortOrder = sortOrder)

private val Any.sectionId: String
    get() = when (this) {
        is io.gelio.app.data.local.entity.FeaturedProjectEntity -> sectionIdTyped
        is io.gelio.app.data.local.entity.VirtualTourEntity -> sectionIdTyped
        is io.gelio.app.data.local.entity.ShowcaseVideoEntity -> sectionIdTyped
        is io.gelio.app.data.local.entity.BrochureEntity -> sectionIdTyped
        is io.gelio.app.data.local.entity.DestinationEntity -> sectionIdTyped
        is io.gelio.app.data.local.entity.ServiceEntity -> sectionIdTyped
        is io.gelio.app.data.local.entity.ReviewCardEntity -> sectionIdTyped
        is io.gelio.app.data.local.entity.ContentPageCardEntity -> sectionIdTyped
        is io.gelio.app.data.local.entity.ArtGalleryHeroEntity -> sectionIdTyped
        else -> error("Unsupported section-scoped entity: ${this::class.java.simpleName}")
    }

private val Any.stableId: String
    get() = when (this) {
        is io.gelio.app.data.local.entity.FeaturedProjectEntity -> stableId
        is io.gelio.app.data.local.entity.VirtualTourEntity -> stableId
        is io.gelio.app.data.local.entity.ShowcaseVideoEntity -> stableId
        is io.gelio.app.data.local.entity.BrochureEntity -> stableId
        is io.gelio.app.data.local.entity.DestinationEntity -> stableId
        is io.gelio.app.data.local.entity.ServiceEntity -> stableId
        is io.gelio.app.data.local.entity.ReviewCardEntity -> stableId
        is io.gelio.app.data.local.entity.ContentPageCardEntity -> stableId
        is io.gelio.app.data.local.entity.ArtGalleryHeroEntity -> stableId
        else -> error("Unsupported section-scoped entity: ${this::class.java.simpleName}")
    }

private fun Any.withSortOrder(sortOrder: Int): Any =
    when (this) {
        is io.gelio.app.data.local.entity.FeaturedProjectEntity -> withSortOrderValue(sortOrder)
        is io.gelio.app.data.local.entity.VirtualTourEntity -> withSortOrderValue(sortOrder)
        is io.gelio.app.data.local.entity.ShowcaseVideoEntity -> withSortOrderValue(sortOrder)
        is io.gelio.app.data.local.entity.BrochureEntity -> withSortOrderValue(sortOrder)
        is io.gelio.app.data.local.entity.DestinationEntity -> withSortOrderValue(sortOrder)
        is io.gelio.app.data.local.entity.ServiceEntity -> withSortOrderValue(sortOrder)
        is io.gelio.app.data.local.entity.ReviewCardEntity -> withSortOrderValue(sortOrder)
        is io.gelio.app.data.local.entity.ContentPageCardEntity -> withSortOrderValue(sortOrder)
        is io.gelio.app.data.local.entity.ArtGalleryHeroEntity -> withSortOrderValue(sortOrder)
        else -> error("Unsupported section-scoped entity: ${this::class.java.simpleName}")
    }
