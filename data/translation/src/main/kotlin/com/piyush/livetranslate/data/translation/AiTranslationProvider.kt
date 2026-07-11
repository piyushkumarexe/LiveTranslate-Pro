package com.piyush.livetranslate.data.translation

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.piyush.livetranslate.core.model.TranslationRequest
import com.piyush.livetranslate.core.model.TranslationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Provider boundary: replace this implementation to use another AI backend. */
interface AiTranslationProvider {
    suspend fun translate(request: TranslationRequest, detectedLanguage: String?): TranslationResult
}

@Singleton
class FirebaseGroqTranslationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AiTranslationProvider {
    override suspend fun translate(request: TranslationRequest, detectedLanguage: String?): TranslationResult {
        check(FirebaseApp.getApps(context).isNotEmpty()) {
            "Firebase is not configured. Add app/google-services.json and rebuild."
        }
        val payload = mapOf(
            "text" to request.text,
            "sourceLanguage" to request.sourceLanguage,
            "targetLanguage" to request.targetLanguage,
            "detectedLanguage" to detectedLanguage,
            "preserveTone" to true,
        )
        val raw = FirebaseFunctions.getInstance()
            .getHttpsCallable("translate")
            .call(payload)
            .await()
            .getData() as? Map<*, *> ?: error("Translation service returned an invalid response.")
        return TranslationResult(
            translatedText = raw["translatedText"]?.toString()?.trim().orEmpty()
                .ifBlank { error("Translation service returned empty text.") },
            detectedLanguage = raw["detectedLanguage"]?.toString() ?: detectedLanguage,
            confidence = (raw["confidence"] as? Number)?.toFloat(),
            provider = raw["provider"]?.toString() ?: "groq",
        )
    }
}
