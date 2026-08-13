package com.worddeck.domain.repository

import com.worddeck.common.AppResult
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {
    fun observeCurrentUser(): Flow<AppResult<User?>>

    suspend fun register(
        displayName: DisplayName,
        email: String,
        password: String,
    ): AppResult<User>

    suspend fun login(email: String, password: String): AppResult<User>

    suspend fun updateDisplayName(displayName: DisplayName): AppResult<User>

    suspend fun logout(): AppResult<Unit>
}
