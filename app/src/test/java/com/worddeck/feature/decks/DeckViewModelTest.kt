package com.worddeck.feature.decks

import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `create uses fixed id and clock and normalizes form values`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = createViewModel(repository)

        viewModel.save(
            "  Spanish basics  ",
            "  English ",
            " Spanish  ",
            " Vocabulary ",
        )
        advanceUntilIdle()

        assertEquals(deck("deck-1", NOW, NOW), repository.savedDeck)
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.operationStatus)
    }

    @Test
    fun `blank title is rejected before saving`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = createViewModel(repository)

        viewModel.save("   ", "English", "Spanish", "Vocabulary")

        assertEquals("must not be blank", viewModel.uiState.value.titleError)
        assertNull(repository.savedDeck)
    }

    @Test
    fun `edit preserves identity creation time and visibility`() = runTest {
        val existing = deck(
            "existing-deck",
            Timestamp(1_000),
            Timestamp(2_000),
            DeckVisibility.PUBLIC,
        )
        val repository = FakeDeckRepository()
        val viewModel = createViewModel(
            repository,
            existing,
            IdGenerator { error("Editing must not generate an ID") },
        )

        viewModel.save("Updated deck", "Bulgarian", "Spanish", "Travel")
        advanceUntilIdle()

        assertEquals(
            existing.copy(
                title = DeckTitle.from("Updated deck").successValue(),
                sourceLanguage = DeckLanguage.from("Bulgarian"),
                targetLanguage = DeckLanguage.from("Spanish"),
                category = DeckCategory.from("Travel"),
                updatedAt = NOW,
            ),
            repository.savedDeck,
        )
    }

    @Test
    fun `delete removes the existing deck`() = runTest {
        val existing = deck("existing-deck", Timestamp(1_000), Timestamp(2_000))
        val repository = FakeDeckRepository()
        val viewModel = createViewModel(repository, existing)

        viewModel.delete()
        advanceUntilIdle()

        assertEquals(existing.id, repository.deletedDeckId)
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.operationStatus)
    }

    private fun createViewModel(
        repository: FakeDeckRepository,
        existingDeck: Deck? = null,
        idGenerator: IdGenerator = IdGenerator { "deck-1" },
    ) = DeckViewModel(
        deckRepository = repository,
        ownerId = USER_ID,
        idGenerator = idGenerator,
        clock = Clock { NOW },
        existingDeck = existingDeck,
    )
}

private val NOW = Timestamp(3_000)
private val USER_ID = UserId.from("user-1").successValue()

private fun deck(
    id: String,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    visibility: DeckVisibility = DeckVisibility.PRIVATE,
) = Deck(
    id = DeckId.from(id).successValue(),
    ownerId = USER_ID,
    title = DeckTitle.from("Spanish basics").successValue(),
    sourceLanguage = DeckLanguage.from("English"),
    targetLanguage = DeckLanguage.from("Spanish"),
    category = DeckCategory.from("Vocabulary"),
    visibility = visibility,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private class FakeDeckRepository : DeckRepository {
    var savedDeck: Deck? = null
        private set
    var deletedDeckId: DeckId? = null
        private set

    override fun observeByOwner(ownerId: UserId): Flow<AppResult<List<Deck>>> =
        flowOf(AppResult.Success(emptyList()))

    override suspend fun save(deck: Deck): AppResult<Unit> {
        savedDeck = deck
        return AppResult.Success(Unit)
    }

    override suspend fun delete(id: DeckId): AppResult<Unit> {
        deletedDeckId = id
        return AppResult.Success(Unit)
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
