package com.worddeck.data.local.mapper

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.data.local.entity.DeckEntity
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckCategory
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.DeckLanguage
import com.worddeck.domain.model.DeckTitle
import com.worddeck.domain.model.DeckVisibility
import com.worddeck.domain.model.UserId
import org.junit.Assert.assertEquals
import org.junit.Test

class DeckMapperTest {
    @Test
    fun `domain deck round trips through Room entity`() {
        val deck = Deck(
            id = DeckId.from("deck-1").successValue(),
            ownerId = UserId.from("user-1").successValue(),
            title = DeckTitle.from("Spanish basics").successValue(),
            sourceLanguage = DeckLanguage.from("English"),
            targetLanguage = DeckLanguage.from("Spanish"),
            category = DeckCategory.from("Vocabulary"),
            visibility = DeckVisibility.PUBLIC,
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
        )

        assertEquals(AppResult.Success(deck), deck.toEntity().toDomain())
    }

    @Test
    fun `invalid persisted title returns validation failure`() {
        val entity = validEntity().copy(title = "   ")

        assertEquals(
            AppResult.Failure(AppError.Validation("deck title", "must not be blank")),
            entity.toDomain(),
        )
    }

    @Test
    fun `unknown persisted visibility returns validation failure`() {
        val entity = validEntity().copy(visibility = "FRIENDS_ONLY")

        assertEquals(
            AppResult.Failure(
                AppError.Validation("deck visibility", "Unknown value: FRIENDS_ONLY"),
            ),
            entity.toDomain(),
        )
    }

    private fun validEntity() = DeckEntity(
        id = "deck-1",
        ownerId = "user-1",
        title = "Spanish basics",
        sourceLanguage = "English",
        targetLanguage = "Spanish",
        category = "Vocabulary",
        visibility = DeckVisibility.PRIVATE.name,
        createdAt = 1_000,
        updatedAt = 2_000,
    )
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
