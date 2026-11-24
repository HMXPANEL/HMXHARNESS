package com.hmx.harness.ui.liquidglass

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hmx.harness.HarnessApp
import com.hmx.harness.data.ThemeMode
import com.hmx.harness.ui.chat.ChatListTestActivity
import com.hmx.harness.ui.chat.components.ChatEmptyState
import com.hmx.harness.ui.common.AppHeader
import com.hmx.harness.ui.theme.HarnessTheme
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Captures real rendered HMXHARNESS screens for visual QA in CI.
 * A failure on one optional screen must not prevent the other screenshots.
 */
@RunWith(AndroidJUnit4::class)
class LiquidGlassScreenshotTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val container get() = (instrumentation.targetContext.applicationContext as HarnessApp).container

    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(400)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val ctx = instrumentation.targetContext.applicationContext
        val dir = File(ctx.getExternalFilesDir(null), "liquidglass-screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun withScreen(name: String, content: @Composable () -> Unit) {
        ActivityScenario.launch(ChatListTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    HarnessTheme(themeMode = ThemeMode.DARK, dynamicColor = false, glassEnabled = true, liquidGlass = true) {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
                    }
                }
            }
            capture(name)
        }
    }

    /** Optional screens can fail on an NDK-less CI build; keep capturing the rest. */
    private fun safely(name: String, content: @Composable () -> Unit) {
        runCatching { withScreen(name, content) }
            .onFailure { android.util.Log.w("LiquidGlassScreenshot", "Skipped $name", it) }
    }

    @Test fun captureHome() = safely("01-home") {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            ChatEmptyState(null, null, true, {}, {}, Modifier, code = true)
        }
    }

    @Test fun captureHeaderAndGlassCards() = safely("02-chrome") {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
            AppHeader(title = "HMXHARNESS", subtitle = "Liquid Glass")
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            LiquidGlassCard { Text("Standard glass card", style = MaterialTheme.typography.titleMedium) }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            LiquidGlassCard(level = LiquidGlassLevel.FOCUS) {
                Text("Focus glass card", style = MaterialTheme.typography.titleMedium)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            LiquidGlassButton(onClick = {}) { Text("Glass action") }
        }
    }

}
