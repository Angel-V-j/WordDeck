package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.worddeck.data.local.entity.ReviewEventEntity
import com.worddeck.domain.model.ReviewActivity
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

    @Query(
        """
        SELECT
          COUNT(CASE WHEN reviewedAt BETWEEN :sevenDaysAgo AND :timestamp THEN 1 END)
            AS last7DaysReviews,
          COUNT(CASE WHEN reviewedAt BETWEEN :thirtyDaysAgo AND :timestamp THEN 1 END)
            AS last30DaysReviews,
          COUNT(CASE WHEN reviewedAt <= :timestamp THEN 1 END) AS allTimeReviews
        FROM review_events
        WHERE userId = :userId
        """,
    )
    fun observeActivity(
        userId: String,
        sevenDaysAgo: Long,
        thirtyDaysAgo: Long,
        timestamp: Long,
    ): Flow<ReviewActivity>
}
