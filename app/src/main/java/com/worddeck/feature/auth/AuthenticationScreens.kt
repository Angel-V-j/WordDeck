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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
        uiState = uiState,
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
) {
    val isSubmitting = uiState.submitStatus == OperationStatus.LOADING
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthenticationForm(
        title = stringResource(R.string.register_title),
        uiState = uiState,
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
            onClick = onOpenLogin,
            enabled = !isSubmitting,
        ) {
            Text(stringResource(R.string.open_login_action))
        }
    }
}

@Composable
private fun AuthenticationForm(
    title: String,
    uiState: AuthUiState,
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
            uiState.error?.let {
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
    is AppError.Validation -> "${error.field}: ${error.reason}"
}
