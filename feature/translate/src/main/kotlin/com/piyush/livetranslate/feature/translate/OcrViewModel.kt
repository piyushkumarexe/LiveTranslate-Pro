package com.piyush.livetranslate.feature.translate

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.piyush.livetranslate.core.model.TranslationOrigin
import com.piyush.livetranslate.core.model.TranslationRequest
import com.piyush.livetranslate.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class OcrUiState(
    val recognizedText: String = "",
    val translatedText: String = "",
    val targetLanguage: String = "en-US",
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OcrViewModel @Inject constructor(private val repository: TranslationRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(OcrUiState())
    val state: StateFlow<OcrUiState> = mutableState.asStateFlow()

    fun process(context: Context, uri: Uri) = viewModelScope.launch {
        mutableState.value = state.value.copy(loading = true, error = null)
        runCatching {
            val image = InputImage.fromFilePath(context, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).await().text
        }.fold(
            onSuccess = { text ->
                mutableState.value = state.value.copy(recognizedText = text, loading = false)
                if (text.isNotBlank()) translate(text) else mutableState.value = state.value.copy(error = "No readable text found.")
            },
            onFailure = { mutableState.value = state.value.copy(loading = false, error = it.message ?: "Could not read this image.") },
        )
    }

    fun setText(text: String) { mutableState.value = state.value.copy(recognizedText = text) }
    fun setTarget(tag: String) { mutableState.value = state.value.copy(targetLanguage = tag) }
    fun translateCurrent() = viewModelScope.launch { translate(state.value.recognizedText) }

    private suspend fun translate(text: String) {
        mutableState.value = state.value.copy(loading = true, error = null)
        repository.translate(TranslationRequest(text, "auto", state.value.targetLanguage, TranslationOrigin.OCR, true)).fold(
            onSuccess = { mutableState.value = state.value.copy(loading = false, translatedText = it.translatedText) },
            onFailure = { mutableState.value = state.value.copy(loading = false, error = it.message ?: "Translation failed.") },
        )
    }
}
