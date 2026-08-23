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
            validateRequiredText(raw, "display name") { DisplayName(it) }
    }
}

@JvmInline
value class DeckTitle private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<DeckTitle> =
            validateRequiredText(raw, "deck title") { DeckTitle(it) }
    }
}

@JvmInline
value class DeckLanguage private constructor(val value: String) {
    companion object {
        fun from(raw: String?): DeckLanguage? =
            normalizeOptionalText(raw) { DeckLanguage(it) }
    }
}

@JvmInline
value class DeckCategory private constructor(val value: String) {
    companion object {
        fun from(raw: String?): DeckCategory? =
            normalizeOptionalText(raw) { DeckCategory(it) }
    }
}

@JvmInline
value class CardSide private constructor(val value: String) {
    companion object {
        fun from(raw: String): AppResult<CardSide> =
            validateRequiredText(raw, "card side") { CardSide(it) }
    }
}

private fun <T> validateRequiredText(
    raw: String,
    field: String,
    create: (String) -> T,
): AppResult<T> {
    val normalized = raw.trim()
    return if (normalized.isEmpty()) {
        AppResult.Failure(AppError.Validation(field, "must not be blank"))
    } else {
        AppResult.Success(create(normalized))
    }
}

private fun <T> normalizeOptionalText(
    raw: String?,
    create: (String) -> T,
): T? {
    val normalized = raw?.trim().orEmpty()
    return if (normalized.isEmpty()) null else create(normalized)
}
