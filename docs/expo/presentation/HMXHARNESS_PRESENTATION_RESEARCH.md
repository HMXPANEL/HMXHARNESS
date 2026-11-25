# HMXHARNESS — Project Expo Presentation Research

**Purpose:** single evidence base for building the Project Expo presentation. This is
research, not the PowerPoint. No application source, audit report or workflow was modified.

**Evidence policy used here:**
1. All **15** reports in `docs/audits/full-source-code/` were reviewed and cross-referenced.
2. `docs/expo/` (8 documents), root `README.md`, the existing architecture documents
   (`docs/expo/HMXHARNESS_ARCHITECTURE.md`, `docs/HMXHARNESS_ARCHITECTURE.md`) and the
   committed screenshots under `.github/assets/showcase/` were inspected.
3. Where documents disagreed, **current source code** was treated as the final authority.
   Section 0 records every conflict resolved and how.
4. Implementation status vocabulary: **runtime-wired** (real call path + automated tests),
   **test-only** (compiled/tested but no runtime caller), **UI-only**, **conditional**
   (setting/hardware/human consent required), **experimental**, **user-reported** (developer's
   manual device claim, not reproduced by the audits), **verified** (executed by CI).

---

## 0. Conflict resolution (read this before preparing slides)

| # | Conflict found | Evidence checked | Resolution used in this document |
| --- | --- | --- | --- |
| 1 | `README.md:352` lists the "verification gate" as an HMX contribution; all 15 audit reports and 7 Expo docs say the HMX gate is **test-only** | `hmx/HmxTaskVerification.kt:47-81`; `git grep canClaimCompletion\|HmxApprovalPolicy` found **no** runtime caller — only `HmxTaskVerificationTest.kt:31-61`. Real completion: `AgentEngine.kt:244,837` → `RunManager.kt:715,737`. Real approval: `AgentEngine.kt:42-48,1755-1830`, `ShellPolicy.kt`, `WorkspaceFs.kt:95-101`. | HMX **verdict/approval objects** are a genuine HMX addition, but they are **display/test-only**. Never call HMXHARNESS "verification-gated" at runtime. Say: "verification evidence is recorded and shown; the HMX gate objects are unit-tested but not wired." |
| 2 | Committed screenshots `.github/assets/showcase/01-code.jpg` and `02-chat.jpg` show "ANDROIDHARNESS" / "AndroidHarness", version 3.3 and `Sanuu7/AndroidHarness` | Current identity: `AgentEngine.kt:2637` "You are HMXHARNESS…"; `values/strings.xml:4` `app_name = HMXHARNESS`; `app/build.gradle.kts` version 4.0 (40); `README.md:352-353` | Screenshots are **historical pre-rebrand assets** showing real UI workflows but obsolete rendered text. Use them only as UI-flow illustrations with that caveat; do **not** present them as current branding and do **not** fabricate replacement screenshots. |
| 3 | `10-local-models.jpg` shows the experimental GGUF GPU preview toggle **on** and a Vulkan device, while audits say GPU is "default off" | `SettingsRepository.kt:234` default `false`; `LocalAccelerationPolicy.kt:11-12`; `LocalModelManager.kt:499-501,689-692` | No contradiction: opt-in feature shown in an enabled state. The screenshot proves the UI can display GPU status, **not** that inference correctness/performance was verified. |
| 4 | "21 locales" in Expo docs vs 22 `values*` directories in source | `find app/src/main/res -maxdepth 1 -type d -name 'values-*'` = **21** translated dirs; base `values/` is the English source of truth; `locales_config.xml:8-31` declares en + 21 locales; 1,129 strings in base | Phrase as "21 translated locales plus the English base" — consistent with both. |
| 5 | Tool counts: "about 60" in Expo architecture docs vs "82" in the complete/feature audits | `tools/Tool.kt:108-165` base = 52 registered; `:166-215` +30 conditionals; `HarnessApp.kt:344-375` satisfies all → **82 at startup**; Chat allowlist = 25 (`core/ChatExperience.kt:9-17`) | Use 82 at startup, ~85 in a Code run plus MCP tools, 25 in Chat. The "~60 base" refers to a subset. |
| 6 | "Instrumented tests exist and compile" vs CI evidence | No workflow compiles `androidTest` (`android-ci.yml:61-76` only `testDebugUnitTest`, `lintDebug`, assembly). Instrumented tests were not compiled locally either. | Say "555 instrumented `@Test` methods exist; they are **not compiled or run by CI** and were not executed in this audit." Do not claim they pass. |
| 7 | Screenshots show Google Play / 1K downloads; current project does not target Play publishing | `README.md` installation section uses GitHub Releases/Actions; release is debug-signed (`app/build.gradle.kts:101-106`) | Do not claim Play availability or store readiness. Historical screenshot text is upstream content, not a current distribution channel. |

---

## 1. Project introduction and problem statement

HMXHARNESS is a native Android application (`com.hmx.harness`, version 4.0/40, `minSdk 26`,
`targetSdk 36`, `compileSdk 37`) that runs an AI coding agent and chat assistant **on the
phone**. It is a single-activity Compose app with no project-owned backend.
[Source: Complete Source Audit §2; README.md "Overview"; `app/build.gradle.kts:60-70`]

**Problem:** a coding agent normally assumes a laptop — terminal, filesystem, IDE, Git.
Students and developers away from a desk need to read code, make edits, run checks and
publish a commit. Phone-only alternatives are usually web wrappers or chat assistants that
cannot touch a real project. [README "Problem statement"; Expo Project Overview §2]

**Honest framing:** HMXHARNESS removes the *requirement* to own a laptop for a useful subset
of work; it does not claim to replace a desktop IDE. It is an MIT-licensed derivative of
AndroidHarness by Sanuu7, not a from-scratch rewrite. [README.md:46,351-353; LICENSE:1-15]

## 2. Proposed solution

One integrated on-device stack: agent loop + tools + tiered shell + editor + WebView browser
+ local GGUF inference + GitHub integration, with cloud model providers configurable by the
user. Everything sensitive stays local (Keystore-backed credentials); cloud inference is
explicitly opt-in via provider configuration. [Complete Source Audit §3-7; Architecture report §5]

## 3. Complete application architecture

- **Startup:** `HarnessApp.attachBaseContext:34-37` → `onCreate:42` → `StorageGate.install:48`
  → `AppContainer(this):50` → privileged route `:51-55` → reminder re-arm `:58`.
  [Detailed Architecture §1]
- **Composition root:** ~524-line manual `AppContainer` (`HarnessApp.kt:72-595`); **no
  Hilt/Dagger/Koin** (verified zero matches). Only two ViewModels — `ChatViewModel` and
  `EditorBufferViewModel`; other screens read `AppContainer` directly. [Architecture §4]
- **Layers:** Compose UI / ViewModels / AppContainer / (AgentEngine+RunManager, ToolRegistry,
  LLM providers, Workspaces+editor, Room+DataStore+Keystore, GitHub+MCP+browser, Linux
  environment+native JNI). [README "Architecture overview"; Expo Architecture §1]
- **Data flow:** composer → `ChatViewModel.send:839` → `RunManager.startRun:429` →
  `AgentEngine.run:678` → `Tool.execute:1882` → `SessionRepository` → Room → UI flows.
  [Architecture §5]
- **Background:** 5 foreground services, 3 WorkManager workers, 5 broadcast receivers declared
  in `AndroidManifest.xml`; no crash reporter/telemetry (verified absence). [Architecture §6, §8]
- **State:** conversations/checkpoints/drafts/reminders durable; **in-flight runs are not
  auto-resumed after process death**. [Architecture §7; Critical Findings P1 architecture]

**Slide diagram:** use the ASCII diagram from `HMXHARNESS_DETAILED_ARCHITECTURE.md:57-103`
(redrawn as shapes), not a speculative cloud/server diagram.

## 4. AI agent internal workflow

Real loop: `AgentEngine.run()` `while(true)` at `AgentEngine.kt:463-882`, with iteration cap
`:468-473` (default unlimited), `TaskBudget.check()` `:464`, tool-call parsing from SSE
(`OpenAiCompatProvider.kt:385-408` etc.), parallel read-only tools and `task` subagents
(`executeCalls():1112-1197`), ordered tool-result commit `:853-858`, checkpoints `:1848,1889`,
compaction `:2393-2440`, cancellation via jobs/notification actions (`:1250-1257` in RunManager),
and completion via `AgentEvent.Finished` `:244,837` → `RunManager.kt:715,737`.
[FULL source: AI Agent Deep Analysis §1-2; Complete Source Audit §4]

**Context:** system prompt `systemPrompt():2604-2737` (identity at `:2637` = HMXHARNESS),
environment `:2759-2826`, memory cap 5,000 chars (`MemoryRepository.kt:73`), repo map
`:2728-2735`, budget `ChatContextPolicy.kt:5` = 250,000. [AI Agent Deep Analysis Q2-Q3]

**Permissions:** four `PermissionMode` values (`:42-48`), decision tree `:1755-1830` with
package installs always confirming, `ShellPolicy` denylist/containment, `WorkspaceFs` canonical
path check. Biometric gates **enabling FULL_ACCESS**, not individual cards. [AI Agent Q12]

**Must state honestly:** the HMX verdict/approval objects are **test-only**. [Section 0 conflict 1]

## 5. All major tools and integrations

**82 tools at startup** (52 base + 30 conditional), ~85 in Code runs plus N MCP tools, 25 in
Chat. [Tool System Audit §0, §4; Complete Source Audit §5]

| Family | Examples | Key controls |
| --- | --- | --- |
| Files | `read_file`, `write_file`, `edit_file`, `apply_patch`, `grep`, `read_image`, `file_restore` | workspace containment, atomic patch, ReDoS guard, SHA-256 restore |
| Shell / device | `shell`, `shell_background`, `bg_list`, `device_owner_shell`, `env_status`, `doctor`, `logcat`, `pkg_*`, `android_sdk_install` | denylist, signature grants, output caps, always-confirm installs |
| Git / GitHub | 10 `git_*` tools, `github_create_pull_request`, `github_api/search/file` | reviewed publish, `.harness/` excluded from commits, no force-push |
| Chat-only | chat recall, image inspect/zoom, device-flow read, reminders/alarm/timer, `offer_code_handoff` | fixed 25-name allowlist |
| Web / browser | `web_search`, `web_fetch`, `http_request`, 14 browser tools | provider allowlist, SSRF guard, source budget, sandboxed WebView |
| Memory / skills / agents | `remember`, `recall`, `task`, `subagent_*`, `skill_view`, `todo_write`, artifacts | profile-scoped, session-bound, engine-intercepted `task` |
| Settings / plugins / automation / code intelligence | `app_settings_*`, `app_plugins`, `automation_*`, `codegraph_*`, `lsp`, `reference_read` | access gates, approval, config-scoped reads |
| MCP | `mcp__<server>__<tool>` dynamic adapters | always non-read-only → gated; workspace server approval; PKCE |

Caveats for judges: `http_request` is marked read-only but can send POST/PUT/PATCH/DELETE;
`codegraph_sync` and `browser_get_logs(clear=true)` also have side effects while marked read-only;
`remember` is auto-approved in all modes. [Tool System Audit §2]

## 6. Technology stack

Kotlin 2.4.10, Gradle 9.7.0, AGP 9.3.1, KSP 2.3.11; Jetpack Compose BOM 2026.08.00 /
Material 3 Expressive 1.5.0-alpha26; Room 2.8.4, DataStore, WorkManager; OkHttp 4.12.0 +
SSE; Kotlin Coroutines/Flow; AndroidX Security Crypto + Biometric; Shizuku/libsu/Dhizuku;
sora-editor; PrismalAGSL + kyant0 Backdrop; native C++17/CMake/NDK 27.2.12479018 with
llama.cpp. [README "Technology stack"; Expo Tech Stack; Complete Source Audit §2]

## 7. AI providers and local GGUF models

Four wire protocols — OpenAI-compatible, OpenAI Responses, Anthropic Messages, Gemini —
plus ChatGPT account (OAuth PKCE), a built-in keyless relay to **third-party** endpoints,
`FallbackProvider` and encrypted `RotatingKeyProvider` (both runtime-wired).
[AI Provider Audit §1-3; Complete Source Audit §6]

**Local GGUF:** CPU is the shipped path (`LocalNative.kt:5-10`, llama.cpp pinned
`19cce691…`, SHA-256 verified downloads in `noBackupFilesDir/local-models`). **GPU is
opt-in experimental** (`localModelExperimental` default false at `SettingsRepository.kt:234`,
`LocalAccelerationPolicy.kt:11-12`); 32-bit ABIs get stubs. **No model training code exists**
— only format conversion/quantization (`safetensors_import.cpp:174-291`). [AI Provider Audit §4]

## 8. Android coding environment

Workspaces: app-private, SAF-picked, device-folder (SHELL) and SSH — **no project-type
registry**. Supported: generic repos, web HTML/CSS/JS/TS (static preview at
`https://harness.workspace` via `WebViewAssetLoader`), Java-only Gradle-less Android builds,
and Android Gradle with caveats. Not supported: Kotlin/Compose/AndroidX/AAR on the Gradle-less
route, server-side execution in the preview origin, language-server features for most languages.
[Android Environment Audit §1, §5]

Editor: sora-editor with tabs, undo/redo, find/replace, go-to-line, 8 syntax modes; semantic
completion/navigation/rename/format only for web/CSS/JSON families, guarded by tests
(`SmartEditorEditsTest.kt:29-38`). [Android Environment Audit §2]

Terminal tiers: `TERMUX_SSH | ROOT | PRIVILEGED | APP_LINUX | TOYBOX` chosen by cwd/grants;
Termux-derived userspace with SHA-256-verified packages. [Android Environment Audit §3-4]

**User-reported:** chat, file creation/editing, terminal and website preview worked on a
physical device, including a Student Attendance Management site. [Section 13]

## 9. GitHub and cloud build infrastructure

Device Flow uses only a public Client ID (no client secret), PAT fallback, EncryptedSharedPreferences
token storage, reviewed commits (`commit --only`), plain explicit-refspec push with `ls-remote`
verification and **no force-push**. Four workflows; CI installs the pinned NDK, runs unit tests,
debug lint, both APKs and uploads `hmxharness-apks` (90-day retention).
[GitHub/CI Audit §1-5]

**Live evidence:** run `45338701934` (commit `ae41ac7`) = success; artifact `14898111209`,
110,461,001 bytes, non-expired. Earlier failure `45337564227` was `./gradlew: Permission
denied` (exit 126), fixed by restoring Git mode `100755`. [GitHub/CI Audit §6; Test Coverage §7]

## 10. Original HMXHARNESS contributions

Product identity/branding; four GitHub Actions workflows + lint baseline; `hmx/` verification
verdict and approval-policy objects (**test/display-only**); Connected Accounts card;
adaptive Liquid Glass tier configuration/tokens over third-party renderers; the documentation
set. [Original Contributions report §3-4; README.md:352-353]

**Defensible uniqueness:** integrated on-device stack with no project backend; cloud CI
producing verified APKs; explicit provenance/honesty layer; adaptive glass configuration.
[Original Contributions §4]

## 11. Inherited upstream components

Agent loop, core tool registry and tools, data layer, memory, subagents, and the 21 translated
locales are inherited from the MIT-licensed AndroidHarness project by Sanuu7. Per-file
attribution exists for Shizuku-adapted ADB files ("Adapted from Shizuku, Apache 2.0"), vendored
CodeGraph with HMX patches, and the Caveman skill adapted from Julius Brussee's MIT skill.
[Original Contributions §2-3; LICENSE:1-15; README.md:351-353]

Do **not** claim: world's first mobile coding agent, all code original, trained model, verified
GPU, enforced verification gate, or Play/store readiness. [Original Contributions §5]

## 12. Verified features

- **Executed and gating in CI:** 2,478 JVM unit tests, debug lint (new issues only),
  `assembleDebug` + `assembleRelease`, non-empty APK check, artifact upload.
  [Test Coverage §3-4; GitHub/CI §6]
- **Runtime-wired with automated tests (source-level):** agent loop, chat/plan modes, tool
  system, shell/toolchain, Git, browser, research, memory, subagents, workspaces, editor,
  preview, localization, backups, background services, CPU GGUF, providers, cloud CI.
  [Feature Implementation Matrix; Complete Source Audit §4-7]
- **User-reported only (not reproduced):** physical-device chat, file creation/editing,
  terminal commands, website preview. [Complete Source Audit §11]

**Not verified anywhere:** 555 instrumented tests (not compiled/run by CI), native C++
behaviour, pixel/visual correctness, hardware performance, live OAuth approval, live provider
calls. [Test Coverage §4-8]

## 13. Student attendance website demonstration

**User-reported:** a Student Attendance Management website was generated and previewed on a
physical device, with an attendance date selector, enrolled-student count, present/absent
statistics, student roster, attendance marking and an add-student form.
[Complete Source Audit §11; Expo Assessment §1]

**What source confirms:** a real preview mechanism exists — `WorkspacePathHandler.kt:8-19`
serves the active workspace at `https://harness.workspace/ws/...` via `WebViewAssetLoader`
(static files + MIME types, **no server-side execution**); `BrowserController.kt:685-690,733`
also supports navigating to a locally run dev server. [Android Environment Audit §5]

**Direct visual evidence supplied by the developer:** three untracked device screenshots were
present in the working directory during this research (they are not part of the audit reports
and were left untouched):

- `Screenshot_2026-11-24-14-02-31-05_28c2859092379cd2f7b967065d899178.jpg` — the
  **Attendance** dashboard headed "Student management dashboard", attendance date
  `24/11/2026`, *Enrolled students 5*, *Present 4 (80%)*, *Absent 1 (20%)*, *Not marked 0*,
  and a "Class roster" section. The app's agent banner reads **"Completed · Open page"**.
- `Screenshot_2026-11-24-14-02-33-92_28c2859092379cd2f7b967065d899178.jpg` — the class roster
  with five named students (Aarav Sharma ST-101, Diya Patel ST-102, Kabir Singh ST-103,
  Meera Iyer ST-104, Rohan Gupta ST-105) and Present / Absent toggles.
- `Screenshot_2026-11-24-14-02-35-72_28c2859092379cd2f7b967065d899178.jpg` — the same roster
  scrolled to the Student name / Roll number fields and Add student button.

These images directly corroborate the developer's report that the agent generated a working
attendance website and opened it in the in-app preview. They remain **developer-supplied
device evidence**, not something this research reproduced; the screenshots are untracked files
in the working directory and were preserved as-is.

**What is still NOT confirmed:** the site's source files in the repository, persistence across
sessions, correctness beyond the displayed values, or build/install of any other app. Label the
demo **USER-REPORTED with supplied screenshots**; a live re-run remains the strongest evidence.
The older committed showcase screenshots do not depict this site.

## 14. Security and privacy

- **Credentials:** EncryptedSharedPreferences + Android Keystore (`KeyStoreManager.kt:9-42`);
  GitHub Device Flow uses no client secret (`GitHubAuthTest.kt:92,162`).
- **Controls verified sound:** workspace canonicalization, shell denylist, artifact/editor
  WebView sandboxing, MCP PKCE, update URL allowlist. [Security Deep Audit "Controls verified"]
- **4 High:** debug-signed release/updater trust; cleartext traffic + `http://` endpoints;
  DB not excluded from Android backup; plaintext MCP config with fail-open HMAC.
  **5 Medium / 6 Low** also recorded. [Security Deep Audit §Findings]
- **Privacy:** no analytics/advertising/crash SDK; cloud provider requests send prompts and
  context (possibly file contents/command output) off-device; local GGUF is the only offline
  path; `PRIVACY.md` has 22 sections and an explicit unverified-items section.
  [README "Privacy and security"; PRIVACY review report]

Never say "no data leaves the device" or "fully secure".

## 15. Current limitations

Debug-signed alpha release (not Play-ready); unwired HMX gate; no hardware evidence artifact;
GPU experimental; instrumented tests/native tests absent from CI; 3,930 baselined lint issues;
release lint disabled; screenshots non-gating; process-death loss of in-flight runs; stale CI
comment; `HMX_GITHUB_CLIENT_ID` not currently set (Device Flow disabled; PAT still works).
[Critical Findings P0-P2; Test Coverage §3-8; GitHub/CI §8]

## 16. Innovation and uniqueness

The **combination** is the story, not individual novelties: native Android agent + real tiered
shell + on-device GGUF + browser automation + MCP + subagents + memory + adaptive glass UI,
with no project backend and cloud-built APKs. HMX-specific work is the CI/provenance/verdict
layer; the engine is inherited. [Original Contributions §4; Uniqueness doc §1-3; Expo Assessment §1]

## 17. Future roadmap

Prioritised, evidence-linked (from `HMXHARNESS_CRITICAL_FINDINGS_AND_RECOMMENDATIONS.md`):
1. wire or rename the verification gate; 2. rehearse/capture one live end-to-end demo;
3. backup DB exclusions + disable cleartext + URL validation; 4. dedicated release signing +
mandatory digest; 5. encrypt MCP secrets/fail closed; 6. release lint + pixel assertions + CI
comment/baseline alignment; 7. one instrumented device smoke for shell tier and CPU GGUF.
Also: real-device visual/thermal verification, more connected-account scope controls, continued
translation coverage, better repo maps for large workspaces. [README "Roadmap"]

## 18. Ten-slide Project Expo outline

| # | Slide | Core message | Evidence anchor |
| --- | --- | --- | --- |
| 1 | Title | HMXHARNESS — AI coding agent that runs natively on Android | README title; `strings.xml:4` |
| 2 | Problem | Laptop assumed; phone users need real files/shell/Git | README "Problem statement" |
| 3 | Solution | One on-device stack; no project backend | Architecture §3; README §"Overview" |
| 4 | Architecture | Real class diagram (no invented server) | Detailed Architecture §3 |
| 5 | Agent loop | `AgentEngine.run` → SSE → tools → results → `Finished` | AI Agent Deep Analysis §1-2 |
| 6 | Tools & environment | 82 tools, tiered shell, Termux toolchain, editor | Tool System §0-2; Environment §2-4 |
| 7 | Providers & local AI | 4 protocols + keyless relay + CPU GGUF; GPU experimental | Provider Audit §1-4 |
| 8 | Live demo | chat → edit → diff → terminal → website preview | User-reported + 3 supplied device screenshots; Section 13 |
| 9 | GitHub & CI | Device Flow (no secret), 4 workflows, real APK artifact | GitHub/CI §5-6 |
| 10 | Honesty & roadmap | Provenance, gate caveat, security findings, next steps | Critical Findings; Original Contributions |

## 19. Ten-minute presentation script

| Time | Say | Show |
| --- | --- | --- |
| 0:00-1:00 | "HMXHARNESS is a native Android app that runs a coding agent, chat, a real tiered shell and optional local models on the phone. No PC and no project server." | Slide 1-3; app icon |
| 1:00-2:00 | "The problem: agents assume a laptop. We remove that requirement for real files, commands and Git." | Slide 2 |
| 2:00-3:00 | "Architecture: one activity, Compose, a manual AppContainer, and an agent loop that drives tools and persists results in Room." | Slide 4 (real diagram) |
| 3:00-4:00 | "The loop streams provider events, parses tool calls, runs safe reads in parallel, checkpoints before edits, and finishes on AgentEvent.Finished." | Slide 5; `AgentEngine.kt:463` |
| 4:00-5:00 | "82 tools are registered at startup; Chat sees a fixed 25-tool allowlist; Plan mode restricts schemas to read-only." | Slide 6; `Tool.kt:81-217` |
| 5:00-6:00 | "Shell tiers select app-Linux, toybox, Shizuku, root or Termux SSH by workspace and grants; the Termux-derived toolchain is SHA-256 gated." | `ShellTierRouter.kt:68-82` |
| 6:00-7:00 | "Providers: four wire protocols plus keyless third-party relay. CPU GGUF is shipped; GPU is experimental and default-off. We do not train models." | Slide 7 |
| 7:00-8:30 | "Live demo: chat → file edit and diff → terminal → website preview." **Do this live; do not substitute screenshots.** | Slide 8; device |
| 8:30-9:15 | "GitHub Device Flow needs no client secret; CI builds, tests, lints and uploads real APKs — this is a successful run and artifact." | Slide 9; run `45338701934` |
| 9:15-10:00 | "Honesty: we are an MIT derivative; the HMX verdict objects are test-only, not an enforced gate; release is debug-signed; next steps are wired gating, signing and device smoke tests." | Slide 10 |

## 20. Likely judge questions and evidence-based answers

1. **"Is this your own engine?"** — "The loop at `AgentEngine.kt:296,463` is inherited upstream AndroidHarness; we are an MIT derivative (`LICENSE:1-15`, `README.md:351`). Our additions are listed in `README.md:352`."
2. **"Web wrapper or server?"** — "No. Single native activity (`AndroidManifest.xml:114-131`), Compose (`MainActivity.kt:98`), no project-owned backend (`README.md:161,302`)."
3. **"What did HMX add?"** — "Identity, four CI workflows, `hmx/` verdict/approval objects (test/display-only), Connected Accounts card, lint baseline, glass tiers, docs."
4. **"Does the verification gate block bad completions?"** — "No. `assess()` renders a card (`VerificationCard.kt:48`); `canClaimCompletion()` and `HmxApprovalPolicy` have no runtime caller. Real approval is `PermissionMode` (`AgentEngine.kt:42`), `RunManager.approve/deny` (`:1122,1128`) and `ShellPolicy.kt:17-40`."
5. **"Shell without root?"** — "`ShellTierRouter.kt:26-33,68-82` picks app-LINUX or TOYBOX; privileged tiers need Shizuku/WADB/root. ADB transport was adapted from Shizuku (Apache 2.0)."
6. **"Can it delete arbitrary files?"** — "Default containment: `ShellPolicy.kt:17-40,289-341`, `WorkspaceFs.kt:95-101`. FULL_ACCESS removes layers — we will not demo it."
7. **"Is local AI real?"** — "CPU GGUF: `LocalNative.kt:5-10`, llama.cpp pinned in `CMakeLists.txt:85-90`. GPU is opt-in experimental (default false). No training code."
8. **"How does MCP OAuth work?"** — "OAuth 2.1 PKCE, custom-scheme redirect (`McpOAuth.kt:63-79,191`). Live consent needs a human and is not in CI."
9. **"Does browser automation really drive a page?"** — "Real WebView tools (`BrowserTools.kt:71-649`); instrumented tests exist but CI does not run them — we demo live."
10. **"What proves the build and tests?"** — "Run `45338701934` succeeded: 2,478 unit tests, debug lint, both APKs, artifact `14898111209` (110,461,001 bytes). 555 instrumented tests are not run by CI."
11. **"Are glass effects yours?"** — "Tier detection/tokens are HMX (`GlassQuality.kt:17,39-80`); rendering is PrismalAGSL + Backdrop. Screenshots have no pixel assertions."
12. **"Store-ready?"** — "No. Debug-signed alpha (`app/build.gradle.kts:101-106`), release lint disabled."
13. **"Tested on device?"** — "USER-REPORTED manual testing of chat, file editing, terminal and the Student Attendance Management site; the developer supplied three device screenshots of the attendance dashboard and roster. The audits themselves ran no device."
14. **"Why the old AndroidHarness text in screenshots?"** — "Those are historical pre-rebrand assets; current source says HMXHARNESS at `AgentEngine.kt:2637` and `strings.xml:4`."

## Appendix — report index (all 15 reviewed)

`HMXHARNESS_COMPLETE_SOURCE_AUDIT.md`, `HMXHARNESS_DETAILED_ARCHITECTURE.md`,
`HMXHARNESS_AI_AGENT_DEEP_ANALYSIS.md`, `HMXHARNESS_TOOL_SYSTEM_AUDIT.md`,
`HMXHARNESS_ANDROID_ENVIRONMENT_AUDIT.md`,
`HMXHARNESS_AI_PROVIDER_AND_LOCAL_MODEL_AUDIT.md`,
`HMXHARNESS_GITHUB_CLOUD_INFRASTRUCTURE_AUDIT.md`, `HMXHARNESS_SECURITY_DEEP_AUDIT.md`,
`HMXHARNESS_DATABASE_AND_MEMORY_AUDIT.md`, `HMXHARNESS_UI_UX_AND_PERFORMANCE_AUDIT.md`,
`HMXHARNESS_TEST_COVERAGE_AUDIT.md`, `HMXHARNESS_FEATURE_IMPLEMENTATION_MATRIX.md`,
`HMXHARNESS_ORIGINAL_CONTRIBUTIONS_AND_UPSTREAM_ANALYSIS.md`,
`HMXHARNESS_PROJECT_EXPO_TECHNICAL_ASSESSMENT.md`,
`HMXHARNESS_CRITICAL_FINDINGS_AND_RECOMMENDATIONS.md` — all in `docs/audits/full-source-code/`.
