package com.worddeck.core

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
)
