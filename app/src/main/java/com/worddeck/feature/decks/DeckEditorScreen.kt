package com.worddeck.feature.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.OperationStatus

@Composable
fun DeckEditorScreen(
    uiState: DeckEditorUiState,
    onSave: (title: String, sourceLanguage: String, targetLanguage: String, category: String) -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSaving = uiState.saveStatus == OperationStatus.LOADING
    val isSaved = uiState.saveStatus == OperationStatus.SUCCESS
    var title by rememberSaveable(uiState.isEditing) { mutableStateOf(uiState.title) }
    var sourceLanguage by rememberSaveable(uiState.isEditing) {
        mutableStateOf(uiState.sourceLanguage)
    }
    var targetLanguage by rememberSaveable(uiState.isEditing) {
        mutableStateOf(uiState.targetLanguage)
    }
    var category by rememberSaveable(uiState.isEditing) { mutableStateOf(uiState.category) }

    LaunchedEffect(isSaved) {
        if (isSaved) onSaved()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, enabled = !isSaving) {
            Text(stringResource(R.string.back_action))
        }
        Text(
            text = stringResource(
                if (uiState.isEditing) R.string.edit_deck_title else R.string.create_deck_title,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            singleLine = true,
            label = { Text(stringResource(R.string.deck_title_label)) },
            isError = uiState.titleError != null,
            supportingText = uiState.titleError?.let { reason ->
                { Text(stringResource(R.string.deck_title_error, reason)) }
            },
        )
        OutlinedTextField(
            value = sourceLanguage,
            onValueChange = { sourceLanguage = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            singleLine = true,
            label = { Text(stringResource(R.string.source_language_label)) },
        )
        OutlinedTextField(
            value = targetLanguage,
            onValueChange = { targetLanguage = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            singleLine = true,
            label = { Text(stringResource(R.string.target_language_label)) },
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            singleLine = true,
            label = { Text(stringResource(R.string.deck_category_label)) },
        )
        if (uiState.error != null) {
            Text(
                text = stringResource(R.string.save_deck_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (isSaving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Button(
            onClick = { onSave(title, sourceLanguage, targetLanguage, category) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
        ) {
            Text(stringResource(R.string.save_deck_action))
        }
    }
}
