package com.piyush.livetranslate.core.model

import java.util.UUID

data class AuthUser(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
)

data class LanguageOption(
    val tag: String,
    val displayName: String,
    val nativeName: String = displayName,
    val supportsSpeech: Boolean = true,
)

object LanguageCatalog {
    val automatic = LanguageOption("auto", "Detect language", "Auto", false)
    val supported = listOf(
        LanguageOption("en-US", "English", "English"),
        LanguageOption("hi-IN", "Hindi", "हिन्दी"),
        LanguageOption("hinglish", "Hindi (Latin / Hinglish)", "Hinglish"),
        LanguageOption("bn-IN", "Bengali", "বাংলা"),
        LanguageOption("gu-IN", "Gujarati", "ગુજરાતી"),
        LanguageOption("kn-IN", "Kannada", "ಕನ್ನಡ"),
        LanguageOption("ml-IN", "Malayalam", "മലയാളം"),
        LanguageOption("mr-IN", "Marathi", "मराठी"),
        LanguageOption("pa-IN", "Punjabi", "ਪੰਜਾਬੀ"),
        LanguageOption("ta-IN", "Tamil", "தமிழ்"),
        LanguageOption("te-IN", "Telugu", "తెలుగు"),
        LanguageOption("ur-PK", "Urdu", "اردو"),
        LanguageOption("ar-SA", "Arabic", "العربية"),
        LanguageOption("de-DE", "German", "Deutsch"),
        LanguageOption("es-ES", "Spanish", "Español"),
        LanguageOption("fr-FR", "French", "Français"),
        LanguageOption("it-IT", "Italian", "Italiano"),
        LanguageOption("ja-JP", "Japanese", "日本語"),
        LanguageOption("ko-KR", "Korean", "한국어"),
        LanguageOption("pt-BR", "Portuguese", "Português"),
        LanguageOption("ru-RU", "Russian", "Русский"),
        LanguageOption("zh-CN", "Chinese", "中文"),
    )

    fun all(includeAuto: Boolean = true) = if (includeAuto) listOf(automatic) + supported else supported
    fun byTag(tag: String): LanguageOption = all().firstOrNull { it.tag == tag } ?: LanguageOption(tag, tag)
    fun speechLocale(tag: String): String = when (tag) {
        "auto", "hinglish" -> "hi-IN"
        else -> tag
    }
}

enum class TranslationOrigin { TEXT, SPEECH, CONVERSATION, OCR, SCREEN_ACCESSIBILITY, SCREEN_OCR }
enum class SyncState { PENDING, SYNCED, FAILED }

data class Translation(
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val detectedLanguage: String? = null,
    val confidence: Float? = null,
    val origin: TranslationOrigin = TranslationOrigin.TEXT,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING,
)

data class TranslationRequest(
    val text: String,
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "en-US",
    val origin: TranslationOrigin = TranslationOrigin.TEXT,
    val saveToHistory: Boolean = true,
)

data class TranslationResult(
    val translatedText: String,
    val detectedLanguage: String? = null,
    val confidence: Float? = null,
    val provider: String = "groq",
    val cached: Boolean = false,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class VoiceGender { SYSTEM, FEMALE, MALE }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: String = "en",
    val voiceGender: VoiceGender = VoiceGender.SYSTEM,
    val voiceSpeed: Float = 1f,
    val dynamicColor: Boolean = true,
    val onboardingComplete: Boolean = false,
    val cloudSyncEnabled: Boolean = true,
    val overlayConsentGranted: Boolean = false,
    val overlayTargetLanguage: String = "en-US",
    val overlayOcrFallbackEnabled: Boolean = true,
    val overlaySaveHistory: Boolean = false,
)

data class SpeechState(
    val isListening: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val rms: Float = 0f,
    val error: String? = null,
)
