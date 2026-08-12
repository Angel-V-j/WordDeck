package com.worddeck.feature.home

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.core.AppContainer
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.User
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
    fun `uses fake repository supplied by app container independently of Compose UI`() = runTest {
        val repository = FakeDeckRepository(decks = emptyList())
        val appContainer = AppContainer(
            authenticationRepository = UnusedAuthenticationRepository,
            deckRepository = repository,
            flashcardRepository = UnusedFlashcardRepository,
        )
        val viewModel = HomeViewModel(
            deckRepository = appContainer.deckRepository,
            ownerId = "user-1",
        )

        advanceUntilIdle()

        assertEquals("user-1", repository.observedOwnerId)
        assertEquals(HomeUiState.Content(emptyList()), viewModel.uiState.value)
    }
}

private object UnusedAuthenticationRepository : AuthenticationRepository {
    override fun observeCurrentUser(): Flow<AppResult<User?>> = unusedDependency()

    override suspend fun register(email: String, password: String): AppResult<User> = unusedDependency()

    override suspend fun login(email: String, password: String): AppResult<User> = unusedDependency()

    override suspend fun logout(): AppResult<Unit> = unusedDependency()
}

private object UnusedFlashcardRepository : FlashcardRepository {
    override fun observeByDeck(deckId: String): Flow<AppResult<List<Flashcard>>> = unusedDependency()

    override suspend fun findById(id: String): AppResult<Flashcard?> = unusedDependency()

    override suspend fun save(flashcard: Flashcard): AppResult<Unit> = unusedDependency()

    override suspend fun delete(id: String): AppResult<Unit> = unusedDependency()
}

private fun unusedDependency(): Nothing = error("This dependency is not used by HomeViewModel")

private class FakeDeckRepository(
    private val decks: List<Deck>,
) : DeckRepository {
    var observedOwnerId: String? = null
        private set

    override fun observeByOwner(ownerId: String): Flow<AppResult<List<Deck>>> {
        observedOwnerId = ownerId
        return flowOf(AppResult.Success(decks))
    }

    override suspend fun findById(id: String): AppResult<Deck?> =
        AppResult.Failure(AppError.Unavailable("Not used by this test"))

    override suspend fun save(deck: Deck): AppResult<Unit> =
        AppResult.Failure(AppError.Unavailable("Not used by this test"))

    override suspend fun delete(id: String): AppResult<Unit> =
        AppResult.Failure(AppError.Unavailable("Not used by this test"))
}
