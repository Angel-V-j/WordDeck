package com.worddeck.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.ReviewRating
import com.worddeck.domain.model.StudyCard
import com.worddeck.domain.model.StudySession
import com.worddeck.domain.model.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StudyStage {
    QUESTION,
    ANSWER_REVEALED,
    COMPLETED,
}

enum class StudyMode {
    FLASHCARD,
    TYPED_ANSWER,
}

enum class TypedAnswerResult {
    CORRECT,
    INCORRECT,
}

data class StudyUiState(
    val currentCard: StudyCard? = null,
    val position: Int = 0,
    val totalCards: Int = 0,
    val stage: StudyStage = StudyStage.COMPLETED,
    val mode: StudyMode = StudyMode.FLASHCARD,
    val typedAnswer: String = "",
    val typedAnswerResult: TypedAnswerResult? = null,
    val typedAnswerError: Boolean = false,
    val reviewStatus: OperationStatus = OperationStatus.IDLE,
    val error: AppError? = null,
    val ratings: Map<CardId, ReviewRating> = emptyMap(),
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
)

class StudyViewModel(
    session: StudySession,
    private val currentUserId: UserId,
    private val reviewFlashcard: ReviewFlashcardUseCase,
    mode: StudyMode = StudyMode.FLASHCARD,
) : ViewModel() {
    private val cards = session.cards
    private var currentIndex = 0

    private val _uiState = MutableStateFlow(
        StudyUiState(
            currentCard = cards.firstOrNull(),
            position = if (cards.isEmpty()) 0 else 1,
            totalCards = cards.size,
            stage = if (cards.isEmpty()) StudyStage.COMPLETED else StudyStage.QUESTION,
            mode = mode,
        ),
    )
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    fun revealAnswer() {
        if (_uiState.value.mode != StudyMode.FLASHCARD) return
        if (_uiState.value.stage != StudyStage.QUESTION) return
        _uiState.value = _uiState.value.copy(stage = StudyStage.ANSWER_REVEALED)
    }

    fun updateTypedAnswer(answer: String) {
        val currentState = _uiState.value
        if (currentState.mode != StudyMode.TYPED_ANSWER) return
        if (currentState.stage != StudyStage.QUESTION) return

        _uiState.value = currentState.copy(
            typedAnswer = answer,
            typedAnswerError = false,
        )
    }

    fun submitTypedAnswer() {
        val currentState = _uiState.value
        val currentCard = currentState.currentCard ?: return
        if (currentState.mode != StudyMode.TYPED_ANSWER) return
        if (currentState.stage != StudyStage.QUESTION) return

        val normalizedAnswer = currentState.typedAnswer.trim()
        if (normalizedAnswer.isEmpty()) {
            _uiState.value = currentState.copy(typedAnswerError = true)
            return
        }

        val expectedAnswer = currentCard.flashcard.back.value.trim()
        // Typed answers use one explainable rule: trim the edges and ignore letter case.
        val result = if (normalizedAnswer.equals(expectedAnswer, ignoreCase = true)) {
            TypedAnswerResult.CORRECT
        } else {
            TypedAnswerResult.INCORRECT
        }

        _uiState.value = currentState.copy(
            stage = StudyStage.ANSWER_REVEALED,
            typedAnswerResult = result,
            typedAnswerError = false,
        )
    }

    fun rate(rating: ReviewRating) {
        val currentState = _uiState.value
        val currentCard = currentState.currentCard ?: return
        if (currentState.stage != StudyStage.ANSWER_REVEALED) return
        if (currentState.reviewStatus == OperationStatus.LOADING) return

        _uiState.value = currentState.copy(
            reviewStatus = OperationStatus.LOADING,
            error = null,
        )
        viewModelScope.launch {
            val result = reviewFlashcard(
                currentUserId = currentUserId,
                currentState = currentCard.reviewState,
                rating = rating,
            )
            when (result) {
                is AppResult.Success -> moveToNextCard(currentState, currentCard.flashcard.id, rating)
                is AppResult.Failure -> {
                    _uiState.value = currentState.copy(
                        reviewStatus = OperationStatus.ERROR,
                        error = result.error,
                    )
                }
            }
        }
    }

    private fun moveToNextCard(
        currentState: StudyUiState,
        reviewedCardId: CardId,
        rating: ReviewRating,
    ) {
        val updatedRatings = currentState.ratings + (reviewedCardId to rating)
        val wasCorrect = currentState.typedAnswerResult == TypedAnswerResult.CORRECT
        val wasIncorrect = currentState.typedAnswerResult == TypedAnswerResult.INCORRECT
        val correctAnswers = currentState.correctAnswers + if (wasCorrect) 1 else 0
        val incorrectAnswers = currentState.incorrectAnswers + if (wasIncorrect) 1 else 0
        currentIndex += 1

        if (currentIndex >= cards.size) {
            _uiState.value = currentState.copy(
                currentCard = null,
                position = cards.size,
                stage = StudyStage.COMPLETED,
                typedAnswer = "",
                typedAnswerResult = null,
                typedAnswerError = false,
                reviewStatus = OperationStatus.SUCCESS,
                error = null,
                ratings = updatedRatings,
                correctAnswers = correctAnswers,
                incorrectAnswers = incorrectAnswers,
            )
        } else {
            _uiState.value = currentState.copy(
                currentCard = cards[currentIndex],
                position = currentIndex + 1,
                stage = StudyStage.QUESTION,
                typedAnswer = "",
                typedAnswerResult = null,
                typedAnswerError = false,
                reviewStatus = OperationStatus.IDLE,
                error = null,
                ratings = updatedRatings,
                correctAnswers = correctAnswers,
                incorrectAnswers = incorrectAnswers,
            )
        }
    }
}
