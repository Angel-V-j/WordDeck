package com.worddeck.feature.study

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewActivity
import com.worddeck.domain.model.ReviewRating
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.Sm2Rules
import com.worddeck.domain.model.StudyProgress
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewFlashcardUseCaseTest {
    @Test
    fun `successful review uses fixed time and records the new state and one event`() = runTest {
        val repository = FakeReviewRepository()
        val useCase = ReviewFlashcardUseCase(
            reviewRepository = repository,
            clock = Clock { REVIEWED_AT },
            idGenerator = IdGenerator { "review-1" },
        )

        val result = useCase(USER_ID, initialState(), ReviewRating.GOOD)

        val updatedState = (result as AppResult.Success).value
        assertEquals(1, updatedState.repetition)
        assertEquals(1, updatedState.intervalDays)
        assertEquals(REVIEWED_AT, updatedState.lastReviewedAt)
        assertEquals(Timestamp(REVIEWED_AT.epochMilliseconds + DAY), updatedState.nextReviewAt)
        assertEquals(updatedState, repository.recordedState)
        assertEquals("review-1", repository.recordedEvent?.id?.value)
        assertEquals(USER_ID, repository.recordedEvent?.userId)
        assertEquals(CARD_ID, repository.recordedEvent?.cardId)
        assertEquals(ReviewRating.GOOD.quality, repository.recordedEvent?.quality)
        assertEquals(REVIEWED_AT, repository.recordedEvent?.reviewedAt)
        assertEquals(1, repository.recordCalls)
    }

    @Test
    fun `repository failure is returned without pretending that the review succeeded`() = runTest {
        val expectedError = AppError.Unavailable("review")
        val repository = FakeReviewRepository(AppResult.Failure(expectedError))
        val useCase = ReviewFlashcardUseCase(
            reviewRepository = repository,
            clock = Clock { REVIEWED_AT },
            idGenerator = IdGenerator { "review-1" },
        )

        val result = useCase(USER_ID, initialState(), ReviewRating.AGAIN)

        assertEquals(AppResult.Failure(expectedError), result)
        assertEquals(1, repository.recordCalls)
    }

}

private class FakeReviewRepository(
    private val result: AppResult<Unit> = AppResult.Success(Unit),
) : ReviewRepository {
    var recordedState: ReviewState? = null
        private set
    var recordedEvent: ReviewEvent? = null
        private set
    var recordCalls: Int = 0
        private set

    override fun observeStates(userId: UserId): Flow<AppResult<List<ReviewState>>> =
        flowOf(AppResult.Success(emptyList()))

    override fun observeHistory(
        userId: UserId,
        cardId: CardId,
    ): Flow<AppResult<List<ReviewEvent>>> = flowOf(AppResult.Success(emptyList()))

    override fun observeProgress(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> = flowOf(AppResult.Success(StudyProgress()))

    override fun observeProgressByDeck(
        userId: UserId,
        deckId: DeckId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> = flowOf(AppResult.Success(StudyProgress()))

    override fun observeActivity(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<ReviewActivity>> = flowOf(AppResult.Success(ReviewActivity()))

    override suspend fun recordReview(
        reviewState: ReviewState,
        reviewEvent: ReviewEvent,
    ): AppResult<Unit> {
        recordCalls += 1
        recordedState = reviewState
        recordedEvent = reviewEvent
        return result
    }
}

private val USER_ID = UserId.from("user-1").successValue()
private val CARD_ID = CardId.from("card-1").successValue()
private val REVIEWED_AT = Timestamp(1_755_000_000_000)
private const val DAY = 86_400_000L

private fun initialState(): ReviewState = ReviewState.initial(
    userId = USER_ID,
    cardId = CARD_ID,
    dueAt = REVIEWED_AT,
)

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
