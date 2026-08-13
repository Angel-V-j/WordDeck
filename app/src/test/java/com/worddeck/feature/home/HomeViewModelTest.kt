package com.worddeck.feature.home

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
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
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.User
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.AuthenticationRepository
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository
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
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts in loading state before repository result is collected`() = runTest {
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Success(emptyList())),
            ownerId = userId(),
        )

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `uses fake repository from app container and exposes content`() = runTest {
        val decks = listOf(createDeck())
        val repository = FakeDeckRepository(AppResult.Success(decks))
        val appContainer = AppContainer(
            authenticationRepository = UnusedAuthenticationRepository,
            deckRepository = repository,
            flashcardRepository = UnusedFlashcardRepository,
        )
        val viewModel = HomeViewModel(
            deckRepository = appContainer.deckRepository,
            ownerId = userId(),
        )

        advanceUntilIdle()

        assertEquals(userId(), repository.observedOwnerId)
        assertEquals(HomeUiState.Content(decks), viewModel.uiState.value)
    }

    @Test
    fun `exposes empty state when repository returns no decks`() = runTest {
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Success(emptyList())),
            ownerId = userId(),
        )

        advanceUntilIdle()

        assertEquals(HomeUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `exposes error state when repository fails`() = runTest {
        val error = AppError.Unavailable("decks")
        val viewModel = HomeViewModel(
            deckRepository = FakeDeckRepository(AppResult.Failure(error)),
            ownerId = userId(),
        )

        advanceUntilIdle()

        assertEquals(HomeUiState.Error(error), viewModel.uiState.value)
    }
}

private fun createDeck(): Deck = Deck(
    id = DeckId.from("deck-1").successValue(),
    ownerId = userId(),
    title = DeckTitle.from("Spanish basics").successValue(),
    sourceLanguage = DeckLanguage.from("English").successValue(),
    targetLanguage = DeckLanguage.from("Spanish").successValue(),
    category = DeckCategory.from("Vocabulary").successValue(),
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
        email: String,
        password: String,
    ): AppResult<User> = unusedDependency()

    override suspend fun login(email: String, password: String): AppResult<User> = unusedDependency()

    override suspend fun updateDisplayName(displayName: DisplayName): AppResult<User> =
        unusedDependency()

    override suspend fun logout(): AppResult<Unit> = unusedDependency()
}

private object UnusedFlashcardRepository : FlashcardRepository {
    override fun observeByDeck(deckId: DeckId): Flow<AppResult<List<Flashcard>>> = unusedDependency()

    override suspend fun findById(id: CardId): AppResult<Flashcard?> = unusedDependency()

    override suspend fun save(flashcard: Flashcard): AppResult<Unit> = unusedDependency()

    override suspend fun delete(id: CardId): AppResult<Unit> = unusedDependency()
}

private fun unusedDependency(): Nothing = error("This dependency is not used by HomeViewModel")

private class FakeDeckRepository(
    private val observedResult: AppResult<List<Deck>>,
) : DeckRepository {
    var observedOwnerId: UserId? = null
        private set

    override fun observeByOwner(ownerId: UserId): Flow<AppResult<List<Deck>>> {
        observedOwnerId = ownerId
        return flowOf(observedResult)
    }

    override suspend fun findById(id: DeckId): AppResult<Deck?> =
        AppResult.Failure(AppError.Unavailable("Not used by this test"))

    override suspend fun save(deck: Deck): AppResult<Unit> =
        AppResult.Failure(AppError.Unavailable("Not used by this test"))

    override suspend fun delete(id: DeckId): AppResult<Unit> =
        AppResult.Failure(AppError.Unavailable("Not used by this test"))
}
