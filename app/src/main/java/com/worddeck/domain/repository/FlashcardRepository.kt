package com.worddeck.domain.repository

import com.worddeck.common.AppResult
import com.worddeck.domain.model.CardId
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.Flashcard
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun observeByDeck(deckId: DeckId): Flow<AppResult<List<Flashcard>>>

    suspend fun findById(id: CardId): AppResult<Flashcard?>

    suspend fun save(flashcard: Flashcard): AppResult<Unit>

    suspend fun delete(id: CardId): AppResult<Unit>
}
