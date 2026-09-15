package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.worddeck.data.local.entity.ReviewEventEntity
import com.worddeck.domain.model.ReviewActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewEventDao {
    @Query("DELETE FROM review_events WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    // Deleting a deck cascades into reviews; never erase another local user's progress.
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM review_events
            INNER JOIN flashcards ON flashcards.id = review_events.cardId
            INNER JOIN decks ON decks.id = flashcards.deckId
            WHERE decks.ownerId = :ownerId AND review_events.userId != :ownerId
        )
        """,
    )
    suspend fun hasOtherUsersForOwner(ownerId: String): Boolean

    @Insert
    suspend fun insert(reviewEvent: ReviewEventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(reviewEvent: ReviewEventEntity): Long

    @Query("SELECT * FROM review_events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ReviewEventEntity?

    @Query("SELECT * FROM review_events WHERE userId = :userId AND pendingSync = 1")
    suspend fun findPendingByUser(userId: String): List<ReviewEventEntity>

    @Query("UPDATE review_events SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String): Int

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
