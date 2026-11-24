package com.hmx.harness.ui.showcase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hmx.harness.AppContainer
import com.hmx.harness.HarnessApp
import com.hmx.harness.core.ChatMessage
import com.hmx.harness.core.Role
import com.hmx.harness.data.AppSettings
import com.hmx.harness.data.ThemeMode
import com.hmx.harness.ui.AppNav
import com.hmx.harness.ui.chat.ChatScreen
import com.hmx.harness.ui.chat.ChatViewModel
import com.hmx.harness.ui.common.ExpressiveDropdownMenu
import com.hmx.harness.ui.common.ExpressiveDropdownMenuItem
import com.hmx.harness.ui.files.WorkspaceToolMode
import com.hmx.harness.ui.files.WorkspaceToolsSheet
import com.hmx.harness.ui.liquidglass.GlassDialog
import com.hmx.harness.ui.settings.ProvidersScreen
import com.hmx.harness.ui.settings.SettingsPage
import com.hmx.harness.ui.settings.SettingsScreen
import com.hmx.harness.ui.setup.SetupScreen
import com.hmx.harness.ui.theme.HarnessTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import java.util.UUID

/**
 * Debug-only host for cloud screenshot QA.
 *
 * Every screen below is the shipped screen composable, rendered with the real
 * [AppContainer], repositories and ViewModels. The activity only picks which
 * screen to show so a screenshot run does not need fragile UI automation.
 * It is never part of a release build.
 */
class GlassShowcaseActivity : ComponentActivity() {
    // A single process stays alive while the cloud script broadcasts screen
    // changes. Relaunching the activity for every screenshot churns GPU layers
    // and kills the software emulator after a few captures.
    private val screenState = mutableStateOf(SCREEN_HOME)
    private val themeState = mutableStateOf<ThemeMode?>(null)
    private val drawerState = mutableStateOf(false)
    private val routeState = mutableStateOf<String?>(null)
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra(EXTRA_SCREEN)?.let { screenState.value = it }
            intent.getStringExtra(EXTRA_THEME)?.let { value ->
                themeState.value = runCatching { ThemeMode.valueOf(value.uppercase()) }.getOrNull()
            }
            drawerState.value = intent.getStringExtra(EXTRA_DRAWER) == "open"
            if (intent.hasExtra(EXTRA_ROUTE)) routeState.value = intent.getStringExtra(EXTRA_ROUTE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val container = (application as HarnessApp).container
        screenState.value = intent.getStringExtra(EXTRA_SCREEN) ?: SCREEN_HOME
        themeState.value = intent.getStringExtra(EXTRA_THEME)?.let {
            runCatching { ThemeMode.valueOf(it.uppercase()) }.getOrNull()
        }
        drawerState.value = intent.getStringExtra(EXTRA_DRAWER) == "open"
        routeState.value = intent.getStringExtra(EXTRA_ROUTE)
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter(ACTION_SET_SCREEN),
            ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
        val capture = intent.getStringExtra(EXTRA_CAPTURE)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val screen = screenState.value
            val themeMode = themeState.value ?: settings.themeMode
            val dark = when (themeMode) {
                com.hmx.harness.data.ThemeMode.LIGHT -> false
                com.hmx.harness.data.ThemeMode.DARK,
                com.hmx.harness.data.ThemeMode.AMOLED -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            androidx.compose.runtime.SideEffect {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
            HarnessTheme(
                themeMode = themeMode,
                dynamicColor = settings.dynamicColor,
                progressBarStyle = settings.progressBarStyle,
                colorPalette = settings.colorPalette,
                glassEnabled = true,
                liquidGlass = true,
                glassTransparency = settings.harnessGlassTransparency,
                motionBlurEnabled = settings.motionBlurEnabled,
                motionBlurIntensity = settings.motionBlurIntensity,
            ) {
                Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                    when (screen) {
                        SCREEN_ONBOARDING -> SetupScreen(container) {}
                        SCREEN_SETTINGS -> SettingsScreen(container, onBack = {})
                        SCREEN_APPEARANCE -> SettingsScreen(container, onBack = {}, initialPage = SettingsPage.APPEARANCE)
                        SCREEN_GITHUB -> SettingsScreen(container, onBack = {}, initialPage = SettingsPage.CONNECTED_ACCOUNTS)
                        SCREEN_9ROUTER -> SettingsScreen(container, onBack = {}, initialPage = SettingsPage.NINE_ROUTER)
                        SCREEN_PROVIDERS -> ProvidersScreen(container, onBack = {})
                        SCREEN_CHAT -> ChatShowcase(container, seeded = true)
                        SCREEN_NAV -> AppNavShowcase(container, openDrawer = drawerState.value, initialRoute = routeState.value)
                        SCREEN_TOOLS -> WorkspaceToolsSheet(container, null, null, WorkspaceToolMode.COMMANDS, {}, { _, _ -> }, emptyList())
                        SCREEN_QUICK_OPEN -> WorkspaceToolsSheet(container, null, null, WorkspaceToolMode.FILES, {}, { _, _ -> }, emptyList())
                        SCREEN_MENU -> MenuShowcase()
                        SCREEN_DIALOG -> DialogShowcase()
                        else -> ChatShowcase(container, seeded = false)
                    }
                }
            }
        }
        // Debug-only capture: draw the real view hierarchy into a bitmap instead of
        // asking the system screencap service (which kills SwiftShader emulators).
        if (!capture.isNullOrBlank()) {
            window.decorView.postDelayed({
                runCatching {
                    val view = window.decorView
                    if (view.width <= 0 || view.height <= 0) return@runCatching
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    val dir = File(getExternalFilesDir(null), "liquidglass-screenshots").apply { mkdirs() }
                    File(dir, "$capture.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    android.util.Log.i("GlassShowcase", "captured $capture ${view.width}x${view.height}")
                }.onFailure { android.util.Log.w("GlassShowcase", "capture failed for $capture", it) }
            }, 4000)
        }
    }

    override fun onDestroy() {
        if (receiverRegistered) unregisterReceiver(screenReceiver)
        receiverRegistered = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_SET_SCREEN = "com.hmx.harness.SET_GLASS_SCREEN"
        const val EXTRA_SCREEN = "screen"
        const val EXTRA_CAPTURE = "capture"
        const val EXTRA_THEME = "theme"
        const val EXTRA_DRAWER = "drawer"
        const val EXTRA_ROUTE = "route"
        const val SCREEN_HOME = "home"
        const val SCREEN_ONBOARDING = "onboarding"
        const val SCREEN_SETTINGS = "settings"
        const val SCREEN_PROVIDERS = "providers"
        const val SCREEN_CHAT = "chat"
        const val SCREEN_NAV = "appnav"
        const val SCREEN_TOOLS = "tools"
        const val SCREEN_QUICK_OPEN = "quickopen"
        const val SCREEN_APPEARANCE = "appearance"
        const val SCREEN_GITHUB = "github"
        const val SCREEN_9ROUTER = "9router"
        const val SCREEN_MENU = "menu"
        const val SCREEN_DIALOG = "dialog"
    }
}

/** The real chat screen; [seeded] adds a stored conversation so the answer layout is visible. */
@Composable
private fun ChatShowcase(container: AppContainer, seeded: Boolean) {
    var sessionId by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(!seeded) }
    LaunchedEffect(seeded) {
        if (seeded) {
            sessionId = runCatching { seedConversation(container) }.getOrNull()
            ready = true
        }
    }
    if (!ready) return
    val viewModel = remember(sessionId) { ChatViewModel(container, sessionId) }
    ChatScreen(
        viewModel = viewModel,
        onOpenDrawer = {},
        onOpenFile = { _, _ -> },
        onNewChat = {},
        onOpenTerminal = {},
        onOpenSubagent = {},
    )
}

/** Stored messages, not fake UI state: the same rows a real conversation would hold. */
private suspend fun seedConversation(container: AppContainer): String {
    val id = container.sessions.createSession("Liquid Glass interface")
    container.sessions.addMessage(id, ChatMessage(
        id = UUID.randomUUID().toString(),
        role = Role.USER,
        text = "Show me the new Liquid Glass interface.",
        createdAt = System.currentTimeMillis(),
    ))
    container.sessions.addMessage(id, ChatMessage(
        id = UUID.randomUUID().toString(),
        role = Role.ASSISTANT,
        text = "The whole app is glass now: the drawer, the composer and every panel " +
            "sample the background behind them, bend its light at the edges and keep " +
            "the text fully opaque so it stays readable.",
        createdAt = System.currentTimeMillis(),
    ))
    return id
}

/** Real navigation shell, used to photograph the drawer after an edge swipe. */
@Composable
private fun AppNavShowcase(container: AppContainer, openDrawer: Boolean = false, initialRoute: String? = null) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { container.settings.setOnboardingDone(true) }
        ready = true
    }
    if (ready) AppNav(container, initialDrawerOpen = openDrawer, initialRoute = initialRoute)
}

/** Glass popup surface with real menu rows, captured in its open state. */
@Composable
private fun MenuShowcase() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        ExpressiveDropdownMenu(expanded = true, onDismissRequest = {}, modifier = Modifier.padding(start = 40.dp, top = 120.dp)) {
            listOf("New chat", "New code chat", "Search workspace", "Providers", "Settings").forEach { label ->
                ExpressiveDropdownMenuItem(text = { Text(label) }, onClick = {})
            }
        }
    }
}

/** Glass dialog surface using the same shared material as the rest of the app. */
@Composable
private fun DialogShowcase() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassDialog(modifier = Modifier.padding(24.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                Text("Replace project files?", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Text("The selected files will be written to the workspace. This action can be undone from Changes.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.TextButton(onClick = {}) { Text("Cancel") }
                    androidx.compose.material3.TextButton(onClick = {}) { Text("Replace") }
                }
            }
        }
    }
}
