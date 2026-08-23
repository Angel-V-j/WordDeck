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

        viewModel.login(expectedUser.email!!.value, "secret1")
        advanceUntilIdle()
        assertEquals(expectedUser, viewModel.uiState.value.currentUser)

        viewModel.logout()
        advanceUntilIdle()
        assertEquals(1, repository.loginCalls)
        assertEquals(1, repository.logoutCalls)
        assertEquals(null, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `online login refreshes cloud data after authentication`() = runTest {
        val expectedUser = user()
        var synchronizedUser: User? = null
        val viewModel = AuthViewModel(
            authenticationRepository = FakeAuthenticationRepository(
                loginResult = AppResult.Success(expectedUser),
            ),
            refreshAfterOnlineAuthentication = { synchronizedUser = it },
        )
        advanceUntilIdle()

        viewModel.login("maria@example.com", "secret1")
        advanceUntilIdle()

        assertEquals(expectedUser, synchronizedUser)
    }

    @Test
    fun `linking an offline profile leaves the initial upload to the sync flow`() = runTest {
        val offlineUser = user().copy(email = null, firebaseUid = null)
        var synchronizedUser: User? = null
        val viewModel = AuthViewModel(
            authenticationRepository = FakeAuthenticationRepository(
                initialSession = AppResult.Success(offlineUser),
                registerResult = AppResult.Success(user()),
            ),
            refreshAfterOnlineAuthentication = { synchronizedUser = it },
        )
        advanceUntilIdle()

        viewModel.register("Maria", "maria@example.com", "secret1")
        advanceUntilIdle()

        assertEquals(null, synchronizedUser)
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

    @Test
    fun `registration service failure offers an offline profile using only the name`() = runTest {
        val repository = FakeAuthenticationRepository(
            registerResult = AppResult.Failure(AppError.NetworkUnavailable),
        )
        val viewModel = AuthViewModel(
            authenticationRepository = repository,
            networkAvailability = MutableStateFlow(true),
        )
        advanceUntilIdle()

        viewModel.register("Angel", "angel@example.com", "secret1")
        advanceUntilIdle()

        assertEquals(AuthFallback.REGISTRATION, viewModel.uiState.value.offlineFallback)
        assertEquals(
            AppError.Unavailable("authentication"),
            viewModel.uiState.value.error,
        )

        viewModel.continueOffline()
        advanceUntilIdle()

        assertEquals("Angel", repository.offlineDisplayName?.value)
        assertEquals(null, viewModel.uiState.value.currentUser?.email)
        assertEquals(null, viewModel.uiState.value.currentUser?.firebaseUid)
    }

    @Test
    fun `cancelling registration fallback stays unauthenticated`() = runTest {
        val repository = FakeAuthenticationRepository(
            registerResult = AppResult.Failure(AppError.Unavailable("registration")),
        )
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.register("Angel", "angel@example.com", "secret1")
        advanceUntilIdle()
        viewModel.dismissOfflineFallback()

        assertEquals(null, viewModel.uiState.value.offlineFallback)
        assertEquals(null, viewModel.uiState.value.currentUser)
        assertEquals(0, repository.createOfflineProfileCalls)
    }

    @Test
    fun `login service failure restores only an existing local profile`() = runTest {
        val storedUser = offlineUser("Stored user")
        val repository = FakeAuthenticationRepository(
            loginResult = AppResult.Failure(AppError.NetworkUnavailable),
            restoreResult = AppResult.Success(storedUser),
        )
        val viewModel = AuthViewModel(
            authenticationRepository = repository,
            networkAvailability = MutableStateFlow(true),
        )
        advanceUntilIdle()

        viewModel.login("unrelated@example.com", "not-stored")
        advanceUntilIdle()
        assertEquals(AuthFallback.LOGIN, viewModel.uiState.value.offlineFallback)

        viewModel.continueOffline()
        advanceUntilIdle()

        assertEquals(storedUser, viewModel.uiState.value.currentUser)
        assertEquals(1, repository.restoreLocalProfileCalls)
        assertEquals(0, repository.createOfflineProfileCalls)
    }

    @Test
    fun `login fallback without a stored profile is rejected`() = runTest {
        val repository = FakeAuthenticationRepository(
            loginResult = AppResult.Failure(AppError.Unavailable("login")),
            restoreResult = AppResult.Failure(
                AppError.Validation("local profile", "is not available"),
            ),
        )
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.login("maria@example.com", "secret1")
        advanceUntilIdle()
        viewModel.continueOffline()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.currentUser)
        assertEquals(null, viewModel.uiState.value.offlineFallback)
        assertEquals(
            AppError.Validation("local profile", "is not available"),
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun `actual network failure does not offer the Firebase unavailable fallback`() = runTest {
        val viewModel = AuthViewModel(
            authenticationRepository = FakeAuthenticationRepository(
                loginResult = AppResult.Failure(AppError.NetworkUnavailable),
            ),
            networkAvailability = MutableStateFlow(false),
        )
        advanceUntilIdle()

        viewModel.login("maria@example.com", "secret1")
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.offlineFallback)
        assertEquals(AppError.NetworkUnavailable, viewModel.uiState.value.error)
    }
}

private class FakeAuthenticationRepository(
    initialSession: AppResult<User?> = AppResult.Success(null),
    private val registerResult: AppResult<User> = AppResult.Success(user()),
    private val loginResult: AppResult<User> = AppResult.Success(user()),
    private val restoreResult: AppResult<User> = AppResult.Success(offlineUser()),
    private val logoutResult: AppResult<Unit> = AppResult.Success(Unit),
) : AuthenticationRepository {
    private val session = MutableStateFlow(initialSession)

    var registerCalls = 0
        private set
    var loginCalls = 0
        private set
    var logoutCalls = 0
        private set
    var createOfflineProfileCalls = 0
        private set
    var restoreLocalProfileCalls = 0
        private set
    var offlineDisplayName: DisplayName? = null
        private set

    override fun observeCurrentUser(): Flow<AppResult<User?>> = session

    override suspend fun createOfflineProfile(displayName: DisplayName): AppResult<User> {
        createOfflineProfileCalls += 1
        offlineDisplayName = displayName
        return AppResult.Success(offlineUser(displayName.value))
    }

    override suspend fun restoreLocalProfile(): AppResult<User> {
        restoreLocalProfileCalls += 1
        return restoreResult
    }

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

    override suspend fun reauthenticate(
        email: EmailAddress,
        password: String,
    ): AppResult<Unit> = AppResult.Success(Unit)

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

private fun offlineUser(displayName: String = "Maria") = User(
    id = UserId.from("local-user-1").successValue(),
    email = null,
    displayName = DisplayName.from(displayName).successValue(),
    firebaseUid = null,
)

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
