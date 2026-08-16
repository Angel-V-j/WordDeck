package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextValuesTest {
    @Test
    fun `required text is trimmed before value class is created`() {
        val result = CardSide.from("  hola  ")

        assertEquals("hola", result.successValue().value)
    }

    @Test
    fun `blank required text returns validation failure`() {
        val result = DeckTitle.from("   ")

        assertEquals(
            AppResult.Failure(AppError.Validation("deck title", "must not be blank")),
            result,
        )
    }

    @Test
    fun `blank display name returns validation failure`() {
        assertEquals(
            AppResult.Failure(AppError.Validation("display name", "must not be blank")),
            DisplayName.from("   "),
        )
    }

    @Test
    fun `invalid email returns validation failure`() {
        assertEquals(
            AppResult.Failure(AppError.Validation("email", "has invalid format")),
            EmailAddress.from("invalid-email"),
        )
    }

    @Test
    fun `email without top level domain is invalid`() {
        assertEquals(
            AppResult.Failure(AppError.Validation("email", "has invalid format")),
            EmailAddress.from("maria@example"),
        )
    }

    @Test
    fun `common email characters are accepted`() {
        assertEquals(
            "maria.petkova+study@example-domain.com",
            EmailAddress.from("maria.petkova+study@example-domain.com").successValue().value,
        )
    }

    @Test
    fun `blank optional text is normalized to null`() {
        val result = DeckCategory.from("   ")

        assertNull(result)
    }

    @Test
    fun `missing optional language is represented as null`() {
        val result = DeckLanguage.from(null)

        assertNull(result)
    }

    @Test
    fun `specified optional language is trimmed`() {
        val result = DeckLanguage.from("  Spanish  ")

        assertEquals("Spanish", result?.value)
    }

    @Test
    fun `different identifier types are not interchangeable`() {
        val userId = UserId.from("user-1").successValue()
        val deckId = DeckId.from("deck-1").successValue()
        val reviewEventId = ReviewEventId.from("review-1").successValue()

        assertEquals("user-1", userId.value)
        assertEquals("deck-1", deckId.value)
        assertEquals("review-1", reviewEventId.value)
        assertTrue(userId::class != deckId::class)
        assertTrue(deckId::class != reviewEventId::class)
    }

    @Test
    fun `blank identifier returns validation failure`() {
        assertEquals(
            AppResult.Failure(AppError.Validation("deck id", "must not be blank")),
            DeckId.from("   "),
        )
    }

    @Test
    fun `identifier with surrounding whitespace returns validation failure`() {
        assertEquals(
            AppResult.Failure(
                AppError.Validation("deck id", "must not contain surrounding whitespace"),
            ),
            DeckId.from(" deck-1 "),
        )
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
