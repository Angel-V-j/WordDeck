package com.worddeck.domain.model

import com.worddeck.common.Timestamp

/** Controls public-catalog discoverability, not future classroom membership. */
enum class DeckVisibility {
    PRIVATE,
    PUBLIC,
}

data class Deck(
    val id: DeckId,
    val ownerId: UserId,
    val title: DeckTitle,
    val sourceLanguage: DeckLanguage?,
    val targetLanguage: DeckLanguage?,
    val category: DeckCategory?,
    val visibility: DeckVisibility,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
