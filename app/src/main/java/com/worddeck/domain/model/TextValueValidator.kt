package com.worddeck.domain.model

import com.worddeck.common.AppError
import com.worddeck.common.AppResult

internal object TextValueValidator {
    fun <T> validate(
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

    fun <T> normalizeOptional(
        raw: String?,
        create: (String) -> T,
    ): T? {
        val normalized = raw?.trim().orEmpty()
        return if (normalized.isEmpty()) null else create(normalized)
    }
}
