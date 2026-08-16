package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult

@JvmInline
value class UserId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<UserId> =
            validateId(raw, "user id") { UserId(it) }
    }
}

@JvmInline
value class DeckId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<DeckId> =
            validateId(raw, "deck id") { DeckId(it) }
    }
}

@JvmInline
value class CardId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<CardId> =
            validateId(raw, "card id") { CardId(it) }
    }
}

@JvmInline
value class ReviewEventId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<ReviewEventId> =
            validateId(raw, "review event id") { ReviewEventId(it) }
    }
}

private fun <T> validateId(
    raw: String,
    field: String,
    create: (String) -> T,
): AppResult<T> {
    if (raw.isBlank()) {
        return AppResult.Failure(AppError.Validation(field, "must not be blank"))
    }
    if (raw != raw.trim()) {
        return AppResult.Failure(
            AppError.Validation(field, "must not contain surrounding whitespace"),
        )
    }
    return AppResult.Success(create(raw))
}
