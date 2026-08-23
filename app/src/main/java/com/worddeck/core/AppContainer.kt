package com.worddeck.core

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.worddeck.common.Clock
import com.worddeck.common.IdGenerator
import com.worddeck.common.SystemClock
import com.worddeck.common.observeNetworkAvailability
import com.worddeck.data.local.LocalUserStore
import com.worddeck.data.local.database.WordDeckDatabase
import com.worddeck.data.remote.firebase.FirebaseAuthRepository
import com.worddeck.data.repository.LocalDeckRepository
import com.worddeck.data.repository.LocalFlashcardRepository
import com.worddeck.data.repository.LocalAuthenticationRepository
import com.worddeck.data.repository.LocalReviewRepository
import com.worddeck.data.sync.SyncCoordinator
import com.worddeck.domain.repository.AuthenticationRepository
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository
import com.worddeck.domain.repository.ReviewRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Application-level dependencies shared by the presentation layer.
 *
 * Concrete repositories are supplied by the application composition root.
 * Tests can construct a container with fakes without Android or Compose.
 */
class AppContainer(
    val authenticationRepository: AuthenticationRepository,
    val deckRepository: DeckRepository,
    val flashcardRepository: FlashcardRepository,
    val reviewRepository: ReviewRepository,
    val syncCoordinator: SyncCoordinator,
    val idGenerator: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
    val clock: Clock = SystemClock,
    val networkAvailability: Flow<Boolean> = flowOf(true),
) {
    companion object {
        fun create(context: Context): AppContainer {
            val database = WordDeckDatabase.create(context)
            val idGenerator = IdGenerator { UUID.randomUUID().toString() }
            val firebaseAuth = FirebaseAuthRepository(FirebaseAuth.getInstance())
            return AppContainer(
                authenticationRepository = LocalAuthenticationRepository(
                    localUserStore = LocalUserStore(
                        context.getSharedPreferences(USER_PREFERENCES, Context.MODE_PRIVATE),
                    ),
                    firebaseAuth = firebaseAuth,
                    idGenerator = idGenerator,
                ),
                deckRepository = LocalDeckRepository(
                    database.deckDao(),
                    SystemClock,
                ),
                flashcardRepository = LocalFlashcardRepository(
                    database.flashcardDao(),
                    SystemClock,
                ),
                reviewRepository = LocalReviewRepository(database),
                syncCoordinator = SyncCoordinator.create(
                    database,
                    FirebaseFirestore.getInstance(),
                ),
                idGenerator = idGenerator,
                networkAvailability = observeNetworkAvailability(context),
            )
        }
    }
}

private const val USER_PREFERENCES = "worddeck-user"
