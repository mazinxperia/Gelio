package io.gelio.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrowseGallery
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.RoomService
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import io.gelio.app.core.util.encodeNavArg

enum class DesignSection(
    val routeKey: String,
    val label: String,
    val icon: ImageVector,
) {
    IMAGES("images", "Project Images", Icons.Rounded.BrowseGallery),
    TOURS("tours", "Virtual Tours", Icons.Rounded.TravelExplore),
    VIDEOS("videos", "YouTube Videos", Icons.Rounded.VideoLibrary),
    BROCHURES("brochures", "Brochures", Icons.Rounded.Preview);

    companion object {
        fun fromRouteKey(value: String?): DesignSection =
            entries.firstOrNull { it.routeKey == value } ?: IMAGES
    }
}

enum class TourismSection(
    val routeKey: String,
    val label: String,
    val icon: ImageVector,
) {
    DESTINATIONS("destinations", "Destinations", Icons.Rounded.Map),
    SERVICES("services", "Services", Icons.Rounded.RoomService),
    BROCHURES("brochures", "Brochures", Icons.Rounded.Luggage);

    companion object {
        fun fromRouteKey(value: String?): TourismSection =
            entries.firstOrNull { it.routeKey == value } ?: DESTINATIONS
    }
}

object AppDestinations {
    const val CLIENT_STAGE = "client-stage"
    const val WELCOME = "welcome"
    const val BRAND_SELECTION = "brand_selection"
    const val ADMIN_HOME = "admin"
    const val ADMIN_SETTINGS = "admin/settings"
    const val ADMIN_KIOSK = "admin/kiosk"
    const val ADMIN_BACKUP_IMPORT = "admin/settings/backup-import"
    const val ADMIN_CLEAR_DATA = "admin/clear-data"
    const val ADMIN_SECTIONS = "admin/sections"
    const val ADMIN_COMPANY_SECTIONS = "admin/sections/{companyId}"
    fun adminCompanySections(companyId: String): String = "admin/sections/${encodeNavArg(companyId)}"
    const val ADMIN_SECTION_EDITOR = "admin/sections/editor/{sectionId}"
    fun adminSectionEditor(sectionId: String): String = "admin/sections/editor/${encodeNavArg(sectionId)}"
    const val ADMIN_ART_GALLERY_ITEMS_EDITOR = "admin/sections/editor/{sectionId}/hero/{heroId}"
    fun adminArtGalleryItemsEditor(sectionId: String, heroId: String): String =
        "admin/sections/editor/${encodeNavArg(sectionId)}/hero/${encodeNavArg(heroId)}"
    const val DESIGN_ADMIN = "admin/design"
    const val DESIGN_FEATURED_PROJECTS_ADMIN = "admin/design/featured-projects"
    const val DESIGN_VIRTUAL_TOURS_ADMIN = "admin/design/virtual-tours"
    const val DESIGN_VIDEOS_ADMIN = "admin/design/videos"
    const val DESIGN_BROCHURES_ADMIN = "admin/design/brochures"
    const val TOURISM_ADMIN = "admin/tourism"
    const val TOURISM_DESTINATIONS_ADMIN = "admin/tourism/destinations"
    const val SERVICES_ADMIN = "admin/tourism/services"
    const val TOURISM_BROCHURES_ADMIN = "admin/tourism/brochures"

    const val DESIGN_SHELL = "design/{section}"
    fun design(section: DesignSection): String = "design/${section.routeKey}"

    const val TOURISM_SHELL = "tourism/{section}"
    fun tourism(section: TourismSection): String = "tourism/${section.routeKey}"

    const val GALLERY = "gallery/{projectId}"
    fun gallery(projectId: String): String = "gallery/${encodeNavArg(projectId)}"

    const val TOUR_VIEWER = "viewer/tour/{tourId}"
    fun tourViewer(tourId: String): String = "viewer/tour/${encodeNavArg(tourId)}"

    const val VIDEO_VIEWER = "viewer/video/{videoId}"
    fun videoViewer(videoId: String): String = "viewer/video/${encodeNavArg(videoId)}"

    const val BROCHURE_VIEWER = "viewer/brochure/{brochureId}"
    fun brochureViewer(brochureId: String): String = "viewer/brochure/${encodeNavArg(brochureId)}"

    const val WEB_VIEWER = "viewer/web?title={title}&url={url}&home={home}&close={close}"
    fun webViewer(
        title: String,
        url: String,
        homeRoute: String,
        closeRoute: String,
    ): String = "viewer/web?title=${encodeNavArg(title)}&url=${encodeNavArg(url)}&home=${encodeNavArg(homeRoute)}&close=${encodeNavArg(closeRoute)}"
}
