package com.worddeck.domain.model

data class User(
    val id: UserId,
    val email: String? = null,
    val displayName: String? = null,
)
