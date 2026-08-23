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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormErrors(
    val displayName: String? = null,
    val email: String? = null,
    val password: String? = null,
)

enum class AuthFallback {
    REGISTRATION,
    LOGIN,
}

data class AuthUiState(
    val currentUser: User? = null,
    val sessionStatus: OperationStatus = OperationStatus.LOADING,
    val submitStatus: OperationStatus = OperationStatus.IDLE,
    val formErrors: AuthFormErrors = AuthFormErrors(),
    val error: AppError? = null,
    val offlineFallback: AuthFallback? = null,
)

class AuthViewModel(
    private val authenticationRepository: AuthenticationRepository,
    private val refreshAfterOnlineAuthentication: suspend (User) -> Unit = {},
    networkAvailability: Flow<Boolean> = flowOf(true),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var networkAvailable = true
    // Offline registration keeps only the validated name; email and password are never retained.
    private var pendingOfflineDisplayName: DisplayName? = null

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
        viewModelScope.launch {
            networkAvailability.collect { networkAvailable = it }
        }
    }

    fun login(email: String, password: String) {
        if (_uiState.value.submitStatus == OperationStatus.LOADING) return

        val emailResult = EmailAddress.from(email)
        val formErrors = AuthFormErrors(
            email = emailResult.validationReason(),
            password = password.requiredReason(),
        )
        if (formErrors.hasErrors()) {
            showFormErrors(formErrors)
            return
        }

        val validEmail = when (emailResult) {
            is AppResult.Success -> emailResult.value
            is AppResult.Failure -> {
                showFailure(emailResult.error)
                return
            }
        }

        submit(
            synchronizeAfterSuccess = true,
            fallbackOnUnavailable = AuthFallback.LOGIN,
        ) {
            authenticationRepository.login(
                email = validEmail,
                password = password,
            )
        }
    }

    fun createOfflineProfile(displayName: String) {
        if (_uiState.value.submitStatus == OperationStatus.LOADING) return

        val displayNameResult = DisplayName.from(displayName)
        val validDisplayName = when (displayNameResult) {
            is AppResult.Success -> displayNameResult.value
            is AppResult.Failure -> {
                showFailure(displayNameResult.error)
                return
            }
        }
        submit { authenticationRepository.createOfflineProfile(validDisplayName) }
    }

    fun register(displayName: String, email: String, password: String) {
        if (_uiState.value.submitStatus == OperationStatus.LOADING) return

        val displayNameResult = DisplayName.from(displayName)
        val emailResult = EmailAddress.from(email)
        val formErrors = AuthFormErrors(
            displayName = displayNameResult.validationReason(),
            email = emailResult.validationReason(),
            password = password.registrationPasswordReason(),
        )
        if (formErrors.hasErrors()) {
            showFormErrors(formErrors)
            return
        }

        val validDisplayName = when (displayNameResult) {
            is AppResult.Success -> displayNameResult.value
            is AppResult.Failure -> {
                showFailure(displayNameResult.error)
                return
            }
        }
        val validEmail = when (emailResult) {
            is AppResult.Success -> emailResult.value
            is AppResult.Failure -> {
                showFailure(emailResult.error)
                return
            }
        }

        // A local-only profile is uploaded by the explicit sync flow after registration.
        // A normal online registration can use the regular two-way refresh.
        val isLinkingOfflineProfile = _uiState.value.currentUser?.isLinked == false
        submit(
            synchronizeAfterSuccess = !isLinkingOfflineProfile,
            fallbackOnUnavailable = if (isLinkingOfflineProfile) {
                null
            } else {
                AuthFallback.REGISTRATION
            },
            offlineDisplayName = validDisplayName,
        ) {
            authenticationRepository.register(
                displayName = validDisplayName,
                email = validEmail,
                password = password,
            )
        }
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
        val formErrors = AuthFormErrors(
            displayName = displayNameResult.validationReason(),
        )
        if (formErrors.hasErrors()) {
            showFormErrors(formErrors)
            return
        }

        val validDisplayName = when (displayNameResult) {
            is AppResult.Success -> displayNameResult.value
            is AppResult.Failure -> {
                showFailure(displayNameResult.error)
                return
            }
        }

        submit {
            authenticationRepository.updateDisplayName(validDisplayName)
        }
    }

    fun clearErrors() {
        pendingOfflineDisplayName = null
        _uiState.update {
            it.copy(
                submitStatus = OperationStatus.IDLE,
                formErrors = AuthFormErrors(),
                error = null,
                offlineFallback = null,
            )
        }
    }

    fun dismissOfflineFallback() {
        pendingOfflineDisplayName = null
        _uiState.update { it.copy(offlineFallback = null) }
    }

    fun continueOffline() {
        if (_uiState.value.submitStatus == OperationStatus.LOADING) return

        when (_uiState.value.offlineFallback) {
            AuthFallback.REGISTRATION -> {
                val displayName = pendingOfflineDisplayName ?: return
                pendingOfflineDisplayName = null
                submit { authenticationRepository.createOfflineProfile(displayName) }
            }
            AuthFallback.LOGIN -> {
                pendingOfflineDisplayName = null
                submit { authenticationRepository.restoreLocalProfile() }
            }
            null -> Unit
        }
    }

    private fun submit(
        synchronizeAfterSuccess: Boolean = false,
        fallbackOnUnavailable: AuthFallback? = null,
        offlineDisplayName: DisplayName? = null,
        operation: suspend () -> AppResult<User>,
    ) {
        _uiState.update {
            it.copy(
                submitStatus = OperationStatus.LOADING,
                formErrors = AuthFormErrors(),
                error = null,
                offlineFallback = null,
            )
        }
        viewModelScope.launch {
            when (val result = operation()) {
                is AppResult.Success -> {
                    _uiState.value = AuthUiState(
                        currentUser = result.value,
                        sessionStatus = OperationStatus.SUCCESS,
                        submitStatus = OperationStatus.SUCCESS,
                    )
                    if (synchronizeAfterSuccess) {
                        refreshAfterOnlineAuthentication(result.value)
                    }
                }
                is AppResult.Failure -> {
                    val error = result.error.asAuthenticationError(networkAvailable)
                    if (
                        fallbackOnUnavailable != null &&
                        networkAvailable &&
                        error is AppError.Unavailable
                    ) {
                        pendingOfflineDisplayName = offlineDisplayName
                        _uiState.update {
                            it.copy(
                                submitStatus = OperationStatus.ERROR,
                                error = error,
                                offlineFallback = fallbackOnUnavailable,
                            )
                        }
                    } else {
                        showFailure(error)
                    }
                }
            }
        }
    }

    private fun showFormErrors(formErrors: AuthFormErrors) {
        _uiState.update {
            it.copy(
                submitStatus = OperationStatus.ERROR,
                formErrors = formErrors,
                error = null,
            )
        }
    }

    private fun showFailure(error: AppError) {
        pendingOfflineDisplayName = null
        if (error == AppError.Authentication.Unauthenticated) {
            _uiState.value = AuthUiState(
                sessionStatus = OperationStatus.SUCCESS,
                submitStatus = OperationStatus.ERROR,
                error = error,
                offlineFallback = null,
            )
            return
        }

        val formErrors = (error as? AppError.Validation)?.toFormErrors()
        _uiState.update {
            if (formErrors != null) {
                it.copy(
                    submitStatus = OperationStatus.ERROR,
                    formErrors = formErrors,
                    error = null,
                    offlineFallback = null,
                )
            } else {
                it.copy(
                    submitStatus = OperationStatus.ERROR,
                    formErrors = AuthFormErrors(),
                    error = error,
                    offlineFallback = null,
                )
            }
        }
    }
}

/** Firebase can report a network exception even when Android still has validated internet. */
internal fun AppError.asAuthenticationError(networkAvailable: Boolean): AppError =
    if (networkAvailable && this == AppError.NetworkUnavailable) {
        AppError.Unavailable("authentication")
    } else {
        this
    }

private fun AuthFormErrors.hasErrors(): Boolean =
    displayName != null || email != null || password != null

private fun AppError.Validation.toFormErrors(): AuthFormErrors? = when (field) {
    "display name" -> AuthFormErrors(displayName = reason)
    "email" -> AuthFormErrors(email = reason)
    "password" -> AuthFormErrors(password = reason)
    else -> null
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
