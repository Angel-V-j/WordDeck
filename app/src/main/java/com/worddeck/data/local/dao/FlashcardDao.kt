package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Delete
    suspend fun delete(flashcard: FlashcardEntity): Int

    @Query(
        """
        SELECT flashcards.* FROM flashcards
        INNER JOIN decks ON decks.id = flashcards.deckId
        WHERE flashcards.id = :id AND decks.ownerId = :ownerId
        LIMIT 1
        """,
    )
    suspend fun findById(id: String, ownerId: String): FlashcardEntity?

    @Query(
        """
        SELECT flashcards.* FROM flashcards
        INNER JOIN decks ON decks.id = flashcards.deckId
        WHERE flashcards.deckId = :deckId AND decks.ownerId = :ownerId
        ORDER BY flashcards.createdAt, flashcards.id
        """,
    )
    fun observeByDeck(deckId: String, ownerId: String): Flow<List<FlashcardEntity>>
}
