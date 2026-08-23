package com.worddeck.feature.auth

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.EmailAddress
import com.worddeck.domain.model.User
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.AuthenticationRepository
import com.worddeck.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `restores the current session from the repository`() = runTest {
        val expectedUser = user()
        val viewModel = AuthViewModel(
            FakeAuthenticationRepository(AppResult.Success(expectedUser)),
        )

        advanceUntilIdle()

        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.sessionStatus)
        assertEquals(expectedUser, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `invalid login and registration forms do not call the repository`() = runTest {
        val repository = FakeAuthenticationRepository()
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.login("invalid-email", "")
        assertEquals("has invalid format", viewModel.uiState.value.formErrors.email)
        assertEquals("must not be blank", viewModel.uiState.value.formErrors.password)
        assertEquals(0, repository.loginCalls)

        viewModel.register(" ", "maria@example.com", "123")
        assertEquals("must not be blank", viewModel.uiState.value.formErrors.displayName)
        assertEquals("must be at least 6 characters", viewModel.uiState.value.formErrors.password)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `successful login updates the session and logout clears it`() = runTest {
        val expectedUser = user()
        val repository = FakeAuthenticationRepository(
            loginResult = AppResult.Success(expectedUser),
        )
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.login(expectedUser.email.value, "secret1")
        advanceUntilIdle()
        assertEquals(expectedUser, viewModel.uiState.value.currentUser)

        viewModel.logout()
        advanceUntilIdle()
        assertEquals(1, repository.loginCalls)
        assertEquals(1, repository.logoutCalls)
        assertEquals(null, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `repository failure is exposed to the UI`() = runTest {
        val error = AppError.Authentication.InvalidCredentials
        val viewModel = AuthViewModel(
            FakeAuthenticationRepository(loginResult = AppResult.Failure(error)),
        )
        advanceUntilIdle()

        viewModel.login("maria@example.com", "wrong-password")
        advanceUntilIdle()

        assertEquals(OperationStatus.ERROR, viewModel.uiState.value.submitStatus)
        assertEquals(error, viewModel.uiState.value.error)
    }
}

private class FakeAuthenticationRepository(
    initialSession: AppResult<User?> = AppResult.Success(null),
    private val registerResult: AppResult<User> = AppResult.Success(user()),
    private val loginResult: AppResult<User> = AppResult.Success(user()),
    private val logoutResult: AppResult<Unit> = AppResult.Success(Unit),
) : AuthenticationRepository {
    private val session = MutableStateFlow(initialSession)

    var registerCalls = 0
        private set
    var loginCalls = 0
        private set
    var logoutCalls = 0
        private set

    override fun observeCurrentUser(): Flow<AppResult<User?>> = session

    override suspend fun register(
        displayName: DisplayName,
        email: EmailAddress,
        password: String,
    ): AppResult<User> {
        registerCalls += 1
        return registerResult
    }

    override suspend fun login(email: EmailAddress, password: String): AppResult<User> {
        loginCalls += 1
        return loginResult
    }

    override suspend fun logout(): AppResult<Unit> {
        logoutCalls += 1
        return logoutResult
    }

    override suspend fun updateDisplayName(displayName: DisplayName): AppResult<User> =
        AppResult.Success(user(displayName.value))
}

private fun user(displayName: String = "Maria") = User(
    id = UserId.from("user-1").successValue(),
    email = EmailAddress.from("maria@example.com").successValue(),
    displayName = DisplayName.from(displayName).successValue(),
)

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
