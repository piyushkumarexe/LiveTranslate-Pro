package com.piyush.livetranslate.data.translation

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.piyush.livetranslate.core.database.dao.SettingsDao
import com.piyush.livetranslate.core.database.entity.UserSettingsEntity
import com.piyush.livetranslate.core.model.ThemeMode
import com.piyush.livetranslate.core.model.UserSettings
import com.piyush.livetranslate.core.model.VoiceGender
import com.piyush.livetranslate.domain.repository.AuthRepository
import com.piyush.livetranslate.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SettingsDao,
    private val auth: AuthRepository,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = dao.observe().map { it?.toModel() ?: UserSettings() }

    override suspend fun update(settings: UserSettings) {
        val entity = settings.toEntity()
        dao.upsert(entity)
        val uid = auth.currentUserId() ?: return
        if (FirebaseApp.getApps(context).isEmpty() || !settings.cloudSyncEnabled) return
        runCatching {
            // Firestore queues this write in its persistent offline queue. Do not await a
            // server acknowledgement here: changing a local setting must never block offline.
            FirebaseFirestore.getInstance().collection("settings").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "themeMode" to settings.themeMode.name,
                    "appLanguage" to settings.appLanguage,
                    "voiceGender" to settings.voiceGender.name,
                    "voiceSpeed" to settings.voiceSpeed,
                    "dynamicColor" to settings.dynamicColor,
                    "cloudSyncEnabled" to settings.cloudSyncEnabled,
                    "updatedAt" to entity.updatedAt,
                ),
                SetOptions.merge(),
            )
        }
    }
}

private fun UserSettingsEntity.toModel() = UserSettings(
    themeMode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.SYSTEM),
    appLanguage = appLanguage,
    voiceGender = runCatching { VoiceGender.valueOf(voiceGender) }.getOrDefault(VoiceGender.SYSTEM),
    voiceSpeed = voiceSpeed,
    dynamicColor = dynamicColor,
    onboardingComplete = onboardingComplete,
    cloudSyncEnabled = cloudSyncEnabled,
    overlayConsentGranted = overlayConsentGranted,
    overlayTargetLanguage = overlayTargetLanguage,
    overlayOcrFallbackEnabled = overlayOcrFallbackEnabled,
    overlaySaveHistory = overlaySaveHistory,
)

private fun UserSettings.toEntity() = UserSettingsEntity(
    themeMode = themeMode.name,
    appLanguage = appLanguage,
    voiceGender = voiceGender.name,
    voiceSpeed = voiceSpeed.coerceIn(.5f, 2f),
    dynamicColor = dynamicColor,
    onboardingComplete = onboardingComplete,
    cloudSyncEnabled = cloudSyncEnabled,
    overlayConsentGranted = overlayConsentGranted,
    overlayTargetLanguage = overlayTargetLanguage,
    overlayOcrFallbackEnabled = overlayOcrFallbackEnabled,
    overlaySaveHistory = overlaySaveHistory,
)
