package com.worddeck.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard
import com.worddeck.feature.decks.DeckUiState
import com.worddeck.feature.decks.FlashcardSection
import com.worddeck.feature.decks.FlashcardUiState
import com.worddeck.ui.components.BackButton
import com.worddeck.ui.components.EmptyState
import com.worddeck.ui.components.WordDeckBrand

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDeckClick: (DeckId) -> Unit,
    onCreateDeck: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onLanguageFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Use every owned deck so a selection never removes the other available options.
    val categories = uiState.decks.mapNotNull { it.category?.value }
        .distinctBy { it.lowercase() }.sortedWith(String.CASE_INSENSITIVE_ORDER)
    val languages = uiState.decks.flatMap {
        listOfNotNull(it.sourceLanguage?.value, it.targetLanguage?.value)
    }.distinctBy { it.lowercase() }.sortedWith(String.CASE_INSENSITIVE_ORDER)

    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WordDeckBrand()
                Text(stringResource(R.string.your_decks_title),
                    style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            OutlinedTextField(
                shape = MaterialTheme.shapes.medium,
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_decks_label)) },
                singleLine = true,
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onCreateDeck, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(18.dp))
                    Text(stringResource(R.string.new_deck_action))
                }
                DeckFilterMenu(
                    label = stringResource(R.string.category_filter_action),
                    description = stringResource(R.string.category_filter_label),
                    allLabel = stringResource(R.string.all_categories),
                    selected = uiState.categoryFilter, options = categories,
                    onSelect = onCategoryFilterChange,
                )
                DeckFilterMenu(
                    label = stringResource(R.string.language_filter_action),
                    description = stringResource(R.string.language_filter_label),
                    allLabel = stringResource(R.string.all_languages),
                    selected = uiState.languageFilter, options = languages,
                    onSelect = onLanguageFilterChange,
                )
            }
        }
        when (uiState.status) {
            OperationStatus.IDLE, OperationStatus.LOADING -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.loading_decks))
                }
            }
            OperationStatus.ERROR -> item {
                Text(stringResource(R.string.deck_list_error), color = MaterialTheme.colorScheme.error)
            }
            OperationStatus.SUCCESS -> if (uiState.visibleDecks.isEmpty()) {
                item {
                    EmptyState(stringResource(if (uiState.decks.isEmpty()) {
                        R.string.empty_decks
                    } else R.string.no_matching_decks))
                }
            } else {
                items(uiState.visibleDecks, key = { it.id.value }) { deck ->
                    DeckItem(deck, onClick = { onDeckClick(deck.id) })
                }
            }
        }
    }
}

@Composable
private fun DeckFilterMenu(
    label: String,
    description: String,
    allLabel: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Box(Modifier.widthIn(max = 180.dp)) {
        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                expanded = true
            },
            modifier = Modifier.semantics {
                contentDescription = description
                stateDescription = selected.ifBlank { allLabel }
            },
            contentPadding = PaddingValues(horizontal = 12.dp),
            border = BorderStroke(1.dp, if (selected.isBlank()) {
                MaterialTheme.colorScheme.outlineVariant
            } else MaterialTheme.colorScheme.primary),
        ) {
            Text(selected.ifBlank { label }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null,
                modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 180.dp, max = 280.dp).heightIn(max = 320.dp)) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = {
                expanded = false
                onSelect("")
            })
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = if (option.equals(selected, ignoreCase = true)) {
                        MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.onSurface) },
                    onClick = { expanded = false; onSelect(option) },
                    trailingIcon = if (option.equals(selected, ignoreCase = true)) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else null,
                )
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
    onOpenStatistics: () -> Unit = {},
    onOpenFlashcardHistory: (CardId) -> Unit = {},
    onStartStudy: () -> Unit = {},
    onStartTypedStudy: () -> Unit = {},
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
        modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        if (deck == null) {
            Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(onClick = onBack, enabled = !isDeleting)
                Text(stringResource(R.string.deck_details_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.deck_not_found))
            }
        } else {
            FlashcardSection(
                uiState = flashcardUiState,
                onSave = onSaveFlashcard,
                onDelete = onDeleteFlashcard,
                onClearOperation = onClearFlashcardOperation,
                onSearchQueryChange = onFlashcardSearchQueryChange,
                onOpenHistory = onOpenFlashcardHistory,
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                header = {
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BackButton(onClick = onBack, enabled = !isDeleting)
                        Text(deck.title.value, style = MaterialTheme.typography.headlineMedium)
                        DeckMetadata(deck)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = onStartStudy, enabled = !isDeleting,
                                contentPadding = PaddingValues(horizontal = 12.dp)) {
                                Text(stringResource(R.string.start_study_action))
                            }
                            FilledTonalButton(onClick = onStartTypedStudy, enabled = !isDeleting,
                                contentPadding = PaddingValues(horizontal = 12.dp)) {
                                Text(stringResource(R.string.start_typed_study_action))
                            }
                            OutlinedButton(onClick = onOpenStatistics, enabled = !isDeleting,
                                contentPadding = PaddingValues(horizontal = 12.dp)) {
                                Text(stringResource(R.string.deck_statistics_action))
                            }
                        }
                    }
                },
            )
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (isDeleting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    if (uiState.error != null) {
                        Text(stringResource(R.string.delete_deck_error), color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                    }
                    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onEdit, enabled = !isDeleting) {
                            Text(stringResource(R.string.edit_deck_action))
                        }
                        TextButton(onClick = { showDeleteConfirmation = true }, enabled = !isDeleting) {
                            Text(stringResource(R.string.delete_deck_action), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckItem(deck: Deck, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_cards), contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(deck.title.value, style = MaterialTheme.typography.titleMedium)
                DeckMetadata(deck)
            }
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
        Text(languages, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    deck.category?.let {
        Text(it.value, style = MaterialTheme.typography.bodySmall)
    }
}
