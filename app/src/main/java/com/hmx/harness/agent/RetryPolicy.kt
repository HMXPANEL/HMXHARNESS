package com.hmx.harness.agent

import com.hmx.harness.llm.ApiException
import java.io.IOException

/**
 * Decides which provider failures are worth retrying and how long to wait.
 * Transient conditions only, rate limits, server overload, network drops.
 * Permanent errors (bad request, auth, not found) surface to the user at once.
 */
internal object RetryPolicy {
    const val MAX_RETRIES = 3
    private const val BASE_DELAY_MS = 1_000L

    private val TRANSIENT_MESSAGE = Regex(
        "overloaded|rate.?limit|timeout|timed out|temporarily|try again|" +
            "connection reset|connection refused|ECONNRESET|socket closed|JSON error injected into SSE stream",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Provider codes that will not succeed on another attempt: exhausted
     * quotas, rejected credentials, or a request the server will always
     * refuse. A 429 with one of these codes is not a temporary rate limit.
     */
    private val PERMANENT_ERROR_CODES = setOf(
        "subscription_sharing_usage_limit_exceeded",
        "harness_free_quota_exhausted",
        "promotion_model_limit_reached",
        "insufficient_quota",
        "quota_exceeded",
        "billing_hard_limit_reached",
        "invalid_api_key",
        "authentication_error",
        "permission_error",
        "invalid_request_error",
        "context_length_exceeded",
    )

    /** HTTP codes worth another attempt: request timeout, rate limit, server errors. */
    fun isRetryableCode(code: Int): Boolean =
        code == 408 || code == 429 || code in 500..599

    fun isPermanentErrorCode(errorCode: String?): Boolean =
        errorCode != null && errorCode.trim().lowercase() in PERMANENT_ERROR_CODES

    /**
     * Classifies a failure from its (optional) thrown cause, in-stream message
     * and machine-readable provider code. [ApiException] is checked before
     * [IOException] because it subclasses it: an HTTP 400 must not retry even
     * though it arrives as an IOException. An explicit in-stream [code] takes
     * precedence over message-based guesses, but a permanent [errorCode] wins
     * over both so a quota 429 is never mistaken for a temporary rate limit.
     */
    fun isRetryable(
        cause: Throwable?,
        message: String?,
        code: Int? = null,
        errorCode: String? = null,
    ): Boolean {
        if (isPermanentErrorCode(errorCode)) return false
        if (code != null) return isRetryableCode(code)
        var c = cause
        while (c != null) {
            if (c is ApiException) {
                if (isPermanentErrorCode(c.errorCode)) return false
                return isRetryableCode(c.code)
            }
            if (c is IOException) return true
            c = c.cause
        }
        return message != null && TRANSIENT_MESSAGE.containsMatchIn(message)
    }

    /**
     * Backoff for the next attempt. A provider's Retry-After wins, bounded to
     * five minutes so one temporary limit cannot freeze a run indefinitely.
     * Otherwise exponential 1s / 2s / 4s with +/-30% jitter.
     * [attempt] is 1-based.
     */
    fun delayMs(attempt: Int, retryAfterSeconds: Long? = null): Long {
        if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            return retryAfterSeconds.coerceAtMost(MAX_RETRY_AFTER_SECONDS) * 1_000L
        }
        val exp = (attempt - 1).coerceIn(0, 4)
        val base = BASE_DELAY_MS shl exp
        val jitter = (base * 0.3 * (Math.random() * 2 - 1)).toLong()
        return base + jitter
    }

    private const val MAX_RETRY_AFTER_SECONDS = 300L
}
