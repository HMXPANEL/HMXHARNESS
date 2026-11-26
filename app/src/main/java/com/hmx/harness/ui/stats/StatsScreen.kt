package com.hmx.harness.ui.stats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.hmx.harness.ui.theme.motionBlur
import com.hmx.harness.ui.common.localizedOption
import com.hmx.harness.data.UsageExport
import com.hmx.harness.data.UsageKind
import com.hmx.harness.data.db.SessionEntity
import com.hmx.harness.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.hmx.harness.AppContainer
import com.hmx.harness.data.SessionRepository
import com.hmx.harness.data.db.CodeActivityTotals
import com.hmx.harness.ui.common.AppHeader
import com.hmx.harness.ui.common.formatTokenCount
import com.hmx.harness.ui.theme.HarnessMono
import com.hmx.harness.ui.theme.LocalStatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hmx.harness.ui.common.TextButton

private enum class StatsSort(val label: String) { TOKENS("Tokens"), PRICE("Price"), REQUESTS("Requests") }
private data class UsageView(
    val stats: StatsSnapshot = StatsSnapshot(),
    val code: CodeActivityTotals = CodeActivityTotals(),
    val sessions: List<SessionEntity> = emptyList(),
)

@Composable
fun StatsScreen(container: AppContainer, onBack: () -> Unit) = StatsScreen(container.sessions, onBack)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreen(repository: SessionRepository, onBack: () -> Unit) {
    val uiContext = androidx.compose.ui.platform.LocalContext.current
    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    var sort by rememberSaveable { mutableStateOf(StatsSort.TOKENS) }
    var definitions by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(System.currentTimeMillis(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = System.currentTimeMillis(); delay(60_000) }
        }
    }
    val window = range.window(now)
    val flow = remember(repository, window, range) {
        combine(repository.sessions, repository.statsUsageBetween(window.since, window.until), repository.codeActivityBetween(window.since, window.until)) { sessions, usage, code ->
            UsageView(statsSnapshot(usage, sessions.takeIf { range == StatsRange.LIFETIME }), code, sessions)
        }
    }
    val view = key(window, range) { flow.collectAsStateWithLifecycle(initialValue = UsageView()).value }
    val bundle = view.stats.bundle
    val byKind = view.stats.byKind
    val byProvider = view.stats.byProvider
    val bySession = view.stats.bySession
    val sessionTitles = remember(view.sessions) { view.sessions.associate { it.id to it.title } }
    val models = view.stats.byModel.map { row -> row to com.hmx.harness.llm.ModelPrices.estimateSplit(
        model = row.model, totalInputTokens = row.inputTokens, outputTokens = row.outputTokens,
        cachedTokens = row.cachedTokens, cacheWriteTokens = row.cacheWriteTokens,
        peakInputTokens = row.peakInputTokens, peakOutputTokens = row.peakOutputTokens,
        peakCachedTokens = row.peakCachedTokens, peakCacheWriteTokens = row.peakCacheWriteTokens,
        providerKey = com.hmx.harness.llm.ModelsDev.providerKeyFor(row.providerName)) }
    val knownPrices = models.mapNotNull { it.second }
    val cost = knownPrices.takeIf { it.isNotEmpty() }?.sum()
    val code = view.code
    val scheme = MaterialTheme.colorScheme
    val green = LocalStatusColors.current.success
    val scope = rememberCoroutineScope()
    var pendingCsv by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingJson by remember { mutableStateOf<Pair<String, String>?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val exportFailedText = stringResource(R.string.ui_usage_export_failed)
    fun writeExport(uri: Uri?, pending: Pair<String, String>?) {
        if (uri == null || pending == null) return
        scope.launch(Dispatchers.IO) {
            try {
                uiContext.contentResolver.openOutputStream(uri)?.use { it.write(pending.second.toByteArray()) }
                    ?: error("no output stream")
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    exportError = exportFailedText
                }
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val pending = pendingCsv
        pendingCsv = null
        writeExport(uri, pending)
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val pending = pendingJson
        pendingJson = null
        writeExport(uri, pending)
    }
    fun exportSession(session: SessionUsage, json: Boolean) {
        scope.launch {
            val events = repository.usageEventsOnce(session.sessionId)
            val name = UsageExport.fileName(session.sessionId, if (json) "json" else "csv")
            val text = if (json) UsageExport.json(events) else UsageExport.csv(events)
            if (json) {
                pendingJson = name to text
                jsonLauncher.launch(name)
            } else {
                pendingCsv = name to text
                csvLauncher.launch(name)
            }
        }
    }
    @Composable fun kindLabel(kind: String): String = stringResource(when (kind) {
        UsageKind.SUBAGENT -> R.string.ui_usage_subagents
        UsageKind.COMPACTION -> R.string.ui_usage_compaction
        UsageKind.PROBE -> R.string.ui_usage_probes
        UsageKind.AUTOMATION -> R.string.ui_usage_automation
        else -> R.string.ui_usage_task
    })
    Scaffold(containerColor = scheme.surface, topBar = {
        AppHeader(uiContext.getString(R.string.settings_page_usage_title), subtitle = uiContext.getString(R.string.ui_model_activity_and_ai_code_changes), onBack = onBack)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).motionBlur().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (LocalDensity.current.fontScale > 1.2f) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatsRange.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            FilterChip(selected = range == option, onClick = { range = option }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                label = { Text(localizedOption(option.label), Modifier.fillMaxWidth(), textAlign = TextAlign.Center) })
                        }
                    }
                }
            } else SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                StatsRange.entries.forEachIndexed { index, option ->
                    SegmentedButton(selected = range == option, onClick = { range = option }, icon = {},
                        shape = SegmentedButtonDefaults.itemShape(index, StatsRange.entries.size)) {
                        Text(localizedOption(option.label), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiContext.getString(R.string.ui_model_activity), style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer)
                    Text(formatTokenCount(bundle.input + bundle.output), style = MaterialTheme.typography.displayMedium,
                        fontFamily = HarnessMono, color = scheme.onPrimaryContainer)
                    Text(uiContext.getString(R.string.ui_total_tokens_1396, (bundle.requests).toString(), (if (bundle.requests == 1L) "request" else "requests").toString(), (bundle.sessionCount).toString(), (if (bundle.sessionCount == 1) "session" else "sessions").toString()),
                        style = MaterialTheme.typography.bodySmall, color = scheme.onPrimaryContainer)
                    HorizontalDivider(color = scheme.onPrimaryContainer.copy(alpha = .18f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UsageMetric("Estimated cost", cost?.let { "$${"%.2f".format(it)}" } ?: "Unavailable", Modifier.weight(1f))
                        UsageMetric("Cache hit rate", if (bundle.input > 0) "${"%.1f".format(bundle.cached.toDouble() / bundle.input * 100)}%" else "—", Modifier.weight(1f))
                    }
                    if (knownPrices.size < models.size) Text(uiContext.getString(R.string.ui_cost_includes_models_with_known_list_prices), style = MaterialTheme.typography.labelSmall)
                }
            }
            UsageCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Code, null, tint = scheme.primary)
                    Column {
                        Text(uiContext.getString(R.string.ui_ai_code_changes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(uiContext.getString(R.string.ui_tracked_edits, (range.label).toString(), (code.edits).toString()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = green.copy(alpha = .09f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("+${lineCount(code.added)}", style = if (lineCount(code.added).length > 8) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall, fontFamily = HarnessMono, color = green)
                            Text(uiContext.getString(R.string.ui_lines_added), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = scheme.errorContainer.copy(alpha = .45f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("−${lineCount(code.removed)}", style = if (lineCount(code.removed).length > 8) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall, fontFamily = HarnessMono, color = scheme.error)
                            Text(uiContext.getString(R.string.ui_lines_removed), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                val total = code.added + code.removed
                if (total > 0) Row(Modifier.fillMaxWidth().height(6.dp)) {
                    if (code.added > 0) Box(Modifier.weight(code.added.toFloat() / total).fillMaxHeight().background(green, RoundedCornerShape(3.dp)))
                    if (code.removed > 0) Box(Modifier.weight(code.removed.toFloat() / total).fillMaxHeight().background(scheme.error, RoundedCornerShape(3.dp)))
                }
                val net = code.added - code.removed
                Text(uiContext.getString(R.string.ui_net_change_lines, (if (net >= 0) "+" else "−").toString(), (lineCount(kotlin.math.abs(net))).toString()), style = MaterialTheme.typography.bodyMedium)
                if (code.edits == 0L) Text(uiContext.getString(R.string.ui_no_tracked_code_changes_in_this_period), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text(uiContext.getString(R.string.ui_source_code_and_configuration_totals_stay_after), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            UsageCard {
                Text(uiContext.getString(R.string.ui_token_breakdown), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UsageMetric(uiContext.getString(R.string.ui_fresh_input), formatTokenCount(bundle.freshInput), Modifier.weight(1f))
                    UsageMetric(uiContext.getString(R.string.ui_output_1407), formatTokenCount(bundle.output), Modifier.weight(1f))
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = .5f))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UsageMetric(uiContext.getString(R.string.ui_cache_reads), formatTokenCount(bundle.cached), Modifier.weight(1f))
                    UsageMetric(uiContext.getString(R.string.ui_cache_writes_1409), formatTokenCount(bundle.cacheWrite), Modifier.weight(1f))
                }
            }
            UsageCard {
                Text(stringResource(R.string.ui_usage_by_provider), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (byProvider.isEmpty()) Text(stringResource(R.string.ui_no_dated_model_activity_in_this_period),
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                byProvider.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(color = scheme.outlineVariant.copy(alpha = .45f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(row.providerName.ifBlank { stringResource(R.string.ui_usage_no_session_scope) },
                                style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(R.string.ui_usage_requests_value, row.requests.toString()) +
                                    ((row.retriedRequests).takeIf { it > 0 }?.let { retried ->
                                        " · " + stringResource(R.string.ui_usage_retried_value, retried.toString())
                                    } ?: ""),
                                style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                            )
                        }
                        Text(formatTokenCount(row.totalTokens), fontFamily = HarnessMono,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            UsageCard {
                Text(stringResource(R.string.ui_usage_by_task), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOf(UsageKind.CHAT, UsageKind.SUBAGENT, UsageKind.COMPACTION, UsageKind.PROBE, UsageKind.AUTOMATION).forEach { kind ->
                    val row = byKind.firstOrNull { it.kind == kind }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(kindLabel(kind), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.ui_usage_requests_value, (row?.requests ?: 0).toString()) +
                                    ((row?.retriedRequests ?: 0).takeIf { it > 0 }?.let { retried ->
                                        " · " + stringResource(R.string.ui_usage_retried_value, retried.toString())
                                    } ?: ""),
                                style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                            )
                        }
                        Text(formatTokenCount(row?.totalTokens ?: 0L), fontFamily = HarnessMono,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = .5f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_usage_overhead), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.ui_usage_overhead_desc), style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant)
                    }
                    Text(formatTokenCount(byKind.filter { it.overhead }.sumOf { it.totalTokens }),
                        fontFamily = HarnessMono, style = MaterialTheme.typography.bodyMedium)
                }
                if (bundle.retriedRequests > 0) Text(
                    stringResource(R.string.ui_usage_retried_requests) + ": " + bundle.retriedRequests,
                    style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.ui_usage_cross_provider_note), style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant)
            }
            UsageCard {
                Text(stringResource(R.string.ui_usage_sessions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (bySession.isEmpty()) Text(stringResource(R.string.ui_no_dated_model_activity_in_this_period),
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                bySession.forEachIndexed { index, session ->
                    if (index > 0) HorizontalDivider(color = scheme.outlineVariant.copy(alpha = .45f))
                    val title = if (session.sessionId == UsageKind.NO_SESSION) {
                        stringResource(R.string.ui_usage_no_session_scope)
                    } else {
                        sessionTitles[session.sessionId]?.takeIf { it.isNotBlank() } ?: session.sessionId
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(R.string.ui_usage_requests_value, session.requests.toString()) +
                                    " · " + formatTokenCount(session.totalTokens),
                                style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                            )
                        }
                        if (session.sessionId != UsageKind.NO_SESSION) {
                            TextButton(onClick = { exportSession(session, json = false) }) {
                                Text(stringResource(R.string.ui_usage_export_csv))
                            }
                            TextButton(onClick = { exportSession(session, json = true) }) {
                                Text(stringResource(R.string.ui_usage_export_json))
                            }
                        }
                    }
                }
                exportError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.error) }
                if (bySession.any { it.sessionId == UsageKind.NO_SESSION }) Text(
                    stringResource(R.string.ui_usage_no_session_scope_desc),
                    style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            UsageCard {
                Text(uiContext.getString(R.string.ui_by_model), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    StatsSort.entries.forEachIndexed { index, option ->
                        SegmentedButton(selected = sort == option, onClick = { sort = option }, icon = {}, shape = SegmentedButtonDefaults.itemShape(index, StatsSort.entries.size)) { Text(localizedOption(option.label)) }
                    }
                }
                val sorted = when (sort) {
                    StatsSort.TOKENS -> models.sortedByDescending { it.first.totalTokens }
                    StatsSort.PRICE -> models.sortedByDescending { it.second ?: -1.0 }
                    StatsSort.REQUESTS -> models.sortedByDescending { it.first.requests }
                }
                if (sorted.isEmpty()) Text(stringResource(R.string.ui_no_dated_model_activity_in_this_period), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                val max = sorted.maxOfOrNull { it.first.totalTokens }?.coerceAtLeast(1) ?: 1
                sorted.forEachIndexed { index, (model, price) ->
                    if (index > 0) HorizontalDivider(color = scheme.outlineVariant.copy(alpha = .45f))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(model.model, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(uiContext.getString(R.string.ui_requests_extra84, (model.providerName).toString(), (model.requests).toString()), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(uiContext.getString(R.string.settings_agent_context_value, (formatTokenCount(model.totalTokens)).toString()), fontFamily = HarnessMono, style = MaterialTheme.typography.bodySmall)
                            Text(price?.let { "≈ $${"%.2f".format(it)}" } ?: uiContext.getString(R.string.ui_price_unavailable), style = MaterialTheme.typography.bodySmall)
                        }
                        LinearProgressIndicator(progress = { model.totalTokens.toFloat() / max }, modifier = Modifier.fillMaxWidth().height(4.dp))
                    }
                }
            }
            UsageCard {
                TextButton(onClick = { definitions = !definitions }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Text(uiContext.getString(R.string.ui_how_usage_is_counted), Modifier.weight(1f), color = scheme.onSurface)
                    Icon(if (definitions) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                }
                if (definitions) {
                    Text(uiContext.getString(R.string.ui_ai_code_changes_count_applied_source_and), style = MaterialTheme.typography.bodySmall)
                    Text(uiContext.getString(R.string.ui_tracked_file_tools_and_eligible_local_foreground), style = MaterialTheme.typography.bodySmall)
                    Text(uiContext.getString(R.string.ui_today_uses_your_device_s_calendar_day), style = MaterialTheme.typography.bodySmall)
                    Text(uiContext.getString(R.string.ui_token_totals_include_subagents_and_compaction_cache), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun UsageCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

private fun lineCount(value: Long): String = java.text.NumberFormat.getIntegerInstance().format(value)

@Composable
private fun UsageMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontFamily = HarnessMono)
    }
}
