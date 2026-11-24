# HMXHARNESS — Feature Verification Matrix

> Status vocabulary:
> **V** = verified (code + automated tests/build), **I** = implemented (code
> present, not fully tested here), **P** = partially implemented, **E** =
> experimental, **N** = not implemented/found.
>
> "Verified" is only used where the build/tests actually exercise it. The app was
> **not installed on a physical device** during this audit.

| # | Feature | Description | Status | Source evidence | Demo steps |
|---:|---|---|:--:|---|---|
| 1 | Code agent | Multi-round agent that edits files, runs commands and verifies | **V** | `agent/AgentEngine.kt` (`run`, `executeCalls`); unit tests in `app/src/test/.../agent/`; CI passes | New Code chat → "add a test to X" → watch tool cards and diff |
| 2 | Chat mode | Conversation-only assistant: fixed tool allowlist, no project files/terminal/device | **V** (tool set fixed in code; unit-tested context policy) | `data/ConversationConfig.kt` (`isChat`, `allowedTools`), `core/ChatExperience.kt` (`tools`, system prompt: "no project files, terminal ... or arbitrary filesystem access"), `ChatContextPolicyTest` | Drawer → Chat → ask a question with web search |
| 3 | Plan mode | Read-only tool schema subset | **I** | `AgentMode.PLAN`, `readOnlyOnly = mode == PLAN` (`AgentEngine.kt:978`) | Composer mode → Plan → agent proposes without writing |
| 4 | Subagents | `task` tool spawns read-only or action agents | **V** | `AgentEngine.runSubagent` (line 2159), `SubagentRuns.kt`; `SubagentConfigurationTest` | Ask agent to "use a subagent to inspect X" |
| 5 | Universal/project memory | Scoped memory with categories, pinning, search | **I** | `data/MemoryRepository.kt` (scopes/categories), `MemoryTools.kt`, `MemoryNotesTest` | Settings → Memory → add fact → agent recalls it |
| 6 | Context compaction | Auto-compaction under budget | **V** | `agent/AutoCompaction.kt`, `HistoryCompactor.kt`; `AutoCompactionTest`, `HistoryCompactorTest` | Long chat → context ring near 75% → Compact |
| 7 | Verification cards | Build/test/lint outcomes + staleness | **V** | `agent/VerificationEvidence.kt`, `hmx/HmxTaskVerification.kt`, `ui/chat/components/VerificationCard.kt` | Ask for a change → run tests → card shows PASSED/STALE |
| 8 | Steering & queue | Inject/queue follow-ups mid-run | **I** | `agent/Steering.kt`, `QuestionAutoContinue.kt`; `SteeringTest` | While running, hold send → "Steer after this step" |
| 9 | Question cards | Staged multiple-choice/written answers | **I** | `agent/AskUserOptions.kt`; `AskUserOptionsTest` | Ask agent to request a preference |
| 10 | File tools + undo | Read/write/edit/apply_patch, atomic writes, history | **V** | `tools/FileTools.kt`, `PatchTools.kt`, `FuzzyEdit.kt`, `FileHistoryTools.kt`; tool unit tests | Ask to edit a file → open diff → Undo |
| 11 | Shell + access modes | Toybox/APP_LINUX/Shizuku/root/ADB execution | **I** | `tools/ShellTool.kt`, `data/env/ShellTierRouter.kt` (`TERMUX_SSH/ROOT/PRIVILEGED/APP_LINUX/TOYBOX`), `ShellPolicyTest` | Run `uname -a`; switch access mode in Settings |
| 12 | Linux toolchain | Termux bash/git/python/node install, SHA-256 verified, mirrors | **I** | `data/env/LinuxEnvironment.kt`, `PackageMirrors.kt` | Settings → Terminal & device → install toolchain |
| 13 | Git tools | status/diff/commit/log/branch/checkout/push/pull | **I** | `tools/GitTools.kt` (registry entries in `Tool.kt`) | Ask agent to commit and push a change |
| 14 | GitHub Device Flow | OAuth App device flow, no client secret | **I** (unit-tested; live not verified) | `github/GitHubAuth.kt` (`/login/device/code`, `authorization_pending`, `slow_down`, refresh); `GitHubAuthTest.kt` | Connected Accounts → GitHub → copy code → approve on github.com |
| 15 | GitHub repos/PRs | Import repo, commit, open PR | **I** | `github/GitHubRepository.kt`, `GitHubPullRequests.kt`; `GitHubPullRequestsTest`, `GitHubRepositoryTest` | Settings → GitHub → import repo → create PR |
| 16 | Browser automation | WebView navigate/click/type/eval/screenshot | **I** | `browser/BrowserController.kt`, `tools/BrowserTools.kt`; androidTest `browser/` | Ask agent to open a local preview and click a button |
| 17 | Web research | 9 keyed providers + keyless engines, passages, citations | **I** | `tools/SearchProviders.kt` (9 keyed + keyless), `SearchBackends.kt`, `WebSearchText.kt` | Chat → ask a current question → Sources list |
| 18 | MCP | Client, OAuth 2.1 PKCE, dynamic tools | **I** | `tools/mcp/McpManager.kt`, `McpOAuth.kt`, `McpToolAdapter.kt` | Settings → MCP → add server → tool appears |
| 19 | Plugins | Catalog, install, hooks, marketplaces | **I** | `plugins/PluginManager.kt`, `PluginCatalog.kt`, `config/PluginHooks.kt` | Settings → Plugins → install a plugin |
| 20 | Automations | Scheduled prompts via WorkManager | **I** | `automation/AutomationWorker.kt`, `AutomationManager.kt`; `AutomationAiPlanner.kt` | Composer menu → Schedule → hourly |
| 21 | Reminders/alarms | Notification reminders, Clock alarms, timers | **I** | `reminders/`, `tools/ReminderTools.kt`, `Manifest` receivers | Ask "remind me in 1 minute" |
| 22 | Voice in/out | 7 cloud voice providers + native speech | **I** | `data/audio/` (`EdgeTtsClient`, `GroqWhisperClient`, `OpenAiAudioClient`, `ElevenLabsClient`, `DeepgramVoiceClient`, `ProviderVoiceClients` for Cartesia/AssemblyAI) | Mic button → speak; speaker on a reply |
| 23 | Local GGUF models | llama.cpp JNI, CPU + Vulkan on 64-bit | **I** (not verified on this device) | `local/LocalNative.kt`, `local/LocalModelProvider.kt`, `app/src/main/cpp/local_model.cpp` | Settings → Local models → download small GGUF → chat |
| 24 | 9Router | Local routing node integration | **I** | `router/NineRouterManager.kt`, `NineRouterNodeRuntime.kt`, `NineRouterControl.kt` | Settings → 9Router → start node |
| 25 | Telegram remote | Bot bridge for remote chat/approvals | **I** | `telegram/TelegramBotClient.kt`, `TelegramGateway.kt`, `TelegramService.kt` | Settings → Telegram → pair bot |
| 26 | Live Mode | Voice conversation with tool access levels | **I** | `agent/LiveMode.kt`; `LiveModeTest` | Composer → Live Mode |
| 27 | Liquid Glass UI | Custom tokens + Prismal rendering + 3 tiers | **I** (screenshots documented) | `ui/liquidglass/*`, `ui/theme/`; `docs/HMXHARNESS_VISUAL_VERIFICATION.md` | Toggle Dark/Light; show drawer over content |
| 28 | Code editor | sora-editor with tabs/search | **I** | `ui/files/`, `libs.versions.toml` (`sora-editor`) | Files → open file → edit → save |
| 29 | Projects/workspaces | Multi-folder projects, read-only grants | **I** | `workspace/`, `ui/projects/`, `agent/GrantPersistence.kt` | Create project → add two folders |
| 30 | 21 locales | Translated UI with fallback | **V** (format/localization tests) | `app/src/main/res/values-*` (21 dirs); localization unit tests | Settings → Appearance → Language |
| 31 | Backups | JSON chat export/import; encrypted settings | **I** | `data/ChatBackup.kt` (`ChatBackupManager`), `data/BackupEncryption.kt` | Settings → Backup → export |
| 32 | Agent edits settings | Natural-language settings changes | **I** | `tools/AppSettingsTools.kt` | "Turn on dark mode" |
| 33 | Rich replies | Charts, tables, Mermaid, interactive HTML | **I** | `core/response/RichResponse.kt`, `ui/chat/` | Ask for a chart or diagram |
| 34 | Cloud build + APK | Tests, lint, APK artifacts | **V** | `.github/workflows/android-ci.yml`; runs `45204023911`, `45219214615`; `aapt` verified | Show the Actions run and download artifact |

## Status summary

| Status | Count |
|---|---:|
| V — verified by tests/build | 8 |
| I — implemented, not fully tested here | 26 |
| P/E/N | 0 |

> "Implemented" does **not** mean "verified on a phone". It means the code path
> exists and compiles. Device-level behaviour (shell access, local inference,
> voice, Telegram, Shizuku, browser automation) requires real hardware,
> permissions and accounts and was not exercised in this audit.

## Features documented but not independently confirmed

These appear in `FEATURES.md`/`README.md`; the code exists, but this audit did
not run them end-to-end:

- Telegram remote approvals bound to a fingerprint.
- 9Router node lifecycle and terminal UI parity.
- GPU (Vulkan) acceleration on real hardware.
- Cloud-emulator visual QA for the `github` screen on `main`.
- Metered-network-aware automation scheduling.

## Documented sources that are out of date

- `docs/HMXHARNESS_PERFORMANCE_REPORT.md` describes an earlier glass design
  (`LiquidGlassQuality.LITE`, `< 320 MB` threshold, "no backdrop capture"). Current
  code uses `GlassTier.LOW/MID/HIGH` (`ui/liquidglass/GlassQuality.kt`) with real
  PrismalAGSL backdrop blur/refraction (`LiquidGlassModifiers.kt`). Cite the
  performance report only for its "unmeasured" conclusion, not for design details.
- `README.md`'s "Attribution and upstream" paragraph calls GitHub Device Flow an
  HMX addition; git shows it was introduced upstream (`2207914579`). The reports
  in `docs/expo/` use the git record, not that sentence.
