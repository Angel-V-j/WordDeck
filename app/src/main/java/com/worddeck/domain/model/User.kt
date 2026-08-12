package com.worddeck.domain.model

data class User(
    val id: UserId,
    val email: EmailAddress,
    val displayName: DisplayName,
)
