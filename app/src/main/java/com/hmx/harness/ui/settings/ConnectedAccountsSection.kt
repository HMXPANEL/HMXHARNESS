package com.hmx.harness.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hmx.harness.AppContainer
import com.hmx.harness.chatgpt.ChatGptProtocol
import com.hmx.harness.R
import com.hmx.harness.data.AppSettings
import com.hmx.harness.github.GitHubConnectionState
import com.hmx.harness.ui.common.openOAuthBrowser
import com.hmx.harness.ui.common.Button
import com.hmx.harness.ui.common.OutlinedButton
import com.hmx.harness.ui.theme.HarnessGlassAlertDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.hmx.harness.ui.common.TextButton

@Composable
internal fun ConnectedAccountsSection(container: AppContainer) {
    val uiContext = androidx.compose.ui.platform.LocalContext.current
    val state by container.chatGpt.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingSignOut by remember { mutableStateOf<String?>(null) }
    var selecting by remember { mutableStateOf<String?>(null) }
    var selectionError by remember { mutableStateOf<String?>(null) }
    fun update(action: suspend () -> Unit) { scope.launch {
        try { action(); selectionError = null }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            selectionError = when {
                e is java.io.IOException && e.message?.startsWith("ChatGPT") == true -> e.message
                e.message in setOf(uiContext.getString(R.string.ui_this_account_did_not_offer_any_models), uiContext.getString(R.string.ui_reconnect_chatgpt_to_use_this_account),
                    uiContext.getString(R.string.ui_choose_a_model_offered_by_this_account)) -> e.message
                else -> uiContext.getString(R.string.ui_could_not_update_this_account_check_your)
            }
        }
    } }
    val github by container.github.state.collectAsStateWithLifecycle()
    GitHubAccountCard(container, github)
    ChatGptAccountsPanel(state, settings.activeProviderId, settings.activeModel, selecting,
        error = selectionError ?: state.error,
        onAdd = { container.chatGpt.startSignIn { openOAuthBrowser(context, Uri.parse(it)) } },
        onCancelSignIn = container.chatGpt::cancelSignIn,
        onSelect = { account ->
            if (selecting == null) {
                selecting = account.providerId
                update {
                    try {
                        val config = container.chatGpt.selectAccount(account.providerId, settings.activeModel.orEmpty())
                        container.settings.setActiveSelection(config.id, config.model)
                    } finally { selecting = null }
                }
            }
        },
        onRefresh = { container.chatGpt.refresh(it.providerId) },
        onCheckModels = { container.chatGpt.checkNewerModels(it.providerId) },
        onReconnect = { account -> container.chatGpt.startSignIn(account.providerId) { openOAuthBrowser(context, Uri.parse(it)) } },
        onSignOut = { pendingSignOut = it.providerId },
        onUsage = { openOAuthBrowser(context, Uri.parse(ChatGptProtocol.USAGE_URL)) },
    )
    if (state.showWelcome) AlertDialog(
        onDismissRequest = { update { container.chatGpt.dismissWelcome() } },
        title = { Text(stringResource(R.string.settings_accounts_using_plan)) },
        text = { Text(stringResource(R.string.settings_accounts_using_plan_desc)) },
        confirmButton = { TextButton(onClick = { update { container.chatGpt.dismissWelcome() } }) { Text(stringResource(R.string.settings_action_got_it)) } },
    )
    pendingSignOut?.let { id -> AlertDialog(
        onDismissRequest = { pendingSignOut = null },
        title = { Text(stringResource(R.string.settings_accounts_signout_title)) },
        text = { Text(stringResource(R.string.settings_accounts_signout_message)) },
        confirmButton = { TextButton(onClick = { container.chatGpt.signOut(id); pendingSignOut = null }) { Text(stringResource(R.string.settings_chatgpt_sign_out)) } },
        dismissButton = { TextButton(onClick = { pendingSignOut = null }) { Text(stringResource(R.string.settings_action_cancel)) } },
    ) }
}

/**
 * GitHub Device Flow card shown with the other connected accounts.
 *
 * The flow needs only the public OAuth App Client ID: no client secret, private
 * key or backend. The token is written through the app's Android Keystore
 * backed store, never to plain preferences or logs.
 */
@Composable
private fun GitHubAccountCard(container: AppContainer, state: GitHubConnectionState) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    // Read callback strings during composition so they stay configuration-aware.
    val openBrowserFailed = stringResource(R.string.settings_github_open_browser_failed)
    val signInCodeLabel = stringResource(R.string.ui_github_sign_in_code)
    val copiedFormat = stringResource(R.string.ui_copied)
    val couldNotCopy = stringResource(R.string.ui_could_not_copy_code)
    var confirmDisconnect by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var avatar by remember(state.avatarUrl) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }

    LaunchedEffect(state.avatarUrl) {
        val url = state.avatarUrl
        avatar = if (url.isBlank()) null else runCatching {
            val bytes = container.github.api.image(url)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { localError = String.format(openBrowserFailed, url) }
    }
    fun copyCode(code: String) {
        runCatching {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText(signInCodeLabel, code))
        }.onSuccess {
            localError = String.format(copiedFormat, signInCodeLabel)
        }.onFailure {
            localError = couldNotCopy
        }
    }

    val connected = state.login != null
    SettingsPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(scheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = avatar
                    if (image != null) {
                        Image(image, contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Icon(painterResource(R.drawable.ic_github), contentDescription = null,
                            tint = scheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("GitHub", style = MaterialTheme.typography.titleMedium)
                    if (connected) {
                        Text(stringResource(R.string.ui_connected_as, "@${state.login}"),
                            style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                    }
                }
            }

            val code = state.code
            if (code != null) {
                Text(stringResource(R.string.settings_github_copy_code_note), style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant)
                Surface(shape = MaterialTheme.shapes.medium, color = scheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Text(code.userCode, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.headlineSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { copyCode(code.userCode) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_copy))
                    }
                    OutlinedButton(onClick = { open(code.url) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_open_github))
                    }
                }
                Text(code.url, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                TextButton(onClick = container.github::cancelSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_chatgpt_cancel_signin))
                }
            } else if (!connected) {
                Button(
                    onClick = { localError = null; container.github.startSignIn() },
                    enabled = !state.busy && container.github.clientId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_sign_in_with_github))
                }
                if (container.github.clientId.isBlank()) {
                    Text(stringResource(R.string.settings_github_signin_unavailable),
                        style = MaterialTheme.typography.bodySmall, color = scheme.error)
                }
                Text(stringResource(R.string.settings_github_scope_note),
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { localError = null; container.github.startSignIn() },
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.ui_reconnect)) }
                    TextButton(onClick = { confirmDisconnect = true }, enabled = !state.busy,
                        modifier = Modifier.weight(1f)) { Text(stringResource(R.string.settings_github_disconnect)) }
                }
            }

            if (state.busy) com.hmx.harness.ui.common.AppLinearProgressIndicator(Modifier.fillMaxWidth())
            (localError ?: state.message)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (localError != null) scheme.error else scheme.onSurfaceVariant)
            }
        }
    }

    if (confirmDisconnect) HarnessGlassAlertDialog(
        onDismissRequest = { confirmDisconnect = false },
        title = { Text(stringResource(R.string.settings_github_disconnect_title)) },
        text = { Text(stringResource(R.string.settings_github_disconnect_message)) },
        confirmButton = {
            TextButton(onClick = {
                confirmDisconnect = false
                container.github.cancelSignIn()
                scope.launch { container.github.disconnect() }
            }) { Text(stringResource(R.string.settings_action_disconnect)) }
        },
        dismissButton = {
            TextButton(onClick = { confirmDisconnect = false }) {
                Text(stringResource(R.string.settings_action_cancel))
            }
        },
    )
}
