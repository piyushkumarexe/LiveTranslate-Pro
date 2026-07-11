package com.piyush.livetranslate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.livetranslate.core.model.AuthUser
import com.piyush.livetranslate.core.model.UserSettings
import com.piyush.livetranslate.domain.repository.AuthRepository
import com.piyush.livetranslate.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppUiState(val ready: Boolean = false, val settings: UserSettings = UserSettings(), val user: AuthUser? = null)

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    val state = combine(settingsRepository.settings, authRepository.currentUser) { settings, user ->
        AppUiState(ready = true, settings = settings, user = user)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())
}
