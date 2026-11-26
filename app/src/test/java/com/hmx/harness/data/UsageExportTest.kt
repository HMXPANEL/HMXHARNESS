package com.hmx.harness.data

import com.hmx.harness.data.db.UsageEventEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageExportTest {
    private fun row(
        rowId: Long,
        createdAt: Long,
        sessionId: String = "session-1",
        provider: String = "Provider",
        model: String = "model",
        kind: String = UsageKind.CHAT,
        attempt: Int = 0,
        input: Long = 10,
        output: Long = 2,
        cached: Long = 3,
        cacheWrite: Long = 1,
    ) = UsageEventEntity(
        rowId = rowId, sessionId = sessionId, providerName = provider, model = model,
        inputTokens = input, outputTokens = output, cachedTokens = cached,
        cacheWriteTokens = cacheWrite, createdAt = createdAt, kind = kind, attempt = attempt,
    )

    @Test fun `csv is chronological and carries kind and attempt`() {
        val csv = UsageExport.csv(listOf(
            row(rowId = 2, createdAt = 2000, kind = UsageKind.COMPACTION, attempt = 1),
            row(rowId = 1, createdAt = 1000, sessionId = "session-0", provider = "Other"),
        ))
        val lines = csv.trimEnd().lines()
        assertEquals(UsageExport.csv(listOf(row(1, 1000))).lineSequence().first(), lines.first())
        assertTrue(lines[1].startsWith("session-0,"))
        assertTrue(lines[1].endsWith(",chat,0"))
        assertTrue(lines[2].startsWith("session-1,"))
        assertTrue(lines[2].endsWith(",compaction,1"))
    }

    @Test fun `csv quotes separators and quotes without changing columns`() {
        val csv = UsageExport.csv(listOf(row(1, 1000, provider = "A,B", model = "x\"y")))
        val data = csv.trimEnd().lines()[1]
        assertTrue(data.contains("\"A,B\""))
        assertTrue(data.contains("\"x\"\"y\""))
        assertEquals(10, splitCsv(data).size)
        // A newline inside a field is quoted, never emitted as a bare row break.
        assertTrue(UsageExport.csv(listOf(row(1, 1000, model = "line\nbreak"))).contains("\"line\nbreak\""))
    }

    @Test fun `empty export still has the documented header`() {
        assertEquals(
            "session_id,timestamp,provider,model,input_tokens,output_tokens,cached_tokens,cache_write_tokens,kind,attempt",
            UsageExport.csv(emptyList()).trimEnd(),
        )
        assertEquals(0, Json.parseToJsonElement(UsageExport.json(emptyList())).jsonArray.size)
    }

    @Test fun `json contains only accounting fields and never prompt text`() {
        val parsed = Json.parseToJsonElement(UsageExport.json(listOf(
            row(1, 1_700_000_000_000, provider = "P", model = "M", kind = UsageKind.SUBAGENT, attempt = 2),
        ))).jsonArray.single().jsonObject
        assertEquals(
            setOf("sessionId", "timestamp", "provider", "model", "inputTokens", "outputTokens",
                "cachedTokens", "cacheWriteTokens", "kind", "attempt"),
            parsed.keys,
        )
        assertEquals("subagent", parsed.getValue("kind").jsonPrimitive.content)
        assertEquals("2", parsed.getValue("attempt").jsonPrimitive.content)
        assertEquals("2023-11-14T22:13:20Z", parsed.getValue("timestamp").jsonPrimitive.content)
        assertFalse(parsed.keys.any { it.contains("prompt", ignoreCase = true) || it.contains("response", ignoreCase = true) })
    }

    @Test fun `multi provider rows keep their own identity and totals`() {
        val json = Json.parseToJsonElement(UsageExport.json(listOf(
            row(1, 1000, provider = "P1", model = "m1", input = 100, output = 10),
            row(2, 2000, provider = "P2", model = "m2", input = 200, output = 20),
        ))).jsonArray.map { it.jsonObject }
        assertEquals(listOf("P1", "P2"), json.map { it.getValue("provider").jsonPrimitive.content })
        assertEquals(listOf("100", "200"), json.map { it.getValue("inputTokens").jsonPrimitive.content })
    }

    @Test fun `export file names are filesystem safe`() {
        assertEquals("usage-session-1.csv", UsageExport.fileName("session-1", "csv"))
        assertFalse(UsageExport.fileName("../../etc/passwd", "json").contains('/'))
        assertEquals("usage-session.json", UsageExport.fileName("", "json"))
    }

    private fun splitCsv(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val c = line[index]
            when {
                quoted && c == '"' && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"'); index++
                }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { cells += current.toString(); current.clear() }
                else -> current.append(c)
            }
            index++
        }
        cells += current.toString()
        return cells
    }
}
