@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.clientstage

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.gelio.app.R
import io.gelio.app.app.LocalAppContainer
import io.gelio.app.app.ShowcaseViewModel
import io.gelio.app.core.navigation.ClientStageTarget
import io.gelio.app.core.theme.clientStageDefaultEffectsSpec
import io.gelio.app.core.theme.clientStageDefaultSpatialSpec
import io.gelio.app.core.theme.clientStageFastEffectsSpec
import io.gelio.app.core.theme.clientStageFastSpatialSpec
import io.gelio.app.core.theme.clientStageSlowEffectsSpec
import io.gelio.app.core.theme.clientStageSlowSpatialSpec
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.ui.ShowcaseHomeButton
import io.gelio.app.core.ui.ShowcasePrimaryActionButton
import io.gelio.app.data.model.AppSettings
import io.gelio.app.data.model.ShowcaseCompany
import io.gelio.app.data.model.ShowcaseSection
import io.gelio.app.features.company.CompanyShellScreen
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val WelcomeEditorialSerif = FontFamily(
    Font(R.font.noto_serif_variable, weight = FontWeight.W300),
    Font(R.font.noto_serif_variable, weight = FontWeight.W400),
    Font(R.font.noto_serif_variable, weight = FontWeight.W500),
    Font(R.font.noto_serif_variable, weight = FontWeight.W600),
    Font(R.font.noto_serif_variable, weight = FontWeight.W700),
)

private val WelcomeArtisticFont = FontFamily(
    Font(R.font.brittany_signature, weight = FontWeight.W300),
    Font(R.font.brittany_signature, weight = FontWeight.W400),
    Font(R.font.brittany_signature, weight = FontWeight.W500),
    Font(R.font.brittany_signature, weight = FontWeight.W600),
    Font(R.font.brittany_signature, weight = FontWeight.W700),
)

@Composable
fun ClientShowcaseStage(
    target: ClientStageTarget,
    settings: AppSettings,
    showcaseViewModel: ShowcaseViewModel,
    onTargetChange: (ClientStageTarget) -> Unit,
    onAdminOpen: () -> Unit,
    onProjectClick: (String) -> Unit,
    onTourClick: (String) -> Unit,
    onAllProjectsClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBrochureClick: (String) -> Unit,
) {
    val appContainer = LocalAppContainer.current
    val kioskRuntime = appContainer.kioskController.runtimeState.collectAsStateWithLifecycle().value
    val isWelcome = target == ClientStageTarget.Welcome
    val isBrandSelection = target == ClientStageTarget.BrandSelection
    val activeCompanyTarget = target as? ClientStageTarget.Company
    val isCompany = activeCompanyTarget != null
    val visibleCompanies = showcaseViewModel.companies.collectAsStateWithLifecycle().value
    val orderedCompanies = remember(visibleCompanies) { visibleCompanies.sortedBy { it.sortOrder } }
    val activeHeaderCompany = remember(activeCompanyTarget, orderedCompanies) {
        activeCompanyTarget?.let { targetCompany ->
            orderedCompanies.firstOrNull { it.id == targetCompany.companyId }
        }
    }
    var embeddedDetailActive by remember(target) { mutableStateOf(false) }

    LaunchedEffect(target, orderedCompanies) {
        if (target == ClientStageTarget.BrandSelection && orderedCompanies.size == 1) {
            onTargetChange(ClientStageTarget.Company(orderedCompanies.single().id))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val compactLandscape = maxWidth < 840.dp || maxHeight < 560.dp
        val welcomeLogoSize = minOf(320.dp, maxHeight * 0.48f, maxWidth * 0.32f).coerceAtLeast(160.dp)
        val stageHeaderLogoSize = if (compactLandscape) 76.dp else 170.dp
        val activeBrandLogoSize = if (compactLandscape) 72.dp else 108.dp
        val headerTopPadding = if (compactLandscape) 14.dp else 30.dp
        val homePaddingTop = if (compactLandscape) 16.dp else 34.dp
        val homePaddingStart = if (compactLandscape) 16.dp else 32.dp
        val brandLogoSize = minOf(420.dp, maxHeight * 0.64f, maxWidth * 0.32f).coerceAtLeast(176.dp)
        val brandSpacerWidth = if (compactLandscape) 48.dp else minOf(180.dp, maxWidth * 0.10f)
        val logoTargetY = if (isWelcome) (maxHeight - welcomeLogoSize) / 2f else headerTopPadding
        val logoTargetScale = if (isWelcome) 1f else stageHeaderLogoSize / welcomeLogoSize
        val stageLogoVisible = isWelcome || isBrandSelection
        val welcomeSidePadding = if (compactLandscape) 28.dp else 72.dp
        val welcomeCenterGap = if (compactLandscape) 40.dp else 88.dp
        val welcomeSideZoneWidth = (
            (maxWidth - (welcomeSidePadding * 2) - welcomeLogoSize - (welcomeCenterGap * 2)) / 2f
            ).coerceAtLeast(if (compactLandscape) 180.dp else 260.dp)

        AnimatedVisibility(
            visible = isCompany,
            enter = slideInVertically(
                animationSpec = clientStageDefaultSpatialSpec(),
                initialOffsetY = { it / 3 },
            ) + fadeIn(animationSpec = clientStageDefaultEffectsSpec()),
            exit = slideOutVertically(
                animationSpec = clientStageFastSpatialSpec(),
                targetOffsetY = { it / 4 },
            ) + fadeOut(animationSpec = clientStageFastEffectsSpec()),
        ) {
            val companyId = activeCompanyTarget?.companyId.orEmpty()
            val allSections = showcaseViewModel.adminSections(companyId).collectAsStateWithLifecycle().value
            val fallbackCompany by produceState<ShowcaseCompany?>(initialValue = null, companyId, visibleCompanies) {
                value = null
                if (companyId.isBlank() || visibleCompanies.any { it.id == companyId }) return@produceState
                value = showcaseViewModel.adminCompanySnapshot(companyId)
            }
            val company = remember(visibleCompanies, fallbackCompany, companyId) {
                visibleCompanies.firstOrNull { it.id == companyId } ?: fallbackCompany
            }
            val fallbackSections by produceState<List<ShowcaseSection>>(initialValue = emptyList(), companyId, allSections) {
                value = emptyList()
                if (companyId.isBlank() || allSections.isNotEmpty()) return@produceState
                value = showcaseViewModel.adminSectionsSnapshot(companyId)
            }
            val resolvedSections = remember(allSections, fallbackSections) {
                if (allSections.isNotEmpty()) allSections else fallbackSections
            }
            val sections = remember(resolvedSections) { resolvedSections.filterNot { it.hidden } }

            LaunchedEffect(companyId, sections) {
                if (companyId.isNotBlank() && sections.isNotEmpty()) {
                    showcaseViewModel.prewarmSections(sections)
                }
            }

            LaunchedEffect(companyId, activeCompanyTarget?.sectionId, visibleCompanies, allSections, fallbackSections) {
                if (companyId.isNotBlank() && allSections.isEmpty() && fallbackSections.isNotEmpty()) {
                    Log.w(
                        "ClientShowcaseStage",
                        "Live sections flow was empty for $companyId; using snapshot fallback with ${fallbackSections.size} sections.",
                    )
                }
            }

            CompanyShellScreen(
                company = company,
                sections = sections,
                selectedSectionId = activeCompanyTarget?.sectionId,
                showcaseViewModel = showcaseViewModel,
                onSectionSelected = { sectionId ->
                    val resolvedCompanyId = activeCompanyTarget?.companyId ?: return@CompanyShellScreen
                    onTargetChange(ClientStageTarget.Company(resolvedCompanyId, sectionId))
                },
                onWelcomeClick = { onTargetChange(ClientStageTarget.Welcome) },
                onProjectClick = onProjectClick,
                onTourClick = onTourClick,
                onVideoClick = onVideoClick,
                onBrochureClick = onBrochureClick,
                onAllProjectsClick = onAllProjectsClick,
                embeddedInClientStage = true,
            )
        }

        BrandSelectionLogos(
            visible = isBrandSelection,
            companies = orderedCompanies,
            logoSize = brandLogoSize,
            spacerWidth = brandSpacerWidth,
            onCompanyClick = { companyId -> onTargetChange(ClientStageTarget.Company(companyId)) },
            onOpenAdmin = onAdminOpen,
        )

        StageWelcomeLogo(
            visible = stageLogoVisible,
            companyLogoUri = when {
                orderedCompanies.size == 1 -> orderedCompanies.single().logoPath
                settings.homescreenLogoPath.isNotBlank() -> settings.homescreenLogoPath
                else -> null
            },
            logoSize = welcomeLogoSize,
            targetScale = logoTargetScale,
            targetY = logoTargetY,
            targetAlpha = if (stageLogoVisible) 1f else 0f,
            onClick = {
                if (orderedCompanies.size == 1) {
                    onTargetChange(ClientStageTarget.Company(orderedCompanies.single().id))
                } else {
                    onTargetChange(ClientStageTarget.BrandSelection)
                }
            },
            onLongClick = onAdminOpen,
        )

        ActiveBrandHeader(
            company = activeHeaderCompany,
            visible = isCompany && !embeddedDetailActive,
            logoSize = activeBrandLogoSize,
            topPadding = if (compactLandscape) 8.dp else 12.dp,
        )

        StageHomeButton(
            visible = !isWelcome && !embeddedDetailActive,
            topPadding = homePaddingTop,
            startPadding = homePaddingStart,
            onClick = { onTargetChange(ClientStageTarget.Welcome) },
        )

        StageWelcomeHero(
            visible = isWelcome,
            title = settings.idleHeroTitle,
            caption = settings.idleHeroCaption,
            compactLandscape = compactLandscape,
            zoneWidth = welcomeSideZoneWidth,
            startPadding = welcomeSidePadding,
        )

        StageClock(
            visible = isWelcome,
            compactLandscape = compactLandscape,
            maxHeight = maxHeight,
            zoneWidth = welcomeSideZoneWidth,
            endPadding = welcomeSidePadding,
        )

        StageBatteryStatus(
            visible = isWelcome,
            compactLandscape = compactLandscape,
            zoneWidth = welcomeSideZoneWidth,
            endPadding = welcomeSidePadding,
        )

        StageSleepButton(
            visible = isWelcome && kioskRuntime.active,
            topPadding = logoTargetY + welcomeLogoSize + if (compactLandscape) 16.dp else 24.dp,
            onClick = { appContainer.kioskController.requestManualSleep() },
        )
    }
}

@Composable
private fun BoxScope.StageSleepButton(
    visible: Boolean,
    topPadding: Dp,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = clientStageDefaultEffectsSpec(),
        label = "stage_sleep_alpha",
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 48f,
        animationSpec = clientStageFastSpatialSpec(),
        label = "stage_sleep_y",
    )
    if (!visible && alpha <= 0.001f) return

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = topPadding)
            .size(58.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = "Sleep screen",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.StageClock(
    visible: Boolean,
    compactLandscape: Boolean,
    maxHeight: Dp,
    zoneWidth: Dp,
    endPadding: Dp,
) {
    val now by produceState(initialValue = LocalDateTime.now(), visible) {
        while (visible) {
            value = LocalDateTime.now()
            delay(1_000)
        }
    }
    val locale = remember { Locale.getDefault() }
    val hour = remember(now, locale) { now.format(DateTimeFormatter.ofPattern("hh", locale)) }
    val minute = remember(now, locale) { now.format(DateTimeFormatter.ofPattern("mm", locale)) }
    val amPm = remember(now, locale) { now.format(DateTimeFormatter.ofPattern("a", locale)).lowercase(locale) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = clientStageDefaultEffectsSpec(),
        label = "stage_clock_alpha",
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else -120f,
        animationSpec = clientStageFastSpatialSpec(),
        label = "stage_clock_y",
    )
    val blockWidth = when {
        compactLandscape -> zoneWidth
        else -> zoneWidth
    }
    val blockHeight = when {
        compactLandscape -> 210.dp
        maxHeight >= 900.dp -> 320.dp
        else -> 276.dp
    }
    val hourSize = when {
        compactLandscape -> 118.sp
        zoneWidth >= 320.dp -> 180.sp
        else -> 154.sp
    }
    val minuteSize = when {
        compactLandscape -> 108.sp
        zoneWidth >= 320.dp -> 168.sp
        else -> 142.sp
    }
    val amPmSize = when {
        compactLandscape -> 24.sp
        zoneWidth >= 320.dp -> 40.sp
        else -> 32.sp
    }
    val minuteOffsetX = when {
        compactLandscape -> 64.dp
        zoneWidth >= 320.dp -> 112.dp
        else -> 92.dp
    }
    val minuteOffsetY = when {
        compactLandscape -> 86.dp
        maxHeight >= 900.dp -> 132.dp
        else -> 114.dp
    }
    val colonAlpha by animateFloatAsState(
        targetValue = if (now.second % 2 == 0) 0.88f else 0.18f,
        animationSpec = clientStageFastEffectsSpec(),
        label = "stage_clock_colon_alpha",
    )
    if (!visible && alpha <= 0.001f) return

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(
                end = endPadding,
            )
            .width(blockWidth)
            .wrapContentHeight()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hour,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = WelcomeEditorialSerif,
                    fontWeight = FontWeight.W400,
                    fontSize = hourSize,
                    lineHeight = hourSize * 0.85f,
                    letterSpacing = (-3).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.94f),
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = WelcomeEditorialSerif,
                    fontWeight = FontWeight.W400,
                    fontSize = hourSize,
                    lineHeight = hourSize * 0.85f,
                    letterSpacing = (-2).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = colonAlpha),
                modifier = Modifier.padding(bottom = (hourSize.value * 0.08f).dp),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = minuteOffsetX, top = minuteOffsetY),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = minute,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = WelcomeEditorialSerif,
                    fontWeight = FontWeight.W400,
                    fontSize = minuteSize,
                    lineHeight = minuteSize * 0.84f,
                    letterSpacing = (-3).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.36f),
            )
            Text(
                text = amPm,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = WelcomeEditorialSerif,
                    fontWeight = FontWeight.W500,
                    fontSize = amPmSize,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp, bottom = (minuteSize.value * 0.12f).dp),
            )
        }
    }
}

@Composable
private fun BoxScope.StageWelcomeHero(
    visible: Boolean,
    title: String,
    caption: String,
    compactLandscape: Boolean,
    zoneWidth: Dp,
    startPadding: Dp,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = clientStageDefaultEffectsSpec(),
        label = "stage_hero_alpha",
    )
    val translationX by animateFloatAsState(
        targetValue = if (visible) 0f else -120f,
        animationSpec = clientStageFastSpatialSpec(),
        label = "stage_hero_x",
    )
    val trimmedTitle = title.ifBlank { "Hero Text" }.trim()
    val hasArtisticDot = trimmedTitle.endsWith(".")
    val titleLength = trimmedTitle.length
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleStyleBase = MaterialTheme.typography.displayLarge.copy(
        fontFamily = WelcomeArtisticFont,
        fontWeight = FontWeight.W300,
        letterSpacing = (-1.8).sp,
    )
    val maxTitleFontSize = when {
        compactLandscape && titleLength <= 10 -> 60.sp
        compactLandscape && titleLength <= 16 -> 50.sp
        compactLandscape && titleLength <= 22 -> 42.sp
        compactLandscape && titleLength <= 30 -> 34.sp
        compactLandscape -> 26.sp
        titleLength <= 10 -> 100.sp
        titleLength <= 16 -> 82.sp
        titleLength <= 22 -> 66.sp
        titleLength <= 30 -> 52.sp
        titleLength <= 40 -> 42.sp
        titleLength <= 52 -> 34.sp
        else -> 26.sp
    }
    val minTitleFontSize = if (compactLandscape) 18.sp else 20.sp
    val dotColor = MaterialTheme.colorScheme.primary
    val titleFontSize = remember(trimmedTitle, zoneWidth, compactLandscape, dotColor) {
        val maxWidthPx = with(density) { zoneWidth.roundToPx() }
        var candidate = maxTitleFontSize
        while (candidate.value > minTitleFontSize.value) {
            val candidateAnnotated = buildAnnotatedString {
                append(if (hasArtisticDot) trimmedTitle.dropLast(1) else trimmedTitle)
                if (hasArtisticDot) {
                    withStyle(SpanStyle(
                        fontSize = candidate * 2.4f,
                        color = dotColor,
                        fontWeight = FontWeight.Bold
                    )) {
                        append(".")
                    }
                }
            }
            val result = textMeasurer.measure(
                text = candidateAnnotated,
                style = titleStyleBase.copy(
                    fontSize = candidate,
                    lineHeight = candidate * 0.88f,
                ),
                maxLines = 1,
                softWrap = false,
                constraints = Constraints(maxWidth = maxWidthPx),
            )
            if (!result.hasVisualOverflow && !result.didOverflowWidth) break
            candidate = (candidate.value - 2f).sp
        }
        if (candidate.value < minTitleFontSize.value) minTitleFontSize else candidate
    }
    val captionFontSize = when {
        compactLandscape -> 18.sp
        caption.trim().length <= 42 -> 26.sp
        caption.trim().length <= 76 -> 22.sp
        else -> 18.sp
    }

    if (!visible && alpha <= 0.001f) return

    Column(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(
                start = startPadding,
            )
            .width(zoneWidth)
            .graphicsLayer {
                this.alpha = alpha
                this.translationX = translationX
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = remember(trimmedTitle, titleFontSize, dotColor) {
                buildAnnotatedString {
                    append(if (hasArtisticDot) trimmedTitle.dropLast(1) else trimmedTitle)
                    if (hasArtisticDot) {
                        withStyle(SpanStyle(
                            fontSize = titleFontSize * 2.4f,
                            color = dotColor,
                            fontWeight = FontWeight.Bold
                        )) {
                            append(".")
                        }
                    }
                }
            },
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = WelcomeArtisticFont,
                fontWeight = FontWeight.W300,
                fontSize = titleFontSize,
                lineHeight = titleFontSize * 0.88f,
                letterSpacing = (-1.8).sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = caption.ifBlank { "enter the caption here" },
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = WelcomeArtisticFont,
                fontWeight = FontWeight.W400,
                fontSize = captionFontSize,
                lineHeight = captionFontSize * 1.34f,
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            modifier = Modifier
                .padding(top = if (compactLandscape) 14.dp else 22.dp)
                .widthIn(max = zoneWidth * 0.84f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoxScope.StageBatteryStatus(
    visible: Boolean,
    compactLandscape: Boolean,
    zoneWidth: Dp,
    endPadding: Dp,
) {
    val batteryState by rememberStageBatteryState()
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = clientStageDefaultEffectsSpec(),
        label = "stage_battery_alpha",
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else -60f,
        animationSpec = clientStageFastSpatialSpec(),
        label = "stage_battery_y",
    )
    val textSize = if (compactLandscape) 30.sp else 42.sp
    val iconSize = if (compactLandscape) 26.dp else 34.dp

    if (!visible && alpha <= 0.001f) return

    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = if (compactLandscape) 24.dp else 40.dp)
            .wrapContentWidth()
            .height(if (compactLandscape) 48.dp else 64.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (batteryState.isCharging) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = "Charging",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(iconSize),
                )
            }
            Text(
                text = "${batteryState.percent}%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = WelcomeEditorialSerif,
                    fontWeight = FontWeight.W500,
                    fontSize = textSize,
                    lineHeight = textSize * 0.92f,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
            )
        }
    }
}

private data class StageBatteryState(
    val percent: Int = 0,
    val isCharging: Boolean = false,
)

@Composable
private fun rememberStageBatteryState(): State<StageBatteryState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(StageBatteryState()) }

    DisposableEffect(context) {
        fun updateFromIntent(intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 0
            state.value = StageBatteryState(
                percent = percent.coerceIn(0, 100),
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            )
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        updateFromIntent(context.registerReceiver(null, filter))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                updateFromIntent(intent)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return state
}

@Composable
private fun BoxScope.StageHomeButton(
    visible: Boolean,
    topPadding: Dp,
    startPadding: Dp,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) clientStageDefaultEffectsSpec() else clientStageFastEffectsSpec(),
        label = "stage_home_alpha",
    )
    val translationX by animateFloatAsState(
        targetValue = if (visible) 0f else -180f,
        animationSpec = if (visible) clientStageDefaultSpatialSpec() else clientStageFastSpatialSpec(),
        label = "stage_home_x",
    )

    if (!visible && alpha <= 0.001f) return

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = topPadding, start = startPadding)
            .graphicsLayer {
                this.alpha = alpha
                this.translationX = translationX
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
    ) {
        ShowcaseHomeButton(
            onClick = {
                if (visible) onClick()
            },
        )
    }
}

@Composable
private fun BoxScope.StageWelcomeLogo(
    visible: Boolean,
    companyLogoUri: String?,
    logoSize: Dp,
    targetScale: Float,
    targetY: Dp,
    targetAlpha: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = clientStageFastSpatialSpec(),
        label = "stage_logo_press",
    )
    val logoScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = clientStageSlowSpatialSpec(),
        label = "welcome_logo_scale",
    )
    val logoY by animateFloatAsState(
        targetValue = with(density) { targetY.toPx() },
        animationSpec = clientStageSlowSpatialSpec(),
        label = "welcome_logo_y",
    )
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = clientStageDefaultEffectsSpec(),
        label = "welcome_logo_alpha",
    )

    if (!visible && alpha <= 0.001f) return

    val baseModifier = Modifier
        .align(Alignment.TopCenter)
        .size(logoSize)
        .graphicsLayer {
            translationY = logoY
            transformOrigin = TransformOrigin(0.5f, 0f)
            val resolvedScale = logoScale * pressScale
            scaleX = resolvedScale
            scaleY = resolvedScale
            this.alpha = alpha.coerceIn(0f, 1f)
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
        .pointerInput(visible) {
            if (!visible) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    
                    var maxPointersFirst = currentEvent.changes.count { it.pressed }
                    
                    while (true) {
                        val event = awaitPointerEvent()
                        val count = event.changes.count { it.pressed }
                        if (count > maxPointersFirst) maxPointersFirst = count
                        if (count == 0) break
                    }
                    isPressed = false
                    
                    if (maxPointersFirst < 2) {
                        onClick()
                        return@awaitEachGesture
                    }
                    
                    try {
                        withTimeout(300L) {
                            awaitFirstDown(requireUnconsumed = false)
                        }
                    } catch (e: PointerEventTimeoutCancellationException) {
                        onClick()
                        return@awaitEachGesture
                    }
                    isPressed = true
                    
                    var twoFingersDown = currentEvent.changes.count { it.pressed } >= 2
                    if (!twoFingersDown) {
                        try {
                            withTimeout(200L) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val count = event.changes.count { it.pressed }
                                    if (count >= 2) {
                                        twoFingersDown = true
                                        break
                                    }
                                    if (count == 0) break
                                }
                            }
                        } catch (e: PointerEventTimeoutCancellationException) { }
                    }
                    
                    if (twoFingersDown) {
                        try {
                            withTimeout(3000L) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count { it.pressed } < 2) break
                                }
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            onLongClick()
                        }
                    } else {
                        onClick()
                    }
                    
                    while (true) {
                        if (currentEvent.changes.count { it.pressed } == 0) break
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } == 0) break
                    }
                    isPressed = false
                }
            }

    if (companyLogoUri.isNullOrBlank()) {
        Image(
            painter = painterResource(io.gelio.app.R.drawable.gelio_logo),
            contentDescription = "Gelio logo",
            modifier = baseModifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
    } else {
        io.gelio.app.core.ui.OptimizedAsyncImage(
            model = companyLogoUri,
            contentDescription = "Company logo",
            modifier = baseModifier,
            maxWidth = logoSize,
            maxHeight = logoSize,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
    }
}

@Composable
private fun BoxScope.BrandSelectionLogos(
    visible: Boolean,
    companies: List<ShowcaseCompany>,
    logoSize: Dp,
    spacerWidth: Dp,
    onCompanyClick: (String) -> Unit,
    onOpenAdmin: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) clientStageDefaultEffectsSpec() else clientStageFastEffectsSpec(),
        label = "brand_selection_alpha",
    )

    if (!visible && alpha <= 0.001f) return

    if (companies.isEmpty()) {
        StageSetupState(
            visible = visible,
            onCreateCompany = onOpenAdmin,
            onImportBackup = onOpenAdmin,
        )
        return
    }

    when {
        companies.size == 2 -> {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        this.alpha = alpha
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                companies.forEachIndexed { index, company ->
                    StageBrandLogo(
                        visible = visible,
                        model = company.logoModel(),
                        contentDescription = company.name,
                        size = logoSize,
                        enterFromLeft = index == 0,
                        onClick = { onCompanyClick(company.id) },
                    )
                    if (index < companies.lastIndex) {
                        StageBrandLogoSpacer(width = spacerWidth)
                    }
                }
            }
        }

        companies.size >= 3 -> {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
                horizontalArrangement = Arrangement.spacedBy(spacerWidth * 0.4f, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                userScrollEnabled = true,
            ) {
                itemsIndexed(companies, key = { _, item -> item.id }) { index, company ->
                    StageBrandLogo(
                        visible = visible,
                        model = company.logoModel(),
                        contentDescription = company.name,
                        size = logoSize,
                        enterFromLeft = index == 0,
                        onClick = { onCompanyClick(company.id) },
                    )
                }
            }
        }

        else -> {
            StageBrandLogo(
                visible = visible,
                model = companies.single().logoModel(),
                contentDescription = companies.single().name,
                size = logoSize,
                enterFromLeft = true,
                onClick = { onCompanyClick(companies.single().id) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        this.alpha = alpha
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
            )
        }
    }
}

@Composable
private fun StageBrandLogoSpacer(width: Dp) {
    Box(modifier = Modifier.size(width = width, height = 1.dp))
}

@Composable
private fun StageBrandLogo(
    visible: Boolean,
    model: Any,
    contentDescription: String,
    size: Dp,
    enterFromLeft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = clientStageFastSpatialSpec(),
        label = "$contentDescription stage logo scale",
    )
    val slideOffset by animateFloatAsState(
        targetValue = if (visible) 0f else if (enterFromLeft) -420f else 420f,
        animationSpec = if (visible) clientStageSlowSpatialSpec() else clientStageDefaultSpatialSpec(),
        label = "$contentDescription stage logo x",
    )

    OptimizedAsyncImage(
        model = model,
        contentDescription = contentDescription,
        maxWidth = size,
        maxHeight = size,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationX = slideOffset
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = visible,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
}

@Composable
private fun BoxScope.ActiveBrandHeader(
    company: ShowcaseCompany?,
    visible: Boolean,
    logoSize: Dp,
    topPadding: Dp,
) {
    val model = company?.logoModel() ?: return

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) clientStageDefaultEffectsSpec() else clientStageFastEffectsSpec(),
        label = "active_brand_alpha",
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else -180f,
        animationSpec = if (visible) clientStageDefaultSpatialSpec() else clientStageFastSpatialSpec(),
        label = "active_brand_y",
    )

    if (!visible && alpha <= 0.001f) return

    OptimizedAsyncImage(
        model = model,
        contentDescription = null,
        maxWidth = logoSize,
        maxHeight = logoSize,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = topPadding)
            .size(logoSize)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
    )
}

@Composable
private fun BoxScope.StageSetupState(
    visible: Boolean,
    onCreateCompany: () -> Unit,
    onImportBackup: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) clientStageDefaultEffectsSpec() else clientStageFastEffectsSpec(),
        label = "setup_state_alpha",
    )
    if (!visible && alpha <= 0.001f) return

    Surface(
        modifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer {
                this.alpha = alpha
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Start Your Gelio Library",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "This install is intentionally blank. Create your first company or import a Gelio backup to start the kiosk.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShowcasePrimaryActionButton(
                    label = "Create First Company",
                    onClick = onCreateCompany,
                )
                ShowcasePrimaryActionButton(
                    label = "Import Backup",
                    onClick = onImportBackup,
                )
            }
        }
    }

}

private fun ShowcaseCompany.logoModel(): Any =
    logoPath.takeIf(String::isNotBlank) ?: R.drawable.gelio_logo
