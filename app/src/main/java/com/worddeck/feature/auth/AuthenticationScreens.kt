package com.worddeck.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.AppError
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.User

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onOpenRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSubmitting = uiState.submitStatus == OperationStatus.LOADING
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthenticationForm(
        title = stringResource(R.string.login_title),
        error = uiState.error,
        modifier = modifier,
    ) {
        AuthenticationTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.email_label),
            errorReason = uiState.formErrors.email,
            enabled = !isSubmitting,
            keyboardType = KeyboardType.Email,
        )
        AuthenticationTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password_label),
            errorReason = uiState.formErrors.password,
            enabled = !isSubmitting,
            isPassword = true,
        )
        SubmitButton(
            text = stringResource(R.string.login_action),
            isLoading = isSubmitting,
            onClick = { onLogin(email, password) },
        )
        TextButton(
            onClick = onOpenRegister,
            enabled = !isSubmitting,
        ) {
            Text(stringResource(R.string.open_register_action))
        }
    }
}

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (displayName: String, email: String, password: String) -> Unit,
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
    initialDisplayName: String = "",
    onCancel: (() -> Unit)? = null,
) {
    val isSubmitting = uiState.submitStatus == OperationStatus.LOADING
    var displayName by rememberSaveable(initialDisplayName) {
        mutableStateOf(initialDisplayName)
    }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthenticationForm(
        title = stringResource(R.string.register_title),
        error = uiState.error,
        modifier = modifier,
    ) {
        AuthenticationTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = stringResource(R.string.display_name_label),
            errorReason = uiState.formErrors.displayName,
            enabled = !isSubmitting,
        )
        AuthenticationTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.email_label),
            errorReason = uiState.formErrors.email,
            enabled = !isSubmitting,
            keyboardType = KeyboardType.Email,
        )
        AuthenticationTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password_label),
            errorReason = uiState.formErrors.password,
            enabled = !isSubmitting,
            isPassword = true,
        )
        SubmitButton(
            text = stringResource(R.string.register_action),
            isLoading = isSubmitting,
            onClick = { onRegister(displayName, email, password) },
        )
        TextButton(
            onClick = onCancel ?: onOpenLogin,
            enabled = !isSubmitting,
        ) {
            Text(
                stringResource(
                    if (onCancel == null) R.string.open_login_action else R.string.cancel_action,
                ),
            )
        }
    }
}

@Composable
fun OfflineProfileScreen(
    uiState: AuthUiState,
    onContinue: (displayName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSubmitting = uiState.submitStatus == OperationStatus.LOADING
    var displayName by rememberSaveable { mutableStateOf("") }

    AuthenticationForm(
        title = stringResource(R.string.offline_profile_title),
        error = uiState.error,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.offline_profile_explanation))
        AuthenticationTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = stringResource(R.string.display_name_label),
            errorReason = uiState.formErrors.displayName,
            enabled = !isSubmitting,
        )
        SubmitButton(
            text = stringResource(R.string.continue_offline_action),
            isLoading = isSubmitting,
            onClick = { onContinue(displayName) },
        )
    }
}

@Composable
fun AuthenticationFallbackDialog(
    fallback: AuthFallback?,
    onContinueOffline: () -> Unit,
    onCancel: () -> Unit,
) {
    if (fallback == null) return

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.authentication_unavailable_title)) },
        text = { Text(stringResource(R.string.authentication_offline_fallback_message)) },
        confirmButton = {
            TextButton(onClick = onContinueOffline) {
                Text(stringResource(R.string.continue_offline_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel_action))
            }
        },
    )
}

@Composable
fun SyncFlow(
    syncUiState: SyncUiState,
    authUiState: AuthUiState,
    user: User?,
    onRegister: (displayName: String, email: String, password: String) -> Unit,
    onReauthenticate: (email: String, password: String) -> Unit,
    onAcceptOffer: () -> Unit,
    onPostpone: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onClearAuthErrors: () -> Unit,
) {
    when (syncUiState.step) {
        SyncStep.IDLE -> Unit
        SyncStep.OFFER -> SyncOfferDialog(
            onSync = onAcceptOffer,
            onLater = onPostpone,
        )
        SyncStep.REGISTRATION -> if (user != null) {
            Surface(modifier = Modifier.fillMaxSize()) {
                RegisterScreen(
                    uiState = authUiState,
                    onRegister = onRegister,
                    onOpenLogin = {},
                    initialDisplayName = user.displayName.value,
                    onCancel = {
                        onClearAuthErrors()
                        onCancel()
                    },
                )
            }
        }
        SyncStep.REAUTHENTICATION -> Surface(modifier = Modifier.fillMaxSize()) {
            ReauthenticationScreen(
                uiState = syncUiState,
                initialEmail = user?.email?.value.orEmpty(),
                onContinue = onReauthenticate,
                onCancel = onCancel,
            )
        }
        SyncStep.CONFIRMATION -> SyncConfirmationDialog(
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
        SyncStep.SYNCING -> SyncProgressDialog()
        SyncStep.SUCCESS -> SyncResultDialog(
            title = stringResource(R.string.sync_success_title),
            message = stringResource(R.string.sync_success_message),
            onDismiss = onCancel,
        )
        SyncStep.ERROR -> SyncResultDialog(
            title = stringResource(R.string.sync_error_title),
            message = syncUiState.error?.let { authenticationErrorMessage(it) }
                ?: stringResource(R.string.sync_error_message),
            onDismiss = onCancel,
        )
    }
}

@Composable
private fun ReauthenticationScreen(
    uiState: SyncUiState,
    initialEmail: String,
    onContinue: (email: String, password: String) -> Unit,
    onCancel: () -> Unit,
) {
    val isSubmitting = uiState.operationStatus == OperationStatus.LOADING
    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }

    AuthenticationForm(
        title = stringResource(R.string.reauthentication_title),
        error = uiState.error,
    ) {
        Text(stringResource(R.string.reauthentication_explanation))
        AuthenticationTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.email_label),
            errorReason = uiState.emailError,
            enabled = !isSubmitting,
            keyboardType = KeyboardType.Email,
        )
        AuthenticationTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password_label),
            errorReason = uiState.passwordError,
            enabled = !isSubmitting,
            isPassword = true,
        )
        SubmitButton(
            text = stringResource(R.string.continue_action),
            isLoading = isSubmitting,
            onClick = { onContinue(email, password) },
        )
        TextButton(onClick = onCancel, enabled = !isSubmitting) {
            Text(stringResource(R.string.cancel_action))
        }
    }
}

@Composable
private fun SyncOfferDialog(onSync: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.connection_restored_title)) },
        text = { Text(stringResource(R.string.connection_restored_message)) },
        confirmButton = {
            TextButton(onClick = onSync) { Text(stringResource(R.string.sync_action)) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.later_action)) }
        },
    )
}

@Composable
private fun SyncConfirmationDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.sync_confirmation_title)) },
        text = { Text(stringResource(R.string.sync_confirmation_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.confirm_sync_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}

@Composable
private fun SyncProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.syncing_title)) },
        text = { CircularProgressIndicator() },
        confirmButton = {},
    )
}

@Composable
private fun SyncResultDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok_action)) }
        },
    )
}

@Composable
private fun AuthenticationForm(
    title: String,
    error: AppError?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            error?.let {
                Text(
                    text = authenticationErrorMessage(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            content()
        }
    }
}

@Composable
private fun AuthenticationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorReason: String?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        isError = errorReason != null,
        supportingText = errorReason?.let { reason ->
            { Text("$label $reason") }
        },
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
private fun SubmitButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(text)
        }
    }
}

@Composable
private fun authenticationErrorMessage(error: AppError): String = when (error) {
    AppError.Authentication.InvalidCredentials ->
        stringResource(R.string.invalid_credentials_error)
    AppError.Authentication.EmailAlreadyInUse ->
        stringResource(R.string.email_already_used_error)
    AppError.Authentication.Unauthenticated ->
        stringResource(R.string.session_expired_error)
    AppError.NetworkUnavailable -> stringResource(R.string.network_error)
    is AppError.Unavailable -> stringResource(R.string.authentication_unavailable_error)
    is AppError.Validation -> if (error.field == "local profile") {
        stringResource(R.string.local_profile_unavailable_error)
    } else {
        "${error.field}: ${error.reason}"
    }
}
