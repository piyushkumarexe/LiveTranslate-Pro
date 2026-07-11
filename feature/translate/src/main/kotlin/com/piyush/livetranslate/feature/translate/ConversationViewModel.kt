package com.piyush.livetranslate.feature.translate

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationMessage(val original: String, val translated: String, val personA: Boolean)
data class ConversationUiState(
    val languageA: String = "en-US",
    val languageB: String = "hi-IN",
    val personATurn: Boolean = true,
    val running: Boolean = false,
    val translating: Boolean = false,
    val messages: List<ConversationMessage> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val repository: TranslationRepository,
    private val speech: SpeechRecognizerEngine,
    private val tts: SpeechSynthesizer,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = mutableState.asStateFlow()
    val speechState = speech.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.piyush.livetranslate.core.model.SpeechState())
    private val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())
    private var handled = ""

    init {
        viewModelScope.launch {
            speech.state.map { it.finalText }.filter(String::isNotBlank).collect { text ->
                if (text != handled && state.value.running) { handled = text; handleTurn(text) }
            }
        }
    }

    fun toggle() {
        if (state.value.running) {
            mutableState.value = state.value.copy(running = false, translating = false)
            speech.cancel(); tts.stop()
        } else {
            mutableState.value = state.value.copy(running = true, error = null)
            listen()
        }
    }

    fun setLanguageA(tag: String) { mutableState.value = state.value.copy(languageA = tag) }
    fun setLanguageB(tag: String) { mutableState.value = state.value.copy(languageB = tag) }
    fun swap() { val s = state.value; mutableState.value = s.copy(languageA = s.languageB, languageB = s.languageA, personATurn = !s.personATurn) }

    private fun listen() {
        val locale = if (state.value.personATurn) state.value.languageA else state.value.languageB
        handled = ""
        speech.start(LanguageCatalog.speechLocale(locale), continuous = false)
    }

    private suspend fun handleTurn(text: String) {
        speech.stop()
        val current = state.value
        val source = if (current.personATurn) current.languageA else current.languageB
        val target = if (current.personATurn) current.languageB else current.languageA
        mutableState.value = current.copy(translating = true)
        repository.translate(TranslationRequest(text, source, target, TranslationOrigin.CONVERSATION, true)).fold(
            onSuccess = { result ->
                mutableState.value = state.value.copy(
                    translating = false,
                    messages = state.value.messages + ConversationMessage(text, result.translatedText, current.personATurn),
                )
                tts.speak(result.translatedText, target, settings.value.voiceSpeed, settings.value.voiceGender)
                delay((result.translatedText.length * 45L).coerceIn(900L, 5_000L))
                if (state.value.running) {
                    mutableState.value = state.value.copy(personATurn = !current.personATurn)
                    speech.cancel()
                    delay(250)
                    listen()
                }
            },
            onFailure = { mutableState.value = state.value.copy(translating = false, running = false, error = it.message ?: "Translation failed.") },
        )
    }

    override fun onCleared() { speech.cancel(); tts.stop(); super.onCleared() }
}
