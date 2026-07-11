package com.piyush.livetranslate.feature.overlay

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

internal class OcrFallbackEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): String = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        .textBlocks
        .flatMap { it.lines }
        .map { it.text.replace(Regex("\\s+"), " ").trim() }
        .filter { it.length >= 2 && it.any(Char::isLetter) }
        .filterNot(::looksSensitive)
        .distinct()
        .take(12)
        .joinToString("\n")
        .take(1_500)

    private fun looksSensitive(text: String): Boolean {
        val lowered = text.lowercase()
        val label = listOf("password", "passcode", "one-time password", "verification code", "security code", "cvv", "otp").any(lowered::contains)
        val shortCode = Regex("(?<!\\d)\\d{4,8}(?!\\d)").containsMatchIn(text)
        val cardLike = Regex("(?:\\d[ -]?){13,19}").containsMatchIn(text)
        return cardLike || (label && shortCode)
    }

    fun close() = recognizer.close()
}
