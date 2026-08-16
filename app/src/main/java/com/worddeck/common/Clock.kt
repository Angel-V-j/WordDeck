package com.worddeck.common

@JvmInline
value class Timestamp(val epochMilliseconds: Long)

fun interface Clock {
    fun now(): Timestamp
}

object SystemClock : Clock {
    override fun now(): Timestamp = Timestamp(System.currentTimeMillis())
}
