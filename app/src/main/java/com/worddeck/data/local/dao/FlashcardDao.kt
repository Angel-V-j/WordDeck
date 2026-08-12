package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.worddeck.data.local.entity.FlashcardEntity

@Dao
interface FlashcardDao {
    @Insert
    suspend fun insert(flashcard: FlashcardEntity)

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY createdAt, id")
    suspend fun findByDeckId(deckId: String): List<FlashcardEntity>
}
