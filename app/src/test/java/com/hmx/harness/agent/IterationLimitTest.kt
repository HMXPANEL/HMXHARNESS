package com.hmx.harness.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IterationLimitTest {
    @Test fun `new installs get fifty rounds and existing installs keep unlimited`() {
        assertEquals(50, IterationLimit.DEFAULT)
        assertEquals(50, IterationLimit.migratedValue(stored = null, onboardingDone = false))
        assertEquals(0, IterationLimit.migratedValue(stored = null, onboardingDone = true))
    }

    @Test fun `explicit values always win including zero`() {
        assertNull(IterationLimit.migratedValue(stored = 0, onboardingDone = false))
        assertNull(IterationLimit.migratedValue(stored = 25, onboardingDone = true))
    }

    @Test fun `limit pause is explained and recognized for Continue`() {
        val message = IterationLimit.pausedMessage(50)
        assertEquals("Task paused because it reached the 50-iteration limit.", message)
        assertTrue(IterationLimit.isLimitReason(message))
        assertFalse(IterationLimit.isLimitReason("Stopped after 3 tool iterations (safety limit)."))
        assertFalse(IterationLimit.isLimitReason(null))
    }

    @Test fun `settings offers the requested bounded choices and explicit unlimited`() {
        assertTrue(IterationLimit.OPTIONS.containsAll(listOf(0, 20, 50, 75, 100)))
        assertEquals(0, IterationLimit.OPTIONS.first())
    }
}
