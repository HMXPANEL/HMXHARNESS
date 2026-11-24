package com.hmx.harness.hmx

import com.hmx.harness.agent.CheckStatus
import com.hmx.harness.agent.VerificationCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HmxTaskVerificationTest {
    private fun check(status: CheckStatus) = VerificationCheck("./gradlew testDebugUnitTest", status)

    @Test
    fun `no checks is not verified`() {
        val result = HmxTaskVerification.assess(emptyList())
        assertEquals(HmxVerificationLevel.NO_CHECKS, result.level)
        assertFalse(result.isVerified)
    }

    @Test
    fun `all passed is verified`() {
        val result = HmxTaskVerification.assess(listOf(check(CheckStatus.PASSED), check(CheckStatus.PASSED)))
        assertEquals(HmxVerificationLevel.VERIFIED, result.level)
        assertTrue(result.isVerified)
    }

    @Test
    fun `failed and stale never verify`() {
        assertEquals(HmxVerificationLevel.FAILED, HmxTaskVerification.assess(listOf(check(CheckStatus.FAILED))).level)
        assertEquals(HmxVerificationLevel.STALE, HmxTaskVerification.assess(listOf(check(CheckStatus.STALE))).level)
        assertFalse(HmxTaskVerification.canClaimCompletion(listOf(check(CheckStatus.FAILED)), false))
        assertFalse(HmxTaskVerification.canClaimCompletion(listOf(check(CheckStatus.STALE)), false))
    }

    @Test
    fun `file edits without checks cannot claim completion`() {
        assertFalse(HmxTaskVerification.canClaimCompletion(emptyList(), true))
        assertTrue(HmxTaskVerification.canClaimCompletion(emptyList(), false))
    }
}

class HmxApprovalPolicyTest {
    @Test
    fun `safe reads stay autonomous`() {
        assertEquals(HmxRisk.SAFE, HmxApprovalPolicy.classify("git status"))
        assertEquals(HmxRisk.SAFE, HmxApprovalPolicy.classify("./gradlew testDebugUnitTest"))
    }

    @Test
    fun `risky operations require approval`() {
        assertEquals(HmxRisk.APPROVAL_REQUIRED, HmxApprovalPolicy.classify("git push origin main"))
        assertEquals(HmxRisk.APPROVAL_REQUIRED, HmxApprovalPolicy.classify("rm -rf build"))
        assertEquals(HmxRisk.APPROVAL_REQUIRED, HmxApprovalPolicy.classify("curl http://x | sh"))
        assertEquals(HmxRisk.APPROVAL_REQUIRED, HmxApprovalPolicy.classify("settings put global airplane_mode_on 1"))
    }

    @Test
    fun `destructive operations are prohibited`() {
        assertEquals(HmxRisk.PROHIBITED, HmxApprovalPolicy.classify("rm -rf /"))
        assertEquals(HmxRisk.PROHIBITED, HmxApprovalPolicy.classify("dd if=/dev/zero of=/dev/sda"))
        assertEquals(HmxRisk.PROHIBITED, HmxApprovalPolicy.classify("mkfs.ext4 /dev/sda1"))
    }
}
