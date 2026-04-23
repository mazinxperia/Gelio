package io.gelio.app.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.gelio.app.data.model.AppColorMode
import io.gelio.app.data.model.AppSettings
import io.gelio.app.data.model.CuratedPalette
import io.gelio.app.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PexelsApiKeySaveState(
    val testing: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false,
)

class SettingsViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    val uiState: StateFlow<AppSettings> =
        appContainer.settingsRepository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    private val _pexelsApiKeySaveState = MutableStateFlow(PexelsApiKeySaveState())
    val pexelsApiKeySaveState: StateFlow<PexelsApiKeySaveState> = _pexelsApiKeySaveState.asStateFlow()

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateThemeMode(themeMode)
        }
    }

    fun updateColorMode(colorMode: AppColorMode) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateColorMode(colorMode)
        }
    }

    fun updateCuratedPalette(curatedPalette: CuratedPalette) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateCuratedPalette(curatedPalette)
        }
    }

    fun updateIdleTimeoutMinutes(minutes: Int) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateIdleTimeoutMinutes(minutes)
        }
    }

    fun updateAdminPin(pin: String) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateAdminPin(pin)
        }
    }

    fun updateKioskModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateKioskModeEnabled(enabled)
        }
    }

    fun updateIdleHeroTitle(title: String) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateIdleHeroTitle(title)
        }
    }

    fun updateIdleHeroCaption(caption: String) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateIdleHeroCaption(caption)
        }
    }

    fun updateNeutralBaseColor(color: String) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateNeutralBaseColor(color)
        }
    }

    fun updateHomescreenLogoPath(path: String) {
        viewModelScope.launch {
            appContainer.settingsRepository.updateHomescreenLogoPath(path)
        }
    }

    fun clearPexelsApiKeyFeedback() {
        _pexelsApiKeySaveState.value = PexelsApiKeySaveState()
    }

    fun testAndSavePexelsApiKey(apiKey: String) {
        viewModelScope.launch {
            val trimmed = apiKey.trim()
            if (trimmed.isBlank()) {
                _pexelsApiKeySaveState.value = PexelsApiKeySaveState(
                    testing = false,
                    message = "Enter a Pexels API key first.",
                    saved = false,
                )
                return@launch
            }
            _pexelsApiKeySaveState.value = PexelsApiKeySaveState(
                testing = true,
                message = "Testing API key...",
                saved = false,
            )
            appContainer.pexelsRepository.validateApiKey(trimmed)
                .onSuccess {
                    appContainer.settingsRepository.updatePexelsApiKey(trimmed)
                    _pexelsApiKeySaveState.value = PexelsApiKeySaveState(
                        testing = false,
                        message = "Pexels API key verified and saved.",
                        saved = true,
                    )
                }
                .onFailure { error ->
                    _pexelsApiKeySaveState.value = PexelsApiKeySaveState(
                        testing = false,
                        message = error.message ?: "Unable to verify the Pexels API key.",
                        saved = false,
                    )
                }
        }
    }

    fun clearPexelsApiKey() {
        viewModelScope.launch {
            appContainer.settingsRepository.updatePexelsApiKey("")
            _pexelsApiKeySaveState.value = PexelsApiKeySaveState(
                testing = false,
                message = "Saved Pexels API key cleared.",
                saved = true,
            )
        }
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SettingsViewModel(appContainer)
                }
            }
    }
}
