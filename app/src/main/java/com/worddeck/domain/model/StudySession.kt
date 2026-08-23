package com.worddeck.domain.model

import com.worddeck.common.Timestamp

data class StudyCard(
    val flashcard: Flashcard,
    val reviewState: ReviewState,
)

data class StudySession(
    val cards: List<StudyCard>,
)

/**
 * Selects the cards that can be studied now without accessing Room or system time.
 *
 * A missing review state means that the card is new. Its initial state stays in
 * memory until the user completes and saves the first review.
 */
fun startStudySession(
    userId: UserId,
    deck: Deck,
    flashcards: List<Flashcard>,
    reviewStates: List<ReviewState>,
    startedAt: Timestamp,
): StudySession {
    if (deck.ownerId != userId) {
        return StudySession(emptyList())
    }

    val statesByCard = reviewStates
        .filter { it.userId == userId }
        .associateBy { it.cardId }

    val dueCards = mutableListOf<StudyCard>()

    for (flashcard in flashcards) {
        if (flashcard.deckId != deck.id) {
            continue
        }

        val savedState = statesByCard[flashcard.id]
        val reviewState = savedState ?: ReviewState.initial(
            userId = userId,
            cardId = flashcard.id,
            dueAt = startedAt,
        )
        val isNew = savedState == null || savedState.lastReviewedAt == null
        val isDue = reviewState.nextReviewAt.epochMilliseconds <=
            startedAt.epochMilliseconds

        if (isNew || isDue) {
            dueCards += StudyCard(flashcard, reviewState)
        }
    }

    dueCards.sortWith(
        compareBy<StudyCard> { it.reviewState.nextReviewAt.epochMilliseconds }
            .thenBy { it.flashcard.createdAt.epochMilliseconds }
            .thenBy { it.flashcard.id.value },
    )

    return StudySession(dueCards)
}
