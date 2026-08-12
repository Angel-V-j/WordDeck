package com.worddeck.core

import android.content.Context
import com.worddeck.data.local.database.WordDeckDatabase
import com.worddeck.data.repository.LocalDeckRepository
import com.worddeck.data.repository.LocalFlashcardRepository
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository

/**
 * Application-level dependencies shared by the presentation layer.
 *
 * Concrete repositories are supplied by the application composition root.
 * Tests can construct a container with fakes without Android or Compose.
 */
class AppContainer(
    val deckRepository: DeckRepository,
    val flashcardRepository: FlashcardRepository,
) {
    constructor(database: WordDeckDatabase) : this(
        deckRepository = LocalDeckRepository(database.deckDao()),
        flashcardRepository = LocalFlashcardRepository(database.flashcardDao()),
    )

    companion object {
        fun create(context: Context): AppContainer =
            AppContainer(WordDeckDatabase.create(context))
    }
}
