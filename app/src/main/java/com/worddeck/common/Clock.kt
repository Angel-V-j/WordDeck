package com.worddeck.common

@JvmInline
value class Timestamp(val epochMilliseconds: Long)

fun interface Clock {
    fun now(): Timestamp
}
