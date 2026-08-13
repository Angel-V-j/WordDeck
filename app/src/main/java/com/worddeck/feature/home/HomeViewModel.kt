package com.worddeck.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val status: OperationStatus = OperationStatus.LOADING,
    // Navigation needs the complete owner-scoped list even when the screen is filtered.
    val decks: List<Deck> = emptyList(),
    val visibleDecks: List<Deck> = decks,
    val searchQuery: String = "",
    val categoryFilter: String = "",
    val languageFilter: String = "",
    val error: AppError? = null,
)

class HomeViewModel(
    private val deckRepository: DeckRepository,
    ownerId: UserId,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deckRepository.observeByOwner(ownerId).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        publishWithVisibleDecks(
                            _uiState.value.copy(
                                status = OperationStatus.SUCCESS,
                                decks = result.value,
                                error = null,
                            ),
                        )
                    }
                    is AppResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            status = OperationStatus.ERROR,
                            decks = emptyList(),
                            visibleDecks = emptyList(),
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        publishWithVisibleDecks(_uiState.value.copy(searchQuery = query))
    }

    fun updateCategoryFilter(category: String) {
        publishWithVisibleDecks(_uiState.value.copy(categoryFilter = category))
    }

    fun updateLanguageFilter(language: String) {
        publishWithVisibleDecks(_uiState.value.copy(languageFilter = language))
    }

    private fun publishWithVisibleDecks(state: HomeUiState) {
        val query = state.searchQuery.trim()
        val category = state.categoryFilter.trim()
        val language = state.languageFilter.trim()

        val visibleDecks = state.decks.filter { deck ->
            deck.matchesSearch(query) &&
                deck.matchesCategory(category) &&
                deck.matchesLanguage(language)
        }
        _uiState.value = state.copy(visibleDecks = visibleDecks)
    }
}

private fun Deck.matchesSearch(query: String): Boolean {
    if (query.isEmpty()) return true

    return title.value.contains(query, ignoreCase = true) ||
        sourceLanguage?.value?.contains(query, ignoreCase = true) == true ||
        targetLanguage?.value?.contains(query, ignoreCase = true) == true ||
        category?.value?.contains(query, ignoreCase = true) == true
}

private fun Deck.matchesCategory(filter: String): Boolean {
    if (filter.isEmpty()) return true
    return category?.value?.contains(filter, ignoreCase = true) == true
}

private fun Deck.matchesLanguage(filter: String): Boolean {
    if (filter.isEmpty()) return true

    return sourceLanguage?.value?.contains(filter, ignoreCase = true) == true ||
        targetLanguage?.value?.contains(filter, ignoreCase = true) == true
}
