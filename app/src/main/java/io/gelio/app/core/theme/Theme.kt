package io.gelio.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import io.gelio.app.data.model.AppColorMode
import io.gelio.app.data.model.AppSettings
import io.gelio.app.data.model.CuratedPalette
import io.gelio.app.data.model.ThemeMode

data class ShowcaseSpacing(
    val micro: androidx.compose.ui.unit.Dp = 4.dp,
    val xSmall: androidx.compose.ui.unit.Dp = 8.dp,
    val small: androidx.compose.ui.unit.Dp = 12.dp,
    val medium: androidx.compose.ui.unit.Dp = 16.dp,
    val large: androidx.compose.ui.unit.Dp = 20.dp,
    val xLarge: androidx.compose.ui.unit.Dp = 24.dp,
    val xxLarge: androidx.compose.ui.unit.Dp = 32.dp,
    val hero: androidx.compose.ui.unit.Dp = 48.dp,
)

data class ShowcaseRadius(
    val smooth8: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(8.dp),
    val smooth12: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(12.dp),
    val smooth16: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(16.dp),
    val smooth20: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(20.dp),
    val smooth24: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(24.dp),
    val smooth28: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(28.dp),
    val smooth32: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(32.dp),
    val smoothPill: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(50.dp),
)

val LocalShowcaseSpacing = staticCompositionLocalOf { ShowcaseSpacing() }
val LocalShowcaseRadius = staticCompositionLocalOf { ShowcaseRadius() }

// Map the core Material shapes to the expressive ones roughly
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val ShowcaseMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    largeIncreased = RoundedCornerShape(36.dp),
    extraLarge = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(40.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GelioTheme(
    settings: AppSettings,
    paletteContext: BrandPaletteContext,
    content: @Composable () -> Unit,
) {
    val isDark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        settings.colorMode == AppColorMode.COLORS && isDark -> showcaseCuratedDarkColorScheme(settings.curatedPalette)
        settings.colorMode == AppColorMode.COLORS && !isDark -> showcaseCuratedLightColorScheme(settings.curatedPalette)
        isDark -> showcaseBrandDarkColorScheme(paletteContext, settings.neutralBaseColor)
        else -> showcaseBrandLightColorScheme(paletteContext, settings.neutralBaseColor)
    }

    CompositionLocalProvider(
        LocalShowcaseSpacing provides ShowcaseSpacing(),
        LocalShowcaseRadius provides ShowcaseRadius(),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = ShowcaseTypography,
            shapes = ShowcaseMaterialShapes,
            content = content,
        )
    }
}

@Composable
fun curatedPalettePreviewColor(curatedPalette: CuratedPalette, dark: Boolean) =
    if (dark) showcaseCuratedDarkColorScheme(curatedPalette) else showcaseCuratedLightColorScheme(curatedPalette)
