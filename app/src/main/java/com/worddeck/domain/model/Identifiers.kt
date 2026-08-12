package com.worddeck.domain.model

import com.worddeck.common.AppResult

@JvmInline
value class UserId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<UserId> =
            TextValueValidator.validate(raw, "user id") { UserId(it) }
    }
}

@JvmInline
value class DeckId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<DeckId> =
            TextValueValidator.validate(raw, "deck id") { DeckId(it) }
    }
}

@JvmInline
value class CardId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<CardId> =
            TextValueValidator.validate(raw, "card id") { CardId(it) }
    }
}
