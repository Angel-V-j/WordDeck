package com.worddeck.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.worddeck.feature.auth.AuthUiState
import com.worddeck.feature.auth.LoginScreen
import com.worddeck.feature.auth.RegisterScreen
import com.worddeck.feature.home.HomeScreen

object AppDestination {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val HOME = "main/home"
}

@Composable
fun AppNavigation(
    uiState: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (displayName: String, email: String, password: String) -> Unit,
    onLogout: () -> Unit,
    onClearErrors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUser = uiState.currentUser
    when {
        uiState.isSessionLoading -> LoadingScreen(modifier)
        currentUser == null -> AuthNavigation(
            uiState = uiState,
            onLogin = onLogin,
            onRegister = onRegister,
            onClearErrors = onClearErrors,
            modifier = modifier,
        )
        else -> MainNavigation(
            uiState = uiState,
            user = currentUser,
            onLogout = onLogout,
            modifier = modifier,
        )
    }
}

@Composable
private fun AuthNavigation(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onClearErrors: () -> Unit,
    modifier: Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppDestination.LOGIN,
        modifier = modifier,
    ) {
        composable(AppDestination.LOGIN) {
            LoginScreen(
                uiState = uiState,
                onLogin = onLogin,
                onOpenRegister = {
                    onClearErrors()
                    navController.navigate(AppDestination.REGISTER)
                },
            )
        }
        composable(AppDestination.REGISTER) {
            RegisterScreen(
                uiState = uiState,
                onRegister = onRegister,
                onOpenLogin = {
                    onClearErrors()
                    navController.popBackStack()
                },
            )
        }
    }
}

@Composable
private fun MainNavigation(
    uiState: AuthUiState,
    user: com.worddeck.domain.model.User,
    onLogout: () -> Unit,
    modifier: Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppDestination.HOME,
        modifier = modifier,
    ) {
        composable(AppDestination.HOME) {
            HomeScreen(
                user = user,
                isLoggingOut = uiState.isSubmitting,
                error = uiState.error,
                onLogout = onLogout,
            )
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
