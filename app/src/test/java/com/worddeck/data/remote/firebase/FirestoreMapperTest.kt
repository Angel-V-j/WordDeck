package com.worddeck.data.remote.firebase

import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.CardSide
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.DeckTitle
import com.worddeck.domain.model.DeckVisibility
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.MasteryLevel
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewEventId
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.Sm2Quality
import com.worddeck.domain.model.UserId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreMapperTest {
    @Test
    fun stableFirestoreModelsRoundTripAndTombstonesStayInactive() {
        val userId = UserId.from("user-1").successValue()
        val deck = Deck(
            id = DeckId.from("deck-1").successValue(),
            ownerId = userId,
            title = DeckTitle.from("German basics").successValue(),
            sourceLanguage = null,
            targetLanguage = null,
            category = null,
            visibility = DeckVisibility.PRIVATE,
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
        )
        val card = Flashcard(
            id = CardId.from("card-1").successValue(),
            deckId = deck.id,
            front = CardSide.from("Haus").successValue(),
            back = CardSide.from("Къща").successValue(),
            exampleSentence = "Das ist mein Haus.",
            additionalInformation = null,
            createdAt = Timestamp(3_000),
            updatedAt = Timestamp(4_000),
        )
        val state = ReviewState(
            userId = userId,
            cardId = card.id,
            repetition = 3,
            easeFactor = 2.6,
            intervalDays = 16,
            lastReviewedAt = Timestamp(5_000),
            lastQuality = Sm2Quality.FOUR,
            nextReviewAt = Timestamp(6_000),
            successfulReviewCount = 4,
            failedReviewCount = 1,
            masteryLevel = MasteryLevel.LEARNING,
        )
        val event = ReviewEvent(
            id = ReviewEventId.from("event-1").successValue(),
            userId = userId,
            cardId = card.id,
            quality = Sm2Quality.FIVE,
            reviewedAt = Timestamp(7_000),
        )

        assertEquals(deck, deck.toFirestoreDto().toDomain().successValue())
        assertEquals(card, card.toFirestoreDto().toDomain().successValue())
        assertEquals(state, state.toFirestoreDto(Timestamp(8_000)).toDomain().successValue())
        assertEquals(event, event.toFirestoreDto().toDomain().successValue())
        assertTrue(
            deck.toFirestoreDto().copy(deletedAt = 9_000).toDomain() is AppResult.Failure,
        )
        assertTrue(
            card.toFirestoreDto().copy(deletedAt = 9_000).toDomain() is AppResult.Failure,
        )
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
