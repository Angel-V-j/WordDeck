package com.worddeck.feature.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun FlashcardSection(
    uiState: FlashcardUiState,
    onSave: (Flashcard?, String, String, String, String) -> Unit,
    onDelete: (CardId) -> Unit,
    onClearOperation: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenHistory: (CardId) -> Unit = {},
    modifier: Modifier = Modifier,
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

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.cards_title),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(
                onClick = {
                    onClearOperation()
                    editingFlashcardId = null
                    showEditor = true
                },
                enabled = !isWorking,
            ) {
                Text(stringResource(R.string.add_card_action))
            }
        }

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_cards_label)) },
            singleLine = true,
        )

        if (isWorking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (uiState.operationStatus == OperationStatus.ERROR && uiState.error != null) {
            Text(
                text = stringResource(R.string.card_operation_error),
                color = MaterialTheme.colorScheme.error,
            )
        }

        when (uiState.listStatus) {
            OperationStatus.IDLE,
            OperationStatus.LOADING,
            -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            OperationStatus.ERROR -> Text(
                text = stringResource(R.string.card_list_error),
                color = MaterialTheme.colorScheme.error,
            )
            OperationStatus.SUCCESS -> if (uiState.cards.isEmpty()) {
                Text(
                    stringResource(
                        if (uiState.searchQuery.isBlank()) {
                            R.string.empty_cards
                        } else {
                            R.string.no_matching_cards
                        },
                    ),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                    value = example,
                    onValueChange = { example = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isWorking,
                    label = { Text(stringResource(R.string.card_example_label)) },
                )
                OutlinedTextField(
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
            Row {
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
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(flashcard.front.value, style = MaterialTheme.typography.titleMedium)
            Text(flashcard.back.value, style = MaterialTheme.typography.bodyLarge)
            flashcard.exampleSentence?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
