package com.worddeck.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.your_decks_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Row {
                TextButton(onClick = onCreateDeck) {
                    Text(stringResource(R.string.new_deck_action))
                }
                TextButton(onClick = onOpenProfile) {
                    Text(stringResource(R.string.profile_title))
                }
            }
        }
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_decks_label)) },
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = uiState.categoryFilter,
                onValueChange = onCategoryFilterChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.category_filter_label)) },
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = uiState.languageFilter,
                onValueChange = onLanguageFilterChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.language_filter_label)) },
                singleLine = true,
            )
        }

        when (uiState.status) {
            OperationStatus.IDLE,
            OperationStatus.LOADING,
            -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.loading_decks))
                }
            }
            OperationStatus.ERROR -> item {
                Text(
                    text = stringResource(R.string.deck_list_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OperationStatus.SUCCESS -> if (uiState.visibleDecks.isEmpty()) {
                item {
                    val message = if (uiState.decks.isEmpty()) {
                        R.string.empty_decks
                    } else {
                        R.string.no_matching_decks
                    }
                    Text(stringResource(message))
                }
            } else {
                items(uiState.visibleDecks, key = { it.id.value }) { deck ->
                    DeckItem(deck = deck, onClick = { onDeckClick(deck.id) })
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
    onOpenFlashcardHistory: (CardId) -> Unit = {},
    onStartStudy: () -> Unit = {},
    onStartTypedStudy: () -> Unit = {},
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        if (deck == null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack, enabled = !isDeleting) {
                    Text(stringResource(R.string.back_action))
                }
                Text(
                    text = stringResource(R.string.deck_details_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(stringResource(R.string.deck_not_found))
            }
        } else if (maxWidth > maxHeight) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                DeckSummary(
                    deck = deck,
                    uiState = uiState,
                    isDeleting = isDeleting,
                    onEdit = onEdit,
                    onDeleteRequest = { showDeleteConfirmation = true },
                    onBack = onBack,
                    onStartStudy = onStartStudy,
                    onStartTypedStudy = onStartTypedStudy,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                )
                FlashcardSection(
                    uiState = flashcardUiState,
                    onSave = onSaveFlashcard,
                    onDelete = onDeleteFlashcard,
                    onClearOperation = onClearFlashcardOperation,
                    onSearchQueryChange = onFlashcardSearchQueryChange,
                    onOpenHistory = onOpenFlashcardHistory,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeckSummary(
                    deck = deck,
                    uiState = uiState,
                    isDeleting = isDeleting,
                    onEdit = onEdit,
                    onDeleteRequest = { showDeleteConfirmation = true },
                    onBack = onBack,
                    onStartStudy = onStartStudy,
                    onStartTypedStudy = onStartTypedStudy,
                )
                FlashcardSection(
                    uiState = flashcardUiState,
                    onSave = onSaveFlashcard,
                    onDelete = onDeleteFlashcard,
                    onClearOperation = onClearFlashcardOperation,
                    onSearchQueryChange = onFlashcardSearchQueryChange,
                    onOpenHistory = onOpenFlashcardHistory,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DeckSummary(
    deck: Deck,
    uiState: DeckUiState,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onBack: () -> Unit,
    onStartStudy: () -> Unit,
    onStartTypedStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, enabled = !isDeleting) {
            Text(stringResource(R.string.back_action))
        }
        Text(
            text = stringResource(R.string.deck_details_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(deck.title.value, style = MaterialTheme.typography.titleLarge)
        DeckMetadata(deck)
        TextButton(
            onClick = onStartStudy,
            enabled = !isDeleting,
        ) {
            Text(stringResource(R.string.start_study_action))
        }
        TextButton(
            onClick = onStartTypedStudy,
            enabled = !isDeleting,
        ) {
            Text(stringResource(R.string.start_typed_study_action))
        }
        Row {
            TextButton(onClick = onEdit, enabled = !isDeleting) {
                Text(stringResource(R.string.edit_deck_action))
            }
            TextButton(onClick = onDeleteRequest, enabled = !isDeleting) {
                Text(
                    text = stringResource(R.string.delete_deck_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
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
