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
    fun `blank optional text is normalized to null`() {
        val result = DeckCategory.from("   ")

        assertNull(result.successValue())
    }

    @Test
    fun `missing optional language is represented as null`() {
        val result = DeckLanguage.from(null)

        assertNull(result.successValue())
    }

    @Test
    fun `specified optional language is trimmed`() {
        val result = DeckLanguage.from("  Spanish  ")

        assertEquals("Spanish", result.successValue()?.value)
    }

    @Test
    fun `different identifier types share validation without becoming interchangeable`() {
        val userId = UserId.from(" user-1 ").successValue()
        val deckId = DeckId.from(" deck-1 ").successValue()

        assertEquals("user-1", userId.value)
        assertEquals("deck-1", deckId.value)
        assertTrue(userId::class != deckId::class)
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
