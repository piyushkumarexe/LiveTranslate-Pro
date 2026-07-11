package com.piyush.livetranslate.data.translation

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.piyush.livetranslate.core.database.dao.FavoriteDao
import com.piyush.livetranslate.core.database.dao.MetadataDao
import com.piyush.livetranslate.core.database.dao.TranslationDao
import com.piyush.livetranslate.core.database.entity.FavoriteEntity
import com.piyush.livetranslate.core.database.entity.OfflineMetadataEntity
import com.piyush.livetranslate.core.database.entity.TranslationEntity
import com.piyush.livetranslate.core.model.SyncState
import com.piyush.livetranslate.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationSyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translations: TranslationDao,
    private val favorites: FavoriteDao,
    private val metadata: MetadataDao,
    private val auth: AuthRepository,
) {
    private fun db(): FirebaseFirestore? = if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()

    suspend fun sync(): Result<Unit> = runCatching {
        val uid = auth.currentUserId() ?: return@runCatching
        val firestore = db() ?: return@runCatching

        translations.pending().forEach { row ->
            firestore.collection("translations").document(row.id)
                .set(row.toCloud(uid), SetOptions.merge()).await()
            translations.setSyncState(row.id, SyncState.SYNCED.name)
        }
        favorites.pending().forEach { favorite ->
            val documentId = "${uid}_${favorite.translationId}"
            firestore.collection("favorites").document(documentId).set(
                mapOf(
                    "userId" to uid,
                    "translationId" to favorite.translationId,
                    "createdAt" to favorite.createdAt,
                ),
                SetOptions.merge(),
            ).await()
            favorites.setSyncState(favorite.translationId, SyncState.SYNCED.name)
        }

        val remoteTranslations = firestore.collection("translations")
            .whereEqualTo("userId", uid).limit(500).get().await()
            .documents.mapNotNull { document -> document.data?.toEntity(document.id) }
        if (remoteTranslations.isNotEmpty()) translations.upsertAll(remoteTranslations)

        val remoteFavorites = firestore.collection("favorites")
            .whereEqualTo("userId", uid).limit(500).get().await()
        remoteFavorites.documents.forEach { document ->
            val id = document.getString("translationId") ?: return@forEach
            if (translations.get(id) != null) {
                favorites.upsert(FavoriteEntity(id, uid, document.getLong("createdAt") ?: System.currentTimeMillis(), SyncState.SYNCED.name))
            }
        }
        metadata.upsert(OfflineMetadataEntity("last_successful_sync", System.currentTimeMillis().toString()))
    }

    suspend fun deleteRemote(id: String) {
        val firestore = db() ?: return
        // The returned Task intentionally is not awaited. Firestore persists the mutation
        // locally and transmits it when connectivity returns.
        runCatching { firestore.collection("translations").document(id).delete() }
        deleteFavoriteRemote(id)
    }

    suspend fun deleteFavoriteRemote(id: String) {
        val uid = auth.currentUserId() ?: return
        val firestore = db() ?: return
        runCatching { firestore.collection("favorites").document("${uid}_$id").delete() }
    }
}

private fun TranslationEntity.toCloud(uid: String): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to uid,
    "sourceText" to sourceText,
    "translatedText" to translatedText,
    "sourceLanguage" to sourceLanguage,
    "targetLanguage" to targetLanguage,
    "detectedLanguage" to detectedLanguage,
    "confidence" to confidence,
    "origin" to origin,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

private fun Map<String, Any>.toEntity(id: String): TranslationEntity? = runCatching {
    TranslationEntity(
        id = id,
        userId = this["userId"]?.toString(),
        sourceText = this["sourceText"]?.toString().orEmpty(),
        translatedText = this["translatedText"]?.toString().orEmpty(),
        sourceLanguage = this["sourceLanguage"]?.toString() ?: "auto",
        targetLanguage = this["targetLanguage"]?.toString() ?: "en-US",
        detectedLanguage = this["detectedLanguage"]?.toString(),
        confidence = (this["confidence"] as? Number)?.toFloat(),
        origin = this["origin"]?.toString() ?: "TEXT",
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        syncState = SyncState.SYNCED.name,
    )
}.getOrNull()
