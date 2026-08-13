package com.worddeck

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worddeck.core.AppContainer
import com.worddeck.feature.auth.AuthViewModel
import com.worddeck.navigation.AppNavigation
import com.worddeck.ui.theme.WordDeckTheme

@Composable
fun WordDeckApp(appContainer: AppContainer) {
    val authViewModel = viewModel<AuthViewModel> {
        AuthViewModel(appContainer.authenticationRepository)
    }
    WordDeckContent(authViewModel)
}

@Composable
fun WordDeckContent(authViewModel: AuthViewModel) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    WordDeckTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation(
                uiState = uiState,
                onLogin = authViewModel::login,
                onRegister = authViewModel::register,
                onLogout = authViewModel::logout,
                onClearErrors = authViewModel::clearErrors,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
