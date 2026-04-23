@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.features.admin.appsettings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.gelio.app.app.PexelsApiKeySaveState
import io.gelio.app.core.theme.BrandPaletteContext
import io.gelio.app.core.theme.curatedPalettePreviewColor
import io.gelio.app.core.theme.showcaseBrandDarkColorScheme
import io.gelio.app.core.theme.showcaseBrandLightColorScheme
import io.gelio.app.core.ui.AdminPinChangeDialog
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.data.model.AppColorMode
import io.gelio.app.data.model.AppSettings
import io.gelio.app.data.model.CuratedPalette
import io.gelio.app.data.model.ThemeMode

@Composable
fun AppSettingsScreen(
    settings: AppSettings,
    pexelsApiKeySaveState: PexelsApiKeySaveState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorModeChange: (AppColorMode) -> Unit,
    onCuratedPaletteChange: (CuratedPalette) -> Unit,
    onNeutralBaseColorChange: (String) -> Unit,
    onIdleTimeoutChange: (Int) -> Unit,
    onAdminPinChange: (String) -> Unit,
    onIdleHeroTitleChange: (String) -> Unit,
    onIdleHeroCaptionChange: (String) -> Unit,
    onTestAndSavePexelsApiKey: (String) -> Unit,
    onClearPexelsApiKey: () -> Unit,
    onClearPexelsApiKeyFeedback: () -> Unit,
    onKioskModeClick: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pexelsApiKeyInput by rememberSaveable(settings.pexelsApiKey) { mutableStateOf(settings.pexelsApiKey) }
    var idleHeroTitleInput by rememberSaveable(settings.idleHeroTitle) { mutableStateOf(settings.idleHeroTitle) }
    var idleHeroCaptionInput by rememberSaveable(settings.idleHeroCaption) { mutableStateOf(settings.idleHeroCaption) }
    var neutralBaseColorInput by rememberSaveable(settings.neutralBaseColor) { mutableStateOf(settings.neutralBaseColor) }
    val previewDark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val pexelsDirty = pexelsApiKeyInput.trim() != settings.pexelsApiKey.trim()
    val idleHeroDirty =
        idleHeroTitleInput != settings.idleHeroTitle || idleHeroCaptionInput != settings.idleHeroCaption
    val neutralBaseDirty = neutralBaseColorInput.trim() != settings.neutralBaseColor.trim()

    ShowcaseBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            ViewerTopBar(
                title = "App Settings",
                subtitle = "Brand Palette & Colors",
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1640.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    SettingsHeroCard(
                        colorMode = settings.colorMode,
                        curatedPalette = settings.curatedPalette,
                        previewDark = previewDark,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            SettingsSectionCard(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "Color source",
                                subtitle = "Switch between company-driven brand colors and curated static Material 3 color palettes.",
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    ChoicePill(
                                        modifier = Modifier.weight(1f),
                                        label = "Colors",
                                        selected = settings.colorMode == AppColorMode.COLORS,
                                        onClick = { onColorModeChange(AppColorMode.COLORS) },
                                    )
                                    ChoicePill(
                                        modifier = Modifier.weight(1f),
                                        label = "Brand palette",
                                        selected = settings.colorMode == AppColorMode.BRAND,
                                        onClick = { onColorModeChange(AppColorMode.BRAND) },
                                    )
                                }

                                if (settings.colorMode == AppColorMode.COLORS) {
                                    InfoCard(
                                        title = "Colors mode applies globally",
                                        subtitle = "Welcome, admin, company selection, and all client viewers use one curated Material 3 palette in both light and dark mode.",
                                    )
                                } else {
                                    InfoCard(
                                        title = "Brand mode stays company-driven",
                                        subtitle = "Welcome and admin use the global neutral base color, while each company keeps its own generated brand palette from the company seed color.",
                                    )
                                }
                            }

                            SettingsSectionCard(
                                icon = Icons.Rounded.Palette,
                                title = "Neutral base color",
                                subtitle = "Controls the global neutral base used by the welcome screen, admin surfaces, and dark company backgrounds.",
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    OutlinedTextField(
                                        value = neutralBaseColorInput,
                                        onValueChange = { neutralBaseColorInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Neutral Base Hex") },
                                        singleLine = true,
                                    )
                                    PaletteCard(
                                        title = "Neutral base preview",
                                        subtitle = "${settings.neutralBaseColor} applied to the Gelio neutral surface system",
                                        scheme = if (previewDark) {
                                            showcaseBrandDarkColorScheme(BrandPaletteContext.NEUTRAL, neutralBaseColorInput)
                                        } else {
                                            showcaseBrandLightColorScheme(BrandPaletteContext.NEUTRAL, neutralBaseColorInput)
                                        },
                                        selected = settings.colorMode == AppColorMode.BRAND,
                                    )
                                    if (neutralBaseDirty) {
                                        ChoicePill(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = "Save Neutral Base Color",
                                            selected = false,
                                            onClick = { onNeutralBaseColorChange(neutralBaseColorInput) },
                                        )
                                    }
                                }
                            }

                            SettingsSectionCard(
                                icon = Icons.Rounded.DarkMode,
                                title = "Theme mode",
                                subtitle = "Each curated palette already includes both light and dark Material 3 role sets.",
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    ThemeMode.entries.forEach { mode ->
                                        ChoicePill(
                                            modifier = Modifier.weight(1f),
                                            label = mode.label(),
                                            selected = settings.themeMode == mode,
                                            onClick = { onThemeModeChange(mode) },
                                        )
                                    }
                                }
                            }

                            SettingsSectionCard(
                                icon = Icons.Rounded.Schedule,
                                title = "Idle timeout",
                                subtitle = "Controls how fast the kiosk returns to the welcome screen after inactivity.",
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(18.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "${settings.idleTimeoutMinutes} min",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        SelectionBadge(
                                            label = if (settings.idleTimeoutMinutes <= 3) "Fast return" else "Relaxed return",
                                        )
                                    }
                                    Slider(
                                        value = settings.idleTimeoutMinutes.toFloat(),
                                        onValueChange = { onIdleTimeoutChange(it.toInt()) },
                                        valueRange = 1f..15f,
                                        steps = 13,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ),
                                    )
                                    TimeoutStepsRow(current = settings.idleTimeoutMinutes)
                                }
                            }

                            SettingsSectionCard(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "Idle home editorial",
                                subtitle = "Controls the large hero title and caption shown only on the kiosk welcome screen.",
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    OutlinedTextField(
                                        value = idleHeroTitleInput,
                                        onValueChange = { idleHeroTitleInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Hero Text") },
                                        singleLine = false,
                                        minLines = 1,
                                        maxLines = 2,
                                    )
                                    OutlinedTextField(
                                        value = idleHeroCaptionInput,
                                        onValueChange = { idleHeroCaptionInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Hero Caption") },
                                        singleLine = false,
                                        minLines = 2,
                                        maxLines = 4,
                                    )
                                    if (idleHeroDirty) {
                                        ChoicePill(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = "Save Idle Editorial",
                                            selected = false,
                                            onClick = {
                                                onIdleHeroTitleChange(idleHeroTitleInput)
                                                onIdleHeroCaptionChange(idleHeroCaptionInput)
                                            },
                                        )
                                    }
                                    Text(
                                        text = "These are global welcome-screen overlays. The startup loading screen is not affected.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            SettingsSectionCard(
                                icon = Icons.Rounded.SettingsSuggest,
                                title = "Admin access",
                                subtitle = "Admin now opens only from the long-press PIN gate.",
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    InfoCard(
                                        title = "PIN protection",
                                        subtitle = "Long-press the logo, enter 4 digits, and admin opens only after a local offline PIN check.",
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Stored PIN",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        SelectionBadge(label = "\u2022\u2022\u2022\u2022")
                                    }
                                    ChoicePill(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = "Change PIN",
                                        selected = false,
                                        onClick = { showPinDialog = true },
                                    )
                                }
                            }

                            SettingsSectionCard(
                                icon = Icons.Rounded.Shield,
                                title = "Kiosk mode",
                                subtitle = "User-space single-app hardening for private-deploy tablets.",
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    InfoCard(
                                        title = if (settings.kioskModeEnabled) "Kiosk requested" else "Kiosk disabled",
                                        subtitle = "Open the dedicated kiosk screen to grant launcher, overlay, usage, accessibility, battery, and exact-alarm access, then enable or disable the runtime lock.",
                                    )
                                    ChoicePill(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = "Open Kiosk Mode",
                                        selected = false,
                                        onClick = onKioskModeClick,
                                    )
                                }
                            }

                            SettingsSectionCard(
                                icon = Icons.Rounded.Public,
                                title = "Pexels web import",
                                subtitle = "Store the Pexels API key here. Save first runs a live key test, then keeps it locally for the web image picker.",
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    OutlinedTextField(
                                        value = pexelsApiKeyInput,
                                        onValueChange = {
                                            pexelsApiKeyInput = it
                                            onClearPexelsApiKeyFeedback()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Pexels API Key") },
                                        singleLine = true,
                                    )
                                    Text(
                                        text = if (settings.pexelsApiKey.isBlank()) {
                                            "No API key saved yet."
                                        } else {
                                            "A Pexels key is already stored for admin web imports."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (pexelsDirty) {
                                        ChoicePill(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = if (pexelsApiKeySaveState.testing) "Testing..." else "Test & Save Pexels Key",
                                            selected = false,
                                            enabled = !pexelsApiKeySaveState.testing && pexelsApiKeyInput.isNotBlank(),
                                            onClick = { onTestAndSavePexelsApiKey(pexelsApiKeyInput) },
                                        )
                                    }
                                    if (settings.pexelsApiKey.isNotBlank()) {
                                        ChoicePill(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = "Clear Saved Key",
                                            selected = false,
                                            enabled = !pexelsApiKeySaveState.testing,
                                            onClick = {
                                                pexelsApiKeyInput = ""
                                                onClearPexelsApiKey()
                                            },
                                        )
                                    }
                                    pexelsApiKeySaveState.message?.let { message ->
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (pexelsApiKeySaveState.saved) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            SettingsSectionCard(
                                icon = Icons.Rounded.Palette,
                                title = "Curated colors",
                                subtitle = "Static Material 3 palettes with full light and dark role sets. Choosing one switches the whole app into Colors mode.",
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    CuratedPalette.entries.forEach { palette ->
                                        PaletteCard(
                                            title = palette.label,
                                            subtitle = paletteSubtitle(palette, previewDark),
                                            scheme = curatedPalettePreviewColor(palette, previewDark),
                                            selected = settings.colorMode == AppColorMode.COLORS && settings.curatedPalette == palette,
                                            onClick = {
                                                onCuratedPaletteChange(palette)
                                                onColorModeChange(AppColorMode.COLORS)
                                            },
                                        )
                                    }
                                }
                            }

                            SettingsSectionCard(
                                icon = Icons.Rounded.SettingsSuggest,
                                title = "Brand mode behavior",
                                subtitle = "Brand mode now follows Gelio neutral surfaces plus one generated palette per company.",
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    PaletteCard(
                                        title = "Neutral home / admin",
                                        subtitle = "${settings.neutralBaseColor} base for welcome, admin, and shared dark surfaces",
                                        scheme = if (previewDark) {
                                            showcaseBrandDarkColorScheme(BrandPaletteContext.NEUTRAL, settings.neutralBaseColor)
                                        } else {
                                            showcaseBrandLightColorScheme(BrandPaletteContext.NEUTRAL, settings.neutralBaseColor)
                                        },
                                        selected = settings.colorMode == AppColorMode.BRAND,
                                    )
                                    InfoCard(
                                        title = "Company palettes are generated from one seed color",
                                        subtitle = "Each company keeps one editable main brand color in the company manager. Gelio derives the rest of the palette automatically so contrast and surface roles stay usable.",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        AdminPinChangeDialog(
            onDismiss = { showPinDialog = false },
            onSavePin = {
                onAdminPinChange(it)
                showPinDialog = false
            },
        )
    }
}

@Composable
private fun SettingsHeroCard(
    colorMode: AppColorMode,
    curatedPalette: CuratedPalette,
    previewDark: Boolean,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "APP COLOR SYSTEM",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = if (colorMode == AppColorMode.COLORS) {
                        "Curated static Material 3 colors now drive the entire app."
                    } else {
                        "Brand mode keeps the neutral base global and company palettes automatic."
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (colorMode == AppColorMode.COLORS) {
                        "Colors mode uses a curated global Material 3 palette with proper light and dark role mapping across every screen, instead of company-specific route palettes."
                    } else {
                        "Brand mode keeps welcome and admin on the saved neutral base color, then lets each company derive its own contrast-safe palette from one saved seed color."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HeroMetric("Mode", if (colorMode == AppColorMode.COLORS) "Colors" else "Brand")
                    HeroMetric(
                        "Palette",
                        if (colorMode == AppColorMode.COLORS) curatedPalette.label else "Automatic by company",
                    )
                    HeroMetric("Preview", if (previewDark) "Dark roles" else "Light roles")
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ChoicePill(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.985f
            selected -> 1.01f
            else -> 1f
        },
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "settings_choice_scale",
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainerLow
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun PaletteCard(
    title: String,
    subtitle: String,
    scheme: ColorScheme,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (selected) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaletteSwatches(scheme = scheme)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.W600,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (selected) {
                SelectionBadge(label = "Active")
            }
        }
    }
}

@Composable
private fun PaletteSwatches(
    scheme: ColorScheme,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = scheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SwatchBlock(scheme.primary)
                SwatchBlock(scheme.primaryContainer)
                SwatchBlock(scheme.secondaryContainer)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SwatchBlock(scheme.tertiaryContainer)
                SwatchBlock(scheme.surfaceContainerHigh)
                SwatchBlock(scheme.background)
            }
        }
    }
}

@Composable
private fun SwatchBlock(
    color: Color,
) {
    Surface(
        modifier = Modifier
            .width(28.dp)
            .height(18.dp),
        shape = MaterialTheme.shapes.small,
        color = color,
    ) {}
}

@Composable
private fun SelectionBadge(
    label: String,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun TimeoutStepsRow(
    current: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(1, 3, 5, 10, 15).forEach { value ->
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (current == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    subtitle: String,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ThemeMode.label(): String =
    when (this) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System"
    }

private fun paletteSubtitle(
    palette: CuratedPalette,
    dark: Boolean,
): String =
    when (palette) {
        CuratedPalette.BASELINE -> if (dark) "Official-style lilac baseline, dark roles" else "Official-style lilac baseline, light roles"
        CuratedPalette.ROSE -> if (dark) "Soft rose containers with deep plum contrast" else "Warm rose and blush containers"
        CuratedPalette.TERRACOTTA -> if (dark) "Earthy clay accents over tinted dark surfaces" else "Terracotta warmth with soft cream surfaces"
        CuratedPalette.OCEAN -> if (dark) "Cool blue accents over charcoal-blue surfaces" else "Ocean blue with quiet light containers"
        CuratedPalette.EMERALD -> if (dark) "Deep green accents with calm dark surfaces" else "Emerald green with soft misty neutrals"
    }
