package com.piyush.livetranslate.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.livetranslate.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: TranslationRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val history = query.flatMapLatest(repository::observeHistory).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites = repository.observeFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = mutableMessage.asStateFlow()

    fun search(value: String) { query.value = value }
    fun delete(id: String) = viewModelScope.launch { repository.delete(id) }
    fun clear() = viewModelScope.launch { repository.clearHistory() }
    fun toggleFavorite(id: String, favorite: Boolean) = viewModelScope.launch { repository.setFavorite(id, favorite) }
    suspend fun exportCsv(): String = repository.exportCsv()
    fun sync() = viewModelScope.launch {
        mutableMessage.value = "Syncing…"
        repository.syncNow().fold(
            onSuccess = { mutableMessage.value = "Cloud sync complete" },
            onFailure = { mutableMessage.value = it.message ?: "Sync failed" },
        )
    }
    fun consumeMessage() { mutableMessage.value = null }
}
