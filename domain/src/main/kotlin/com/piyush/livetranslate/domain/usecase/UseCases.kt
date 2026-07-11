package com.piyush.livetranslate.domain.usecase

import com.piyush.livetranslate.core.model.TranslationRequest
import com.piyush.livetranslate.core.model.UserSettings
import com.piyush.livetranslate.domain.repository.AuthRepository
import com.piyush.livetranslate.domain.repository.SettingsRepository
import com.piyush.livetranslate.domain.repository.TranslationRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String) = repository.signInWithGoogleIdToken(idToken)
}

class SignOutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}

class TranslateTextUseCase @Inject constructor(private val repository: TranslationRepository) {
    suspend operator fun invoke(request: TranslationRequest) = repository.translate(request)
}

class SetFavoriteUseCase @Inject constructor(private val repository: TranslationRepository) {
    suspend operator fun invoke(id: String, favorite: Boolean) = repository.setFavorite(id, favorite)
}

class UpdateSettingsUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(settings: UserSettings) = repository.update(settings)
}
