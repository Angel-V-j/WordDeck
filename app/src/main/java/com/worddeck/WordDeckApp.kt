package com.worddeck

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worddeck.core.AppContainer
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository
import com.worddeck.domain.repository.ReviewRepository
import com.worddeck.feature.auth.AuthViewModel
import com.worddeck.feature.auth.AuthenticationFallbackDialog
import com.worddeck.feature.auth.SyncFlow
import com.worddeck.feature.auth.SyncViewModel
import com.worddeck.navigation.AppNavigation
import com.worddeck.ui.theme.WordDeckTheme

@Composable
fun WordDeckApp(appContainer: AppContainer) {
    val authViewModel = viewModel<AuthViewModel> {
        AuthViewModel(
            authenticationRepository = appContainer.authenticationRepository,
            networkAvailability = appContainer.networkAvailability,
            refreshAfterOnlineAuthentication = { user ->
                val firebaseUid = user.firebaseUid
                if (firebaseUid != null) {
                    appContainer.syncCoordinator.download(user.id, firebaseUid)
                }
            },
        )
    }
    val syncViewModel = viewModel<SyncViewModel> {
        SyncViewModel(
            authenticationRepository = appContainer.authenticationRepository,
            networkAvailability = appContainer.networkAvailability,
            downloadCloudData = { user ->
                val firebaseUid = user.firebaseUid
                if (firebaseUid == null) {
                    AppResult.Failure(AppError.Authentication.Unauthenticated)
                } else {
                    appContainer.syncCoordinator.downloadAndReplace(user.id, firebaseUid)
                }
            },
            uploadLocalChanges = { user ->
                val firebaseUid = user.firebaseUid
                if (firebaseUid == null) {
                    AppResult.Failure(AppError.Authentication.Unauthenticated)
                } else {
                    appContainer.syncCoordinator.forceUpload(user.id, firebaseUid)
                }
            },
        )
    }
    WordDeckContent(
        authViewModel = authViewModel,
        syncViewModel = syncViewModel,
        deckRepository = appContainer.deckRepository,
        flashcardRepository = appContainer.flashcardRepository,
        reviewRepository = appContainer.reviewRepository,
        idGenerator = appContainer.idGenerator,
        clock = appContainer.clock,
    )
}

@Composable
fun WordDeckContent(
    authViewModel: AuthViewModel,
    syncViewModel: SyncViewModel,
    deckRepository: DeckRepository,
    flashcardRepository: FlashcardRepository,
    reviewRepository: ReviewRepository,
    idGenerator: IdGenerator,
    clock: Clock,
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val syncUiState by syncViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authUiState.currentUser) {
        syncViewModel.updateUser(authUiState.currentUser)
    }

    WordDeckTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavigation(
                    uiState = authUiState,
                    deckRepository = deckRepository,
                    flashcardRepository = flashcardRepository,
                    reviewRepository = reviewRepository,
                    idGenerator = idGenerator,
                    clock = clock,
                    networkAvailable = syncUiState.networkAvailable,
                    onLogin = authViewModel::login,
                    onRegister = authViewModel::register,
                    onCreateOfflineProfile = authViewModel::createOfflineProfile,
                    onUpdateDisplayName = authViewModel::updateDisplayName,
                    onSync = syncViewModel::requestSync,
                    onDownload = syncViewModel::requestDownload,
                    onLogout = authViewModel::logout,
                    onClearErrors = authViewModel::clearErrors,
                    modifier = Modifier.fillMaxSize(),
                )
                AuthenticationFallbackDialog(
                    fallback = authUiState.offlineFallback,
                    onContinueOffline = authViewModel::continueOffline,
                    onCancel = authViewModel::dismissOfflineFallback,
                )
                SyncFlow(
                    syncUiState = syncUiState,
                    authUiState = authUiState,
                    user = authUiState.currentUser,
                    onRegister = authViewModel::register,
                    onReauthenticate = syncViewModel::reauthenticate,
                    onAcceptOffer = syncViewModel::acceptOffer,
                    onPostpone = syncViewModel::postpone,
                    onConfirm = syncViewModel::confirm,
                    onCancel = syncViewModel::cancel,
                    onClearAuthErrors = authViewModel::clearErrors,
                )
            }
        }
    }
}
