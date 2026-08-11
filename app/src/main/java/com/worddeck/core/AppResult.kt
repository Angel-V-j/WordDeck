package com.worddeck.core

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : AppResult<Nothing>
}
