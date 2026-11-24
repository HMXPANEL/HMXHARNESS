package com.hmx.harness.hmx

import com.hmx.harness.agent.CheckStatus
import com.hmx.harness.agent.VerificationCheck

/**
 * HMX verification gate.
 *
 * The upstream app already records real build/test/lint results and marks
 * checks stale after a later edit. This policy turns those records into an
 * explicit verdict so completion is never claimed from an empty or stale
 * evidence set. It is used by the verification card in chat.
 */
internal enum class HmxVerificationLevel { VERIFIED, UNVERIFIED, FAILED, STALE, NO_CHECKS }

internal data class HmxVerificationResult(
    val level: HmxVerificationLevel,
    val passed: Int,
    val failed: Int,
    val stale: Int,
    val unknown: Int,
) {
    val isVerified: Boolean get() = level == HmxVerificationLevel.VERIFIED
}

internal object HmxTaskVerification {
    fun assess(checks: List<VerificationCheck>): HmxVerificationResult {
        val passed = checks.count { it.status == CheckStatus.PASSED }
        val failed = checks.count { it.status == CheckStatus.FAILED }
        val stale = checks.count { it.status == CheckStatus.STALE }
        val unknown = checks.count { it.status == CheckStatus.UNKNOWN }
        val level = when {
            checks.isEmpty() -> HmxVerificationLevel.NO_CHECKS
            failed > 0 -> HmxVerificationLevel.FAILED
            stale > 0 || (passed > 0 && passed < checks.size) -> HmxVerificationLevel.STALE
            passed == checks.size -> HmxVerificationLevel.VERIFIED
            else -> HmxVerificationLevel.UNVERIFIED
        }
        return HmxVerificationResult(level, passed, failed, stale, unknown)
    }

    /**
     * A completion claim is supported only when every recorded check passed
     * and none are stale. A turn that touched files but ran no checks is
     * explicitly unverified rather than silently treated as successful.
     */
    fun canClaimCompletion(checks: List<VerificationCheck>, touchedFiles: Boolean): Boolean {
        val result = assess(checks)
        if (result.failed > 0 || result.stale > 0) return false
        if (checks.isEmpty()) return !touchedFiles
        return result.isVerified
    }
}

/** Hybrid execution: autonomous by default, approval for risky operations. */
internal enum class HmxRisk { SAFE, APPROVAL_REQUIRED, PROHIBITED }

internal object HmxApprovalPolicy {
    private val prohibited = listOf(
        Regex("(^|\\s)rm\\s+-rf\\s+/(?:\\s|$)"),
        Regex("mkfs(\\.|\\s)"),
        Regex("dd\\s+if=.*of=/dev/"),
        Regex(":\\(\\s*\\)\\s*\\{.*\\|.*\\}"),
    )
    private val approval = listOf(
        Regex("(^|\\s)(sudo|su|doas)(\\s|$)"),
        Regex("(^|\\s)(rm|rmdir|shred)(\\s|$)"),
        Regex("(^|\\s)(curl|wget)\\s+.*\\|\\s*(sh|bash|zsh)(\\s|$)"),
        Regex("(^|\\s)(git\\s+push|gh\\s+pr\\s+create|gh\\s+release\\s+create)(\\s|$)"),
        Regex("(^|\\s)(chmod|chown)\\s+(-R\\s+)?777(\\s|$)"),
        Regex("(^|\\s)settings\\s+put(\\s|$)"),
        Regex("(^|\\s)reboot(\\s|$)"),
    )

    fun classify(command: String): HmxRisk {
        val normalized = command.trim().lowercase()
        if (prohibited.any { it.containsMatchIn(normalized) }) return HmxRisk.PROHIBITED
        if (approval.any { it.containsMatchIn(normalized) }) return HmxRisk.APPROVAL_REQUIRED
        return HmxRisk.SAFE
    }
}
