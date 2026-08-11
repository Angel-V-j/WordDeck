package com.worddeck.domain.model

data class Flashcard(
    val id: String,
    val deckId: String,
    val front: String,
    val back: String,
)
