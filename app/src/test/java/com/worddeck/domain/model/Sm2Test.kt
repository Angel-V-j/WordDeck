package com.worddeck.domain.model

import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.Timestamp
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2Test {
    @Test
    fun `quality boundaries and UI rating mapping follow the documented scale`() {
        assertEquals(0, Sm2Quality.from(0).successValue().value)
        assertEquals(5, Sm2Quality.from(5).successValue().value)
        assertTrue(Sm2Quality.from(-1) is AppResult.Failure)
        assertTrue(Sm2Quality.from(6) is AppResult.Failure)

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
        assertEquals(0, input.successfulReviewCount)
        assertEquals(0, input.failedReviewCount)
        assertEquals(MasteryLevel.NEW, Sm2Rules.classifyMastery(0, 0, 0, null))
    }

    @Test
    fun `successful reviews use one six and rounded multiplied intervals`() {
        val fixedClock = Clock { REVIEWED_AT }
        val first = Sm2Scheduler.review(
            Sm2Input.initial(Sm2Quality.FOUR),
            fixedClock.now(),
        )
        val second = Sm2Scheduler.review(
            Sm2Input(
                repetition = first.repetition,
                easeFactor = first.easeFactor,
                intervalDays = first.intervalDays,
                successfulReviewCount = first.successfulReviewCount,
                failedReviewCount = first.failedReviewCount,
                quality = Sm2Quality.FOUR,
            ),
            REVIEWED_AT,
        )
        val later = Sm2Scheduler.review(
            Sm2Input(
                repetition = 2,
                easeFactor = 2.3,
                intervalDays = 6,
                successfulReviewCount = 2,
                failedReviewCount = 0,
                quality = Sm2Quality.FOUR,
            ),
            REVIEWED_AT,
        )

        assertEquals(1, first.intervalDays)
        assertEquals(6, second.intervalDays)
        assertEquals(14, later.intervalDays)
        assertEquals(3, later.successfulReviewCount)
        assertEquals(
            REVIEWED_AT.epochMilliseconds + 14 * DAY,
            later.nextReviewAt.epochMilliseconds,
        )
    }

    @Test
    fun `failed review resets progress and updates failure classification`() {
        val result = Sm2Scheduler.review(
            Sm2Input(
                repetition = 5,
                easeFactor = 2.5,
                intervalDays = 30,
                successfulReviewCount = 5,
                failedReviewCount = 2,
                quality = Sm2Quality.TWO,
            ),
            REVIEWED_AT,
        )

        assertEquals(0, result.repetition)
        assertEquals(1, result.intervalDays)
        assertEquals(2.18, result.easeFactor, DOUBLE_TOLERANCE)
        assertEquals(5, result.successfulReviewCount)
        assertEquals(3, result.failedReviewCount)
        assertEquals(MasteryLevel.PROBLEMATIC, result.masteryLevel)
    }

    @Test
    fun `ease factor never falls below the minimum`() {
        val result = Sm2Scheduler.review(
            Sm2Input(
                repetition = 3,
                easeFactor = Sm2Rules.MINIMUM_EASE_FACTOR,
                intervalDays = 10,
                successfulReviewCount = 3,
                failedReviewCount = 2,
                quality = Sm2Quality.ZERO,
            ),
            REVIEWED_AT,
        )

        assertEquals(Sm2Rules.MINIMUM_EASE_FACTOR, result.easeFactor, DOUBLE_TOLERANCE)
    }

    @Test
    fun `mastery moves back to learning after success and reaches mastered after four successes`() {
        val recovered = Sm2Scheduler.review(
            Sm2Input(0, 2.0, 1, 2, 3, Sm2Quality.FOUR),
            REVIEWED_AT,
        )
        val mastered = Sm2Scheduler.review(
            Sm2Input(3, 2.5, 15, 3, 0, Sm2Quality.FOUR),
            REVIEWED_AT,
        )

        assertEquals(MasteryLevel.LEARNING, recovered.masteryLevel)
        assertEquals(MasteryLevel.MASTERED, mastered.masteryLevel)
        assertEquals(4, mastered.successfulReviewCount)
    }
}

private val REVIEWED_AT = Timestamp(
    Instant.parse("2026-08-14T09:30:00Z").toEpochMilli(),
)
private const val DAY = 86_400_000L
private const val DOUBLE_TOLERANCE = 0.000000001

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
