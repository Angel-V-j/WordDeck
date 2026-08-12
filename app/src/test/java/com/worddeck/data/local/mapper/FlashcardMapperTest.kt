package com.worddeck.data.local.mapper

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.data.local.entity.FlashcardEntity
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.CardSide
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard
import org.junit.Assert.assertEquals
import org.junit.Test

class FlashcardMapperTest {
    @Test
    fun `domain flashcard round trips through Room entity`() {
        val flashcard = Flashcard(
            id = CardId.from("card-1").successValue(),
            deckId = DeckId.from("deck-1").successValue(),
            front = CardSide.from("hello").successValue(),
            back = CardSide.from("hola").successValue(),
            exampleSentence = "Hello, how are you?",
            additionalInformation = "Common greeting",
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
        )

        assertEquals(AppResult.Success(flashcard), flashcard.toEntity().toDomain())
    }

    @Test
    fun `invalid persisted card side returns validation failure`() {
        val entity = FlashcardEntity(
            id = "card-1",
            deckId = "deck-1",
            front = " ",
            back = "hola",
            exampleSentence = null,
            additionalInformation = null,
            createdAt = 1_000,
            updatedAt = 2_000,
        )

        assertEquals(
            AppResult.Failure(AppError.Validation("card side", "must not be blank")),
            entity.toDomain(),
        )
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
