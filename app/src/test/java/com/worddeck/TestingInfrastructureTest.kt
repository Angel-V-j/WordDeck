package com.worddeck

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class TestingInfrastructureTest {
    @Test
    fun coroutineTestScopeExecutesScheduledWork() = runTest {
        var workCompleted = false

        launch {
            workCompleted = true
        }.join()

        assertTrue(workCompleted)
    }
}
