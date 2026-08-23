package com.worddeck.data.remote.firebase

/** Primitive-only models used at the Firestore boundary. */
internal data class DeckDto(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val sourceLanguage: String? = null,
    val targetLanguage: String? = null,
    val category: String? = null,
    val visibility: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

internal data class FlashcardDto(
    val id: String = "",
    val deckId: String = "",
    val front: String = "",
    val back: String = "",
    val exampleSentence: String? = null,
    val additionalInformation: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

internal data class ReviewStateDto(
    val userId: String = "",
    val cardId: String = "",
    val repetition: Int = 0,
    val easeFactor: Double = 0.0,
    val intervalDays: Int = 0,
    val lastReviewedAt: Long? = null,
    val lastQuality: Int? = null,
    val nextReviewAt: Long = 0,
    val successfulReviewCount: Int = 0,
    val failedReviewCount: Int = 0,
    val masteryLevel: String = "",
    val updatedAt: Long = 0,
)

internal data class ReviewEventDto(
    val id: String = "",
    val userId: String = "",
    val cardId: String = "",
    val quality: Int = 0,
    val reviewedAt: Long = 0,
)
