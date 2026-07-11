package com.piyush.livetranslate.feature.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.livetranslate.core.model.UserSettings
import com.piyush.livetranslate.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OverlayControlUiState(
    val settings: UserSettings = UserSettings(),
    val runtime: OverlayRuntimeStatus = OverlayRuntimeStatus(),
)

@HiltViewModel
class OverlayControlViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val runtimeController: OverlayRuntimeController,
) : ViewModel() {
    val state: StateFlow<OverlayControlUiState> = combine(settingsRepository.settings, runtimeController.status) { settings, runtime ->
        OverlayControlUiState(settings, runtime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverlayControlUiState())

    fun grantConsent() = update { copy(overlayConsentGranted = true) }
    fun revokeConsent() = update { copy(overlayConsentGranted = false) }.also { runtimeController.command(OverlayCommand.StopService) }
    fun targetLanguage(tag: String) = update { copy(overlayTargetLanguage = tag) }
    fun ocrFallback(enabled: Boolean) = update { copy(overlayOcrFallbackEnabled = enabled) }
    fun saveHistory(enabled: Boolean) = update { copy(overlaySaveHistory = enabled) }
    fun togglePause() = runtimeController.command(OverlayCommand.TogglePause)
    fun scanOcr() = runtimeController.command(OverlayCommand.ScanWithOcr)
    fun stop() = runtimeController.command(OverlayCommand.StopService)

    private fun update(block: UserSettings.() -> UserSettings) = viewModelScope.launch {
        settingsRepository.update(state.value.settings.block())
    }
}
