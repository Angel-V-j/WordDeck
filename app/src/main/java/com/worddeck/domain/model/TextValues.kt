package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult

private val EMAIL_REGEX = Regex(
    pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
)

@JvmInline
value class EmailAddress private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<EmailAddress> {
            val normalized = raw.trim()
            if (normalized.isEmpty()) {
                return AppResult.Failure(AppError.Validation("email", "must not be blank"))
            }
            if (!EMAIL_REGEX.matches(normalized)) {
                return AppResult.Failure(AppError.Validation("email", "has invalid format"))
            }
            return AppResult.Success(EmailAddress(normalized))
        }
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
