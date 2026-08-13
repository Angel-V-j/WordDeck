package com.worddeck.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.Flashcard
import com.worddeck.feature.decks.DeckUiState
import com.worddeck.feature.decks.FlashcardSection
import com.worddeck.feature.decks.FlashcardUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDeckClick: (DeckId) -> Unit,
    onCreateDeck: () -> Unit,
    onOpenProfile: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onLanguageFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.your_decks_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Row {
                TextButton(onClick = onCreateDeck) {
                    Text(stringResource(R.string.new_deck_action))
                }
                TextButton(onClick = onOpenProfile) {
                    Text(stringResource(R.string.profile_title))
                }
            }
        }

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_decks_label)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.categoryFilter,
            onValueChange = onCategoryFilterChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.category_filter_label)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.languageFilter,
            onValueChange = onLanguageFilterChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.language_filter_label)) },
            singleLine = true,
        )

        when (uiState.status) {
            OperationStatus.IDLE,
            OperationStatus.LOADING,
            -> CenteredContent {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading_decks))
            }
            OperationStatus.ERROR -> CenteredContent {
                Text(
                    text = stringResource(R.string.deck_list_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OperationStatus.SUCCESS -> if (uiState.visibleDecks.isEmpty()) {
                CenteredContent {
                    val message = if (uiState.decks.isEmpty()) {
                        R.string.empty_decks
                    } else {
                        R.string.no_matching_decks
                    }
                    Text(stringResource(message))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.visibleDecks, key = { it.id.value }) { deck ->
                        DeckItem(deck = deck, onClick = { onDeckClick(deck.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun DeckDetailsScreen(
    deck: Deck?,
    uiState: DeckUiState,
    flashcardUiState: FlashcardUiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSaveFlashcard: (Flashcard?, String, String, String, String) -> Unit,
    onDeleteFlashcard: (CardId) -> Unit,
    onClearFlashcardOperation: () -> Unit,
    onFlashcardSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    val isDeleting = uiState.operationStatus == OperationStatus.LOADING

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_deck_dialog_title)) },
            text = { Text(stringResource(R.string.delete_deck_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, enabled = !isDeleting) {
            Text(stringResource(R.string.back_action))
        }
        Text(
            text = stringResource(R.string.deck_details_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (deck == null) {
            Text(stringResource(R.string.deck_not_found))
        } else {
            Text(deck.title.value, style = MaterialTheme.typography.titleLarge)
            DeckMetadata(deck)
            TextButton(onClick = onEdit, enabled = !isDeleting) {
                Text(stringResource(R.string.edit_deck_action))
            }
            TextButton(
                onClick = { showDeleteConfirmation = true },
                enabled = !isDeleting,
            ) {
                Text(
                    text = stringResource(R.string.delete_deck_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (isDeleting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (uiState.error != null) {
                Text(
                    text = stringResource(R.string.delete_deck_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            FlashcardSection(
                uiState = flashcardUiState,
                onSave = onSaveFlashcard,
                onDelete = onDeleteFlashcard,
                onClearOperation = onClearFlashcardOperation,
                onSearchQueryChange = onFlashcardSearchQueryChange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DeckItem(deck: Deck, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(deck.title.value, style = MaterialTheme.typography.titleMedium)
            DeckMetadata(deck)
        }
    }
}

@Composable
private fun DeckMetadata(deck: Deck) {
    val languages = listOfNotNull(
        deck.sourceLanguage?.value,
        deck.targetLanguage?.value,
    ).joinToString(" → ")

    if (languages.isNotEmpty()) {
        Text(languages, style = MaterialTheme.typography.bodyMedium)
    }
    deck.category?.let {
        Text(it.value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CenteredContent(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
