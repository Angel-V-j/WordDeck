package com.worddeck.core

import com.worddeck.common.Clock
import com.worddeck.common.Timestamp

class MutableClock(initialTimestamp: Timestamp) : Clock {
    private var timestamp = initialTimestamp

    override fun now(): Timestamp = timestamp

    fun set(timestamp: Timestamp) {
        this.timestamp = timestamp
    }
}