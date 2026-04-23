package io.gelio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.gelio.app.data.local.entity.ArtGalleryCardEntity
import io.gelio.app.data.local.entity.ArtGalleryHeroEntity
import io.gelio.app.data.local.entity.BrochureEntity
import io.gelio.app.data.local.entity.CompanyEntity
import io.gelio.app.data.local.entity.ContentPageCardEntity
import io.gelio.app.data.local.entity.DestinationEntity
import io.gelio.app.data.local.entity.FeaturedProjectEntity
import io.gelio.app.data.local.entity.GlobalLinkEntity
import io.gelio.app.data.local.entity.SectionEntity
import io.gelio.app.data.local.entity.ServiceEntity
import io.gelio.app.data.local.entity.ShowcaseVideoEntity
import io.gelio.app.data.local.entity.ReviewCardEntity
import io.gelio.app.data.local.entity.RemoteImageAssetEntity
import io.gelio.app.data.local.entity.VirtualTourEntity
import io.gelio.app.data.local.entity.WorldMapPinEntity
import io.gelio.app.data.local.entity.WorldMapSectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowcaseDao {
    @Query("SELECT * FROM companies WHERE hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies ORDER BY sortOrder ASC")
    fun observeAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE id = :companyId LIMIT 1")
    fun observeCompany(companyId: String): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies ORDER BY sortOrder ASC")
    suspend fun getAllCompaniesSnapshot(): List<CompanyEntity>

    @Query("SELECT COUNT(*) FROM companies")
    suspend fun countCompanies(): Int

    @Upsert
    suspend fun upsertCompanies(companies: List<CompanyEntity>)

    @Upsert
    suspend fun upsertCompany(company: CompanyEntity)

    @Query("DELETE FROM companies WHERE id = :companyId")
    suspend fun deleteCompany(companyId: String)

    @Query("DELETE FROM companies")
    suspend fun deleteAllCompanies()

    @Query("SELECT * FROM sections WHERE companyId = :companyId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleSections(companyId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE companyId = :companyId ORDER BY sortOrder ASC")
    fun observeAllSections(companyId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :sectionId LIMIT 1")
    fun observeSection(sectionId: String): Flow<SectionEntity?>

    @Query("SELECT * FROM sections WHERE id = :sectionId LIMIT 1")
    suspend fun getSectionSnapshot(sectionId: String): SectionEntity?

    @Query("SELECT * FROM sections WHERE companyId = :companyId ORDER BY sortOrder ASC")
    suspend fun getSectionsSnapshot(companyId: String): List<SectionEntity>

    @Query("SELECT * FROM sections ORDER BY companyId ASC, sortOrder ASC")
    suspend fun getAllSectionsSnapshot(): List<SectionEntity>

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun countSections(): Int

    @Upsert
    suspend fun upsertSections(sections: List<SectionEntity>)

    @Upsert
    suspend fun upsertSection(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :sectionId")
    suspend fun deleteSection(sectionId: String)

    @Query("DELETE FROM sections")
    suspend fun deleteAllSections()

    @Query("SELECT * FROM world_map_sections WHERE sectionId = :sectionId LIMIT 1")
    fun observeWorldMapSection(sectionId: String): Flow<WorldMapSectionEntity?>

    @Query("SELECT * FROM world_map_sections WHERE sectionId = :sectionId LIMIT 1")
    suspend fun getWorldMapSectionSnapshot(sectionId: String): WorldMapSectionEntity?

    @Query("SELECT * FROM world_map_sections")
    suspend fun getAllWorldMapSectionsSnapshot(): List<WorldMapSectionEntity>

    @Upsert
    suspend fun upsertWorldMapSection(section: WorldMapSectionEntity)

    @Upsert
    suspend fun upsertWorldMapSections(sections: List<WorldMapSectionEntity>)

    @Query("DELETE FROM world_map_sections WHERE sectionId = :sectionId")
    suspend fun deleteWorldMapSection(sectionId: String)

    @Query("DELETE FROM world_map_sections")
    suspend fun deleteAllWorldMapSections()

    @Query("SELECT * FROM world_map_pins WHERE sectionId = :sectionId ORDER BY id ASC")
    fun observeWorldMapPins(sectionId: String): Flow<List<WorldMapPinEntity>>

    @Query("SELECT * FROM world_map_pins WHERE sectionId = :sectionId ORDER BY id ASC")
    suspend fun getWorldMapPinsSnapshot(sectionId: String): List<WorldMapPinEntity>

    @Query("SELECT * FROM world_map_pins ORDER BY sectionId ASC, id ASC")
    suspend fun getAllWorldMapPinsSnapshot(): List<WorldMapPinEntity>

    @Upsert
    suspend fun upsertWorldMapPins(pins: List<WorldMapPinEntity>)

    @Upsert
    suspend fun upsertWorldMapPin(pin: WorldMapPinEntity)

    @Query("DELETE FROM world_map_pins WHERE sectionId = :sectionId")
    suspend fun deleteWorldMapPinsForSection(sectionId: String)

    @Query("DELETE FROM world_map_pins")
    suspend fun deleteAllWorldMapPins()

    @Query("SELECT * FROM review_cards WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleReviewCards(sectionId: String): Flow<List<ReviewCardEntity>>

    @Query("SELECT * FROM review_cards WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllReviewCards(sectionId: String): Flow<List<ReviewCardEntity>>

    @Query("SELECT * FROM review_cards ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllReviewCardsSnapshot(): List<ReviewCardEntity>

    @Query("SELECT * FROM review_cards WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getReviewCardsSnapshot(sectionId: String): List<ReviewCardEntity>

    @Upsert
    suspend fun upsertReviewCards(items: List<ReviewCardEntity>)

    @Upsert
    suspend fun upsertReviewCard(item: ReviewCardEntity)

    @Query("DELETE FROM review_cards WHERE id = :id")
    suspend fun deleteReviewCard(id: String)

    @Query("DELETE FROM review_cards WHERE sectionId = :sectionId")
    suspend fun deleteReviewCardsForSection(sectionId: String)

    @Query("DELETE FROM review_cards")
    suspend fun deleteAllReviewCards()

    @Query("SELECT * FROM content_page_cards WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleContentPageCards(sectionId: String): Flow<List<ContentPageCardEntity>>

    @Query("SELECT * FROM content_page_cards WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllContentPageCards(sectionId: String): Flow<List<ContentPageCardEntity>>

    @Query("SELECT * FROM content_page_cards ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllContentPageCardsSnapshot(): List<ContentPageCardEntity>

    @Query("SELECT * FROM content_page_cards WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getContentPageCardsSnapshot(sectionId: String): List<ContentPageCardEntity>

    @Upsert
    suspend fun upsertContentPageCards(items: List<ContentPageCardEntity>)

    @Upsert
    suspend fun upsertContentPageCard(item: ContentPageCardEntity)

    @Query("DELETE FROM content_page_cards WHERE id = :id")
    suspend fun deleteContentPageCard(id: String)

    @Query("DELETE FROM content_page_cards WHERE sectionId = :sectionId")
    suspend fun deleteContentPageCardsForSection(sectionId: String)

    @Query("DELETE FROM content_page_cards")
    suspend fun deleteAllContentPageCards()

    @Query("SELECT * FROM art_gallery_heroes WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleArtGalleryHeroes(sectionId: String): Flow<List<ArtGalleryHeroEntity>>

    @Query("SELECT * FROM art_gallery_heroes WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllArtGalleryHeroes(sectionId: String): Flow<List<ArtGalleryHeroEntity>>

    @Query("SELECT * FROM art_gallery_heroes WHERE id = :heroId LIMIT 1")
    fun observeArtGalleryHero(heroId: String): Flow<ArtGalleryHeroEntity?>

    @Query("SELECT * FROM art_gallery_heroes WHERE id = :heroId LIMIT 1")
    suspend fun getArtGalleryHeroSnapshot(heroId: String): ArtGalleryHeroEntity?

    @Query("SELECT * FROM art_gallery_heroes WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getArtGalleryHeroesSnapshot(sectionId: String): List<ArtGalleryHeroEntity>

    @Query("SELECT * FROM art_gallery_heroes ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllArtGalleryHeroesSnapshot(): List<ArtGalleryHeroEntity>

    @Upsert
    suspend fun upsertArtGalleryHeroes(items: List<ArtGalleryHeroEntity>)

    @Upsert
    suspend fun upsertArtGalleryHero(item: ArtGalleryHeroEntity)

    @Query("DELETE FROM art_gallery_heroes WHERE id = :id")
    suspend fun deleteArtGalleryHero(id: String)

    @Query("DELETE FROM art_gallery_heroes WHERE sectionId = :sectionId")
    suspend fun deleteArtGalleryHeroesForSection(sectionId: String)

    @Query("DELETE FROM art_gallery_heroes")
    suspend fun deleteAllArtGalleryHeroes()

    @Query("SELECT * FROM art_gallery_cards WHERE heroId = :heroId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleArtGalleryCards(heroId: String): Flow<List<ArtGalleryCardEntity>>

    @Query("SELECT * FROM art_gallery_cards WHERE heroId = :heroId ORDER BY sortOrder ASC")
    fun observeAllArtGalleryCards(heroId: String): Flow<List<ArtGalleryCardEntity>>

    @Query("""
        SELECT c.* FROM art_gallery_cards c
        INNER JOIN art_gallery_heroes h ON c.heroId = h.id
        WHERE h.sectionId = :sectionId AND h.hidden = 0 AND c.hidden = 0
        ORDER BY h.sortOrder ASC, c.sortOrder ASC
    """)
    fun observeVisibleArtGalleryCardsForSection(sectionId: String): Flow<List<ArtGalleryCardEntity>>

    @Query("""
        SELECT c.* FROM art_gallery_cards c
        INNER JOIN art_gallery_heroes h ON c.heroId = h.id
        WHERE h.sectionId = :sectionId
        ORDER BY h.sortOrder ASC, c.sortOrder ASC
    """)
    fun observeAllArtGalleryCardsForSection(sectionId: String): Flow<List<ArtGalleryCardEntity>>

    @Query("SELECT * FROM art_gallery_cards WHERE heroId = :heroId ORDER BY sortOrder ASC")
    suspend fun getArtGalleryCardsSnapshot(heroId: String): List<ArtGalleryCardEntity>

    @Query("""
        SELECT c.* FROM art_gallery_cards c
        INNER JOIN art_gallery_heroes h ON c.heroId = h.id
        WHERE h.sectionId = :sectionId AND h.hidden = 0 AND c.hidden = 0
        ORDER BY h.sortOrder ASC, c.sortOrder ASC
    """)
    suspend fun getVisibleArtGalleryCardsForSectionSnapshot(sectionId: String): List<ArtGalleryCardEntity>

    @Query("""
        SELECT c.* FROM art_gallery_cards c
        INNER JOIN art_gallery_heroes h ON c.heroId = h.id
        WHERE h.sectionId = :sectionId
        ORDER BY h.sortOrder ASC, c.sortOrder ASC
    """)
    suspend fun getArtGalleryCardsForSectionSnapshot(sectionId: String): List<ArtGalleryCardEntity>

    @Query("SELECT * FROM art_gallery_cards ORDER BY heroId ASC, sortOrder ASC")
    suspend fun getAllArtGalleryCardsSnapshot(): List<ArtGalleryCardEntity>

    @Upsert
    suspend fun upsertArtGalleryCards(items: List<ArtGalleryCardEntity>)

    @Upsert
    suspend fun upsertArtGalleryCard(item: ArtGalleryCardEntity)

    @Query("DELETE FROM art_gallery_cards WHERE id = :id")
    suspend fun deleteArtGalleryCard(id: String)

    @Query("DELETE FROM art_gallery_cards WHERE heroId = :heroId")
    suspend fun deleteArtGalleryCardsForHero(heroId: String)

    @Query("DELETE FROM art_gallery_cards WHERE heroId IN (SELECT id FROM art_gallery_heroes WHERE sectionId = :sectionId)")
    suspend fun deleteArtGalleryCardsForSection(sectionId: String)

    @Query("DELETE FROM art_gallery_cards")
    suspend fun deleteAllArtGalleryCards()

    @Query("SELECT * FROM remote_image_assets ORDER BY importedAt ASC")
    suspend fun getAllRemoteImageAssetsSnapshot(): List<RemoteImageAssetEntity>

    @Upsert
    suspend fun upsertRemoteImageAssets(items: List<RemoteImageAssetEntity>)

    @Upsert
    suspend fun upsertRemoteImageAsset(item: RemoteImageAssetEntity)

    @Query("DELETE FROM remote_image_assets WHERE localPath = :localPath")
    suspend fun deleteRemoteImageAsset(localPath: String)

    @Query("DELETE FROM remote_image_assets WHERE localPath IN (:localPaths)")
    suspend fun deleteRemoteImageAssets(localPaths: List<String>)

    @Query("DELETE FROM remote_image_assets")
    suspend fun deleteAllRemoteImageAssets()

    @Query("SELECT * FROM featured_projects WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleFeaturedProjects(sectionId: String): Flow<List<FeaturedProjectEntity>>

    @Query("SELECT * FROM featured_projects WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllFeaturedProjects(sectionId: String): Flow<List<FeaturedProjectEntity>>

    @Query("SELECT * FROM featured_projects WHERE id = :id LIMIT 1")
    fun observeFeaturedProject(id: String): Flow<FeaturedProjectEntity?>

    @Upsert
    suspend fun upsertFeaturedProjects(projects: List<FeaturedProjectEntity>)

    @Upsert
    suspend fun upsertFeaturedProject(project: FeaturedProjectEntity)

    @Query("DELETE FROM featured_projects WHERE id = :id")
    suspend fun deleteFeaturedProject(id: String)

    @Query("DELETE FROM featured_projects WHERE sectionId = :sectionId")
    suspend fun deleteFeaturedProjectsForSection(sectionId: String)

    @Query("SELECT COUNT(*) FROM featured_projects")
    suspend fun countFeaturedProjects(): Int

    @Query("SELECT * FROM featured_projects WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getFeaturedProjectsSnapshot(sectionId: String): List<FeaturedProjectEntity>

    @Query("SELECT * FROM featured_projects ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllFeaturedProjectsSnapshot(): List<FeaturedProjectEntity>

    @Query("SELECT * FROM virtual_tours WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleVirtualTours(sectionId: String): Flow<List<VirtualTourEntity>>

    @Query("SELECT * FROM virtual_tours WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllVirtualTours(sectionId: String): Flow<List<VirtualTourEntity>>

    @Query("SELECT * FROM virtual_tours WHERE id = :id LIMIT 1")
    fun observeVirtualTour(id: String): Flow<VirtualTourEntity?>

    @Upsert
    suspend fun upsertVirtualTours(tours: List<VirtualTourEntity>)

    @Upsert
    suspend fun upsertVirtualTour(tour: VirtualTourEntity)

    @Query("DELETE FROM virtual_tours WHERE id = :id")
    suspend fun deleteVirtualTour(id: String)

    @Query("DELETE FROM virtual_tours WHERE sectionId = :sectionId")
    suspend fun deleteVirtualToursForSection(sectionId: String)

    @Query("SELECT * FROM virtual_tours WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getVirtualToursSnapshot(sectionId: String): List<VirtualTourEntity>

    @Query("SELECT * FROM virtual_tours ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllVirtualToursSnapshot(): List<VirtualTourEntity>

    @Query("SELECT * FROM videos WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleVideos(sectionId: String): Flow<List<ShowcaseVideoEntity>>

    @Query("SELECT * FROM videos WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllVideos(sectionId: String): Flow<List<ShowcaseVideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    fun observeVideo(id: String): Flow<ShowcaseVideoEntity?>

    @Query("SELECT * FROM videos WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getVideosSnapshot(sectionId: String): List<ShowcaseVideoEntity>

    @Query("SELECT * FROM videos ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllVideosSnapshot(): List<ShowcaseVideoEntity>

    @Upsert
    suspend fun upsertVideos(videos: List<ShowcaseVideoEntity>)

    @Upsert
    suspend fun upsertVideo(video: ShowcaseVideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String)

    @Query("DELETE FROM videos WHERE sectionId = :sectionId")
    suspend fun deleteVideosForSection(sectionId: String)

    @Query("SELECT * FROM brochures WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleBrochures(sectionId: String): Flow<List<BrochureEntity>>

    @Query("SELECT * FROM brochures WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllBrochures(sectionId: String): Flow<List<BrochureEntity>>

    @Query("SELECT * FROM brochures WHERE id = :id LIMIT 1")
    fun observeBrochure(id: String): Flow<BrochureEntity?>

    @Upsert
    suspend fun upsertBrochures(brochures: List<BrochureEntity>)

    @Upsert
    suspend fun upsertBrochure(brochure: BrochureEntity)

    @Query("SELECT * FROM brochures WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getBrochuresSnapshot(sectionId: String): List<BrochureEntity>

    @Query("SELECT * FROM brochures ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllBrochuresSnapshot(): List<BrochureEntity>

    @Query("SELECT * FROM brochures WHERE brand = :brand ORDER BY sortOrder ASC")
    suspend fun getAllBrochuresSnapshot(brand: String): List<BrochureEntity>

    @Query("DELETE FROM brochures WHERE id = :id")
    suspend fun deleteBrochure(id: String)

    @Query("DELETE FROM brochures WHERE sectionId = :sectionId")
    suspend fun deleteBrochuresForSection(sectionId: String)

    @Query("SELECT * FROM destinations WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleDestinations(sectionId: String): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllDestinations(sectionId: String): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations WHERE id = :id LIMIT 1")
    fun observeDestination(id: String): Flow<DestinationEntity?>

    @Query("SELECT * FROM destinations WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getDestinationsSnapshot(sectionId: String): List<DestinationEntity>

    @Query("SELECT * FROM destinations ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllDestinationsSnapshot(): List<DestinationEntity>

    @Upsert
    suspend fun upsertDestinations(destinations: List<DestinationEntity>)

    @Upsert
    suspend fun upsertDestination(destination: DestinationEntity)

    @Query("DELETE FROM destinations WHERE id = :id")
    suspend fun deleteDestination(id: String)

    @Query("DELETE FROM destinations WHERE sectionId = :sectionId")
    suspend fun deleteDestinationsForSection(sectionId: String)

    @Query("SELECT * FROM services WHERE sectionId = :sectionId AND hidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleServices(sectionId: String): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    fun observeAllServices(sectionId: String): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    fun observeService(id: String): Flow<ServiceEntity?>

    @Query("SELECT * FROM services WHERE sectionId = :sectionId ORDER BY sortOrder ASC")
    suspend fun getServicesSnapshot(sectionId: String): List<ServiceEntity>

    @Query("SELECT * FROM services ORDER BY sectionId ASC, sortOrder ASC")
    suspend fun getAllServicesSnapshot(): List<ServiceEntity>

    @Upsert
    suspend fun upsertServices(services: List<ServiceEntity>)

    @Upsert
    suspend fun upsertService(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteService(id: String)

    @Query("DELETE FROM services WHERE sectionId = :sectionId")
    suspend fun deleteServicesForSection(sectionId: String)

    @Query("SELECT * FROM global_links WHERE id = :id LIMIT 1")
    fun observeGlobalLink(id: String): Flow<GlobalLinkEntity?>

    @Query("SELECT * FROM global_links WHERE sectionId = :sectionId ORDER BY label ASC")
    fun observeGlobalLinks(sectionId: String): Flow<List<GlobalLinkEntity>>

    @Query("SELECT * FROM global_links WHERE sectionId = :sectionId ORDER BY label ASC")
    suspend fun getGlobalLinksSnapshot(sectionId: String): List<GlobalLinkEntity>

    @Query("SELECT * FROM global_links ORDER BY sectionId ASC, label ASC")
    suspend fun getAllGlobalLinksSnapshot(): List<GlobalLinkEntity>

    @Upsert
    suspend fun upsertGlobalLinks(links: List<GlobalLinkEntity>)

    @Upsert
    suspend fun upsertGlobalLink(link: GlobalLinkEntity)

    @Query("DELETE FROM global_links WHERE id = :id")
    suspend fun deleteGlobalLink(id: String)

    @Query("SELECT COUNT(*) FROM global_links")
    suspend fun countGlobalLinks(): Int

    @Query("DELETE FROM global_links WHERE sectionId = :sectionId")
    suspend fun deleteGlobalLinksForSection(sectionId: String)

    @Query("DELETE FROM featured_projects")
    suspend fun deleteAllFeaturedProjects()

    @Query("DELETE FROM virtual_tours")
    suspend fun deleteAllVirtualTours()

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Query("DELETE FROM brochures")
    suspend fun deleteAllBrochures()

    @Query("DELETE FROM destinations")
    suspend fun deleteAllDestinations()

    @Query("DELETE FROM services")
    suspend fun deleteAllServices()

    @Query("DELETE FROM global_links")
    suspend fun deleteAllGlobalLinks()

    @Transaction
    suspend fun replaceAllContent(
        companies: List<CompanyEntity> = emptyList(),
        sections: List<SectionEntity> = emptyList(),
        featuredProjects: List<FeaturedProjectEntity> = emptyList(),
        virtualTours: List<VirtualTourEntity> = emptyList(),
        videos: List<ShowcaseVideoEntity> = emptyList(),
        brochures: List<BrochureEntity> = emptyList(),
        destinations: List<DestinationEntity> = emptyList(),
        services: List<ServiceEntity> = emptyList(),
        globalLinks: List<GlobalLinkEntity> = emptyList(),
        worldMapSections: List<WorldMapSectionEntity> = emptyList(),
        worldMapPins: List<WorldMapPinEntity> = emptyList(),
        reviewCards: List<ReviewCardEntity> = emptyList(),
        contentPageCards: List<ContentPageCardEntity> = emptyList(),
        artGalleryHeroes: List<ArtGalleryHeroEntity> = emptyList(),
        artGalleryCards: List<ArtGalleryCardEntity> = emptyList(),
        remoteImageAssets: List<RemoteImageAssetEntity> = emptyList(),
    ) {
        deleteAllWorldMapPins()
        deleteAllWorldMapSections()
        deleteAllReviewCards()
        deleteAllContentPageCards()
        deleteAllArtGalleryCards()
        deleteAllArtGalleryHeroes()
        deleteAllRemoteImageAssets()
        deleteAllFeaturedProjects()
        deleteAllVirtualTours()
        deleteAllVideos()
        deleteAllBrochures()
        deleteAllDestinations()
        deleteAllServices()
        deleteAllGlobalLinks()
        deleteAllSections()
        deleteAllCompanies()

        upsertCompanies(companies)
        upsertSections(sections)
        upsertFeaturedProjects(featuredProjects)
        upsertVirtualTours(virtualTours)
        upsertVideos(videos)
        upsertBrochures(brochures)
        upsertDestinations(destinations)
        upsertServices(services)
        upsertGlobalLinks(globalLinks)
        upsertWorldMapSections(worldMapSections)
        upsertWorldMapPins(worldMapPins)
        upsertReviewCards(reviewCards)
        upsertContentPageCards(contentPageCards)
        upsertArtGalleryHeroes(artGalleryHeroes)
        upsertArtGalleryCards(artGalleryCards)
        upsertRemoteImageAssets(remoteImageAssets)
    }
}
