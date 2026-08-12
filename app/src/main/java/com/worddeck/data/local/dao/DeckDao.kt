package com.worddeck.data.local.dao

import androidx.room.Dao
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
