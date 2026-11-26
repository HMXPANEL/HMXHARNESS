package com.hmx.harness.data

import com.hmx.harness.data.db.UsageEventEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Local, user-initiated usage export. Only the accounting fields below are
 * written: never prompts, responses, code, file paths or credentials.
 *
 * Both formats are chronological so a user can line them up with the in-app
 * session breakdown without re-sorting.
 */
object UsageExport {
    private val json = Json { prettyPrint = true }

    private val csvColumns = listOf(
        "session_id", "timestamp", "provider", "model",
        "input_tokens", "output_tokens", "cached_tokens", "cache_write_tokens",
        "kind", "attempt",
    )

    fun csv(events: List<UsageEventEntity>): String = buildString {
        append(csvColumns.joinToString(",")).append('\n')
        ordered(events).forEach { event ->
            append(csvRow(
                event.sessionId,
                timestamp(event.createdAt),
                event.providerName,
                event.model,
                event.inputTokens.toString(),
                event.outputTokens.toString(),
                event.cachedTokens.toString(),
                event.cacheWriteTokens.toString(),
                event.kind,
                event.attempt.toString(),
            )).append('\n')
        }
    }

    fun json(events: List<UsageEventEntity>): String {
        val array = buildJsonArray {
            ordered(events).forEach { event ->
                addJsonObject {
                    put("sessionId", event.sessionId)
                    put("timestamp", timestamp(event.createdAt))
                    put("provider", event.providerName)
                    put("model", event.model)
                    put("inputTokens", event.inputTokens)
                    put("outputTokens", event.outputTokens)
                    put("cachedTokens", event.cachedTokens)
                    put("cacheWriteTokens", event.cacheWriteTokens)
                    put("kind", event.kind)
                    put("attempt", event.attempt)
                }
            }
        }
        return json.encodeToString(JsonArray.serializer(), array)
    }

    fun fileName(sessionId: String, extension: String): String {
        val safe = sessionId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(40)
        return "usage-${safe.ifEmpty { "session" }}.$extension"
    }

    private fun ordered(events: List<UsageEventEntity>): List<UsageEventEntity> =
        events.sortedWith(compareBy({ it.createdAt }, { it.rowId }))

    private fun timestamp(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    private fun csvRow(vararg cells: String): String = cells.joinToString(",") { csvCell(it) }

    /** RFC 4180 quoting: only fields that would otherwise change the column layout. */
    private fun csvCell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
