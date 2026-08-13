package com.worddeck.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.EmailAddress
import com.worddeck.domain.model.User
import com.worddeck.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormErrors(
    val displayName: String? = null,
    val email: String? = null,
    val password: String? = null,
)

data class AuthUiState(
    val currentUser: User? = null,
    val isSessionLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val formErrors: AuthFormErrors = AuthFormErrors(),
    val error: AppError? = null,
)

class AuthViewModel(
    private val authenticationRepository: AuthenticationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authenticationRepository.observeCurrentUser().collect { result ->
                _uiState.update { current ->
                    when (result) {
                        is AppResult.Success -> current.copy(
                            currentUser = result.value,
                            isSessionLoading = false,
                            error = null,
                        )
                        is AppResult.Failure -> current.copy(
                            currentUser = null,
                            isSessionLoading = false,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (_uiState.value.isSubmitting) return

        submit(
            formErrors = validateLogin(email, password),
            operation = {
                authenticationRepository.login(
                    email = email.trim(),
                    password = password,
                )
            },
        )
    }

    fun register(displayName: String, email: String, password: String) {
        if (_uiState.value.isSubmitting) return

        submit(
            formErrors = validateRegistration(displayName, email, password),
            operation = {
                authenticationRepository.register(
                    displayName = DisplayName.from(displayName).successValue(),
                    email = email.trim(),
                    password = password,
                )
            },
        )
    }

    fun logout() {
        if (_uiState.value.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = authenticationRepository.logout()) {
                is AppResult.Success -> _uiState.value = AuthUiState(
                    isSessionLoading = false,
                )
                is AppResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    fun clearErrors() {
        _uiState.update { it.copy(formErrors = AuthFormErrors(), error = null) }
    }

    private fun submit(
        formErrors: AuthFormErrors,
        operation: suspend () -> AppResult<User>,
    ) {
        if (formErrors.hasErrors()) {
            _uiState.update { it.copy(formErrors = formErrors, error = null) }
            return
        }

        _uiState.update {
            it.copy(
                isSubmitting = true,
                formErrors = AuthFormErrors(),
                error = null,
            )
        }
        viewModelScope.launch {
            when (val result = operation()) {
                is AppResult.Success -> _uiState.value = AuthUiState(
                    currentUser = result.value,
                    isSessionLoading = false,
                )
                is AppResult.Failure -> showFailure(result.error)
            }
        }
    }

    private fun showFailure(error: AppError) {
        _uiState.update {
            if (error is AppError.Validation) {
                it.copy(
                    isSubmitting = false,
                    formErrors = error.toFormErrors(),
                )
            } else {
                it.copy(isSubmitting = false, error = error)
            }
        }
    }
}

private fun validateLogin(email: String, password: String) = AuthFormErrors(
    email = EmailAddress.from(email).validationReason(),
    password = password.requiredReason(),
)

private fun validateRegistration(
    displayName: String,
    email: String,
    password: String,
) = AuthFormErrors(
    displayName = DisplayName.from(displayName).validationReason(),
    email = EmailAddress.from(email).validationReason(),
    password = password.registrationPasswordReason(),
)

private fun AuthFormErrors.hasErrors(): Boolean =
    displayName != null || email != null || password != null

private fun AppError.Validation.toFormErrors(): AuthFormErrors = when (field) {
    "display name" -> AuthFormErrors(displayName = reason)
    "email" -> AuthFormErrors(email = reason)
    "password" -> AuthFormErrors(password = reason)
    else -> AuthFormErrors()
}

private fun AppResult<*>.validationReason(): String? =
    ((this as? AppResult.Failure)?.error as? AppError.Validation)?.reason

private fun String.requiredReason(): String? =
    if (isBlank()) "must not be blank" else null

private fun String.registrationPasswordReason(): String? = when {
    isBlank() -> "must not be blank"
    length < 6 -> "must be at least 6 characters"
    else -> null
}

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
