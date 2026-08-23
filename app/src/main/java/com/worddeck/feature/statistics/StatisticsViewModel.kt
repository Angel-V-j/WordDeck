package com.worddeck.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.StudyProgress
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val status: OperationStatus = OperationStatus.LOADING,
    val progress: StudyProgress = StudyProgress(),
    val error: AppError? = null,
)

class StatisticsViewModel(
    reviewRepository: ReviewRepository,
    userId: UserId,
    clock: Clock,
    deckId: DeckId? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        val timestamp = clock.now()
        val progressFlow = if (deckId == null) {
            reviewRepository.observeProgress(userId, timestamp)
        } else {
            reviewRepository.observeProgressByDeck(userId, deckId, timestamp)
        }

        viewModelScope.launch {
            progressFlow.collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Success -> StatisticsUiState(
                        status = OperationStatus.SUCCESS,
                        progress = result.value,
                    )
                    is AppResult.Failure -> StatisticsUiState(
                        status = OperationStatus.ERROR,
                        error = result.error,
                    )
                }
            }
        }
    }
}
