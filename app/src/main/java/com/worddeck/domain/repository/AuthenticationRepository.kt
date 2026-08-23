package com.worddeck.domain.repository

import com.worddeck.common.AppResult
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.EmailAddress
import com.worddeck.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {
    fun observeCurrentUser(): Flow<AppResult<User?>>

    suspend fun createOfflineProfile(displayName: DisplayName): AppResult<User>

    suspend fun restoreLocalProfile(): AppResult<User>

    suspend fun register(
        displayName: DisplayName,
        email: EmailAddress,
        password: String,
    ): AppResult<User>

    suspend fun login(email: EmailAddress, password: String): AppResult<User>

    suspend fun reauthenticate(email: EmailAddress, password: String): AppResult<Unit>

    suspend fun updateDisplayName(displayName: DisplayName): AppResult<User>

    suspend fun logout(): AppResult<Unit>
}
