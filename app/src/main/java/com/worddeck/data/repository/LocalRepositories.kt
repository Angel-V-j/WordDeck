package com.worddeck.data.repository

import android.database.sqlite.SQLiteException
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.data.local.dao.DeckDao
import com.worddeck.data.local.dao.FlashcardDao
import com.worddeck.data.local.mapper.toDomainDecks
import com.worddeck.data.local.mapper.toDomainFlashcards
import com.worddeck.data.local.mapper.toEntity
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Maps Deck domain operations directly to Room. Local Room data is not an authorization boundary. */
class LocalDeckRepository(
    private val deckDao: DeckDao,
) : DeckRepository {
    override fun observeByOwner(ownerId: UserId): Flow<AppResult<List<Deck>>> =
        deckDao.observeByOwner(ownerId.value)
            .map { it.toDomainDecks() }
            .asDatabaseResult("decks")

    override suspend fun save(deck: Deck): AppResult<Unit> = databaseWrite("deck") {
        deckDao.save(deck.toEntity())
    }

    override suspend fun delete(id: DeckId): AppResult<Unit> = databaseWrite("deck") {
        deckDao.deleteById(id.value)
    }
}

/** Maps Flashcard domain operations directly to Room. Access checks belong to the calling flow. */
class LocalFlashcardRepository(
    private val flashcardDao: FlashcardDao,
) : FlashcardRepository {
    override fun observeByDeck(deckId: DeckId): Flow<AppResult<List<Flashcard>>> =
        flashcardDao.observeByDeck(deckId.value)
            .map { it.toDomainFlashcards() }
            .asDatabaseResult("flashcards")

    override suspend fun save(flashcard: Flashcard): AppResult<Unit> = databaseWrite("flashcard") {
        flashcardDao.save(flashcard.toEntity())
    }

    override suspend fun delete(id: CardId): AppResult<Unit> = databaseWrite("flashcard") {
        flashcardDao.deleteById(id.value)
    }
}

/** Converts expected SQLite failures to AppResult; programming errors stay visible during development. */
private fun <T> Flow<AppResult<T>>.asDatabaseResult(resource: String): Flow<AppResult<T>> =
    catch { error ->
        if (error !is SQLiteException) throw error
        emit(databaseFailure(resource))
    }

private suspend fun databaseWrite(
    resource: String,
    write: suspend () -> Unit,
): AppResult<Unit> = try {
    write()
    AppResult.Success(Unit)
} catch (_: SQLiteException) {
    databaseFailure(resource)
}

private fun databaseFailure(resource: String): AppResult.Failure =
    AppResult.Failure(AppError.Unavailable(resource))
