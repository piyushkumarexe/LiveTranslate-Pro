package com.piyush.livetranslate.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.livetranslate.core.model.AuthUser
import com.piyush.livetranslate.domain.repository.AuthRepository
import com.piyush.livetranslate.domain.repository.SettingsRepository
import com.piyush.livetranslate.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(val loading: Boolean = false, val user: AuthUser? = null, val error: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val signIn: SignInWithGoogleUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val currentUser = authRepository.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    fun authenticate(idToken: String) = viewModelScope.launch {
        mutableState.value = LoginUiState(loading = true)
        signIn(idToken).fold(
            onSuccess = { mutableState.value = LoginUiState(user = it) },
            onFailure = { mutableState.value = LoginUiState(error = it.message ?: "Sign-in failed.") },
        )
    }

    fun completeOnboarding() = viewModelScope.launch {
        val current = settingsRepository.settings.first()
        settingsRepository.update(current.copy(onboardingComplete = true))
    }

    fun clearError() { mutableState.value = mutableState.value.copy(error = null) }
}
