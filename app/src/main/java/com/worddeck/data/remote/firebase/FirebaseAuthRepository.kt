package com.worddeck.data.remote.firebase

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.User
import com.worddeck.domain.repository.AuthenticationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val EMAIL_FIELD = "email"
private const val PASSWORD_FIELD = "password"
private const val BLANK_REASON = "must not be blank"

internal class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
) : AuthenticationRepository {
    override fun observeCurrentUser(): Flow<AppResult<User?>> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val currentUser = auth.currentUser
            val result = if (currentUser == null) {
                AppResult.Success(null)
            } else {
                currentUser.toDomainUser()
                    ?.let { AppResult.Success(it) }
                    ?: unavailable("session")
            }
            trySend(result)
        }

        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun register(
        displayName: DisplayName,
        email: String,
        password: String,
    ): AppResult<User> {
        val normalizedEmail = email.trim()
        blankInputFailure(normalizedEmail, EMAIL_FIELD)?.let { return it }
        blankInputFailure(password, PASSWORD_FIELD)?.let { return it }

        return try {
            val firebaseUser = firebaseAuth
                .createUserWithEmailAndPassword(normalizedEmail, password)
                .await()
                .user
                ?: return unavailable("registration")

            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.value)
                    .build(),
            ).await()

            firebaseUser.toDomainUser()
                ?.let { AppResult.Success(it) }
                ?: unavailable("registration")
        } catch (exception: FirebaseException) {
            AppResult.Failure(exception.toRegistrationError())
        }
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        val normalizedEmail = email.trim()
        blankInputFailure(normalizedEmail, EMAIL_FIELD)?.let { return it }
        blankInputFailure(password, PASSWORD_FIELD)?.let { return it }

        return try {
            val firebaseUser = firebaseAuth
                .signInWithEmailAndPassword(normalizedEmail, password)
                .await()
                .user
                ?: return unavailable("login")

            firebaseUser.toDomainUser()
                ?.let { AppResult.Success(it) }
                ?: unavailable("login")
        } catch (exception: FirebaseException) {
            AppResult.Failure(exception.toLoginError())
        }
    }

    override suspend fun updateDisplayName(displayName: DisplayName): AppResult<User> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return AppResult.Failure(AppError.Authentication.Unauthenticated)

        return try {
            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.value)
                    .build(),
            ).await()

            firebaseUser.toDomainUser()
                ?.let { AppResult.Success(it) }
                ?: unavailable("profile")
        } catch (exception: FirebaseException) {
            AppResult.Failure(exception.toProfileError())
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        firebaseAuth.signOut()
        return AppResult.Success(Unit)
    }
}

private fun blankInputFailure(value: String, field: String): AppResult.Failure? =
    if (value.isBlank()) {
        AppResult.Failure(AppError.Validation(field, BLANK_REASON))
    } else {
        null
    }

private fun unavailable(resource: String): AppResult.Failure =
    AppResult.Failure(AppError.Unavailable(resource))
