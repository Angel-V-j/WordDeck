package com.worddeck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.worddeck.data.local.entity.ReviewStateEntity
import com.worddeck.domain.model.StudyProgress
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
        LEFT JOIN review_states
          ON review_states.cardId = flashcards.id
         AND review_states.userId = :userId
        WHERE flashcards.deckId = :deckId
        """,
    )
    fun observeProgressByDeck(
        userId: String,
        deckId: String,
        timestamp: Long,
    ): Flow<StudyProgress>
}
