# HMXHARNESS — Project Overview

> **Repository state (fresh migration).** `https://github.com/HMXPANEL/HMXHARNESS.git`,
> default branch `main`, HEAD `ae41ac7399d2bdfa6248cbf4c00d362451675990`
> ("Fix CI: restore executable bit on Gradle wrapper"). The project tree was
> imported as a ZIP into a brand-new empty repository; the previous repository's
> Git history was **not** carried over and remains untouched as a backup. The
> new history contains only two commits: the initial migration import
> (`0d98456`) and the wrapper executable-bit fix (`ae41ac7`).
>
> Source paths in this document set are relative to
> `app/src/main/java/com/hmx/harness/` unless stated otherwise. Claims about
> behaviour are tied to files; claims about builds are tied to the current CI
> run. Device-dependent behaviour is explicitly labelled as not hardware-verified.

## 1. What HMXHARNESS is

HMXHARNESS is a **native Android application** (`applicationId = com.hmx.harness`)
that puts an AI coding agent and a chat assistant on a phone. It is not a thin
web wrapper and does not require a desktop host. `MainActivity.kt` is a single
activity rendering Jetpack Compose; navigation lives in `ui/AppNav.kt`; the
composition root is `AppContainer` in `HarnessApp.kt`.

| Identity value | Value | Source |
|---|---|---|
| Application ID | `com.hmx.harness` (debug builds use `.debug`) | `app/build.gradle.kts` |
| Version | `versionCode 40`, `versionName "4.0"` | `app/build.gradle.kts` |
| SDK range | `minSdk 26`, `targetSdk 36`, `compileSdk 37` | `app/build.gradle.kts` |
| Languages | 1,231 Kotlin files (732 in `app/src/main`, 352 unit tests, 140 instrumented tests); 5 Java; 12 C/C++/CMake files under `app/src/main/cpp/` | repository measurement |
| UI | Jetpack Compose, Material 3 Expressive, custom HMX Liquid Glass material system | `ui/AppNav.kt`, `ui/liquidglass/` |

Two working modes are documented in `README.md` and implemented in
`core/ChatExperience.kt` and `data/ConversationConfig.kt`:

- **Code** — the agent can read and edit workspace files, run shell commands and
  use Git/GitHub tools.
- **Chat** — a conversation-only assistant with a fixed tool allowlist: no
  project files, no terminal, no arbitrary filesystem access.

`agent/AgentEngine.kt` additionally defines `AgentMode` (`ACT` / `PLAN`), where
`PLAN` restricts tool schemas to read-only (`readOnlyOnly = mode == AgentMode.PLAN`).

### Provenance — read this before repeating any authorship claim

HMXHARNESS is an **independently maintained MIT-licensed derivative of
AndroidHarness by Sanuu7**. `LICENSE` preserves the original MIT text verbatim,
including `Copyright (c) 2026 Sanuu7`. `README.md` ("License and legal
attribution") and the app's About screen (`ui/settings/SettingsScreen.kt`,
which credits `Sanuu` and links `github.com/Sanuu7`) preserve that attribution.

The upstream project contributed the agent engine, tool framework and much of
the UI foundation. HMXHARNESS contributes its own identity, cloud CI, the
Liquid Glass material system, the `hmx/` verification and approval objects, the
GitHub connection card in Settings, and its documentation. Provenance is
per-component, not a blanket statement: upstream headers are rare, so do not
claim "all engine code is upstream" or "all code is HMX-original". Honest
examples of mixed origin include ADB transport files under `data/env/adb/`
marked "Adapted from Shizuku (Apache 2.0)", the vendored CodeGraph bundle with
HMX patches in `data/env/CodeGraphPatches.kt`, and the Caveman skill adapted
from Julius Brussee's MIT work (`app/src/main/assets/licenses/caveman.txt`).
Third-party license notices live in `app/src/main/assets/licenses/`,
`app/src/main/assets/richtext/*/LICENSE` and
`app/src/main/assets/editor-tools/LICENSES.txt`.

**This repository is a fresh migration.** Historical commit hashes, commit
counts, author statistics and CI history from the previous repository are not
part of this repository and are deliberately not reproduced here. Attribution
is preserved through the license, notices and documentation instead.

## 2. Problem statement

A coding agent normally assumes a laptop: a terminal, a filesystem, an IDE, a
package manager and a stable network. Students, hobbyists and developers who
are mobile or do not own a PC cannot use that setup. Existing phone terminals
can run commands but do not provide a reasoning agent that reads a project,
makes edits, runs checks and reports evidence.

Documented motivation in `README.md`: *"An AI coding agent and chat assistant
that runs on your Android phone. No PC required. No root required."* The
README's "Core ideas" describe the design goal: visible tool calls, approval
for destructive actions, graceful degradation and on-device credentials.

**Inferred motivation (clearly inferred, not a personal story):** the feature
set — phone-only agent, tiered shell access, Termux toolchain bootstrap,
on-device GGUF inference — addresses the constraints of developing without a
desktop. No personal origin story is documented; do not invent one.

## 3. Proposed solution

Run the agent loop, tools and data on the phone itself, and connect to whatever
model provider or local model the user configures (`agent/AgentEngine.kt`,
`llm/Llm.kt`, `local/LocalModelProvider.kt`). No HMX backend exists: provider
requests go directly from the device to the configured endpoint.

| User need | Implementation evidence |
|---|---|
| Work on a real repository from a phone | `workspace/WorkspaceFs.kt`, `tools/FileTools.kt`, `tools/PatchTools.kt`, `tools/GitTools.kt` |
| Real command execution without a PC | `data/env/ShellTierRouter.kt`, `tools/ShellTool.kt`, `data/env/LinuxEnvironment.kt` |
| Use cloud or local models | `llm/Llm.kt` (`ProviderFactory.create`), `local/LocalNative.kt` |
| Keep credentials on the device | `data/KeyStoreManager.kt` (`EncryptedSharedPreferences` with Keystore `AES256_GCM` master key) |
| Work with GitHub without shipping a client secret | `github/GitHubAuth.kt` (Device Flow; upstream implementation), `ui/settings/ConnectedAccountsSection.kt` (HMX-added card) |
| See what the agent did | `ui/chat/components/VerificationCard.kt`, `agent/VerificationEvidence.kt` |

## 4. Target users and use cases

Documented in `README.md` and `PRIVACY.md`:

| User | Need |
|---|---|
| Students / hobby developers without a PC | Work on real repositories from an Android phone. |
| Developers on the move | Read, edit, run checks and push commits without a laptop. |
| Privacy-conscious users | Credentials stay in Keystore-backed storage; GGUF models can run on-device. |
| Power users | Terminal, Git, Shizuku/root/Wireless ADB, MCP, plugins and automation. |

Primary use cases:

1. **Mobile coding** — open a project folder, ask for a change, review the diff,
   run checks and push (Code mode; `ui/AppNav.kt`, `tools/GitTools.kt`).
2. **Research and writing** — questions with web search, page reading and rich
   answers (Chat mode; `core/ChatExperience.kt`, `tools/SearchProviders.kt`).
3. **Repository work** — import a GitHub repository, commit and open pull
   requests (`github/GitHubRepository.kt`, `github/GitHubPullRequests.kt`).
4. **Device automation** — scheduled prompts, reminders, alarms and timers
   (`automation/AutomationWorker.kt`, `reminders/`).
5. **Private/local AI** — GGUF inference on-device or cloud providers
   (`local/`, `app/src/main/cpp/`).
6. **Remote control** — drive the agent from Telegram while the app is
   minimised (`telegram/TelegramGateway.kt`).

## 5. Repository health signals (current repository)

| Signal | Verified value | Source |
|---|---|---|
| Cloud CI | **Success** | run `45338701934` for commit `ae41ac7` |
| CI steps | Wrapper validation, JDK 21, pinned NDK install, unit tests, lintDebug, assembleDebug + assembleRelease, APK verification, artifact upload — all passed | `.github/workflows/android-ci.yml`, run `45338701934` |
| APK artifact | `hmxharness-apks`, id `14898111209`, 110,461,001 bytes, not expired | current Actions artifact |
| Earlier failed run | `45337564227` failed with `./gradlew: Permission denied` (exit 126) after ZIP extraction lost the executable bit; fixed by restoring Git mode `100755` on `gradlew` (commit `ae41ac7`) | current repository history |
| Unit tests | 2,478 `@Test` methods (352 Kotlin files) under `app/src/test` | repository measurement |
| Instrumented tests | 555 `@Test` methods (140 Kotlin files) under `app/src/androidTest`; **not run by CI** | repository measurement |
| Lint | `lintDebug` passes; `app/lint-baseline.xml` freezes inherited findings | CI run `45338701934` |
| Locales / strings | 21 `values-*` locale directories plus base `values/`; 1,129 entries in `values/strings.xml` | `app/src/main/res/` |
| Source footprint | 2,526 files on disk, 2,513 Git-tracked; 143 Markdown files on disk (130 tracked); 8 of those are `docs/expo/` | repository measurement |

## 6. What can be demonstrated at a Project Expo

| Demo | Evidence | Verification status |
|---|---|---|
| Agent edits a file; diff and verification card appear | `agent/AgentEngine.kt`, `agent/VerificationEvidence.kt`, `ui/chat/components/VerificationCard.kt` | Code and unit tests; device UI not hardware-verified |
| Switch Code / Chat / Plan modes | `core/ChatExperience.kt`, `data/ConversationConfig.kt`, `AgentMode` in `agent/AgentEngine.kt` | Code and unit tests |
| Tiered shell command | `data/env/ShellTierRouter.kt`, `tools/ShellTool.kt`, `tools/ShellPolicy.kt` | Implemented and unit-tested; not hardware-verified |
| Local GGUF model (CPU) | `local/LocalNative.kt`, `local/LocalModelProvider.kt`, `app/src/main/cpp/local_model.cpp` | Implemented; not hardware-verified; GPU is opt-in experimental |
| Liquid Glass light/dark UI | `ui/liquidglass/GlassQuality.kt`, `ui/theme/` | Tier resolution unit-tested; screenshots captured, **no gating pixel assertions** |
| 21-language UI | `app/src/main/res/values-*` | Format/localization unit tests |
| Cloud build produces real APKs | `.github/workflows/android-ci.yml`, run `45338701934`, artifact `14898111209` | **CI-verified** |

## 7. Honest limitations (summary)

- Release APKs are signed with the **Android debug signing config**
  (`app/build.gradle.kts`: `signingConfig = signingConfigs.getByName("debug")`).
  They are installable alpha builds, **not Play Store ready**.
- The `HMX_GITHUB_CLIENT_ID` repository variable is **not currently set**. CI
  warns and continues; GitHub Device Flow sign-in is disabled in that artifact,
  while personal access token (PAT) sign-in still works. No client secret was
  introduced.
- `HmxApprovalPolicy` and `canClaimCompletion()` in `hmx/HmxTaskVerification.kt`
  have **no runtime caller** (only `HmxTaskVerificationTest.kt`). The HMX verdict
  object is used by `ui/chat/components/VerificationCard.kt` for display; it does
  not gate the agent loop. Real approvals come from `PermissionMode` in
  `agent/AgentEngine.kt`, `RunManager.approve/deny` and `tools/ShellPolicy.kt`.
- **No physical-device verification** was performed. Device-dependent behaviour
  is implemented and often test-covered, but not hardware-verified.
- Instrumented tests exist but are not executed by CI; the visual-QA workflow
  captures screenshots, and `LiquidGlassScreenshotTest.kt` contains no pixel
  assertions and swallows failures with `runCatching { }.onFailure { Log.w }`.
- Cleartext traffic is enabled in the manifest and MCP static headers are stored
  in plaintext config; these are documented in `SECURITY.md`. No claim of "no
  vulnerabilities" or third-party audit is made. No analytics or crash SDK;
  network calls go directly to configured providers.

See `HMXHARNESS_FEATURE_VERIFICATION.md` for the feature-by-feature matrix and
`HMXHARNESS_UNIQUENESS_AND_COMPARISON.md` for claims that must not be made.
