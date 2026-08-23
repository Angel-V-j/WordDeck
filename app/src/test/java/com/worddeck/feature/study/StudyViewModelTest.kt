package com.worddeck.feature.study

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.common.OperationStatus
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.CardSide
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.ReviewActivity
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewRating
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.StudyCard
import com.worddeck.domain.model.StudyProgress
import com.worddeck.domain.model.StudySession
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.ReviewRepository
import com.worddeck.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `flashcard reviews advance and finish with a rating summary`() = runTest {
        val first = studyCard("card-1", "hello", "hola")
        val second = studyCard("card-2", "cat", "gato")
        val repository = RecordingReviewRepository()
        val viewModel = createViewModel(StudySession(listOf(first, second)), repository = repository)

        viewModel.revealAnswer()
        viewModel.rate(ReviewRating.GOOD)
        advanceUntilIdle()
        assertEquals(second, viewModel.uiState.value.currentCard)
        assertEquals(2, viewModel.uiState.value.position)

        viewModel.revealAnswer()
        viewModel.rate(ReviewRating.AGAIN)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StudyStage.COMPLETED, state.stage)
        assertNull(state.currentCard)
        assertEquals(1, state.ratings.values.count { it == ReviewRating.GOOD })
        assertEquals(1, state.ratings.values.count { it == ReviewRating.AGAIN })
        assertEquals(2, repository.recordCalls)
    }

    @Test
    fun `typed answers normalize input and count correct and incorrect results`() = runTest {
        val viewModel = createViewModel(
            StudySession(
                listOf(
                    studyCard("card-1", "hello", "hola"),
                    studyCard("card-2", "cat", "gato"),
                ),
            ),
            mode = StudyMode.TYPED_ANSWER,
        )

        viewModel.updateTypedAnswer("  HoLa  ")
        viewModel.submitTypedAnswer()
        assertEquals(TypedAnswerResult.CORRECT, viewModel.uiState.value.typedAnswerResult)
        viewModel.rate(ReviewRating.EASY)
        advanceUntilIdle()

        viewModel.updateTypedAnswer("perro")
        viewModel.submitTypedAnswer()
        assertEquals(TypedAnswerResult.INCORRECT, viewModel.uiState.value.typedAnswerResult)
        viewModel.rate(ReviewRating.HARD)
        advanceUntilIdle()

        assertEquals(StudyStage.COMPLETED, viewModel.uiState.value.stage)
        assertEquals(1, viewModel.uiState.value.correctAnswers)
        assertEquals(1, viewModel.uiState.value.incorrectAnswers)
    }

    @Test
    fun `empty typed answer stays on the question and shows validation`() {
        val viewModel = createViewModel(
            StudySession(listOf(studyCard("card-1", "hello", "hola"))),
            mode = StudyMode.TYPED_ANSWER,
        )

        viewModel.updateTypedAnswer("   ")
        viewModel.submitTypedAnswer()

        assertEquals(StudyStage.QUESTION, viewModel.uiState.value.stage)
        assertNull(viewModel.uiState.value.typedAnswerResult)
        assertTrue(viewModel.uiState.value.typedAnswerError)
    }

    @Test
    fun `failed review remains on the card and exposes the error`() = runTest {
        val card = studyCard("card-1", "hello", "hola")
        val error = AppError.Unavailable("review")
        val viewModel = createViewModel(
            StudySession(listOf(card)),
            repository = RecordingReviewRepository(AppResult.Failure(error)),
        )

        viewModel.revealAnswer()
        viewModel.rate(ReviewRating.GOOD)
        advanceUntilIdle()

        assertEquals(card, viewModel.uiState.value.currentCard)
        assertEquals(StudyStage.ANSWER_REVEALED, viewModel.uiState.value.stage)
        assertEquals(OperationStatus.ERROR, viewModel.uiState.value.reviewStatus)
        assertEquals(error, viewModel.uiState.value.error)
    }
}

private val USER_ID = UserId.from("user-1").successValue()
private val DECK_ID = DeckId.from("deck-1").successValue()
private val NOW = Timestamp(10_000)

private fun createViewModel(
    session: StudySession,
    mode: StudyMode = StudyMode.FLASHCARD,
    repository: ReviewRepository = RecordingReviewRepository(),
) = StudyViewModel(
    session = session,
    currentUserId = USER_ID,
    reviewFlashcard = ReviewFlashcardUseCase(
        reviewRepository = repository,
        clock = Clock { NOW },
        idGenerator = IdGenerator { "review-1" },
    ),
    mode = mode,
)

private class RecordingReviewRepository(
    private val result: AppResult<Unit> = AppResult.Success(Unit),
) : ReviewRepository {
    var recordCalls = 0
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
        return result
    }
}

private fun studyCard(id: String, front: String, back: String): StudyCard {
    val flashcard = Flashcard(
        id = CardId.from(id).successValue(),
        deckId = DECK_ID,
        front = CardSide.from(front).successValue(),
        back = CardSide.from(back).successValue(),
        exampleSentence = null,
        additionalInformation = null,
        createdAt = NOW,
        updatedAt = NOW,
    )
    return StudyCard(
        flashcard = flashcard,
        reviewState = ReviewState.initial(USER_ID, flashcard.id, NOW),
    )
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
