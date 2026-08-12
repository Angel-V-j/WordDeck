package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.worddeck.data.local.entity.DeckEntity

@Dao
interface DeckDao {
    @Insert
    suspend fun insert(deck: DeckEntity)

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DeckEntity?
}
