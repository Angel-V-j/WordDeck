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
        SELECT flashcards.* FROM flashcards
        INNER JOIN decks ON decks.id = flashcards.deckId
        WHERE decks.ownerId = :ownerId
          AND flashcards.pendingSync = 1
        """,
    )
    suspend fun findPendingByOwner(ownerId: String): List<FlashcardEntity>

    @Query(
        """
        SELECT flashcards.id FROM flashcards
        INNER JOIN decks ON decks.id = flashcards.deckId
        WHERE decks.ownerId = :ownerId
          AND decks.deletedAt IS NULL
          AND flashcards.deletedAt IS NULL
        """,
    )
    suspend fun findActiveIdsByOwner(ownerId: String): List<String>

    @Query(
        """
        UPDATE flashcards SET pendingSync = 0
        WHERE id = :id AND updatedAt = :updatedAt
        """,
    )
    suspend fun markSynced(id: String, updatedAt: Long): Int

    @Query(
        """
        SELECT * FROM flashcards
        WHERE deckId = :deckId
          AND deletedAt IS NULL
        ORDER BY createdAt, id
        """,
    )
    fun observeByDeck(deckId: String): Flow<List<FlashcardEntity>>
}
