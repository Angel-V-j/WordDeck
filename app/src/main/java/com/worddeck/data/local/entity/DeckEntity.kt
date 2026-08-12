package com.worddeck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decks",
    indices = [Index(value = ["ownerId"])],
)
data class DeckEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val title: String,
    val sourceLanguage: String?,
    val targetLanguage: String?,
    val category: String?,
    val visibility: String,
    val createdAt: Long,
    val updatedAt: Long,
)
