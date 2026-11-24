# HMXHARNESS — Project Overview

> Evidence-based audit of the application code at `main` (`22e674e9`; later
> commits are documentation only). Every claim below
> is tied to source files or repository documents. Claims that are not backed by
> code are explicitly marked.

## 1. What HMXHARNESS is

HMXHARNESS is a native **Android application** (`com.hmx.harness`) that puts an
AI **coding agent** and a **chat assistant** on the phone. It is not a thin web
wrapper and does not require a desktop host.

- **App identity:** `applicationId = "com.hmx.harness"`, `versionName = "4.0"`,
  `versionCode = 40`, `minSdk 26`, `targetSdk 36`, `compileSdk 37`
  (`app/build.gradle.kts`).
- **Size of the source tree:** 732 Kotlin files under `app/src/main/java`
  (~158,000 lines), 314 UI Kotlin files, 11 C/C++ source/header files.
- **Two working modes:** `Code` (the agent, with files and shell) and `Chat`
  (conversation-only: a fixed tool allowlist with no project-file, terminal or
  device access), described in `README.md`, `core/ChatExperience.kt` and
  `data/ConversationConfig.kt`; `AgentMode` (`ACT`, `PLAN`) in
  `agent/AgentEngine.kt` further restricts Code to read-only tools in Plan mode.

### Provenance — important and verifiable

HMXHARNESS is an **MIT-licensed derivative of AndroidHarness** by Sanuu7.
The `LICENSE` file reads `Copyright (c) 2026 Sanuu7`. `README.md`
("Attribution and upstream") states that the upstream architecture, agent
engine, tools and UI foundation were preserved, and that this project adds its
own identity, cloud CI, Liquid Glass material system and GitHub Device Flow
integration.

> **Audit correction.** Git history shows that the GitHub **Device Flow
> implementation** (`github/GitHubAuth.kt`) was introduced upstream in commit
> `2207914579` (2026-10-09, author Sanuu) — it predates the rebrand. HMX's
> GitHub work is the **Connected Accounts card** (`e169ef40`) and the **CI
> Client-ID injection** (`f9dccd39`, `5e100284`). The README sentence therefore
> overstates HMX's authorship; this document set does not repeat that claim.
> Likewise the 21 locales are upstream (2026-11-19); HMX added two strings to
> them (`5030a1da`, `5c090d5d`).

Git history supports this:

| Metric | Value |
|---|---|
| Total commits reachable from `main` | 961 |
| Commits by upstream author `Sanuu` | 828 |
| Commits by HMX identities (`HMXHARNESS`, `HMXHARNESS Build Agent`) | ~80 |
| First HMX commit | `2cb00a3d` (2026-11-20) "Rebrand AndroidHarness to HMXHARNESS and add cloud CI" |
| History span | 2026-08-24 → 2026-11-23 |

**Therefore:** do not present HMXHARNESS as an app written entirely from
scratch. Present it honestly as an independently maintained derivative with
substantial original additions (see `HMXHARNESS_CUSTOM_MODULES.md`).

## 2. Target users

Documented in `README.md` and `PRIVACY.md`:

| User | Need |
|---|---|
| Students / hobby developers without a PC | Work on real repositories from an Android phone. |
| Developers on the move | Read, edit, run tests and push commits without carrying a laptop. |
| Privacy-conscious users | Credentials stay in Keystore-backed storage; local GGUF models run on-device. |
| Power users | Terminal, Git, Shizuku/root/Wireless ADB, MCP, plugins and automation. |

*Documented motivation* (README): "An AI coding agent and chat assistant that
runs on your Android phone. No PC required. No root required."
The README's "Core ideas" list what the app optimises for: visible tool calls,
approval for destructive actions, graceful degradation, on-device credentials.

*Inferred motivation* (clearly inferred, not a personal story): the feature set
(phone-only agent, Shizuku/ADB shell, Termux toolchain bootstrap, on-device
GGUF inference, metered-network-aware automations) addresses the constraints of
developing without a desktop. **The developer's personal origin story is not
documented in the repository**, so do not invent one.

## 3. Primary use cases

1. **Mobile coding:** open a project folder, ask the agent to change code, review
   diffs, run build/test checks and push (Code mode).
2. **Research and writing:** ask questions with web search, page reading,
   citations and rich answers (Chat mode).
3. **Repository work:** import a GitHub repository, commit and open pull
   requests from the phone (`github/`, `tools/GitTools.kt`, `tools/GitHubTools.kt`).
4. **Device automation:** scheduled prompts, reminders, alarms, timers
   (`automation/`, `reminders/`).
5. **Private/local AI:** run GGUF models on the device (`local/`, `app/src/main/cpp/`)
   or connect cloud providers.
6. **Remote control:** drive the agent from Telegram while the app is minimised
   (`telegram/`).

## 4. Documented product principles

From `README.md`:

- **Everything important is visible.** Tool calls, diffs, command output and
  verification results appear as cards.
- **Destructive or privileged actions ask for approval.** Enforced in
  `AgentEngine` (`PermissionMode`) and `tools/ShellPolicy.kt`.
- **Graceful degradation.** Shell access, glass rendering quality and model
  choice adapt to the device (`ui/liquidglass/GlassQuality.kt`,
  `data/env/ShellTierRouter.kt`).
- **Credentials stay on the device.** `data/KeyStoreManager.kt` uses
  `EncryptedSharedPreferences` with an Android Keystore `AES256_GCM` master key.

## 5. What is demonstrable at a Project Expo

These are the strongest, evidence-backed things a student can show:

| Demo | Evidence |
|---|---|
| Ask the agent to edit a file and watch a diff + verification card | `agent/AgentEngine.kt`, `agent/VerificationEvidence.kt`, `ui/chat/components/VerificationCard.kt` |
| Switch between Code and Chat modes | `ui/AppNav.kt`, `AgentMode` |
| Sign in to GitHub with a device code (no client secret) | `github/GitHubAuth.kt` |
| Run a shell command through Shizuku / root / toybox | `data/env/`, `tools/ShellTool.kt` |
| Run a local GGUF model on-device (supported device) | `local/LocalNative.kt`, `app/src/main/cpp/local_model.cpp` |
| Show the Liquid Glass UI in light and dark | `ui/liquidglass/`, `ui/theme/` |
| Show 21-language UI | `app/src/main/res/values-*` (21 locales) |
| Show the cloud build producing real APKs | `.github/workflows/android-ci.yml` |

## 6. Repository health signals

| Signal | Value | Source |
|---|---|---|
| Cloud CI | Green on `main` | run `45204023911`, `45219214615` |
| Unit tests | 2,478 `@Test` methods across 351 test files | `app/src/test` |
| Instrumentation tests | 555 `@Test` methods across 139 files (not wired into CI) | `app/src/androidTest` |
| Android lint | 0 errors; pre-existing baseline | CI step "Run Android lint (debug)" |
| Locales | 21 (`values-*`) | `app/src/main/res` |
| Release APK | `com.hmx.harness`, 4.0 (40) | `aapt dump badging` on CI artifact |

## 7. Honest limitations (summary)

- Release APKs are signed with the **Android debug certificate**
  (`app/build.gradle.kts` release `signingConfig = signingConfigs.getByName("debug")`).
- **Live GitHub Device Flow approval** was not completed in this audit (requires
  a human). Flow logic is unit-tested (`GitHubAuthTest.kt`).
- **Instrumentation tests are not run by CI**; the emulator workflow is used for
  screenshots, not `connectedAndroidTest`.
- **Real-device performance** is not measured; `docs/HMXHARNESS_PERFORMANCE_REPORT.md`
  states frame-time/jank are unmeasured.
- Cleartext traffic is globally enabled (`AndroidManifest.xml`) — documented HIGH
  security finding in `docs/HMXHARNESS_SECURITY_AUDIT.md`.

See `HMXHARNESS_UNIQUENESS_AND_COMPARISON.md` for what must **not** be claimed.
