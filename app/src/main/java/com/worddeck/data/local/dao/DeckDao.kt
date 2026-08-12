package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.worddeck.data.local.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert
    suspend fun insert(deck: DeckEntity)

    @Update
    suspend fun update(deck: DeckEntity): Int

    @Delete
    suspend fun delete(deck: DeckEntity): Int

    @Query(
        """
        SELECT * FROM decks
        WHERE id = :id AND ownerId = :ownerId
        LIMIT 1
        """,
    )
    suspend fun findById(id: String, ownerId: String): DeckEntity?

    @Query(
        """
        SELECT * FROM decks
        WHERE ownerId = :ownerId
        ORDER BY updatedAt DESC, id
        """,
    )
    fun observeByOwner(ownerId: String): Flow<List<DeckEntity>>
}
