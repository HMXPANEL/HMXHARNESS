# HMXHARNESS — Complete Source Audit (Executive Summary)

**Date:** 2026-11-24
**Repository:** https://github.com/HMXPANEL/HMXHARNESS.git
**Application:** HMXHARNESS · **Package:** `com.hmx.harness` · **Version:** 4.0 (40)
**Branch:** `audit/hmxharness-full-source-audit` · **Base:** `9188783`
**Method:** independent static source inspection, public CI evidence and source-verified
GitHub API checks. No device/emulator was used, no Gradle build was run, and no source,
schema, dependency, provider, auth or workflow file was modified.

## 1. Scope and evidence discipline

This audit deliberately did **not** treat existing audit or documentation reports as
evidence. Every technical claim was re-verified against current source with `find`, `grep -n`,
`git grep` and file reads. CI conclusions were verified through the public GitHub API
(anonymous), and the APK artifact's existence and size were confirmed there. Where a claim
could not be verified, it is labelled unverified. All fifteen required reports are in
`docs/audits/full-source-code/`.

## 2. Repository inventory (measured)

| Metric | Value |
| --- | ---: |
| Files on disk / Git-tracked | 2,532 / 2,519 |
| Kotlin: main / test / androidTest / debug / githubDelivery / playDelivery | 732 / 352 / 140 / 4 / 2 / 1 (1,231 total) |
| Java / C++ `.cpp` / C headers / CMake | 5 / 8 / 3 / 1 |
| JS+MJS / Python / shell / XML | 7 / 3 / 5 / 1,009 |
| Markdown on disk / tracked | 149 / 136 |
| Modules | `:app` plus conditional dynamic-feature `:local_gpu` |
| `@Test` methods | 2,478 unit (351 files), 555 instrumented (139 files) |
| Room | 14 entities, schema v27, 23 migrations, second debug DB |
| GitHub Actions workflows | 4 |
| Source sets | main, debug, githubDelivery, playDelivery, test, androidTest |

Main entry points: `HarnessApp.kt` (Application/AppContainer), `MainActivity.kt` (single
activity), `ui/AppNav.kt` (NavHost), `agent/AgentEngine.kt` + `agent/RunManager.kt` (agent),
`tools/Tool.kt` (registry).

## 3. Architecture summary

Manual dependency injection via a ~524-line `AppContainer` built synchronously in
`Application.onCreate` — no Hilt/Dagger/Koin. Only two ViewModels; other screens are
Composables reading `AppContainer` directly. Manual navigation with in-memory pending
channels (no Navigation `deepLink{}`). Foreground services, WorkManager workers and
broadcast receivers are declared in the manifest. Data flow:
UI → `ChatViewModel.send()` → `RunManager.startRun()` → `AgentEngine.run()` →
`ToolRegistry`/`Tool.execute()` → `SessionRepository` → Room → UI flows.
Full details and an ASCII diagram: `HMXHARNESS_DETAILED_ARCHITECTURE.md`.

## 4. AI coding agent

The agent loop is real and runtime-wired: `AgentEngine.run()` `while(true)` at `:463-882`,
streaming via OkHttp SSE (`Llm.kt:245-305`), id-keyed tool-call parsing, parallel safe
read-only tools and `task` subagents (`executeCalls():1112-1197`), ordered tool-result
commit, checkpoints before edits, retry/fallback/compaction, job and notification-based
cancellation, and `AgentEvent.Finished` as the completion signal. Filters enforce Plan-mode
read-only schemas and a 25-tool Chat allowlist.

**Critical exception:** `HmxTaskVerification.canClaimCompletion()` and `HmxApprovalPolicy`
are **test-only** — no runtime caller. Completion is **not** verification-gated at runtime.
See `HMXHARNESS_AI_AGENT_DEEP_ANALYSIS.md`.

## 5. Tool system

82 tools are registered at app startup (52 base + 30 conditional); ~85 in a Code run plus
N MCP tools; Chat advertises a fixed 25-tool allowlist. Families: file/patch/history,
shell/background/device-owner, env/diagnostics, Git/PR, chat-only, web/browser, memory/skills/
agents, settings/plugins/automation/code-intelligence, and dynamic MCP adapters. No
implemented tool is unregistered. Notable caveats: `http_request` declares read-only while
allowing mutating HTTP methods; `codegraph_sync` and `browser_get_logs(clear=true)` also have
side effects while marked read-only; `remember` is auto-approved in every mode. See
`HMXHARNESS_TOOL_SYSTEM_AUDIT.md`.

## 6. Providers and local models

Four wire protocols (OpenAI-compatible, Responses, Anthropic, Gemini), ChatGPT account
(OAuth PKCE), a built-in keyless relay over third-party endpoints, fallback and encrypted
key rotation — all runtime-wired. CPU GGUF is the shipped local path (llama.cpp pinned
`19cce691…`, downloads SHA-256 verified); GPU is experimental/default-off and 32-bit ABIs
receive stubs. No model training code exists. See
`HMXHARNESS_AI_PROVIDER_AND_LOCAL_MODEL_AUDIT.md`.

## 7. GitHub and cloud infrastructure

Device Flow uses only a public Client ID (no client secret), PAT fallback, encrypted token
storage, reviewed commits and a plain (never forced) push. Four workflows; CI installs the
pinned NDK, runs unit tests + debug lint + both APKs and uploads artifacts. Live evidence:
run `45338701934` success on `ae41ac7`; artifact `14898111209` (`hmxharness-apks`,
110,461,001 bytes). Earlier failure `45337564227` was `./gradlew: Permission denied`,
fixed by restoring Git mode `100755`. See
`HMXHARNESS_GITHUB_CLOUD_INFRASTRUCTURE_AUDIT.md`.

## 8. Security

4 High, 5 Medium and 6 Low findings: debug-signed release/updater trust, app-wide cleartext
with user-configurable `http://` endpoints, database inclusion in Android backups, plaintext
MCP credentials with fail-open HMAC, unredacted debug `error`, 6-character backup password,
exported debug activities, single-slot MCP OAuth state, optional update digest, and others.
Several controls were verified as sound (Keystore credentials, artifact/editor WebView
sandboxing, workspace path containment, MCP PKCE, URL allowlist). See
`HMXHARNESS_SECURITY_DEEP_AUDIT.md`.

## 9. Database and memory

Room v27 with 14 entities and 23 migrations; FTS4 search; bounded memory with scopes,
categories, consolidation and prompt recall; encrypted backup construction. Possible issues:
destructive fallback with migration path starting at v4, stale migration tests, missing
indices, non-atomic `clearMessages`, read-modify-write race, unbounded snapshots, partial
plaintext chat backup, and inconsistent encryption at rest. See
`HMXHARNESS_DATABASE_AND_MEMORY_AUDIT.md`.

## 10. UI/UX, performance, testing

Navigation is largely consistent but has duplicate back-stack entries and process-volatile
deep links. Chat uses keyed lazy lists with prefetch disabled (crash workaround). Strong
accessibility coverage (400 `contentDescription`s, 136 `semantics{}`) with sub-48dp targets
and missing `onClickLabel`s. Liquid Glass has three capability tiers over third-party
renderers; LOW still allocates full-screen capture layers; motion blur is well engineered.
The only animation path ignoring "remove animations" is the chat skeleton.

CI runs 2,478 unit tests, debug lint and APK assembly; it does **not** compile or run 555
instrumented tests, has no native tests, no pixel assertions, and applies a 3,930-issue
lint baseline. See `HMXHARNESS_UI_UX_AND_PERFORMANCE_AUDIT.md` and
`HMXHARNESS_TEST_COVERAGE_AUDIT.md`.

## 11. User-reported manual evidence

The developer reports successful physical-device use of AI chat, file creation/editing,
terminal commands and website preview, including a Student Attendance Management site with
date selector, counts, roster, attendance marking and add-student form. These are
**USER-REPORTED**; this audit did not reproduce them and does not infer untested features
from them. Source confirms the underlying capabilities.

## 12. Original contributions vs upstream/third-party

Inherited upstream AndroidHarness: agent loop, core tools, data layer, memory, subagents,
localization. HMXHARNESS additions: product identity, cloud CI workflows, `hmx/` verification
and approval objects (test/display-only), Connected Accounts card, lint baseline, adaptive
glass tier configuration and documentation. Third-party: llama.cpp, Shizuku/libsu/Dhizuku,
sora-editor, PrismalAGSL/Backdrop, OkHttp, BouncyCastle/Conscrypt, JSch, MCP libraries,
Termux packages. See `HMXHARNESS_ORIGINAL_CONTRIBUTIONS_AND_UPSTREAM_ANALYSIS.md`.

## 13. Expo readiness

Honest assessment: high technical complexity and a working integrated workflow, but
derivative innovation, unwired verification gate, debug-signed release, non-gating visual
tests and no independent hardware artifact. Strongest demo is the live on-device
chat→edit→terminal→preview loop. Full criteria, judge questions and safe answers:
`HMXHARNESS_PROJECT_EXPO_TECHNICAL_ASSESSMENT.md`; prioritised actions:
`HMXHARNESS_CRITICAL_FINDINGS_AND_RECOMMENDATIONS.md`.

## 14. Limitations of this audit

- No device/emulator, no Gradle execution, no instrumented test results.
- No live provider API calls, no live OAuth approval, no hardware performance measurement.
- Third-party dependency internals and bundled binary provenance were not exhaustively audited.
- ProGuard/R8 behaviour, Navigation back-stack persistence and runtime FGS-type consequences
  were not verified.
- Several documents cited as historical evidence (`docs/HMXHARNESS_FINAL_BUILD_REPORT.md`,
  `docs/HMXHARNESS_PROGRESS.md`, `docs/HMXHARNESS_VISUAL_VERIFICATION.md`) exist on disk but
  are **untracked** because of the `docs/*` ignore rule; they were preserved, not modified.
- "Implemented" means source and automated tests exist, not that the feature was observed working.
