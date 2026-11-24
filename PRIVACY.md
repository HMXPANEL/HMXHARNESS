# Privacy Policy — HMXHARNESS

**Product:** HMXHARNESS (`com.hmx.harness`)
**Repository:** https://github.com/HMXPANEL/HMXHARNESS
**Last updated:** 2026-11-24

This policy describes how HMXHARNESS handles information, based on the behaviour
implemented in this repository. It uses the names of real components in
`app/src/main/java/com/hmx/harness/` so that each statement can be checked
against source. Where behaviour could not be independently verified, the text
says so instead of guessing. Security findings and boundaries are documented
separately in [SECURITY.md](SECURITY.md).

> **Important:** HMXHARNESS is a tool that connects to services **you** choose and
> configure. When you use a cloud AI provider, web search, GitHub or an MCP
> server, information is transmitted off your device to that service. The app
> has no HMXHARNESS-operated server. Local GGUF inference is the only path that
> keeps a model request on the device.

---

## 1. Introduction and scope

This policy applies to the HMXHARNESS Android application built from this
repository, including debug and release variants. It covers the app itself, not
the independent websites or services you choose to connect. Those services have
their own privacy policies, and HMXHARNESS does not control their practices.

The application is a developer and power-user tool. It can read and change
files, execute programs, and connect to external services when you enable those
capabilities. Use it with projects, tools, models and services you trust.

## 2. Information collected

HMXHARNESS has no account system of its own, no analytics SDK, no advertising
SDK and no crash-reporting SDK. It does not maintain a server-side profile about
you.

The app handles the following categories of information because they are needed
for its features:

| Category | Examples | Where it comes from |
| --- | --- | --- |
| Conversations | Prompts, replies, tool calls, attachments | You |
| Workspace content | Files you open, edit or include as context | Your selected project folders |
| Provider configuration | Endpoint URLs, model IDs, API keys | You |
| Connection credentials | GitHub OAuth token or personal access token, MCP credentials, SSH keys | You / the service you authorize |
| Settings and preferences | Appearance, language, permissions, approvals | You |
| Diagnostics | Debug logs and tool output | The app and the tools you run |

## 3. Information stored locally

The app stores chats and agent history, workspace references, settings,
checkpoints, memory notes and credentials on the device:

- **Room databases:** `data/db/Database.kt` holds conversations, projects,
  checkpoints, memory and related records; `llm/debug/DebugLogDatabase.kt` holds
  diagnostic log records.
- **DataStore preferences:** `data/SettingsRepository.kt` persists settings.
- **Files:** workspace files live in folders you select through the Storage
  Access Framework; app-private files hold downloads, toolchain data and
  exported backups.
- **Credentials:** provider keys, GitHub tokens, SSH credentials and saved
  sign-in state are stored through AndroidX Security Crypto
  (`data/KeyStoreManager.kt`) using `EncryptedSharedPreferences` with an Android
  Keystore `AES256_GCM` master key. This protects data at rest against casual
  access to app preferences; it is not a guarantee against a compromised
  operating system or code running with the app's own authority.

Local storage is not automatically encrypted beyond the credential store. Chats,
memory, checkpoints and workspace files do not have a blanket application-level
encryption guarantee, and shared-storage or remote workspaces have their own
access rules.

## 4. AI provider requests

When you use the agent, the app sends your prompt and the context needed to
answer it to the AI provider you selected. **Context may include file contents,
diffs, command output, images and other data from your workspace.** What is sent
depends on the conversation, the tools used and your configuration.

Requests go directly from the device to the endpoint you configure. The app
does not proxy model traffic through a server operated by HMXHARNESS. Supported
protocol implementations live under `llm/` (OpenAI-compatible, OpenAI Responses,
Anthropic Messages, Gemini), with account-based ChatGPT support under
`chatgpt/` and fallback/rotation in `llm/FallbackProvider.kt` and
`llm/RotatingKeyProvider.kt`.

Custom endpoints are supported. Prefer HTTPS, check the destination and
understand that whatever you configure will receive the request contents.

## 5. Third-party model providers

HMXHARNESS does not train or host a general-purpose AI model. Cloud inference is
performed by the provider you configure, such as OpenAI, Anthropic, Google
Gemini, an OpenAI-compatible service, or a ChatGPT account. Usage is governed by
that provider's terms and privacy policy and may be billed to your account.

The built-in keyless provider (`llm/HarnessProvider.kt`) routes to third-party
public gateways. These are external free tiers, not HMXHARNESS services. They
can rate-limit, change or disappear, and their operators may process requests
under their own policies. Do not treat the built-in option as guaranteed private
or guaranteed free.

Voice features can send audio or text to configured speech providers (for
example Edge speech, Groq, OpenAI, ElevenLabs, Deepgram, Cartesia and
AssemblyAI). Web research sends queries to the search service you configure, or
to public search engines when a keyless engine is used.

## 6. GitHub authentication

HMXHARNESS supports two GitHub sign-in paths:

- **OAuth App Device Flow** (`github/GitHubAuth.kt`): the app requests a device
  code, displays a user code and verification URL, and polls GitHub's token
  endpoint. You approve the request in a browser on github.com. Only the public
  OAuth **Client ID** is used; there is no client secret and no HMXHARNESS
  authentication server.
- **Personal access token:** you create a token on GitHub and paste it into the
  app. The app verifies it by calling GitHub's API and saves it only after
  verification.

The requested OAuth scopes are `repo` and `offline_access`; `workflow` is added
only when you enable workflow editing. `repo` is used because the same
connection can import private repositories and publish commits or pull requests.
If you do not want to grant repository access, use a narrowly scoped personal
access token instead.

CI builds receive the public Client ID from the `HMX_GITHUB_CLIENT_ID`
repository variable (or a same-named secret for compatibility), write it into
the gitignored `local.properties`, and expose it as `BuildConfig.GITHUB_CLIENT_ID`.
At the time of this policy that variable is not configured, so current cloud
builds disable Device Flow sign-in; personal access token sign-in remains
available.

## 7. Token handling

GitHub tokens, provider keys, search keys, MCP credentials and SSH credentials
are stored through the Keystore-backed encrypted preferences described in
section 3. The app redacts tokens and credentials embedded in remote URLs from
user-visible errors and tool output (`SecretRedactor`, with unit tests in
`app/src/test/java/com/hmx/harness/tools/SecretRedactorTest.kt`).

Documented limits, taken from [SECURITY.md](SECURITY.md):

- Tooling may materialize credentials in app-private files. MCP command
  arguments, environment values and static headers are stored in the MCP
  configuration file; its HMAC is an integrity check, **not** encryption or a
  sandbox.
- GitHub CLI token material has been observed in an app-private `linux/` prefix
  (`.gh-token`).
- Credentials can be exposed if you place them in prompts, project instructions,
  command arguments or logs. Redaction is best-effort, not a guarantee.

## 8. Shell and file access

Shell execution is tiered by `data/env/ShellTierRouter.kt` (for example toybox,
app Linux environment, Shizuku, root and Termux SSH). `tools/ShellPolicy.kt`
applies a denylist and workspace containment checks before execution, and the
agent's permission modes (`CONFIRM_ALL`, `CONFIRM_RISKY`, `FULL_AUTO`,
`FULL_ACCESS`) gate privileged or destructive actions. Full access deliberately
skips the normal shell denylist and broadens access; enable it only when needed.

`workspace/WorkspaceFs.kt` resolves and canonicalizes paths and rejects paths
outside a selected workspace. File writes and history are exposed as diffs and
undo records. These are application-level checks, not a complete operating
system sandbox: programs you launch run as the app's Android UID, and root or
Shizuku can extend OS-level access. See [SECURITY.md](SECURITY.md).

## 9. Browser access

`browser/BrowserController.kt` drives a WebView for the agent browser, with
navigate, click, type, scroll, JavaScript evaluation and screenshot actions.
`browser/WorkspacePathHandler.kt` serves workspace files through
`WebViewAssetLoader` over a stable origin. Workspace previews execute project
content, and interactive artifacts use a separate WebView with network and
file/content access blocked and no native JavaScript bridge.

Browser state, synthetic preview origins and a selected workspace are **not** an
isolated browser profile for mutually untrusted projects or accounts. Website
JavaScript can run, and browsing state can persist.

## 10. MCP integrations

`tools/mcp/McpManager.kt` and related classes connect to Model Context Protocol
servers that you configure. Connecting a server can execute a local command or
contact a remote service. Project definitions require approval bound to the
workspace and configuration, and MCP tool calls enter the normal permission
flow. OAuth uses state and PKCE, but those mechanisms do not establish that a
configured server or endpoint is trustworthy.

Review server commands, environment variables, headers and sign-in hosts before
enabling them. Static headers and environment values may be stored in
plaintext in the MCP configuration file (see section 7).

## 11. Optional local model processing

`local/LocalNative.kt` and the native sources under `app/src/main/cpp/` run GGUF
models on the device through llama.cpp. Local inference does not send the model
request to a cloud provider. It is optional, requires a model download and
compatible hardware (64-bit ABIs for GPU paths), and may still involve network
activity for downloading the model or for other tools you invoke. Local model
files are parsed by native code; import them only from sources you trust.

## 12. Logs and diagnostics

The app keeps debug logs and records of tool calls and their output, including
in `llm/debug/DebugLogDatabase.kt`. Captured request/response bodies are
field-encrypted with an Android Keystore key (`llm/debug/DebugCrypto.kt`) and
debug capture is opt-in and off by default. Logs can still contain command
output and request details. Secret redaction (`tools/SecretRedactor.kt`,
`llm/debug/DebugRedact.kt`) is applied to user-visible errors and tool results,
but you should assume that anything you type into a prompt or command may be
recorded in local diagnostics. Do not place credentials in prompts or command
arguments.

## 13. Crash reporting

The application does **not** include a crash-reporting SDK, and no crash reports
are transmitted by HMXHARNESS. Unhandled failures appear in the app and may be
recorded in local debug logs. This statement describes the dependencies declared
in `app/build.gradle.kts`; it is not a claim that the Android platform itself
never collects diagnostics.

## 14. Analytics and telemetry

There is no analytics or advertising SDK in this repository and no HMXHARNESS
telemetry endpoint. The app does not report your usage to HMXHARNESS. Usage of
configured third-party providers and services is governed by those services.

## 15. Data retention

HMXHARNESS does not operate a server, so it does not set a server-side retention
period for your data. Conversations, settings, checkpoints and credentials
remain on your device until you delete them, until the app's data is cleared, or
until the app is uninstalled.

Retention by third-party providers (for example API request logs) is determined
by their own policies and is not specified or guaranteed by this document.
No specific retention duration is claimed here.

## 16. Data deletion

- **Disconnecting GitHub** removes the saved connection and repository
  credentials from the device after confirmation; your repositories and local
  commits are kept.
- **Removing a provider, key or MCP server** in Settings stops the app using it.
- **Clearing app data or uninstalling HMXHARNESS** removes local app data,
  subject to Android's own data-management behaviour.
- Debug and release builds have different application IDs and separate storage;
  uninstalling one does not remove the other's data.

Exported backup files are separate copies. Deleting data inside the app does not
delete files you exported or content stored by other services.

## 17. Android permissions

The app requests permissions only for features it implements. Some are optional.

| Permission | Purpose |
| --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Model providers, GitHub, web research, updates |
| `POST_NOTIFICATIONS` | Progress and approval notifications |
| `POST_PROMOTED_NOTIFICATIONS` | Android 16 Live Update promotion of agent-run notifications |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep long runs and transfers alive with a visible notification |
| `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` | Resume scheduled work and automations |
| `SCHEDULE_EXACT_ALARM`, `SET_ALARM` | User-requested reminders, alarms and timers |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional; helps long runs survive battery management |
| `moe.shizuku.manager.permission.API_V23` | Optional privileged shell through Shizuku |
| `REQUEST_INSTALL_PACKAGES` | Install app updates you download |
| `ACCESS_LOCAL_NETWORK` | Local model servers, 9Router and LAN transfers |
| `RECORD_AUDIO` | Voice input |
| `USE_BIOMETRIC` | Optional app lock |
| `VIBRATE` | Haptic feedback |
| `MANAGE_EXTERNAL_STORAGE` | Optional All files access for real workspace paths |
| `WRITE_EXTERNAL_STORAGE` | Legacy storage write, limited to `maxSdkVersion="29"` |
| `READ_EXTERNAL_STORAGE` | Legacy storage read, limited to `maxSdkVersion="32"` |

The exact declared set is in `app/src/main/AndroidManifest.xml`. Root, Device
Owner and Wireless ADB access are separate opt-in capabilities, not install-time
permissions.

## 18. Third-party services

Depending on your configuration, the app can communicate with: cloud AI model
providers and the built-in keyless gateways; search providers and public search
engines; GitHub (`github.com`, `api.github.com`); MCP servers; SSH/SFTP hosts;
FTP servers; Telegram; Termux package mirrors for the Linux toolchain; 9Router
nodes; and update/release endpoints on GitHub. Each is an independent service
with its own privacy practices. HMXHARNESS does not control them.

## 19. Security safeguards

- Keystore-backed encrypted credential storage (`data/KeyStoreManager.kt`).
- Workspace path containment and shell policy (`workspace/WorkspaceFs.kt`,
  `tools/ShellPolicy.kt`).
- Permission modes and approval prompts, with optional biometric gating.
- Secret redaction in errors and tool output.
- Password-encrypted settings exports using AES-256-GCM and PBKDF2
  (`data/BackupEncryption.kt`).
- Android backup and device-transfer rules that exclude selected credential and
  configuration locations (`app/src/main/res/xml/backup_rules.xml`,
  `data_extraction_rules.xml`). These rules do not exclude the main chat
  database, so chats and file snapshots may be included in device backups
  depending on your Android and Google backup settings; see sections 15 and the
  "Unresolved and unverified items" section at the end of this policy.

These are best-effort safeguards, not guarantees. Known limitations, including
cleartext traffic being enabled in the manifest, plaintext MCP configuration and
debug-signed release builds, are documented in [SECURITY.md](SECURITY.md).

## 20. User choices and controls

You can:

- choose whether to use cloud providers, the built-in keyless provider or local
  GGUF models;
- add, remove or replace provider keys, endpoints and MCP servers;
- choose OAuth Device Flow or a personal access token, and disconnect GitHub;
- select editable or read-only workspaces;
- choose a permission mode and approve or deny individual actions;
- enable or deny notifications, biometric lock and All files access;
- control device-level backups in Android settings; and
- export, inspect and delete local data.

Denying an optional permission disables the feature that needs it rather than
silently changing what the app does.

## 21. Children's privacy

HMXHARNESS is a developer and power-user tool. It is not directed at children
under 13, and no age-based data collection is implemented. If a child uses the
app, a parent or guardian should supervise provider accounts and permissions.

## 22. Contact and reporting

There is no company postal address or dedicated privacy email for this project;
none is claimed here.

- Questions or privacy concerns: open an issue in the
  [HMXHARNESS issue tracker](https://github.com/HMXPANEL/HMXHARNESS/issues).
- Security vulnerabilities: use **Report a vulnerability** on the repository's
  [security advisories page](https://github.com/HMXPANEL/HMXHARNESS/security/advisories)
  when private reporting is available. If it is not, follow the private-contact
  fallback in [SECURITY.md](SECURITY.md). Do not post credentials, tokens,
  sensitive logs or working exploits in a public issue.

## Unresolved and unverified items

- Live GitHub Device Flow approval requires a human and was not performed as
  part of this documentation review; only the flow's unit tests were verified.
- Physical-device behaviour (shell tiers, local inference, voice and browser
  automation) was not exercised on hardware here.
- The behaviour of third-party providers, gateways and package mirrors is
  outside this repository's control and was not audited.
- `backup_rules.xml` and `data_extraction_rules.xml` exclude credential, MCP
  configuration, toolchain and Telegram locations, but they do **not** exclude
  the main chat database (`harness.db`), which contains messages, memory and
  file snapshots. Whether those records leave the device under Google auto-backup
  depends on device and OS behaviour and was not verified here.
- There is no privacy-policy hyperlink inside the installed app; this document
  is the repository's policy. No obsolete in-app policy URL was found.
- No independent third-party security audit or regulatory compliance assessment
  has been performed, and none is claimed.
