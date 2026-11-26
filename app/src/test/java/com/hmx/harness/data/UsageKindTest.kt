package com.hmx.harness.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageKindTest {
    @Test fun `unknown and legacy kinds fall back to chat`() {
        assertEquals(UsageKind.CHAT, UsageKind.normalize(null))
        assertEquals(UsageKind.CHAT, UsageKind.normalize(""))
        assertEquals(UsageKind.CHAT, UsageKind.normalize("legacy"))
        assertEquals(UsageKind.SUBAGENT, UsageKind.normalize(" SubAgent "))
    }

    @Test fun `overhead excludes task and subagent work`() {
        assertTrue(UsageKind.isOverhead(UsageKind.COMPACTION))
        assertTrue(UsageKind.isOverhead(UsageKind.PROBE))
        assertTrue(UsageKind.isOverhead(UsageKind.AUTOMATION))
        assertFalse(UsageKind.isOverhead(UsageKind.CHAT))
        assertFalse(UsageKind.isOverhead(UsageKind.SUBAGENT))
    }

    @Test fun `missing counts are never recorded and no session is never a fake id`() {
        assertFalse(UsageKind.hasReportedTokens(0, 0))
        assertTrue(UsageKind.hasReportedTokens(0, 1))
        assertTrue(UsageKind.hasReportedTokens(1, 0))
        assertEquals("", UsageKind.NO_SESSION)
        assertFalse(UsageKind.NO_SESSION in UsageKind.all)
    }
}
