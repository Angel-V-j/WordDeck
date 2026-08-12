package com.worddeck.domain.model

data class Deck(
    val id: DeckId,
    val ownerId: UserId,
    val title: String,
)
