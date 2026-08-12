package com.worddeck.domain.model

import com.worddeck.core.AppError
import com.worddeck.core.AppResult

@JvmInline
value class UserId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<UserId> = IdentifierValidator.validate(raw, "user id") { UserId(it) }
    }
}

@JvmInline
value class DeckId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<DeckId> = IdentifierValidator.validate(raw, "deck id") { DeckId(it) }
    }
}

@JvmInline
value class CardId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<CardId> = IdentifierValidator.validate(raw, "card id") { CardId(it) }
    }
}

private object IdentifierValidator {
    fun <T> validate(raw: String, field: String, create: (String) -> T): AppResult<T> {
        val normalized = raw.trim()
        return if (normalized.isEmpty()) {
            AppResult.Failure(AppError.Validation(field, "must not be blank"))
        } else {
            AppResult.Success(create(normalized))
        }
    }
}