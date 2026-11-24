# HMXHARNESS — Expo Master Factsheet

> One page to run the stall from. Everything here is traceable to a file in
> this repository or to a named CI run. Nothing here is aspirational.
>
> **Repository state:** `https://github.com/HMXPANEL/HMXHARNESS.git`, default
> branch `main`, HEAD `ae41ac7399d2bdfa6248cbf4c00d362451675990`
> ("Fix CI: restore executable bit on Gradle wrapper"). Fresh migration into a
> new empty repository; only two commits exist (`0d98456` import, `ae41ac7`
> wrapper fix). Old repository history was not carried over.
>
> **CI:** run `45338701934` on `ae41ac7` — **success**, all steps passed.
> **APK artifact:** `hmxharness-apks`, id `14898111209`, 110,461,001 bytes,
> not expired. Authenticated link:
> `https://api.github.com/repos/HMXPANEL/HMXHARNESS/actions/artifacts/14898111209/zip`.

Source paths below are relative to `app/src/main/java/com/hmx/harness/`.

---

## 0. The 20-second pitch

> "HMXHARNESS is an open-source AI coding agent that runs **natively on an
> Android phone** — no PC, no server. It edits real files, runs real commands
> through a tiered shell (Toybox, app Linux, Shizuku, root or Termux SSH),
> connects to cloud models or on-device GGUF, shows build/test evidence in chat,
> and signs in to GitHub with Device Flow so no client secret is shipped. It is
> an MIT-licensed derivative of AndroidHarness by Sanuu7, extended here with a
> Liquid Glass material system, cloud CI that produces real APKs, and HMX-owned
> verification-display objects. CPU local inference is shipped; GPU is opt-in
> experimental; device behaviour is implemented with tests but was not
> hardware-verified."

---

## 1. Ten evidence-backed selling points

| # | Say it like this | Evidence |
|---:|---|---|
| 1 | **A coding agent that lives on the phone.** Single-activity Compose app; the loop runs on the device, not in a cloud sandbox. | `MainActivity.kt`, `ui/AppNav.kt`, `agent/AgentEngine.kt` (`run` :296, loop :463) |
| 2 | **Real commands, with privilege tiers.** Toybox → app Linux → Shizuku → root → Termux SSH, chosen at runtime. | `data/env/ShellTierRouter.kt`, `tools/ShellTool.kt`, `tools/ShellPolicy.kt` |
| 3 | **A Linux toolchain on the phone.** Bash, Git, Python, Node from Termux packages with SHA-256 verification. | `data/env/LinuxEnvironment.kt`, `data/env/PackageMirrors.kt` |
| 4 | **No authentication server.** GitHub Device Flow needs only a public Client ID. The flow is inherited upstream; HMX added the Settings card and CI wiring. The variable is currently unset, so Device Flow is disabled in this artifact; PAT works. | `github/GitHubAuth.kt`, `ui/settings/ConnectedAccountsSection.kt`, `SECURITY.md` |
| 5 | **Visible, staleness-aware verification.** Real shell build/test/lint results are recorded and marked `STALE` after edits; the chat card shows the verdict. Do **not** say a gate blocks completion. | `agent/VerificationEvidence.kt`, `ui/chat/components/VerificationCard.kt:48` |
| 6 | **Approval for dangerous actions.** Permission modes, `RunManager.approve/deny` with notification actions, shell denylist and workspace containment. | `PermissionMode` in `agent/AgentEngine.kt:42`, `agent/RunManager.kt:1122`/`:1128`, `tools/ShellPolicy.kt`, `workspace/WorkspaceFs.kt` |
| 7 | **Optional local AI — CPU shipped, GPU experimental.** llama.cpp via CMake; GGUF on device; GPU requires `localModelExperimental = true`; 32-bit ABIs get stubs. | `local/LocalNative.kt`, `local/LocalModelProvider.kt`, `local/LocalAccelerationPolicy.kt`, `data/SettingsRepository.kt:234`, `app/src/main/cpp/CMakeLists.txt` |
| 8 | **A custom adaptive Liquid Glass material.** One identity, three GPU-cost tiers chosen from device capability; tier resolution unit-tested, **no gating pixel comparison**. | `ui/liquidglass/GlassQuality.kt`, `GlassTokens.kt`, `LiquidGlassModifiers.kt`, `HarnessGlassModeTest.kt` |
| 9 | **A 21-language UI.** 21 `values-*` locales plus base `values/` with 1,129 strings; locales inherited upstream. | `app/src/main/res/values-*`, `app/src/main/res/values/strings.xml` |
| 10 | **Cloud build produces real APKs.** Unit tests → lint → debug + release → verify → upload, green on `main`. | `.github/workflows/android-ci.yml`, run `45338701934`, artifact `14898111209` |

**Scale footnote ("how big is it?"):** 2,526 files on disk / 2,513 tracked;
1,231 Kotlin files (732 main, 352 unit tests, 140 instrumented); 5 Java; 12
C/C++/CMake files; 1,009 XML files; 2,478 unit `@Test` methods; 555
instrumented `@Test` methods; Room v27 with 14 entities and 23 migrations; 100
assets; 4 workflows; 3 README variants; 8 Expo documents.

**Feature-status footnote:** all **23 investigated features** have real source
implementations **and** automated test files. Separate three buckets when
speaking: (a) CI-verified unit tests/build/lint, (b) instrumented tests that
exist but are not run by CI, (c) not hardware-verified.

---

## 2. Two-to-three minute live demo script

Assumes an APK is installed and a provider key is saved. **Do not attempt live
GitHub Device Flow approval** — the Client-ID variable is unset and approval
needs a human. Show the screen and cite the unit tests instead.

| Time | Action | Say |
|---|---|---|
| 0:00 | Open app, Home | "Native Android app — one activity, Compose, no web wrapper. The agent loop runs here." |
| 0:15 | New **Code** chat: *"In this project, add a test for the slug function and run the unit tests."* | "Code mode can write. Chat uses a fixed conversation-only tool set. Plan mode restricts tool schemas to read-only." |
| 0:35 | Point at tool cards | "Every call is visible as a card — file reads, diffs, shell output." |
| 1:00 | Approval prompt | "Privileged actions stop here: permission modes, `RunManager` approval/deny and notification actions, plus a shell denylist and workspace containment." |
| 1:20 | Verification card | "The evidence card says **PASSED** or **STALE** — recorded by `VerificationEvidence`. Be precise: the HMX verdict/approval objects in `hmx/` are test-only and do not gate the loop." |
| 1:45 | Files → diff → Undo | "Atomic writes with before/after history." |
| 2:00 | Settings → Connected accounts → GitHub | "Device Flow: code on the phone, approval on github.com, token into Keystore-encrypted storage. The flow is upstream; HMX added this card and CI wiring. The Client ID is not configured here, so PAT is the working path today." |
| 2:15 | Settings → Local models | "CPU GGUF inference is shipped. GPU requires the experimental toggle and is not verified here." |
| 2:30 | Toggle dark/light, open drawer | "Liquid Glass: three detected tiers. Tier logic is unit-tested; screenshots exist but there is no gating pixel comparison." |
| 2:45 | Show CI run + artifact | "Tests, lint, both APKs and an uploaded artifact — that's run `45338701934`." |

**If no device/emulator is available:** show (1) the CI run and artifact,
(2) source code for `VerificationEvidence.kt` / `VerificationCard.kt`, and
(3) the feature matrix. Say plainly that no physical-device verification was
performed.

---

## 3. Ten-minute talk outline

| Min | Section | Content | Must-mention evidence |
|---|---|---|---|
| 0–1 | Problem | A coding agent normally assumes a laptop. | `README.md` positioning |
| 1–2 | What it is | Native Android app, agent + chat, no PC, no root required. | `MainActivity.kt`, `app/build.gradle.kts` |
| 2–3 | **Honest provenance** | MIT derivative of AndroidHarness by Sanuu7; upstream engine/tools/UI inherited; HMX adds identity, CI, Liquid Glass, verification-display objects, Settings card, docs. Fresh migration — no old Git history. | `LICENSE`, `README.md`, `ui/settings/SettingsScreen.kt` |
| 3–5 | Architecture | One `AgentEngine` loop: context → stream → tools → permission → results → verification evidence → next round. | `agent/AgentEngine.kt:463`, `:1112`, `llm/Llm.kt:208` |
| 5–6 | Device access | Tiered shell, Termux toolchain, WebView browser, ADB pairing. | `ShellTierRouter.kt`, `LinuxEnvironment.kt`, `browser/BrowserController.kt` |
| 6–7 | **Evidence, not a gate** | `STALE` semantics and the card; be explicit that HMX approval/completion objects are test-only. | `VerificationEvidence.kt`, `HmxTaskVerification.kt`, `HmxTaskVerificationTest.kt` |
| 7–8 | Model layer | Four wire protocols + ChatGPT + local GGUF; CPU shipped, GPU opt-in; no HMX-hosted model. | `llm/Llm.kt`, `local/LocalNative.kt`, `local/LocalAccelerationPolicy.kt` |
| 8–9 | Design + i18n | Liquid Glass tokens/tiers, 21 locales, 1,129 strings. | `ui/liquidglass/`, `app/src/main/res/values-*` |
| 9–10 | Engineering + honesty | Fresh migration, wrapper-bit CI fix, optional NDK, cloud-only CI, and what is **not** verified (hardware, performance, live OAuth, runtime gate, pixel comparison). | `.github/workflows/android-ci.yml`, `SECURITY.md`, this document set |

---

## 4. Five general judge questions — safe answers

**Q1. "Is this your own app, or a reskin?"**
> "It's an honest MIT derivative of AndroidHarness by Sanuu7. The license and
> upstream credit are preserved, and the About screen credits the upstream
> author. We added the identity, cloud CI, the Liquid Glass material system,
> verification-display objects, the GitHub Settings card and the documentation.
> This repository is a fresh migration, so we do not use old commit history as
> authorship evidence. We also don't claim every file is upstream or every file
> is ours — provenance is per component."

**Q2. "What problem does this solve that a laptop doesn't?"**
> "It removes the laptop requirement for a useful subset of work: read, edit,
> check and push from a phone. We do not claim it replaces a desktop IDE."

**Q3. "Where does the AI come from? Did you train a model?"**
> "No training code exists. Cloud models come from the provider the user
> configures — OpenAI-compatible, Responses, Anthropic or Gemini protocols.
> Locally we compile llama.cpp and run GGUF models on CPU; GPU is an opt-in
> experimental toggle. The built-in free option routes to third-party public
> gateways, not an HMX-hosted model."

**Q4. "What happens if the AI does something destructive?"**
> "Permission modes with full access behind biometrics, `RunManager`
> approval/deny and notification actions, a shell denylist that expands
> subshells and variables, workspace containment, and file history for undo.
> Be precise: the HMX verification/approval objects in `hmx/` are unit-tested
> but not wired into the runtime approval path."

**Q5. "What isn't finished?"**
> "No physical-device verification; live GitHub Device Flow approval needs a
> human and the Client-ID variable is unset; release APKs are debug-signed;
> cleartext traffic is enabled in the manifest; the HMX completion gate is
> test-only; and the glass screenshot test has no pixel assertions."

---

## 5. Five technical judge questions — safe answers

**T1. "Walk me through your agent loop."**
> "A single coroutine loop at `agent/AgentEngine.kt:463`. Each iteration builds
> or compacts context, streams the provider over OkHttp SSE, parses
> `StreamEvent`s, executes tool calls via `executeCalls` (`:1112`), persists
> results in call order and emits `AgentEvent`. Termination uses `maxIterations`
> (`:468`), a finished answer or a task budget. Read-only tools and subagents
> run in parallel; other tools run sequentially."

**T2. "How do you stop the model writing outside the project?"**
> "`workspace/WorkspaceFs.kt` canonicalises paths and rejects anything outside
> the workspace root. Independently `tools/ShellPolicy.kt` expands redirections,
> subshells and variables, applies a denylist and containment checks. Approval
> is a third gate via `PermissionMode`, `RunManager.approve/deny` and
> notification actions."

**T3. "What is STALE and why does it matter?"**
> "`agent/VerificationEvidence.kt` records `PASSED / FAILED / UNKNOWN` per
> recognised build/test/lint command. Later edits flip earlier passes to
> `STALE`. `ui/chat/components/VerificationCard.kt:48` calls
> `HmxTaskVerification.assess` to display a verdict. Important nuance:
> `canClaimCompletion()` and `HmxApprovalPolicy` are referenced only by
> `HmxTaskVerificationTest.kt`, so nothing in the agent loop is currently
> blocked by them — that is a gap, not a feature."

**T4. "Why Device Flow, and where is the token?"**
> "A redirect flow would need a client secret or a private redirect URI; Device
> Flow needs neither. `github/GitHubAuth.kt` posts to `/login/device/code`,
> shows the code and URL, polls `/login/oauth/access_token` handling pending,
> slow-down, expiry and denial, and refreshes via `offline_access`. Tokens go
> into `data/KeyStoreManager.kt` (`EncryptedSharedPreferences` with Keystore
> `AES256_GCM`). PAT is the fallback. The flow is upstream code; HMX added the
> Settings card and CI Client-ID injection. The variable is not set today, and
> no secret was introduced."

**T5. "What did you measure, and what's your frame-time?"**
> "We measured build and test outcomes: green CI with 2,478 unit tests,
> `lintDebug`, both APKs and artifact `14898111209` (110,461,001 bytes). Runner
> duration was not measured. Frame time and jank are unmeasured; no physical
> device was used. The shipped glass code has three tiers and PrismalAGSL
> backdrop sampling, but the instrumented screenshot test has no pixel
> assertions and logs failures instead of failing."

---

## 6. Claims that must NOT be made

| ❌ Never say | ✅ Say instead | Why |
|---|---|---|
| "We built this from scratch" | "MIT derivative of AndroidHarness; here is exactly what HMX added" | Upstream authorship and notices preserved in `LICENSE` / `README.md` |
| "All engine code is upstream" / "every file is ours" | "Provenance is per component" | Explicit headers are rare; some files are adapted or vendored |
| "We built / trained an AI model" | "Pre-trained models via APIs and llama.cpp" | No training code |
| "First AI coding agent on Android" | "A native Android coding agent with a real shell and local models" | No market survey; upstream exists |
| "Fully offline AI coding" | "Optional local GGUF (CPU); cloud is default; GPU opt-in experimental" | Model download and RAM required |
| "Works on any Android phone" | "Android 8.0+, 64-bit for local inference; privileged shell needs setup" | `minSdk 26`, ABI stubs, tiering |
| "Production-ready / Play-ready" | "Installable debug-signed alpha build" | Release uses the debug signing config |
| "Secure / no vulnerabilities / audited" | "Findings documented in `SECURITY.md`" | Cleartext traffic, plaintext MCP headers; no third-party audit |
| "HMX verification gate blocks completion" | "Evidence is recorded and shown; HMX gate objects are test-only" | No runtime caller for `canClaimCompletion()` / `HmxApprovalPolicy` |
| "Visual correctness is verified" | "Tier logic unit-tested; screenshots captured, no pixel assertions" | `LiquidGlassScreenshotTest.kt` suppresses failures |
| "Vulkan GPU inference shipped and verified" | "CPU GGUF shipped; GPU opt-in and hardware-unverified" | `localModelExperimental` default false |
| "Built Shizuku / Termux / llama.cpp / PrismalAGSL" | "Integrated; ADB files adapted from Shizuku (Apache 2.0)" | Third-party |
| "We wrote Device Flow / the 21 translations" | "Upstream flow and locales; HMX added the card, CI wiring and documentation" | Attribution in `LICENSE` / `README.md` |
| "Faster than Claude Code / Cline / Aider" | "Different platform and constraints; no benchmark" | Nothing measured |
| "GitHub OAuth verified end-to-end" | "Flow logic unit-tested; live approval not performed; variable unset" | Human step required |
| "Tested on a real device" | "CI-verified unit tests/build/lint; instrumented tests exist but are not run in CI" | No hardware verification |
| "Zero cost / free AI" | "No server bill; model usage billed by your provider" | User-paid API |
| "Enterprise-grade / third-party audited" | "Internal repository documentation only" | No audit |

---

## 7. Numbers card ("how big / how many?")

| Question | Answer | Source |
|---|---|---|
| Kotlin files (main / unit / instrumented) | 732 / 352 / 140 = 1,231 | `app/src/main`, `app/src/test`, `app/src/androidTest` |
| Java / C-C++-CMake / JS-MJS / Python / shell | 5 / 12 / 7 / 3 / 5 | repository measurement |
| XML files / assets | 1,009 / 100 | repository measurement |
| Unit / instrumented `@Test` methods | 2,478 / 555 | `app/src/test`, `app/src/androidTest` |
| Room entities / schema / migrations | 14 / v27 / 23 (+ separate debug-log database) | `data/db/Database.kt` |
| Locales / strings | 21 / 1,129 | `app/src/main/res/values-*`, `values/strings.xml` |
| Files on disk / tracked | 2,526 / 2,513 | repository measurement |
| Markdown on disk / tracked | 143 / 130 (8 in `docs/expo/`) | repository measurement |
| Images / workflows / README variants | 18 under `.github/assets` / 4 / 3 | repository measurement |
| CI artifact size | **110,461,001 bytes** | artifact `14898111209` |
| CI status | **success** | run `45338701934` on `ae41ac7` |
| Earlier CI failure | `45337564227` — `./gradlew: Permission denied` (exit 126); fixed by mode `100755` in `ae41ac7` | current repository history |
| ABIs shipped | armeabi-v7a, arm64-v8a, x86, x86_64 | `app/build.gradle.kts` |
| Release signing | Android **debug** config | `app/build.gradle.kts` |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 | `app/build.gradle.kts` |
| Version | 4.0 (40) | `app/build.gradle.kts` |

---

## 8. If something breaks on the day

| Problem | Recovery |
|---|---|
| No network for cloud model | Switch to a saved local GGUF model (CPU), or narrate `VerificationEvidence.kt` / `VerificationCard.kt` from source |
| App won't install | Use the CI artifact link; say no physical-device verification was performed in this environment |
| Shell tier unavailable | Show the `ShellTierRouter` fallback chain and the toybox tier; explain hardware dependence |
| GitHub sign-in needs approval or Client ID | Do not attempt live; show `GitHubAuth.kt` + `GitHubAuthTest.kt`, explain Device Flow, and note PAT sign-in still works |
| Judge asks about the HMX gate | Show `HmxTaskVerification.kt`, `HmxTaskVerificationTest.kt` and the card; say plainly it is test-only, with real approvals in `AgentEngine` / `RunManager` / `ShellPolicy` |
| Judge asks for GPU performance | Show `LocalAccelerationPolicy.kt`; say CPU is shipped, GPU is opt-in experimental, and nothing was benchmarked |
| Judge asks for a feature you can't find | Check source **and** tests; if absent from code, say "documented but not implemented as described" |

---

## 9. One-paragraph abstract (for the poster)

> HMXHARNESS is an open-source AI coding agent that runs natively on Android. A
> single Compose activity hosts a multi-round agent loop
> (`agent/AgentEngine.kt`) that reads and edits workspace files, executes
> commands through a tiered shell (`data/env/ShellTierRouter.kt`), drives a real
> WebView browser (`browser/BrowserController.kt`), and connects to cloud models
> over four wire protocols or to on-device GGUF models through llama.cpp.
> Credentials are sealed in Keystore-backed encrypted storage
> (`data/KeyStoreManager.kt`), and GitHub access uses OAuth Device Flow
> (`github/GitHubAuth.kt`) so no client secret is shipped. Its distinguishing
> design choice is **visible, staleness-aware evidence**: build and test results
> are recorded and invalidated when files change
> (`agent/VerificationEvidence.kt`, `ui/chat/components/VerificationCard.kt`).
> The HMX verdict and approval objects in `hmx/HmxTaskVerification.kt` exist and
> are unit-tested but are **not** wired into the runtime gate — that is an
> acknowledged gap. The project is an MIT-licensed derivative of AndroidHarness,
> extended with a custom adaptive Liquid Glass material, cloud CI producing real
> APKs (run `45338701934`, artifact `14898111209`), and HMX-owned documentation.
> The upstream Device Flow implementation and 21 locales are inherited, not
> HMX-authored; this is a fresh migration whose earlier Git history was not
> carried over. CPU local inference is shipped, GPU is opt-in experimental, and
> device-dependent behaviour is implemented with automated test coverage but
> was not hardware-verified.
