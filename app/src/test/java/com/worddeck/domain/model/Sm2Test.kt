package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2Test {
    @Test
    fun `quality accepts every value from zero to five`() {
        for (value in 0..5) {
            assertEquals(value, Sm2Quality.from(value).successValue().value)
        }
    }

    @Test
    fun `quality rejects values outside the SM-2 scale`() {
        val expectedError = AppError.Validation(
            field = "review quality",
            reason = "must be between 0 and 5",
        )

        assertEquals(AppResult.Failure(expectedError), Sm2Quality.from(-1))
        assertEquals(AppResult.Failure(expectedError), Sm2Quality.from(6))
    }

    @Test
    fun `review ratings have an explicit SM-2 quality mapping`() {
        assertEquals(Sm2Quality.ZERO, ReviewRating.AGAIN.quality)
        assertEquals(Sm2Quality.THREE, ReviewRating.HARD.quality)
        assertEquals(Sm2Quality.FOUR, ReviewRating.GOOD.quality)
        assertEquals(Sm2Quality.FIVE, ReviewRating.EASY.quality)
    }

    @Test
    fun `initial input uses the documented SM-2 defaults`() {
        val input = Sm2Input.initial(Sm2Quality.FOUR)

        assertEquals(0, input.repetition)
        assertEquals(2.5, input.easeFactor, 0.0)
        assertEquals(0, input.intervalDays)
        assertEquals(Sm2Quality.FOUR, input.quality)
        assertEquals(1.3, Sm2Rules.MINIMUM_EASE_FACTOR, 0.0)
        assertEquals(1, Sm2Rules.FIRST_SUCCESS_INTERVAL_DAYS)
        assertEquals(6, Sm2Rules.SECOND_SUCCESS_INTERVAL_DAYS)
    }

    @Test
    fun `qualities below three reset successful repetition progress`() {
        assertTrue(Sm2Rules.shouldReset(Sm2Quality.ZERO))
        assertTrue(Sm2Rules.shouldReset(Sm2Quality.ONE))
        assertTrue(Sm2Rules.shouldReset(Sm2Quality.TWO))
        assertFalse(Sm2Rules.shouldReset(Sm2Quality.THREE))
        assertFalse(Sm2Rules.shouldReset(Sm2Quality.FIVE))
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
