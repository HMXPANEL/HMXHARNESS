# HMXHARNESS — Project Expo Technical Assessment

**Audit branch:** `audit/hmxharness-full-source-audit`
**Basis:** source inspection plus user-reported manual testing. No judging scores are
invented and no prize outcome is claimed. No device or emulator was used by this audit.

## 1. Qualitative assessment

| Criterion | Honest assessment | Evidence |
| --- | --- | --- |
| Problem relevance | Strong: phone-only users lack workspace, shell, diff and Git in one tool. | `README.md:44,56,66` |
| Innovation | Incremental/derivative. Novel integration and packaging, not a new agent algorithm; core loop/tools/memory/subagents are upstream. HMX additions are narrow. | `README.md:351-353`, `docs/expo/HMXHARNESS_CUSTOM_MODULES.md` §1-3 |
| Technical complexity | High as a system: 732 main Kotlin files, multi-tier shell, native JNI, MCP, PKCE, WebView automation, context compaction. Complexity is inherited + integrated, not wholly authored here. | `find app/src/main/java -name '*.kt'` = 732; `app/build.gradle.kts:48-176,200-276`; `ShellTierRouter.kt:26-131` |
| Implementation quality | Mixed: 2,478 unit and 555 instrumented `@Test` methods in-tree, lint baseline and CI builds; but release lint gate disabled, inherited debt baselined, and key gates unwired. | `app/build.gradle.kts:134-144`; `docs/expo/HMXHARNESS_FEATURE_VERIFICATION.md:28-32` |
| Working demo | **USER-REPORTED**: chat, file creation/editing, terminal commands and a Student Attendance Management website preview were manually exercised on a physical device. Not reproduced by this audit. | User report for this brief; `docs/expo/HMXHARNESS_FEATURE_VERIFICATION.md:30` |
| Reliability | Gate logic and tier resolution unit-tested; device behaviour not CI-verified; `HmxTaskVerification.canClaimCompletion()` has no runtime caller. | `docs/expo/HMXHARNESS_FEATURE_VERIFICATION.md:31,45,87-89`; `HmxTaskVerification.kt:47` |
| UX | Broad: Material theming, 21 locales/1,129 strings, editor tabs, diffs, chat cards. Visual pixel correctness unasserted. | `values/strings.xml` (1,129 strings); `FEATURES.md:97-101`; `LiquidGlassScreenshotTest.kt:61-62` |
| Security | Thoughtful containment (`ShellPolicy`, `WorkspaceFs`, Keystore, scoped permissions), but static MCP headers plaintext and debug-signed release. | `ShellPolicy.kt:17-40,713`; `WorkspaceFs.kt:95-101`; `app/build.gradle.kts:101-110` |
| Practical usefulness | High if the live demo works: code agent + editor + terminal + preview on a phone. Provider keys/gateways add caveats. | `README.md:78-79,196-204`; user report |
| Scalability | Honest limits: large-workspace repo maps are future work; one app process; `FULL_ACCESS` removes containment. | `README.md:343`; `AgentEngine.kt:42-45` |
| Future potential | Good: opt-in GPU, verification wiring and release signing are concrete next steps. | `README.md:339-343` |

## 2. Strongest features to demonstrate live

1. **End-to-end on-device coding loop.** Prompt → agent edits files → diff/undo →
   terminal command → web preview. Evidence: `AgentEngine.kt:296,463`;
   `Tool.kt:108-165`; `FileTools.kt`/`PatchTools.kt`; `RunManager.kt:1122,1128`.
2. **Tiered shell with visible fallback.** Show the status/doctor card and a running
   command; explain `APP_LINUX → TOYBOX`. Evidence: `ShellTierRouter.kt:26-33,70-79`;
   `LinuxEnvironment.kt:1659-1723`; `Tool.kt:120-122`.
3. **Editor + file-history undo.** Evidence: `FEATURES.md:78,97-101`;
   `tools/FileHistoryTools.kt`; `app/build.gradle.kts:265`.
4. **GitHub Device Flow UI (state, not live auth).** Evidence:
   `ui/settings/ConnectedAccountsSection.kt`; `GitHubAuth.kt:135-160`; `README.md:283`.
5. **Cloud CI as an engineering artifact.** Evidence: `.github/workflows/android-ci.yml:61-97`;
   artifact `hmxharness-apks` (`:95`).
6. **Adaptive glass tier switching** on two devices/profiles if available. Evidence:
   `GlassQuality.kt:39-80`.
7. **Optional CPU GGUF.** Evidence: `LocalNative.kt:5-8`;
   `LocalAccelerationPolicy.kt:11-12`; `SettingsRepository.kt:234`.

## 3. Features to avoid demonstrating or describe cautiously

| Feature | Why | Evidence |
| --- | --- | --- |
| "HMX verification gate blocks completion" | `canClaimCompletion()`/`HmxApprovalPolicy` are unit-test-only; only `assess()` renders a card. | `HmxTaskVerification.kt:47-81`; `VerificationCard.kt:48`; `docs/expo/HMXHARNESS_FEATURE_VERIFICATION.md:31,45` |
| "GPU acceleration verified" | Opt-in experimental, default off, separate hash-pinned package, 32-bit stubs, no benchmark. | `LocalAccelerationPolicy.kt:11-12`; `SettingsRepository.kt:234`; `app/build.gradle.kts:167-168,280-324`; `gpu_unavailable.cpp:1-3` |
| Live GitHub/MCP OAuth | Needs a human; Client ID absent in current artifact disables Device Flow; PAT fallback. | `README.md:283`; `GitHubAuth.kt:141,191` |
| Glass visual correctness from screenshots | Screenshot test has no pixel assertions and suppresses failures. | `LiquidGlassScreenshotTest.kt:40,61-62`; `android-visual-qa.yml:109,137` |
| "Production-ready release" | Debug-signed; release lint gate disabled; inherited debt baselined. | `app/build.gradle.kts:101-110,134-143` |
| Device-dependent tiers as "tested" | Instrumented tests not in CI; no independent hardware verification here. | `docs/expo/HMXHARNESS_FEATURE_VERIFICATION.md:29-30,87-89` |
| "Fully offline AI" | Cloud is default; local needs a download; only CPU GGUF can be offline. | `README.md:196-204,325,327` |

## 4. Weaknesses judges may identify

1. **Attribution vs marketing gap** — any slide implying wholly original work is
   contradicted by `README.md:46,351`.
2. **Unwired "gate"** — `HmxTaskVerification.kt:47-81` is not in the runtime path;
   there is an internal contradiction between `docs/HMXHARNESS_FINAL_BUILD_REPORT.md:29`
   and `docs/audits/HMXHARNESS_SECOND_FULL_REPOSITORY_AUDIT.md:77`.
3. **No hardware-verification artifact** for shell tiers, GGUF, voice, browser or
   Shizuku; USER-REPORTED testing helps the interview but is not inspectable evidence.
4. **Release signing and disabled lint gate** (`app/build.gradle.kts:106,139-143`).
5. **Plaintext static MCP headers** and keyless third-party gateways.
6. **Visual QA is non-gating screenshot capture**, not pixel assertion.
7. **Complexity ownership** — be ready with the provenance table in the companion
   upstream-analysis report.
8. **Contradictory historical docs** — e.g. `docs/HMXHARNESS_PROGRESS.md:19` marks the
   gate work "COMPLETED" while the later audit calls it a GAP.

## 5. Likely judge questions — evidence-backed answers

1. **"Is this your own engine?"** — "The loop in `AgentEngine.kt:296,463` and `RunManager.kt`
   is inherited upstream AndroidHarness code; we are an MIT derivative (`LICENSE:1-15`,
   `README.md:351`). Our contributions are `README.md:352`."
2. **"Web wrapper or server?"** — "No. Single native activity
   `AndroidManifest.xml:114-131`, Compose via `MainActivity.kt:98`, no project backend
   (`README.md:161,302`)."
3. **"What did you add?"** — "Verification verdict/approval objects
   (`hmx/HmxTaskVerification.kt:14-81`), Connected Accounts card, four CI workflows,
   lint baseline, identity and docs (`README.md:352`)."
4. **"Does the gate really block bad completions?"** — "No — `assess()` renders a card
   (`VerificationCard.kt:48`); `canClaimCompletion()` and `HmxApprovalPolicy` are
   test-only. Real approval is `PermissionMode` (`AgentEngine.kt:42`),
   `RunManager.approve/deny` (`:1122,:1128`) and `ShellPolicy.kt:17-40`."
5. **"Shell without root?"** — "`ShellTierRouter.kt:26-33,70-79` picks app-LINUX or
   TOYBOX; privileged options use Shizuku/WADB/Dhizuku (`AndroidManifest.xml:89-103`).
   ADB transport was adapted from Shizuku under Apache 2.0 (`AdbClient.kt:25`)."
6. **"Can it delete arbitrary files?"** — "Default containment: `ShellPolicy.kt:17-40,713`
   and `WorkspaceFs.kt:95-101`. `FULL_ACCESS` removes layers (`AgentEngine.kt:42-45`);
   we would not demo it."
7. **"Is local AI real?"** — "CPU GGUF is shipped: `LocalNative.kt:5-8`, llama.cpp pinned
   in `CMakeLists.txt:85-90`. GPU is opt-in experimental (`LocalAccelerationPolicy.kt:11-12`,
   default false at `SettingsRepository.kt:234`); 32-bit ABIs get a stub."
8. **"Why is GPU a separate download?"** — "Excluded from base APK, delivered as a
   SHA-256-pinned package or Play feature (`app/build.gradle.kts:167-168,280-324`)."
9. **"How does MCP OAuth work?"** — "OAuth 2.1 + PKCE, RFC discovery, custom-scheme
   redirect (`McpOAuth.kt:63-79,191`; `AndroidManifest.xml:124-130`); live consent needs
   a human."
10. **"Does browser automation drive a page?"** — "Real WebView tools
    (`BrowserTools.kt:71,108,146,429,466`; `BrowserController.kt:167-175`). Instrumented
    tests exist but CI doesn't run them — demo live."
11. **"What proves it builds?"** — "Four workflows; CI runs unit tests, lint and both
    APKs (`.github/workflows/android-ci.yml:61-97`). 2,478 unit and 555 instrumented
    `@Test` methods in-tree; instrumented not executed by CI."
12. **"Are glass effects yours?"** — "Tier logic/tokens are HMX (`GlassQuality.kt:17,39-80`);
    rendering uses PrismalAGSL + Backdrop (`app/build.gradle.kts:266-269`). Screenshots
    have no pixel assertions."
13. **"Store-ready?"** — "No — debug-signed alpha (`app/build.gradle.kts:101-110`, `:106`);
    lint vital gate disabled."
14. **"How tested?"** — "CI: unit tests/build/lint. Instrumented tests exist but not in CI.
    Separately **USER-REPORTED**: manual physical-device exercise of chat, file
    editing, terminal and Student Attendance Management website preview."

## 6. Recommended critical fixes before the Expo (not implemented)

- **P0:** wire or rename the "verification gate"; rehearse one complete live demo;
  prepare a one-slide provenance table.
- **P1:** re-enable or document the disabled release lint gate; add a real pixel
  assertion or stop presenting screenshots as proof; provide a release signing config
  or label the APK a debug-signed alpha; move static MCP secrets off plaintext or
  state the limitation prominently.
- **P2:** run one instrumented smoke test on a device for shell tier and one CPU GGUF
  inference; set `HMX_GITHUB_CLIENT_ID` if Device Flow is to be shown; freeze the demo
  configuration (disable `FULL_ACCESS` and experimental GPU).

No judging score or prize outcome is predicted. All claims above are evidence-linked;
anything not reproduced by this audit is labelled USER-REPORTED or code-inspected only.
