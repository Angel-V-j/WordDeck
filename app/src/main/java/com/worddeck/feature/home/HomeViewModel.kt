package com.worddeck.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.domain.model.Deck
import com.worddeck.domain.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Empty : HomeUiState

    data class Content(val decks: List<Deck>) : HomeUiState

    data class Error(val cause: AppError) : HomeUiState
}

class HomeViewModel(
    private val deckRepository: DeckRepository,
    ownerId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deckRepository.observeByOwner(ownerId).collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Success -> if (result.value.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Content(result.value)
                    }
                    is AppResult.Failure -> HomeUiState.Error(result.error)
                }
            }
        }
    }
}
