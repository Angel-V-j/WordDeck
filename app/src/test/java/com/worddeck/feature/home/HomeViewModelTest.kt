package com.worddeck.feature.home

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.OperationStatus
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckCategory
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.DeckLanguage
import com.worddeck.domain.model.DeckTitle
import com.worddeck.domain.model.DeckVisibility
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `requests the owner scoped flow and exposes its content`() = runTest {
        val decks = listOf(createDeck())
        val repository = FakeDeckRepository(AppResult.Success(decks))
        val viewModel = HomeViewModel(repository, USER_ID)

        advanceUntilIdle()

        assertEquals(USER_ID, repository.observedOwnerId)
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals(decks, viewModel.uiState.value.visibleDecks)
    }

    @Test
    fun `search category and language filters normalize and combine predictably`() = runTest {
        val spanish = createDeck("deck-1", "Basic verbs", "English", "Spanish", "Vocabulary")
        val french = createDeck("deck-2", "Travel phrases", "English", "French", "Travel")
        val viewModel = HomeViewModel(
            FakeDeckRepository(AppResult.Success(listOf(spanish, french))),
            USER_ID,
        )
        advanceUntilIdle()

        viewModel.updateSearchQuery("  PHRASES ")
        viewModel.updateCategoryFilter(" travel ")
        viewModel.updateLanguageFilter(" FRENCH ")
        assertEquals(listOf(french), viewModel.uiState.value.visibleDecks)

        viewModel.updateSearchQuery("   ")
        viewModel.updateCategoryFilter("")
        viewModel.updateLanguageFilter("")
        assertEquals(listOf(spanish, french), viewModel.uiState.value.visibleDecks)
    }
}

private val USER_ID = UserId.from("user-1").successValue()

private fun createDeck(
    id: String = "deck-1",
    title: String = "Spanish basics",
    sourceLanguage: String = "English",
    targetLanguage: String = "Spanish",
    category: String = "Vocabulary",
) = Deck(
    id = DeckId.from(id).successValue(),
    ownerId = USER_ID,
    title = DeckTitle.from(title).successValue(),
    sourceLanguage = DeckLanguage.from(sourceLanguage),
    targetLanguage = DeckLanguage.from(targetLanguage),
    category = DeckCategory.from(category),
    visibility = DeckVisibility.PRIVATE,
    createdAt = Timestamp(1_000),
    updatedAt = Timestamp(1_000),
)

private class FakeDeckRepository(result: AppResult<List<Deck>>) : DeckRepository {
    private val results = MutableStateFlow(result)
    var observedOwnerId: UserId? = null
        private set

    override fun observeByOwner(ownerId: UserId): Flow<AppResult<List<Deck>>> {
        observedOwnerId = ownerId
        return results
    }

    override suspend fun save(deck: Deck): AppResult<Unit> =
        AppResult.Failure(AppError.Unavailable("Not used"))

    override suspend fun delete(id: DeckId): AppResult<Unit> =
        AppResult.Failure(AppError.Unavailable("Not used"))
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
