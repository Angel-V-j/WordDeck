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

data class DeckEditorUiState(
    val title: String = "",
    val sourceLanguage: String = "",
    val targetLanguage: String = "",
    val category: String = "",
    val isEditing: Boolean = false,
    val saveStatus: OperationStatus = OperationStatus.IDLE,
    val titleError: String? = null,
    val error: AppError? = null,
)

class DeckEditorViewModel(
    private val deckRepository: DeckRepository,
    private val ownerId: UserId,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val existingDeck: Deck? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(existingDeck.toEditorState())
    val uiState: StateFlow<DeckEditorUiState> = _uiState.asStateFlow()

    fun save(
        title: String,
        sourceLanguage: String,
        targetLanguage: String,
        category: String,
    ) {
        if (_uiState.value.saveStatus == OperationStatus.LOADING) return

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

    private fun showValidationFailure(error: AppError) {
        if (error is AppError.Validation && error.field == "deck title") {
            _uiState.value = _uiState.value.copy(
                saveStatus = OperationStatus.ERROR,
                titleError = error.reason,
                error = null,
            )
        } else {
            _uiState.value = _uiState.value.copy(
                saveStatus = OperationStatus.ERROR,
                titleError = null,
                error = error,
            )
        }
    }

    private fun saveDeck(fields: ValidatedDeckFields) {
        val deck = createOrUpdateDeck(fields) ?: return

        _uiState.value = _uiState.value.copy(
            saveStatus = OperationStatus.LOADING,
            titleError = null,
            error = null,
        )
        viewModelScope.launch {
            when (val result = deckRepository.save(deck)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    saveStatus = OperationStatus.SUCCESS,
                )
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(
                    saveStatus = OperationStatus.ERROR,
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
                    saveStatus = OperationStatus.ERROR,
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

    val sourceLanguageResult = DeckLanguage.from(sourceLanguage)
    val validSourceLanguage = when (sourceLanguageResult) {
        is AppResult.Success -> sourceLanguageResult.value
        is AppResult.Failure -> return sourceLanguageResult
    }

    val targetLanguageResult = DeckLanguage.from(targetLanguage)
    val validTargetLanguage = when (targetLanguageResult) {
        is AppResult.Success -> targetLanguageResult.value
        is AppResult.Failure -> return targetLanguageResult
    }

    val categoryResult = DeckCategory.from(category)
    val validCategory = when (categoryResult) {
        is AppResult.Success -> categoryResult.value
        is AppResult.Failure -> return categoryResult
    }

    return AppResult.Success(
        ValidatedDeckFields(
            title = validTitle,
            sourceLanguage = validSourceLanguage,
            targetLanguage = validTargetLanguage,
            category = validCategory,
        ),
    )
}

private fun Deck?.toEditorState(): DeckEditorUiState = if (this == null) {
    DeckEditorUiState()
} else {
    DeckEditorUiState(
        title = title.value,
        sourceLanguage = sourceLanguage?.value.orEmpty(),
        targetLanguage = targetLanguage?.value.orEmpty(),
        category = category?.value.orEmpty(),
        isEditing = true,
    )
}
