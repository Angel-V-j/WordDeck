package com.worddeck.domain.model

import com.worddeck.common.Timestamp

data class Flashcard(
    val id: CardId,
    val ownerId: UserId,
    val deckId: DeckId,
    val front: CardSide,
    val back: CardSide,
    val exampleSentence: String?,
    val additionalInformation: String?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
