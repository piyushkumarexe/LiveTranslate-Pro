package com.piyush.livetranslate.data.translation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.piyush.livetranslate.core.model.VoiceGender
import com.piyush.livetranslate.domain.repository.SpeechSynthesizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSpeechSynthesizer @Inject constructor(
    @ApplicationContext context: Context,
) : SpeechSynthesizer {
    private val ready = CompletableDeferred<Boolean>()
    private val tts = TextToSpeech(context) { status -> ready.complete(status == TextToSpeech.SUCCESS) }

    override suspend fun speak(text: String, localeTag: String, speed: Float, gender: VoiceGender) {
        if (text.isBlank() || !ready.await()) return
        val locale = Locale.forLanguageTag(localeTag.replace("hinglish", "hi-IN"))
        tts.language = locale
        tts.setSpeechRate(speed.coerceIn(.5f, 2f))
        chooseVoice(locale, gender)?.let { tts.voice = it }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    override fun stop() { tts.stop() }

    private fun chooseVoice(locale: Locale, gender: VoiceGender): Voice? {
        val voices = tts.voices.orEmpty().filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
        if (gender == VoiceGender.SYSTEM) return voices.firstOrNull()
        val token = if (gender == VoiceGender.FEMALE) "female" else "male"
        return voices.firstOrNull { it.name.contains(token, ignoreCase = true) } ?: voices.firstOrNull()
    }
}
