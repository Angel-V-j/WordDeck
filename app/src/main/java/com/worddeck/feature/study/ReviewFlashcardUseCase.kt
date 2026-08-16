package com.worddeck.feature.study

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewEventId
import com.worddeck.domain.model.ReviewRating
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.Sm2Input
import com.worddeck.domain.model.Sm2Scheduler
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.ReviewRepository

/** Coordinates one real review: SM-2 calculation followed by one atomic repository write. */
class ReviewFlashcardUseCase(
    private val reviewRepository: ReviewRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(
        currentUserId: UserId,
        currentState: ReviewState,
        rating: ReviewRating,
    ): AppResult<ReviewState> {
        if (currentState.userId != currentUserId) {
            return AppResult.Failure(
                AppError.Validation("review state", "belongs to another user"),
            )
        }

        val eventId = when (val result = ReviewEventId.from(idGenerator.generate())) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val reviewedAt = clock.now()
        val result = Sm2Scheduler.review(
            input = Sm2Input(
                repetition = currentState.repetition,
                easeFactor = currentState.easeFactor,
                intervalDays = currentState.intervalDays,
                successfulReviewCount = currentState.successfulReviewCount,
                failedReviewCount = currentState.failedReviewCount,
                quality = rating.quality,
            ),
            reviewedAt = reviewedAt,
        )
        val updatedState = ReviewState(
            userId = currentUserId,
            cardId = currentState.cardId,
            repetition = result.repetition,
            easeFactor = result.easeFactor,
            intervalDays = result.intervalDays,
            lastReviewedAt = result.lastReviewedAt,
            lastQuality = result.lastQuality,
            nextReviewAt = result.nextReviewAt,
            successfulReviewCount = result.successfulReviewCount,
            failedReviewCount = result.failedReviewCount,
            masteryLevel = result.masteryLevel,
        )
        val reviewEvent = ReviewEvent(
            id = eventId,
            userId = currentUserId,
            cardId = currentState.cardId,
            quality = rating.quality,
            reviewedAt = reviewedAt,
        )

        return when (val saveResult = reviewRepository.recordReview(updatedState, reviewEvent)) {
            is AppResult.Success -> AppResult.Success(updatedState)
            is AppResult.Failure -> saveResult
        }
    }
}
