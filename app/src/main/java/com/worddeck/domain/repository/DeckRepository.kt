package com.worddeck.domain.repository

import com.worddeck.common.AppResult
import com.worddeck.domain.model.Deck
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun observeByOwner(ownerId: String): Flow<AppResult<List<Deck>>>

    suspend fun findById(id: String): AppResult<Deck?>

    suspend fun save(deck: Deck): AppResult<Unit>

    suspend fun delete(id: String): AppResult<Unit>
}
