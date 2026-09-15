package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import kotlin.math.ceil

/** The six response qualities defined by SM-2. */
enum class Sm2Quality(val value: Int) {
    ZERO(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    ;

    companion object {
        fun from(value: Int): AppResult<Sm2Quality> {
            val matchingQuality = entries.firstOrNull { it.value == value }
            if (matchingQuality == null) {
                return AppResult.Failure(
                    AppError.Validation(
                        field = "review quality",
                        reason = "unknown value $value; expected 0 to 5",
                    ),
                )
            }
            return AppResult.Success(matchingQuality)
        }
    }
}

/**
 * The four simple choices shown by WordDeck and their explicit SM-2 qualities.
 *
 * The UI does not invent or calculate quality values itself.
 */
enum class ReviewRating(val quality: Sm2Quality) {
    AGAIN(Sm2Quality.ZERO),
    HARD(Sm2Quality.THREE),
    GOOD(Sm2Quality.FOUR),
    EASY(Sm2Quality.FIVE),
}

/** Previous review progress and the quality of the current answer. */
data class Sm2Input(
    val repetition: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val successfulReviewCount: Int,
    val failedReviewCount: Int,
    val quality: Sm2Quality,
)

/** Updated algorithm values returned after reviewing one card. */
data class Sm2Result(
    val repetition: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val successfulReviewCount: Int,
    val failedReviewCount: Int,
    val masteryLevel: MasteryLevel,
    val lastQuality: Sm2Quality,
    val lastReviewedAt: Timestamp,
    val nextReviewAt: Timestamp,
)

/**
 * WordDeck's fixed SM-2 parameters.
 *
 * A quality below [MINIMUM_SUCCESS_QUALITY] resets repetition progress. A
 * successful first repetition uses [FIRST_SUCCESS_INTERVAL_DAYS], the second
 * uses [SECOND_SUCCESS_INTERVAL_DAYS], and later intervals use the previous
 * interval and the card's ease factor.
 */
object Sm2Rules {
    const val INITIAL_REPETITION = 0
    const val INITIAL_EASE_FACTOR = 2.5
    const val INITIAL_INTERVAL_DAYS = 0
    const val MINIMUM_EASE_FACTOR = 1.3
    const val MINIMUM_SUCCESS_QUALITY = 3
    const val RESET_INTERVAL_DAYS = 1
    const val FIRST_SUCCESS_INTERVAL_DAYS = 1
    const val SECOND_SUCCESS_INTERVAL_DAYS = 6
    const val PROBLEMATIC_FAILURE_THRESHOLD = 3
    const val MASTERED_REPETITION_THRESHOLD = 4

    fun shouldReset(quality: Sm2Quality): Boolean =
        quality.value < MINIMUM_SUCCESS_QUALITY

    fun classifyMastery(
        repetition: Int,
        successfulReviewCount: Int,
        failedReviewCount: Int,
        lastQuality: Sm2Quality?,
    ): MasteryLevel = when {
        successfulReviewCount == 0 && failedReviewCount == 0 -> MasteryLevel.NEW
        lastQuality != null &&
            shouldReset(lastQuality) &&
            failedReviewCount >= PROBLEMATIC_FAILURE_THRESHOLD -> MasteryLevel.PROBLEMATIC
        repetition >= MASTERED_REPETITION_THRESHOLD -> MasteryLevel.MASTERED
        else -> MasteryLevel.LEARNING
    }
}

/**
 * Applies one SM-2 review without reading Android or system time.
 *
 * [reviewedAt] is supplied by the caller, which keeps this calculation
 * deterministic and easy to unit-test.
 */
object Sm2Scheduler {
    fun review(input: Sm2Input, reviewedAt: Timestamp): Sm2Result {
        val shouldReset = Sm2Rules.shouldReset(input.quality)

        val newRepetition = if (shouldReset) {
            Sm2Rules.INITIAL_REPETITION
        } else {
            input.repetition + 1
        }

        val newIntervalDays = when {
            shouldReset -> Sm2Rules.RESET_INTERVAL_DAYS
            input.repetition == 0 -> Sm2Rules.FIRST_SUCCESS_INTERVAL_DAYS
            input.repetition == 1 -> Sm2Rules.SECOND_SUCCESS_INTERVAL_DAYS
            else -> ceil(input.intervalDays * input.easeFactor).toInt()
        }

        val qualityDifference = 5 - input.quality.value
        val easeFactorChange = 0.1 - qualityDifference * (
            0.08 + qualityDifference * 0.02
        )
        val newEaseFactor = (input.easeFactor + easeFactorChange)
            .coerceAtLeast(Sm2Rules.MINIMUM_EASE_FACTOR)

        val newSuccessfulReviewCount = if (shouldReset) {
            input.successfulReviewCount
        } else {
            input.successfulReviewCount + 1
        }
        val newFailedReviewCount = if (shouldReset) {
            input.failedReviewCount + 1
        } else {
            input.failedReviewCount
        }
        val newMasteryLevel = Sm2Rules.classifyMastery(
            repetition = newRepetition,
            successfulReviewCount = newSuccessfulReviewCount,
            failedReviewCount = newFailedReviewCount,
            lastQuality = input.quality,
        )
        val nextReviewAt = Timestamp(
            reviewedAt.epochMilliseconds +
                newIntervalDays.toLong() * MILLISECONDS_PER_DAY,
        )

        return Sm2Result(
            repetition = newRepetition,
            easeFactor = newEaseFactor,
            intervalDays = newIntervalDays,
            successfulReviewCount = newSuccessfulReviewCount,
            failedReviewCount = newFailedReviewCount,
            masteryLevel = newMasteryLevel,
            lastQuality = input.quality,
            lastReviewedAt = reviewedAt,
            nextReviewAt = nextReviewAt,
        )
    }
}

private const val MILLISECONDS_PER_DAY = 24L * 60L * 60L * 1000L
