package com.worddeck.data.remote.firebase

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.CardSide
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckCategory
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.DeckLanguage
import com.worddeck.domain.model.DeckTitle
import com.worddeck.domain.model.DeckVisibility
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.MasteryLevel
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewEventId
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.Sm2Quality
import com.worddeck.domain.model.UserId

internal fun Deck.toFirestoreDto(
    deletedAt: Timestamp? = null,
    ownerId: UserId = this.ownerId,
): DeckDto = DeckDto(
    id = id.value,
    ownerId = ownerId.value,
    title = title.value,
    sourceLanguage = sourceLanguage?.value,
    targetLanguage = targetLanguage?.value,
    category = category?.value,
    visibility = visibility.name,
    createdAt = createdAt.epochMilliseconds,
    updatedAt = updatedAt.epochMilliseconds,
    deletedAt = deletedAt?.epochMilliseconds,
)

internal fun DeckDto.toDomain(allowDeleted: Boolean = false): AppResult<Deck> {
    if (deletedAt != null && !allowDeleted) {
        return deletedContentFailure("deck")
    }

    val domainId = DeckId.from(id).valueOrReturnFailure { return it }
    val domainOwnerId = UserId.from(ownerId).valueOrReturnFailure { return it }
    val domainTitle = DeckTitle.from(title).valueOrReturnFailure { return it }
    val domainVisibility = DeckVisibility.entries.firstOrNull { it.name == visibility }
        ?: return AppResult.Failure(
            AppError.Validation("deck visibility", "Unknown value: $visibility"),
        )

    return AppResult.Success(
        Deck(
            id = domainId,
            ownerId = domainOwnerId,
            title = domainTitle,
            sourceLanguage = DeckLanguage.from(sourceLanguage),
            targetLanguage = DeckLanguage.from(targetLanguage),
            category = DeckCategory.from(category),
            visibility = domainVisibility,
            createdAt = Timestamp(createdAt),
            updatedAt = Timestamp(updatedAt),
        ),
    )
}

internal fun Flashcard.toFirestoreDto(deletedAt: Timestamp? = null): FlashcardDto = FlashcardDto(
    id = id.value,
    deckId = deckId.value,
    front = front.value,
    back = back.value,
    exampleSentence = exampleSentence,
    additionalInformation = additionalInformation,
    createdAt = createdAt.epochMilliseconds,
    updatedAt = updatedAt.epochMilliseconds,
    deletedAt = deletedAt?.epochMilliseconds,
)

internal fun FlashcardDto.toDomain(allowDeleted: Boolean = false): AppResult<Flashcard> {
    if (deletedAt != null && !allowDeleted) {
        return deletedContentFailure("flashcard")
    }

    val domainId = CardId.from(id).valueOrReturnFailure { return it }
    val domainDeckId = DeckId.from(deckId).valueOrReturnFailure { return it }
    val domainFront = CardSide.from(front).valueOrReturnFailure { return it }
    val domainBack = CardSide.from(back).valueOrReturnFailure { return it }

    return AppResult.Success(
        Flashcard(
            id = domainId,
            deckId = domainDeckId,
            front = domainFront,
            back = domainBack,
            exampleSentence = exampleSentence?.trim()?.ifEmpty { null },
            additionalInformation = additionalInformation?.trim()?.ifEmpty { null },
            createdAt = Timestamp(createdAt),
            updatedAt = Timestamp(updatedAt),
        ),
    )
}

internal fun ReviewState.toFirestoreDto(
    updatedAt: Timestamp,
    userId: UserId = this.userId,
): ReviewStateDto = ReviewStateDto(
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
    updatedAt = updatedAt.epochMilliseconds,
)

internal fun ReviewStateDto.toDomain(): AppResult<ReviewState> {
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

internal fun ReviewEvent.toFirestoreDto(
    userId: UserId = this.userId,
): ReviewEventDto = ReviewEventDto(
    id = id.value,
    userId = userId.value,
    cardId = cardId.value,
    quality = quality.value,
    reviewedAt = reviewedAt.epochMilliseconds,
)

internal fun ReviewEventDto.toDomain(): AppResult<ReviewEvent> {
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

private fun deletedContentFailure(type: String): AppResult.Failure =
    AppResult.Failure(AppError.Validation(type, "is marked as deleted"))

// Stop at the first invalid remote value instead of creating a partly valid domain model.
private inline fun <T> AppResult<T>.valueOrReturnFailure(
    onFailure: (AppResult.Failure) -> Nothing,
): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> onFailure(this)
}
