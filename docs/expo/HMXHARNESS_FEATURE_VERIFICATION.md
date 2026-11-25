# HMXHARNESS — Feature Verification Matrix

> **Scope of this matrix.** Every claim is tied to source in
> `app/src/main/java/com/hmx/harness/` (paths below are relative to that
> directory), to a test under `app/src/test/` or `app/src/androidTest/`, or to
> CI run `45338701934` on commit `ae41ac7`.
>
> **Status vocabulary**
>
> - **CI-verified** — exercised by the green CI run (unit tests, lintDebug,
>   assembleDebug + assembleRelease) or directly asserted by a passing unit
>   test in that run.
> - **Implemented + tests** — real source implementation **and** automated test
>   files exist. Tests may be unit tests, instrumented tests, or both.
> - **GAP** — code exists, but an expected runtime behaviour is absent or a test
>   is non-gating; stated explicitly rather than counted as verified.
>
> **Cross-cutting limits:** instrumented tests exist but **are not run by CI**,
> and **no physical-device verification was performed**. Therefore no
> device-dependent feature is "hardware-verified", regardless of test coverage.
> The **23 features investigated in the source audit all have real source
> implementations and automated test files**; none is merely planned.

## Verification layers at a glance

| Layer | What exists | What is actually verified |
|---|---|---|
| CI unit tests + build + lint | 2,478 `@Test` methods in `app/src/test`; `lintDebug`; both APKs | **Yes** — run `45338701934` succeeded; artifact `hmxharness-apks` id `14898111209` (110,461,001 bytes) |
| Instrumented tests | 555 `@Test` methods in `app/src/androidTest` | Files exist and compile; **not executed by CI** |
| Hardware / device behaviour | Shell tiers, GGUF, voice, browser, Telegram, Shizuku | **Not verified** — no physical device was used |
| HMX runtime verification gate | `hmx/HmxTaskVerification.kt` objects | **GAP** — `canClaimCompletion()` and `HmxApprovalPolicy` are test-only; verdict display only |
| Visual correctness | Emulator screenshots + `LiquidGlassScreenshotTest.kt` | **GAP** — no pixel assertions; failures logged via `runCatching { }.onFailure { Log.w }` |

## Feature matrix

| # | Feature | Source evidence | Automated test evidence | Status | Notes |
|---:|---|---|---|---|---|
| 1 | Code agent loop | `agent/AgentEngine.kt` (`run` :296, `while (true)` :463, `executeCalls` :1112); `agent/RunManager.kt` | unit tests under `app/src/test/.../agent/` | **CI-verified** (loop code + tests); device UI not hardware-verified | Budgets, retries, compaction, events |
| 2 | Chat mode | `core/ChatExperience.kt`, `data/ConversationConfig.kt` (`isChat`, `allowedTools`) | `ChatContextPolicyTest`, chat experience tests | **CI-verified** (fixed allowlist + tests) | No project files, terminal or arbitrary filesystem |
| 3 | Plan mode | `AgentMode.PLAN`, `readOnlyOnly = mode == AgentMode.PLAN` (`agent/AgentEngine.kt:978`) | agent mode/permission unit tests | **Implemented + tests** | Read-only tool schema subset |
| 4 | Subagents | `AgentEngine.runSubagent`, `agent/SubagentRuns.kt`, `SubagentConfiguration.kt` | `SubagentConfigurationTest` and related unit tests | **Implemented + tests** | `task` spawns read-only or action agents |
| 5 | Memory | `data/MemoryRepository.kt`, `agent/MemoryNotes.kt`, `MemoryTopics.kt` | `MemoryNotesTest`, memory repository tests | **Implemented + tests** | Global/project/chat scopes, pinning, search |
| 6 | Context compaction | `agent/AutoCompaction.kt`, `HistoryCompactor.kt`, `ChatContextPolicy.kt` | `AutoCompactionTest`, `HistoryCompactorTest` | **CI-verified** | Budget-driven history reduction |
| 7 | Verification evidence + card | `agent/VerificationEvidence.kt` (`CheckStatus`, `collect`), `ui/chat/components/VerificationCard.kt` | verification evidence unit tests; card tests | **CI-verified** for `STALE` logic; UI not hardware-verified | Records shell build/test/lint results; flips passes to `STALE` after edits |
| 8 | HMX verdict / approval objects | `hmx/HmxTaskVerification.kt` (`assess`, `canClaimCompletion()` :47, `HmxApprovalPolicy` :56); card uses `assess` at `ui/chat/components/VerificationCard.kt:48` | `app/src/test/java/com/hmx/harness/hmx/HmxTaskVerificationTest.kt` | **GAP** | `canClaimCompletion()` and `HmxApprovalPolicy` have **no runtime caller**; they do not gate the agent or classify commands. Real approval: `PermissionMode`, `RunManager.approve/deny`, notification actions, `tools/ShellPolicy.kt` |
| 9 | Steering & queue | `agent/Steering.kt`, `QuestionAutoContinue.kt` | `SteeringTest` | **Implemented + tests** | Messages injected between rounds |
| 10 | Question cards | `agent/AskUserOptions.kt` | `AskUserOptionsTest` | **Implemented + tests** | Staged multiple-choice / written answers |
| 11 | File tools + undo | `tools/FileTools.kt`, `PatchTools.kt`, `FuzzyEdit.kt`, `FileHistoryTools.kt` | tool unit tests under `app/src/test/.../tools/` | **CI-verified** (tool logic + tests) | Atomic writes and before/after history |
| 12 | Shell + access tiers | `tools/ShellTool.kt`, `BgShell.kt`, `ShellPolicy.kt`, `data/env/ShellTierRouter.kt` | `ShellPolicyTest`, shell tool tests | **Implemented + tests**; **not hardware-verified** | `TOYBOX`, `APP_LINUX`, `PRIVILEGED` (Shizuku), `ROOT`, `TERMUX_SSH`; real execution needs a device |
| 13 | Linux toolchain | `data/env/LinuxEnvironment.kt`, `PackageMirrors.kt` | environment/bootstrap unit tests | **Implemented + tests**; **not hardware-verified** | Termux packages with SHA-256 verification and mirrors |
| 14 | Git tools | `tools/GitTools.kt` (registered in `tools/Tool.kt`) | Git tool unit tests | **Implemented + tests**; **not hardware-verified** | status, diff, commit, push, pull |
| 15 | GitHub Device Flow + repos/PRs | `github/GitHubAuth.kt`, `GitHubRepository.kt`, `GitHubPullRequests.kt`, `ui/settings/ConnectedAccountsSection.kt` | `app/src/test/java/com/hmx/harness/github/GitHubAuthTest.kt`, `GitHubRepositoryTest`, `GitHubPullRequestsTest` | **Implemented + tests** | Flow logic unit-tested; **live approval not performed** (needs a human). Flow is upstream; HMX added the card and CI wiring. `HMX_GITHUB_CLIENT_ID` is not set, so Device Flow is disabled in the current artifact; PAT works |
| 16 | Browser automation | `browser/BrowserController.kt`, `browser/WorkspacePathHandler.kt`, `tools/BrowserTools.kt` | instrumented tests under `app/src/androidTest/.../browser/` | **Implemented + tests**; instrumented tests **not run by CI**; **not hardware-verified** | Real WebView navigate/click/type/eval/screenshot |
| 17 | Web research | `tools/SearchProviders.kt`, `SearchBackends.kt`, `WebSearchText.kt`, `TavilyPageReader.kt` | search backend unit tests | **Implemented + tests** | Keyed providers plus keyless engines; network behaviour not hardware-verified |
| 18 | MCP | `tools/mcp/McpManager.kt`, `McpConnection.kt`, `McpTransport.kt`, `McpOAuth.kt`, `McpToolAdapter.kt` | MCP unit tests | **Implemented + tests**; **not hardware-verified** | OAuth 2.1 PKCE; static headers stored in plaintext config (`SECURITY.md`) |
| 19 | Plugins | `plugins/PluginManager.kt`, `PluginCatalog.kt`, `PluginStore.kt`, `config/PluginHooks.kt` | plugin manager unit tests | **Implemented + tests** | Catalog, install, hooks |
| 20 | Automations / reminders | `automation/AutomationWorker.kt`, `AutomationManager.kt`, `reminders/`, `tools/ReminderTools.kt` | automation unit tests | **Implemented + tests**; **not hardware-verified** | WorkManager scheduling, notifications/alarms |
| 21 | Voice in / out | `data/audio/` clients (Edge TTS, Groq, OpenAI, ElevenLabs, Deepgram, Cartesia, AssemblyAI) | audio client unit tests | **Implemented + tests**; **not hardware-verified** | Needs provider keys and device audio |
| 22 | Local GGUF inference | `local/LocalNative.kt`, `LocalModelProvider.kt`, `app/src/main/cpp/local_model.cpp`, `CMakeLists.txt` | local model / policy unit tests | **Implemented + tests**; **not hardware-verified** | **CPU** is the shipped path. GPU requires `localModelExperimental = true` (default `false`, `data/SettingsRepository.kt:234`) and `local/LocalAccelerationPolicy.kt`; 32-bit ABIs get stubs |
| 23 | Liquid Glass UI | `ui/liquidglass/GlassQuality.kt` (`GlassTier`, `detectGlassTier`, `profileFor`), `GlassTokens.kt`, `LiquidGlassModifiers.kt` | `app/src/test/java/com/hmx/harness/ui/theme/HarnessGlassModeTest.kt` (tier resolution); `app/src/androidTest/java/com/hmx/harness/ui/liquidglass/LiquidGlassScreenshotTest.kt` (screenshots) | **CI-verified** for tier resolution; **GAP** for visual correctness | Screenshot test has no pixel assertions and suppresses failures; visual-QA workflow captures images but is not gating |

### Other implemented areas (source referenced, with automated tests)

`router/NineRouterManager.kt` (9Router), `telegram/TelegramGateway.kt` and
`TelegramBotClient.kt` (remote control), `agent/LiveMode.kt` (voice mode),
`ui/files/` with sora-editor (code editor), `workspace/` and
`ui/projects/` (workspaces), `app/src/main/res/values-*` (21 locales, 1,129
strings), `data/ChatBackup.kt` and `data/BackupEncryption.kt` (backups),
`tools/AppSettingsTools.kt` (agent-editable settings), `core/response/RichResponse.kt`
(rich replies), and `.github/workflows/android-ci.yml` (cloud build). These are
implemented with test files; none was hardware-verified in this environment.

## What CI actually proves

CI run `45338701934` demonstrated:

- Gradle wrapper validation and JDK 21 setup succeed.
- The pinned NDK 27.2.12479018 installs and the build works with native code.
- All 2,478 unit tests pass; `lintDebug` passes with `app/lint-baseline.xml`.
- `assembleDebug` and `assembleRelease` produce non-empty APKs; release is
  **debug-signed** (installable alpha, not Play-ready).
- The `hmxharness-apks` artifact (id `14898111209`, 110,461,001 bytes) was
  uploaded and is not expired.
- The missing `HMX_GITHUB_CLIENT_ID` variable produces a warning, not a failure;
  no client secret was introduced.

CI does **not** prove: instrumented test results, on-device shell tier
behaviour, local GGUF inference (CPU or experimental GPU), voice, Telegram,
live GitHub approval, or visual pixel correctness.

## Honest gaps to state at the Expo

1. The HMX completion/approval objects exist and are unit-tested but are not
   runtime-wired; do not present them as an enforced gate.
2. Glass tier resolution is unit-tested; screenshots are captured, but there is
   no gating pixel comparison and the screenshot test suppresses failures.
3. GPU inference is opt-in and experimental; CPU GGUF is the shipped path.
4. Instrumented tests are not run by CI, and no physical device was used — say
   "implemented with automated test coverage, not hardware-verified".
