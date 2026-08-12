package com.worddeck.domain.repository

import com.worddeck.common.AppResult
import com.worddeck.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {
    fun observeCurrentUser(): Flow<AppResult<User?>>

    suspend fun register(email: String, password: String): AppResult<User>

    suspend fun login(email: String, password: String): AppResult<User>

    suspend fun logout(): AppResult<Unit>
}
