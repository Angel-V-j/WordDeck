package com.worddeck.domain.model

import com.worddeck.common.Timestamp

data class Deck(
    val id: DeckId,
    val ownerId: UserId,
    val title: DeckTitle,
    val sourceLanguage: DeckLanguage?,
    val targetLanguage: DeckLanguage?,
    val category: DeckCategory?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
