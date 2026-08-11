package com.worddeck.domain.model

data class User(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
)
