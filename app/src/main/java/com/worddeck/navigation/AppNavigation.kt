package com.worddeck.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.User
import com.worddeck.domain.model.Deck
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.feature.auth.AuthUiState
import com.worddeck.feature.auth.LoginScreen
import com.worddeck.feature.auth.ProfileScreen
import com.worddeck.feature.auth.RegisterScreen
import com.worddeck.feature.decks.DeckEditorScreen
import com.worddeck.feature.decks.DeckUiState
import com.worddeck.feature.decks.DeckViewModel
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
    const val CREATE_DECK = "main/decks/create"
    const val EDIT_DECK = "main/decks/{$DECK_ID}/edit"

    fun deckDetails(deckId: String): String = "main/decks/${Uri.encode(deckId)}"

    fun editDeck(deckId: String): String = "main/decks/${Uri.encode(deckId)}/edit"
}

@Composable
fun AppNavigation(
    uiState: AuthUiState,
    homeUiState: HomeUiState,
    deckRepository: DeckRepository,
    idGenerator: IdGenerator,
    clock: Clock,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (displayName: String, email: String, password: String) -> Unit,
    onUpdateDisplayName: (displayName: String) -> Unit,
    onLogout: () -> Unit,
    onClearErrors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUser = uiState.currentUser
    when {
        uiState.sessionStatus == OperationStatus.LOADING -> LoadingScreen(modifier)
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
            deckRepository = deckRepository,
            idGenerator = idGenerator,
            clock = clock,
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
    deckRepository: DeckRepository,
    idGenerator: IdGenerator,
    clock: Clock,
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
                onCreateDeck = { navController.navigate(AppDestination.CREATE_DECK) },
                onOpenProfile = { navController.navigate(AppDestination.PROFILE) },
            )
        }
        composable(AppDestination.PROFILE) {
            ProfileScreen(
                user = user,
                isSubmitting = uiState.submitStatus == OperationStatus.LOADING,
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
            val deck = homeUiState.decks.firstOrNull { it.id.value == selectedId }
            if (deck == null) {
                DeckDetailsScreen(
                    deck = null,
                    uiState = DeckUiState(),
                    onEdit = {},
                    onDelete = {},
                    onBack = { navController.popBackStack() },
                )
            } else {
                val detailsViewModel = viewModel<DeckViewModel>(
                    key = "details-${deck.id.value}",
                ) {
                    DeckViewModel(
                        deckRepository = deckRepository,
                        ownerId = user.id,
                        idGenerator = idGenerator,
                        clock = clock,
                        existingDeck = deck,
                    )
                }
                val detailsState by detailsViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(detailsState.operationStatus) {
                    if (detailsState.operationStatus == OperationStatus.SUCCESS) {
                        navController.popBackStack()
                    }
                }
                DeckDetailsScreen(
                    deck = deck,
                    uiState = detailsState,
                    onEdit = {
                        navController.navigate(AppDestination.editDeck(deck.id.value))
                    },
                    onDelete = detailsViewModel::delete,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(AppDestination.CREATE_DECK) {
            DeckEditorDestination(
                key = "create-${user.id.value}",
                existingDeck = null,
                user = user,
                deckRepository = deckRepository,
                idGenerator = idGenerator,
                clock = clock,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = AppDestination.EDIT_DECK,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val selectedId = backStackEntry.arguments?.getString("deckId")
            val deck = homeUiState.decks.firstOrNull { it.id.value == selectedId }
            if (deck == null) {
                DeckDetailsScreen(
                    deck = null,
                    uiState = DeckUiState(),
                    onEdit = {},
                    onDelete = {},
                    onBack = { navController.popBackStack() },
                )
            } else {
                DeckEditorDestination(
                    key = "edit-${deck.id.value}",
                    existingDeck = deck,
                    user = user,
                    deckRepository = deckRepository,
                    idGenerator = idGenerator,
                    clock = clock,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun DeckEditorDestination(
    key: String,
    existingDeck: Deck?,
    user: User,
    deckRepository: DeckRepository,
    idGenerator: IdGenerator,
    clock: Clock,
    onBack: () -> Unit,
) {
    val editorViewModel = viewModel<DeckViewModel>(key = key) {
        DeckViewModel(
            deckRepository = deckRepository,
            ownerId = user.id,
            idGenerator = idGenerator,
            clock = clock,
            existingDeck = existingDeck,
        )
    }
    val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()
    DeckEditorScreen(
        uiState = editorState,
        onSave = editorViewModel::save,
        onSaved = onBack,
        onBack = onBack,
    )
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
