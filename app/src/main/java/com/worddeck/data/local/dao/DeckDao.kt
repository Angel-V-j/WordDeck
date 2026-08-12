package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.worddeck.data.local.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Upsert
    suspend fun save(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DeckEntity?

    @Query(
        """
        SELECT * FROM decks
        WHERE ownerId = :ownerId
        ORDER BY updatedAt DESC, id
        """,
    )
    fun observeByOwner(ownerId: String): Flow<List<DeckEntity>>
}
