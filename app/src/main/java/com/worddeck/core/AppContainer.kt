package com.worddeck.core

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.worddeck.data.local.database.WordDeckDatabase
import com.worddeck.data.remote.firebase.FirebaseAuthRepository
import com.worddeck.data.repository.LocalDeckRepository
import com.worddeck.data.repository.LocalFlashcardRepository
import com.worddeck.domain.repository.AuthenticationRepository
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository

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
) {
    constructor(
        database: WordDeckDatabase,
        authenticationRepository: AuthenticationRepository,
    ) : this(
        authenticationRepository = authenticationRepository,
        deckRepository = LocalDeckRepository(database.deckDao()),
        flashcardRepository = LocalFlashcardRepository(database.flashcardDao()),
    )

    companion object {
        fun create(context: Context): AppContainer =
            AppContainer(
                database = WordDeckDatabase.create(context),
                authenticationRepository = FirebaseAuthRepository(
                    FirebaseAuth.getInstance(),
                ),
            )
    }
}
