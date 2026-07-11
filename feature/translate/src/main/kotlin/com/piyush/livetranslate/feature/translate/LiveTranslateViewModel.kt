package com.piyush.livetranslate.feature.translate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.livetranslate.core.model.LanguageCatalog
import com.piyush.livetranslate.core.model.TranslationOrigin
import com.piyush.livetranslate.core.model.TranslationRequest
import com.piyush.livetranslate.core.model.UserSettings
import com.piyush.livetranslate.domain.repository.SettingsRepository
import com.piyush.livetranslate.domain.repository.SpeechRecognizerEngine
import com.piyush.livetranslate.domain.repository.SpeechSynthesizer
import com.piyush.livetranslate.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveTranslateUiState(
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "en-US",
    val recognizedText: String = "",
    val translatedText: String = "",
    val detectedLanguage: String? = null,
    val confidence: Float? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class LiveTranslateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TranslationRepository,
    private val speech: SpeechRecognizerEngine,
    private val synthesizer: SpeechSynthesizer,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        LiveTranslateUiState(
            sourceLanguage = savedStateHandle["source"] ?: "auto",
            targetLanguage = savedStateHandle["target"] ?: "en-US",
        ),
    )
    val state: StateFlow<LiveTranslateUiState> = mutableState.asStateFlow()
    val speechState = speech.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.piyush.livetranslate.core.model.SpeechState())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    init {
        viewModelScope.launch {
            speech.state.map { it.partialText }.filter(String::isNotBlank).debounce(420).distinctUntilChanged().collectLatest { text ->
                mutableState.value = mutableState.value.copy(recognizedText = text)
                translate(text, save = false, origin = TranslationOrigin.SPEECH)
            }
        }
        viewModelScope.launch {
            speech.state.map { it.finalText }.filter(String::isNotBlank).distinctUntilChanged().collectLatest { text ->
                mutableState.value = mutableState.value.copy(recognizedText = text)
                translate(text, save = true, origin = TranslationOrigin.SPEECH)
            }
        }
    }

    fun toggleListening() {
        if (speechState.value.isListening) speech.stop()
        else speech.start(LanguageCatalog.speechLocale(state.value.sourceLanguage), continuous = true)
    }

    fun setInput(text: String) { mutableState.value = mutableState.value.copy(recognizedText = text) }

    fun translateTyped(text: String = state.value.recognizedText) {
        mutableState.value = mutableState.value.copy(recognizedText = text)
        viewModelScope.launch { translate(text, save = true, origin = TranslationOrigin.TEXT) }
    }

    fun setSource(tag: String) { mutableState.value = mutableState.value.copy(sourceLanguage = tag) }
    fun setTarget(tag: String) { mutableState.value = mutableState.value.copy(targetLanguage = tag) }
    fun swap() {
        val current = mutableState.value
        if (current.sourceLanguage == "auto") return
        mutableState.value = current.copy(sourceLanguage = current.targetLanguage, targetLanguage = current.sourceLanguage, recognizedText = current.translatedText, translatedText = current.recognizedText)
    }

    fun replay() = viewModelScope.launch {
        val current = state.value
        synthesizer.speak(current.translatedText, current.targetLanguage, settings.value.voiceSpeed, settings.value.voiceGender)
    }

    fun favoriteLatest() = viewModelScope.launch {
        val latest = repository.observeHistory().first().firstOrNull()
        latest?.let { repository.setFavorite(it.id, !it.isFavorite) }
    }

    fun clearError() { mutableState.value = mutableState.value.copy(error = null) }

    private suspend fun translate(text: String, save: Boolean, origin: TranslationOrigin) {
        if (text.isBlank()) return
        val current = state.value
        mutableState.value = current.copy(loading = true, error = null)
        repository.translate(TranslationRequest(text, current.sourceLanguage, current.targetLanguage, origin, save)).fold(
            onSuccess = { result -> mutableState.value = mutableState.value.copy(loading = false, translatedText = result.translatedText, detectedLanguage = result.detectedLanguage, confidence = result.confidence) },
            onFailure = { error -> mutableState.value = mutableState.value.copy(loading = false, error = error.message ?: "Translation failed.") },
        )
    }

    override fun onCleared() { speech.cancel(); synthesizer.stop(); super.onCleared() }
}
