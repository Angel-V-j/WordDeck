package com.worddeck.feature.statistics

import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.OperationStatus
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.ReviewActivity
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.StudyProgress
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.ReviewRepository
import com.worddeck.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `overall statistics expose repository progress activity user and fixed time`() = runTest {
        val progress = StudyProgress(2, 3, 4, 1, 5)
        val activity = ReviewActivity(2, 4, 9)
        val repository = FakeProgressRepository(progress, activity)
        val viewModel = StatisticsViewModel(repository, USER_ID, Clock { NOW })

        advanceUntilIdle()

        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals(progress, viewModel.uiState.value.progress)
        assertEquals(activity, viewModel.uiState.value.activity)
        assertEquals(USER_ID, repository.observedUserId)
        assertEquals(NOW, repository.observedTimestamp)
    }
}

private class FakeProgressRepository(
    private val progress: StudyProgress,
    private val activity: ReviewActivity,
) : ReviewRepository {
    var observedUserId: UserId? = null
    var observedTimestamp: Timestamp? = null

    override fun observeProgress(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> {
        observedUserId = userId
        observedTimestamp = timestamp
        return flowOf(AppResult.Success(progress))
    }

    override fun observeActivity(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<ReviewActivity>> = flowOf(AppResult.Success(activity))

    override fun observeProgressByDeck(
        userId: UserId,
        deckId: DeckId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> = unused()

    override fun observeStates(userId: UserId): Flow<AppResult<List<ReviewState>>> = unused()
    override fun observeHistory(
        userId: UserId,
        cardId: CardId,
    ): Flow<AppResult<List<ReviewEvent>>> = unused()

    override suspend fun recordReview(
        reviewState: ReviewState,
        reviewEvent: ReviewEvent,
    ): AppResult<Unit> = unused()
}

private val USER_ID = UserId.from("user-1").successValue()
private val NOW = Timestamp(10_000)

private fun unused(): Nothing = error("Not used by StatisticsViewModel")
private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
