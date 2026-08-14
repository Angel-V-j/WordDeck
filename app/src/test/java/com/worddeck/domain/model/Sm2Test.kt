package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.Timestamp
import java.time.Instant
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
        assertEquals(
            AppResult.Failure(
                AppError.Validation(
                    field = "review quality",
                    reason = "unknown value -1; expected 0 to 5",
                ),
            ),
            Sm2Quality.from(-1),
        )
        assertEquals(
            AppResult.Failure(
                AppError.Validation(
                    field = "review quality",
                    reason = "unknown value 6; expected 0 to 5",
                ),
            ),
            Sm2Quality.from(6),
        )
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

    @Test
    fun `all quality values produce the documented reference result`() {
        val cases = listOf(
            QualityCase(Sm2Quality.ZERO, repetition = 0, easeFactor = 1.70),
            QualityCase(Sm2Quality.ONE, repetition = 0, easeFactor = 1.96),
            QualityCase(Sm2Quality.TWO, repetition = 0, easeFactor = 2.18),
            QualityCase(Sm2Quality.THREE, repetition = 1, easeFactor = 2.36),
            QualityCase(Sm2Quality.FOUR, repetition = 1, easeFactor = 2.50),
            QualityCase(Sm2Quality.FIVE, repetition = 1, easeFactor = 2.60),
        )

        for (case in cases) {
            val result = Sm2Scheduler.review(
                input = Sm2Input.initial(case.quality),
                reviewedAt = REVIEWED_AT,
            )

            assertEquals(case.repetition, result.repetition)
            assertEquals(case.easeFactor, result.easeFactor, DOUBLE_TOLERANCE)
            assertEquals(1, result.intervalDays)
            assertEquals(case.quality, result.lastQuality)
            assertEquals(REVIEWED_AT, result.lastReviewedAt)
        }
    }

    @Test
    fun `first second and later successful reviews use one six and multiplied intervals`() {
        val first = Sm2Scheduler.review(
            input = Sm2Input.initial(Sm2Quality.FOUR),
            reviewedAt = REVIEWED_AT,
        )
        val second = Sm2Scheduler.review(
            input = Sm2Input(
                repetition = first.repetition,
                easeFactor = first.easeFactor,
                intervalDays = first.intervalDays,
                quality = Sm2Quality.FOUR,
            ),
            reviewedAt = REVIEWED_AT,
        )
        val third = Sm2Scheduler.review(
            input = Sm2Input(
                repetition = second.repetition,
                easeFactor = second.easeFactor,
                intervalDays = second.intervalDays,
                quality = Sm2Quality.FIVE,
            ),
            reviewedAt = REVIEWED_AT,
        )

        assertEquals(1, first.repetition)
        assertEquals(1, first.intervalDays)
        assertEquals(2, second.repetition)
        assertEquals(6, second.intervalDays)
        assertEquals(3, third.repetition)
        assertEquals(15, third.intervalDays)
        assertEquals(2.6, third.easeFactor, DOUBLE_TOLERANCE)
    }

    @Test
    fun `failed review resets repetition and interval but keeps adjusted ease factor`() {
        val result = Sm2Scheduler.review(
            input = Sm2Input(
                repetition = 5,
                easeFactor = 2.5,
                intervalDays = 30,
                quality = Sm2Quality.TWO,
            ),
            reviewedAt = REVIEWED_AT,
        )

        assertEquals(0, result.repetition)
        assertEquals(1, result.intervalDays)
        assertEquals(2.18, result.easeFactor, DOUBLE_TOLERANCE)
    }

    @Test
    fun `ease factor never falls below the minimum`() {
        val result = Sm2Scheduler.review(
            input = Sm2Input(
                repetition = 3,
                easeFactor = Sm2Rules.MINIMUM_EASE_FACTOR,
                intervalDays = 10,
                quality = Sm2Quality.ZERO,
            ),
            reviewedAt = REVIEWED_AT,
        )

        assertEquals(Sm2Rules.MINIMUM_EASE_FACTOR, result.easeFactor, DOUBLE_TOLERANCE)
    }

    @Test
    fun `fractional interval is rounded up to a whole day`() {
        val result = Sm2Scheduler.review(
            input = Sm2Input(
                repetition = 2,
                easeFactor = 2.3,
                intervalDays = 6,
                quality = Sm2Quality.FOUR,
            ),
            reviewedAt = REVIEWED_AT,
        )

        assertEquals(14, result.intervalDays)
    }

    @Test
    fun `next review date uses the timestamp supplied by a fixed clock`() {
        val fixedClock = Clock { REVIEWED_AT }

        val result = Sm2Scheduler.review(
            input = Sm2Input.initial(Sm2Quality.FOUR),
            reviewedAt = fixedClock.now(),
        )

        assertEquals(
            Instant.parse("2026-08-15T09:30:00Z").toEpochMilli(),
            result.nextReviewAt.epochMilliseconds,
        )
    }
}

private data class QualityCase(
    val quality: Sm2Quality,
    val repetition: Int,
    val easeFactor: Double,
)

private val REVIEWED_AT = Timestamp(
    Instant.parse("2026-08-14T09:30:00Z").toEpochMilli(),
)

private const val DOUBLE_TOLERANCE = 0.000000001

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
