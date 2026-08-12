package com.worddeck.data.local.mapper

import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.data.local.entity.FlashcardEntity
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.CardSide
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard

internal fun Flashcard.toEntity(): FlashcardEntity = FlashcardEntity(
    id = id.value,
    deckId = deckId.value,
    front = front.value,
    back = back.value,
    exampleSentence = exampleSentence,
    additionalInformation = additionalInformation,
    createdAt = createdAt.epochMilliseconds,
    updatedAt = updatedAt.epochMilliseconds,
)

internal fun FlashcardEntity.toDomain(): AppResult<Flashcard> {
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
            exampleSentence = exampleSentence,
            additionalInformation = additionalInformation,
            createdAt = Timestamp(createdAt),
            updatedAt = Timestamp(updatedAt),
        ),
    )
}

internal fun List<FlashcardEntity>.toDomainFlashcards(): AppResult<List<Flashcard>> {
    val flashcards = ArrayList<Flashcard>(size)
    for (entity in this) {
        when (val result = entity.toDomain()) {
            is AppResult.Success -> flashcards += result.value
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(flashcards)
}

// Stop at the first invalid Room value instead of creating a partly valid domain model.
private inline fun <T> AppResult<T>.valueOrReturnFailure(
    onFailure: (AppResult.Failure) -> Nothing,
): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> onFailure(this)
}
