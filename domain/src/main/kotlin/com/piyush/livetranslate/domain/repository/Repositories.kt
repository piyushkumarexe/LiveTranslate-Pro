package com.piyush.livetranslate.domain.repository

import com.piyush.livetranslate.core.model.AuthUser
import com.piyush.livetranslate.core.model.Translation
import com.piyush.livetranslate.core.model.TranslationRequest
import com.piyush.livetranslate.core.model.TranslationResult
import com.piyush.livetranslate.core.model.UserSettings
import com.piyush.livetranslate.core.model.SpeechState
import com.piyush.livetranslate.core.model.VoiceGender
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
    fun currentUserId(): String?
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser>
    suspend fun signOut()
}

interface TranslationRepository {
    fun observeHistory(query: String = ""): Flow<List<Translation>>
    fun observeFavorites(): Flow<List<Translation>>
    suspend fun translate(request: TranslationRequest): Result<TranslationResult>
    suspend fun delete(id: String)
    suspend fun clearHistory()
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun exportCsv(): String
    suspend fun syncNow(): Result<Unit>
    suspend fun clearCache()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(settings: UserSettings)
}

interface SpeechRecognizerEngine {
    val state: Flow<SpeechState>
    fun start(localeTag: String, continuous: Boolean = true)
    fun stop()
    fun cancel()
}

interface SpeechSynthesizer {
    suspend fun speak(text: String, localeTag: String, speed: Float, gender: VoiceGender)
    fun stop()
}
