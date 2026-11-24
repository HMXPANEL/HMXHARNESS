# HMXHARNESS — Engineering Challenges and Costs

> Two rules govern this document:
> 1. **Costs are only reported where measured.** No dollar figure, pricing tier,
>    billing claim or invented retention period appears here. Where a cost is
>    user-specific (model API spending), the mechanism is described instead of a
>    number. CI runner time was **not measured** for this document.
> 2. **Challenges are the ones actually visible in the current repository.**
>    Historical commit hashes, old CI run IDs and old repository statistics are
>    intentionally absent: this repository is a fresh migration whose previous
>    Git history was not carried over.
>
> Paths are relative to `app/src/main/java/com/hmx/harness/` unless stated.

---

## Part A — Engineering challenges

### A1. Migrating into a brand-new repository without carrying history

**Problem.** The project tree was delivered as an extracted ZIP and imported
into a new empty GitHub repository (`HMXPANEL/HMXHARNESS`). Only two commits
exist: the migration import and the wrapper executable-bit fix (`ae41ac7`).
The previous repository's history, branch structure and CI records are absent;
it was left untouched as a backup.

**What was done.** Attribution was moved from commit history into durable
repository artifacts: `LICENSE` preserves the MIT text and
`Copyright (c) 2026 Sanuu7`; `README.md` has an explicit "License and legal
attribution" section; third-party notices live in
`app/src/main/assets/licenses/`, `app/src/main/assets/richtext/*/LICENSE` and
`app/src/main/assets/editor-tools/LICENSES.txt`; the About screen
(`ui/settings/SettingsScreen.kt`) credits `Sanuu` and links the upstream
author. Provenance is described per component, not as a blanket claim.

**Residual risk.** Per-file upstream headers are rare, so a reader cannot infer
authorship from most files. Do not claim "all engine code is upstream" or "all
code is HMX-original"; cite component-level notes such as the Shizuku adaptation
comments in `data/env/adb/`, the HMX patches in `data/env/CodeGraphPatches.kt`
and the Caveman notice in `app/src/main/assets/licenses/caveman.txt`.

### A2. CI failure caused by the extracted ZIP losing the executable bit

**Problem.** The first CI run on the new repository (`45337564227`) failed with
`./gradlew: Permission denied` (exit 126): ZIP extraction had dropped the
executable bit on `gradlew`.

**What was done.** Commit `ae41ac7` restored Git mode `100755` on `gradlew`.

**Verified outcome.** Run `45338701934` for `ae41ac7` completed with conclusion
**success**; wrapper validation, JDK 21 setup, pinned NDK install, unit tests,
`lintDebug`, `assembleDebug` + `assembleRelease`, APK verification and artifact
upload all passed. Artifact `hmxharness-apks` id `14898111209` is 110,461,001
bytes and not expired.

### A3. Native code must be optional for Kotlin-only builds

**Problem.** The app has a real native layer (`app/src/main/cpp/`, llama.cpp via
`FetchContent` in `CMakeLists.txt`), but requiring an NDK everywhere would make
local builds fragile.

**What was done.** `app/build.gradle.kts` computes `hasNdk` and omits the native
block when the pinned NDK (27.2.12479018) is absent. CI installs the NDK
explicitly (`.github/workflows/android-ci.yml`), so the cloud artifact includes
native code.

**Trade-off.** A no-NDK local build is not feature-identical to the CI artifact
for local inference. State this rather than implying parity.

### A4. No physical device available for verification

**Problem.** No device was available for installation or interactive testing in
this environment. Consequently every device-dependent behaviour — shell tiers,
Shizuku/root/ADB, local GGUF, voice, browser, Telegram, GitHub approval — is
**implemented with automated test coverage but not hardware-verified**.

**Consequence.** The feature matrix (`HMXHARNESS_FEATURE_VERIFICATION.md`)
separates three things that earlier documentation blurred:
(a) CI-verified (unit tests, lint, build), (b) instrumented tests that exist but
are **not run by CI**, and (c) hardware behaviour that was not exercised. All
23 investigated features have source implementations and automated test files;
none is merely planned.

### A5. A "verification gate" that exists in code but not in the runtime path

**Problem.** `hmx/HmxTaskVerification.kt` defines `HmxVerificationLevel`,
`HmxTaskVerification.assess`, `canClaimCompletion()` (`:47`) and
`HmxApprovalPolicy` (`:56`). A repo-wide search shows:

- `assess()` is used by `ui/chat/components/VerificationCard.kt:48` to compute
  the **displayed** verdict.
- `canClaimCompletion()` and `HmxApprovalPolicy` are referenced only by
  `app/src/test/java/com/hmx/harness/hmx/HmxTaskVerificationTest.kt`.

**What is true.** `agent/VerificationEvidence.kt` records real shell
build/test/lint results (`CheckStatus.PASSED/FAILED/UNKNOWN`) and flips earlier
passes to `STALE` after later edits; the card renders that state. Actual
approvals come from `PermissionMode` (`agent/AgentEngine.kt:42`),
`RunManager.approve/deny` (`agent/RunManager.kt:1122`, `:1128`) with notification
actions, plus `tools/ShellPolicy.kt` and `workspace/WorkspaceFs.kt`.

**Residual risk.** Do not market the HMX objects as an enforced completion gate.
List this as a gap and a next step; it is not a shipped differentiator.

### A6. Visual verification is not gated

**Problem.** `app/src/androidTest/java/com/hmx/harness/ui/liquidglass/LiquidGlassScreenshotTest.kt`
captures real screenshots but contains **no pixel assertions** and wraps screens
in `runCatching { }.onFailure { Log.w }`, so a missing or broken screen does not
fail the test. The visual-QA workflow captures emulator images; it is not a
comparison gate.

**What is verified.** Tier resolution is unit-tested in
`app/src/test/java/com/hmx/harness/ui/theme/HarnessGlassModeTest.kt`; tier logic
is in `ui/liquidglass/GlassQuality.kt` (`GlassTier.LOW/MID/HIGH`,
`detectGlassTier`, `profileFor`). Rendering primitives are third-party
(PrismalAGSL, kyant0 Backdrop), while the token/tier system is HMX code.

**Residual risk.** "Designed for low-end devices" is not "measured on a
low-end device". Frame time and jank are **unmeasured**.

### A7. Local GPU inference is opt-in and experimental

**Problem.** Running GGUF models on a phone is memory- and backend-sensitive;
enabling Vulkan by default would make failures hard to attribute.

**What was done.** CPU inference is the shipped path via `local/LocalNative.kt`
and `app/src/main/cpp/local_model.cpp`. GPU selection requires
`SettingsRepository.localModelExperimental = true`
(`data/SettingsRepository.kt:234`, default `false`) and the policy in
`local/LocalAccelerationPolicy.kt` (`useGpu(experimental, preference, available)`;
`retryOnCpu` for early GPU failures). `app/src/main/cpp/CMakeLists.txt` gives
32-bit ABIs `local_unavailable.cpp` stubs.

**Residual risk.** GPU behaviour is not hardware-verified; present it as
experimental, not as a shipped or verified capability.

### A8. GitHub sign-in without a client secret — and without a configured Client ID

**Problem.** Device Flow needs (a) an OAuth App with Device Flow enabled and
(b) a human to approve a code on `github.com`. Neither can be completed by an
automated build. In this repository the `HMX_GITHUB_CLIENT_ID` variable is
**not set**.

**What the code does.** `github/GitHubAuth.kt` implements the full flow
(`/login/device/code` → poll `/login/oauth/access_token` → handle
`authorization_pending`, `slow_down`, `expired_token`, `access_denied` → refresh
via `offline_access`) with PAT fallback. The flow is inherited upstream; HMX
added `ui/settings/ConnectedAccountsSection.kt` and CI Client-ID injection
(`.github/workflows/android-ci.yml`). CI warns and continues when the variable
is absent. No client secret was introduced. Logic is covered by
`app/src/test/java/com/hmx/harness/github/GitHubAuthTest.kt`; live approval was
not performed.

### A9. Privileged shell without a single API

**Problem.** "Run a real command on the phone" has no single API: root may not
exist, Shizuku needs one-time setup, and the app must still work with nothing
privileged available.

**What was done.** `data/env/ShellTierRouter.kt` selects an `ExecutionTier`
(`TERMUX_SSH`, `ROOT`, `PRIVILEGED` = Shizuku, `APP_LINUX`, `TOYBOX`);
`tools/ShellPolicy.kt` applies a denylist and canonical containment checks
before execution regardless of tier; `workspace/WorkspaceFs.kt` enforces the
workspace root.

**Residual risk.** More tiers mean more paths to test; the tier matrix is not
exhaustively covered by instrumented tests, and those tests are not run in CI.

### A10. Security debt documented, not hidden

| Finding | Evidence | Status |
|---|---|---|
| Cleartext traffic enabled globally | `app/src/main/AndroidManifest.xml:83` (`android:usesCleartextTraffic="true"`); `SECURITY.md` advises preferring HTTPS | Documented; do not claim "secure by default" |
| MCP static headers / env values stored in plaintext config (HMAC is integrity, not encryption) | `tools/mcp/McpManager.kt`, `SECURITY.md` | Documented |
| Release APK signed with the Android **debug** config | `app/build.gradle.kts` | Documented; not Play-ready |
| Workspace/shell checks are best-effort, not a complete sandbox | `SECURITY.md`, `workspace/WorkspaceFs.kt`, `tools/ShellPolicy.kt` | Documented |
| No guaranteed response time, remediation deadline or bug bounty | `SECURITY.md` | Documented |

No third-party audit exists. No analytics or crash SDK; provider calls go
directly from the device (`llm/Llm.kt`).

### A11. Maintaining a large derived codebase

| Metric | Value |
|---|---|
| Files on disk / Git-tracked | 2,526 / 2,513 |
| Markdown files on disk / tracked | 143 / 130 (8 are `docs/expo/`) |
| Kotlin files | 1,231 total (732 main, 352 unit tests, 140 instrumented tests) |
| Native sources | 12 C/C++/CMake files under `app/src/main/cpp/` |
| XML files / assets | 1,009 / 100 |
| Locales / strings | 21 `values-*` dirs plus base; 1,129 strings |
| Unit / instrumented `@Test` methods | 2,478 / 555 |
| Room | schema v27, 14 `@Entity`, 23 migrations (`data/db/Database.kt`) plus a separate debug-log database |
| Workflows / README variants | 4 / 3 |

The practical challenge is keeping a large derived codebase compiling,
lint-clean and test-green while preserving attribution and per-component
provenance.

---

## Part B — Costs (measured, not estimated)

### B1. What the project does not pay for

| Item | Reality | Evidence |
|---|---|---|
| Backend server | **None exists.** Provider requests go from the device to the configured endpoint. | `llm/Llm.kt`; no server module in `settings.gradle.kts` |
| Database hosting | **None.** Room is on-device. | `data/db/Database.kt` |
| Push / analytics / crash SDK | **None present.** | `app/build.gradle.kts`, `PRIVACY.md` |
| Model training | **None performed.** No training code exists. | `app/src/main/cpp/safetensors_import.cpp` is format conversion only |

### B2. Measured artifact and storage sizes

| Artifact | Size | Source |
|---|---|---|
| CI APK artifact `hmxharness-apks` (debug + release) | **110,461,001 bytes** (~105 MiB) | Actions artifact id `14898111209`, run `45338701934` |
| Recommended local model — SmolLM2 135M Q4_K_M | **105,454,432 bytes** | `local/LocalModelRecommendations.kt` |
| SmolLM2 360M Q4_K_M | **270,590,880 bytes** | same |
| Qwen 3 0.6B Q4_K_M | **484,220,320 bytes** | same |
| SmolLM2 1.7B Q4_K_M | **1,055,609,824 bytes** | same |

Local models cost **device storage, download bandwidth and battery**, not money
to HMXHARNESS; the smallest catalog model is still about 100 MB.

### B3. Cloud build cost

Measured facts only:

- CI runs on `ubuntu-latest` with JDK 21 and an explicitly installed pinned NDK;
  `timeout-minutes: 90` and concurrency cancellation are configured in
  `.github/workflows/android-ci.yml`.
- Run `45338701934` succeeded with all steps passing. **Runner duration was not
  measured for this document**, so no minute figure is reported here.
- Billing tiers and included minutes are account-specific; **no dollar amount,
  pricing claim or retention period is invented**.
- Failed runs cost time: the earlier run `45337564227` failed at the wrapper
  step and required the `ae41ac7` fix. Retrying builds is the real operational
  cost driver.

### B4. Runtime cost the user pays, and how the app exposes it

- **Cloud model usage** is charged by the user's chosen provider, not by
  HMXHARNESS. The app estimates spend locally from published list prices in
  `llm/ModelPrices.kt` (`costFor`), with metadata from `llm/ModelCatalog.kt`
  and `llm/ModelsDev.kt`.
- **Free path:** `llm/HarnessProvider.kt` routes to third-party public gateways.
  This is a third-party free tier, not an HMX-hosted model; it can be
  rate-limited or withdrawn and is not guaranteed free inference.
- **Keyless web search:** `tools/SearchProviders.kt` can scrape result pages
  without an API key, but is slower and can be blocked. Keyed providers are the
  user's own accounts.
- **GitHub Device Flow** requires no client secret; PAT sign-in remains
  available in the current artifact where the Client-ID variable is unset.

### B5. Hardware requirements implied by the code

| Requirement | Evidence |
|---|---|
| Android 8.0+ (`minSdk 26`) | `app/build.gradle.kts` |
| 64-bit ABI for local inference (`arm64-v8a` / `x86_64`); 32-bit gets stubs | `app/src/main/cpp/CMakeLists.txt`, `local_unavailable.cpp` |
| RAM for recommended local models (2 GiB for 135M, 3 GiB for Qwen3 0.6B, 4 GiB for 1.7B) | `local/LocalModelRecommendations.kt` (`LocalModelSpec.minimumRamGiB`) |
| Shizuku **or** root **or** Wireless ADB for privileged shell | `data/env/ShizukuManager.kt`, `RootManager.kt`, `data/env/adb/` |
| Network for cloud models, web search, GitHub | `llm/Llm.kt`, `tools/SearchProviders.kt`, `github/` |

### B6. What would cost money or effort to take this further (plan, not current cost)

1. **A release keystore** — required before any store distribution; today the
   release build uses the debug config (`app/build.gradle.kts`).
2. **A Play Console account** — a one-off fee if store distribution is wanted;
   not paid here.
3. **A paid model API budget** for reproducible benchmarks — variable and
   user-chosen; none is claimed.
4. **A physical test device or accelerated emulator** — needed for the
   unmeasured frame-time, local-inference and shell-tier results.
5. **Wiring the HMX gate into the runtime** — engineering work, not a licence
   or fee: `canClaimCompletion()` and `HmxApprovalPolicy` are currently
   test-only.
6. **Gating visual QA** — add pixel assertions to
   `LiquidGlassScreenshotTest.kt` instead of logging failures.

---

## Part C — The three sentences to say out loud

1. "There is no server bill, because there is no server: the agent, the database
   and the credentials all live on the phone."
2. "Our measured infrastructure footprint is a 110,461,001-byte CI artifact from
   a green build; runner time was not measured, and failed builds are what
   actually cost time."
3. "What we have **not** done is hardware verification, performance measurement,
   live GitHub approval, or runtime enforcement of the HMX verification gate —
   those are pending, not passed."
