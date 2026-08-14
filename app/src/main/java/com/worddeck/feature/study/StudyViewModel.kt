package com.worddeck.feature.study

import androidx.lifecycle.ViewModel
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.ReviewRating
import com.worddeck.domain.model.StudyCard
import com.worddeck.domain.model.StudySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val ratings: Map<CardId, ReviewRating> = emptyMap(),
)

class StudyViewModel(
    session: StudySession,
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

        val updatedRatings = currentState.ratings + (currentCard.flashcard.id to rating)
        currentIndex += 1

        if (currentIndex >= cards.size) {
            _uiState.value = currentState.copy(
                currentCard = null,
                position = cards.size,
                stage = StudyStage.COMPLETED,
                typedAnswer = "",
                typedAnswerResult = null,
                typedAnswerError = false,
                ratings = updatedRatings,
            )
        } else {
            _uiState.value = currentState.copy(
                currentCard = cards[currentIndex],
                position = currentIndex + 1,
                stage = StudyStage.QUESTION,
                typedAnswer = "",
                typedAnswerResult = null,
                typedAnswerError = false,
                ratings = updatedRatings,
            )
        }
    }
}
