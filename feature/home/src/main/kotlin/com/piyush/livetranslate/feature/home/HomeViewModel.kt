package com.piyush.livetranslate.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.livetranslate.domain.repository.AuthRepository
import com.piyush.livetranslate.domain.repository.TranslationRepository
import com.piyush.livetranslate.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    translationRepository: TranslationRepository,
    private val signOut: SignOutUseCase,
) : ViewModel() {
    val user = authRepository.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val recent = translationRepository.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun logout() = viewModelScope.launch { signOut() }
}
