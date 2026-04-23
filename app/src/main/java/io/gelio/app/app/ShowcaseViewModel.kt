package io.gelio.app.app

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.gelio.app.core.performance.PerfLog
import io.gelio.app.core.ui.warmGalleryPreviewAspectRatio
import io.gelio.app.core.util.generateImageThumbnailIfSupported
import io.gelio.app.core.util.generatePdfCoverThumbnailToAppStorage
import io.gelio.app.core.util.mapPreviewPath
import io.gelio.app.core.util.resolveVirtualTourThumbnailUrl
import io.gelio.app.data.model.Brochure
import io.gelio.app.data.model.ArtGalleryCard
import io.gelio.app.data.model.ArtGalleryHero
import io.gelio.app.data.model.ArtGalleryHeroGroup
import io.gelio.app.data.model.Brand
import io.gelio.app.data.model.BrandLink
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
import io.gelio.app.features.map.saveMapPreviewToStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class ShowcaseViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    private val repository = appContainer.showcaseRepository
    private val appContext = appContainer.applicationContext
    private val companyFlows = ConcurrentHashMap<String, StateFlow<ShowcaseCompany?>>()
    private val sectionFlows = ConcurrentHashMap<String, StateFlow<ShowcaseSection?>>()
    private val companySectionsFlows = ConcurrentHashMap<String, StateFlow<List<ShowcaseSection>>>()
    private val adminCompanySectionsFlows = ConcurrentHashMap<String, StateFlow<List<ShowcaseSection>>>()
    private val featuredProjectFlows = ConcurrentHashMap<String, StateFlow<List<FeaturedProject>>>()
    private val adminFeaturedProjectFlows = ConcurrentHashMap<String, StateFlow<List<FeaturedProject>>>()
    private val virtualTourFlows = ConcurrentHashMap<String, StateFlow<List<VirtualTour>>>()
    private val adminVirtualTourFlows = ConcurrentHashMap<String, StateFlow<List<VirtualTour>>>()
    private val videoFlows = ConcurrentHashMap<String, StateFlow<List<ShowcaseVideo>>>()
    private val adminVideoFlows = ConcurrentHashMap<String, StateFlow<List<ShowcaseVideo>>>()
    private val brochureFlows = ConcurrentHashMap<String, StateFlow<List<Brochure>>>()
    private val adminBrochureFlows = ConcurrentHashMap<String, StateFlow<List<Brochure>>>()
    private val destinationFlows = ConcurrentHashMap<String, StateFlow<List<Destination>>>()
    private val adminDestinationFlows = ConcurrentHashMap<String, StateFlow<List<Destination>>>()
    private val serviceFlows = ConcurrentHashMap<String, StateFlow<List<Service>>>()
    private val adminServiceFlows = ConcurrentHashMap<String, StateFlow<List<Service>>>()
    private val sectionLinkFlows = ConcurrentHashMap<String, StateFlow<List<BrandLink>>>()
    private val worldMapSectionFlows = ConcurrentHashMap<String, StateFlow<WorldMapSection?>>()
    private val worldMapPinsFlows = ConcurrentHashMap<String, StateFlow<List<WorldMapPin>>>()
    private val reviewCardFlows = ConcurrentHashMap<String, StateFlow<List<ReviewCard>>>()
    private val adminReviewCardFlows = ConcurrentHashMap<String, StateFlow<List<ReviewCard>>>()
    private val contentPageCardFlows = ConcurrentHashMap<String, StateFlow<List<ContentPageCard>>>()
    private val adminContentPageCardFlows = ConcurrentHashMap<String, StateFlow<List<ContentPageCard>>>()
    private val artGalleryHeroGroupFlows = ConcurrentHashMap<String, StateFlow<List<ArtGalleryHeroGroup>>>()
    private val artGalleryHeroFlows = ConcurrentHashMap<String, StateFlow<List<ArtGalleryHero>>>()
    private val adminArtGalleryHeroFlows = ConcurrentHashMap<String, StateFlow<List<ArtGalleryHero>>>()
    private val artGalleryCardFlows = ConcurrentHashMap<String, StateFlow<List<ArtGalleryCard>>>()
    private val adminArtGalleryCardFlows = ConcurrentHashMap<String, StateFlow<List<ArtGalleryCard>>>()
    private val artGalleryHeroDetailFlows = ConcurrentHashMap<String, StateFlow<ArtGalleryHero?>>()
    private val nextFlowId = java.util.concurrent.atomic.AtomicInteger(0)
    private val featuredProjectDetailFlows = ConcurrentHashMap<String, StateFlow<FeaturedProject?>>()
    private val virtualTourDetailFlows = ConcurrentHashMap<String, StateFlow<VirtualTour?>>()
    private val videoDetailFlows = ConcurrentHashMap<String, StateFlow<ShowcaseVideo?>>()
    private val brochureDetailFlows = ConcurrentHashMap<String, StateFlow<Brochure?>>()
    private val virtualTourAutoThumbnailFlows = ConcurrentHashMap<String, StateFlow<Map<String, String>>>()
    private val topLevelHydrationStates = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    private val cachedHydrationStates = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    private val _startupWarmupState = MutableStateFlow(
        StartupWarmupState(
            isReady = false,
            stage = StartupWarmupStage.Seed,
            completedTasks = 0,
            totalTasks = 1,
            message = "Preparing data",
        ),
    )
    val startupWarmupState: StateFlow<StartupWarmupState> = _startupWarmupState.asStateFlow()
    private var startupWarmupJob: Job? = null

    val companies: StateFlow<List<ShowcaseCompany>> =
        snapshotBackedStateFlow(
            initialValue = emptyList(),
            upstream = { repository.companies },
            snapshot = repository::visibleCompaniesSnapshot,
            isEmpty = List<ShowcaseCompany>::isEmpty,
            hydrationState = topLevelHydrationState("companies"),
        )

    val adminCompanies: StateFlow<List<ShowcaseCompany>> =
        snapshotBackedStateFlow(
            initialValue = emptyList(),
            upstream = { repository.adminCompanies },
            snapshot = repository::adminCompaniesSnapshot,
            isEmpty = List<ShowcaseCompany>::isEmpty,
            hydrationState = topLevelHydrationState("adminCompanies"),
        )

    init {
        ensureStartupWarmup()
    }

    fun company(companyId: String): StateFlow<ShowcaseCompany?> =
        cachedSnapshotStateFlow(companyFlows, companyId, null, { repository.company(companyId) }, { repository.adminCompanySnapshot(companyId) }) { it == null }

    fun sections(companyId: String): StateFlow<List<ShowcaseSection>> =
        cachedSnapshotStateFlow(companySectionsFlows, companyId, emptyList(), { repository.sections(companyId) }, { repository.visibleSectionsSnapshot(companyId) }, List<ShowcaseSection>::isEmpty)

    fun adminSections(companyId: String): StateFlow<List<ShowcaseSection>> =
        cachedSnapshotStateFlow(adminCompanySectionsFlows, companyId, emptyList(), { repository.adminSections(companyId) }, { repository.adminSectionsSnapshot(companyId) }, List<ShowcaseSection>::isEmpty)

    fun section(sectionId: String): StateFlow<ShowcaseSection?> =
        cachedSnapshotStateFlow(sectionFlows, sectionId, null, { repository.section(sectionId) }, { repository.sectionSnapshot(sectionId) }) { it == null }

    suspend fun adminCompanySnapshot(companyId: String): ShowcaseCompany? = repository.adminCompanySnapshot(companyId)
    fun moveCompany(companyId: String, direction: Int) {
        viewModelScope.launch {
            repository.moveCompany(companyId, direction)
        }
    }

    fun createCompany(
        name: String,
        logoPath: String,
        brandSeedColor: String,
        onCreated: (ShowcaseCompany) -> Unit = {},
    ) {
        viewModelScope.launch {
            onCreated(repository.createCompany(name, logoPath, brandSeedColor))
        }
    }

    fun updateCompany(company: ShowcaseCompany) {
        viewModelScope.launch {
            repository.updateCompany(company)
        }
    }

    fun toggleCompanyVisibility(company: ShowcaseCompany) {
        viewModelScope.launch {
            repository.toggleCompanyVisibility(company)
        }
    }

    fun deleteCompany(companyId: String) {
        viewModelScope.launch {
            repository.deleteCompany(companyId)
        }
    }
    suspend fun adminSectionsSnapshot(companyId: String): List<ShowcaseSection> = repository.adminSectionsSnapshot(companyId)

    fun featuredProjects(sectionId: String): StateFlow<List<FeaturedProject>> =
        cachedSnapshotStateFlow(featuredProjectFlows, sectionId, emptyList(), { repository.visibleFeaturedProjects(sectionId) }, { repository.visibleFeaturedProjectsSnapshot(sectionId) }, List<FeaturedProject>::isEmpty)

    fun adminFeaturedProjects(sectionId: String): StateFlow<List<FeaturedProject>> =
        cachedSnapshotStateFlow(
            adminFeaturedProjectFlows,
            sectionId,
            emptyList(),
            { repository.adminFeaturedProjects(sectionId) },
            { repository.adminFeaturedProjectsSnapshot(sectionId) },
            List<FeaturedProject>::isEmpty,
        )

    fun virtualTours(sectionId: String): StateFlow<List<VirtualTour>> =
        cachedSnapshotStateFlow(virtualTourFlows, sectionId, emptyList(), { repository.visibleVirtualTours(sectionId) }, { repository.visibleVirtualToursSnapshot(sectionId) }, List<VirtualTour>::isEmpty)

    fun adminVirtualTours(sectionId: String): StateFlow<List<VirtualTour>> =
        cachedSnapshotStateFlow(
            adminVirtualTourFlows,
            sectionId,
            emptyList(),
            { repository.adminVirtualTours(sectionId) },
            { repository.adminVirtualToursSnapshot(sectionId) },
            List<VirtualTour>::isEmpty,
        )

    fun videos(sectionId: String): StateFlow<List<ShowcaseVideo>> =
        cachedSnapshotStateFlow(videoFlows, sectionId, emptyList(), { repository.visibleVideos(sectionId) }, { repository.visibleVideosSnapshot(sectionId) }, List<ShowcaseVideo>::isEmpty)

    fun adminVideos(sectionId: String): StateFlow<List<ShowcaseVideo>> =
        cachedSnapshotStateFlow(
            adminVideoFlows,
            sectionId,
            emptyList(),
            { repository.adminVideos(sectionId) },
            { repository.adminVideosSnapshot(sectionId) },
            List<ShowcaseVideo>::isEmpty,
        )

    fun brochures(sectionId: String): StateFlow<List<Brochure>> =
        cachedSnapshotStateFlow(brochureFlows, sectionId, emptyList(), { repository.visibleBrochures(sectionId) }, { repository.visibleBrochuresSnapshot(sectionId) }, List<Brochure>::isEmpty)

    fun adminBrochures(sectionId: String): StateFlow<List<Brochure>> =
        cachedSnapshotStateFlow(
            adminBrochureFlows,
            sectionId,
            emptyList(),
            { repository.adminBrochures(sectionId) },
            { repository.adminBrochuresSnapshot(sectionId) },
            List<Brochure>::isEmpty,
        )

    fun destinations(sectionId: String): StateFlow<List<Destination>> =
        cachedSnapshotStateFlow(destinationFlows, sectionId, emptyList(), { repository.visibleDestinations(sectionId) }, { repository.visibleDestinationsSnapshot(sectionId) }, List<Destination>::isEmpty)

    fun adminDestinations(sectionId: String): StateFlow<List<Destination>> =
        cachedSnapshotStateFlow(
            adminDestinationFlows,
            sectionId,
            emptyList(),
            { repository.adminDestinations(sectionId) },
            { repository.adminDestinationsSnapshot(sectionId) },
            List<Destination>::isEmpty,
        )

    fun services(sectionId: String): StateFlow<List<Service>> =
        cachedSnapshotStateFlow(serviceFlows, sectionId, emptyList(), { repository.visibleServices(sectionId) }, { repository.visibleServicesSnapshot(sectionId) }, List<Service>::isEmpty)

    fun adminServices(sectionId: String): StateFlow<List<Service>> =
        cachedSnapshotStateFlow(
            adminServiceFlows,
            sectionId,
            emptyList(),
            { repository.adminServices(sectionId) },
            { repository.adminServicesSnapshot(sectionId) },
            List<Service>::isEmpty,
        )

    fun sectionLinks(sectionId: String): StateFlow<List<BrandLink>> =
        cachedSnapshotStateFlow(sectionLinkFlows, sectionId, emptyList(), { repository.sectionLinks(sectionId) }, { repository.sectionLinksSnapshot(sectionId) }, List<BrandLink>::isEmpty)

    fun worldMapSection(sectionId: String): StateFlow<WorldMapSection?> =
        cachedSnapshotStateFlow(worldMapSectionFlows, sectionId, null, { repository.worldMapSection(sectionId) }, { repository.worldMapSectionSnapshot(sectionId) }) { it == null }

    fun worldMapPins(sectionId: String): StateFlow<List<WorldMapPin>> =
        cachedSnapshotStateFlow(worldMapPinsFlows, sectionId, emptyList(), { repository.worldMapPins(sectionId) }, { repository.worldMapPinsSnapshot(sectionId) }, List<WorldMapPin>::isEmpty)

    fun reviewCards(sectionId: String): StateFlow<List<ReviewCard>> =
        cachedSnapshotStateFlow(reviewCardFlows, sectionId, emptyList(), { repository.visibleReviewCards(sectionId) }, { repository.visibleReviewCardsSnapshot(sectionId) }, List<ReviewCard>::isEmpty)

    fun adminReviewCards(sectionId: String): StateFlow<List<ReviewCard>> =
        cachedSnapshotStateFlow(
            adminReviewCardFlows,
            sectionId,
            emptyList(),
            { repository.adminReviewCards(sectionId) },
            { repository.adminReviewCardsSnapshot(sectionId) },
            List<ReviewCard>::isEmpty,
        )

    fun contentPageCards(sectionId: String): StateFlow<List<ContentPageCard>> =
        cachedSnapshotStateFlow(
            contentPageCardFlows,
            sectionId,
            emptyList(),
            { repository.visibleContentPageCards(sectionId) },
            { repository.visibleContentPageCardsSnapshot(sectionId) },
            List<ContentPageCard>::isEmpty,
        )

    fun adminContentPageCards(sectionId: String): StateFlow<List<ContentPageCard>> =
        cachedSnapshotStateFlow(
            adminContentPageCardFlows,
            sectionId,
            emptyList(),
            { repository.adminContentPageCards(sectionId) },
            { repository.adminContentPageCardsSnapshot(sectionId) },
            List<ContentPageCard>::isEmpty,
        )

    fun artGalleryGroups(sectionId: String): StateFlow<List<ArtGalleryHeroGroup>> =
        cachedSnapshotStateFlow(
            artGalleryHeroGroupFlows,
            sectionId,
            emptyList(),
            { repository.visibleArtGalleryGroups(sectionId) },
            { repository.visibleArtGalleryGroupsSnapshot(sectionId) },
            List<ArtGalleryHeroGroup>::isEmpty,
        )

    fun artGalleryHeroes(sectionId: String): StateFlow<List<ArtGalleryHero>> =
        cachedSnapshotStateFlow(
            artGalleryHeroFlows,
            sectionId,
            emptyList(),
            { repository.visibleArtGalleryHeroes(sectionId) },
            { repository.visibleArtGalleryHeroesSnapshot(sectionId) },
            List<ArtGalleryHero>::isEmpty,
        )

    fun adminArtGalleryHeroes(sectionId: String): StateFlow<List<ArtGalleryHero>> =
        cachedSnapshotStateFlow(
            adminArtGalleryHeroFlows,
            sectionId,
            emptyList(),
            { repository.adminArtGalleryHeroes(sectionId) },
            { repository.adminArtGalleryHeroesSnapshot(sectionId) },
            List<ArtGalleryHero>::isEmpty,
        )

    fun artGalleryHero(heroId: String): StateFlow<ArtGalleryHero?> =
        cachedSnapshotStateFlow(
            artGalleryHeroDetailFlows,
            heroId,
            null,
            { repository.artGalleryHero(heroId) },
            { repository.artGalleryHeroSnapshot(heroId) },
        ) { it == null }

    fun artGalleryCards(heroId: String): StateFlow<List<ArtGalleryCard>> =
        cachedSnapshotStateFlow(
            artGalleryCardFlows,
            heroId,
            emptyList(),
            { repository.visibleArtGalleryCards(heroId) },
            { repository.visibleArtGalleryCardsSnapshot(heroId) },
            List<ArtGalleryCard>::isEmpty,
        )

    fun adminArtGalleryCards(heroId: String): StateFlow<List<ArtGalleryCard>> =
        cachedSnapshotStateFlow(
            adminArtGalleryCardFlows,
            heroId,
            emptyList(),
            { repository.adminArtGalleryCards(heroId) },
            { repository.adminArtGalleryCardsSnapshot(heroId) },
            List<ArtGalleryCard>::isEmpty,
        )

    fun featuredProject(id: String): StateFlow<FeaturedProject?> =
        cachedSnapshotStateFlow(featuredProjectDetailFlows, id, null, { repository.featuredProject(id) }, { repository.featuredProjectSnapshot(id) }) { it == null }

    fun virtualTour(id: String): StateFlow<VirtualTour?> =
        cachedSnapshotStateFlow(virtualTourDetailFlows, id, null, { repository.virtualTour(id) }, { repository.virtualTourSnapshot(id) }) { it == null }

    fun virtualTourAutoThumbnails(sectionId: String): StateFlow<Map<String, String>> =
        cachedSnapshotStateFlow(
            virtualTourAutoThumbnailFlows,
            sectionId,
            emptyMap(),
            upstream = {
                repository.visibleVirtualTours(sectionId).map { tours ->
                    resolveVirtualTourAutoThumbnailMap(tours)
                }
            },
            snapshot = {
                resolveVirtualTourAutoThumbnailMap(repository.visibleVirtualToursSnapshot(sectionId))
            },
            isEmpty = { false },
        )

    fun video(id: String): StateFlow<ShowcaseVideo?> =
        cachedSnapshotStateFlow(videoDetailFlows, id, null, { repository.video(id) }, { repository.videoSnapshot(id) }) { it == null }

    fun brochure(id: String): StateFlow<Brochure?> =
        cachedSnapshotStateFlow(brochureDetailFlows, id, null, { repository.brochure(id) }, { repository.brochureSnapshot(id) }) { it == null }

    fun createSection(
        companyId: String,
        type: SectionType,
        title: String,
        onCreated: (ShowcaseSection) -> Unit = {},
    ) {
        viewModelScope.launch {
            onCreated(repository.createSection(companyId, type, title))
        }
    }

    fun renameSection(section: ShowcaseSection, newTitle: String) {
        viewModelScope.launch {
            repository.renameSection(section, newTitle)
        }
    }

    fun toggleSectionVisibility(section: ShowcaseSection) {
        viewModelScope.launch {
            repository.toggleSectionVisibility(section)
        }
    }

    fun moveSection(sectionId: String, direction: Int) {
        viewModelScope.launch {
            repository.moveSection(sectionId, direction)
        }
    }

    fun deleteSection(sectionId: String) {
        viewModelScope.launch {
            repository.deleteSection(sectionId)
        }
    }

    fun saveService(service: Service) {
        viewModelScope.launch { repository.saveService(service) }
    }

    fun saveDestination(destination: Destination) {
        viewModelScope.launch { repository.saveDestination(destination) }
    }

    fun deleteDestination(id: String) {
        viewModelScope.launch { repository.deleteDestination(id) }
    }

    fun toggleDestinationVisibility(destination: Destination) {
        viewModelScope.launch { repository.toggleDestinationVisibility(destination) }
    }

    fun moveDestination(id: String, direction: Int) {
        viewModelScope.launch { repository.moveDestination(id, direction) }
    }

    fun saveFeaturedProject(project: FeaturedProject) {
        viewModelScope.launch { repository.saveFeaturedProject(project) }
    }

    fun deleteFeaturedProject(id: String) {
        viewModelScope.launch { repository.deleteFeaturedProject(id) }
    }

    fun toggleFeaturedProjectVisibility(project: FeaturedProject) {
        viewModelScope.launch { repository.toggleFeaturedProjectVisibility(project) }
    }

    fun moveFeaturedProject(id: String, direction: Int) {
        viewModelScope.launch { repository.moveFeaturedProject(id, direction) }
    }

    fun saveVirtualTour(tour: VirtualTour) {
        viewModelScope.launch { repository.saveVirtualTour(tour) }
    }

    fun deleteVirtualTour(id: String) {
        viewModelScope.launch { repository.deleteVirtualTour(id) }
    }

    fun toggleVirtualTourVisibility(tour: VirtualTour) {
        viewModelScope.launch { repository.toggleVirtualTourVisibility(tour) }
    }

    fun moveVirtualTour(id: String, direction: Int) {
        viewModelScope.launch { repository.moveVirtualTour(id, direction) }
    }

    fun saveVideo(video: ShowcaseVideo) {
        viewModelScope.launch { repository.saveVideo(video) }
    }

    fun deleteVideo(id: String) {
        viewModelScope.launch { repository.deleteVideo(id) }
    }

    fun toggleVideoVisibility(video: ShowcaseVideo) {
        viewModelScope.launch { repository.toggleVideoVisibility(video) }
    }

    fun moveVideo(id: String, direction: Int) {
        viewModelScope.launch { repository.moveVideo(id, direction) }
    }

    fun saveBrochure(brochure: Brochure) {
        viewModelScope.launch { repository.saveBrochure(brochure) }
    }

    fun deleteBrochure(id: String, brand: Brand) {
        viewModelScope.launch { repository.deleteBrochure(id, brand) }
    }

    fun toggleBrochureVisibility(brochure: Brochure) {
        viewModelScope.launch { repository.toggleBrochureVisibility(brochure) }
    }

    fun moveBrochure(id: String, brand: Brand, direction: Int) {
        viewModelScope.launch { repository.moveBrochure(id, brand, direction) }
    }

    fun saveSectionLink(
        sectionId: String,
        label: String,
        url: String,
    ) {
        viewModelScope.launch {
            repository.saveSectionLink(sectionId, label, url)
        }
    }

    fun deleteSectionLink(id: String) {
        viewModelScope.launch {
            repository.deleteSectionLink(id)
        }
    }

    fun deleteService(id: String) {
        viewModelScope.launch { repository.deleteService(id) }
    }

    fun toggleServiceVisibility(service: Service) {
        viewModelScope.launch { repository.toggleServiceVisibility(service) }
    }

    fun moveService(id: String, direction: Int) {
        viewModelScope.launch { repository.moveService(id, direction) }
    }

    fun saveWorldMap(section: WorldMapSection, pins: List<WorldMapPin>) {
        viewModelScope.launch {
            repository.saveWorldMap(section, pins)
        }
    }

    fun saveReviewCard(card: ReviewCard) {
        viewModelScope.launch { repository.saveReviewCard(card) }
    }

    fun deleteReviewCard(id: String) {
        viewModelScope.launch { repository.deleteReviewCard(id) }
    }

    fun toggleReviewCardVisibility(card: ReviewCard) {
        viewModelScope.launch { repository.toggleReviewCardVisibility(card) }
    }

    fun moveReviewCard(id: String, direction: Int) {
        viewModelScope.launch { repository.moveReviewCard(id, direction) }
    }

    fun saveContentPageCard(card: ContentPageCard) {
        viewModelScope.launch { repository.saveContentPageCard(card) }
    }

    fun deleteContentPageCard(id: String) {
        viewModelScope.launch { repository.deleteContentPageCard(id) }
    }

    fun toggleContentPageCardVisibility(card: ContentPageCard) {
        viewModelScope.launch { repository.toggleContentPageCardVisibility(card) }
    }

    fun moveContentPageCard(id: String, direction: Int) {
        viewModelScope.launch { repository.moveContentPageCard(id, direction) }
    }

    fun saveArtGalleryHero(hero: ArtGalleryHero) {
        viewModelScope.launch { repository.saveArtGalleryHero(hero) }
    }

    fun deleteArtGalleryHero(id: String) {
        viewModelScope.launch { repository.deleteArtGalleryHero(id) }
    }

    fun toggleArtGalleryHeroVisibility(hero: ArtGalleryHero) {
        viewModelScope.launch { repository.toggleArtGalleryHeroVisibility(hero) }
    }

    fun moveArtGalleryHero(id: String, direction: Int) {
        viewModelScope.launch { repository.moveArtGalleryHero(id, direction) }
    }

    fun saveArtGalleryCard(card: ArtGalleryCard) {
        viewModelScope.launch { repository.saveArtGalleryCard(card) }
    }

    fun deleteArtGalleryCard(id: String) {
        viewModelScope.launch { repository.deleteArtGalleryCard(id) }
    }

    fun toggleArtGalleryCardVisibility(card: ArtGalleryCard) {
        viewModelScope.launch { repository.toggleArtGalleryCardVisibility(card) }
    }

    fun moveArtGalleryCard(id: String, direction: Int) {
        viewModelScope.launch { repository.moveArtGalleryCard(id, direction) }
    }

    fun ensureDefaultCompaniesPresent() {
        viewModelScope.launch {
            repository.ensureDefaultCompaniesPresent()
        }
    }

    fun prewarmSections(sections: List<ShowcaseSection>) {
        sections.forEach { section ->
            when (section.type) {
                SectionType.IMAGE_GALLERY -> {
                    featuredProjects(section.id)
                    sectionLinks(section.id)
                }

                SectionType.TOUR_360 -> {
                    virtualTours(section.id)
                    virtualTourAutoThumbnails(section.id)
                }

                SectionType.YOUTUBE_VIDEOS -> {
                    videos(section.id)
                }

                SectionType.PDF_VIEWER -> {
                    brochures(section.id)
                }

                SectionType.DESTINATIONS -> {
                    destinations(section.id)
                }

                SectionType.SERVICES -> {
                    services(section.id)
                }

                SectionType.WORLD_MAP,
                SectionType.REGION_MAP -> {
                    worldMapSection(section.id)
                    worldMapPins(section.id)
                }

                SectionType.GOOGLE_REVIEWS -> {
                    reviewCards(section.id)
                }

                SectionType.CONTENT_PAGE -> {
                    contentPageCards(section.id)
                }

                SectionType.ART_GALLERY -> {
                    artGalleryGroups(section.id)
                    adminArtGalleryHeroes(section.id)
                }
            }
        }
    }

    private fun ensureStartupWarmup() {
        if (_startupWarmupState.value.isReady) return
        if (startupWarmupJob?.isActive == true) return
        startupWarmupJob = viewModelScope.launch {
            runStartupWarmup()
        }
    }

    private suspend fun runStartupWarmup() {
        var stage = StartupWarmupStage.Seed
        var message = "Preparing data"
        var completedTasks = 0
        var totalTasks = 0

        fun publish(
            ready: Boolean = false,
            errorMessage: String? = null,
        ) {
            _startupWarmupState.value = StartupWarmupState(
                isReady = ready,
                stage = if (errorMessage != null) StartupWarmupStage.Failed else stage,
                completedTasks = completedTasks,
                totalTasks = totalTasks.coerceAtLeast(1),
                message = message,
                errorMessage = errorMessage,
            )
        }

        fun setStage(
            newStage: StartupWarmupStage,
            newMessage: String,
        ) {
            stage = newStage
            message = newMessage
            publish()
        }

        fun addTasks(count: Int) {
            totalTasks += count.coerceAtLeast(0)
            publish()
        }

        fun finishTask(doneMessage: String) {
            completedTasks += 1
            message = doneMessage
            publish()
        }

        try {
            setStage(StartupWarmupStage.Seed, "Preparing library")
            addTasks(1)
            appContainer.showcaseInitialized.first { it }
            finishTask("Core data ready")

            setStage(StartupWarmupStage.LoadCompanies, "Loading companies")
            addTasks(2)
            companies
            adminCompanies
            awaitTopLevelHydration("companies")
            finishTask("Visible companies ready")
            awaitTopLevelHydration("adminCompanies")
            finishTask("Admin companies ready")

            val visibleCompanies = withContext(Dispatchers.IO) { repository.visibleCompaniesSnapshot() }
            val adminCompaniesSnapshot = withContext(Dispatchers.IO) { repository.adminCompaniesSnapshot() }
            val allCompanies = (visibleCompanies + adminCompaniesSnapshot).distinctBy { it.id }

            setStage(StartupWarmupStage.LoadSections, "Loading sections")
            addTasks(allCompanies.size * 3)
            val visibleSectionsByCompany = linkedMapOf<String, List<ShowcaseSection>>()
            val adminSectionsByCompany = linkedMapOf<String, List<ShowcaseSection>>()
            for (company in allCompanies) {
                company(company.id)
                awaitCachedHydration(companyFlows, company.id)
                finishTask("${company.name} index ready")

                sections(company.id)
                awaitCachedHydration(companySectionsFlows, company.id)
                val visibleSections = withContext(Dispatchers.IO) { repository.visibleSectionsSnapshot(company.id) }
                visibleSectionsByCompany[company.id] = visibleSections
                finishTask("${company.name} client sections ready")

                adminSections(company.id)
                awaitCachedHydration(adminCompanySectionsFlows, company.id)
                val adminSections = withContext(Dispatchers.IO) { repository.adminSectionsSnapshot(company.id) }
                adminSectionsByCompany[company.id] = adminSections
                finishTask("${company.name} admin sections ready")
            }

            val visibleSections = visibleSectionsByCompany.values.flatten()

            setStage(StartupWarmupStage.WarmSectionPayloads, "Warming section data")
            addTasks(visibleSections.size)
            for (section in visibleSections) {
                section(section.id)
                awaitCachedHydration(sectionFlows, section.id)
                finishTask("${section.title} index ready")
            }
            for (section in visibleSections) {
                warmSectionPayload(
                    section = section,
                    onPlanned = ::addTasks,
                    onCompleted = ::finishTask,
                )
            }

            setStage(StartupWarmupStage.WarmLightweightMedia, "Preparing previews")
            warmSectionLightweightMedia(
                sections = visibleSections,
                onPlanned = ::addTasks,
                onCompleted = ::finishTask,
            )

            setStage(StartupWarmupStage.WarmNetworkEnrichments, "Resolving online previews")
            val tourSections = visibleSections.filter { it.type == SectionType.TOUR_360 }
            addTasks(tourSections.size)
            for (section in tourSections) {
                virtualTourAutoThumbnails(section.id)
                awaitCachedHydration(virtualTourAutoThumbnailFlows, section.id)
                finishTask("${section.title} online previews ready")
            }

            setStage(StartupWarmupStage.Finalize, "Finalizing")
            addTasks(1)
            finishTask("Startup complete")
            publish(ready = true)
        } catch (error: Throwable) {
            publish(
                ready = false,
                errorMessage = error.message ?: "Unable to prepare startup data.",
            )
        }
    }

    private suspend fun warmSectionPayload(
        section: ShowcaseSection,
        onPlanned: (Int) -> Unit,
        onCompleted: (String) -> Unit,
    ) {
        when (section.type) {
            SectionType.IMAGE_GALLERY -> {
                onPlanned(3)
                featuredProjects(section.id)
                awaitCachedHydration(featuredProjectFlows, section.id)
                onCompleted("${section.title} gallery ready")

                sectionLinks(section.id)
                awaitCachedHydration(sectionLinkFlows, section.id)
                onCompleted("${section.title} links ready")

                adminFeaturedProjects(section.id)
                awaitCachedHydration(adminFeaturedProjectFlows, section.id)
                onCompleted("${section.title} admin gallery ready")
            }

            SectionType.TOUR_360 -> {
                onPlanned(2)
                virtualTours(section.id)
                awaitCachedHydration(virtualTourFlows, section.id)
                onCompleted("${section.title} tours ready")

                adminVirtualTours(section.id)
                awaitCachedHydration(adminVirtualTourFlows, section.id)
                onCompleted("${section.title} admin tours ready")
            }

            SectionType.YOUTUBE_VIDEOS -> {
                onPlanned(2)
                videos(section.id)
                awaitCachedHydration(videoFlows, section.id)
                onCompleted("${section.title} videos ready")

                adminVideos(section.id)
                awaitCachedHydration(adminVideoFlows, section.id)
                onCompleted("${section.title} admin videos ready")
            }

            SectionType.PDF_VIEWER -> {
                onPlanned(2)
                brochures(section.id)
                awaitCachedHydration(brochureFlows, section.id)
                onCompleted("${section.title} brochures ready")

                adminBrochures(section.id)
                awaitCachedHydration(adminBrochureFlows, section.id)
                onCompleted("${section.title} admin brochures ready")
            }

            SectionType.DESTINATIONS -> {
                onPlanned(2)
                destinations(section.id)
                awaitCachedHydration(destinationFlows, section.id)
                onCompleted("${section.title} destinations ready")

                adminDestinations(section.id)
                awaitCachedHydration(adminDestinationFlows, section.id)
                onCompleted("${section.title} admin destinations ready")
            }

            SectionType.SERVICES -> {
                onPlanned(2)
                services(section.id)
                awaitCachedHydration(serviceFlows, section.id)
                onCompleted("${section.title} services ready")

                adminServices(section.id)
                awaitCachedHydration(adminServiceFlows, section.id)
                onCompleted("${section.title} admin services ready")
            }

            SectionType.WORLD_MAP,
            SectionType.REGION_MAP -> {
                onPlanned(2)
                worldMapSection(section.id)
                awaitCachedHydration(worldMapSectionFlows, section.id)
                onCompleted("${section.title} map ready")

                worldMapPins(section.id)
                awaitCachedHydration(worldMapPinsFlows, section.id)
                onCompleted("${section.title} map pins ready")
            }

            SectionType.GOOGLE_REVIEWS -> {
                onPlanned(2)
                reviewCards(section.id)
                awaitCachedHydration(reviewCardFlows, section.id)
                onCompleted("${section.title} ratings ready")

                adminReviewCards(section.id)
                awaitCachedHydration(adminReviewCardFlows, section.id)
                onCompleted("${section.title} admin ratings ready")
            }

            SectionType.CONTENT_PAGE -> {
                onPlanned(2)
                contentPageCards(section.id)
                awaitCachedHydration(contentPageCardFlows, section.id)
                onCompleted("${section.title} content ready")

                adminContentPageCards(section.id)
                awaitCachedHydration(adminContentPageCardFlows, section.id)
                onCompleted("${section.title} admin content ready")
            }

            SectionType.ART_GALLERY -> {
                onPlanned(2)
                artGalleryGroups(section.id)
                awaitCachedHydration(artGalleryHeroGroupFlows, section.id)
                onCompleted("${section.title} art gallery ready")

                adminArtGalleryHeroes(section.id)
                awaitCachedHydration(adminArtGalleryHeroFlows, section.id)
                onCompleted("${section.title} admin art gallery ready")
            }
        }
    }

    private suspend fun warmSectionLightweightMedia(
        sections: List<ShowcaseSection>,
        onPlanned: (Int) -> Unit,
        onCompleted: (String) -> Unit,
    ) {
        for (section in sections) {
            when (section.type) {
                SectionType.IMAGE_GALLERY -> {
                    val projects = withContext(Dispatchers.IO) { repository.adminFeaturedProjectsSnapshot(section.id) }
                    onPlanned(projects.size)
                    projects.forEach { project ->
                        warmPreviewImage(project.thumbnailUri)
                        project.galleryImages.forEach { imagePath ->
                            warmPreviewImage(imagePath)
                        }
                        onCompleted("${project.projectName} previews ready")
                    }
                }

                SectionType.TOUR_360 -> {
                    val tours = withContext(Dispatchers.IO) { repository.adminVirtualToursSnapshot(section.id) }
                    onPlanned(tours.size)
                    tours.forEach { tour ->
                        warmPreviewImage(tour.thumbnailUri)
                        onCompleted("${tour.projectName} preview ready")
                    }
                }

                SectionType.YOUTUBE_VIDEOS -> {
                    val videos = withContext(Dispatchers.IO) { repository.adminVideosSnapshot(section.id) }
                    onPlanned(videos.size)
                    videos.forEach { video ->
                        warmPreviewImage(video.thumbnailUri.orEmpty())
                        onCompleted("${video.title} preview ready")
                    }
                }

                SectionType.PDF_VIEWER -> {
                    val brochures = withContext(Dispatchers.IO) { repository.adminBrochuresSnapshot(section.id) }
                    onPlanned(brochures.size)
                    brochures.forEach { brochure ->
                        var coverPath = brochure.coverThumbnailUri
                        val coverMissing = coverPath.isBlank() || mapFileMissing(coverPath)
                        if (coverMissing) {
                            val generated = generatePdfCoverThumbnailToAppStorage(
                                context = appContext,
                                pdfPath = brochure.pdfUri,
                            ).orEmpty()
                            if (generated.isNotBlank() && generated != brochure.coverThumbnailUri) {
                                repository.saveBrochure(brochure.copy(coverThumbnailUri = generated))
                                coverPath = generated
                            }
                        }
                        warmPreviewImage(coverPath)
                        onCompleted("${brochure.title} cover ready")
                    }
                }

                SectionType.DESTINATIONS -> {
                    val destinations = withContext(Dispatchers.IO) { repository.adminDestinationsSnapshot(section.id) }
                    onPlanned(destinations.size)
                    destinations.forEach { destination ->
                        warmPreviewImage(destination.imageUri)
                        onCompleted("${destination.destinationName} preview ready")
                    }
                }

                SectionType.SERVICES -> {
                    val services = withContext(Dispatchers.IO) { repository.adminServicesSnapshot(section.id) }
                    onPlanned(services.size)
                    services.forEach { service ->
                        warmPreviewImage(service.imageUri)
                        onCompleted("${service.serviceTitle} preview ready")
                    }
                }

                SectionType.WORLD_MAP,
                SectionType.REGION_MAP -> {
                    onPlanned(1)
                    val mapSection = withContext(Dispatchers.IO) { repository.worldMapSectionSnapshot(section.id) }
                    if (mapSection != null && mapSection.assetName.isNotBlank() && mapPreviewPath(appContext, section.id).isNullOrBlank()) {
                        val pins = withContext(Dispatchers.IO) { repository.worldMapPinsSnapshot(section.id) }
                        saveMapPreviewToStorage(appContext, mapSection, pins)
                    }
                    onCompleted("${section.title} map preview ready")
                }

                SectionType.GOOGLE_REVIEWS -> Unit

                SectionType.CONTENT_PAGE -> {
                    val cards = withContext(Dispatchers.IO) { repository.adminContentPageCardsSnapshot(section.id) }
                    onPlanned(cards.size)
                    cards.forEach { card ->
                        warmPreviewImage(card.imagePath)
                        onCompleted("${card.title} preview ready")
                    }
                }

                SectionType.ART_GALLERY -> {
                    val groups = withContext(Dispatchers.IO) { repository.visibleArtGalleryGroupsSnapshot(section.id) }
                    onPlanned(groups.size)
                    groups.forEach { group ->
                        group.cards.forEach { card -> warmPreviewImage(card.imagePath) }
                        onCompleted("${group.hero.title} artwork ready")
                    }
                }
            }
        }
    }

    private suspend fun resolveVirtualTourAutoThumbnailMap(
        tours: List<VirtualTour>,
    ): Map<String, String> = coroutineScope {
        val semaphore = Semaphore(4)
        tours
            .filter { it.thumbnailUri.isBlank() }
            .map { tour ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        tour.id to resolveVirtualTourThumbnailUrl(tour.embedUrl).orEmpty()
                    }
                }
            }
            .awaitAll()
            .filter { (_, url) -> url.isNotBlank() }
            .toMap()
    }

    private suspend fun warmPreviewImage(path: String) {
        if (path.isBlank()) return
        withContext(Dispatchers.IO) {
            generateImageThumbnailIfSupported(appContext, path)
        }
        warmGalleryPreviewAspectRatio(appContext, path)
    }

    private suspend fun awaitTopLevelHydration(key: String) {
        topLevelHydrationState(key).first { it }
    }

    private suspend fun awaitCachedHydration(
        cache: ConcurrentHashMap<String, *>,
        key: String,
    ) {
        cachedHydrationState(cache, key).first { it }
    }

    private fun topLevelHydrationState(key: String): MutableStateFlow<Boolean> =
        topLevelHydrationStates.getOrPut(key) { MutableStateFlow(false) }

    private fun cachedHydrationState(
        cache: ConcurrentHashMap<String, *>,
        key: String,
    ): MutableStateFlow<Boolean> =
        cachedHydrationStates.getOrPut("${System.identityHashCode(cache)}:$key") {
            MutableStateFlow(false)
        }

    private fun mapFileMissing(path: String): Boolean =
        path.isBlank() || runCatching { java.io.File(path) }.getOrNull()?.exists() != true

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ShowcaseViewModel(appContainer) }
            }
    }

    private fun <T> cachedStateFlow(
        cache: ConcurrentHashMap<String, StateFlow<T>>,
        key: String,
        initialValue: T,
        upstream: () -> Flow<T>,
    ): StateFlow<T> = cache.getOrPut(key) {
        upstream().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialValue,
        )
    }

    private fun <T> cachedSnapshotStateFlow(
        cache: ConcurrentHashMap<String, StateFlow<T>>,
        key: String,
        initialValue: T,
        upstream: () -> Flow<T>,
        snapshot: suspend () -> T,
        isEmpty: (T) -> Boolean,
    ): StateFlow<T> = cache.getOrPut(key) {
        snapshotBackedStateFlow(
            initialValue = initialValue,
            upstream = upstream,
            snapshot = snapshot,
            isEmpty = isEmpty,
            hydrationState = cachedHydrationState(cache, key),
        )
    }

    private fun <T> snapshotBackedStateFlow(
        initialValue: T,
        upstream: () -> Flow<T>,
        snapshot: suspend () -> T,
        isEmpty: (T) -> Boolean,
        hydrationState: MutableStateFlow<Boolean>? = null,
    ): StateFlow<T> {
        val state = MutableStateFlow(initialValue)
        val flowId = nextFlowId.incrementAndGet()
        hydrationState?.value = false
        viewModelScope.launch {
            val tSnap = SystemClock.elapsedRealtime()
            val seeded = runCatching { withContext(Dispatchers.IO) { snapshot() } }.getOrNull()
            PerfLog.d("DB", "snapshot#$flowId took ${SystemClock.elapsedRealtime() - tSnap}ms empty=${seeded?.let(isEmpty) ?: true}")
            if (seeded != null && !isEmpty(seeded)) state.value = seeded
            hydrationState?.value = true
            val tFirst = SystemClock.elapsedRealtime()
            var firstLogged = false
            upstream().collect { liveValue ->
                if (!firstLogged) {
                    PerfLog.d("VM", "flow#$flowId first-live in ${SystemClock.elapsedRealtime() - tFirst}ms empty=${isEmpty(liveValue)}")
                    firstLogged = true
                }
                when {
                    !isEmpty(liveValue) -> state.value = liveValue
                    isEmpty(state.value) -> {
                        val fallback = runCatching { withContext(Dispatchers.IO) { snapshot() } }.getOrNull()
                        state.value = if (fallback != null && !isEmpty(fallback)) fallback else liveValue
                    }
                }
            }
        }
        return state
    }

}
