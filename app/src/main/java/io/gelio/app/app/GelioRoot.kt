package io.gelio.app.app

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Image
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.gelio.app.core.performance.LocalDevicePerformanceProfile
import io.gelio.app.core.performance.PerfLog
import io.gelio.app.core.performance.rememberDevicePerformanceProfile
import io.gelio.app.core.navigation.AppDestinations
import io.gelio.app.core.navigation.ClientStageTarget
import io.gelio.app.core.navigation.GelioNavHost
import io.gelio.app.core.theme.BrandPaletteContext
import io.gelio.app.core.theme.GelioTheme
import io.gelio.app.core.navigation.paletteContext
import io.gelio.app.R
import kotlinx.coroutines.delay
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass provided")
}

enum class ShowcaseAspectBucket {
    CLASSIC_FOUR_THREE,
    BALANCED,
    WIDE,
    ULTRA_WIDE,
}

enum class ShowcaseDensityBucket {
    STANDARD,
    HIGH,
}

data class ShowcaseAdaptiveProfile(
    val widthDp: Int,
    val heightDp: Int,
    val densityDpi: Int,
    val aspectRatio: Float,
    val aspectBucket: ShowcaseAspectBucket,
    val densityBucket: ShowcaseDensityBucket,
    val outerMargin: Dp,
    val contentPaddingHorizontal: Dp,
    val contentPaddingVertical: Dp,
    val gutter: Dp,
    val sectionSpacing: Dp,
    val heroSpacing: Dp,
    val railWidth: Dp,
    val pageMaxWidth: Dp,
    val centeredContentMaxWidth: Dp,
    val viewerMaxWidth: Dp,
    val heroMaxHeight: Dp,
    val cardHeightSmall: Dp,
    val cardHeightMedium: Dp,
    val cardHeightLarge: Dp,
    val previewFrameHeight: Dp,
    val thumbnailStripHeight: Dp,
    val tabSpacing: Dp,
    val homeButtonInsetTop: Dp,
    val homeButtonInsetStart: Dp,
    val topChromeReserve: Dp,
    val welcomeLogoSize: Dp,
    val brandLogoSize: Dp,
    val adminSidebarWidth: Dp,
    val adminPreviewWidth: Dp,
    val adminPanelGap: Dp,
    val dialogMaxWidth: Dp,
    val minTouchTarget: Dp,
    val compactLandscape: Boolean,
)

val LocalAdaptiveProfile = staticCompositionLocalOf<ShowcaseAdaptiveProfile> {
    error("No AdaptiveProfile provided")
}

data class ShowcaseLayoutTokens(
    val margin: Dp,
    val gutter: Dp,
    val gridSpacing: Dp,
    val cardHeight: Dp,
    val heroMaxHeight: Dp,
    val headerPadding: Dp,
    val pageMaxWidth: Dp,
    val contentMaxWidth: Dp,
    val viewerMaxWidth: Dp,
    val railWidth: Dp,
    val previewFrameHeight: Dp,
    val thumbnailStripHeight: Dp,
    val panelGap: Dp,
    val adminSidebarWidth: Dp,
    val adminPreviewWidth: Dp,
    val dialogMaxWidth: Dp,
    val touchTarget: Dp,
)

val LocalLayoutTokens = staticCompositionLocalOf<ShowcaseLayoutTokens> {
    error("No LayoutTokens provided")
}

data class ShowcaseScreenConfig(
    val widthClass: WindowWidthSizeClass,
    val heightClass: WindowHeightSizeClass,
    val isExpanded: Boolean,
)

val LocalScreenConfig = staticCompositionLocalOf<ShowcaseScreenConfig> {
    error("No ScreenConfig provided")
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
@Suppress("DEPRECATION")
fun GelioRoot(
    appContainer: AppContainer,
) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(appContainer),
    )
    val showcaseViewModel: ShowcaseViewModel = viewModel(
        factory = ShowcaseViewModel.factory(appContainer),
    )
    val backupViewModel: BackupViewModel = viewModel(
        factory = BackupViewModel.factory(appContainer),
    )
    val clearDataViewModel: ClearDataViewModel = viewModel(
        factory = ClearDataViewModel.factory(appContainer),
    )
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val startupWarmupState by showcaseViewModel.startupWarmupState.collectAsStateWithLifecycle()
    val lastInteractionMillis by appContainer.lastInteractionMillis.collectAsStateWithLifecycle()
    var navHostReady by remember { mutableStateOf(false) }
    LaunchedEffect(startupWarmupState.isReady) {
        if (startupWarmupState.isReady && !navHostReady) {
            // Yield one frame so the splash actually paints before we mount the
            // heavy NavHost + 20+ flows (prevents the cold-start layout freeze).
            androidx.compose.runtime.withFrameNanos { }
            navHostReady = true
        }
    }
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route.orEmpty()
    var clientStageTarget by remember { mutableStateOf<ClientStageTarget>(ClientStageTarget.Welcome) }
    var paletteContext by remember { mutableStateOf(BrandPaletteContext.NEUTRAL) }

    LaunchedEffect(currentRoute) {
        if (currentRoute.isNotEmpty()) {
            PerfLog.d("NAV", "route -> $currentRoute")
        }
    }

    LaunchedEffect(currentRoute, clientStageTarget) {
        paletteContext = when {
            currentRoute == AppDestinations.CLIENT_STAGE -> clientStageTarget.paletteContext
            currentRoute.startsWith("design") -> BrandPaletteContext.DESIGN
            currentRoute.startsWith("tourism") -> BrandPaletteContext.TOURISM
            currentRoute == AppDestinations.WELCOME -> BrandPaletteContext.NEUTRAL
            currentRoute == AppDestinations.BRAND_SELECTION -> BrandPaletteContext.NEUTRAL
            currentRoute.startsWith("admin") -> BrandPaletteContext.NEUTRAL
            else -> paletteContext
        }
    }

    LaunchedEffect(currentRoute) {
        appContainer.kioskController.updateCurrentRoute(currentRoute)
    }

    LaunchedEffect(lastInteractionMillis, settings.idleTimeoutMinutes, currentRoute, clientStageTarget) {
        val clientStageAtWelcome = currentRoute == AppDestinations.CLIENT_STAGE && clientStageTarget == ClientStageTarget.Welcome
        if (currentRoute == AppDestinations.WELCOME || clientStageAtWelcome || currentRoute.startsWith("admin")) return@LaunchedEffect
        val snapshot = lastInteractionMillis
        delay(settings.idleTimeoutMinutes * 60_000L)
        if (snapshot == lastInteractionMillis) {
            clientStageTarget = ClientStageTarget.Welcome
            navController.navigate(AppDestinations.CLIENT_STAGE) {
                launchSingleTop = true
                popUpTo(AppDestinations.CLIENT_STAGE) {
                    inclusive = false
                }
            }
        }
    }

    GelioTheme(
        settings = settings,
        paletteContext = paletteContext,
    ) {
        val activity = LocalView.current.context as Activity
        val density = LocalDensity.current
        val containerSize = LocalWindowInfo.current.containerSize
        val widthDp = with(density) { containerSize.width.toDp().value.roundToInt() }
        val heightDp = with(density) { containerSize.height.toDp().value.roundToInt() }
        val densityDpi = activity.resources.configuration.densityDpi
        val windowSizeClass = calculateWindowSizeClass(activity)
        val screenConfig = remember(windowSizeClass) {
            ShowcaseScreenConfig(
                widthClass = windowSizeClass.widthSizeClass,
                heightClass = windowSizeClass.heightSizeClass,
                isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        val adaptiveProfile = remember(
            screenConfig,
            widthDp,
            heightDp,
            densityDpi,
        ) {
            buildAdaptiveProfile(
                widthDp = widthDp,
                heightDp = heightDp,
                densityDpi = densityDpi,
                widthClass = screenConfig.widthClass,
                heightClass = screenConfig.heightClass,
            )
        }

        val layoutTokens = remember(adaptiveProfile) {
            ShowcaseLayoutTokens(
                margin = adaptiveProfile.outerMargin,
                gutter = adaptiveProfile.gutter,
                gridSpacing = adaptiveProfile.sectionSpacing,
                cardHeight = adaptiveProfile.cardHeightMedium,
                heroMaxHeight = adaptiveProfile.heroMaxHeight,
                headerPadding = adaptiveProfile.heroSpacing,
                pageMaxWidth = adaptiveProfile.pageMaxWidth,
                contentMaxWidth = adaptiveProfile.centeredContentMaxWidth,
                viewerMaxWidth = adaptiveProfile.viewerMaxWidth,
                railWidth = adaptiveProfile.railWidth,
                previewFrameHeight = adaptiveProfile.previewFrameHeight,
                thumbnailStripHeight = adaptiveProfile.thumbnailStripHeight,
                panelGap = adaptiveProfile.adminPanelGap,
                adminSidebarWidth = adaptiveProfile.adminSidebarWidth,
                adminPreviewWidth = adaptiveProfile.adminPreviewWidth,
                dialogMaxWidth = adaptiveProfile.dialogMaxWidth,
                touchTarget = adaptiveProfile.minTouchTarget,
            )
        }

        val performanceProfile = rememberDevicePerformanceProfile()
        val kioskRuntimeState = appContainer.kioskController.runtimeState.collectAsStateWithLifecycle().value
        val view = LocalView.current
        val backgroundColor = MaterialTheme.colorScheme.background
        val useDarkIcons = contrastRatio(Color.Black, backgroundColor) >= contrastRatio(Color.White, backgroundColor)
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            view.isHapticFeedbackEnabled = false
            activity.window.decorView.isHapticFeedbackEnabled = false
            val resolvedBackground = backgroundColor.toArgb()
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            activity.window.statusBarColor = resolvedBackground
            activity.window.navigationBarColor = resolvedBackground
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity.window.isNavigationBarContrastEnforced = false
                activity.window.isStatusBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = useDarkIcons
                isAppearanceLightNavigationBars = useDarkIcons
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        CompositionLocalProvider(
            LocalAppContainer provides appContainer,
            LocalDevicePerformanceProfile provides performanceProfile,
            LocalWindowSizeClass provides windowSizeClass,
            LocalScreenConfig provides screenConfig,
            LocalAdaptiveProfile provides adaptiveProfile,
            LocalLayoutTokens provides layoutTokens,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                if (navHostReady) {
                GelioNavHost(
                        navController = navController,
                        settingsViewModel = settingsViewModel,
                        showcaseViewModel = showcaseViewModel,
                        backupViewModel = backupViewModel,
                        clearDataViewModel = clearDataViewModel,
                        clientStageTarget = clientStageTarget,
                        onClientStageTargetChange = { clientStageTarget = it },
                    )
                } else {
                    GelioStartupSplash(startupWarmupState)
                }
            }
        }
    }
}

@Composable
private fun GelioStartupSplash(
    warmupState: StartupWarmupState,
) {
    val progress = remember(warmupState.completedTasks, warmupState.totalTasks) {
        (warmupState.completedTasks.toFloat() / warmupState.totalTasks.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 40.dp)
            .wrapContentSize(Alignment.Center),
    ) {
        Column(
            modifier = Modifier.size(width = 360.dp, height = 260.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        )
        {
            Image(
                painter = painterResource(id = R.drawable.gelio_logo),
                contentDescription = "Gelio",
                modifier = Modifier.size(144.dp),
            )
            Text(
                text = warmupState.stage.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                text = warmupState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .padding(top = 18.dp)
                    .height(6.dp)
                    .size(width = 280.dp, height = 6.dp),
            )
            Text(
                text = "${warmupState.completedTasks} / ${warmupState.totalTasks.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
            warmupState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = maxOf(foreground.luminance(), background.luminance())
    val darker = minOf(foreground.luminance(), background.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun buildAdaptiveProfile(
    widthDp: Int,
    heightDp: Int,
    densityDpi: Int,
    widthClass: WindowWidthSizeClass,
    heightClass: WindowHeightSizeClass,
): ShowcaseAdaptiveProfile {
    val landscapeWidth = max(widthDp, heightDp).toFloat()
    val landscapeHeight = min(widthDp, heightDp).toFloat()
    val aspectRatio = if (landscapeHeight == 0f) 1f else landscapeWidth / landscapeHeight
    val aspectBucket = when {
        aspectRatio <= 1.38f -> ShowcaseAspectBucket.CLASSIC_FOUR_THREE
        aspectRatio <= 1.55f -> ShowcaseAspectBucket.BALANCED
        aspectRatio <= 1.78f -> ShowcaseAspectBucket.WIDE
        else -> ShowcaseAspectBucket.ULTRA_WIDE
    }
    val densityBucket = if (densityDpi >= 280) ShowcaseDensityBucket.HIGH else ShowcaseDensityBucket.STANDARD
    val compactLandscape = landscapeWidth < 900f || landscapeHeight < 600f

    fun clamp(minDp: Float, preferredDp: Float, maxDp: Float): Dp =
        preferredDp.coerceIn(minDp, maxDp).dp

    val outerMargin = clamp(
        minDp = 14f,
        preferredDp = landscapeWidth * if (aspectBucket == ShowcaseAspectBucket.CLASSIC_FOUR_THREE) 0.020f else 0.028f,
        maxDp = 60f,
    )
    val gutter = clamp(
        minDp = 12f,
        preferredDp = outerMargin.value * 0.72f,
        maxDp = 36f,
    )
    val sectionSpacing = clamp(
        minDp = 12f,
        preferredDp = outerMargin.value * 0.64f,
        maxDp = 30f,
    )
    val heroSpacing = clamp(
        minDp = 12f,
        preferredDp = outerMargin.value * 0.42f,
        maxDp = 24f,
    )
    val railWidth = clamp(
        minDp = 180f,
        preferredDp = landscapeWidth * if (aspectBucket == ShowcaseAspectBucket.CLASSIC_FOUR_THREE) 0.20f else 0.18f,
        maxDp = 320f,
    )
    val pageMaxWidth = clamp(
        minDp = 960f,
        preferredDp = landscapeWidth * 0.92f,
        maxDp = 2200f,
    )
    val centeredContentMaxWidth = clamp(
        minDp = 820f,
        preferredDp = landscapeWidth * 0.78f,
        maxDp = 1680f,
    )
    val viewerMaxWidth = clamp(
        minDp = 880f,
        preferredDp = landscapeWidth * 0.86f,
        maxDp = 1840f,
    )
    val heroMaxHeight = clamp(
        minDp = 220f,
        preferredDp = landscapeHeight * if (heightClass == WindowHeightSizeClass.Expanded) 0.30f else 0.26f,
        maxDp = 340f,
    )
    val cardHeightSmall = clamp(132f, landscapeHeight * 0.16f, 180f)
    val cardHeightMedium = clamp(160f, landscapeHeight * 0.205f, 220f)
    val cardHeightLarge = clamp(200f, landscapeHeight * 0.27f, 320f)
    val previewFrameHeight = clamp(250f, landscapeHeight * 0.40f, 460f)
    val thumbnailStripHeight = clamp(94f, landscapeHeight * 0.135f, 154f)
    val tabSpacing = clamp(8f, outerMargin.value * 0.34f, 18f)
    val adminSidebarWidth = clamp(320f, landscapeWidth * 0.28f, 440f)
    val adminPreviewWidth = clamp(280f, landscapeWidth * 0.24f, 420f)
    val adminPanelGap = clamp(14f, gutter.value, 28f)
    val dialogMaxWidth = clamp(520f, landscapeWidth * 0.72f, 1280f)
    val topChromeReserve = clamp(
        minDp = 88f,
        preferredDp = landscapeHeight * if (compactLandscape) 0.12f else 0.16f,
        maxDp = 156f,
    )
    val welcomeLogoSize = clamp(
        minDp = 220f,
        preferredDp = landscapeHeight * if (widthClass == WindowWidthSizeClass.Expanded) 0.46f else 0.40f,
        maxDp = 420f,
    )
    val brandLogoSize = clamp(
        minDp = 210f,
        preferredDp = landscapeHeight * if (aspectBucket == ShowcaseAspectBucket.CLASSIC_FOUR_THREE) 0.34f else 0.38f,
        maxDp = 420f,
    )

    return ShowcaseAdaptiveProfile(
        widthDp = widthDp,
        heightDp = heightDp,
        densityDpi = densityDpi,
        aspectRatio = aspectRatio,
        aspectBucket = aspectBucket,
        densityBucket = densityBucket,
        outerMargin = outerMargin,
        contentPaddingHorizontal = outerMargin,
        contentPaddingVertical = clamp(12f, outerMargin.value * 0.72f, 40f),
        gutter = gutter,
        sectionSpacing = sectionSpacing,
        heroSpacing = heroSpacing,
        railWidth = railWidth,
        pageMaxWidth = pageMaxWidth,
        centeredContentMaxWidth = centeredContentMaxWidth,
        viewerMaxWidth = viewerMaxWidth,
        heroMaxHeight = heroMaxHeight,
        cardHeightSmall = cardHeightSmall,
        cardHeightMedium = cardHeightMedium,
        cardHeightLarge = cardHeightLarge,
        previewFrameHeight = previewFrameHeight,
        thumbnailStripHeight = thumbnailStripHeight,
        tabSpacing = tabSpacing,
        homeButtonInsetTop = clamp(16f, outerMargin.value * 0.72f, 34f),
        homeButtonInsetStart = clamp(4f, outerMargin.value * 0.48f, 32f),
        topChromeReserve = topChromeReserve,
        welcomeLogoSize = welcomeLogoSize,
        brandLogoSize = brandLogoSize,
        adminSidebarWidth = adminSidebarWidth,
        adminPreviewWidth = adminPreviewWidth,
        adminPanelGap = adminPanelGap,
        dialogMaxWidth = dialogMaxWidth,
        minTouchTarget = if (densityBucket == ShowcaseDensityBucket.HIGH) 50.dp else 48.dp,
        compactLandscape = compactLandscape,
    )
}
