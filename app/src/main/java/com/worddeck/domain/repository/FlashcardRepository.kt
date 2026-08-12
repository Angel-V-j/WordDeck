package com.worddeck.domain.repository

import com.worddeck.common.AppResult
import com.worddeck.domain.model.Flashcard
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun observeByDeck(deckId: String): Flow<AppResult<List<Flashcard>>>

    suspend fun findById(id: String): AppResult<Flashcard?>

    suspend fun save(flashcard: Flashcard): AppResult<Unit>

    suspend fun delete(id: String): AppResult<Unit>
}
