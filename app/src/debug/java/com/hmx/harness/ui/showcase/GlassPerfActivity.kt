package com.hmx.harness.ui.showcase

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hmx.harness.HarnessApp
import com.hmx.harness.data.AppSettings
import com.hmx.harness.ui.liquidglass.GlassCard
import com.hmx.harness.ui.liquidglass.GlassTier
import com.hmx.harness.ui.liquidglass.LocalGlassTier
import com.hmx.harness.ui.liquidglass.LocalGlassProfile
import com.hmx.harness.ui.liquidglass.profileFor
import androidx.compose.runtime.CompositionLocalProvider
import com.hmx.harness.ui.theme.HarnessTheme
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

/**
 * Debug-only frame sampling on the shipped glass components. It scrolls a real
 * LazyColumn of glass cards and reports measured frame intervals; it never
 * fabricates numbers and is absent from release builds.
 */
class GlassPerfActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val profile = intent.getStringExtra(EXTRA_PROFILE) ?: "mid"
        val forcedTier = intent.getStringExtra(EXTRA_TIER)?.let {
            runCatching { GlassTier.valueOf(it.uppercase()) }.getOrNull()
        }
        val container = (application as HarnessApp).container
        var report = ""
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            HarnessTheme(
                themeMode = settings.themeMode,
                dynamicColor = false,
                glassEnabled = true,
                liquidGlass = true,
                glassTransparency = settings.harnessGlassTransparency,
            ) {
                CompositionLocalProvider(
                    LocalGlassTier provides (forcedTier ?: LocalGlassTier.current),
                    LocalGlassProfile provides profileFor(forcedTier ?: LocalGlassTier.current),
                ) {
                    PerfScreen(profile) { report = it }
                }
            }
        }
        window.decorView.postDelayed({
            runCatching {
                val dir = File(getExternalFilesDir(null), "liquidglass-perf").apply { mkdirs() }
                File(dir, "$profile.txt").writeText(report.ifBlank { "no samples collected" })
                android.util.Log.i("GlassPerf", "$profile: $report")
            }.onFailure { android.util.Log.w("GlassPerf", "write failed", it) }
        }, 12_000)
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_TIER = "tier"
    }
}

@Composable
private fun PerfScreen(profile: String, onReport: (String) -> Unit) {
    val listState = rememberLazyListState()
    val tier = LocalGlassTier.current
    var running by remember { mutableStateOf(true) }
    val frames = remember { ArrayList<Long>() }

    LaunchedEffect(Unit) {
        delay(1_500)
        val choreographer = Choreographer.getInstance()
        var last = 0L
        val callback = object : FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (last != 0L) frames.add((frameTimeNanos - last) / 1_000_000L)
                last = frameTimeNanos
                if (running) choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)
        repeat(3) {
            listState.animateScrollToItem(40)
            delay(250)
            listState.animateScrollToItem(0)
            delay(250)
        }
        delay(700)
        running = false
        onReport(buildReport(profile, tier.name, frames))
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text("Glass performance · $profile", style = MaterialTheme.typography.headlineSmall)
                    Text("Tier: ${tier.name} · measured while scrolling glass cards", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(80) { index ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleMedium)
                        Text("Liquid Glass performance sample row", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun buildReport(profile: String, tier: String, frames: List<Long>): String {
    if (frames.isEmpty()) return "$profile tier=$tier samples=0"
    val sorted = frames.sorted()
    val average = frames.sum().toDouble() / frames.size
    val p95 = sorted[(sorted.size * .95).toInt().coerceAtMost(sorted.size - 1)]
    val jank = frames.count { it > 32 }
    val runtime = Runtime.getRuntime()
    return buildString {
        appendLine("profile=$profile tier=$tier samples=${frames.size}")
        appendLine("avg_frame_ms=${(average * 100).roundToInt() / 100.0}")
        appendLine("p95_frame_ms=$p95")
        appendLine("jank_frames_gt_32ms=$jank")
        appendLine("java_heap_used_mb=${(runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)}")
        appendLine("java_heap_max_mb=${runtime.maxMemory() / (1024 * 1024)}")
        appendLine("measured_on=${System.currentTimeMillis()}")
    }
}
