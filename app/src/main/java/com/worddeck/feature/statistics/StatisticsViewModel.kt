package com.worddeck.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.ReviewActivity
import com.worddeck.domain.model.StudyProgress
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val status: OperationStatus = OperationStatus.LOADING,
    val progress: StudyProgress = StudyProgress(),
    val activity: ReviewActivity? = null,
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
        val stateFlow = if (deckId == null) {
            combine(
                reviewRepository.observeProgress(userId, timestamp),
                reviewRepository.observeActivity(userId, timestamp),
            ) { progressResult, activityResult ->
                statisticsState(progressResult, activityResult)
            }
        } else {
            reviewRepository.observeProgressByDeck(userId, deckId, timestamp)
                .map { result -> statisticsState(result, null) }
        }

        viewModelScope.launch {
            stateFlow.collect { state ->
                _uiState.value = state
            }
        }
    }
}

private fun statisticsState(
    progressResult: AppResult<StudyProgress>,
    activityResult: AppResult<ReviewActivity>?,
): StatisticsUiState {
    val progress = when (progressResult) {
        is AppResult.Success -> progressResult.value
        is AppResult.Failure -> return StatisticsUiState(
            status = OperationStatus.ERROR,
            error = progressResult.error,
        )
    }

    val activity = when (activityResult) {
        null -> null
        is AppResult.Success -> activityResult.value
        is AppResult.Failure -> return StatisticsUiState(
            status = OperationStatus.ERROR,
            error = activityResult.error,
        )
    }

    return StatisticsUiState(
        status = OperationStatus.SUCCESS,
        progress = progress,
        activity = activity,
    )
}
