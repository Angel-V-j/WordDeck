package com.worddeck.feature.statistics

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.OperationStatus
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.DeckId
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
    fun `overall statistics expose populated progress for the current user and time`() = runTest {
        val progress = StudyProgress(2, 3, 4, 1, 5)
        val repository = FakeProgressRepository(overall = AppResult.Success(progress))
        val viewModel = StatisticsViewModel(repository, USER_ID, Clock { NOW })

        advanceUntilIdle()

        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals(progress, viewModel.uiState.value.progress)
        assertEquals(USER_ID, repository.observedUserId)
        assertEquals(NOW, repository.observedTimestamp)
    }

    @Test
    fun `empty progress is represented as successful empty content`() = runTest {
        val viewModel = StatisticsViewModel(
            reviewRepository = FakeProgressRepository(
                overall = AppResult.Success(StudyProgress()),
            ),
            userId = USER_ID,
            clock = Clock { NOW },
        )

        advanceUntilIdle()

        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals(0, viewModel.uiState.value.progress.totalCards)
    }

    @Test
    fun `deck statistics use the selected deck dataset`() = runTest {
        val deckAProgress = StudyProgress(newCards = 1, learningCards = 2, dueCards = 2)
        val deckBProgress = StudyProgress(masteredCards = 3, dueCards = 1)
        val repository = FakeProgressRepository(
            deckResults = mapOf(
                DECK_A to AppResult.Success(deckAProgress),
                DECK_B to AppResult.Success(deckBProgress),
            ),
        )

        val deckAViewModel = StatisticsViewModel(repository, USER_ID, Clock { NOW }, DECK_A)
        advanceUntilIdle()
        assertEquals(deckAProgress, deckAViewModel.uiState.value.progress)

        val deckBViewModel = StatisticsViewModel(repository, USER_ID, Clock { NOW }, DECK_B)
        advanceUntilIdle()
        assertEquals(deckBProgress, deckBViewModel.uiState.value.progress)
        assertEquals(DECK_B, repository.observedDeckId)
    }

    @Test
    fun `repository failure is exposed as an error state`() = runTest {
        val error = AppError.Unavailable("study progress")
        val viewModel = StatisticsViewModel(
            reviewRepository = FakeProgressRepository(overall = AppResult.Failure(error)),
            userId = USER_ID,
            clock = Clock { NOW },
        )

        advanceUntilIdle()

        assertEquals(OperationStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(error, viewModel.uiState.value.error)
    }
}

private class FakeProgressRepository(
    private val overall: AppResult<StudyProgress> = AppResult.Success(StudyProgress()),
    private val deckResults: Map<DeckId, AppResult<StudyProgress>> = emptyMap(),
) : ReviewRepository {
    var observedUserId: UserId? = null
        private set
    var observedDeckId: DeckId? = null
        private set
    var observedTimestamp: Timestamp? = null
        private set

    override fun observeProgress(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> {
        observedUserId = userId
        observedTimestamp = timestamp
        return flowOf(overall)
    }

    override fun observeProgressByDeck(
        userId: UserId,
        deckId: DeckId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> {
        observedUserId = userId
        observedDeckId = deckId
        observedTimestamp = timestamp
        return flowOf(deckResults.getValue(deckId))
    }

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
private val DECK_A = DeckId.from("deck-a").successValue()
private val DECK_B = DeckId.from("deck-b").successValue()
private val NOW = Timestamp(10_000)

private fun unused(): Nothing = error("Not used by StatisticsViewModel")

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
