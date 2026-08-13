package com.worddeck.feature.decks

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
import com.worddeck.domain.repository.FlashcardRepository
import com.worddeck.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `observes cards only for the selected deck`() = runTest {
        val cards = listOf(flashcard())
        val repository = FakeFlashcardRepository(AppResult.Success(cards))
        val viewModel = createViewModel(repository)

        assertEquals(OperationStatus.LOADING, viewModel.uiState.value.listStatus)
        advanceUntilIdle()

        assertEquals(DECK_ID, repository.observedDeckId)
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.listStatus)
        assertEquals(cards, viewModel.uiState.value.cards)
    }

    @Test
    fun `empty repository result exposes an empty successful list`() = runTest {
        val viewModel = createViewModel(
            FakeFlashcardRepository(AppResult.Success(emptyList())),
        )

        advanceUntilIdle()

        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.listStatus)
        assertEquals(emptyList<Flashcard>(), viewModel.uiState.value.cards)
    }

    @Test
    fun `blank front and back are rejected before save`() = runTest {
        val repository = FakeFlashcardRepository()
        val viewModel = createViewModel(repository)

        viewModel.save(null, " ", "hola", "", "")
        assertEquals("must not be blank", viewModel.uiState.value.frontError)

        viewModel.save(null, "hello", " ", "", "")
        assertEquals("must not be blank", viewModel.uiState.value.backError)
        assertNull(repository.savedFlashcard)
    }

    @Test
    fun `create uses fixed id and clock and normalizes values`() = runTest {
        val repository = FakeFlashcardRepository()
        val viewModel = createViewModel(repository)

        viewModel.save(
            existingFlashcard = null,
            front = "  hello  ",
            back = "  hola ",
            exampleSentence = "  Hello, Maria! ",
            additionalInformation = "  Common greeting  ",
        )
        advanceUntilIdle()

        assertEquals(
            flashcard(
                id = "card-1",
                front = "hello",
                back = "hola",
                exampleSentence = "Hello, Maria!",
                additionalInformation = "Common greeting",
                createdAt = NOW,
                updatedAt = NOW,
            ),
            repository.savedFlashcard,
        )
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.operationStatus)
    }

    @Test
    fun `blank optional values are saved as null`() = runTest {
        val repository = FakeFlashcardRepository()
        val viewModel = createViewModel(repository)

        viewModel.save(null, "hello", "hola", " ", "")
        advanceUntilIdle()

        assertNull(repository.savedFlashcard?.exampleSentence)
        assertNull(repository.savedFlashcard?.additionalInformation)
    }

    @Test
    fun `edit preserves card identity deck and creation time`() = runTest {
        val existingFlashcard = flashcard(
            id = "existing-card",
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
        )
        val repository = FakeFlashcardRepository()
        val viewModel = createViewModel(
            repository = repository,
            idGenerator = IdGenerator { error("Editing must not create another ID") },
        )

        viewModel.save(existingFlashcard, "updated", "actualizada", "", "Note")
        advanceUntilIdle()

        assertEquals(
            existingFlashcard.copy(
                front = CardSide.from("updated").successValue(),
                back = CardSide.from("actualizada").successValue(),
                exampleSentence = null,
                additionalInformation = "Note",
                updatedAt = NOW,
            ),
            repository.savedFlashcard,
        )
    }

    @Test
    fun `delete removes selected card`() = runTest {
        val repository = FakeFlashcardRepository()
        val viewModel = createViewModel(repository)
        val cardId = CardId.from("card-1").successValue()

        viewModel.delete(cardId)

        assertEquals(OperationStatus.LOADING, viewModel.uiState.value.operationStatus)
        advanceUntilIdle()
        assertEquals(cardId, repository.deletedCardId)
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.operationStatus)
    }

    private fun createViewModel(
        repository: FakeFlashcardRepository,
        idGenerator: IdGenerator = IdGenerator { "card-1" },
    ) = FlashcardViewModel(
        flashcardRepository = repository,
        deckId = DECK_ID,
        idGenerator = idGenerator,
        clock = Clock { NOW },
    )
}

private val DECK_ID = DeckId.from("deck-1").successValue()
private val NOW = Timestamp(3_000)

private fun flashcard(
    id: String = "card-1",
    front: String = "hello",
    back: String = "hola",
    exampleSentence: String? = "Hello!",
    additionalInformation: String? = "Greeting",
    createdAt: Timestamp = Timestamp(1_000),
    updatedAt: Timestamp = Timestamp(2_000),
) = Flashcard(
    id = CardId.from(id).successValue(),
    deckId = DECK_ID,
    front = CardSide.from(front).successValue(),
    back = CardSide.from(back).successValue(),
    exampleSentence = exampleSentence,
    additionalInformation = additionalInformation,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private class FakeFlashcardRepository(
    initialResult: AppResult<List<Flashcard>> = AppResult.Success(emptyList()),
    private val saveResult: AppResult<Unit> = AppResult.Success(Unit),
    private val deleteResult: AppResult<Unit> = AppResult.Success(Unit),
) : FlashcardRepository {
    private val cards = MutableStateFlow(initialResult)

    var observedDeckId: DeckId? = null
        private set
    var savedFlashcard: Flashcard? = null
        private set
    var deletedCardId: CardId? = null
        private set

    override fun observeByDeck(deckId: DeckId): Flow<AppResult<List<Flashcard>>> {
        observedDeckId = deckId
        return cards
    }

    override suspend fun findById(id: CardId): AppResult<Flashcard?> = AppResult.Success(null)

    override suspend fun save(flashcard: Flashcard): AppResult<Unit> {
        savedFlashcard = flashcard
        return saveResult
    }

    override suspend fun delete(id: CardId): AppResult<Unit> {
        deletedCardId = id
        return deleteResult
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
