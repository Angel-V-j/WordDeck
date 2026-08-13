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
    fun `starts with loading session state`() = runTest {
        val viewModel = AuthViewModel(FakeAuthenticationRepository())

        assertEquals(OperationStatus.LOADING, viewModel.uiState.value.sessionStatus)
    }

    @Test
    fun `restores existing session from repository`() = runTest {
        val user = user()
        val repository = FakeAuthenticationRepository(
            initialSession = AppResult.Success(user),
        )
        val viewModel = AuthViewModel(repository)

        advanceUntilIdle()

        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.sessionStatus)
        assertEquals(user, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `empty session opens signed out flow`() = runTest {
        val viewModel = AuthViewModel(FakeAuthenticationRepository())

        advanceUntilIdle()

        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.sessionStatus)
        assertEquals(null, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `login validates input before calling repository`() = runTest {
        val repository = FakeAuthenticationRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login(email = "invalid-email", password = "")

        assertEquals("has invalid format", viewModel.uiState.value.formErrors.email)
        assertEquals("must not be blank", viewModel.uiState.value.formErrors.password)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `register validates required display name`() = runTest {
        val repository = FakeAuthenticationRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.register(displayName = " ", email = "maria@example.com", password = "secret1")

        assertEquals("must not be blank", viewModel.uiState.value.formErrors.displayName)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `register validates password length before calling repository`() = runTest {
        val repository = FakeAuthenticationRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.register(
            displayName = "Maria",
            email = "maria@example.com",
            password = "123",
        )

        assertEquals(
            "must be at least 6 characters",
            viewModel.uiState.value.formErrors.password,
        )
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `login exposes loading then signed in state`() = runTest {
        val user = user()
        val repository = FakeAuthenticationRepository(loginResult = AppResult.Success(user))
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.login(email = user.email.value, password = "secret1")

        assertEquals(OperationStatus.LOADING, viewModel.uiState.value.submitStatus)
        advanceUntilIdle()
        assertEquals(OperationStatus.SUCCESS, viewModel.uiState.value.submitStatus)
        assertEquals(user, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `repository failure is exposed to the UI`() = runTest {
        val error = AppError.Authentication.InvalidCredentials
        val repository = FakeAuthenticationRepository(loginResult = AppResult.Failure(error))
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.login(email = "maria@example.com", password = "wrong-password")
        advanceUntilIdle()

        assertEquals(OperationStatus.ERROR, viewModel.uiState.value.submitStatus)
        assertEquals(error, viewModel.uiState.value.error)
        assertEquals(null, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `logout clears signed in state`() = runTest {
        val repository = FakeAuthenticationRepository(initialSession = AppResult.Success(user()))
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, repository.logoutCalls)
        assertEquals(null, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `blank display name is rejected before profile update`() = runTest {
        val repository = FakeAuthenticationRepository(initialSession = AppResult.Success(user()))
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.updateDisplayName("   ")

        assertEquals("must not be blank", viewModel.uiState.value.formErrors.displayName)
        assertEquals(0, repository.updateDisplayNameCalls)
    }

    @Test
    fun `profile update replaces current user`() = runTest {
        val updatedUser = user("Maria Petrova")
        val repository = FakeAuthenticationRepository(
            initialSession = AppResult.Success(user()),
            updateDisplayNameResult = AppResult.Success(updatedUser),
        )
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.updateDisplayName("  Maria Petrova  ")
        advanceUntilIdle()

        assertEquals("Maria Petrova", repository.updatedDisplayName?.value)
        assertEquals(updatedUser, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `unauthenticated profile update returns to signed out state`() = runTest {
        val repository = FakeAuthenticationRepository(
            initialSession = AppResult.Success(user()),
            updateDisplayNameResult = AppResult.Failure(
                AppError.Authentication.Unauthenticated,
            ),
        )
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.updateDisplayName("Maria Petrova")
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.currentUser)
        assertEquals(
            AppError.Authentication.Unauthenticated,
            viewModel.uiState.value.error,
        )
    }
}

private class FakeAuthenticationRepository(
    initialSession: AppResult<User?> = AppResult.Success(null),
    private val registerResult: AppResult<User> = AppResult.Success(user()),
    private val loginResult: AppResult<User> = AppResult.Success(user()),
    private val logoutResult: AppResult<Unit> = AppResult.Success(Unit),
    private val updateDisplayNameResult: AppResult<User> = AppResult.Success(user()),
) : AuthenticationRepository {
    private val session = MutableStateFlow(initialSession)

    var registerCalls = 0
        private set
    var loginCalls = 0
        private set
    var logoutCalls = 0
        private set
    var updateDisplayNameCalls = 0
        private set
    var updatedDisplayName: DisplayName? = null
        private set

    override fun observeCurrentUser(): Flow<AppResult<User?>> = session

    override suspend fun register(
        displayName: DisplayName,
        email: String,
        password: String,
    ): AppResult<User> {
        registerCalls += 1
        return registerResult
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        loginCalls += 1
        return loginResult
    }

    override suspend fun logout(): AppResult<Unit> {
        logoutCalls += 1
        return logoutResult
    }

    override suspend fun updateDisplayName(displayName: DisplayName): AppResult<User> {
        updateDisplayNameCalls += 1
        updatedDisplayName = displayName
        return updateDisplayNameResult
    }
}

private fun user(displayName: String = "Maria"): User = User(
    id = UserId.from("user-1").successValue(),
    email = EmailAddress.from("maria@example.com").successValue(),
    displayName = DisplayName.from(displayName).successValue(),
)

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
