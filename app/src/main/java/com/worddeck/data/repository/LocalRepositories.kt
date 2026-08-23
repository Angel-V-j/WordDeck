package com.worddeck.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Clock
import com.worddeck.common.SystemClock
import com.worddeck.common.Timestamp
import com.worddeck.data.local.dao.DeckDao
import com.worddeck.data.local.dao.FlashcardDao
import com.worddeck.data.local.database.WordDeckDatabase
import com.worddeck.data.local.mapper.toDomainDecks
import com.worddeck.data.local.mapper.toDomainFlashcards
import com.worddeck.data.local.mapper.toDomainReviewEvents
import com.worddeck.data.local.mapper.toDomainReviewStates
import com.worddeck.data.local.mapper.toEntity
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.ReviewActivity
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.StudyProgress
import com.worddeck.domain.model.UserId
import com.worddeck.domain.repository.DeckRepository
import com.worddeck.domain.repository.FlashcardRepository
import com.worddeck.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Maps Deck domain operations directly to Room. Local Room data is not an authorization boundary. */
class LocalDeckRepository(
    private val deckDao: DeckDao,
    private val clock: Clock = SystemClock,
    private val onLocalChange: () -> Unit = {},
) : DeckRepository {
    override fun observeByOwner(ownerId: UserId): Flow<AppResult<List<Deck>>> =
        deckDao.observeByOwner(ownerId.value)
            .map { it.toDomainDecks() }
            .asDatabaseResult("decks")

    override suspend fun save(deck: Deck): AppResult<Unit> = databaseWrite("deck", onLocalChange) {
        deckDao.save(deck.toEntity())
    }

    override suspend fun delete(id: DeckId): AppResult<Unit> = databaseWrite("deck", onLocalChange) {
        deckDao.markDeleted(id.value, clock.now().epochMilliseconds)
    }
}

/** Maps Flashcard domain operations directly to Room. Access checks belong to the calling flow. */
class LocalFlashcardRepository(
    private val flashcardDao: FlashcardDao,
    private val clock: Clock = SystemClock,
    private val onLocalChange: () -> Unit = {},
) : FlashcardRepository {
    override fun observeByDeck(deckId: DeckId): Flow<AppResult<List<Flashcard>>> =
        flashcardDao.observeByDeck(deckId.value)
            .map { it.toDomainFlashcards() }
            .asDatabaseResult("flashcards")

    override suspend fun save(flashcard: Flashcard): AppResult<Unit> = databaseWrite(
        "flashcard",
        onLocalChange,
    ) {
        flashcardDao.save(flashcard.toEntity())
    }

    override suspend fun delete(id: CardId): AppResult<Unit> = databaseWrite(
        "flashcard",
        onLocalChange,
    ) {
        flashcardDao.markDeleted(id.value, clock.now().epochMilliseconds)
    }
}

/** Keeps review progress and its immutable history event in one Room transaction. */
class LocalReviewRepository(
    private val database: WordDeckDatabase,
    private val onLocalChange: () -> Unit = {},
) : ReviewRepository {
    override fun observeStates(userId: UserId): Flow<AppResult<List<ReviewState>>> =
        database.reviewStateDao()
            .observeByUser(userId.value)
            .map { it.toDomainReviewStates() }
            .asDatabaseResult("review states")

    override fun observeHistory(
        userId: UserId,
        cardId: CardId,
    ): Flow<AppResult<List<ReviewEvent>>> = database.reviewEventDao()
        .observeByUserAndCard(userId.value, cardId.value)
        .map { it.toDomainReviewEvents() }
        .asDatabaseResult("review history")

    override fun observeProgress(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> = database.reviewStateDao()
        .observeProgress(userId.value, timestamp.epochMilliseconds)
        .asSuccessfulDatabaseResult("study progress")

    override fun observeProgressByDeck(
        userId: UserId,
        deckId: DeckId,
        timestamp: Timestamp,
    ): Flow<AppResult<StudyProgress>> = database.reviewStateDao()
        .observeProgressByDeck(userId.value, deckId.value, timestamp.epochMilliseconds)
        .asSuccessfulDatabaseResult("study progress")

    override fun observeActivity(
        userId: UserId,
        timestamp: Timestamp,
    ): Flow<AppResult<ReviewActivity>> = database.reviewEventDao()
        .observeActivity(
            userId = userId.value,
            sevenDaysAgo = timestamp.epochMilliseconds - SEVEN_DAYS_IN_MILLIS,
            thirtyDaysAgo = timestamp.epochMilliseconds - THIRTY_DAYS_IN_MILLIS,
            timestamp = timestamp.epochMilliseconds,
        )
        .asSuccessfulDatabaseResult("review activity")

    override suspend fun recordReview(
        reviewState: ReviewState,
        reviewEvent: ReviewEvent,
    ): AppResult<Unit> = databaseWrite("review", onLocalChange) {
        database.withTransaction {
            database.reviewStateDao().save(reviewState.toEntity())
            database.reviewEventDao().insert(reviewEvent.toEntity())
        }
    }
}

/** Converts expected SQLite failures to AppResult; programming errors stay visible during development. */
private fun <T> Flow<AppResult<T>>.asDatabaseResult(resource: String): Flow<AppResult<T>> =
    catch { error ->
        if (error !is SQLiteException) throw error
        emit(databaseFailure(resource))
    }

private fun <T> Flow<T>.asSuccessfulDatabaseResult(resource: String): Flow<AppResult<T>> =
    map<T, AppResult<T>> { value -> AppResult.Success(value) }
        .asDatabaseResult(resource)

private suspend fun databaseWrite(
    resource: String,
    onSuccess: () -> Unit = {},
    write: suspend () -> Unit,
): AppResult<Unit> {
    try {
        write()
    } catch (_: SQLiteException) {
        return databaseFailure(resource)
    }

    // Scheduling happens only after Room has committed successfully.
    onSuccess()
    return AppResult.Success(Unit)
}

private fun databaseFailure(resource: String): AppResult.Failure =
    AppResult.Failure(AppError.Unavailable(resource))

private const val DAY_IN_MILLIS = 86_400_000L
private const val SEVEN_DAYS_IN_MILLIS = 7 * DAY_IN_MILLIS
private const val THIRTY_DAYS_IN_MILLIS = 30 * DAY_IN_MILLIS
