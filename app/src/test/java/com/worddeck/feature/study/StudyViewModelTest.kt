package com.worddeck.feature.study

import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.CardSide
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.ReviewRating
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.StudyCard
import com.worddeck.domain.model.StudySession
import com.worddeck.domain.model.UserId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyViewModelTest {
    @Test
    fun `reveal and rate advance to the next question and remember the rating`() {
        val first = studyCard("card-1", "hello", "hola")
        val second = studyCard("card-2", "cat", "gato")
        val viewModel = StudyViewModel(StudySession(listOf(first, second)))

        assertEquals(StudyStage.QUESTION, viewModel.uiState.value.stage)
        assertEquals(first, viewModel.uiState.value.currentCard)

        viewModel.revealAnswer()
        assertEquals(StudyStage.ANSWER_REVEALED, viewModel.uiState.value.stage)

        viewModel.rate(ReviewRating.GOOD)

        assertEquals(StudyStage.QUESTION, viewModel.uiState.value.stage)
        assertEquals(second, viewModel.uiState.value.currentCard)
        assertEquals(2, viewModel.uiState.value.position)
        assertEquals(
            ReviewRating.GOOD,
            viewModel.uiState.value.ratings[first.flashcard.id],
        )
    }

    @Test
    fun `rating the last revealed card completes the session`() {
        val card = studyCard("card-1", "hello", "hola")
        val viewModel = StudyViewModel(StudySession(listOf(card)))

        viewModel.revealAnswer()
        viewModel.rate(ReviewRating.AGAIN)

        assertEquals(StudyStage.COMPLETED, viewModel.uiState.value.stage)
        assertNull(viewModel.uiState.value.currentCard)
        assertEquals(ReviewRating.AGAIN, viewModel.uiState.value.ratings[card.flashcard.id])
    }

    @Test
    fun `rating is ignored until the answer is revealed`() {
        val card = studyCard("card-1", "hello", "hola")
        val viewModel = StudyViewModel(StudySession(listOf(card)))

        viewModel.rate(ReviewRating.EASY)

        assertEquals(StudyStage.QUESTION, viewModel.uiState.value.stage)
        assertEquals(card, viewModel.uiState.value.currentCard)
        assertEquals(emptyMap<CardId, ReviewRating>(), viewModel.uiState.value.ratings)
    }

    @Test
    fun `typed answer ignores surrounding whitespace and letter case`() {
        val card = studyCard("card-1", "hello", "hola")
        val viewModel = StudyViewModel(
            session = StudySession(listOf(card)),
            mode = StudyMode.TYPED_ANSWER,
        )

        viewModel.updateTypedAnswer("  HoLa  ")
        viewModel.submitTypedAnswer()

        assertEquals(StudyStage.ANSWER_REVEALED, viewModel.uiState.value.stage)
        assertEquals(TypedAnswerResult.CORRECT, viewModel.uiState.value.typedAnswerResult)
        assertFalse(viewModel.uiState.value.typedAnswerError)
    }

    @Test
    fun `typed answer reports an incorrect answer`() {
        val card = studyCard("card-1", "hello", "hola")
        val viewModel = StudyViewModel(
            session = StudySession(listOf(card)),
            mode = StudyMode.TYPED_ANSWER,
        )

        viewModel.updateTypedAnswer("adios")
        viewModel.submitTypedAnswer()

        assertEquals(TypedAnswerResult.INCORRECT, viewModel.uiState.value.typedAnswerResult)
        assertEquals(StudyStage.ANSWER_REVEALED, viewModel.uiState.value.stage)
    }

    @Test
    fun `empty typed answer stays on the question and shows validation`() {
        val card = studyCard("card-1", "hello", "hola")
        val viewModel = StudyViewModel(
            session = StudySession(listOf(card)),
            mode = StudyMode.TYPED_ANSWER,
        )

        viewModel.updateTypedAnswer("   ")
        viewModel.submitTypedAnswer()

        assertEquals(StudyStage.QUESTION, viewModel.uiState.value.stage)
        assertNull(viewModel.uiState.value.typedAnswerResult)
        assertTrue(viewModel.uiState.value.typedAnswerError)
    }

    @Test
    fun `typed answer can be rated only after its result is shown`() {
        val first = studyCard("card-1", "hello", "hola")
        val second = studyCard("card-2", "cat", "gato")
        val viewModel = StudyViewModel(
            session = StudySession(listOf(first, second)),
            mode = StudyMode.TYPED_ANSWER,
        )

        viewModel.rate(ReviewRating.GOOD)
        assertEquals(first, viewModel.uiState.value.currentCard)

        viewModel.updateTypedAnswer("hola")
        viewModel.submitTypedAnswer()
        viewModel.rate(ReviewRating.HARD)

        assertEquals(second, viewModel.uiState.value.currentCard)
        assertEquals(ReviewRating.HARD, viewModel.uiState.value.ratings[first.flashcard.id])
        assertEquals("", viewModel.uiState.value.typedAnswer)
        assertNull(viewModel.uiState.value.typedAnswerResult)
        assertFalse(viewModel.uiState.value.typedAnswerError)
    }
}

private val USER_ID = UserId.from("user-1").successValue()
private val DECK_ID = DeckId.from("deck-1").successValue()
private val NOW = Timestamp(10_000)

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
