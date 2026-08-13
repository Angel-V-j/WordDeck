package com.worddeck.data.local.mapper

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.data.local.entity.DeckEntity
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckCategory
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.DeckLanguage
import com.worddeck.domain.model.DeckTitle
import com.worddeck.domain.model.DeckVisibility
import com.worddeck.domain.model.UserId

internal fun Deck.toEntity(): DeckEntity = DeckEntity(
    id = id.value,
    ownerId = ownerId.value,
    title = title.value,
    sourceLanguage = sourceLanguage?.value,
    targetLanguage = targetLanguage?.value,
    category = category?.value,
    visibility = visibility.name,
    createdAt = createdAt.epochMilliseconds,
    updatedAt = updatedAt.epochMilliseconds,
)

internal fun DeckEntity.toDomain(): AppResult<Deck> {
    val domainId = DeckId.from(id).valueOrReturnFailure { return it }
    val domainOwnerId = UserId.from(ownerId).valueOrReturnFailure { return it }
    val domainTitle = DeckTitle.from(title).valueOrReturnFailure { return it }
    val domainSourceLanguage = DeckLanguage.from(sourceLanguage)
    val domainTargetLanguage = DeckLanguage.from(targetLanguage)
    val domainCategory = DeckCategory.from(category)
    val domainVisibility = DeckVisibility.entries.firstOrNull { it.name == visibility }
        ?: return AppResult.Failure(
            AppError.Validation("deck visibility", "Unknown value: $visibility"),
        )

    return AppResult.Success(
        Deck(
            id = domainId,
            ownerId = domainOwnerId,
            title = domainTitle,
            sourceLanguage = domainSourceLanguage,
            targetLanguage = domainTargetLanguage,
            category = domainCategory,
            visibility = domainVisibility,
            createdAt = Timestamp(createdAt),
            updatedAt = Timestamp(updatedAt),
        ),
    )
}

internal fun List<DeckEntity>.toDomainDecks(): AppResult<List<Deck>> {
    val decks = ArrayList<Deck>(size)
    for (entity in this) {
        when (val result = entity.toDomain()) {
            is AppResult.Success -> decks += result.value
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(decks)
}

// Stop at the first invalid Room value instead of creating a partly valid domain model.
private inline fun <T> AppResult<T>.valueOrReturnFailure(
    onFailure: (AppResult.Failure) -> Nothing,
): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> onFailure(this)
}
