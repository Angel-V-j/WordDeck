package com.worddeck.data.remote.firebase

import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.CardSide
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckCategory
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.DeckLanguage
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
    fun deckRoundTripPreservesDomainValues() {
        val deck = Deck(
            id = DeckId.from("deck-1").successValue(),
            ownerId = UserId.from("user-1").successValue(),
            title = DeckTitle.from("German basics").successValue(),
            sourceLanguage = DeckLanguage.from("Bulgarian"),
            targetLanguage = DeckLanguage.from("German"),
            category = DeckCategory.from("Travel"),
            visibility = DeckVisibility.PRIVATE,
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
        )

        val dto = deck.toFirestoreDto()

        assertEquals(null, dto.deletedAt)
        assertEquals(deck, dto.toDomain().successValue())
    }

    @Test
    fun flashcardRoundTripPreservesDomainValues() {
        val flashcard = Flashcard(
            id = CardId.from("card-1").successValue(),
            deckId = DeckId.from("deck-1").successValue(),
            front = CardSide.from("Haus").successValue(),
            back = CardSide.from("Къща").successValue(),
            exampleSentence = "Das ist mein Haus.",
            additionalInformation = "Съществително име",
            createdAt = Timestamp(3_000),
            updatedAt = Timestamp(4_000),
        )

        val dto = flashcard.toFirestoreDto()

        assertEquals(null, dto.deletedAt)
        assertEquals(flashcard, dto.toDomain().successValue())
    }

    @Test
    fun reviewStateRoundTripPreservesProgressAndSyncTimestamp() {
        val state = ReviewState(
            userId = UserId.from("user-1").successValue(),
            cardId = CardId.from("card-1").successValue(),
            repetition = 3,
            easeFactor = 2.6,
            intervalDays = 16,
            lastReviewedAt = Timestamp(5_000),
            lastQuality = Sm2Quality.from(4).successValue(),
            nextReviewAt = Timestamp(6_000),
            successfulReviewCount = 4,
            failedReviewCount = 1,
            masteryLevel = MasteryLevel.LEARNING,
        )
        val updatedAt = Timestamp(5_000)

        val dto = state.toFirestoreDto(updatedAt)

        assertEquals(updatedAt.epochMilliseconds, dto.updatedAt)
        assertEquals(state, dto.toDomain().successValue())
    }

    @Test
    fun reviewEventRoundTripPreservesImmutableEvent() {
        val event = ReviewEvent(
            id = ReviewEventId.from("event-1").successValue(),
            userId = UserId.from("user-1").successValue(),
            cardId = CardId.from("card-1").successValue(),
            quality = Sm2Quality.from(5).successValue(),
            reviewedAt = Timestamp(7_000),
        )

        assertEquals(event, event.toFirestoreDto().toDomain().successValue())
    }

    @Test
    fun tombstonesAreNotMappedToActiveDomainContent() {
        val deletedDeck = DeckDto(
            id = "deck-1",
            ownerId = "user-1",
            title = "Deleted deck",
            visibility = DeckVisibility.PRIVATE.name,
            createdAt = 1_000,
            updatedAt = 2_000,
            deletedAt = 2_000,
        )
        val deletedFlashcard = FlashcardDto(
            id = "card-1",
            deckId = "deck-1",
            front = "Front",
            back = "Back",
            createdAt = 1_000,
            updatedAt = 2_000,
            deletedAt = 2_000,
        )

        assertTrue(deletedDeck.toDomain() is AppResult.Failure)
        assertTrue(deletedFlashcard.toDomain() is AppResult.Failure)
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
