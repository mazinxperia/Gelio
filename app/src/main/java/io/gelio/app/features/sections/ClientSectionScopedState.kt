package io.gelio.app.features.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.gelio.app.app.ShowcaseViewModel
import io.gelio.app.core.util.cleanVirtualTourThumbnailUrl
import io.gelio.app.core.util.youtubeThumbnail
import io.gelio.app.data.model.Brochure
import io.gelio.app.data.model.ArtGalleryHeroGroup
import io.gelio.app.data.model.ContentPageCard
import io.gelio.app.data.model.Destination
import io.gelio.app.data.model.FeaturedProject
import io.gelio.app.data.model.ReviewCard
import io.gelio.app.data.model.SectionType
import io.gelio.app.data.model.Service
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.data.model.ShowcaseVideo
import io.gelio.app.data.model.VirtualTour
import io.gelio.app.data.model.WorldMapPin
import io.gelio.app.data.model.WorldMapSection
import io.gelio.app.features.design.DesignStripItem

internal data class ClientSectionScopedState(
    val section: ShowcaseSection?,
    val featuredProjects: List<FeaturedProject> = emptyList(),
    val virtualTours: List<VirtualTour> = emptyList(),
    val videos: List<ShowcaseVideo> = emptyList(),
    val brochures: List<Brochure> = emptyList(),
    val destinations: List<Destination> = emptyList(),
    val services: List<Service> = emptyList(),
    val allProjectsLink: String = "",
    val worldMapSection: WorldMapSection? = null,
    val worldMapPins: List<WorldMapPin> = emptyList(),
    val reviewCards: List<ReviewCard> = emptyList(),
    val contentPageCards: List<ContentPageCard> = emptyList(),
    val artGalleryGroups: List<ArtGalleryHeroGroup> = emptyList(),
    val stripItems: List<DesignStripItem> = emptyList(),
)

/**
 * Single safe loader path for client sections.
 *
 * Future client section renderers should read section-scoped data through this helper instead of
 * sharing one outer "active section payload" object across multiple tabs.
 */
@Composable
internal fun rememberClientSectionScopedState(
    section: ShowcaseSection?,
    showcaseViewModel: ShowcaseViewModel,
): ClientSectionScopedState {
    val sectionId = section?.id.orEmpty()
    return when (section?.type) {
        null -> remember(section) { ClientSectionScopedState(section = section) }

        SectionType.IMAGE_GALLERY -> {
            val featuredProjects by showcaseViewModel.featuredProjects(sectionId).collectAsStateWithLifecycle()
            val sectionLinks by showcaseViewModel.sectionLinks(sectionId).collectAsStateWithLifecycle()
            val allProjectsLink = remember(sectionLinks) {
                sectionLinks.firstOrNull {
                    it.label.equals("All Items", ignoreCase = true) ||
                        it.label.equals("All Projects", ignoreCase = true)
                }?.url.orEmpty()
            }
            val stripItems = remember(featuredProjects) {
                featuredProjects.map {
                    DesignStripItem(
                        id = it.id,
                        title = it.projectName,
                        imageUri = it.thumbnailUri,
                        source = it,
                    )
                }
            }

            remember(section, featuredProjects, allProjectsLink, stripItems) {
                ClientSectionScopedState(
                    section = section,
                    featuredProjects = featuredProjects,
                    allProjectsLink = allProjectsLink,
                    stripItems = stripItems,
                )
            }
        }

        SectionType.TOUR_360 -> {
            val virtualTours by showcaseViewModel.virtualTours(sectionId).collectAsStateWithLifecycle()
            val autoTourThumbnails by showcaseViewModel.virtualTourAutoThumbnails(sectionId).collectAsStateWithLifecycle()
            val stripItems = remember(virtualTours, autoTourThumbnails) {
                virtualTours.map {
                    DesignStripItem(
                        id = it.id,
                        title = it.projectName,
                        imageUri = it.thumbnailUri
                            .ifBlank { autoTourThumbnails[it.id].orEmpty() }
                            .let(::cleanVirtualTourThumbnailUrl),
                        source = it,
                    )
                }
            }

            remember(section, virtualTours, stripItems) {
                ClientSectionScopedState(
                    section = section,
                    virtualTours = virtualTours,
                    stripItems = stripItems,
                )
            }
        }

        SectionType.YOUTUBE_VIDEOS -> {
            val videos by showcaseViewModel.videos(sectionId).collectAsStateWithLifecycle()
            val stripItems = remember(videos) {
                videos.map {
                    DesignStripItem(
                        id = it.id,
                        title = it.title,
                        imageUri = it.thumbnailUri ?: youtubeThumbnail(it.youtubeLink).orEmpty(),
                        source = it,
                    )
                }
            }

            remember(section, videos, stripItems) {
                ClientSectionScopedState(
                    section = section,
                    videos = videos,
                    stripItems = stripItems,
                )
            }
        }

        SectionType.PDF_VIEWER -> {
            val brochures by showcaseViewModel.brochures(sectionId).collectAsStateWithLifecycle()
            val stripItems = remember(brochures) {
                brochures.map {
                    DesignStripItem(
                        id = it.id,
                        title = it.title,
                        imageUri = it.coverThumbnailUri,
                        source = it,
                    )
                }
            }

            remember(section, brochures, stripItems) {
                ClientSectionScopedState(
                    section = section,
                    brochures = brochures,
                    stripItems = stripItems,
                )
            }
        }

        SectionType.DESTINATIONS -> {
            val destinations by showcaseViewModel.destinations(sectionId).collectAsStateWithLifecycle()
            remember(section, destinations) {
                ClientSectionScopedState(
                    section = section,
                    destinations = destinations,
                )
            }
        }

        SectionType.SERVICES -> {
            val services by showcaseViewModel.services(sectionId).collectAsStateWithLifecycle()
            remember(section, services) {
                ClientSectionScopedState(
                    section = section,
                    services = services,
                )
            }
        }

        SectionType.WORLD_MAP -> {
            val worldMapSection by showcaseViewModel.worldMapSection(sectionId).collectAsStateWithLifecycle()
            val worldMapPins by showcaseViewModel.worldMapPins(sectionId).collectAsStateWithLifecycle()
            remember(section, worldMapSection, worldMapPins) {
                ClientSectionScopedState(
                    section = section,
                    worldMapSection = worldMapSection,
                    worldMapPins = worldMapPins,
                )
            }
        }

        SectionType.GOOGLE_REVIEWS -> {
            val reviewCards by showcaseViewModel.reviewCards(sectionId).collectAsStateWithLifecycle()
            remember(section, reviewCards) {
                ClientSectionScopedState(
                    section = section,
                    reviewCards = reviewCards,
                )
            }
        }

        SectionType.CONTENT_PAGE -> {
            val contentPageCards by showcaseViewModel.contentPageCards(sectionId).collectAsStateWithLifecycle()
            remember(section, contentPageCards) {
                ClientSectionScopedState(
                    section = section,
                    contentPageCards = contentPageCards,
                )
            }
        }

        SectionType.ART_GALLERY -> {
            val artGalleryGroups by showcaseViewModel.artGalleryGroups(sectionId).collectAsStateWithLifecycle()
            remember(section, artGalleryGroups) {
                ClientSectionScopedState(
                    section = section,
                    artGalleryGroups = artGalleryGroups,
                )
            }
        }

        SectionType.REGION_MAP -> remember(section) { ClientSectionScopedState(section = section) }
    }
}
