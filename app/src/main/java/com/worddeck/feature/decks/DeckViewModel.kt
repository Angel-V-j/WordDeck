package com.worddeck.feature.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckCategory
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.DeckLanguage
import com.worddeck.domain.model.DeckTitle
import com.worddeck.domain.model.DeckVisibility
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeckUiState(
    val title: String = "",
    val sourceLanguage: String = "",
    val targetLanguage: String = "",
    val category: String = "",
    val isEditing: Boolean = false,
    val operationStatus: OperationStatus = OperationStatus.IDLE,
    val titleError: String? = null,
    val error: AppError? = null,
)

class DeckViewModel(
    private val deckRepository: DeckRepository,
    private val ownerId: UserId,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val existingDeck: Deck? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(existingDeck.toUiState())
    val uiState: StateFlow<DeckUiState> = _uiState.asStateFlow()

    fun save(
        title: String,
        sourceLanguage: String,
        targetLanguage: String,
        category: String,
    ) {
        if (_uiState.value.operationStatus == OperationStatus.LOADING) return

        val validationResult = validateFields(
            title = title,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            category = category,
        )

        when (validationResult) {
            is AppResult.Success -> saveDeck(validationResult.value)
            is AppResult.Failure -> showValidationFailure(validationResult.error)
        }
    }

    fun delete() {
        if (_uiState.value.operationStatus == OperationStatus.LOADING) return

        val deckId = existingDeck?.id ?: return
        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.LOADING,
            titleError = null,
            error = null,
        )
        viewModelScope.launch {
            when (val result = deckRepository.delete(deckId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    operationStatus = OperationStatus.SUCCESS,
                )
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(
                    operationStatus = OperationStatus.ERROR,
                    error = result.error,
                )
            }
        }
    }

    private fun showValidationFailure(error: AppError) {
        if (error is AppError.Validation && error.field == "deck title") {
            _uiState.value = _uiState.value.copy(
                operationStatus = OperationStatus.ERROR,
                titleError = error.reason,
                error = null,
            )
        } else {
            _uiState.value = _uiState.value.copy(
                operationStatus = OperationStatus.ERROR,
                titleError = null,
                error = error,
            )
        }
    }

    private fun saveDeck(fields: ValidatedDeckFields) {
        val deck = createOrUpdateDeck(fields) ?: return

        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.LOADING,
            titleError = null,
            error = null,
        )
        viewModelScope.launch {
            when (val result = deckRepository.save(deck)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    operationStatus = OperationStatus.SUCCESS,
                )
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(
                    operationStatus = OperationStatus.ERROR,
                    error = result.error,
                )
            }
        }
    }

    private fun createOrUpdateDeck(fields: ValidatedDeckFields): Deck? {
        val now = clock.now()

        if (existingDeck != null) {
            return existingDeck.copy(
                title = fields.title,
                sourceLanguage = fields.sourceLanguage,
                targetLanguage = fields.targetLanguage,
                category = fields.category,
                updatedAt = now,
            )
        }

        val idResult = DeckId.from(idGenerator.generate())
        val deckId = when (idResult) {
            is AppResult.Success -> idResult.value
            is AppResult.Failure -> {
                _uiState.value = _uiState.value.copy(
                    operationStatus = OperationStatus.ERROR,
                    error = idResult.error,
                )
                return null
            }
        }

        return Deck(
            id = deckId,
            ownerId = ownerId,
            title = fields.title,
            sourceLanguage = fields.sourceLanguage,
            targetLanguage = fields.targetLanguage,
            category = fields.category,
            visibility = DeckVisibility.PRIVATE,
            createdAt = now,
            updatedAt = now,
        )
    }
}

private data class ValidatedDeckFields(
    val title: DeckTitle,
    val sourceLanguage: DeckLanguage?,
    val targetLanguage: DeckLanguage?,
    val category: DeckCategory?,
)

private fun validateFields(
    title: String,
    sourceLanguage: String,
    targetLanguage: String,
    category: String,
): AppResult<ValidatedDeckFields> {
    val titleResult = DeckTitle.from(title)
    val validTitle = when (titleResult) {
        is AppResult.Success -> titleResult.value
        is AppResult.Failure -> return titleResult
    }

    val validSourceLanguage = DeckLanguage.from(sourceLanguage)
    val validTargetLanguage = DeckLanguage.from(targetLanguage)
    val validCategory = DeckCategory.from(category)

    return AppResult.Success(
        ValidatedDeckFields(
            title = validTitle,
            sourceLanguage = validSourceLanguage,
            targetLanguage = validTargetLanguage,
            category = validCategory,
        ),
    )
}

private fun Deck?.toUiState(): DeckUiState = if (this == null) {
    DeckUiState()
} else {
    DeckUiState(
        title = title.value,
        sourceLanguage = sourceLanguage?.value.orEmpty(),
        targetLanguage = targetLanguage?.value.orEmpty(),
        category = category?.value.orEmpty(),
        isEditing = true,
    )
}
