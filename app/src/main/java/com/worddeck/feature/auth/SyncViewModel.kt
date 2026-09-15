package com.worddeck.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.EmailAddress
import com.worddeck.domain.model.User
import com.worddeck.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncStep {
    IDLE,
    OFFER,
    REGISTRATION,
    REAUTHENTICATION,
    CONFIRMATION,
    SYNCING,
    SUCCESS,
    ERROR,
}

data class SyncUiState(
    val step: SyncStep = SyncStep.IDLE,
    val networkAvailable: Boolean? = null,
    val operationStatus: OperationStatus = OperationStatus.IDLE,
    val emailError: String? = null,
    val passwordError: String? = null,
    val error: AppError? = null,
)

class SyncViewModel(
    private val authenticationRepository: AuthenticationRepository,
    networkAvailability: Flow<Boolean>,
    private val uploadLocalChanges: suspend (User) -> AppResult<Unit>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null
    private var previousNetworkAvailability: Boolean? = null

    init {
        viewModelScope.launch {
            networkAvailability.collect { isAvailable ->
                val connectionWasRestored = previousNetworkAvailability == false && isAvailable
                previousNetworkAvailability = isAvailable
                val currentState = _uiState.value
                _uiState.value = currentState.copy(networkAvailable = isAvailable)

                if (
                    connectionWasRestored &&
                    currentUser != null &&
                    currentState.step == SyncStep.IDLE
                ) {
                    _uiState.value = _uiState.value.copy(step = SyncStep.OFFER)
                }
            }
        }
    }

    fun updateUser(user: User?) {
        val wasWaitingForRegistration = _uiState.value.step == SyncStep.REGISTRATION
        currentUser = user

        if (user == null) {
            _uiState.value = SyncUiState(
                networkAvailable = _uiState.value.networkAvailable,
            )
        } else if (wasWaitingForRegistration && user.isLinked) {
            startUpload(user)
        }
    }

    fun requestSync() {
        val user = currentUser ?: return
        if (_uiState.value.networkAvailable == false) {
            _uiState.value = _uiState.value.copy(
                step = SyncStep.ERROR,
                error = AppError.NetworkUnavailable,
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            step = if (user.isLinked) SyncStep.REAUTHENTICATION else SyncStep.REGISTRATION,
            operationStatus = OperationStatus.IDLE,
            emailError = null,
            passwordError = null,
            error = null,
        )
    }

    fun acceptOffer() = requestSync()

    fun postpone() {
        reset()
    }

    fun reauthenticate(email: String, password: String) {
        if (_uiState.value.step != SyncStep.REAUTHENTICATION) return
        if (_uiState.value.operationStatus == OperationStatus.LOADING) return

        val emailResult = EmailAddress.from(email)
        val emailError = when (emailResult) {
            is AppResult.Success -> null
            is AppResult.Failure -> (emailResult.error as? AppError.Validation)?.reason
        }
        val passwordError = if (password.isBlank()) "must not be blank" else null
        if (emailError != null || passwordError != null) {
            _uiState.value = _uiState.value.copy(
                operationStatus = OperationStatus.ERROR,
                emailError = emailError,
                passwordError = passwordError,
                error = null,
            )
            return
        }

        val validEmail = when (emailResult) {
            is AppResult.Success -> emailResult.value
            is AppResult.Failure -> return
        }
        _uiState.value = _uiState.value.copy(
            operationStatus = OperationStatus.LOADING,
            emailError = null,
            passwordError = null,
            error = null,
        )
        viewModelScope.launch {
            when (val result = authenticationRepository.reauthenticate(validEmail, password)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    step = SyncStep.CONFIRMATION,
                    operationStatus = OperationStatus.IDLE,
                )
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(
                    operationStatus = OperationStatus.ERROR,
                    error = result.error.asAuthenticationError(
                        _uiState.value.networkAvailable == true,
                    ),
                )
            }
        }
    }

    fun confirm() {
        if (_uiState.value.step != SyncStep.CONFIRMATION) return
        currentUser?.let(::startUpload)
    }

    fun cancel() {
        reset()
    }

    private fun startUpload(user: User) {
        if (!user.isLinked) return
        _uiState.value = _uiState.value.copy(
            step = SyncStep.SYNCING,
            operationStatus = OperationStatus.LOADING,
            error = null,
        )
        viewModelScope.launch {
            when (val result = uploadLocalChanges(user)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    step = SyncStep.SUCCESS,
                    operationStatus = OperationStatus.SUCCESS,
                )
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(
                    step = SyncStep.ERROR,
                    operationStatus = OperationStatus.ERROR,
                    error = result.error,
                )
            }
        }
    }

    private fun reset() {
        _uiState.value = SyncUiState(
            networkAvailable = _uiState.value.networkAvailable,
        )
    }
}
