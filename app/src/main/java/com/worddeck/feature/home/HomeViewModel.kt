package com.worddeck.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val status: OperationStatus = OperationStatus.LOADING,
    val decks: List<Deck> = emptyList(),
    val error: AppError? = null,
)

class HomeViewModel(
    private val deckRepository: DeckRepository,
    ownerId: UserId,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deckRepository.observeByOwner(ownerId).collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Success -> HomeUiState(
                        status = OperationStatus.SUCCESS,
                        decks = result.value,
                    )
                    is AppResult.Failure -> HomeUiState(
                        status = OperationStatus.ERROR,
                        error = result.error,
                    )
                }
            }
        }
    }
}
