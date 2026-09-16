package com.worddeck.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.worddeck.common.Clock
import com.worddeck.common.AppResult
import com.worddeck.common.IdGenerator
import com.worddeck.common.OperationStatus
import com.worddeck.R
import com.worddeck.domain.model.User
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.startStudySession
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository
import com.worddeck.domain.repository.ReviewRepository
import com.worddeck.feature.auth.AuthUiState
import com.worddeck.feature.auth.LoginScreen
import com.worddeck.feature.auth.OfflineProfileScreen
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
import com.worddeck.feature.study.StudyScreen
import com.worddeck.feature.study.StudyMode
import com.worddeck.feature.study.ReviewFlashcardUseCase
import com.worddeck.feature.study.ReviewHistoryScreen
import com.worddeck.feature.study.StudyUiState
import com.worddeck.feature.study.StudyViewModel
import com.worddeck.feature.statistics.StatisticsScreen
import com.worddeck.feature.statistics.StatisticsUiState
import com.worddeck.feature.statistics.StatisticsViewModel

private const val STUDY_MODE_ARGUMENT = "studyMode"

object AppDestination {
    private const val DECK_ID = "deckId"
    private const val CARD_ID = "cardId"

    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val HOME = "main/home"
    const val PROFILE = "main/profile"
    const val STATISTICS = "main/statistics"
    const val DECK_DETAILS = "main/decks/{$DECK_ID}"
    const val STUDY_DECK = "main/decks/{$DECK_ID}/study?$STUDY_MODE_ARGUMENT={$STUDY_MODE_ARGUMENT}"
    const val CARD_HISTORY = "main/cards/{$CARD_ID}/history"
    const val CREATE_DECK = "main/decks/create"
    const val EDIT_DECK = "main/decks/{$DECK_ID}/edit"
    const val DECK_STATISTICS = "main/decks/{$DECK_ID}/statistics"

    fun deckDetails(deckId: String): String = "main/decks/${Uri.encode(deckId)}"

    fun studyDeck(
        deckId: String,
        mode: StudyMode = StudyMode.FLASHCARD,
    ): String = "main/decks/${Uri.encode(deckId)}/study?$STUDY_MODE_ARGUMENT=${mode.name}"

    fun editDeck(deckId: String): String = "main/decks/${Uri.encode(deckId)}/edit"

    fun cardHistory(cardId: String): String = "main/cards/${Uri.encode(cardId)}/history"

    fun deckStatistics(deckId: String): String = "main/decks/${Uri.encode(deckId)}/statistics"
}

@Composable
fun AppNavigation(
    uiState: AuthUiState,
    deckRepository: DeckRepository,
    flashcardRepository: FlashcardRepository,
    reviewRepository: ReviewRepository,
    idGenerator: IdGenerator,
    clock: Clock,
    networkAvailable: Boolean?,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (displayName: String, email: String, password: String, confirmPassword: String) -> Unit,
    onCreateOfflineProfile: (displayName: String) -> Unit,
    onUpdateDisplayName: (displayName: String) -> Unit,
    onSync: () -> Unit,
    onDownload: () -> Unit,
    onLogout: () -> Unit,
    onClearErrors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUser = uiState.currentUser
    when {
        uiState.sessionStatus == OperationStatus.LOADING -> LoadingScreen(modifier)
        currentUser == null && networkAvailable == null -> LoadingScreen(modifier)
        currentUser == null && networkAvailable == false -> OfflineProfileScreen(
            uiState = uiState,
            onContinue = onCreateOfflineProfile,
            modifier = modifier,
        )
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
            reviewRepository = reviewRepository,
            idGenerator = idGenerator,
            clock = clock,
            user = currentUser,
            onUpdateDisplayName = onUpdateDisplayName,
            onSync = onSync,
            onDownload = onDownload,
            onLogout = onLogout,
            modifier = modifier,
        )
    }
}

@Composable
private fun AuthNavigation(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
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
    reviewRepository: ReviewRepository,
    idGenerator: IdGenerator,
    clock: Clock,
    user: User,
    onUpdateDisplayName: (String) -> Unit,
    onSync: () -> Unit,
    onDownload: () -> Unit,
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
    var reviewStatesResult by remember(user.id) {
        mutableStateOf<AppResult<List<ReviewState>>?>(null)
    }
    LaunchedEffect(user.id, reviewRepository) {
        reviewRepository.observeStates(user.id).collect { result ->
            reviewStatesResult = result
        }
    }
    val reviewFlashcard = remember(reviewRepository, clock, idGenerator) {
        ReviewFlashcardUseCase(reviewRepository, clock, idGenerator)
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (MainTab.entries.any { it.route == currentRoute }) {
                WordDeckNavigationBar(currentRoute = currentRoute, onSelect = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(AppDestination.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.HOME,
            modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding),
        ) {
            composable(AppDestination.HOME) {
                HomeScreen(
                    uiState = homeUiState,
                    onDeckClick = { deckId ->
                        navController.navigate(AppDestination.deckDetails(deckId.value))
                    },
                    onCreateDeck = { navController.navigate(AppDestination.CREATE_DECK) },
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
                    onSync = onSync,
                    onDownload = onDownload,
                    onLogout = onLogout,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppDestination.STATISTICS) {
                StatisticsDestination(
                    key = "statistics-${user.id.value}",
                    title = stringResource(R.string.overall_statistics_title),
                    user = user,
                    deckId = null,
                    reviewRepository = reviewRepository,
                    clock = clock,
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
                    MissingDeckDestination(onBack = { navController.popBackStack() })
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
                        onOpenFlashcardHistory = { cardId ->
                            navController.navigate(AppDestination.cardHistory(cardId.value))
                        },
                        onOpenStatistics = {
                            navController.navigate(AppDestination.deckStatistics(currentDeck.id.value))
                        },
                        onBack = { navController.popBackStack() },
                        onStartStudy = {
                            navController.navigate(
                                AppDestination.studyDeck(currentDeck.id.value, StudyMode.FLASHCARD),
                            )
                        },
                        onStartTypedStudy = {
                            navController.navigate(
                                AppDestination.studyDeck(currentDeck.id.value, StudyMode.TYPED_ANSWER),
                            )
                        },
                    )
                }
            }
            composable(
                route = AppDestination.STUDY_DECK,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument(STUDY_MODE_ARGUMENT) {
                        type = NavType.StringType
                        defaultValue = StudyMode.FLASHCARD.name
                    },
                ),
            ) { backStackEntry ->
                val selectedId = backStackEntry.arguments?.getString("deckId")
                val selectedMode = backStackEntry.arguments
                    ?.getString(STUDY_MODE_ARGUMENT)
                    ?.let { value -> StudyMode.entries.firstOrNull { it.name == value } }
                    ?: StudyMode.FLASHCARD
                val currentDeck = homeUiState.decks.firstOrNull { it.id.value == selectedId }

                if (currentDeck == null) {
                    EmptyStudyDestination(onBack = { navController.popBackStack() })
                } else {
                    val flashcardViewModel = viewModel<FlashcardViewModel>(
                        key = "study-cards-${currentDeck.id.value}",
                    ) {
                        FlashcardViewModel(
                            flashcardRepository = flashcardRepository,
                            deckId = currentDeck.id,
                            idGenerator = idGenerator,
                            clock = clock,
                        )
                    }
                    val flashcardState by flashcardViewModel.uiState.collectAsStateWithLifecycle()

                    when (flashcardState.listStatus) {
                        OperationStatus.IDLE,
                        OperationStatus.LOADING,
                        -> LoadingScreen(Modifier)
                        OperationStatus.ERROR -> EmptyStudyDestination(
                            onBack = { navController.popBackStack() },
                        )
                        OperationStatus.SUCCESS -> when (val states = reviewStatesResult) {
                            null -> LoadingScreen(Modifier)
                            is AppResult.Failure -> EmptyStudyDestination(
                                onBack = { navController.popBackStack() },
                            )
                            is AppResult.Success -> {
                                val startedAt = remember(currentDeck.id.value) { clock.now() }
                                val session = remember(
                                    currentDeck.id,
                                    flashcardState.cards,
                                    states.value,
                                    startedAt,
                                ) {
                                    startStudySession(
                                        userId = user.id,
                                        deck = currentDeck,
                                        flashcards = flashcardState.cards,
                                        reviewStates = states.value,
                                        startedAt = startedAt,
                                    )
                                }
                                val studyViewModel = viewModel<StudyViewModel>(
                                    key = "study-${currentDeck.id.value}-${selectedMode.name}-${startedAt.epochMilliseconds}",
                                ) {
                                    StudyViewModel(
                                        session = session,
                                        currentUserId = user.id,
                                        reviewFlashcard = reviewFlashcard,
                                        mode = selectedMode,
                                    )
                                }
                                val studyState by studyViewModel.uiState.collectAsStateWithLifecycle()

                                StudyScreen(
                                    uiState = studyState,
                                    onRevealAnswer = studyViewModel::revealAnswer,
                                    onTypedAnswerChange = studyViewModel::updateTypedAnswer,
                                    onSubmitTypedAnswer = studyViewModel::submitTypedAnswer,
                                    onRate = studyViewModel::rate,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
            composable(
                route = AppDestination.CARD_HISTORY,
                arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val rawCardId = backStackEntry.arguments?.getString("cardId").orEmpty()
                when (val cardId = CardId.from(rawCardId)) {
                    is AppResult.Failure -> ReviewHistoryScreen(
                        history = cardId,
                        onBack = { navController.popBackStack() },
                    )
                    is AppResult.Success -> {
                        var history by remember(user.id, cardId.value) {
                            mutableStateOf<AppResult<List<ReviewEvent>>?>(null)
                        }
                        LaunchedEffect(user.id, cardId.value, reviewRepository) {
                            reviewRepository.observeHistory(user.id, cardId.value).collect { result ->
                                history = result
                            }
                        }
                        ReviewHistoryScreen(
                            history = history,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
            composable(
                route = AppDestination.DECK_STATISTICS,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val rawDeckId = backStackEntry.arguments?.getString("deckId").orEmpty()
                when (val deckId = DeckId.from(rawDeckId)) {
                    is AppResult.Failure -> StatisticsScreen(
                        title = stringResource(R.string.deck_statistics_fallback_title),
                        uiState = StatisticsUiState(
                            status = OperationStatus.ERROR,
                            error = deckId.error,
                        ),
                        onBack = { navController.popBackStack() },
                    )
                    is AppResult.Success -> {
                        val deckTitle = homeUiState.decks
                            .firstOrNull { it.id == deckId.value }
                            ?.title
                            ?.value
                        StatisticsDestination(
                            key = "statistics-${user.id.value}-${deckId.value.value}",
                            title = if (deckTitle == null) {
                                stringResource(R.string.deck_statistics_fallback_title)
                            } else {
                                stringResource(R.string.deck_statistics_title, deckTitle)
                            },
                            user = user,
                            deckId = deckId.value,
                            reviewRepository = reviewRepository,
                            clock = clock,
                            onBack = { navController.popBackStack() },
                        )
                    }
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
                    MissingDeckDestination(onBack = { navController.popBackStack() })
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
private fun StatisticsDestination(
    key: String,
    title: String,
    user: User,
    deckId: DeckId?,
    reviewRepository: ReviewRepository,
    clock: Clock,
    onBack: () -> Unit,
) {
    val statisticsViewModel = viewModel<StatisticsViewModel>(key = key) {
        StatisticsViewModel(
            reviewRepository = reviewRepository,
            userId = user.id,
            clock = clock,
            deckId = deckId,
        )
    }
    val statisticsState by statisticsViewModel.uiState.collectAsStateWithLifecycle()
    StatisticsScreen(
        title = title,
        uiState = statisticsState,
        onBack = onBack,
    )
}

@Composable
private fun MissingDeckDestination(onBack: () -> Unit) {
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
        onBack = onBack,
    )
}

@Composable
private fun EmptyStudyDestination(onBack: () -> Unit) {
    StudyScreen(
        uiState = StudyUiState(),
        onRevealAnswer = {},
        onTypedAnswerChange = {},
        onSubmitTypedAnswer = {},
        onRate = {},
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
