package com.worddeck.common

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTest {
    @Test
    fun `consumer uses date and time supplied by fixed clock`() {
        val fixedTimestamp = Timestamp(
            epochMilliseconds = Instant.parse("2026-08-12T09:30:00Z").toEpochMilli(),
        )
        val fixedClock = Clock { fixedTimestamp }
        val consumer = TimestampConsumer(fixedClock)

        val result = consumer.currentTimestamp()

        assertEquals(fixedTimestamp, result)
    }
}

private class TimestampConsumer(
    private val clock: Clock,
) {
    fun currentTimestamp(): Timestamp = clock.now()
}
