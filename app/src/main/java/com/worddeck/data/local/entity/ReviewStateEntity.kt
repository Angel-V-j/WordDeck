package com.worddeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_states",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReviewStateEntity(
    @PrimaryKey val cardId: String,
    val repetition: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val lastReviewedAt: Long?,
    val lastQuality: Int?,
    val nextReviewAt: Long,
)
