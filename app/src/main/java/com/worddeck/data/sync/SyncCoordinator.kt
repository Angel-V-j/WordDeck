package com.worddeck.data.sync

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.data.local.database.WordDeckDatabase
import com.worddeck.data.local.mapper.toDomain as toLocalDomain
import com.worddeck.data.local.mapper.toEntity
import com.worddeck.data.remote.firebase.DeckDto
import com.worddeck.data.remote.firebase.FirestoreSyncRemoteStore
import com.worddeck.data.remote.firebase.FlashcardDto
import com.worddeck.data.remote.firebase.ReviewEventDto
import com.worddeck.data.remote.firebase.ReviewStateDto
import com.worddeck.data.remote.firebase.toDomain as toRemoteDomain
import com.worddeck.data.remote.firebase.toFirestoreDto
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.Flashcard
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.domain.model.ReviewState
import com.worddeck.domain.model.UserId

internal data class SyncData(
    val decks: List<DeckDto> = emptyList(),
    val flashcards: List<FlashcardDto> = emptyList(),
    val reviewStates: List<ReviewStateDto> = emptyList(),
    val reviewEvents: List<ReviewEventDto> = emptyList(),
)

/** Small testable boundary around the one remote synchronization service. */
internal interface SyncRemoteStore {
    suspend fun upload(userId: String, data: SyncData): AppResult<Unit>

    suspend fun forceUpload(userId: String, data: SyncData): AppResult<Unit>

    suspend fun download(userId: String): AppResult<SyncData>
}

/** Coordinates one explicit Room-first upload/download pass for the current user. */
class SyncCoordinator internal constructor(
    private val database: WordDeckDatabase,
    private val remoteStore: SyncRemoteStore,
) {
    suspend fun sync(
        localUserId: UserId,
        firebaseUid: UserId = localUserId,
    ): AppResult<Unit> = try {
        syncInternal(localUserId, firebaseUid)
    } catch (_: SQLiteException) {
        AppResult.Failure(AppError.Unavailable("synchronization"))
    }

    suspend fun forceUpload(
        localUserId: UserId,
        firebaseUid: UserId,
    ): AppResult<Unit> = try {
        val pendingData = when (val result = loadPendingData(localUserId, firebaseUid)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        when (val result = remoteStore.forceUpload(firebaseUid.value, pendingData)) {
            is AppResult.Success -> {
                markUploadedDataAsSynced(pendingData, localUserId)
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }
    } catch (_: SQLiteException) {
        AppResult.Failure(AppError.Unavailable("synchronization"))
    }

    /** Refreshes Room after a normal online login without uploading pending local changes. */
    suspend fun download(
        localUserId: UserId,
        firebaseUid: UserId = localUserId,
    ): AppResult<Unit> = try {
        downloadInternal(localUserId, firebaseUid)
    } catch (_: SQLiteException) {
        AppResult.Failure(AppError.Unavailable("synchronization"))
    }

    private suspend fun syncInternal(
        localUserId: UserId,
        firebaseUid: UserId,
    ): AppResult<Unit> {
        val pendingData = when (val result = loadPendingData(localUserId, firebaseUid)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }

        when (val result = remoteStore.upload(firebaseUid.value, pendingData)) {
            is AppResult.Success -> markUploadedDataAsSynced(pendingData, localUserId)
            is AppResult.Failure -> return result
        }

        return downloadInternal(localUserId, firebaseUid)
    }

    private suspend fun downloadInternal(
        localUserId: UserId,
        firebaseUid: UserId,
    ): AppResult<Unit> {
        val remoteData = when (val result = remoteStore.download(firebaseUid.value)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val mappedData = when (
            val result = validateAndMapRemoteData(localUserId, firebaseUid, remoteData)
        ) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }

        return saveRemoteData(mappedData)
    }

    private suspend fun loadPendingData(
        localUserId: UserId,
        firebaseUid: UserId,
    ): AppResult<SyncData> {
        val decks = mutableListOf<DeckDto>()
        for (entity in database.deckDao().findPendingByOwner(localUserId.value)) {
            when (val result = entity.toLocalDomain()) {
                is AppResult.Success -> decks += result.value.toFirestoreDto(
                    entity.deletedAt?.let(::Timestamp),
                    firebaseUid,
                )
                is AppResult.Failure -> return result
            }
        }

        val flashcards = mutableListOf<FlashcardDto>()
        for (entity in database.flashcardDao().findPendingByOwner(localUserId.value)) {
            when (val result = entity.toLocalDomain()) {
                is AppResult.Success -> flashcards += result.value.toFirestoreDto(
                    entity.deletedAt?.let(::Timestamp),
                )
                is AppResult.Failure -> return result
            }
        }

        val reviewStates = mutableListOf<ReviewStateDto>()
        for (entity in database.reviewStateDao().findPendingByUser(localUserId.value)) {
            when (val result = entity.toLocalDomain()) {
                is AppResult.Success -> reviewStates += result.value.toFirestoreDto(
                    Timestamp(entity.updatedAt),
                    firebaseUid,
                )
                is AppResult.Failure -> return result
            }
        }

        val reviewEvents = mutableListOf<ReviewEventDto>()
        for (entity in database.reviewEventDao().findPendingByUser(localUserId.value)) {
            when (val result = entity.toLocalDomain()) {
                is AppResult.Success -> reviewEvents += result.value.toFirestoreDto(firebaseUid)
                is AppResult.Failure -> return result
            }
        }

        return AppResult.Success(SyncData(decks, flashcards, reviewStates, reviewEvents))
    }

    private suspend fun markUploadedDataAsSynced(data: SyncData, localUserId: UserId) {
        database.withTransaction {
            for (deck in data.decks) {
                database.deckDao().markSynced(deck.id, deck.updatedAt)
            }
            for (flashcard in data.flashcards) {
                database.flashcardDao().markSynced(flashcard.id, flashcard.updatedAt)
            }
            for (state in data.reviewStates) {
                database.reviewStateDao().markSynced(
                    userId = localUserId.value,
                    cardId = state.cardId,
                    updatedAt = state.updatedAt,
                )
            }
            for (event in data.reviewEvents) {
                database.reviewEventDao().markSynced(event.id)
            }
        }
    }

    private suspend fun validateAndMapRemoteData(
        localUserId: UserId,
        firebaseUid: UserId,
        data: SyncData,
    ): AppResult<MappedSyncData> {
        val decks = mutableListOf<RemoteDeck>()
        for (dto in data.decks) {
            if (dto.ownerId != firebaseUid.value) return ownerMismatch("deck")
            when (val result = dto.toRemoteDomain(allowDeleted = true)) {
                is AppResult.Success -> decks += RemoteDeck(
                    result.value.copy(ownerId = localUserId),
                    dto.deletedAt,
                )
                is AppResult.Failure -> return result
            }
        }

        val allowedDeckIds = database.deckDao().findIdsByOwner(localUserId.value).toMutableSet()
        allowedDeckIds += decks.map { it.deck.id.value }

        val flashcards = mutableListOf<RemoteFlashcard>()
        for (dto in data.flashcards) {
            when (val result = dto.toRemoteDomain(allowDeleted = true)) {
                is AppResult.Success -> {
                    if (result.value.deckId.value !in allowedDeckIds) {
                        return ownerMismatch("flashcard deck")
                    }
                    flashcards += RemoteFlashcard(result.value, dto.deletedAt)
                }
                is AppResult.Failure -> return result
            }
        }

        val allowedCardIds = database.flashcardDao()
            .findIdsByOwner(localUserId.value)
            .toMutableSet()
        allowedCardIds += flashcards.map { it.flashcard.id.value }

        val reviewStates = mutableListOf<RemoteReviewState>()
        for (dto in data.reviewStates) {
            if (dto.userId != firebaseUid.value) return ownerMismatch("review state")
            when (val result = dto.toRemoteDomain()) {
                is AppResult.Success -> {
                    if (result.value.cardId.value !in allowedCardIds) {
                        return ownerMismatch("review state card")
                    }
                    reviewStates += RemoteReviewState(
                        result.value.copy(userId = localUserId),
                        dto.updatedAt,
                    )
                }
                is AppResult.Failure -> return result
            }
        }

        val reviewEvents = mutableListOf<ReviewEvent>()
        for (dto in data.reviewEvents) {
            if (dto.userId != firebaseUid.value) return ownerMismatch("review event")
            when (val result = dto.toRemoteDomain()) {
                is AppResult.Success -> {
                    if (result.value.cardId.value !in allowedCardIds) {
                        return ownerMismatch("review event card")
                    }
                    val localEvent = result.value.copy(userId = localUserId)
                    val existing = database.reviewEventDao().findById(dto.id)
                    if (existing != null) {
                        when (val existingResult = existing.toLocalDomain()) {
                            is AppResult.Success -> if (existingResult.value != localEvent) {
                                return reviewEventConflict(dto.id)
                            }
                            is AppResult.Failure -> return existingResult
                        }
                    }
                    reviewEvents += localEvent
                }
                is AppResult.Failure -> return result
            }
        }

        return AppResult.Success(MappedSyncData(decks, flashcards, reviewStates, reviewEvents))
    }

    private suspend fun saveRemoteData(data: MappedSyncData): AppResult<Unit> = try {
        database.withTransaction {
            for (remoteDeck in data.decks) {
                val deck = remoteDeck.deck
                val local = database.deckDao().findById(deck.id.value)
                if (
                    local == null ||
                    (!local.pendingSync && deck.updatedAt.epochMilliseconds >= local.updatedAt)
                ) {
                    database.deckDao().save(
                        deck.toEntity(
                            pendingSync = false,
                            deletedAt = remoteDeck.deletedAt,
                        ),
                    )
                }
            }
            for (remoteFlashcard in data.flashcards) {
                val flashcard = remoteFlashcard.flashcard
                val local = database.flashcardDao().findById(flashcard.id.value)
                if (
                    local == null ||
                    (!local.pendingSync && flashcard.updatedAt.epochMilliseconds >= local.updatedAt)
                ) {
                    database.flashcardDao().save(
                        flashcard.toEntity(
                            pendingSync = false,
                            deletedAt = remoteFlashcard.deletedAt,
                        ),
                    )
                }
            }
            for (remoteState in data.reviewStates) {
                val state = remoteState.state
                val local = database.reviewStateDao().findByUserAndCard(
                    state.userId.value,
                    state.cardId.value,
                )
                if (
                    local == null ||
                    (!local.pendingSync && remoteState.updatedAt >= local.updatedAt)
                ) {
                    database.reviewStateDao().save(
                        state.toEntity(
                            updatedAt = remoteState.updatedAt,
                            pendingSync = false,
                        ),
                    )
                }
            }
            for (event in data.reviewEvents) {
                database.reviewEventDao().insertIfAbsent(event.toEntity(pendingSync = false))
                database.reviewEventDao().markSynced(event.id.value)
            }
        }
        AppResult.Success(Unit)
    } catch (_: SQLiteException) {
        AppResult.Failure(AppError.Unavailable("synchronization"))
    }

    companion object {
        fun create(
            database: WordDeckDatabase,
            firestore: FirebaseFirestore,
        ): SyncCoordinator = SyncCoordinator(database, FirestoreSyncRemoteStore(firestore))
    }
}

private data class MappedSyncData(
    val decks: List<RemoteDeck>,
    val flashcards: List<RemoteFlashcard>,
    val reviewStates: List<RemoteReviewState>,
    val reviewEvents: List<ReviewEvent>,
)

private data class RemoteDeck(
    val deck: Deck,
    val deletedAt: Long?,
)

private data class RemoteFlashcard(
    val flashcard: Flashcard,
    val deletedAt: Long?,
)

private data class RemoteReviewState(
    val state: ReviewState,
    val updatedAt: Long,
)

private fun ownerMismatch(resource: String): AppResult.Failure = AppResult.Failure(
    AppError.Validation(resource, "does not belong to the current user"),
)

private fun reviewEventConflict(id: String): AppResult.Failure = AppResult.Failure(
    AppError.Validation("review event", "conflicting data for id $id"),
)
