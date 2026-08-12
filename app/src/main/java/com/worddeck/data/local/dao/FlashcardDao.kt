package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.worddeck.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Insert
    suspend fun insert(flashcard: FlashcardEntity)

    @Update
    suspend fun update(flashcard: FlashcardEntity): Int

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
