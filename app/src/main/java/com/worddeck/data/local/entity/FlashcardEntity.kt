package com.worddeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["ownerId"]),
    ],
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val deckId: String,
    val front: String,
    val back: String,
    val exampleSentence: String?,
    val additionalInformation: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
