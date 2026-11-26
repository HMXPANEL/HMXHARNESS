package com.hmx.harness.ui.stats

import com.hmx.harness.data.UsageKind
import com.hmx.harness.data.db.ModelUsagePojo
import com.hmx.harness.data.db.SessionEntity
import com.hmx.harness.data.db.StatsUsagePojo
import java.time.Instant
import java.time.ZoneId

internal data class StatsWindow(val since: Long, val until: Long)

internal enum class StatsRange(val label: String, private val days: Long?) {
    DAY("Today", 1),
    WEEK("1 week", 7),
    MONTH("1 month", 30),
    LIFETIME("Lifetime", null);

    fun window(now: Long, zone: ZoneId = ZoneId.systemDefault()): StatsWindow {
        val count = days ?: return StatsWindow(0, Long.MAX_VALUE)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return StatsWindow(today.minusDays(count - 1).atStartOfDay(zone).toInstant().toEpochMilli(),
            today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
    }
}

internal data class StatsBundle(
    val input: Long = 0,
    val output: Long = 0,
    val cached: Long = 0,
    val cacheWrite: Long = 0,
    val requests: Long = 0,
    val sessionCount: Int = 0,
    /** Best session cache hit rate from usage in the selected window. */
    val peakHitRate: Double? = null,
    /** Requests that needed more than the first attempt. */
    val retriedRequests: Long = 0,
) {
    val freshInput: Long get() = (input - cached - cacheWrite).coerceAtLeast(0)
}

/** Token/request totals for one task kind. */
internal data class KindUsage(
    val kind: String,
    val input: Long,
    val output: Long,
    val cached: Long,
    val cacheWrite: Long,
    val requests: Long,
    val retriedRequests: Long,
) {
    val totalTokens: Long get() = input + output
    val overhead: Boolean get() = UsageKind.isOverhead(kind)
}

/** Token/request totals for one provider. Tokens are provider-reported, not a plan percentage. */
internal data class ProviderUsage(
    val providerName: String,
    val input: Long,
    val output: Long,
    val cached: Long,
    val cacheWrite: Long,
    val requests: Long,
    val retriedRequests: Long,
) {
    val totalTokens: Long get() = input + output
}

/** Token/request totals for one session. An empty id means "outside a chat". */
internal data class SessionUsage(
    val sessionId: String,
    val input: Long,
    val output: Long,
    val cached: Long,
    val cacheWrite: Long,
    val requests: Long,
    val retriedRequests: Long,
) {
    val totalTokens: Long get() = input + output
}

internal data class StatsSnapshot(
    val bundle: StatsBundle = StatsBundle(),
    val byModel: List<ModelUsagePojo> = emptyList(),
    val byProvider: List<ProviderUsage> = emptyList(),
    val byKind: List<KindUsage> = emptyList(),
    val bySession: List<SessionUsage> = emptyList(),
)

/** The hero, breakdown and cache metrics share one dated request snapshot. */
internal fun statsSnapshot(rows: List<StatsUsagePojo>, lifetimeSessions: List<SessionEntity>? = null): StatsSnapshot {
    // Older versions saved only session totals. Keep them in Lifetime without
    // inventing dates for those requests or attributing them to a recent chat touch.
    val totals = lifetimeSessions?.map { s ->
        StatsUsagePojo(s.id, "", "", s.totalInputTokens, s.totalOutputTokens,
            s.totalCachedTokens, s.totalCacheWriteTokens, s.requestCount)
    } ?: rows
    val sessionTotals = totals.groupBy { it.sessionId }
    // Provider checks and automation setup live outside a chat; they must not
    // inflate the session count or win the per-session cache score.
    val chatSessionTotals = sessionTotals.filterKeys { it != UsageKind.NO_SESSION }
    val bundle = StatsBundle(totals.sumOf { it.inputTokens }, totals.sumOf { it.outputTokens },
        totals.sumOf { it.cachedTokens }, totals.sumOf { it.cacheWriteTokens }, totals.sumOf { it.requests },
        chatSessionTotals.size, chatSessionTotals.values.mapNotNull { session ->
            val input = session.sumOf { it.inputTokens }
            if (input > 0) session.sumOf { it.cachedTokens }.toDouble() / input else null
        }.maxOrNull(), totals.sumOf { it.retriedRequests })
    val models = rows.groupBy { it.providerName to it.model }.map { (identity, modelRows) ->
        ModelUsagePojo(identity.first, identity.second, modelRows.sumOf { it.inputTokens },
            modelRows.sumOf { it.outputTokens }, modelRows.sumOf { it.cachedTokens },
            modelRows.sumOf { it.cacheWriteTokens }, modelRows.sumOf { it.requests },
            peakInputTokens = modelRows.sumOf { it.peakInputTokens },
            peakOutputTokens = modelRows.sumOf { it.peakOutputTokens },
            peakCachedTokens = modelRows.sumOf { it.peakCachedTokens },
            peakCacheWriteTokens = modelRows.sumOf { it.peakCacheWriteTokens })
    }.sortedByDescending { it.totalTokens }
    // Kind/session rows come from dated events only; the lifetime fallback
    // above still keeps old session totals in the hero without inventing dates.
    val byProvider = rows.groupBy { it.providerName }.map { (providerName, providerRows) ->
        ProviderUsage(
            providerName = providerName,
            input = providerRows.sumOf { it.inputTokens },
            output = providerRows.sumOf { it.outputTokens },
            cached = providerRows.sumOf { it.cachedTokens },
            cacheWrite = providerRows.sumOf { it.cacheWriteTokens },
            requests = providerRows.sumOf { it.requests },
            retriedRequests = providerRows.sumOf { it.retriedRequests },
        )
    }.sortedByDescending { it.totalTokens }
    val byKind = rows.groupBy { UsageKind.normalize(it.kind) }.map { (kind, kindRows) ->
        KindUsage(
            kind = kind,
            input = kindRows.sumOf { it.inputTokens },
            output = kindRows.sumOf { it.outputTokens },
            cached = kindRows.sumOf { it.cachedTokens },
            cacheWrite = kindRows.sumOf { it.cacheWriteTokens },
            requests = kindRows.sumOf { it.requests },
            retriedRequests = kindRows.sumOf { it.retriedRequests },
        )
    }.sortedByDescending { it.totalTokens }
    val bySession = rows.groupBy { it.sessionId }.map { (sessionId, sessionRows) ->
        SessionUsage(
            sessionId = sessionId,
            input = sessionRows.sumOf { it.inputTokens },
            output = sessionRows.sumOf { it.outputTokens },
            cached = sessionRows.sumOf { it.cachedTokens },
            cacheWrite = sessionRows.sumOf { it.cacheWriteTokens },
            requests = sessionRows.sumOf { it.requests },
            retriedRequests = sessionRows.sumOf { it.retriedRequests },
        )
    }.sortedByDescending { it.totalTokens }
    return StatsSnapshot(bundle, models, byProvider, byKind, bySession)
}
