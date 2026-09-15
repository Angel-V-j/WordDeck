package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.worddeck.data.local.entity.ReviewStateEntity
import com.worddeck.domain.model.StudyProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewStateDao {
    @Query("DELETE FROM review_states WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    // Deleting a deck cascades into reviews; never erase another local user's progress.
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM review_states
            INNER JOIN flashcards ON flashcards.id = review_states.cardId
            INNER JOIN decks ON decks.id = flashcards.deckId
            WHERE decks.ownerId = :ownerId AND review_states.userId != :ownerId
        )
        """,
    )
    suspend fun hasOtherUsersForOwner(ownerId: String): Boolean

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
        WHERE userId = :userId AND pendingSync = 1
        """,
    )
    suspend fun findPendingByUser(userId: String): List<ReviewStateEntity>

    @Query(
        """
        UPDATE review_states SET pendingSync = 0
        WHERE userId = :userId
          AND cardId = :cardId
          AND updatedAt = :updatedAt
        """,
    )
    suspend fun markSynced(userId: String, cardId: String, updatedAt: Long): Int

    @Query(
        """
        SELECT * FROM review_states
        WHERE userId = :userId
        ORDER BY cardId
        """,
    )
    fun observeByUser(userId: String): Flow<List<ReviewStateEntity>>

    @Query(
        """
        SELECT
          COUNT(CASE WHEN review_states.cardId IS NULL
                           OR review_states.masteryLevel = 'NEW' THEN 1 END) AS newCards,
          COUNT(CASE WHEN review_states.masteryLevel = 'LEARNING' THEN 1 END) AS learningCards,
          COUNT(CASE WHEN review_states.masteryLevel = 'MASTERED' THEN 1 END) AS masteredCards,
          COUNT(CASE WHEN review_states.masteryLevel = 'PROBLEMATIC' THEN 1 END) AS problematicCards,
          COUNT(CASE WHEN review_states.cardId IS NULL
                           OR review_states.nextReviewAt <= :timestamp THEN 1 END) AS dueCards
        FROM flashcards
        INNER JOIN decks ON decks.id = flashcards.deckId
        LEFT JOIN review_states
          ON review_states.cardId = flashcards.id
         AND review_states.userId = :userId
        WHERE decks.ownerId = :userId
          AND decks.deletedAt IS NULL
          AND flashcards.deletedAt IS NULL
        """,
    )
    fun observeProgress(userId: String, timestamp: Long): Flow<StudyProgress>

    @Query(
        """
        SELECT
          COUNT(CASE WHEN review_states.cardId IS NULL
                           OR review_states.masteryLevel = 'NEW' THEN 1 END) AS newCards,
          COUNT(CASE WHEN review_states.masteryLevel = 'LEARNING' THEN 1 END) AS learningCards,
          COUNT(CASE WHEN review_states.masteryLevel = 'MASTERED' THEN 1 END) AS masteredCards,
          COUNT(CASE WHEN review_states.masteryLevel = 'PROBLEMATIC' THEN 1 END) AS problematicCards,
          COUNT(CASE WHEN review_states.cardId IS NULL
                           OR review_states.nextReviewAt <= :timestamp THEN 1 END) AS dueCards
        FROM flashcards
        INNER JOIN decks ON decks.id = flashcards.deckId
        LEFT JOIN review_states
          ON review_states.cardId = flashcards.id
         AND review_states.userId = :userId
        WHERE flashcards.deckId = :deckId
          AND decks.ownerId = :userId
          AND decks.deletedAt IS NULL
          AND flashcards.deletedAt IS NULL
        """,
    )
    fun observeProgressByDeck(
        userId: String,
        deckId: String,
        timestamp: Long,
    ): Flow<StudyProgress>
}
