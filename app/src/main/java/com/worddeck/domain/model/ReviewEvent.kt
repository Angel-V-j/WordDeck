package com.worddeck.domain.model

import com.worddeck.common.Timestamp

/** Immutable record of one completed flashcard review. */
data class ReviewEvent(
    val id: ReviewEventId,
    val userId: UserId,
    val cardId: CardId,
    val quality: Sm2Quality,
    val reviewedAt: Timestamp,
)
