package com.worddeck.domain.model

import com.worddeck.common.Timestamp

/**
 * Persisted learning classification for a flashcard.
 *
 * The transition thresholds are implemented by [Sm2Rules.classifyMastery]:
 * - [NEW] has no completed reviews;
 * - [LEARNING] has review history but is not classified as mastered or problematic;
 * - [MASTERED] has reached the documented mastery threshold;
 * - [PROBLEMATIC] has reached the documented repeated-failure threshold.
 */
enum class MasteryLevel {
    NEW,
    LEARNING,
    MASTERED,
    PROBLEMATIC,
}

data class ReviewState(
    val userId: UserId,
    val cardId: CardId,
    val repetition: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val lastReviewedAt: Timestamp?,
    val lastQuality: Sm2Quality?,
    val nextReviewAt: Timestamp,
    val successfulReviewCount: Int,
    val failedReviewCount: Int,
    val masteryLevel: MasteryLevel,
) {
    companion object {
        /** Creates the in-memory progress of a card that has never been reviewed. */
        fun initial(
            userId: UserId,
            cardId: CardId,
            dueAt: Timestamp,
        ): ReviewState = ReviewState(
            userId = userId,
            cardId = cardId,
            repetition = Sm2Rules.INITIAL_REPETITION,
            easeFactor = Sm2Rules.INITIAL_EASE_FACTOR,
            intervalDays = Sm2Rules.INITIAL_INTERVAL_DAYS,
            lastReviewedAt = null,
            lastQuality = null,
            nextReviewAt = dueAt,
            successfulReviewCount = 0,
            failedReviewCount = 0,
            masteryLevel = MasteryLevel.NEW,
        )
    }
}
