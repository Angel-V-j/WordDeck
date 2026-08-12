package com.worddeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_events",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["cardId", "reviewedAt"]),
        Index(value = ["reviewedAt"]),
    ],
)
data class ReviewEventEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val quality: Int,
    val reviewedAt: Long,
)
