package com.worddeck

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worddeck.core.AppContainer
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository
import com.worddeck.feature.auth.AuthViewModel
import com.worddeck.feature.home.HomeUiState
import com.worddeck.feature.home.HomeViewModel
import com.worddeck.navigation.AppNavigation
import com.worddeck.ui.theme.WordDeckTheme

@Composable
fun WordDeckApp(appContainer: AppContainer) {
    val authViewModel = viewModel<AuthViewModel> {
        AuthViewModel(appContainer.authenticationRepository)
    }
    WordDeckContent(
        authViewModel = authViewModel,
        deckRepository = appContainer.deckRepository,
        flashcardRepository = appContainer.flashcardRepository,
        idGenerator = appContainer.idGenerator,
        clock = appContainer.clock,
    )
}

@Composable
fun WordDeckContent(
    authViewModel: AuthViewModel,
    deckRepository: DeckRepository,
    flashcardRepository: FlashcardRepository,
    idGenerator: IdGenerator,
    clock: Clock,
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = uiState.currentUser
    val homeUiState = if (currentUser == null) {
        HomeUiState()
    } else {
        // A new user must not reuse the previous user's deck-list ViewModel.
        val homeViewModel = viewModel<HomeViewModel>(key = "home-${currentUser.id.value}") {
            HomeViewModel(
                deckRepository = deckRepository,
                ownerId = currentUser.id,
            )
        }
        val state by homeViewModel.uiState.collectAsStateWithLifecycle()
        state
    }

    WordDeckTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation(
                uiState = uiState,
                homeUiState = homeUiState,
                deckRepository = deckRepository,
                flashcardRepository = flashcardRepository,
                idGenerator = idGenerator,
                clock = clock,
                onLogin = authViewModel::login,
                onRegister = authViewModel::register,
                onUpdateDisplayName = authViewModel::updateDisplayName,
                onLogout = authViewModel::logout,
                onClearErrors = authViewModel::clearErrors,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
