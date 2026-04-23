package io.gelio.app.data.model

import androidx.compose.runtime.Immutable

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

enum class AppColorMode {
    BRAND,
    COLORS,
}

enum class CuratedPalette(
    val label: String,
) {
    BASELINE("Baseline Lilac"),
    ROSE("Rose Quartz"),
    TERRACOTTA("Terracotta"),
    OCEAN("Ocean"),
    EMERALD("Emerald"),
}

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val colorMode: AppColorMode = AppColorMode.COLORS,
    val curatedPalette: CuratedPalette = CuratedPalette.BASELINE,
    val neutralBaseColor: String = "#444444",
    val idleTimeoutMinutes: Int = 3,
    val adminPin: String = "0000",
    val kioskModeEnabled: Boolean = false,
    val idleHeroTitle: String = "Hero Text",
    val idleHeroCaption: String = "enter the caption here",
    val pexelsApiKey: String = "",
    val homescreenLogoPath: String = "",
)
