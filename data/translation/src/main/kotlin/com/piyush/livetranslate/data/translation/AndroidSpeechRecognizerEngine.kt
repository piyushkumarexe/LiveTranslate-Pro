package com.piyush.livetranslate.data.translation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.piyush.livetranslate.core.model.SpeechState
import com.piyush.livetranslate.domain.repository.SpeechRecognizerEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSpeechRecognizerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechRecognizerEngine, RecognitionListener {
    private val mutableState = MutableStateFlow(SpeechState())
    override val state: StateFlow<SpeechState> = mutableState
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var localeTag: String = "en-US"
    private var continuous = true
    private var requested = false

    override fun start(localeTag: String, continuous: Boolean) {
        this.localeTag = localeTag
        this.continuous = continuous
        requested = true
        mainHandler.post { startInternal() }
    }

    private fun startInternal() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            mutableState.value = SpeechState(error = "Speech recognition is not available on this device.")
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
        }
        recognizer?.cancel()
        mutableState.value = SpeechState(isListening = true)
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 450L)
        })
    }

    override fun stop() {
        requested = false
        mainHandler.post { recognizer?.stopListening(); mutableState.value = mutableState.value.copy(isListening = false) }
    }

    override fun cancel() {
        requested = false
        mainHandler.post { recognizer?.cancel(); mutableState.value = SpeechState() }
    }

    private fun restartIfNeeded() {
        if (continuous && requested) mainHandler.postDelayed(::startInternal, 350)
    }

    override fun onReadyForSpeech(params: Bundle?) { mutableState.value = mutableState.value.copy(isListening = true, error = null) }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) { mutableState.value = mutableState.value.copy(rms = rmsdB.coerceIn(0f, 12f)) }
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { mutableState.value = mutableState.value.copy(isListening = false) }
    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        mutableState.value = mutableState.value.copy(partialText = text, error = null)
    }
    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        mutableState.value = mutableState.value.copy(isListening = false, partialText = text, finalText = text)
        restartIfNeeded()
    }
    override fun onError(error: Int) {
        val recoverable = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        mutableState.value = mutableState.value.copy(
            isListening = false,
            error = if (recoverable) null else errorMessage(error),
        )
        restartIfNeeded()
    }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service network error."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Try again."
        SpeechRecognizer.ERROR_SERVER -> "Speech service is temporarily unavailable."
        else -> "Speech recognition failed ($error)."
    }
}
