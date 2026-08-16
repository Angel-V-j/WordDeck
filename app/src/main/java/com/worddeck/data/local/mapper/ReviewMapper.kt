package com.worddeck.data.local.mapper

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.data.local.entity.ReviewEventEntity
import com.worddeck.data.local.entity.ReviewStateEntity
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.MasteryLevel
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewEventId
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.Sm2Quality
import com.worddeck.domain.model.UserId

internal fun ReviewState.toEntity(): ReviewStateEntity = ReviewStateEntity(
    userId = userId.value,
    cardId = cardId.value,
    repetition = repetition,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    lastReviewedAt = lastReviewedAt?.epochMilliseconds,
    lastQuality = lastQuality?.value,
    nextReviewAt = nextReviewAt.epochMilliseconds,
    successfulReviewCount = successfulReviewCount,
    failedReviewCount = failedReviewCount,
    masteryLevel = masteryLevel.name,
)

internal fun ReviewStateEntity.toDomain(): AppResult<ReviewState> {
    val domainUserId = UserId.from(userId).valueOrReturnFailure { return it }
    val domainCardId = CardId.from(cardId).valueOrReturnFailure { return it }
    val domainLastQuality = if (lastQuality == null) {
        null
    } else {
        Sm2Quality.from(lastQuality).valueOrReturnFailure { return it }
    }
    val domainMasteryLevel = MasteryLevel.entries.firstOrNull { it.name == masteryLevel }
        ?: return AppResult.Failure(
            AppError.Validation("mastery level", "Unknown value: $masteryLevel"),
        )

    return AppResult.Success(
        ReviewState(
            userId = domainUserId,
            cardId = domainCardId,
            repetition = repetition,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            lastReviewedAt = lastReviewedAt?.let(::Timestamp),
            lastQuality = domainLastQuality,
            nextReviewAt = Timestamp(nextReviewAt),
            successfulReviewCount = successfulReviewCount,
            failedReviewCount = failedReviewCount,
            masteryLevel = domainMasteryLevel,
        ),
    )
}

internal fun ReviewEvent.toEntity(): ReviewEventEntity = ReviewEventEntity(
    id = id.value,
    userId = userId.value,
    cardId = cardId.value,
    quality = quality.value,
    reviewedAt = reviewedAt.epochMilliseconds,
)

internal fun ReviewEventEntity.toDomain(): AppResult<ReviewEvent> {
    val domainId = ReviewEventId.from(id).valueOrReturnFailure { return it }
    val domainUserId = UserId.from(userId).valueOrReturnFailure { return it }
    val domainCardId = CardId.from(cardId).valueOrReturnFailure { return it }
    val domainQuality = Sm2Quality.from(quality).valueOrReturnFailure { return it }

    return AppResult.Success(
        ReviewEvent(
            id = domainId,
            userId = domainUserId,
            cardId = domainCardId,
            quality = domainQuality,
            reviewedAt = Timestamp(reviewedAt),
        ),
    )
}

internal fun List<ReviewStateEntity>.toDomainReviewStates(): AppResult<List<ReviewState>> {
    val states = ArrayList<ReviewState>(size)
    for (entity in this) {
        when (val result = entity.toDomain()) {
            is AppResult.Success -> states += result.value
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(states)
}

internal fun List<ReviewEventEntity>.toDomainReviewEvents(): AppResult<List<ReviewEvent>> {
    val events = ArrayList<ReviewEvent>(size)
    for (entity in this) {
        when (val result = entity.toDomain()) {
            is AppResult.Success -> events += result.value
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(events)
}

// Stop at the first invalid Room value instead of returning partly valid review data.
private inline fun <T> AppResult<T>.valueOrReturnFailure(
    onFailure: (AppResult.Failure) -> Nothing,
): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> onFailure(this)
}
