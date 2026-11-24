# HMXHARNESS — Judge Question Preparation

Fourteen evidence-backed questions from
`docs/expo/presentation/HMXHARNESS_PRESENTATION_RESEARCH.md` §20, expanded with short answers,
technical explanations, supporting evidence and the relevant source file. Preserve the honest
tone — do not strengthen a claim beyond the evidence.

---

### Q1. Is the AI agent engine original?

**Short answer:** No. HMXHARNESS is an MIT derivative of AndroidHarness; the engine is inherited and extended.

**Technical explanation:** The loop, tool registry, memory and subagent infrastructure are preserved upstream components. HMX additions are the product identity, cloud CI, `hmx/` verdict objects, Connected Accounts card, glass tier configuration and documentation.

**Evidence:** `README.md:351-353`; `LICENSE:1-15` (`Copyright (c) 2026 Sanuu7`); `hmx/HmxTaskVerification.kt:9` acknowledges upstream verification evidence.

**Source file:** `app/src/main/java/com/hmx/harness/agent/AgentEngine.kt:296,463`; `docs/audits/full-source-code/HMXHARNESS_ORIGINAL_CONTRIBUTIONS_AND_UPSTREAM_ANALYSIS.md`

### Q2. What did HMXHARNESS actually contribute?

**Short answer:** Identity, CI, the `hmx/` verdict/approval objects, Connected Accounts card, lint baseline, glass tiers and documentation.

**Technical explanation:** These are real changes but narrow relative to the ~1,231 Kotlin files. The verdict/approval objects are test/display-only, not an enforced runtime gate.

**Evidence:** `README.md:352`; `.github/workflows/android-ci.yml`; `app/lint-baseline.xml`; `ui/liquidglass/GlassQuality.kt:17,39-80`.

**Source file:** `docs/audits/full-source-code/HMXHARNESS_ORIGINAL_CONTRIBUTIONS_AND_UPSTREAM_ANALYSIS.md:49-64`

### Q3. Does it require root?

**Short answer:** No. Root is optional.

**Technical explanation:** `ShellTierRouter` picks app-Linux or toybox for app-owned paths; privileged tiers use Shizuku or Wireless ADB; ROOT needs a verified backend and per-project permission with no silent fallback.

**Evidence:** `data/env/ShellTierRouter.kt:25-37,68-82`; `data/env/DeviceAccessPolicy.kt:51-104`; `tools/ShellTool.kt:33,129-134`.

**Source file:** `app/src/main/java/com/hmx/harness/data/env/ShellTierRouter.kt`

### Q4. Is local AI really supported?

**Short answer:** Yes for CPU GGUF; GPU is experimental and opt-in.

**Technical explanation:** `LocalNative` loads `libharness_local` and `libggml-cpu.so` on 64-bit ABIs; llama.cpp is pinned in CMake with SHA-256; downloads verify size, GGUF magic and checksum. GPU requires `localModelExperimental = true` and a detected Vulkan device; 32-bit ABIs get stubs. No training code exists.

**Evidence:** `local/LocalNative.kt:5-10`; `app/src/main/cpp/CMakeLists.txt:84-90`; `data/SettingsRepository.kt:234`; `local/LocalAccelerationPolicy.kt:11-12`; `local/LocalModelStore.kt:145-173`.

**Source file:** `app/src/main/java/com/hmx/harness/local/LocalNative.kt`

### Q5. Does it use a cloud server?

**Short answer:** No project-owned backend. Provider calls go directly from the device to endpoints the user configures.

**Technical explanation:** Single activity, no server module; WebView asset loader serves workspace files without sockets. GitHub and model endpoints are user-configured external services.

**Evidence:** `AndroidManifest.xml:114-131`; `README.md:161,302`; `app/build.gradle.kts:234-236`; `browser/WorkspacePathHandler.kt:8-19`.

**Source file:** `docs/audits/full-source-code/HMXHARNESS_COMPLETE_SOURCE_AUDIT.md:39-48`

### Q6. How are shell commands controlled?

**Short answer:** Permission modes, a shell denylist and workspace path containment.

**Technical explanation:** `PermissionMode` has four values; `ShellPolicy` rejects dangerous command shapes and checks expanded paths; `WorkspaceFs` canonicalizes and rejects escapes. FULL_ACCESS deliberately bypasses the denylist — we would not demo it. These are application-level checks, not an OS sandbox.

**Evidence:** `agent/AgentEngine.kt:42-48,1755-1830`; `tools/ShellPolicy.kt:17-58,289-341`; `workspace/WorkspaceFs.kt:95-101`.

**Source file:** `app/src/main/java/com/hmx/harness/tools/ShellPolicy.kt`

### Q7. Is the verification gate enforced?

**Short answer:** No — be precise. Evidence is recorded and displayed; the HMX gate objects are test-only.

**Technical explanation:** `HmxTaskVerification.assess()` renders the card via `VerificationCard.kt:48`. `canClaimCompletion()` and `HmxApprovalPolicy` have **no runtime caller** (only `HmxTaskVerificationTest.kt`). Completion is `AgentEvent.Finished` → `RunManager.finishedNormally`. Real approval is `PermissionMode` / `RunManager.approve/deny` / `ShellPolicy`.

**Evidence:** `hmx/HmxTaskVerification.kt:47-81`; `docs/expo/HMXHARNESS_FEATURE_VERIFICATION.md:31,45`; `agent/AgentEngine.kt:244,837`; `agent/RunManager.kt:715,737`.

**Source file:** `app/src/main/java/com/hmx/harness/hmx/HmxTaskVerification.kt`

### Q8. Is the release production-ready?

**Short answer:** No — it is a debug-signed alpha APK.

**Technical explanation:** `release.signingConfig = signingConfigs.getByName("debug")`, `checkReleaseBuilds = false`, and the lint baseline freezes inherited findings. The in-app updater trusts a matching signer, which is weak with a debug key.

**Evidence:** `app/build.gradle.kts:101-106,134-143`; `data/update/UpdateManager.kt:196-216`; `docs/audits/full-source-code/HMXHARNESS_SECURITY_DEEP_AUDIT.md:16-24`.

**Source file:** `app/build.gradle.kts`

### Q9. How is user data handled?

**Short answer:** Credentials in Keystore-backed encrypted preferences; no analytics/crash SDK; cloud requests send prompts and context off-device.

**Technical explanation:** `KeyStoreManager` uses `EncryptedSharedPreferences` with AES256_GCM master key. Provider requests include prompts, system context, images and possibly file contents/command output. GitHub tokens go only to GitHub. Local GGUF is the only offline path. Chat DB is not excluded from Android backup — a disclosed finding.

**Evidence:** `data/KeyStoreManager.kt:9-42`; `llm/OpenAiCompatProvider.kt:448-462`; `github/GitHubAuth.kt:264-284`; `res/xml/backup_rules.xml:12-22`; `PRIVACY.md` (22 sections).

**Source file:** `docs/audits/full-source-code/HMXHARNESS_SECURITY_DEEP_AUDIT.md:34-42`

### Q10. What happens if the AI provider fails?

**Short answer:** Retry, then fall back to another configured target; the UI says what happened.

**Technical explanation:** `StreamRetrier` + `RetryPolicy` (3 retries, 1/2/4 s backoff) handle transient failures; `FallbackProvider` advances through up to 4 targets with fresh credentials, emits `ProviderChanged` and a `Retrying` event. SSE read timeout is 0 with stall detection; collector cancellation cancels the HTTP call.

**Evidence:** `agent/StreamRetrier.kt:213-273`; `agent/RetryPolicy.kt:11-50`; `llm/FallbackProvider.kt:47-185`; `llm/Llm.kt:211-219,245-305`.

**Source file:** `app/src/main/java/com/hmx/harness/llm/FallbackProvider.kt`

### Q11. Does it really drive a web page?

**Short answer:** Yes — real WebView tools, with instrumented tests that CI does not run.

**Technical explanation:** `BrowserController` navigates, clicks, types, evaluates JS and screenshots; 14 browser tools are registered. Workspace preview uses `WebViewAssetLoader` at a synthetic origin with static file serving only.

**Evidence:** `browser/BrowserController.kt:167-175`; `tools/BrowserTools.kt:68-649`; `tools/Tool.kt:196-215`; `android-ci.yml:61-76`.

**Source file:** `app/src/main/java/com/hmx/harness/browser/BrowserController.kt`

### Q12. Are the glass effects yours?

**Short answer:** The tier logic and tokens are HMX; the rendering primitives are third-party.

**Technical explanation:** `GlassQuality.detectGlassTier` selects LOW/MID/HIGH from heap, cores, low-RAM flag, GLES/Vulkan and SDK; PrismalAGSL and kyant0 Backdrop do the rendering. Screenshots are non-gating: no pixel assertions and failures are logged as warnings.

**Evidence:** `ui/liquidglass/GlassQuality.kt:17,39-80`; `app/build.gradle.kts:266-269`; `androidTest/.../LiquidGlassScreenshotTest.kt:59-63`.

**Source file:** `app/src/main/java/com/hmx/harness/ui/liquidglass/GlassQuality.kt`

### Q13. Was it tested on a real device?

**Short answer:** The developer reports manual device testing and supplied screenshots of the attendance demo; the audits ran no device.

**Technical explanation:** CI runs unit tests, debug lint and APK assembly; 555 instrumented tests exist but are not compiled or executed by CI. Hardware behaviour (shell tiers, GGUF, voice, browser) is not independently verified.

**Evidence:** `docs/expo/presentation/HMXHARNESS_PRESENTATION_RESEARCH.md:13`; `android-ci.yml:61-76`; `docs/audits/full-source-code/HMXHARNESS_TEST_COVERAGE_AUDIT.md:75-89`.

**Source file:** `docs/audits/full-source-code/HMXHARNESS_COMPLETE_SOURCE_AUDIT.md:126-132`

### Q14. How do you prove the build works?

**Short answer:** A successful GitHub Actions run and a verified APK artifact.

**Technical explanation:** Run `45338701934` on commit `ae41ac7` succeeded: wrapper validation, JDK 21, NDK install, unit tests, debug lint, debug + release APK, verification and artifact upload. Artifact `14898111209` is 110,461,001 bytes and non-expired. An earlier run failed with `./gradlew: Permission denied` and was fixed by restoring Git mode `100755`.

**Evidence:** public GitHub API verification recorded in `HMXHARNESS_GITHUB_CLOUD_INFRASTRUCTURE_AUDIT.md:92-103`; `app/build.gradle.kts:101-106`.

**Source file:** `.github/workflows/android-ci.yml:61-98`

---

## Rules during the Q&A

- If a question asks about an unverified feature, say "implemented with tests, not hardware-verified".
- Never imply Play Store readiness, a server backend, an enforced gate, or original authorship of inherited code.
- If pressed on security, cite the four High findings and the remediation order in `HMXHARNESS_CRITICAL_FINDINGS_AND_RECOMMENDATIONS.md`.
