package com.worddeck.domain.repository

import com.worddeck.common.AppResult
import com.worddeck.domain.model.Deck
import com.worddeck.domain.model.DeckId
import com.worddeck.domain.model.UserId
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun observeByOwner(ownerId: UserId): Flow<AppResult<List<Deck>>>

    suspend fun save(deck: Deck): AppResult<Unit>

    suspend fun delete(id: DeckId): AppResult<Unit>
}
