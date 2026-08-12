package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.worddeck.data.local.entity.ReviewEventEntity

@Dao
interface ReviewEventDao {
    @Insert
    suspend fun insert(reviewEvent: ReviewEventEntity)

    @Query(
        """
        SELECT * FROM review_events
        WHERE cardId = :cardId
        ORDER BY reviewedAt, id
        """,
    )
    suspend fun findByCardId(cardId: String): List<ReviewEventEntity>
}
