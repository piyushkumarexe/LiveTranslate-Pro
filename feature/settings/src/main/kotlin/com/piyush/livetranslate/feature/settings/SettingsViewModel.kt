package com.piyush.livetranslate.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.livetranslate.core.model.ThemeMode
import com.piyush.livetranslate.core.model.UserSettings
import com.piyush.livetranslate.core.model.VoiceGender
import com.piyush.livetranslate.domain.repository.SettingsRepository
import com.piyush.livetranslate.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val translations: TranslationRepository,
) : ViewModel() {
    val settings = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())
    fun theme(value: ThemeMode) = update { copy(themeMode = value) }
    fun dynamicColor(value: Boolean) = update { copy(dynamicColor = value) }
    fun voiceGender(value: VoiceGender) = update { copy(voiceGender = value) }
    fun voiceSpeed(value: Float) = update { copy(voiceSpeed = value) }
    fun appLanguage(value: String) = update { copy(appLanguage = value) }
    fun cloudSync(value: Boolean) = update { copy(cloudSyncEnabled = value) }
    fun clearCache() = viewModelScope.launch { translations.clearCache() }
    private fun update(block: UserSettings.() -> UserSettings) = viewModelScope.launch { repository.update(settings.value.block()) }
}
