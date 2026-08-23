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

    @Query(
        """
        UPDATE flashcards
        SET deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            pendingSync = 1
        WHERE id = :id AND deletedAt IS NULL
        """,
    )
    suspend fun markDeleted(id: String, deletedAt: Long): Int

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
        """,
    )
    suspend fun findIdsByOwner(ownerId: String): List<String>

    @Query(
        """
        UPDATE flashcards SET pendingSync = 0
        WHERE id = :id AND updatedAt = :updatedAt
        """,
    )
    suspend fun markSynced(id: String, updatedAt: Long): Int

    @Query(
        """
        SELECT flashcards.* FROM flashcards
        INNER JOIN decks ON decks.id = flashcards.deckId
        WHERE flashcards.deckId = :deckId
          AND decks.deletedAt IS NULL
          AND flashcards.deletedAt IS NULL
        ORDER BY flashcards.createdAt, flashcards.id
        """,
    )
    fun observeByDeck(deckId: String): Flow<List<FlashcardEntity>>
}
