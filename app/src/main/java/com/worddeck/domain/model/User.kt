package com.worddeck.domain.model

data class User(
    val id: UserId,
    val email: EmailAddress?,
    val displayName: DisplayName,
    val firebaseUid: UserId? = id,
) {
    val isLinked: Boolean
        get() = firebaseUid != null
}
