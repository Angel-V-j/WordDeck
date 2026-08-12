package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.worddeck.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewStateDao {
    @Upsert
    suspend fun save(reviewState: ReviewStateEntity)

    @Query(
        """
        SELECT * FROM review_states
        WHERE userId = :userId
          AND cardId = :cardId
        LIMIT 1
        """,
    )
    suspend fun findByUserAndCard(userId: String, cardId: String): ReviewStateEntity?

    @Query(
        """
        SELECT * FROM review_states
        WHERE userId = :userId
        ORDER BY cardId
        """,
    )
    fun observeByUser(userId: String): Flow<List<ReviewStateEntity>>
}
