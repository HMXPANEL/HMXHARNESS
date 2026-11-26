package com.hmx.harness.agent

/**
 * The default safety bound for agent tool iterations. It applies to the
 * model/tool loop only, once per loop round, never per tool call inside a
 * batch. Zero still means the user explicitly chose unlimited.
 */
object IterationLimit {
    const val DEFAULT = 50

    /** Offered in Settings. Zero is the explicit "Unlimited" choice. */
    val OPTIONS = listOf(0, 20, 50, 75, 100)

    fun pausedMessage(limit: Int): String =
        "Task paused because it reached the $limit-iteration limit."

    /** Recognizes the friendly pause so the chat card can say "Continue". */
    fun isLimitReason(reason: String?): Boolean =
        reason != null && reason.contains("-iteration limit", ignoreCase = true)

    /**
     * Value to persist the first time this build reads settings.
     *
     * An existing install (onboarding already completed) keeps the behavior it
     * had before the default changed, which was unlimited. A fresh install gets
     * the new bounded default. An explicitly saved value always wins, including
     * zero for unlimited.
     */
    fun migratedValue(stored: Int?, onboardingDone: Boolean): Int? = when {
        stored != null -> null
        onboardingDone -> 0
        else -> DEFAULT
    }
}
