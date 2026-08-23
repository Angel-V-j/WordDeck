package com.worddeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "review_states",
    primaryKeys = ["userId", "cardId"],
    foreignKeys = [
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["cardId"])],
)
data class ReviewStateEntity(
    val userId: String,
    val cardId: String,
    val repetition: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val lastReviewedAt: Long?,
    val lastQuality: Int?,
    val nextReviewAt: Long,
    val successfulReviewCount: Int,
    val failedReviewCount: Int,
    val masteryLevel: String,
    val updatedAt: Long = lastReviewedAt ?: nextReviewAt,
    val pendingSync: Boolean = true,
)
