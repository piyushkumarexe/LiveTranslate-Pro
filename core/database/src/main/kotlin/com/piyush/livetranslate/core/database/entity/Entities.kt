package com.piyush.livetranslate.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "translations",
    indices = [Index("createdAt"), Index("userId"), Index("syncState")],
)
data class TranslationEntity(
    @PrimaryKey val id: String,
    val userId: String?,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val detectedLanguage: String?,
    val confidence: Float?,
    val origin: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: String,
)

@Entity(
    tableName = "favorites",
    foreignKeys = [ForeignKey(
        entity = TranslationEntity::class,
        parentColumns = ["id"],
        childColumns = ["translationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("translationId", unique = true)],
)
data class FavoriteEntity(
    @PrimaryKey val translationId: String,
    val userId: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val syncState: String = "PENDING",
)

@Entity(tableName = "recent_languages")
data class RecentLanguageEntity(
    @PrimaryKey val languageTag: String,
    val displayName: String,
    val lastUsedAt: Long,
    val useCount: Int,
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val profile: String = "default",
    val themeMode: String = "SYSTEM",
    val appLanguage: String = "en",
    val voiceGender: String = "SYSTEM",
    val voiceSpeed: Float = 1f,
    val dynamicColor: Boolean = true,
    val onboardingComplete: Boolean = false,
    val cloudSyncEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "offline_metadata")
data class OfflineMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_results", indices = [Index("expiresAt")])
data class CachedResultEntity(
    @PrimaryKey val cacheKey: String,
    val sourceText: String,
    val translatedText: String,
    val detectedLanguage: String?,
    val confidence: Float?,
    val provider: String,
    val createdAt: Long,
    val expiresAt: Long,
)
