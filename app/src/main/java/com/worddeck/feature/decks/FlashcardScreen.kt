package com.worddeck.feature.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.Flashcard
import com.worddeck.ui.components.EmptyState

@Composable
fun FlashcardSection(
    uiState: FlashcardUiState,
    onSave: (Flashcard?, String, String, String, String) -> Unit,
    onDelete: (CardId) -> Unit,
    onClearOperation: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenHistory: (CardId) -> Unit = {},
    header: (@Composable () -> Unit)? = null,
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingFlashcardId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<Flashcard?>(null) }
    val editingFlashcard = uiState.cards.firstOrNull { flashcard ->
        flashcard.id.value == editingFlashcardId
    }
    val isWorking = uiState.operationStatus == OperationStatus.LOADING

    LaunchedEffect(uiState.operationStatus) {
        if (uiState.operationStatus == OperationStatus.SUCCESS) {
            showEditor = false
            editingFlashcardId = null
            deleteCandidate = null
            onClearOperation()
        }
    }

    if (showEditor) {
        FlashcardEditorDialog(
            flashcard = editingFlashcard,
            uiState = uiState,
            onSave = onSave,
            onDeleteRequest = { flashcard ->
                showEditor = false
                editingFlashcardId = null
                deleteCandidate = flashcard
            },
            onOpenHistory = onOpenHistory,
            onDismiss = {
                showEditor = false
                editingFlashcardId = null
                onClearOperation()
            },
        )
    }

    deleteCandidate?.let { flashcard ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.delete_card_dialog_title)) },
            text = { Text(stringResource(R.string.delete_card_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                        onDelete(flashcard.id)
                    },
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (header != null) item { header() }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.cards_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                FilledTonalButton(
                    onClick = {
                        onClearOperation()
                        editingFlashcardId = null
                        showEditor = true
                    },
                    enabled = !isWorking,
                ) { Text(stringResource(R.string.add_card_action)) }
            }
        }
        item {
            OutlinedTextField(
                shape = MaterialTheme.shapes.medium,
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_cards_label)) },
                singleLine = true,
            )
        }
        if (isWorking) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        if (uiState.error != null && uiState.operationStatus == OperationStatus.ERROR) item {
            Text(stringResource(R.string.card_operation_error), color = MaterialTheme.colorScheme.error)
        }
        when (uiState.listStatus) {
            OperationStatus.IDLE, OperationStatus.LOADING -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            OperationStatus.ERROR -> item {
                Text(stringResource(R.string.card_list_error), color = MaterialTheme.colorScheme.error)
            }
            OperationStatus.SUCCESS -> if (uiState.cards.isEmpty()) {
                item {
                    EmptyState(stringResource(
                        if (uiState.searchQuery.isBlank()) R.string.empty_cards else R.string.no_matching_cards,
                    ))
                }
            } else {
                items(uiState.cards, key = { it.id.value }) { flashcard ->
                    FlashcardItem(
                        flashcard = flashcard,
                        onClick = {
                            onClearOperation()
                            editingFlashcardId = flashcard.id.value
                            showEditor = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardEditorDialog(
    flashcard: Flashcard?,
    uiState: FlashcardUiState,
    onSave: (Flashcard?, String, String, String, String) -> Unit,
    onDeleteRequest: (Flashcard) -> Unit,
    onOpenHistory: (CardId) -> Unit,
    onDismiss: () -> Unit,
) {
    var front by rememberSaveable(flashcard?.id?.value) {
        mutableStateOf(flashcard?.front?.value.orEmpty())
    }
    var back by rememberSaveable(flashcard?.id?.value) {
        mutableStateOf(flashcard?.back?.value.orEmpty())
    }
    var example by rememberSaveable(flashcard?.id?.value) {
        mutableStateOf(flashcard?.exampleSentence.orEmpty())
    }
    var additionalInformation by rememberSaveable(flashcard?.id?.value) {
        mutableStateOf(flashcard?.additionalInformation.orEmpty())
    }
    val isWorking = uiState.operationStatus == OperationStatus.LOADING

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = {
            Text(
                stringResource(
                    if (flashcard == null) R.string.create_card_title else R.string.edit_card_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.medium,
                    value = front,
                    onValueChange = { front = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isWorking,
                    label = { Text(stringResource(R.string.card_front_label)) },
                    isError = uiState.frontError != null,
                    supportingText = uiState.frontError?.let { reason ->
                        { Text(stringResource(R.string.card_front_error, reason)) }
                    },
                )
                OutlinedTextField(
                    shape = MaterialTheme.shapes.medium,
                    value = back,
                    onValueChange = { back = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isWorking,
                    label = { Text(stringResource(R.string.card_back_label)) },
                    isError = uiState.backError != null,
                    supportingText = uiState.backError?.let { reason ->
                        { Text(stringResource(R.string.card_back_error, reason)) }
                    },
                )
                OutlinedTextField(
                    shape = MaterialTheme.shapes.medium,
                    value = example,
                    onValueChange = { example = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isWorking,
                    label = { Text(stringResource(R.string.card_example_label)) },
                )
                OutlinedTextField(
                    shape = MaterialTheme.shapes.medium,
                    value = additionalInformation,
                    onValueChange = { additionalInformation = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isWorking,
                    label = { Text(stringResource(R.string.card_additional_information_label)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(flashcard, front, back, example, additionalInformation)
                },
                enabled = !isWorking,
            ) {
                Text(stringResource(R.string.save_card_action))
            }
        },
        dismissButton = {
            FlowRow {
                if (flashcard != null) {
                    TextButton(
                        onClick = { onOpenHistory(flashcard.id) },
                        enabled = !isWorking,
                    ) {
                        Text(stringResource(R.string.review_history_action))
                    }
                    TextButton(
                        onClick = { onDeleteRequest(flashcard) },
                        enabled = !isWorking,
                    ) {
                        Text(
                            text = stringResource(R.string.delete_card_action),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss, enabled = !isWorking) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        },
    )
}

@Composable
private fun FlashcardItem(flashcard: Flashcard, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(flashcard.front.value, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 6.dp))
            Text(flashcard.back.value, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            flashcard.exampleSentence?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
