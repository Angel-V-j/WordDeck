package com.worddeck.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
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
import com.worddeck.common.AppError
import com.worddeck.domain.model.User

@Composable
fun ProfileScreen(
    user: User,
    isSubmitting: Boolean,
    displayNameError: String?,
    error: AppError?,
    onUpdateDisplayName: (String) -> Unit,
    onSync: () -> Unit,
    onDownload: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayName by rememberSaveable(user.displayName.value) {
        mutableStateOf(user.displayName.value)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back_action))
            }
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.welcome_user, user.displayName.value),
                style = MaterialTheme.typography.titleLarge,
            )
            val email = user.email
            Text(
                text = if (email == null) {
                    stringResource(R.string.offline_profile_status)
                } else {
                    stringResource(R.string.email_identity, email.value)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                singleLine = true,
                label = { Text(stringResource(R.string.display_name_label)) },
                isError = displayNameError != null,
                supportingText = displayNameError?.let { reason ->
                    { Text(stringResource(R.string.display_name_error, reason)) }
                },
            )
            OutlinedButton(
                onClick = { onUpdateDisplayName(displayName) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
            ) {
                Text(stringResource(R.string.save_display_name_action))
            }
            OutlinedButton(
                onClick = onSync,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
            ) {
                Text(stringResource(R.string.sync_data_action))
            }
            OutlinedButton(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && user.isLinked,
            ) {
                Text(stringResource(R.string.download_data_action))
            }
            if (!user.isLinked) {
                Text(stringResource(R.string.link_before_download_message))
            }
            if (error != null) {
                Text(
                    text = stringResource(R.string.account_action_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (isSubmitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
            ) {
                Text(stringResource(R.string.logout_action))
            }
        }
    }
}
