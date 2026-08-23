package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextValuesTest {
    @Test
    fun `required text is normalized and blank text is rejected`() {
        assertEquals("hola", CardSide.from("  hola  ").successValue().value)
        assertEquals(
            AppResult.Failure(AppError.Validation("deck title", "must not be blank")),
            DeckTitle.from("   "),
        )
    }

    @Test
    fun `email validation accepts a common address and rejects invalid formats`() {
        assertEquals(
            "maria.petkova+study@example-domain.com",
            EmailAddress.from("maria.petkova+study@example-domain.com").successValue().value,
        )
        assertEquals(
            AppResult.Failure(AppError.Validation("email", "has invalid format")),
            EmailAddress.from("invalid-email"),
        )
        assertEquals(
            AppResult.Failure(AppError.Validation("email", "has invalid format")),
            EmailAddress.from("maria@example"),
        )
    }

    @Test
    fun `optional text is trimmed and blank or missing text becomes null`() {
        assertNull(DeckCategory.from("   "))
        assertNull(DeckLanguage.from(null))
        assertEquals("Spanish", DeckLanguage.from("  Spanish  ")?.value)
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
