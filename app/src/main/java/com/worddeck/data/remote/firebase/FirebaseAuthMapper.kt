package com.worddeck.data.remote.firebase

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.EmailAddress
import com.worddeck.domain.model.User
import com.worddeck.domain.model.UserId

private const val EMAIL_FIELD = "email"
private const val PASSWORD_FIELD = "password"
private const val INVALID_EMAIL_REASON = "has invalid format"
private const val WEAK_PASSWORD_REASON = "is too weak"

/** Converts Firebase-specific values and errors at the data/domain boundary. */
internal fun FirebaseUser.toDomainUser(): User? {
    val userId = UserId.from(uid).successValueOrNull() ?: return null
    val emailAddress = email
        ?.let(EmailAddress::from)
        ?.successValueOrNull()
        ?: return null
    val name = displayName
        ?.let(DisplayName::from)
        ?.successValueOrNull()
        ?: return null

    return User(
        id = userId,
        email = emailAddress,
        displayName = name,
    )
}

internal fun FirebaseException.toRegistrationError(): AppError = when (this) {
    is FirebaseNetworkException -> AppError.NetworkUnavailable
    is FirebaseAuthUserCollisionException -> AppError.Authentication.EmailAlreadyInUse
    is FirebaseAuthWeakPasswordException -> AppError.Validation(PASSWORD_FIELD, WEAK_PASSWORD_REASON)
    is FirebaseAuthInvalidCredentialsException -> AppError.Validation(EMAIL_FIELD, INVALID_EMAIL_REASON)
    is FirebaseTooManyRequestsException -> AppError.Unavailable("registration")
    else -> AppError.Unavailable("registration")
}

internal fun FirebaseException.toLoginError(): AppError = when (this) {
    is FirebaseNetworkException -> AppError.NetworkUnavailable
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException,
    -> AppError.Authentication.InvalidCredentials
    is FirebaseTooManyRequestsException -> AppError.Unavailable("login")
    else -> AppError.Unavailable("login")
}

internal fun FirebaseException.toProfileError(): AppError = when (this) {
    is FirebaseNetworkException -> AppError.NetworkUnavailable
    is FirebaseAuthInvalidUserException -> AppError.Authentication.Unauthenticated
    is FirebaseTooManyRequestsException -> AppError.Unavailable("profile")
    else -> AppError.Unavailable("profile")
}

private fun <T> AppResult<T>.successValueOrNull(): T? = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> null
}
