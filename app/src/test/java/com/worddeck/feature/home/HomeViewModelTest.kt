package com.worddeck.feature.home

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.OperationStatus
import com.worddeck.common.Timestamp
import com.worddeck.core.AppContainer
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckCategory
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.DeckLanguage
import com.worddeck.domain.model.DeckTitle
import com.worddeck.domain.model.DeckVisibility
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewActivity
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.StudyProgress
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.EmailAddress
import com.worddeck.domain.model.User
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.AuthenticationRepository
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository
import com.worddeck.domain.repository.ReviewRepository
import com.worddeck.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts in loading state before repository result is collected`() = runTest {
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Success(emptyList())),
            ownerId = userId(),
        )

        assertEquals(OperationStatus.LOADING, viewModel.uiState.value.status)
    }

    @Test
    fun `requests the owner scoped flow and exposes its content`() = runTest {
        val decks = listOf(createDeck())
        val repository = FakeDeckRepository(AppResult.Success(decks))
        val appContainer = AppContainer(
            authenticationRepository = UnusedAuthenticationRepository,
            deckRepository = repository,
            flashcardRepository = UnusedFlashcardRepository,
            reviewRepository = UnusedReviewRepository,
        )
        val viewModel = HomeViewModel(
            deckRepository = appContainer.deckRepository,
            ownerId = userId(),
        )

        advanceUntilIdle()

        assertEquals(userId(), repository.observedOwnerId)
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals(decks, viewModel.uiState.value.decks)
    }

    @Test
    fun `exposes empty state when repository returns no decks`() = runTest {
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Success(emptyList())),
            ownerId = userId(),
        )

        advanceUntilIdle()

        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals(emptyList<Deck>(), viewModel.uiState.value.decks)
    }

    @Test
    fun `exposes error state when repository fails`() = runTest {
        val error = AppError.Unavailable("decks")
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Failure(error)),
            ownerId = userId(),
        )

        advanceUntilIdle()

        assertEquals(OperationStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(error, viewModel.uiState.value.error)
    }

    @Test
    fun `search ignores case and surrounding whitespace for every deck field`() = runTest {
        val firstDeck = createDeck(
            id = "deck-1",
            title = "Everyday verbs",
            sourceLanguage = "English",
            targetLanguage = "Spanish",
            category = "Basics",
        )
        val secondDeck = createDeck(
            id = "deck-2",
            title = "Travel phrases",
            sourceLanguage = "Bulgarian",
            targetLanguage = "French",
            category = "Tourism",
        )
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Success(listOf(firstDeck, secondDeck))),
            ownerId = userId(),
        )
        advanceUntilIdle()

        viewModel.updateSearchQuery("  VERBS  ")
        assertEquals(listOf(firstDeck), viewModel.uiState.value.visibleDecks)

        viewModel.updateSearchQuery(" english ")
        assertEquals(listOf(firstDeck), viewModel.uiState.value.visibleDecks)

        viewModel.updateSearchQuery("SPANISH")
        assertEquals(listOf(firstDeck), viewModel.uiState.value.visibleDecks)

        viewModel.updateSearchQuery(" basics ")
        assertEquals(listOf(firstDeck), viewModel.uiState.value.visibleDecks)

        viewModel.updateSearchQuery("french")
        assertEquals(listOf(secondDeck), viewModel.uiState.value.visibleDecks)
    }

    @Test
    fun `category and language filters work separately and together`() = runTest {
        val spanishBasics = createDeck(
            id = "deck-1",
            title = "Basic words",
            sourceLanguage = "English",
            targetLanguage = "Spanish",
            category = "Vocabulary",
        )
        val frenchTravel = createDeck(
            id = "deck-2",
            title = "Travel phrases",
            sourceLanguage = "English",
            targetLanguage = "French",
            category = "Travel",
        )
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Success(listOf(spanishBasics, frenchTravel))),
            ownerId = userId(),
        )
        advanceUntilIdle()

        viewModel.updateCategoryFilter("  TRAVEL ")
        assertEquals(listOf(frenchTravel), viewModel.uiState.value.visibleDecks)

        viewModel.updateCategoryFilter("")
        viewModel.updateLanguageFilter(" spanish ")
        assertEquals(listOf(spanishBasics), viewModel.uiState.value.visibleDecks)

        viewModel.updateSearchQuery("phrases")
        viewModel.updateCategoryFilter("travel")
        viewModel.updateLanguageFilter("french")
        assertEquals(listOf(frenchTravel), viewModel.uiState.value.visibleDecks)

        viewModel.updateLanguageFilter("Spanish")
        assertEquals(emptyList<Deck>(), viewModel.uiState.value.visibleDecks)
    }

    @Test
    fun `blank search query returns the complete current deck list`() = runTest {
        val decks = listOf(
            createDeck(id = "deck-1", title = "Spanish basics"),
            createDeck(
                id = "deck-2",
                title = "French basics",
                targetLanguage = "French",
            ),
        )
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Success(decks)),
            ownerId = userId(),
        )
        advanceUntilIdle()

        viewModel.updateSearchQuery("Spanish")
        assertEquals(1, viewModel.uiState.value.visibleDecks.size)

        viewModel.updateSearchQuery("   ")
        assertEquals(decks, viewModel.uiState.value.visibleDecks)
    }

    @Test
    fun `repository and filter changes publish consistent deck lists`() = runTest {
        val spanishDeck = createDeck(id = "deck-1", title = "Spanish basics")
        val travelDeck = createDeck(id = "deck-2", title = "Travel phrases")
        val repository = FakeDeckRepository(AppResult.Success(listOf(spanishDeck)))
        val viewModel = HomeViewModel(repository, userId())
        advanceUntilIdle()

        val states = mutableListOf<HomeUiState>()
        val collectionJob: Job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect(states::add)
        }

        states.clear()
        repository.emit(AppResult.Success(listOf(spanishDeck, travelDeck)))
        advanceUntilIdle()

        assertEquals(1, states.size)
        states.forEach { state ->
            assertEquals(listOf(spanishDeck, travelDeck), state.visibleDecks)
        }

        states.clear()
        viewModel.updateSearchQuery("travel")
        assertEquals(1, states.size)
        states.forEach { state ->
            assertEquals("travel", state.searchQuery)
            assertEquals(listOf(travelDeck), state.visibleDecks)
        }
        collectionJob.cancel()
    }
}

private fun createDeck(
    id: String = "deck-1",
    title: String = "Spanish basics",
    sourceLanguage: String = "English",
    targetLanguage: String = "Spanish",
    category: String = "Vocabulary",
): Deck = Deck(
    id = DeckId.from(id).successValue(),
    ownerId = userId(),
    title = DeckTitle.from(title).successValue(),
    sourceLanguage = DeckLanguage.from(sourceLanguage),
    targetLanguage = DeckLanguage.from(targetLanguage),
    category = DeckCategory.from(category),
    visibility = DeckVisibility.PRIVATE,
    createdAt = Timestamp(1_000),
    updatedAt = Timestamp(1_000),
)

private fun userId(): UserId = UserId.from("user-1").successValue()

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value

private object UnusedAuthenticationRepository : AuthenticationRepository {
    override fun observeCurrentUser(): Flow<AppResult<User?>> = unusedDependency()

    override suspend fun register(
        displayName: DisplayName,
        email: EmailAddress,
        password: String,
    ): AppResult<User> = unusedDependency()

    override suspend fun login(email: EmailAddress, password: String): AppResult<User> =
        unusedDependency()

    override suspend fun updateDisplayName(displayName: DisplayName): AppResult<User> =
        unusedDependency()

    override suspend fun logout(): AppResult<Unit> = unusedDependency()
}

private object UnusedFlashcardRepository : FlashcardRepository {
    override fun observeByDeck(deckId: DeckId): Flow<AppResult<List<Flashcard>>> = unusedDependency()

    override suspend fun save(flashcard: Flashcard): AppResult<Unit> = unusedDependency()

    override suspend fun delete(id: CardId): AppResult<Unit> = unusedDependency()
}

private object UnusedReviewRepository : ReviewRepository {
    override fun observeStates(userId: UserId): Flow<AppResult<List<ReviewState>>> =
        unusedDependency()

    override fun observeHistory(
        userId: UserId,
        cardId: CardId,
    ): Flow<AppResult<List<ReviewEvent>>> = unusedDependency()

    override fun observeProgress(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> = unusedDependency()

    override fun observeProgressByDeck(
        userId: UserId,
        deckId: DeckId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> = unusedDependency()

    override fun observeActivity(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<ReviewActivity>> = unusedDependency()

    override suspend fun recordReview(
        reviewState: ReviewState,
        reviewEvent: ReviewEvent,
    ): AppResult<Unit> = unusedDependency()
}

private fun unusedDependency(): Nothing = error("This dependency is not used by HomeViewModel")

private class FakeDeckRepository(
    observedResult: AppResult<List<Deck>>,
) : DeckRepository {
    private val results = MutableStateFlow(observedResult)

    var observedOwnerId: UserId? = null
        private set

    override fun observeByOwner(ownerId: UserId): Flow<AppResult<List<Deck>>> {
        observedOwnerId = ownerId
        return results
    }

    fun emit(result: AppResult<List<Deck>>) {
        results.value = result
    }

    override suspend fun save(deck: Deck): AppResult<Unit> =
        AppResult.Failure(AppError.Unavailable("Not used by this test"))

    override suspend fun delete(id: DeckId): AppResult<Unit> =
        AppResult.Failure(AppError.Unavailable("Not used by this test"))
}
