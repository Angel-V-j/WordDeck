package com.worddeck.feature.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.CardSide
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlashcardUiState(
    val listStatus: OperationStatus = OperationStatus.LOADING,
    val cards: List<Flashcard> = emptyList(),
    val operationStatus: OperationStatus = OperationStatus.IDLE,
    val frontError: String? = null,
    val backError: String? = null,
    val error: AppError? = null,
)

class FlashcardViewModel(
    private val flashcardRepository: FlashcardRepository,
    private val deckId: DeckId,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            flashcardRepository.observeByDeck(deckId).collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Success -> _uiState.value.copy(
                        listStatus = OperationStatus.SUCCESS,
                        cards = result.value,
                        error = null,
                    )
                    is AppResult.Failure -> _uiState.value.copy(
                        listStatus = OperationStatus.ERROR,
                        error = result.error,
                    )
                }
            }
        }
    }

    fun save(
        existingFlashcard: Flashcard?,
        front: String,
        back: String,
        exampleSentence: String,
        additionalInformation: String,
    ) {
        if (_uiState.value.operationStatus == OperationStatus.LOADING) return

        val frontResult = CardSide.from(front)
        val validFront = when (frontResult) {
            is AppResult.Success -> frontResult.value
            is AppResult.Failure -> {
                showFrontError(frontResult.error)
                return
            }
        }

        val backResult = CardSide.from(back)
        val validBack = when (backResult) {
            is AppResult.Success -> backResult.value
            is AppResult.Failure -> {
                showBackError(backResult.error)
                return
            }
        }

        val now = clock.now()
        val flashcard = if (existingFlashcard == null) {
            val idResult = CardId.from(idGenerator.generate())
            val cardId = when (idResult) {
                is AppResult.Success -> idResult.value
                is AppResult.Failure -> {
                    showOperationError(idResult.error)
                    return
                }
            }
            Flashcard(
                id = cardId,
                deckId = deckId,
                front = validFront,
                back = validBack,
                exampleSentence = exampleSentence.normalizedOptional(),
                additionalInformation = additionalInformation.normalizedOptional(),
                createdAt = now,
                updatedAt = now,
            )
        } else {
            existingFlashcard.copy(
                front = validFront,
                back = validBack,
                exampleSentence = exampleSentence.normalizedOptional(),
                additionalInformation = additionalInformation.normalizedOptional(),
                updatedAt = now,
            )
        }

        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.LOADING,
            frontError = null,
            backError = null,
            error = null,
        )
        viewModelScope.launch {
            when (val result = flashcardRepository.save(flashcard)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    operationStatus = OperationStatus.SUCCESS,
                )
                is AppResult.Failure -> showOperationError(result.error)
            }
        }
    }

    fun delete(cardId: CardId) {
        if (_uiState.value.operationStatus == OperationStatus.LOADING) return

        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.LOADING,
            error = null,
        )
        viewModelScope.launch {
            when (val result = flashcardRepository.delete(cardId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    operationStatus = OperationStatus.SUCCESS,
                )
                is AppResult.Failure -> showOperationError(result.error)
            }
        }
    }

    fun clearOperation() {
        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.IDLE,
            frontError = null,
            backError = null,
            error = null,
        )
    }

    private fun showFrontError(error: AppError) {
        val validationError = error as? AppError.Validation
        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.ERROR,
            frontError = validationError?.reason,
            backError = null,
            error = if (validationError == null) error else null,
        )
    }

    private fun showBackError(error: AppError) {
        val validationError = error as? AppError.Validation
        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.ERROR,
            frontError = null,
            backError = validationError?.reason,
            error = if (validationError == null) error else null,
        )
    }

    private fun showOperationError(error: AppError) {
        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.ERROR,
            error = error,
        )
    }
}

private fun String.normalizedOptional(): String? = trim().ifEmpty { null }