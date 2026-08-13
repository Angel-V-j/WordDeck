package com.worddeck.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.OperationStatus
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
    val sessionStatus: OperationStatus = OperationStatus.LOADING,
    val submitStatus: OperationStatus = OperationStatus.IDLE,
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
                            sessionStatus = OperationStatus.SUCCESS,
                            error = null,
                        )
                        is AppResult.Failure -> current.copy(
                            currentUser = null,
                            sessionStatus = OperationStatus.ERROR,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (_uiState.value.submitStatus == OperationStatus.LOADING) return

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
        if (_uiState.value.submitStatus == OperationStatus.LOADING) return

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
        if (_uiState.value.submitStatus == OperationStatus.LOADING) return

        _uiState.update { it.copy(submitStatus = OperationStatus.LOADING, error = null) }
        viewModelScope.launch {
            when (val result = authenticationRepository.logout()) {
                is AppResult.Success -> _uiState.value = AuthUiState(
                    sessionStatus = OperationStatus.SUCCESS,
                    submitStatus = OperationStatus.SUCCESS,
                )
                is AppResult.Failure -> _uiState.update {
                    it.copy(submitStatus = OperationStatus.ERROR, error = result.error)
                }
            }
        }
    }

    fun updateDisplayName(displayName: String) {
        if (_uiState.value.submitStatus == OperationStatus.LOADING) return

        val displayNameResult = DisplayName.from(displayName)
        submit(
            formErrors = AuthFormErrors(
                displayName = displayNameResult.validationReason(),
            ),
            operation = {
                authenticationRepository.updateDisplayName(
                    displayNameResult.successValue(),
                )
            },
        )
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                submitStatus = OperationStatus.IDLE,
                formErrors = AuthFormErrors(),
                error = null,
            )
        }
    }

    private fun submit(
        formErrors: AuthFormErrors,
        operation: suspend () -> AppResult<User>,
    ) {
        if (formErrors.hasErrors()) {
            _uiState.update {
                it.copy(
                    submitStatus = OperationStatus.ERROR,
                    formErrors = formErrors,
                    error = null,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                submitStatus = OperationStatus.LOADING,
                formErrors = AuthFormErrors(),
                error = null,
            )
        }
        viewModelScope.launch {
            when (val result = operation()) {
                is AppResult.Success -> _uiState.value = AuthUiState(
                    currentUser = result.value,
                    sessionStatus = OperationStatus.SUCCESS,
                    submitStatus = OperationStatus.SUCCESS,
                )
                is AppResult.Failure -> showFailure(result.error)
            }
        }
    }

    private fun showFailure(error: AppError) {
        if (error == AppError.Authentication.Unauthenticated) {
            _uiState.value = AuthUiState(
                sessionStatus = OperationStatus.SUCCESS,
                submitStatus = OperationStatus.ERROR,
                error = error,
            )
            return
        }

        _uiState.update {
            if (error is AppError.Validation) {
                it.copy(
                    submitStatus = OperationStatus.ERROR,
                    formErrors = error.toFormErrors(),
                )
            } else {
                it.copy(submitStatus = OperationStatus.ERROR, error = error)
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
