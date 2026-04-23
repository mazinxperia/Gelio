package io.gelio.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.gelio.app.data.model.AppColorMode
import io.gelio.app.data.model.AppSettings
import io.gelio.app.data.model.CuratedPalette
import io.gelio.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        val storedColorMode = preferences[COLOR_MODE]
        AppSettings(
            themeMode = ThemeMode.valueOf(
                preferences[THEME_MODE] ?: ThemeMode.DARK.name,
            ),
            colorMode = when {
                storedColorMode != null -> AppColorMode.valueOf(storedColorMode)
                preferences[USE_DYNAMIC_COLOR] == true -> AppColorMode.COLORS
                else -> AppColorMode.COLORS
            },
            curatedPalette = CuratedPalette.valueOf(
                preferences[CURATED_PALETTE]
                    ?: legacyPalette(preferences[ACCENT_TONE]),
            ),
            neutralBaseColor = preferences[NEUTRAL_BASE_COLOR] ?: DEFAULT_NEUTRAL_BASE_COLOR,
            idleTimeoutMinutes = preferences[IDLE_TIMEOUT_MINUTES] ?: 3,
            adminPin = preferences[ADMIN_PIN] ?: DEFAULT_ADMIN_PIN,
            kioskModeEnabled = preferences[KIOSK_MODE_ENABLED] ?: false,
            idleHeroTitle = preferences[IDLE_HERO_TITLE] ?: DEFAULT_IDLE_HERO_TITLE,
            idleHeroCaption = preferences[IDLE_HERO_CAPTION] ?: DEFAULT_IDLE_HERO_CAPTION,
            pexelsApiKey = preferences[PEXELS_API_KEY].orEmpty(),
            homescreenLogoPath = preferences[HOMESCREEN_LOGO_PATH].orEmpty(),
        )
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = themeMode.name }
    }

    suspend fun updateColorMode(colorMode: AppColorMode) {
        dataStore.edit { it[COLOR_MODE] = colorMode.name }
    }

    suspend fun updateCuratedPalette(curatedPalette: CuratedPalette) {
        dataStore.edit { it[CURATED_PALETTE] = curatedPalette.name }
    }

    suspend fun updateNeutralBaseColor(color: String) {
        dataStore.edit { it[NEUTRAL_BASE_COLOR] = color.normalizeHexColor(DEFAULT_NEUTRAL_BASE_COLOR) }
    }

    suspend fun updateIdleTimeoutMinutes(minutes: Int) {
        dataStore.edit { it[IDLE_TIMEOUT_MINUTES] = minutes.coerceIn(1, 15) }
    }

    suspend fun updateAdminPin(pin: String) {
        require(pin.length == 4 && pin.all(Char::isDigit)) { "Admin PIN must be 4 digits." }
        dataStore.edit { it[ADMIN_PIN] = pin }
    }

    suspend fun updateKioskModeEnabled(enabled: Boolean) {
        dataStore.edit { it[KIOSK_MODE_ENABLED] = enabled }
    }

    suspend fun updateIdleHeroTitle(title: String) {
        dataStore.edit {
            it[IDLE_HERO_TITLE] = title.trim().ifBlank { DEFAULT_IDLE_HERO_TITLE }
        }
    }

    suspend fun updateIdleHeroCaption(caption: String) {
        dataStore.edit {
            it[IDLE_HERO_CAPTION] = caption.trim().ifBlank { DEFAULT_IDLE_HERO_CAPTION }
        }
    }

    suspend fun updatePexelsApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            val trimmed = apiKey.trim()
            if (trimmed.isBlank()) {
                preferences.remove(PEXELS_API_KEY)
            } else {
                preferences[PEXELS_API_KEY] = trimmed
            }
        }
    }

    suspend fun updateHomescreenLogoPath(path: String) {
        dataStore.edit { it[HOMESCREEN_LOGO_PATH] = path }
    }

    suspend fun replaceSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = settings.themeMode.name
            preferences[COLOR_MODE] = settings.colorMode.name
            preferences[CURATED_PALETTE] = settings.curatedPalette.name
            preferences[NEUTRAL_BASE_COLOR] = settings.neutralBaseColor.normalizeHexColor(DEFAULT_NEUTRAL_BASE_COLOR)
            preferences[IDLE_TIMEOUT_MINUTES] = settings.idleTimeoutMinutes.coerceIn(1, 15)
            preferences[ADMIN_PIN] = settings.adminPin.takeIf { it.length == 4 && it.all(Char::isDigit) } ?: DEFAULT_ADMIN_PIN
            preferences[KIOSK_MODE_ENABLED] = settings.kioskModeEnabled
            preferences[IDLE_HERO_TITLE] = settings.idleHeroTitle.trim().ifBlank { DEFAULT_IDLE_HERO_TITLE }
            preferences[IDLE_HERO_CAPTION] = settings.idleHeroCaption.trim().ifBlank { DEFAULT_IDLE_HERO_CAPTION }
            val trimmedKey = settings.pexelsApiKey.trim()
            if (trimmedKey.isBlank()) {
                preferences.remove(PEXELS_API_KEY)
            } else {
                preferences[PEXELS_API_KEY] = trimmedKey
            }
            preferences[HOMESCREEN_LOGO_PATH] = settings.homescreenLogoPath
        }
    }

    suspend fun clearAllSettings() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_MODE = stringPreferencesKey("color_mode")
        val CURATED_PALETTE = stringPreferencesKey("curated_palette")
        val NEUTRAL_BASE_COLOR = stringPreferencesKey("neutral_base_color")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val ACCENT_TONE = stringPreferencesKey("accent_tone")
        val IDLE_TIMEOUT_MINUTES = intPreferencesKey("idle_timeout_minutes")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
        val KIOSK_MODE_ENABLED = booleanPreferencesKey("kiosk_mode_enabled")
        val IDLE_HERO_TITLE = stringPreferencesKey("idle_hero_title")
        val IDLE_HERO_CAPTION = stringPreferencesKey("idle_hero_caption")
        val PEXELS_API_KEY = stringPreferencesKey("pexels_api_key")
        val HOMESCREEN_LOGO_PATH = stringPreferencesKey("homescreen_logo_path")
        const val DEFAULT_ADMIN_PIN = "0000"
        const val DEFAULT_NEUTRAL_BASE_COLOR = "#444444"
        const val DEFAULT_IDLE_HERO_TITLE = "Hero Text"
        const val DEFAULT_IDLE_HERO_CAPTION = "enter the caption here"

        fun legacyPalette(accentTone: String?): String =
            when (accentTone) {
                "AUBURN" -> CuratedPalette.TERRACOTTA.name
                "EMERALD" -> CuratedPalette.EMERALD.name
                else -> CuratedPalette.BASELINE.name
            }
    }
}

private fun String.normalizeHexColor(fallback: String): String {
    val trimmed = trim().removePrefix("#")
    val normalized = when {
        trimmed.length == 6 && trimmed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' } -> trimmed.uppercase()
        trimmed.length == 8 && trimmed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' } -> trimmed.uppercase().drop(2)
        else -> fallback.removePrefix("#")
    }
    return "#$normalized"
}
