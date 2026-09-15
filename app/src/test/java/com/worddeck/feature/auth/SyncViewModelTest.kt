package com.worddeck.feature.auth

import com.worddeck.common.AppError
import com.worddeck.common.AppResult
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
class SyncViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `network recovery offers sync but does not start it automatically`() = runTest {
        val network = MutableStateFlow(false)
        var syncCalls = 0
        val viewModel = SyncViewModel(FakeAuthRepository(), network, downloadCloudData = { AppResult.Success(Unit) }) {
            syncCalls += 1
            AppResult.Success(Unit)
        }
        viewModel.updateUser(linkedUser())
        advanceUntilIdle()

        network.value = true
        advanceUntilIdle()

        assertEquals(SyncStep.OFFER, viewModel.uiState.value.step)
        assertEquals(0, syncCalls)
        viewModel.postpone()
        assertEquals(SyncStep.IDLE, viewModel.uiState.value.step)
        assertEquals(0, syncCalls)
    }

    @Test
    fun `local profile is sent to registration and syncs only after account linking`() = runTest {
        var syncedUser: User? = null
        val viewModel = SyncViewModel(FakeAuthRepository(), MutableStateFlow(true), downloadCloudData = { AppResult.Success(Unit) }) { user ->
            syncedUser = user
            AppResult.Success(Unit)
        }
        viewModel.updateUser(localUser())
        advanceUntilIdle()

        viewModel.requestSync()
        assertEquals(SyncStep.REGISTRATION, viewModel.uiState.value.step)
        assertEquals(null, syncedUser)

        val linked = linkedUser(localId = localUser().id)
        viewModel.updateUser(linked)
        advanceUntilIdle()

        assertEquals(linked, syncedUser)
        assertEquals(SyncStep.SUCCESS, viewModel.uiState.value.step)
    }

    @Test
    fun `linked profile authenticates before confirmation and cancellation does not sync`() = runTest {
        val auth = FakeAuthRepository()
        var syncCalls = 0
        val viewModel = SyncViewModel(auth, MutableStateFlow(true), downloadCloudData = { AppResult.Success(Unit) }) {
            syncCalls += 1
            AppResult.Success(Unit)
        }
        viewModel.updateUser(linkedUser())
        advanceUntilIdle()

        viewModel.requestSync()
        assertEquals(SyncStep.REAUTHENTICATION, viewModel.uiState.value.step)
        viewModel.reauthenticate("maria@example.com", "valid-password")
        advanceUntilIdle()

        assertEquals(1, auth.reauthenticateCalls)
        assertEquals(SyncStep.CONFIRMATION, viewModel.uiState.value.step)
        viewModel.cancel()
        assertEquals(SyncStep.IDLE, viewModel.uiState.value.step)
        assertEquals(0, syncCalls)
    }

    @Test
    fun `failed authentication or upload never reports successful synchronization`() = runTest {
        val auth = FakeAuthRepository(
            reauthenticateResult = AppResult.Failure(AppError.Authentication.InvalidCredentials),
        )
        var syncCalls = 0
        val viewModel = SyncViewModel(auth, MutableStateFlow(true), downloadCloudData = { AppResult.Success(Unit) }) {
            syncCalls += 1
            AppResult.Failure(AppError.NetworkUnavailable)
        }
        viewModel.updateUser(linkedUser())
        advanceUntilIdle()
        viewModel.requestSync()
        viewModel.reauthenticate("maria@example.com", "wrong-password")
        advanceUntilIdle()

        assertEquals(SyncStep.REAUTHENTICATION, viewModel.uiState.value.step)
        assertEquals(AppError.Authentication.InvalidCredentials, viewModel.uiState.value.error)
        assertEquals(0, syncCalls)

        auth.reauthenticateResult = AppResult.Success(Unit)
        viewModel.reauthenticate("maria@example.com", "valid-password")
        advanceUntilIdle()
        viewModel.confirm()
        advanceUntilIdle()

        assertEquals(1, syncCalls)
        assertEquals(SyncStep.ERROR, viewModel.uiState.value.step)
        assertEquals(AppError.NetworkUnavailable, viewModel.uiState.value.error)
    }

    @Test
    fun `available network reports unavailable authentication service without syncing`() = runTest {
        val auth = FakeAuthRepository(
            reauthenticateResult = AppResult.Failure(AppError.NetworkUnavailable),
        )
        var syncCalls = 0
        val viewModel = SyncViewModel(auth, MutableStateFlow(true), downloadCloudData = { AppResult.Success(Unit) }) {
            syncCalls += 1
            AppResult.Success(Unit)
        }
        viewModel.updateUser(linkedUser())
        advanceUntilIdle()

        viewModel.requestSync()
        viewModel.reauthenticate("maria@example.com", "valid-password")
        advanceUntilIdle()

        assertEquals(SyncStep.REAUTHENTICATION, viewModel.uiState.value.step)
        assertEquals(AppError.Unavailable("authentication"), viewModel.uiState.value.error)
        assertEquals(0, syncCalls)
    }
    @Test
    fun `download requires authentication and confirmation and cannot also upload`() = runTest {
        val auth = FakeAuthRepository()
        var downloads = 0
        var uploads = 0
        val completion = kotlinx.coroutines.CompletableDeferred<AppResult<Unit>>()
        val viewModel = SyncViewModel(
            auth,
            MutableStateFlow(true),
            downloadCloudData = {
                downloads++
                completion.await()
            },
            uploadLocalChanges = {
                uploads++
                AppResult.Success(Unit)
            },
        )
        viewModel.updateUser(linkedUser())
        advanceUntilIdle()

        viewModel.requestDownload()
        viewModel.confirm()
        assertEquals(0, downloads)
        assertEquals(SyncStep.REAUTHENTICATION, viewModel.uiState.value.step)
        viewModel.reauthenticate("maria@example.com", "test-password")
        advanceUntilIdle()
        assertEquals(SyncStep.CONFIRMATION, viewModel.uiState.value.step)
        viewModel.cancel()
        assertEquals(0, downloads)

        viewModel.requestDownload()
        viewModel.reauthenticate("maria@example.com", "test-password")
        advanceUntilIdle()
        viewModel.confirm()
        advanceUntilIdle()
        viewModel.confirm()
        viewModel.requestSync()
        viewModel.requestDownload()
        viewModel.cancel()
        assertEquals(SyncStep.SYNCING, viewModel.uiState.value.step)
        assertEquals(1, downloads)
        assertEquals(0, uploads)

        completion.complete(AppResult.Success(Unit))
        advanceUntilIdle()
        assertEquals(SyncStep.SUCCESS, viewModel.uiState.value.step)
        assertEquals(SyncDirection.DOWNLOAD, viewModel.uiState.value.direction)
    }

    @Test
    fun `download rejects local only offline and failed authentication`() = runTest {
        val auth = FakeAuthRepository(
            reauthenticateResult = AppResult.Failure(AppError.Authentication.InvalidCredentials),
        )
        val network = MutableStateFlow(true)
        var downloads = 0
        val viewModel = SyncViewModel(
            auth, network,
            downloadCloudData = { downloads++; AppResult.Success(Unit) },
            uploadLocalChanges = { AppResult.Success(Unit) },
        )
        advanceUntilIdle()
        viewModel.updateUser(localUser())
        viewModel.requestDownload()
        assertEquals(SyncStep.ERROR, viewModel.uiState.value.step)
        assertEquals(0, auth.reauthenticateCalls)

        viewModel.cancel()
        viewModel.updateUser(linkedUser())
        network.value = false
        advanceUntilIdle()
        viewModel.requestDownload()
        assertEquals(AppError.NetworkUnavailable, viewModel.uiState.value.error)

        viewModel.cancel()
        network.value = true
        advanceUntilIdle()
        viewModel.requestDownload()
        viewModel.reauthenticate("maria@example.com", "test-password")
        advanceUntilIdle()
        viewModel.confirm()
        assertEquals(AppError.Authentication.InvalidCredentials, viewModel.uiState.value.error)
        assertEquals(0, downloads)
    }

    @Test
    fun `failed download stays retryable and changing user invalidates confirmation`() = runTest {
        var downloads = 0
        val viewModel = SyncViewModel(
            FakeAuthRepository(), MutableStateFlow(true),
            downloadCloudData = { downloads++; AppResult.Failure(AppError.Unavailable("synchronization")) },
            uploadLocalChanges = { AppResult.Success(Unit) },
        )
        viewModel.updateUser(linkedUser())
        advanceUntilIdle()
        viewModel.requestDownload()
        viewModel.reauthenticate("maria@example.com", "test-password")
        advanceUntilIdle()
        viewModel.updateUser(linkedUser(UserId.from("different-local-user").successValue()))
        viewModel.confirm()
        assertEquals(0, downloads)

        viewModel.requestDownload()
        viewModel.reauthenticate("maria@example.com", "test-password")
        advanceUntilIdle()
        viewModel.confirm()
        advanceUntilIdle()
        assertEquals(1, downloads)
        assertEquals(SyncStep.ERROR, viewModel.uiState.value.step)
    }

}

private class FakeAuthRepository(
    var reauthenticateResult: AppResult<Unit> = AppResult.Success(Unit),
) : AuthenticationRepository {
    private val session = MutableStateFlow<AppResult<User?>>(AppResult.Success(null))
    var reauthenticateCalls = 0

    override fun observeCurrentUser(): Flow<AppResult<User?>> = session
    override suspend fun createOfflineProfile(displayName: DisplayName): AppResult<User> =
        AppResult.Success(localUser())
    override suspend fun restoreLocalProfile(): AppResult<User> = AppResult.Success(localUser())
    override suspend fun register(
        displayName: DisplayName,
        email: EmailAddress,
        password: String,
    ): AppResult<User> = AppResult.Success(linkedUser())
    override suspend fun login(email: EmailAddress, password: String): AppResult<User> =
        AppResult.Success(linkedUser())
    override suspend fun reauthenticate(
        email: EmailAddress,
        password: String,
    ): AppResult<Unit> {
        reauthenticateCalls += 1
        return reauthenticateResult
    }
    override suspend fun updateDisplayName(displayName: DisplayName): AppResult<User> =
        AppResult.Success(linkedUser().copy(displayName = displayName))
    override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
}

private fun localUser(): User = User(
    id = UserId.from("local-user-1").successValue(),
    email = null,
    displayName = DisplayName.from("Maria").successValue(),
    firebaseUid = null,
)

private fun linkedUser(localId: UserId = UserId.from("local-user-1").successValue()): User = User(
    id = localId,
    email = EmailAddress.from("maria@example.com").successValue(),
    displayName = DisplayName.from("Maria").successValue(),
    firebaseUid = UserId.from("firebase-user-1").successValue(),
)

private fun <T> AppResult<T>.successValue(): T = (this as AppResult.Success).value
