package com.worddeck.data.remote.firebase

import com.google.firebase.FirebaseException
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.data.sync.SyncData
import com.worddeck.data.sync.SyncRemoteStore
import kotlinx.coroutines.tasks.await

internal class FirestoreSyncRemoteStore(
    private val firestore: FirebaseFirestore,
) : SyncRemoteStore {
    override suspend fun upload(userId: String, data: SyncData): AppResult<Unit> = try {
        for (deck in data.decks) {
            uploadMutable(document(userId, DECKS, deck.id), deck, deck.updatedAt)
        }
        for (flashcard in data.flashcards) {
            uploadMutable(
                document(userId, FLASHCARDS, flashcard.id),
                flashcard,
                flashcard.updatedAt,
            )
        }
        for (state in data.reviewStates) {
            uploadMutable(
                document(userId, REVIEW_STATES, state.cardId),
                state,
                state.updatedAt,
            )
        }
        for (event in data.reviewEvents) {
            uploadReviewEvent(document(userId, REVIEW_EVENTS, event.id), event)
        }
        AppResult.Success(Unit)
    } catch (error: FirebaseException) {
        syncFailure(error)
    } catch (_: ConflictingReviewEventException) {
        AppResult.Failure(
            AppError.Validation("review event", "same id has different remote data"),
        )
    }

    override suspend fun download(userId: String): AppResult<SyncData> = try {
        AppResult.Success(
            SyncData(
                decks = downloadCollection(userId, DECKS, DeckDto::class.java),
                flashcards = downloadCollection(userId, FLASHCARDS, FlashcardDto::class.java),
                reviewStates = downloadCollection(
                    userId,
                    REVIEW_STATES,
                    ReviewStateDto::class.java,
                ),
                reviewEvents = downloadCollection(
                    userId,
                    REVIEW_EVENTS,
                    ReviewEventDto::class.java,
                ),
            ),
        )
    } catch (error: FirebaseException) {
        syncFailure(error)
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Validation("remote data", "cannot be read"))
    }

    private suspend fun uploadMutable(
        reference: DocumentReference,
        value: Any,
        updatedAt: Long,
    ) {
        firestore.runTransaction { transaction ->
            val remote = transaction.get(reference)
            val remoteUpdatedAt = remote.getLong("updatedAt")
            if (!remote.exists() || remoteUpdatedAt == null || updatedAt > remoteUpdatedAt) {
                transaction.set(reference, value)
            }
        }.await()
    }

    private suspend fun uploadReviewEvent(
        reference: DocumentReference,
        event: ReviewEventDto,
    ) {
        firestore.runTransaction { transaction ->
            val remote = transaction.get(reference)
            if (!remote.exists()) {
                transaction.set(reference, event)
            } else if (remote.toObject(ReviewEventDto::class.java) != event) {
                throw ConflictingReviewEventException()
            }
        }.await()
    }

    private suspend fun <T> downloadCollection(
        userId: String,
        collection: String,
        type: Class<T>,
    ): List<T> = firestore.collection("users")
        .document(userId)
        .collection(collection)
        .get(Source.SERVER)
        .await()
        .documents
        .map { document ->
            document.toObject(type)
                ?: throw IllegalArgumentException("Invalid Firestore document")
        }

    private fun document(
        userId: String,
        collection: String,
        documentId: String,
    ): DocumentReference = firestore.collection("users")
        .document(userId)
        .collection(collection)
        .document(documentId)
}

private class ConflictingReviewEventException : RuntimeException()

private fun syncFailure(error: FirebaseException): AppResult.Failure {
    val isNetworkFailure = error is FirebaseFirestoreException &&
        error.code in setOf(
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        )
    return if (isNetworkFailure) {
        AppResult.Failure(AppError.NetworkUnavailable)
    } else {
        AppResult.Failure(AppError.Unavailable("synchronization"))
    }
}

private const val DECKS = "decks"
private const val FLASHCARDS = "flashcards"
private const val REVIEW_STATES = "reviewStates"
private const val REVIEW_EVENTS = "reviewEvents"
