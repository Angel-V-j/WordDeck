package com.worddeck.domain.model

import com.worddeck.common.Timestamp

/**
 * Persisted learning classification for a flashcard.
 *
 * The exact transition thresholds belong to the spaced-repetition business rules:
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
    val cardId: CardId,
    val repetition: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val lastReviewedAt: Timestamp?,
    val lastQuality: Int?,
    val nextReviewAt: Timestamp,
    val successfulReviewCount: Int,
    val failedReviewCount: Int,
    val masteryLevel: MasteryLevel,
)
