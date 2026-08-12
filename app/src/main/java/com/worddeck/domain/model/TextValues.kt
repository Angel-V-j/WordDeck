package com.worddeck.domain.model

import com.worddeck.common.AppResult

@JvmInline
value class EmailAddress private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<EmailAddress> =
            TextValueValidator.validate(raw, "email") { EmailAddress(it) }
    }
}

@JvmInline
value class DisplayName private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<DisplayName> =
            TextValueValidator.validate(raw, "display name") { DisplayName(it) }
    }
}

@JvmInline
value class DeckTitle private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<DeckTitle> =
            TextValueValidator.validate(raw, "deck title") { DeckTitle(it) }
    }
}

@JvmInline
value class DeckLanguage private constructor(val value: String) {
    companion object {
        fun from(raw: String?): AppResult<DeckLanguage?> =
            TextValueValidator.validateOptional(raw) { DeckLanguage(it) }
    }
}

@JvmInline
value class DeckCategory private constructor(val value: String) {
    companion object {
        fun from(raw: String?): AppResult<DeckCategory?> =
            TextValueValidator.validateOptional(raw) { DeckCategory(it) }
    }
}

@JvmInline
value class CardSide private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<CardSide> =
            TextValueValidator.validate(raw, "card side") { CardSide(it) }
    }
}
