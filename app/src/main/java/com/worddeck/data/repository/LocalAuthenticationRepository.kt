package com.worddeck.data.repository

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.IdGenerator
import com.worddeck.data.local.LocalUserStore
import com.worddeck.data.remote.firebase.FirebaseAuthService
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.EmailAddress
import com.worddeck.domain.model.User
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow

/** Uses the persisted local profile as the application session source. */
internal class LocalAuthenticationRepository(
    private val localUserStore: LocalUserStore,
    private val firebaseAuth: FirebaseAuthService,
    private val idGenerator: IdGenerator,
) : AuthenticationRepository {
    init {
        val storedUser = localUserStore.findStoredUser()
        if (storedUser is AppResult.Success && storedUser.value == null) {
            firebaseAuth.currentUser()?.let(localUserStore::save)
        }
    }

    override fun observeCurrentUser(): Flow<AppResult<User?>> =
        localUserStore.observeActiveUser()

    override suspend fun createOfflineProfile(displayName: DisplayName): AppResult<User> {
        val id = when (val result = UserId.from(idGenerator.generate())) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val user = User(
            id = id,
            email = null,
            displayName = displayName,
            firebaseUid = null,
        )
        return saveAndReturn(user)
    }

    override suspend fun restoreLocalProfile(): AppResult<User> {
        val storedUser = when (val result = localUserStore.findStoredUser()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        } ?: return AppResult.Failure(
            AppError.Validation("local profile", "is not available"),
        )

        return saveAndReturn(storedUser)
    }

    override suspend fun register(
        displayName: DisplayName,
        email: EmailAddress,
        password: String,
    ): AppResult<User> {
        val firebaseUser = when (val result = firebaseAuth.register(displayName, email, password)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }

        val activeUser = when (val result = localUserStore.findActiveUser()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val user = if (activeUser != null && !activeUser.isLinked) {
            activeUser.copy(
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                firebaseUid = firebaseUser.id,
            )
        } else {
            firebaseUser
        }
        return saveAndReturn(user)
    }

    override suspend fun login(
        email: EmailAddress,
        password: String,
    ): AppResult<User> {
        val firebaseUser = when (val result = firebaseAuth.login(email, password)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val storedUser = when (val result = localUserStore.findStoredUser()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val user = if (storedUser?.firebaseUid == firebaseUser.id) {
            storedUser.copy(
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
            )
        } else {
            firebaseUser
        }
        return saveAndReturn(user)
    }

    override suspend fun reauthenticate(
        email: EmailAddress,
        password: String,
    ): AppResult<Unit> {
        val localUser = when (val result = localUserStore.findActiveUser()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        } ?: return unauthenticated()
        val expectedUid = localUser.firebaseUid ?: return unauthenticated()

        val authenticatedUser = when (val result = firebaseAuth.login(email, password)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        if (authenticatedUser.id != expectedUid) {
            firebaseAuth.logout()
            return AppResult.Failure(AppError.Authentication.InvalidCredentials)
        }
        return AppResult.Success(Unit)
    }

    override suspend fun updateDisplayName(displayName: DisplayName): AppResult<User> {
        val localUser = when (val result = localUserStore.findActiveUser()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        } ?: return unauthenticated()

        if (localUser.isLinked) {
            when (val result = firebaseAuth.updateDisplayName(displayName)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return result
            }
        }
        return saveAndReturn(localUser.copy(displayName = displayName))
    }

    override suspend fun logout(): AppResult<Unit> {
        when (val result = firebaseAuth.logout()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return result
        }
        return localUserStore.deactivate()
    }

    private fun saveAndReturn(user: User): AppResult<User> =
        when (val result = localUserStore.save(user)) {
            is AppResult.Success -> AppResult.Success(user)
            is AppResult.Failure -> result
        }
}

private fun unauthenticated(): AppResult.Failure =
    AppResult.Failure(AppError.Authentication.Unauthenticated)
