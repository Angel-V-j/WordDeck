package com.worddeck.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.worddeck.domain.repository.FlashcardRepository
import com.worddeck.feature.auth.AuthUiState
import com.worddeck.feature.auth.LoginScreen
import com.worddeck.feature.auth.ProfileScreen
import com.worddeck.feature.auth.RegisterScreen
import com.worddeck.feature.decks.DeckEditorScreen
import com.worddeck.feature.decks.DeckUiState
import com.worddeck.feature.decks.DeckViewModel
import com.worddeck.feature.decks.FlashcardUiState
import com.worddeck.feature.decks.FlashcardViewModel
import com.worddeck.feature.home.DeckDetailsScreen
import com.worddeck.feature.home.HomeScreen
import com.worddeck.feature.home.HomeViewModel

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
    deckRepository: DeckRepository,
    flashcardRepository: FlashcardRepository,
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
            deckRepository = deckRepository,
            flashcardRepository = flashcardRepository,
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
    deckRepository: DeckRepository,
    flashcardRepository: FlashcardRepository,
    idGenerator: IdGenerator,
    clock: Clock,
    user: User,
    onUpdateDisplayName: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier,
) {
    val navController = rememberNavController()
    // The user ID in the key keeps one user's deck list separate from another user's session.
    val homeViewModel = viewModel<HomeViewModel>(key = "home-${user.id.value}") {
        HomeViewModel(
            deckRepository = deckRepository,
            ownerId = user.id,
        )
    }
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
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
                onSearchQueryChange = homeViewModel::updateSearchQuery,
                onCategoryFilterChange = homeViewModel::updateCategoryFilter,
                onLanguageFilterChange = homeViewModel::updateLanguageFilter,
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
            val latestDeck = homeUiState.decks.firstOrNull { it.id.value == selectedId }
            var deck by remember(selectedId) { mutableStateOf(latestDeck) }
            LaunchedEffect(latestDeck) {
                // Keep the last deck until this destination closes after a successful delete.
                if (latestDeck != null) deck = latestDeck
            }
            val currentDeck = deck
            if (currentDeck == null && homeUiState.status == OperationStatus.LOADING) {
                LoadingScreen(Modifier)
            } else if (currentDeck == null) {
                DeckDetailsScreen(
                    deck = null,
                    uiState = DeckUiState(),
                    flashcardUiState = FlashcardUiState(),
                    onEdit = {},
                    onDelete = {},
                    onSaveFlashcard = { _, _, _, _, _ -> },
                    onDeleteFlashcard = {},
                    onClearFlashcardOperation = {},
                    onFlashcardSearchQueryChange = {},
                    onBack = { navController.popBackStack() },
                )
            } else {
                val detailsViewModel = viewModel<DeckViewModel>(
                    key = "details-${currentDeck.id.value}",
                ) {
                    DeckViewModel(
                        deckRepository = deckRepository,
                        ownerId = user.id,
                        idGenerator = idGenerator,
                        clock = clock,
                        existingDeck = currentDeck,
                    )
                }
                val detailsState by detailsViewModel.uiState.collectAsStateWithLifecycle()
                val flashcardViewModel = viewModel<FlashcardViewModel>(
                    key = "cards-${currentDeck.id.value}",
                ) {
                    FlashcardViewModel(
                        flashcardRepository = flashcardRepository,
                        deckId = currentDeck.id,
                        idGenerator = idGenerator,
                        clock = clock,
                    )
                }
                val flashcardState by flashcardViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(detailsState.operationStatus) {
                    if (detailsState.operationStatus == OperationStatus.SUCCESS) {
                        navController.popBackStack()
                    }
                }
                DeckDetailsScreen(
                    deck = currentDeck,
                    uiState = detailsState,
                    flashcardUiState = flashcardState,
                    onEdit = {
                        navController.navigate(AppDestination.editDeck(currentDeck.id.value))
                    },
                    onDelete = detailsViewModel::delete,
                    onSaveFlashcard = flashcardViewModel::save,
                    onDeleteFlashcard = flashcardViewModel::delete,
                    onClearFlashcardOperation = flashcardViewModel::clearOperation,
                    onFlashcardSearchQueryChange = flashcardViewModel::updateSearchQuery,
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
            val latestDeck = homeUiState.decks.firstOrNull { it.id.value == selectedId }
            var deck by remember(selectedId) { mutableStateOf(latestDeck) }
            LaunchedEffect(latestDeck) {
                if (latestDeck != null) deck = latestDeck
            }
            val currentDeck = deck
            if (currentDeck == null && homeUiState.status == OperationStatus.LOADING) {
                LoadingScreen(Modifier)
            } else if (currentDeck == null) {
                DeckDetailsScreen(
                    deck = null,
                    uiState = DeckUiState(),
                    flashcardUiState = FlashcardUiState(),
                    onEdit = {},
                    onDelete = {},
                    onSaveFlashcard = { _, _, _, _, _ -> },
                    onDeleteFlashcard = {},
                    onClearFlashcardOperation = {},
                    onFlashcardSearchQueryChange = {},
                    onBack = { navController.popBackStack() },
                )
            } else {
                DeckEditorDestination(
                    key = "edit-${currentDeck.id.value}",
                    existingDeck = currentDeck,
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
