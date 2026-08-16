package com.worddeck.domain.repository

import com.worddeck.common.AppResult
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.UserId
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeStates(userId: UserId): Flow<AppResult<List<ReviewState>>>

    fun observeHistory(
        userId: UserId,
        cardId: CardId,
    ): Flow<AppResult<List<ReviewEvent>>>

    /** Saves the updated state and its history event as one operation. */
    suspend fun recordReview(
        reviewState: ReviewState,
        reviewEvent: ReviewEvent,
    ): AppResult<Unit>
}
