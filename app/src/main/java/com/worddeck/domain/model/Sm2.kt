package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult

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
            val quality = entries.firstOrNull { it.value == value }
            if (quality == null) {
                return AppResult.Failure(
                    AppError.Validation(
                        field = "review quality",
                        reason = "must be between 0 and 5",
                    ),
                )
            }
            return AppResult.Success(quality)
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

/** Values supplied to the pure SM-2 calculation in the next task. */
data class Sm2Input(
    val repetition: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val quality: Sm2Quality,
) {
    companion object {
        fun initial(quality: Sm2Quality): Sm2Input = Sm2Input(
            repetition = Sm2Rules.INITIAL_REPETITION,
            easeFactor = Sm2Rules.INITIAL_EASE_FACTOR,
            intervalDays = Sm2Rules.INITIAL_INTERVAL_DAYS,
            quality = quality,
        )
    }
}

/** Updated algorithm values returned by the future pure SM-2 calculation. */
data class Sm2Result(
    val repetition: Int,
    val easeFactor: Double,
    val intervalDays: Int,
)

/**
 * WordDeck's fixed SM-2 parameters.
 *
 * A quality below [MINIMUM_SUCCESS_QUALITY] resets repetition progress. A
 * successful first repetition uses [FIRST_SUCCESS_INTERVAL_DAYS], the second
 * uses [SECOND_SUCCESS_INTERVAL_DAYS], and later intervals use the previous
 * interval and the card's ease factor. The calculation itself belongs to the
 * next task, not to UI or Room code.
 */
object Sm2Rules {
    const val INITIAL_REPETITION = 0
    const val INITIAL_EASE_FACTOR = 2.5
    const val INITIAL_INTERVAL_DAYS = 0
    const val MINIMUM_EASE_FACTOR = 1.3
    const val MINIMUM_SUCCESS_QUALITY = 3
    const val FIRST_SUCCESS_INTERVAL_DAYS = 1
    const val SECOND_SUCCESS_INTERVAL_DAYS = 6

    fun shouldReset(quality: Sm2Quality): Boolean =
        quality.value < MINIMUM_SUCCESS_QUALITY
}
