package com.worddeck.domain.model

import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudySessionTest {
    @Test
    fun `new card receives the documented initial review state`() {
        val newCard = flashcard(id = "new-card")

        val state = startStudySession(
            userId = USER_ID,
            deck = deck(),
            flashcards = listOf(newCard),
            reviewStates = emptyList(),
            startedAt = NOW,
        ).cards.single().reviewState

        assertEquals(USER_ID, state.userId)
        assertEquals(newCard.id, state.cardId)
        assertEquals(0, state.repetition)
        assertEquals(2.5, state.easeFactor, 0.0)
        assertEquals(0, state.intervalDays)
        assertNull(state.lastReviewedAt)
        assertNull(state.lastQuality)
        assertEquals(NOW, state.nextReviewAt)
        assertEquals(MasteryLevel.NEW, state.masteryLevel)
    }

    @Test
    fun `session includes only current owners due cards from the selected deck`() {
        val overdue = flashcard("overdue", createdAt = Timestamp(3_000))
        val dueNow = flashcard("due-now", createdAt = Timestamp(1_000))
        val future = flashcard("future", createdAt = Timestamp(2_000))
        val otherDeckCard = flashcard("other-deck-card", deckId = OTHER_DECK_ID)

        val session = startStudySession(
            userId = USER_ID,
            deck = deck(),
            flashcards = listOf(future, dueNow, otherDeckCard, overdue),
            reviewStates = listOf(
                reviewState(future.id, Timestamp(NOW.epochMilliseconds + 1)),
                reviewState(dueNow.id, NOW),
                reviewState(overdue.id, Timestamp(NOW.epochMilliseconds - 1)),
                reviewState(dueNow.id, Timestamp(20_000), OTHER_USER_ID),
            ),
            startedAt = NOW,
        )
        val foreignSession = startStudySession(
            userId = USER_ID,
            deck = deck(OTHER_USER_ID),
            flashcards = listOf(overdue),
            reviewStates = emptyList(),
            startedAt = NOW,
        )

        assertEquals(listOf(overdue, dueNow), session.cards.map { it.flashcard })
        assertEquals(emptyList<StudyCard>(), foreignSession.cards)
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
    id: String,
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
    cardId: CardId,
    nextReviewAt: Timestamp,
    userId: UserId = USER_ID,
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
