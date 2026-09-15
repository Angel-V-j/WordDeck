package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.worddeck.data.local.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("DELETE FROM decks WHERE ownerId = :ownerId")
    suspend fun deleteByOwner(ownerId: String)

    @Upsert
    suspend fun save(deck: DeckEntity)

    @Query(
        """
        UPDATE decks
        SET deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            pendingSync = 1
        WHERE id = :id AND deletedAt IS NULL
        """,
    )
    suspend fun markDeleted(id: String, deletedAt: Long): Int

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE ownerId = :ownerId AND pendingSync = 1")
    suspend fun findPendingByOwner(ownerId: String): List<DeckEntity>

    @Query("SELECT id FROM decks WHERE ownerId = :ownerId")
    suspend fun findIdsByOwner(ownerId: String): List<String>

    @Query(
        """
        UPDATE decks SET pendingSync = 0
        WHERE id = :id AND updatedAt = :updatedAt
        """,
    )
    suspend fun markSynced(id: String, updatedAt: Long): Int

    @Query(
        """
        SELECT * FROM decks
        WHERE ownerId = :ownerId
          AND deletedAt IS NULL
        ORDER BY updatedAt DESC, id
        """,
    )
    fun observeByOwner(ownerId: String): Flow<List<DeckEntity>>
}
