package com.worddeck.feature.decks

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

        advanceUntilIdle()

        assertEquals(DECK_ID, repository.observedDeckId)
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.listStatus)
        assertEquals(cards, viewModel.uiState.value.cards)
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
    fun `create uses fixed id and clock and normalizes all text`() = runTest {
        val repository = FakeFlashcardRepository()
        val viewModel = createViewModel(repository)

        viewModel.save(
            null,
            "  hello  ",
            "  hola ",
            "  Hello, Maria! ",
            "  Common greeting  ",
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
    }

    @Test
    fun `edit preserves stable fields and delete removes the selected card`() = runTest {
        val existing = flashcard(
            id = "existing-card",
            createdAt = Timestamp(1_000),
            updatedAt = Timestamp(2_000),
        )
        val repository = FakeFlashcardRepository()
        val viewModel = createViewModel(
            repository,
            IdGenerator { error("Editing must not generate an ID") },
        )

        viewModel.save(existing, "updated", "actualizada", "", "Note")
        advanceUntilIdle()
        assertEquals(
            existing.copy(
                front = CardSide.from("updated").successValue(),
                back = CardSide.from("actualizada").successValue(),
                exampleSentence = null,
                additionalInformation = "Note",
                updatedAt = NOW,
            ),
            repository.savedFlashcard,
        )

        viewModel.delete(existing.id)
        advanceUntilIdle()
        assertEquals(existing.id, repository.deletedCardId)
    }

    @Test
    fun `search normalizes text checks every field and blank query restores all cards`() = runTest {
        val greeting = flashcard(
            "card-1",
            "hello",
            "hola",
            "Friendly greeting",
            "Common phrase",
        )
        val animal = flashcard(
            "card-2",
            "cat",
            "gato",
            "The animal sleeps",
            "Noun",
        )
        val viewModel = createViewModel(
            FakeFlashcardRepository(AppResult.Success(listOf(greeting, animal))),
        )
        advanceUntilIdle()

        viewModel.updateSearchQuery("  HELLO  ")
        assertEquals(listOf(greeting), viewModel.uiState.value.cards)
        viewModel.updateSearchQuery(" gato ")
        assertEquals(listOf(animal), viewModel.uiState.value.cards)
        viewModel.updateSearchQuery("ANIMAL")
        assertEquals(listOf(animal), viewModel.uiState.value.cards)
        viewModel.updateSearchQuery(" common ")
        assertEquals(listOf(greeting), viewModel.uiState.value.cards)
        viewModel.updateSearchQuery("   ")
        assertEquals(listOf(greeting, animal), viewModel.uiState.value.cards)
    }

    private fun createViewModel(
        repository: FakeFlashcardRepository,
        idGenerator: IdGenerator = IdGenerator { "card-1" },
    ) = FlashcardViewModel(repository, DECK_ID, idGenerator, Clock { NOW })
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

    override suspend fun save(flashcard: Flashcard): AppResult<Unit> {
        savedFlashcard = flashcard
        return AppResult.Success(Unit)
    }

    override suspend fun delete(id: CardId): AppResult<Unit> {
        deletedCardId = id
        return AppResult.Success(Unit)
    }
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
