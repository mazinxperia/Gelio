package io.gelio.app.core.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.gelio.app.app.BackupViewModel
import io.gelio.app.app.ClearDataViewModel
import io.gelio.app.app.LocalAppContainer
import io.gelio.app.app.SettingsViewModel
import io.gelio.app.app.ShowcaseViewModel
import io.gelio.app.core.theme.LocalNavAnimatedVisibilityScope
import io.gelio.app.core.theme.LocalSharedTransitionScope
import io.gelio.app.core.theme.rememberShowcaseNavTransitions
import io.gelio.app.core.ui.AdminPinGateDialog
import io.gelio.app.core.util.decodeNavArg
import io.gelio.app.data.model.SectionType
import io.gelio.app.features.admin.AdminHomeScreen
import io.gelio.app.features.admin.appsettings.AppSettingsScreen
import io.gelio.app.features.admin.backup.BackupImportScreen
import io.gelio.app.features.admin.brochures.BrochuresAdminScreen
import io.gelio.app.features.admin.cleardata.ClearDataScreen
import io.gelio.app.features.admin.designadmin.DesignVideosAdminScreen
import io.gelio.app.features.admin.designadmin.FeaturedProjectsAdminScreen
import io.gelio.app.features.admin.kiosk.KioskModeScreen
import io.gelio.app.features.admin.kiosk.KioskModeViewModel
import io.gelio.app.features.admin.designadmin.VirtualToursAdminScreen
import io.gelio.app.features.admin.sections.CompanySectionsAdminScreen
import io.gelio.app.features.admin.sections.ArtGalleryAdminScreen
import io.gelio.app.features.admin.sections.ArtGalleryHeroItemsAdminScreen
import io.gelio.app.features.admin.sections.ContentPageAdminScreen
import io.gelio.app.features.admin.sections.RatingsAdminScreen
import io.gelio.app.features.admin.sections.SectionsAdminScreen
import io.gelio.app.features.admin.sections.WorldMapAdminScreen
import io.gelio.app.features.admin.tourismadmin.DestinationsAdminScreen
import io.gelio.app.features.admin.tourismadmin.ServicesAdminScreen
import io.gelio.app.features.clientstage.ClientShowcaseStage
import io.gelio.app.media.gallery.ProjectGalleryScreen
import io.gelio.app.media.pdf.BrochureViewerScreen
import io.gelio.app.media.video.VideoViewerScreen
import io.gelio.app.media.webview.WebViewerScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GelioNavHost(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    showcaseViewModel: ShowcaseViewModel,
    backupViewModel: BackupViewModel,
    clearDataViewModel: ClearDataViewModel,
    clientStageTarget: ClientStageTarget,
    onClientStageTargetChange: (ClientStageTarget) -> Unit,
) {
    val navTransitions = rememberShowcaseNavTransitions()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var showAdminPinGate by rememberSaveable { mutableStateOf(false) }

    fun navigateClientHome() {
        onClientStageTargetChange(ClientStageTarget.Welcome)
        navController.navigate(AppDestinations.CLIENT_STAGE) {
            launchSingleTop = true
            popUpTo(AppDestinations.CLIENT_STAGE) {
                inclusive = false
            }
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = AppDestinations.CLIENT_STAGE,
                enterTransition = {
                    navTransitions.forwardEnter(initialState.destination.route, targetState.destination.route)
                },
                exitTransition = {
                    navTransitions.forwardExit(initialState.destination.route, targetState.destination.route)
                },
                popEnterTransition = {
                    navTransitions.backEnter(initialState.destination.route, targetState.destination.route)
                },
                popExitTransition = {
                    navTransitions.backExit(initialState.destination.route, targetState.destination.route)
                },
            ) {
                composable(AppDestinations.CLIENT_STAGE) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        ClientShowcaseStage(
                            target = clientStageTarget,
                            settings = settings,
                            showcaseViewModel = showcaseViewModel,
                            onTargetChange = onClientStageTargetChange,
                            onAdminOpen = { showAdminPinGate = true },
                            onProjectClick = { navController.navigate(AppDestinations.gallery(it)) },
                            onTourClick = { navController.navigate(AppDestinations.tourViewer(it)) },
                            onAllProjectsClick = { url ->
                                if (url.isNotBlank()) {
                                    navController.navigate(
                                        AppDestinations.webViewer(
                                            title = "All Items",
                                            url = url,
                                            homeRoute = AppDestinations.CLIENT_STAGE,
                                            closeRoute = AppDestinations.CLIENT_STAGE,
                                        ),
                                    )
                                }
                            },
                            onVideoClick = { navController.navigate(AppDestinations.videoViewer(it)) },
                            onBrochureClick = { navController.navigate(AppDestinations.brochureViewer(it)) },
                        )
                    }
                }

                composable(
                    route = AppDestinations.GALLERY,
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val projectId = decodeNavArg(backStackEntry.arguments?.getString("projectId"))
                    val project = showcaseViewModel.featuredProject(projectId).collectAsStateWithLifecycle(initialValue = null).value
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        ProjectGalleryScreen(
                            project = project,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navigateClientHome() },
                        )
                    }
                }

                composable(
                    route = AppDestinations.TOUR_VIEWER,
                    arguments = listOf(navArgument("tourId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val tourId = decodeNavArg(backStackEntry.arguments?.getString("tourId"))
                    val tour = showcaseViewModel.virtualTour(tourId).collectAsStateWithLifecycle(initialValue = null).value
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        WebViewerScreen(
                            title = tour?.projectName ?: "Virtual Tour",
                            subtitle = "Interactive Tour",
                            url = tour?.embedUrl.orEmpty(),
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navigateClientHome() },
                        )
                    }
                }

                composable(
                    route = AppDestinations.VIDEO_VIEWER,
                    arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val videoId = decodeNavArg(backStackEntry.arguments?.getString("videoId"))
                    val video = showcaseViewModel.video(videoId).collectAsStateWithLifecycle(initialValue = null).value
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        VideoViewerScreen(
                            video = video,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navigateClientHome() },
                        )
                    }
                }

                composable(
                    route = AppDestinations.BROCHURE_VIEWER,
                    arguments = listOf(navArgument("brochureId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val brochureId = decodeNavArg(backStackEntry.arguments?.getString("brochureId"))
                    val brochure = showcaseViewModel.brochure(brochureId).collectAsStateWithLifecycle(initialValue = null).value
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        BrochureViewerScreen(
                            brochure = brochure,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navigateClientHome() },
                        )
                    }
                }

                composable(
                    route = AppDestinations.WEB_VIEWER,
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                        navArgument("url") { type = NavType.StringType },
                        navArgument("home") { type = NavType.StringType },
                        navArgument("close") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val title = decodeNavArg(backStackEntry.arguments?.getString("title"))
                    val url = decodeNavArg(backStackEntry.arguments?.getString("url"))
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        WebViewerScreen(
                            title = title,
                            url = url,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navigateClientHome() },
                        )
                    }
                }

                composable(AppDestinations.ADMIN_HOME) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        AdminHomeScreen(
                            onSettingsClick = { navController.navigate(AppDestinations.ADMIN_SETTINGS) },
                            onBackupImportClick = { navController.navigate(AppDestinations.ADMIN_BACKUP_IMPORT) },
                            onClearDataClick = { navController.navigate(AppDestinations.ADMIN_CLEAR_DATA) },
                            onSectionsClick = { navController.navigate(AppDestinations.ADMIN_SECTIONS) },
                            onHomeClick = { navigateClientHome() },
                        )
                    }
                }

                composable(AppDestinations.ADMIN_SETTINGS) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val settings = settingsViewModel.uiState.collectAsStateWithLifecycle().value
                        val pexelsApiKeySaveState = settingsViewModel.pexelsApiKeySaveState.collectAsStateWithLifecycle().value
                        AppSettingsScreen(
                            settings = settings,
                            pexelsApiKeySaveState = pexelsApiKeySaveState,
                            onThemeModeChange = settingsViewModel::updateThemeMode,
                            onColorModeChange = settingsViewModel::updateColorMode,
                            onCuratedPaletteChange = settingsViewModel::updateCuratedPalette,
                            onNeutralBaseColorChange = settingsViewModel::updateNeutralBaseColor,
                            onIdleTimeoutChange = settingsViewModel::updateIdleTimeoutMinutes,
                            onAdminPinChange = settingsViewModel::updateAdminPin,
                            onIdleHeroTitleChange = settingsViewModel::updateIdleHeroTitle,
                            onIdleHeroCaptionChange = settingsViewModel::updateIdleHeroCaption,
                            onTestAndSavePexelsApiKey = settingsViewModel::testAndSavePexelsApiKey,
                            onClearPexelsApiKey = settingsViewModel::clearPexelsApiKey,
                            onClearPexelsApiKeyFeedback = settingsViewModel::clearPexelsApiKeyFeedback,
                            onKioskModeClick = { navController.navigate(AppDestinations.ADMIN_KIOSK) },
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navController.navigate(AppDestinations.ADMIN_HOME) { launchSingleTop = true } },
                        )
                    }
                }

                composable(AppDestinations.ADMIN_KIOSK) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val kioskViewModel: KioskModeViewModel = viewModel(
                            factory = KioskModeViewModel.factory(LocalAppContainer.current),
                        )
                        KioskModeScreen(
                            viewModel = kioskViewModel,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navController.navigate(AppDestinations.ADMIN_SETTINGS) { launchSingleTop = true } },
                        )
                    }
                }

                composable(AppDestinations.ADMIN_BACKUP_IMPORT) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val backupUiState = backupViewModel.uiState.collectAsStateWithLifecycle().value
                        BackupImportScreen(
                            uiState = backupUiState,
                            onExport = backupViewModel::exportBackup,
                            onImportFileSelected = backupViewModel::selectImportFile,
                            onImport = backupViewModel::importBackup,
                            onClearSession = backupViewModel::clearSession,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navController.navigate(AppDestinations.ADMIN_HOME) { launchSingleTop = true } },
                        )
                    }
                }

                composable(AppDestinations.ADMIN_CLEAR_DATA) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val clearDataUiState = clearDataViewModel.uiState.collectAsStateWithLifecycle().value
                        ClearDataScreen(
                            uiState = clearDataUiState,
                            onScan = clearDataViewModel::scanAppData,
                            onClearEverything = {
                                clearDataViewModel.clearEverything(
                                    onAfterSuccessfulReset = backupViewModel::clearSession,
                                )
                            },
                            onClearSession = clearDataViewModel::clearSession,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navController.navigate(AppDestinations.ADMIN_HOME) { launchSingleTop = true } },
                        )
                    }
                }

                composable(AppDestinations.ADMIN_SECTIONS) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val companies = showcaseViewModel.adminCompanies.collectAsStateWithLifecycle().value
                        SectionsAdminScreen(
                            companies = companies,
                            settings = settings,
                            onUpdateHomescreenLogo = settingsViewModel::updateHomescreenLogoPath,
                            onCompanyClick = { navController.navigate(AppDestinations.adminCompanySections(it)) },
                            onCreateCompany = { name, logoPath, brandSeedColor ->
                                showcaseViewModel.createCompany(name, logoPath, brandSeedColor)
                            },
                            onUpdateCompany = showcaseViewModel::updateCompany,
                            onToggleCompanyVisibility = showcaseViewModel::toggleCompanyVisibility,
                            onMoveCompany = showcaseViewModel::moveCompany,
                            onDeleteCompany = showcaseViewModel::deleteCompany,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navController.navigate(AppDestinations.ADMIN_HOME) { launchSingleTop = true } },
                        )
                    }
                }

                composable(
                    route = AppDestinations.ADMIN_COMPANY_SECTIONS,
                    arguments = listOf(navArgument("companyId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val companyId = decodeNavArg(backStackEntry.arguments?.getString("companyId"))
                    val company = showcaseViewModel.company(companyId).collectAsStateWithLifecycle(initialValue = null).value
                    val sections = showcaseViewModel.adminSections(companyId).collectAsStateWithLifecycle(initialValue = emptyList()).value
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        CompanySectionsAdminScreen(
                            company = company,
                            sections = sections,
                            onOpenSection = { navController.navigate(AppDestinations.adminSectionEditor(it)) },
                            onCreateSection = { type, title ->
                                showcaseViewModel.createSection(companyId, type, title) { created ->
                                    navController.navigate(AppDestinations.adminSectionEditor(created.id))
                                }
                            },
                            onRenameSection = showcaseViewModel::renameSection,
                            onToggleSectionVisibility = showcaseViewModel::toggleSectionVisibility,
                            onMoveSection = showcaseViewModel::moveSection,
                            onDeleteSection = showcaseViewModel::deleteSection,
                            onBack = navController::navigateUp,
                            onHome = { navigateClientHome() },
                            onClose = { navController.navigate(AppDestinations.ADMIN_SECTIONS) { launchSingleTop = true } },
                        )
                    }
                }

                composable(
                    route = AppDestinations.ADMIN_SECTION_EDITOR,
                    arguments = listOf(navArgument("sectionId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val sectionId = decodeNavArg(backStackEntry.arguments?.getString("sectionId"))
                    val section = showcaseViewModel.section(sectionId).collectAsStateWithLifecycle(initialValue = null).value
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val resolvedSection = section
                        if (resolvedSection == null) {
                            EmptyAdminSectionState(
                                onBack = navController::navigateUp,
                                onHome = { navigateClientHome() },
                            )
                        } else {
                            when (resolvedSection.type) {
                                SectionType.IMAGE_GALLERY -> {
                                    val items = showcaseViewModel.adminFeaturedProjects(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    val sectionLinks = showcaseViewModel.sectionLinks(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    val allItemsLink = sectionLinks.firstOrNull {
                                        it.label.equals("All Items", ignoreCase = true) ||
                                            it.label.equals("All Projects", ignoreCase = true)
                                    }?.url.orEmpty()
                                    FeaturedProjectsAdminScreen(
                                        projects = items,
                                        allItemsLink = allItemsLink,
                                        onAllItemsLinkSave = { url ->
                                            showcaseViewModel.saveSectionLink(
                                                sectionId = resolvedSection.id,
                                                label = "All Items",
                                                url = url,
                                            )
                                        },
                                        onSaveProject = { showcaseViewModel.saveFeaturedProject(it.copy(sectionId = resolvedSection.id)) },
                                        onDeleteProject = showcaseViewModel::deleteFeaturedProject,
                                        onToggleVisibility = showcaseViewModel::toggleFeaturedProjectVisibility,
                                        onMoveProject = showcaseViewModel::moveFeaturedProject,
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.TOUR_360 -> {
                                    val items = showcaseViewModel.adminVirtualTours(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    VirtualToursAdminScreen(
                                        tours = items,
                                        onSaveTour = { showcaseViewModel.saveVirtualTour(it.copy(sectionId = resolvedSection.id)) },
                                        onDeleteTour = showcaseViewModel::deleteVirtualTour,
                                        onToggleVisibility = showcaseViewModel::toggleVirtualTourVisibility,
                                        onMoveTour = showcaseViewModel::moveVirtualTour,
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.YOUTUBE_VIDEOS -> {
                                    val items = showcaseViewModel.adminVideos(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    DesignVideosAdminScreen(
                                        videos = items,
                                        onSaveVideo = { showcaseViewModel.saveVideo(it.copy(sectionId = resolvedSection.id)) },
                                        onDeleteVideo = showcaseViewModel::deleteVideo,
                                        onToggleVisibility = showcaseViewModel::toggleVideoVisibility,
                                        onMoveVideo = showcaseViewModel::moveVideo,
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.PDF_VIEWER -> {
                                    val brochures = showcaseViewModel.adminBrochures(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    BrochuresAdminScreen(
                                        brand = io.gelio.app.data.model.Brand.GENERIC,
                                        brochures = brochures,
                                        onSaveBrochure = { showcaseViewModel.saveBrochure(it.copy(sectionId = resolvedSection.id, brand = io.gelio.app.data.model.Brand.GENERIC)) },
                                        onDeleteBrochure = { id, brochureBrand -> showcaseViewModel.deleteBrochure(id, brochureBrand) },
                                        onToggleVisibility = showcaseViewModel::toggleBrochureVisibility,
                                        onMoveBrochure = { id, brochureBrand, direction -> showcaseViewModel.moveBrochure(id, brochureBrand, direction) },
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.DESTINATIONS -> {
                                    val items = showcaseViewModel.adminDestinations(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    DestinationsAdminScreen(
                                        destinations = items,
                                        onSaveDestination = { showcaseViewModel.saveDestination(it.copy(sectionId = resolvedSection.id)) },
                                        onDeleteDestination = showcaseViewModel::deleteDestination,
                                        onToggleVisibility = showcaseViewModel::toggleDestinationVisibility,
                                        onMoveDestination = showcaseViewModel::moveDestination,
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.SERVICES -> {
                                    val items = showcaseViewModel.adminServices(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    ServicesAdminScreen(
                                        services = items,
                                        onSaveService = { showcaseViewModel.saveService(it.copy(sectionId = resolvedSection.id)) },
                                        onDeleteService = showcaseViewModel::deleteService,
                                        onToggleVisibility = showcaseViewModel::toggleServiceVisibility,
                                        onMoveService = showcaseViewModel::moveService,
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.WORLD_MAP -> {
                                    val worldMapSection = showcaseViewModel.worldMapSection(resolvedSection.id).collectAsStateWithLifecycle(initialValue = null).value
                                    val pins = showcaseViewModel.worldMapPins(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    WorldMapAdminScreen(
                                        section = resolvedSection,
                                        mapSection = worldMapSection,
                                        pins = pins,
                                        onSave = showcaseViewModel::saveWorldMap,
                                        onRenameSection = showcaseViewModel::renameSection,
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.GOOGLE_REVIEWS -> {
                                    val items = showcaseViewModel.adminReviewCards(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    RatingsAdminScreen(
                                        section = resolvedSection,
                                        items = items,
                                        onSaveReview = { showcaseViewModel.saveReviewCard(it.copy(sectionId = resolvedSection.id)) },
                                        onDeleteReview = showcaseViewModel::deleteReviewCard,
                                        onToggleVisibility = showcaseViewModel::toggleReviewCardVisibility,
                                        onMoveReview = showcaseViewModel::moveReviewCard,
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.CONTENT_PAGE -> {
                                    val items = showcaseViewModel.adminContentPageCards(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    ContentPageAdminScreen(
                                        section = resolvedSection,
                                        items = items,
                                        onSaveCard = { showcaseViewModel.saveContentPageCard(it.copy(sectionId = resolvedSection.id)) },
                                        onDeleteCard = showcaseViewModel::deleteContentPageCard,
                                        onToggleVisibility = showcaseViewModel::toggleContentPageCardVisibility,
                                        onMoveCard = showcaseViewModel::moveContentPageCard,
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                SectionType.ART_GALLERY -> {
                                    val heroes = showcaseViewModel.adminArtGalleryHeroes(resolvedSection.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                                    ArtGalleryAdminScreen(
                                        section = resolvedSection,
                                        heroes = heroes,
                                        onSaveHero = { showcaseViewModel.saveArtGalleryHero(it.copy(sectionId = resolvedSection.id)) },
                                        onDeleteHero = showcaseViewModel::deleteArtGalleryHero,
                                        onToggleHeroVisibility = showcaseViewModel::toggleArtGalleryHeroVisibility,
                                        onMoveHero = showcaseViewModel::moveArtGalleryHero,
                                        onOpenHeroItems = { heroId ->
                                            navController.navigate(AppDestinations.adminArtGalleryItemsEditor(resolvedSection.id, heroId))
                                        },
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                        onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                                    )
                                }

                                else -> {
                                    EmptyAdminSectionState(
                                        onBack = navController::navigateUp,
                                        onHome = { navigateClientHome() },
                                    )
                                }
                            }
                        }
                    }
                }

                composable(
                    route = AppDestinations.ADMIN_ART_GALLERY_ITEMS_EDITOR,
                    arguments = listOf(
                        navArgument("sectionId") { type = NavType.StringType },
                        navArgument("heroId") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val sectionId = decodeNavArg(backStackEntry.arguments?.getString("sectionId"))
                    val heroId = decodeNavArg(backStackEntry.arguments?.getString("heroId"))
                    val section = showcaseViewModel.section(sectionId).collectAsStateWithLifecycle(initialValue = null).value
                    val hero = showcaseViewModel.artGalleryHero(heroId).collectAsStateWithLifecycle(initialValue = null).value
                    val cards = showcaseViewModel.adminArtGalleryCards(heroId).collectAsStateWithLifecycle(initialValue = emptyList()).value
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        val resolvedSection = section
                        val resolvedHero = hero
                        if (resolvedSection == null || resolvedHero == null) {
                            EmptyAdminSectionState(
                                onBack = navController::navigateUp,
                                onHome = { navigateClientHome() },
                            )
                        } else {
                            ArtGalleryHeroItemsAdminScreen(
                                section = resolvedSection,
                                hero = resolvedHero,
                                cards = cards,
                                onSaveCard = { showcaseViewModel.saveArtGalleryCard(it.copy(heroId = resolvedHero.id)) },
                                onDeleteCard = showcaseViewModel::deleteArtGalleryCard,
                                onToggleCardVisibility = showcaseViewModel::toggleArtGalleryCardVisibility,
                                onMoveCard = showcaseViewModel::moveArtGalleryCard,
                                onBack = navController::navigateUp,
                                onHome = { navigateClientHome() },
                                onClose = { navController.navigate(AppDestinations.adminCompanySections(resolvedSection.companyId)) { launchSingleTop = true } },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdminPinGate) {
        AdminPinGateDialog(
            expectedPin = settings.adminPin,
            onDismiss = { showAdminPinGate = false },
            onSuccess = {
                showAdminPinGate = false
                navController.navigate(AppDestinations.ADMIN_HOME) {
                    launchSingleTop = true
                }
            },
            onResetApp = {
                showAdminPinGate = false
                clearDataViewModel.resetAppImmediately {
                    backupViewModel.clearSession()
                    navigateClientHome()
                }
            },
        )
    }
}

@Composable
private fun EmptyAdminSectionState(
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "This section is unavailable.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
