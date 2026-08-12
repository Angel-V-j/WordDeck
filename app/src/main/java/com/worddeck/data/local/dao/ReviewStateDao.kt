package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.worddeck.data.local.entity.ReviewStateEntity

@Dao
interface ReviewStateDao {
    @Insert
    suspend fun insert(reviewState: ReviewStateEntity)

    @Query("SELECT * FROM review_states WHERE cardId = :cardId LIMIT 1")
    suspend fun findByCardId(cardId: String): ReviewStateEntity?
}
