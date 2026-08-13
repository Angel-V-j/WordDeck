package com.worddeck.common

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data class Validation(val field: String, val reason: String) : AppError
    data class Unavailable(val resource: String) : AppError

    data object NetworkUnavailable : AppError

    sealed interface Authentication : AppError {
        data object InvalidCredentials : Authentication
        data object EmailAlreadyInUse : Authentication
        data object Unauthenticated : Authentication
    }
}
