package com.worddeck.feature.decks

import com.worddeck.common.AppError
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `create uses fixed id and clock and normalizes form values`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = createViewModel(
            repository = repository,
            idGenerator = IdGenerator { "deck-1" },
        )

        viewModel.save(
            title = "  Spanish basics  ",
            sourceLanguage = "  English ",
            targetLanguage = " Spanish  ",
            category = " Vocabulary ",
        )
        advanceUntilIdle()

        assertEquals(
            deck(id = "deck-1", createdAt = NOW, updatedAt = NOW),
            repository.savedDeck,
        )
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.operationStatus)
    }

    @Test
    fun `blank title shows validation and does not save`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = createViewModel(repository)

        viewModel.save(
            title = "   ",
            sourceLanguage = "English",
            targetLanguage = "Spanish",
            category = "Vocabulary",
        )

        assertEquals("must not be blank", viewModel.uiState.value.titleError)
        assertNull(repository.savedDeck)
        assertEquals(OperationStatus.ERROR, viewModel.uiState.value.operationStatus)
    }

    @Test
    fun `blank optional values are saved as null`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = createViewModel(repository)

        viewModel.save(
            title = "Spanish basics",
            sourceLanguage = " ",
            targetLanguage = "",
            category = "   ",
        )
        advanceUntilIdle()

        assertNull(repository.savedDeck?.sourceLanguage)
        assertNull(repository.savedDeck?.targetLanguage)
        assertNull(repository.savedDeck?.category)
    }

    @Test
    fun `edit preserves stable fields and does not generate another id`() = runTest {
        val repository = FakeDeckRepository()
        val existingDeck = deck(
            id = "existing-deck",
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
            visibility = DeckVisibility.PUBLIC,
        )
        val viewModel = createViewModel(
            repository = repository,
            existingDeck = existingDeck,
            idGenerator = IdGenerator { error("Editing must not create another ID") },
        )

        assertEquals("Spanish basics", viewModel.uiState.value.title)
        viewModel.save(
            title = "Updated deck",
            sourceLanguage = "Bulgarian",
            targetLanguage = "Spanish",
            category = "Travel",
        )
        advanceUntilIdle()

        assertEquals(
            existingDeck.copy(
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
    fun `repository failure is exposed to the screen`() = runTest {
        val error = AppError.Unavailable("deck")
        val viewModel = createViewModel(FakeDeckRepository(AppResult.Failure(error)))

        viewModel.save("Spanish basics", "", "", "")
        advanceUntilIdle()

        assertEquals(error, viewModel.uiState.value.error)
        assertEquals(OperationStatus.ERROR, viewModel.uiState.value.operationStatus)
    }

    @Test
    fun `delete removes the existing deck`() = runTest {
        val existingDeck = deck(
            id = "existing-deck",
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
        )
        val repository = FakeDeckRepository()
        val viewModel = createViewModel(repository, existingDeck = existingDeck)

        viewModel.delete()

        assertEquals(OperationStatus.LOADING, viewModel.uiState.value.operationStatus)
        advanceUntilIdle()
        assertEquals(existingDeck.id, repository.deletedDeckId)
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.operationStatus)
    }

    @Test
    fun `delete failure is exposed to the screen`() = runTest {
        val error = AppError.Unavailable("deck")
        val existingDeck = deck(
            id = "existing-deck",
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
        )
        val repository = FakeDeckRepository(
            deleteResult = AppResult.Failure(error),
        )
        val viewModel = createViewModel(repository, existingDeck = existingDeck)

        viewModel.delete()
        advanceUntilIdle()

        assertEquals(OperationStatus.ERROR, viewModel.uiState.value.operationStatus)
        assertEquals(error, viewModel.uiState.value.error)
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

private class FakeDeckRepository(
    private val saveResult: AppResult<Unit> = AppResult.Success(Unit),
    private val deleteResult: AppResult<Unit> = AppResult.Success(Unit),
) : DeckRepository {
    var savedDeck: Deck? = null
        private set
    var deletedDeckId: DeckId? = null
        private set

    override fun observeByOwner(ownerId: UserId): Flow<AppResult<List<Deck>>> =
        flowOf(AppResult.Success(emptyList()))

    override suspend fun save(deck: Deck): AppResult<Unit> {
        savedDeck = deck
        return saveResult
    }

    override suspend fun delete(id: DeckId): AppResult<Unit> {
        deletedDeckId = id
        return deleteResult
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
