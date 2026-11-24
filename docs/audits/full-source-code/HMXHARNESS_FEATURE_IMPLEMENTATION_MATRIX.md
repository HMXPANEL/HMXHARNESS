# HMXHARNESS — Feature Implementation Matrix

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** independent source inspection. No device/emulator and no Gradle execution.

## Status vocabulary

- **RUNTIME-WIRED** — real call path in `app/src/main`, with automated tests.
- **TEST-ONLY** — implemented and unit-tested, but no runtime caller.
- **UI-ONLY** — renders data/state but does not drive execution.
- **CONDITIONAL** — enabled only by a setting, hardware capability, source set or
  human consent.
- **NOT VERIFIED ON HARDWARE** — source and tests exist, but no physical-device run was
  performed in this audit.
- **USER-REPORTED** — from the project brief; not reproduced here.

## Master matrix

| # | Feature | Status | Primary evidence | Verification |
| ---: | --- | --- | --- | --- |
| 1 | AI coding agent | RUNTIME-WIRED | `agent/AgentEngine.kt:296,463,1112,1540`; `agent/RunManager.kt:429-860` | 326 unit tests in `agent`; device tests exist but not run by CI |
| 2 | Chat mode | RUNTIME-WIRED | `core/ChatExperience.kt:9-17`; `data/ConversationConfig.kt:43-45`; `core/ChatExperienceTest.kt` | 25-tool allowlist enforced `AgentEngine.kt:911,2519` |
| 3 | Plan mode | RUNTIME-WIRED | `AgentMode.PLAN` `AgentEngine.kt:50-51`; schema filter `:977-980`; runtime rejection `:1585-1591` | device tests `SetupAndPlanDeviceTest.kt`, `HarnessEnvironmentDeviceTest.kt` (not in CI) |
| 4 | Task planning / todos | RUNTIME-WIRED | `agent/TodoStore.kt:33-40`, `todo_write` `MoreTools.kt:636` | `TodoStoreTest`, `CodeActivityGroupsTest` |
| 5 | Context construction | RUNTIME-WIRED | `AgentEngine.systemPrompt():2604-2737`, `runtimeEnvironment():2759-2826`, `MemoryRepository.promptBlock():146-157`, repo map `:2728-2735` | context unit tests; device prompt tests |
| 6 | Model selection/routing | RUNTIME-WIRED | `ProviderFactory.create()` `Llm.kt:224-238`, `HarnessApp.routedProvider():379-406` | `FallbackProviderTest` (202), `ModelCatalogTest` (128) |
| 7 | Streaming responses | RUNTIME-WIRED | `Llm.kt:245-305`, `readSseLine():318-329` | `SseLineReaderTest`, `SseStreamTest`, `SseKeepAliveTest` |
| 8 | Tool calling / registry | RUNTIME-WIRED | `tools/Tool.kt:46-78,81-217`; 82 tools at startup | `ToolRegistryAvailabilityTest`, `ToolReliabilityTest` |
| 9 | Multi-step loop | RUNTIME-WIRED | `while(true)` `AgentEngine.kt:463-882`; parallel `executeCalls():1112-1197` | agent reliability tests |
| 10 | Cancellation | RUNTIME-WIRED | `RunManager.stop():1250-1257`, `NotificationActionReceiver:15-46` | `TaskControlTest`, `KeepaliveTrackerTest` |
| 11 | Error recovery | RUNTIME-WIRED | `StreamRetrier.kt:213-273`, `RetryPolicy.kt:11-50`, `FallbackProvider.kt:47-185`, `ContextOverflow` | dedicated unit tests |
| 12 | Conversation persistence | RUNTIME-WIRED | `SessionRepository.kt:263-311`, `HarnessDao` paging `Database.kt:446-480` | `ChatHistoryPagingDeviceTest.kt:25-75` |
| 13 | Memory | RUNTIME-WIRED | `data/MemoryRepository.kt:10-25,146-205,242-276`; `MemoryPolicy.kt` | `MemoryReliabilityTest`, `MemoryPromptTest`, device tests |
| 14 | Subagents | RUNTIME-WIRED | `agent/SubagentRuns.kt:144-239`, `runSubagent():2159-2390` | `SubagentRunsTest`, device tests |
| 15 | Verification evidence | PARTIAL | `VerificationEvidence.collect():19-46` wired for UI/PR body; **`HmxTaskVerification.canClaimCompletion():47` TEST-ONLY**, `assess()` UI-ONLY | `VerificationEvidenceTest`, `HmxTaskVerificationTest` (7 tests on unwired code) |
| 16 | Approval/permissions | RUNTIME-WIRED | `PermissionMode` `AgentEngine.kt:42-48`, decision tree `:1755-1830`, `ShellPolicy.kt`, `WorkspaceFs.kt:95-101` | `ShellPolicyTest`, `RememberedGrantsTest`; **biometric only gates enabling FULL_ACCESS** (`BiometricAuth.kt:87-103`) |
| 17 | Project workspaces | RUNTIME-WIRED | `WorkspaceManager.kt:36-46,238-253`, `WorkspaceFs.kt` | workspace unit/device tests |
| 18 | File operations | RUNTIME-WIRED | `FileTools.kt`, `PatchTools.kt`, `FuzzyEdit.kt`, `MoreTools.kt` | file tool tests |
| 19 | Code editor | RUNTIME-WIRED | `ui/files/CodeEditorScreen.kt`, sora-editor; 8 syntax modes `CodeTokenizer.kt:32-41` | editor unit/device tests |
| 20 | Semantic editor features | CONDITIONAL | `SmartEditorSupport.kt:9-21` — completion/navigation/rename for web/CSS/JSON only; `SmartEditorEditsTest.kt:29-38` guards non-advertisement | JVM test asserts unsupported actions hidden |
| 21 | Terminal + tiered shell | RUNTIME-WIRED / HW-UNVERIFIED | `ShellTierRouter.kt:25-37,68-82`, `LinuxEnvironment.kt`, PTY `terminal/Pty.kt:58-78` | shell policy unit tests; device tests not in CI |
| 22 | Linux toolchain bootstrap | RUNTIME-WIRED / HW-UNVERIFIED | `LinuxEnvironment.kt:294-298,405-432,571-682`, SHA-256 gated | `LinuxEnvironment` unit tests |
| 23 | Git operations | RUNTIME-WIRED | `tools/GitTools.kt` (10 tools), PR tool | Git tool tests; no force-push tool |
| 24 | GitHub Device Flow | CONDITIONAL | `github/GitHubAuth.kt:135-160,191`, PAT fallback; client ID injected at build time | `GitHubAuthTest` (logic); live approval needs a human |
| 25 | Repository import/clone | RUNTIME-WIRED | `GitHubWorkspaces.kt:109-139` (shell `git clone`) | device tests |
| 26 | Browser automation | RUNTIME-WIRED / HW-UNVERIFIED | `browser/BrowserController.kt:167-175`, 14 browser tools `BrowserTools.kt:68-649` | browser unit + device tests (device not in CI) |
| 27 | Web research | RUNTIME-WIRED | `NetTools.kt:27`, `MoreTools.kt:242`, search providers | search/fetch tests with mocks |
| 28 | Website preview | RUNTIME-WIRED | `WorkspacePathHandler.kt:8-19,32-95` static asset serving at `https://harness.workspace` | preview tests; **USER-REPORTED** attendance site |
| 29 | On-device APK build | CONDITIONAL | `skills/android-build/SKILL.md:20-46`, `build-apk.sh`, Java-only route A; Gradle route B | parsing/launcher unit tests; **no CI job builds an app on-device** |
| 30 | MCP | CONDITIONAL | `tools/mcp/McpManager.kt:319-377`, OAuth PKCE `McpOAuth.kt:63-79` | MCP unit/device tests; live consent needs a human |
| 31 | Plugins | RUNTIME-WIRED | `plugins/PluginManager.kt`, `config/PluginHooks.kt`, `app_plugins` | plugin tests |
| 32 | CodeGraph | CONDITIONAL | `CodeGraphTools.kt:12-112`, vendored CLI `CodeGraphManager.kt` | codegraph unit tests; tools conditionally available |
| 33 | LSP tool | CONDITIONAL | `LanguageServerTool.kt:21,25` — only when `projectConfig.languageServers` non-empty | no dedicated test |
| 34 | Automation / reminders | RUNTIME-WIRED | `automation/AutomationWorker.kt`, `ReminderTools.kt`, WorkManager | automation unit tests |
| 35 | Voice in/out | RUNTIME-WIRED / HW-UNVERIFIED | `data/audio/` providers | audio client unit tests |
| 36 | Local GGUF CPU | RUNTIME-WIRED / HW-UNVERIFIED | `local/LocalNative.kt:5-10`, `local_model.cpp`, pinned llama.cpp | local unit tests + device tests (not in CI) |
| 37 | Local GGUF GPU | EXPERIMENTAL / CONDITIONAL | `localModelExperimental` default false `SettingsRepository.kt:234`; `LocalAccelerationPolicy.kt:11-12`; 32-bit stubs | device test `LocalGgufGpuDeviceTest.kt` (not in CI) |
| 38 | 9Router | CONDITIONAL | `router/NineRouterManager.kt:132-135,253` npm router on loopback | unit + device tests |
| 39 | Telegram remote | RUNTIME-WIRED / HW-UNVERIFIED | `telegram/TelegramBotClient.kt`, gateway, service | telegram tests (29 unit) |
| 40 | Liquid Glass UI | RUNTIME-WIRED (visual not gated) | `ui/liquidglass/GlassQuality.kt:17,39-80`, PrismalAGSL/Backdrop | tier unit tests; screenshot test has no pixel assertions and suppresses failures |
| 41 | Localization | RUNTIME-WIRED | 21 `values-*` + `locales_config.xml:8-31`; parity test | `LocalizationResourcesTest` (4 locales parity) |
| 42 | Backup/restore | RUNTIME-WIRED | `data/ChatBackup.kt`, `SettingsBackup.kt`, `BackupEncryption.kt` | backup unit/device tests |
| 43 | Background services | RUNTIME-WIRED | `AgentService.kt`, `automation/`, `reminders/`, `telegram/`, manifest `:87-198` | background unit tests |
| 44 | In-app updater | RUNTIME-WIRED (weak trust) | `data/update/UpdateManager.kt:188-216`, `UpdateInstallPolicy.kt:17-26` | policy tests; debug-signed release signer; optional digest |
| 45 | Cloud CI/APK artifacts | RUNTIME-WIRED | four workflows; run `45338701934` success (committed evidence) | unit tests + lint + APK assembly executed in CI |

## User-reported manual evidence (not reproduced)

The developer reports successful physical-device use of AI chat, file creation/editing,
terminal command execution and website preview, including a Student Attendance Management
site with date selector, student counts, roster, attendance marking and add-student form.
These are **USER-REPORTED**; this audit did not run a device and cannot confirm the specific
site or its behaviour. Source confirms the underlying capabilities (rows 1, 18, 21, 28).

## Summary counts (this matrix)

45 classified capabilities: 35 RUNTIME-WIRED, 2 TEST-ONLY/PARTIAL (verification gate),
5 CONDITIONAL/EXPERIMENTAL, 3 UI-ONLY/HARDWARE-UNVERIFIED qualifiers, plus 4 user-reported
workflows. Hardware-dependent rows are explicitly marked; no entry is claimed as verified
on a physical device by this audit.
