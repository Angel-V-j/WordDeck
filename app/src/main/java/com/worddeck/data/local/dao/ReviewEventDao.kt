package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.worddeck.data.local.entity.ReviewEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewEventDao {
    @Insert
    suspend fun insert(reviewEvent: ReviewEventEntity)

    @Query(
        """
        SELECT * FROM review_events
        WHERE cardId = :cardId
          AND userId = :userId
        ORDER BY reviewedAt DESC, id DESC
        """,
    )
    fun observeByUserAndCard(userId: String, cardId: String): Flow<List<ReviewEventEntity>>
}
