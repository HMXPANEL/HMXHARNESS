# HMXHARNESS — Second Full Repository Audit

**Date:** 2026-11-24
**Branch:** `docs/hmxharness-complete-documentation`
**Base commit:** `ae41ac7399d2bdfa6248cbf4c00d362451675990` on `main`
**Repository:** https://github.com/HMXPANEL/HMXHARNESS.git

This is an independent audit of the **current** repository. Existing audit
reports were read as ordinary files only; no claim in them was accepted as
evidence. Evidence came from direct `find`, `git ls-files`, `git grep`, file reads
and the current GitHub Actions API.

## A. Repository structure (measured)

| Item | Measured value |
| --- | --- |
| Files on disk (excluding `.git`) | 2,526 |
| Git-tracked files | 2,513 |
| Present but untracked (ignored `docs/*` reports) | 13 |
| Markdown files on disk / tracked | 143 / 130 |
| Kotlin files total | 1,231 (732 `main`, 352 unit tests, 140 instrumented, 4 debug, 2 githubDelivery, 1 playDelivery) |
| Java files | 5 |
| C/C++/CMake under `app/src/main/cpp/` | 12 |
| JS/MJS, Python, shell scripts | 7, 3, 5 |
| XML files | 1,009 |
| Android modules | `:app` always; `:local_gpu` only with `-Pdistribution=play` |
| Room databases | `data/db/AppDatabase` (v27, 14 `@Entity`, 23 migrations) and `llm/debug/DebugLogDatabase` |
| Locale resource directories | 21 `values-*` plus base `values` (1,129 strings) |
| GitHub Actions workflows | 4 |
| Issue/PR templates and metadata | 27 files under `.github/` (including 18 images) |
| Tests | 2,478 `@Test` methods in `app/src/test`; 555 in `app/src/androidTest` |
| Legal notices | Root `LICENSE`, `PRIVACY.md`, `SECURITY.md`; 8 third-party notices under `app/src/main/assets/licenses/`; KaTeX/Mermaid licenses; editor-tools `LICENSES.txt` |
| Absent | `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `NOTICE`, `CODEOWNERS`, `.env` |

## B. Product functionality classification

Labels: **IV** = implemented and verified (real source implementation plus
automated test files); **IT** = implemented but not fully tested; **PI** =
partially implemented; **EX** = experimental; **PL** = planned; **NI** = not
implemented.

"Verified" here means a concrete implementation was found **and** automated
tests exist for it. It does not mean the feature ran on a physical device: no
hardware was available. Instrumented tests exist for many features but are not
executed by CI, which runs unit tests, lint and APK assembly only.

| # | Feature | Class | Evidence |
| ---: | --- | --- | --- |
| 1 | AI coding agent | IV | `agent/AgentEngine.kt`, `RunManager.kt`, `Steering.kt`, `TodoStore.kt`; unit tests under `app/src/test/.../agent/` |
| 2 | Chat mode | IV | `core/ChatExperience.kt`, `data/ConversationConfig.kt`, `tools/ChatTools.kt`; `ChatExperienceTest.kt`, chat device tests |
| 3 | Plan mode | IV | `AgentMode.PLAN` + read-only schema filter in `AgentEngine.kt`; `SetupAndPlanDeviceTest.kt`, `HarnessEnvironmentDeviceTest.kt` |
| 4 | File operations | IV | `tools/FileTools.kt`, `PatchTools.kt`, `FuzzyEdit.kt`, `FileHistoryTools.kt`; file-tool unit tests |
| 5 | Shell execution | IV | `tools/ShellTool.kt`, `data/env/ShellTierRouter.kt`, `tools/ShellPolicy.kt`; `ShellPolicyTest.kt`, shell device tests |
| 6 | Linux environment | IV | `data/env/LinuxEnvironment.kt`, `PackageMirrors.kt`, ADB pairing sources; toolchain unit/device tests |
| 7 | Browser automation | IV | `browser/BrowserController.kt`, `tools/BrowserTools.kt`; browser unit and device tests (device tests not in CI) |
| 8 | GitHub integration | IV | `github/GitHubAuth.kt`, `GitHubRepository.kt`, `GitHubPullRequests.kt`; `GitHubAuthTest.kt` and related tests; live approval not performed |
| 9 | AI provider integrations | IV | `llm/Llm.kt` and provider classes, fallback/rotation; extensive unit tests |
| 10 | Local GGUF inference | IV* | `local/LocalNative.kt`, `app/src/main/cpp/local_model.cpp`, pinned llama.cpp; CPU inference tested. *GPU is opt-in and default-off (`localModelExperimental`), 32-bit ABIs get stubs; no hardware verification. |
| 11 | MCP integrations | IV | `tools/mcp/McpManager.kt`, `McpOAuth.kt`, `McpToolAdapter.kt`; MCP unit/device tests |
| 12 | Subagents | IV | `agent/SubagentRuns.kt`, `SubagentModelResolver.kt`; subagent unit/device tests |
| 13 | Memory | IV | `data/MemoryRepository.kt`, `agent/MemoryNotes.kt`; memory unit/device tests |
| 14 | Tool execution | IV | `tools/Tool.kt` registry, `agent/AgentEngine.kt`; `ToolRegistryAvailabilityTest.kt`, reliability tests |
| 15 | Permissions and approval | IV* | `AgentEngine` PermissionMode + approval flow, `tools/ShellPolicy.kt`, `workspace/WorkspaceFs.kt`. *`hmx/HmxApprovalPolicy` exists and is unit-tested but is not wired into the runtime approval path. |
| 16 | Verification evidence | IV* | `agent/VerificationEvidence.kt` records shell results and STALE state; verdict rendered by `VerificationCard.kt`. *`HmxTaskVerification.canClaimCompletion()` is test-only, not a runtime completion gate. |
| 17 | Project workspaces | IV | `workspace/WorkspaceManager.kt`, `WorkspaceFs.kt`, `SshFs.kt`; workspace unit/device tests |
| 18 | Code editor | IV* | `ui/files/CodeEditorScreen.kt` on sora-editor; editor unit/device tests. *Language-server actions are intentionally partial for some file types. |
| 19 | Website preview | IV | `browser/WorkspacePathHandler.kt` (`WebViewAssetLoader`), `ui/chat/components/WebPreviewSheet.kt`; preview tests |
| 20 | Liquid Glass UI | IV* | `ui/liquidglass/*` on PrismalAGSL/Backdrop; tier unit tests and emulator screenshots. *Screenshot test has no pixel assertions and swallows failures; visual correctness is not gating. |
| 21 | Localization | IV | 21 `values-*` dirs, 1,129 base strings; parity/format unit tests (note: only a subset of locales is parity-asserted) |
| 22 | Backup and restore | IV | `data/SettingsBackup.kt`, `ChatBackup.kt`, `BackupEncryption.kt`; backup unit/device tests |
| 23 | Background services | IV | `AgentService.kt`, `automation/`, `reminders/`, `telegram/`; background unit/device tests |

Summary: all 23 features have a real implementation and automated test coverage
(23 IV, of which 7 carry a documented caveat marked `*`); 0 PI/EX/PL/NI. This is
**not** a claim of hardware verification: instrumented tests are not run by CI,
and no physical-device execution was performed. Two earlier marketing claims are
explicitly corrected in section E: the HMX approval/verification gate is not
runtime-wired, and the glass screenshot test is non-gating.

## C. Technical stack

- **Languages:** Kotlin (1,231 files), Java (5), C/C++ (11 sources plus
  CMakeLists), JS/MJS (7), Python (3), shell (5), XML resources (1,009).
- **Build:** Gradle wrapper 9.7.0, AGP 9.3.1, Kotlin 2.4.10, KSP 2.3.11,
  JDK 21 in CI, NDK 27.2.12479018, CMake 3.22.1, ABIs armeabi-v7a/arm64-v8a/
  x86/x86_64. `app/build.gradle.kts` omits the native block when the pinned NDK
  is absent; CI installs it, so cloud artifacts include native code.
- **UI:** Jetpack Compose, Compose BOM 2026.08.00, Material 3 Expressive
  1.5.0-alpha26, Navigation Compose, sora-editor, PrismalAGSL and kyant0
  Backdrop for the custom Liquid Glass token/tier system.
- **Data:** Room 2.8.4, DataStore Preferences 1.2.1, WorkManager 2.10.5,
  AndroidX Security Crypto 1.1.0, Biometric.
- **Networking:** OkHttp 4.12.0 + SSE, kotlinx.serialization, Conscrypt,
  BouncyCastle, JSch.
- **Native:** llama.cpp fetched by `FetchContent` at pinned revision
  `19cce6919992fdffd92e0ac152caaf1e2b8f254e`; ggml dynamic backends, Vulkan on
  64-bit ABIs, BoringSSL prefab for ADB pairing, PTY and device telemetry.
- **AI providers:** OpenAI-compatible, OpenAI Responses, Anthropic Messages,
  Gemini, ChatGPT account, built-in keyless third-party gateways, local GGUF,
  fallback/key rotation, 9Router.
- **External services:** GitHub, configurable search providers, MCP servers,
  SSH/SFTP, FTP, Telegram, Termux mirrors, speech providers.
- **Tests:** JUnit 4 unit tests, AndroidX instrumented tests (not in CI),
  lint with a baseline, emulator visual QA workflow.
- **Custom vs inherited vs third-party:** the agent engine, tools, data layer
  and localization are inherited from the MIT-licensed AndroidHarness codebase
  and extended here; HMXHARNESS-specific additions include the product identity,
  cloud-only CI, the verification/approval policy in `hmx/`, the Liquid Glass
  token/tier system, the GitHub connection card and CI Client-ID injection.
  llama.cpp, Shizuku, libsu, Dhizuku, sora-editor, PrismalAGSL, Backdrop, JSch,
  BouncyCastle, Conscrypt and others are third-party integrations. No claim is
  made that inherited code was originally written by HMXHARNESS.

## D. Security and privacy audit (evidence and open questions)

Evidence was taken from `AndroidManifest.xml`, `SECURITY.md`, `PRIVACY.md`,
source paths and CI. Findings:

- Credentials use Keystore-backed encrypted preferences
  (`data/KeyStoreManager.kt`); settings backups are AES-256-GCM/PBKDF2
  (`data/BackupEncryption.kt`).
- GitHub Device Flow uses only a public Client ID, no client secret; tokens are
  redacted in user-visible errors.
- Workspace containment (`workspace/WorkspaceFs.kt`) and shell policy
  (`tools/ShellPolicy.kt`) are application-level checks, not an OS sandbox;
  full access bypasses the normal denylist, and root/Shizuku extend access.
- MCP static headers/environment values are stored in plaintext config with an
  HMAC integrity check; GitHub CLI token material may appear in an app-private
  `linux/` prefix (`.gh-token`).
- Cleartext traffic is enabled in the manifest with no scoped network security
  configuration.
- Release APK is signed with the **debug** key (alpha distribution, not
  Play-ready).
- No analytics/advertising/crash SDK and no project-owned backend were found.
- Network traffic goes directly from the device to configured providers/services;
  cloud AI requests can include prompts, file contents and command output.

**Unresolved:** live OAuth approval, physical-device behaviour, third-party
service practices, real-device performance and frame timing were not verified.
No third-party security audit or compliance certification exists.

## E. Previous claims corrected

- Expired old-repository commit hashes, commit counts, branch-consolidation
  stories and old CI run/artifact IDs were removed from `docs/expo/*` and the
  wiki README. The current repository has a fresh two-commit history and cannot
  substantiate those claims.
- "No data leaves the device" implications in `PRIVACY.md` were corrected to
  describe cloud transmission explicitly.
- The broken README anchor to a non-existent "Attribution and upstream" section
  was replaced with a real section.
- Obsolete `AHarness` product strings and references to removed
  `CODE_OF_CONDUCT.md`/`CONTRIBUTING.md` files were corrected.

## F. Remaining gaps

- 13 documentation reports under `docs/` remain untracked because of the
  existing ignore rule; they were preserved, not overwritten.
- Instrumented tests are not part of CI; only unit tests, lint and APK assembly
  are automatically verified.
- The `HMX_GITHUB_CLIENT_ID` repository variable is not configured, so current
  artifacts cannot use GitHub Device Flow sign-in.
- No physical device or accelerated emulator was used for this audit.
