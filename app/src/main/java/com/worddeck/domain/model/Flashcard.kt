package com.worddeck.domain.model

data class Flashcard(
    val id: CardId,
    val deckId: DeckId,
    val front: String,
    val back: String,
)
