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

data class StudyUiState(
    val currentCard: StudyCard? = null,
    val position: Int = 0,
    val totalCards: Int = 0,
    val stage: StudyStage = StudyStage.COMPLETED,
    val ratings: Map<CardId, ReviewRating> = emptyMap(),
)

class StudyViewModel(session: StudySession) : ViewModel() {
    private val cards = session.cards
    private var currentIndex = 0

    private val _uiState = MutableStateFlow(
        StudyUiState(
            currentCard = cards.firstOrNull(),
            position = if (cards.isEmpty()) 0 else 1,
            totalCards = cards.size,
            stage = if (cards.isEmpty()) StudyStage.COMPLETED else StudyStage.QUESTION,
        ),
    )
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    fun revealAnswer() {
        if (_uiState.value.stage != StudyStage.QUESTION) return
        _uiState.value = _uiState.value.copy(stage = StudyStage.ANSWER_REVEALED)
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
                ratings = updatedRatings,
            )
        } else {
            _uiState.value = currentState.copy(
                currentCard = cards[currentIndex],
                position = currentIndex + 1,
                stage = StudyStage.QUESTION,
                ratings = updatedRatings,
            )
        }
    }
}
