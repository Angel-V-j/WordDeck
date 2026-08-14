package com.worddeck.domain.model

import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudySessionTest {
    @Test
    fun `card without review state is new and receives an initial state`() {
        val newCard = flashcard(id = "new-card")

        val session = startStudySession(
            userId = USER_ID,
            deck = deck(),
            flashcards = listOf(newCard),
            reviewStates = emptyList(),
            startedAt = NOW,
        )

        assertEquals(listOf(newCard), session.cards.map { it.flashcard })

        val initialState = session.cards.single().reviewState
        assertEquals(USER_ID, initialState.userId)
        assertEquals(newCard.id, initialState.cardId)
        assertEquals(0, initialState.repetition)
        assertEquals(2.5, initialState.easeFactor, 0.0)
        assertEquals(0, initialState.intervalDays)
        assertNull(initialState.lastReviewedAt)
        assertNull(initialState.lastQuality)
        assertEquals(NOW, initialState.nextReviewAt)
        assertEquals(0, initialState.successfulReviewCount)
        assertEquals(0, initialState.failedReviewCount)
        assertEquals(MasteryLevel.NEW, initialState.masteryLevel)
    }

    @Test
    fun `overdue and exactly due cards are included but future card is excluded`() {
        val overdueCard = flashcard(id = "overdue", createdAt = Timestamp(3_000))
        val dueNowCard = flashcard(id = "due-now", createdAt = Timestamp(1_000))
        val futureCard = flashcard(id = "future", createdAt = Timestamp(2_000))

        val session = startStudySession(
            userId = USER_ID,
            deck = deck(),
            flashcards = listOf(futureCard, dueNowCard, overdueCard),
            reviewStates = listOf(
                reviewState(cardId = futureCard.id, nextReviewAt = Timestamp(10_001)),
                reviewState(cardId = dueNowCard.id, nextReviewAt = NOW),
                reviewState(cardId = overdueCard.id, nextReviewAt = Timestamp(8_000)),
            ),
            startedAt = NOW,
        )

        assertEquals(
            listOf(overdueCard, dueNowCard),
            session.cards.map { it.flashcard },
        )
    }

    @Test
    fun `deck owned by another user cannot start a session`() {
        val session = startStudySession(
            userId = USER_ID,
            deck = deck(ownerId = OTHER_USER_ID),
            flashcards = listOf(flashcard()),
            reviewStates = emptyList(),
            startedAt = NOW,
        )

        assertEquals(emptyList<StudyCard>(), session.cards)
    }

    @Test
    fun `session uses only selected deck cards and current user progress`() {
        val selectedCard = flashcard(id = "selected-card")
        val otherDeckCard = flashcard(
            id = "other-card",
            deckId = OTHER_DECK_ID,
        )

        val session = startStudySession(
            userId = USER_ID,
            deck = deck(),
            flashcards = listOf(otherDeckCard, selectedCard),
            reviewStates = listOf(
                reviewState(
                    userId = OTHER_USER_ID,
                    cardId = selectedCard.id,
                    nextReviewAt = Timestamp(20_000),
                ),
            ),
            startedAt = NOW,
        )

        assertEquals(listOf(selectedCard), session.cards.map { it.flashcard })
        assertEquals(USER_ID, session.cards.single().reviewState.userId)
        assertEquals(MasteryLevel.NEW, session.cards.single().reviewState.masteryLevel)
    }
}

private val USER_ID = UserId.from("user-1").successValue()
private val OTHER_USER_ID = UserId.from("user-2").successValue()
private val DECK_ID = DeckId.from("deck-1").successValue()
private val OTHER_DECK_ID = DeckId.from("deck-2").successValue()
private val NOW = Timestamp(10_000)

private fun deck(ownerId: UserId = USER_ID) = Deck(
    id = DECK_ID,
    ownerId = ownerId,
    title = DeckTitle.from("Spanish").successValue(),
    sourceLanguage = DeckLanguage.from("English"),
    targetLanguage = DeckLanguage.from("Spanish"),
    category = DeckCategory.from("Travel"),
    visibility = DeckVisibility.PRIVATE,
    createdAt = Timestamp(1_000),
    updatedAt = Timestamp(2_000),
)

private fun flashcard(
    id: String = "card-1",
    deckId: DeckId = DECK_ID,
    createdAt: Timestamp = Timestamp(1_000),
) = Flashcard(
    id = CardId.from(id).successValue(),
    deckId = deckId,
    front = CardSide.from("hello").successValue(),
    back = CardSide.from("hola").successValue(),
    exampleSentence = null,
    additionalInformation = null,
    createdAt = createdAt,
    updatedAt = createdAt,
)

private fun reviewState(
    userId: UserId = USER_ID,
    cardId: CardId,
    nextReviewAt: Timestamp,
) = ReviewState(
    userId = userId,
    cardId = cardId,
    repetition = 2,
    easeFactor = 2.5,
    intervalDays = 6,
    lastReviewedAt = Timestamp(1_000),
    lastQuality = Sm2Quality.FOUR,
    nextReviewAt = nextReviewAt,
    successfulReviewCount = 2,
    failedReviewCount = 0,
    masteryLevel = MasteryLevel.LEARNING,
)

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
