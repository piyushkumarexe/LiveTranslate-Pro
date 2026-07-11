package com.piyush.livetranslate.data.translation

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.piyush.livetranslate.core.database.dao.CacheDao
import com.piyush.livetranslate.core.database.dao.FavoriteDao
import com.piyush.livetranslate.core.database.dao.RecentLanguageDao
import com.piyush.livetranslate.core.database.dao.TranslationDao
import com.piyush.livetranslate.core.database.entity.CachedResultEntity
import com.piyush.livetranslate.core.database.entity.FavoriteEntity
import com.piyush.livetranslate.core.database.entity.RecentLanguageEntity
import com.piyush.livetranslate.core.database.entity.TranslationEntity
import com.piyush.livetranslate.core.model.LanguageCatalog
import com.piyush.livetranslate.core.model.SyncState
import com.piyush.livetranslate.core.model.Translation
import com.piyush.livetranslate.core.model.TranslationOrigin
import com.piyush.livetranslate.core.model.TranslationRequest
import com.piyush.livetranslate.core.model.TranslationResult
import com.piyush.livetranslate.domain.repository.AuthRepository
import com.piyush.livetranslate.domain.repository.TranslationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translationDao: TranslationDao,
    private val favoriteDao: FavoriteDao,
    private val cacheDao: CacheDao,
    private val recentLanguageDao: RecentLanguageDao,
    private val provider: AiTranslationProvider,
    private val authRepository: AuthRepository,
    private val syncCoordinator: TranslationSyncCoordinator,
) : TranslationRepository {

    override fun observeHistory(query: String): Flow<List<Translation>> = combine(
        if (query.isBlank()) translationDao.observeAll() else translationDao.search(query.trim()),
        favoriteDao.observeIds(),
    ) { rows, favorites ->
        val favoriteIds = favorites.toSet()
        rows.map { it.toModel(it.id in favoriteIds) }
    }

    override fun observeFavorites(): Flow<List<Translation>> =
        favoriteDao.observeTranslations().combine(favoriteDao.observeIds()) { rows, ids ->
            val set = ids.toSet()
            rows.map { it.toModel(it.id in set) }
        }

    override suspend fun translate(request: TranslationRequest): Result<TranslationResult> = runCatching {
        val text = request.text.trim()
        require(text.isNotBlank()) { "Enter or speak text to translate." }
        require(text.length <= 5_000) { "Text is limited to 5,000 characters." }
        require(request.sourceLanguage != request.targetLanguage) { "Choose two different languages." }

        val normalized = request.copy(text = text)
        val key = cacheKey(normalized)
        val cached = cacheDao.getValid(key)?.let {
            TranslationResult(it.translatedText, it.detectedLanguage, it.confidence, it.provider, cached = true)
        }
        val detected = if (request.sourceLanguage == "auto") detectLanguage(text) else request.sourceLanguage
        val result = cached ?: provider.translate(normalized, detected).also {
            cacheDao.upsert(
                CachedResultEntity(
                    cacheKey = key,
                    sourceText = text,
                    translatedText = it.translatedText,
                    detectedLanguage = it.detectedLanguage ?: detected,
                    confidence = it.confidence,
                    provider = it.provider,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
                ),
            )
        }

        if (request.saveToHistory) {
            recordLanguage(request.sourceLanguage)
            recordLanguage(request.targetLanguage)
            translationDao.upsert(
                TranslationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = authRepository.currentUserId(),
                    sourceText = text,
                    translatedText = result.translatedText,
                    sourceLanguage = request.sourceLanguage,
                    targetLanguage = request.targetLanguage,
                    detectedLanguage = result.detectedLanguage ?: detected,
                    confidence = result.confidence,
                    origin = request.origin.name,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.PENDING.name,
                ),
            )
            enqueueSync()
        }
        result
    }

    override suspend fun delete(id: String) {
        syncCoordinator.deleteRemote(id)
        translationDao.delete(id)
    }

    override suspend fun clearHistory() {
        translationDao.snapshot().forEach { syncCoordinator.deleteRemote(it.id) }
        translationDao.clear()
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        if (favorite) {
            favoriteDao.upsert(FavoriteEntity(id, authRepository.currentUserId()))
        } else {
            favoriteDao.delete(id)
            syncCoordinator.deleteFavoriteRemote(id)
        }
        enqueueSync()
    }

    override suspend fun exportCsv(): String {
        fun String.csv() = "\"${replace("\"", "\"\"")}\""
        val header = "id,source_language,target_language,source_text,translated_text,created_at"
        return buildString {
            appendLine(header)
            translationDao.snapshot().forEach {
                appendLine(listOf(it.id, it.sourceLanguage, it.targetLanguage, it.sourceText, it.translatedText, it.createdAt.toString()).joinToString(",") { value -> value.csv() })
            }
        }
    }

    override suspend fun syncNow(): Result<Unit> = syncCoordinator.sync()
    override suspend fun clearCache() = cacheDao.clear()

    private suspend fun detectLanguage(text: String): String? = runCatching {
        LanguageIdentification.getClient().identifyLanguage(text).await().takeUnless { it == "und" }
    }.getOrNull()

    private fun cacheKey(request: TranslationRequest): String {
        val value = "${request.sourceLanguage}|${request.targetLanguage}|${request.text.lowercase(Locale.ROOT)}"
        return MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private suspend fun recordLanguage(tag: String) {
        if (tag == "auto") return
        val existing = recentLanguageDao.get(tag)
        recentLanguageDao.upsert(
            RecentLanguageEntity(
                languageTag = tag,
                displayName = LanguageCatalog.byTag(tag).displayName,
                lastUsedAt = System.currentTimeMillis(),
                useCount = (existing?.useCount ?: 0) + 1,
            ),
        )
    }

    private fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<TranslationSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("translation-sync", ExistingWorkPolicy.KEEP, request)
    }
}

internal fun TranslationEntity.toModel(favorite: Boolean = false) = Translation(
    id = id,
    userId = userId,
    sourceText = sourceText,
    translatedText = translatedText,
    sourceLanguage = sourceLanguage,
    targetLanguage = targetLanguage,
    detectedLanguage = detectedLanguage,
    confidence = confidence,
    origin = runCatching { TranslationOrigin.valueOf(origin) }.getOrDefault(TranslationOrigin.TEXT),
    isFavorite = favorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = runCatching { SyncState.valueOf(syncState) }.getOrDefault(SyncState.PENDING),
)
