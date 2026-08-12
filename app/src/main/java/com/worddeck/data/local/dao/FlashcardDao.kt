package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.worddeck.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Upsert
    suspend fun save(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): FlashcardEntity?

    @Query(
        """
        SELECT * FROM flashcards
        WHERE deckId = :deckId
        ORDER BY createdAt, id
        """,
    )
    fun observeByDeck(deckId: String): Flow<List<FlashcardEntity>>
}
