package com.worddeck.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.worddeck.domain.model.User
import com.worddeck.feature.auth.AuthUiState
import com.worddeck.feature.auth.LoginScreen
import com.worddeck.feature.auth.ProfileScreen
import com.worddeck.feature.auth.RegisterScreen
import com.worddeck.feature.home.DeckDetailsScreen
import com.worddeck.feature.home.HomeScreen
import com.worddeck.feature.home.HomeUiState

object AppDestination {
    private const val DECK_ID = "deckId"

    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val HOME = "main/home"
    const val PROFILE = "main/profile"
    const val DECK_DETAILS = "main/decks/{$DECK_ID}"

    fun deckDetails(deckId: String): String = "main/decks/${Uri.encode(deckId)}"
}

@Composable
fun AppNavigation(
    uiState: AuthUiState,
    homeUiState: HomeUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (displayName: String, email: String, password: String) -> Unit,
    onUpdateDisplayName: (displayName: String) -> Unit,
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
            homeUiState = homeUiState,
            user = currentUser,
            onUpdateDisplayName = onUpdateDisplayName,
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
    homeUiState: HomeUiState,
    user: User,
    onUpdateDisplayName: (String) -> Unit,
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
                uiState = homeUiState,
                onDeckClick = { deckId ->
                    navController.navigate(AppDestination.deckDetails(deckId.value))
                },
                onOpenProfile = { navController.navigate(AppDestination.PROFILE) },
            )
        }
        composable(AppDestination.PROFILE) {
            ProfileScreen(
                user = user,
                isSubmitting = uiState.isSubmitting,
                displayNameError = uiState.formErrors.displayName,
                error = uiState.error,
                onUpdateDisplayName = onUpdateDisplayName,
                onLogout = onLogout,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = AppDestination.DECK_DETAILS,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val selectedId = backStackEntry.arguments?.getString("deckId")
            val deck = (homeUiState as? HomeUiState.Content)
                ?.decks
                ?.firstOrNull { it.id.value == selectedId }
            DeckDetailsScreen(
                deck = deck,
                onBack = { navController.popBackStack() },
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
