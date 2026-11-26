package com.hmx.harness.data

/**
 * What a recorded request was for. Kept as strings so older databases migrate
 * with a plain default and every provider adapter can tag its own usage.
 *
 * [NO_SESSION] is an explicit "not attached to a chat" scope, never a made-up
 * session id. Provider model checks and automation setup happen outside a chat.
 */
object UsageKind {
    const val CHAT = "chat"
    const val COMPACTION = "compaction"
    const val PROBE = "probe"
    const val AUTOMATION = "automation"
    const val SUBAGENT = "subagent"

    /** Empty scope for usage that has no chat session. Never used as a session lookup key. */
    const val NO_SESSION = ""

    val all = listOf(CHAT, COMPACTION, PROBE, AUTOMATION, SUBAGENT)

    fun normalize(value: String?): String =
        value?.trim()?.lowercase()?.takeIf { it in all } ?: CHAT

    /** Overhead is work done to keep a task running, not the task itself. */
    fun isOverhead(kind: String): Boolean =
        kind == COMPACTION || kind == PROBE || kind == AUTOMATION

    /**
     * Only provider-reported counts are recorded. A response without token
     * counts must never turn into an invented usage row.
     */
    fun hasReportedTokens(input: Int, output: Int): Boolean = input > 0 || output > 0
}
