# HMXHARNESS — Expo Master Factsheet

> One page to run the stall from. Everything here is traceable to a file in this
> repository or to a named CI run. Nothing here is aspirational.
>
> **Repository state:** `main` @ `22e674e9` application code (docs-only commits
> after it) · **CI:** green (runs `45204023911`,
> `45219214615`) · **APK:** verified artifact `hmxharness-apks` (id `14837630763`)

---

## 0. The 20-second pitch

> "HMXHARNESS is an open-source AI coding agent that runs **natively on an
> Android phone** — no PC, no server. It edits real files, runs real commands
> through Shizuku, root or a bundled Linux toolchain, signs in to GitHub with
> Device Flow so there's no client secret, and it refuses to claim a task is done
> unless a build or test actually passed. Built on the MIT-licensed AndroidHarness,
> extended here with a Liquid Glass design system, cloud CI and a verification
> gate."

---

## 1. Ten evidence-backed selling points

Each one is safe to say, and each has a file behind it.

| # | Selling point (say it like this) | Evidence |
|---:|---|---|
| 1 | **A coding agent that lives on the phone.** Single-activity Compose app; the agent loop runs on the device, not in a cloud sandbox. | `agent/AgentEngine.kt` (`run`, `executeCalls`), `MainActivity.kt`, `ui/AppNav.kt` |
| 2 | **It executes real commands, with privilege tiers.** Toybox → app Linux → Shizuku → root → Termux SSH, chosen at runtime. | `data/env/ShellTierRouter.kt`, `tools/ShellTool.kt`, `data/env/ShizukuManager.kt`, `RootManager.kt` |
| 3 | **A Linux toolchain on the phone.** Bash, Git, Python and Node installed from Termux packages with SHA-256 verification and mirror fallback. | `data/env/LinuxEnvironment.kt`, `data/env/PackageMirrors.kt` |
| 4 | **No authentication server.** GitHub OAuth **Device Flow**: the phone shows a code, you approve it on github.com; no client secret is shipped. (Flow inherited from upstream; HMX built the Settings card and CI wiring.) | `github/GitHubAuth.kt` (`/login/device/code`, `/login/oauth/access_token`, `offline_access` refresh), `ui/settings/ConnectedAccountsSection.kt` |
| 5 | **Verification-gated completion.** The agent's checks are recorded, and become `STALE` the moment a file changes again — so "done" requires fresh evidence. | `agent/VerificationEvidence.kt`, `hmx/HmxTaskVerification.kt`, `ui/chat/components/VerificationCard.kt` |
| 6 | **Approval policy for dangerous actions.** Four permission modes plus a shell denylist with variable/backtick expansion and workspace containment. | `PermissionMode` in `agent/AgentEngine.kt`, `tools/ShellPolicy.kt`, `workspace/WorkspaceFs.kt` |
| 7 | **Optional fully local AI.** llama.cpp compiled in via CMake, GGUF models run on-device; Vulkan on 64-bit ABIs. Smallest catalog model is ~100 MB. | `local/LocalNative.kt`, `app/src/main/cpp/local_model.cpp`, `CMakeLists.txt`, `local/LocalModelRecommendations.kt` |
| 8 | **A custom adaptive Liquid Glass material.** One visual identity, three GPU cost tiers selected from device capability. | `ui/liquidglass/GlassQuality.kt` (`GlassTier`, `profileFor`, `detectGlassTier`), `GlassTokens.kt`, `LiquidGlassModifiers.kt` |
| 9 | **A 21-language UI.** Arabic to Vietnamese, with tests guarding translations. | `app/src/main/res/values-*` (21 dirs), `strings.xml` (1,129 strings) |
| 10 | **Everything built and signed in the cloud.** Tests → lint → debug + release APKs → verified artifact, on every push. | `.github/workflows/android-ci.yml`; runs `45204023911`, `45219214615` |

**Scale footnote if asked "how big is it":** 732 Kotlin files / 158,276 lines in
`app/src/main/java`, 35,422 lines of tests, 2,478 unit tests in 351 files, 555
instrumentation tests, 23 Room migrations, 69 tool files.

---

## 2. Two-to-three minute live demo script

Assumes the APK artifact is installed and a provider key is already saved.
**Do not demo live GitHub OAuth approval** (needs a human and a configured
Device-Flow OAuth App) — show the screen, explain the flow, and cite the test.

| Time | Action | Say |
|---|---|---|
| 0:00 | Open app, Home | "This is a native Android app — one activity, Compose, no web wrapper. The agent runs here." |
| 0:15 | New **Code** chat, type: *"In this project, add a test for the slug function and run the unit tests."* | "Code mode can write. Chat has a fixed conversation-only tool set — no project files, no terminal, no device. Plan mode physically restricts the tool schemas to read-only." |
| 0:35 | Let it run; point at tool cards | "Every tool call is visible as a card — file reads, diffs, shell output. Nothing is hidden in a hidden process." |
| 1:00 | Approval prompt appears | "Destructive or privileged actions stop here and wait for me. Four modes: confirm all, confirm risky, full auto, full access — and full access is behind biometrics." |
| 1:20 | Tests run, Verification card appears | "This is the part I'd point judges at. The card says **PASSED**, not 'done'. If I edit a file again, the same check flips to **STALE**. The HMX gate refuses to claim completion on empty or stale evidence." |
| 1:45 | Open **Files** → show diff → Undo | "Atomic writes with before/after history, so an undo is real, not a re-prompt." |
| 2:00 | Settings → Connected accounts → GitHub | "Device Flow: I get a code, I approve on github.com, the token goes into Android Keystore-encrypted storage. There's no backend that ever sees my token, and no client secret in the build." |
| 2:15 | Settings → Local models | "Optional on-device inference — llama.cpp is compiled into the APK. This one model is a hundred megabytes and needs about two gigabytes of RAM." |
| 2:30 | Toggle dark/light, open drawer over content | "Liquid Glass: one design language, three cost tiers — it detects the device and drops expensive layers on low-end hardware." |
| 2:45 | Close: show CI run + artifact | "And all of it is built in the cloud: tests, lint, both APKs, uploaded as a verified artifact — that's this run." |

**If the phone/emulator is unavailable,** fall back to this order: (1) CI run and
APK artifact, (2) the 12 documented screenshots in
`docs/HMXHARNESS_VISUAL_VERIFICATION.md`, (3) the verification card + gates in
code. Say plainly that device execution was not possible in this environment.

---

## 3. Ten-minute talk outline

| Min | Section | Content | Must-mention evidence |
|---|---|---|---|
| 0–1 | Problem | You need a laptop to use a coding agent. Students and developers-on-the-go don't always have one. | `README.md` positioning |
| 1–2 | What it is | Native Android app, agent + chat, no PC, no root required (root/Shizuku optional). | `MainActivity.kt`, `app/build.gradle.kts` |
| 2–3 | **Honest provenance** | MIT derivative of AndroidHarness by Sanuu7: 828 of 961 commits are upstream. HMX adds rebrand identity, cloud CI, Liquid Glass system, verification gate + approval policy, the GitHub connection card, and two strings across the upstream 21 locales. | `LICENSE`, `README.md` "Attribution", `git shortlog`, `git log --author=HMX` |
| 3–5 | Architecture | One `AgentEngine` loop: context → provider stream → tool calls → permission → execute → results → verification → next round. Budgets, compaction, retries, subagents. | `AgentEngine.kt:463` loop, `executeCalls` at `:1112`, `ProviderFactory.create` |
| 5–6 | Device access | Tiered shell, Termux toolchain, real WebView browser automation, ADB pairing. | `ShellTierRouter.kt`, `LinuxEnvironment.kt`, `browser/BrowserController.kt` |
| 6–7 | **The differentiator** | Verification-gated completion + hybrid approval. Explain `STALE`. | `VerificationEvidence.kt`, `HmxTaskVerification.kt`, `HmxApprovalPolicy` |
| 7–8 | Model layer | Four wire protocols + ChatGPT + local GGUF via llama.cpp + fallback/rotation. No HMX-hosted model. | `llm/Llm.kt`, `local/LocalNative.kt`, `app/src/main/cpp/` |
| 8–9 | Design + i18n | Liquid Glass tokens/tiers, 21 locales, 1,129 strings. | `ui/liquidglass/`, `res/values-*` |
| 9–10 | Engineering + honesty | Branch consolidation by fast-forward, lint/resource recovery, cloud-only CI, and what is **not** verified (device behaviour, performance, live OAuth). | `docs/audits/*`, `docs/HMXHARNESS_PERFORMANCE_REPORT.md` |

---

## 4. Five judge questions (general) — with safe answers

**Q1. "Is this your own app, or a reskin of something?"**
> "It's an honest derivative. The upstream is AndroidHarness, MIT-licensed by
> Sanuu7 — 828 of our 961 commits are theirs. We kept the license and the credit.
> What we added is the identity/brand, the cloud-only CI that produces verified
> APKs, the Liquid Glass material system with quality tiers, the verification
> gate and approval policy in `hmx/`, the GitHub connection card plus CI
> client-ID injection, and two new strings across the upstream 21 locales. Two
> things we're careful *not* to claim: the agent engine and the GitHub Device
> Flow implementation are upstream code — we build on them, we didn't write them
> from scratch."

**Q2. "What problem does this actually solve that a laptop doesn't?"**
> "It removes the laptop requirement, not the laptop's advantages. It's for
> reading, editing, running checks and pushing from a phone — on a train, in a
> lab, on a low-end device. We do not claim it's better than a desktop IDE."

**Q3. "Where does the AI come from? Did you train a model?"**
> "No. Zero training code in this repo. Cloud models come from the provider the
> user configures — OpenAI-compatible, Responses, Anthropic or Gemini protocols.
> Locally, we compile llama.cpp and run GGUF models on-device. The built-in free
> option routes to third-party public gateways; it's not our model."

**Q4. "What happens if the AI does something destructive?"**
> "Three layers. Permission modes, with full access behind biometrics. A shell
> denylist that also expands `$(...)`, backticks and variables before judging, and
> canonicalizes paths so nothing escapes the workspace. And evidence-gated
> completion, so it can't report success it didn't verify. Plus file history for
> undo."

**Q5. "What isn't finished?"**
> "Three honest gaps. One: no physical device in this environment — `adb install`
> failed with `INTER_ACROSS_USERS_FULL`, so device features are implemented and
> compile-tested, not hardware-verified. Two: live GitHub Device Flow approval
> needs a human, so only the flow logic is unit-tested. Three: release APKs are
> signed with the debug key and cleartext traffic is globally enabled — both
> documented as findings, not fixed."

---

## 5. Five technical judge questions — with safe answers

**T1. "Walk me through your agent loop. Where's the state machine?"**
> "There isn't a separate state machine — it's a single coroutine loop at
> `AgentEngine.kt:463`. Each iteration: build/compact context against a
> `ContextBudget`, stream the provider over OkHttp SSE, parse `StreamEvent`s,
> execute tool calls, persist results in call order, emit an `AgentEvent`, repeat.
> Termination is `maxIterations` (a safety limit), a finished answer, or a task
> budget check. Subagents and read-only tools run in parallel; everything else is
> sequential."

**T2. "How do you stop the model from writing outside the project?"**
> "Two independent checks. `WorkspaceFs.resolve` canonicalizes and rejects any
> path whose canonical form doesn't start with the workspace root, and SAF paths
> reject `..`. Independently, `ShellPolicy` re-expands shell redirections,
> subshells and variables and applies the same containment test, plus a denylist
> and a system-path allowlist. The approval layer is a third gate, not the only
> one."

**T3. "What is 'STALE' and why does it matter?"**
> "`VerificationEvidence.collect` scans tool results for recognized build/test/
> lint commands and records `PASSED`/`FAILED`/`UNKNOWN` per command. A set of
> mutating tools — `write_file`, `edit_file`, `apply_patch`, `git_checkout`,
> `task`, and others — invalidates earlier passes, flipping them to `STALE`.
> `HmxTaskVerification.assess` then produces `VERIFIED / UNVERIFIED / FAILED /
> STALE / NO_CHECKS`, and `canClaimCompletion` gates the claim. The point is that
> 'it worked five edits ago' is not evidence."

**T4. "Why Device Flow instead of a normal OAuth redirect, and where's the token?"**
> "A redirect needs a client secret or a redirect URI a mobile app can't keep
> private; Device Flow needs neither. `GitHubAuth.startSignIn` posts to
> `/login/device/code`, shows `user_code` + `verification_uri`, and polls
> `/login/oauth/access_token` handling `authorization_pending`, `slow_down`,
> `expired_token` and `access_denied`. With `offline_access` we refresh instead of
> re-prompting. The token lands in `EncryptedSharedPreferences` with an Android
> Keystore `AES256_GCM` master key — `data/KeyStoreManager.kt`. PAT is the
> fallback. The live approval step is the one thing we couldn't automate. To be
> precise about authorship: the flow is upstream code; our contribution is the
> Settings connection card and injecting the Client ID at build time."

**T5. "What did you actually measure, and what's your frame-time?"**
> "We measured build and test outcomes, not runtime performance. Green CI: 2,478
> unit tests, 0 lint errors, both APKs, and an APK signature/ABI check
> (`com.hmx.harness` 4.0/40, four ABIs). We did **not** measure frame time or
> jank — `docs/HMXHARNESS_PERFORMANCE_REPORT.md` says so explicitly, and its
> glass design details are from an earlier iteration, so do not quote them as
> current. The shipped glass code has three quality tiers and real PrismalAGSL
> backdrop sampling; 'we designed for low-end devices' is not 'we measured it'."

---

## 6. Claims that must NOT be made

Say the left column → a judge can disprove it. Say the right column → you're safe.

| ❌ Never say | ✅ Say instead | Why |
|---|---|---|
| "We built this from scratch" | "MIT derivative of AndroidHarness; here's exactly what we added" | 828/961 commits are upstream |
| "We built / trained an AI model" | "We use pre-trained models via APIs and llama.cpp" | No training code exists |
| "First AI coding agent on Android" | "A native Android coding agent with a real shell and local models" | No market survey; upstream exists |
| "Fully offline AI coding" | "Optional local GGUF models; cloud is the default path" | Local needs a ~100 MB+ download and 2–4 GiB RAM |
| "Works on any Android phone" | "Android 8.0+, 64-bit for local inference; privileged shell needs Shizuku or root" | `minSdk 26`, ABI stubs, tiering |
| "Production-ready / Play-ready" | "Installable debug-signed release build" | Release uses the debug signing config |
| "Secure / no vulnerabilities" | "Documented findings: cleartext traffic (HIGH), plaintext MCP headers (MEDIUM)" | `docs/HMXHARNESS_SECURITY_AUDIT.md` |
| "We built Shizuku / Termux / llama.cpp / sora-editor / PrismalAGSL" | "We integrate them" | Third-party |
| "We wrote the GitHub Device Flow / the 21 translations" | "The flow and locales are upstream; we added the Settings card, CI wiring and two strings" | `2207914579`, `6243c2c7` are upstream commits |
| "Liquid Glass is our GPU renderer" | "Our token/tier system on top of PrismalAGSL + Backdrop" | Rendering primitives are third-party |
| "Faster than Claude Code / Cline / Aider" | "Different platform and constraints; no benchmark run" | Nothing measured |
| "GitHub OAuth verified end-to-end" | "Device Flow implemented and unit-tested; live approval needs a human" | Not performed |
| "Tested on a real device" | "Build- and unit-verified in cloud CI; device install blocked in this environment" | `adb install` failed |
| "Zero cost / free AI" | "No server bill; model usage is billed by your provider" | User-paid API |
| "Enterprise-grade / audited" | "Internally audited; findings documented" | No third-party audit |

---

## 7. Numbers card (answers to "how big / how many")

| Question | Answer | Source |
|---|---|---|
| Kotlin files (main) | 732 | `app/src/main/java` |
| Kotlin lines (main / tests) | 158,276 / 35,422 | counted |
| Unit tests / files | 2,478 / 351 | `app/src/test` |
| Instrumentation tests / files | 555 / 139 | `app/src/androidTest` |
| Native sources | 11 files, C++17 | `app/src/main/cpp/` |
| Room entities / schema / migrations | 14 / v27 / 23 | `data/db/Database.kt` |
| Locales / strings | 21 / 1,129 | `res/values-*`, `values/strings.xml` |
| Commits total / upstream / HMX | 961 / 828 / ~80 | `git shortlog` |
| History span | 2026-08-24 → 2026-11-23 | `git log` |
| Tool files | 69 | `tools/` |
| APK artifact size | 110,461,382 B (~105 MiB) | artifact `14837630763` |
| ABIs shipped | armeabi-v7a, arm64-v8a, x86, x86_64 | `app/build.gradle.kts` |
| CI duration (green) | ~28 min warm / ~53 min cold | runs `45219214615`, `45204023911` |
| Release APK signing | Android **debug** certificate | `app/build.gradle.kts` |
| minSdk / target / compile | 26 / 36 / 37 | `app/build.gradle.kts` |

---

## 8. If something breaks on the day

| Problem | Recovery |
|---|---|
| No network for cloud model | Switch to a saved local GGUF model, or narrate the verification card from `docs/HMXHARNESS_VISUAL_VERIFICATION.md` |
| App won't install on the demo phone | Use the CI artifact link + screenshots; say device install was blocked in the audit environment |
| Shell tier unavailable | Show `ShellTierRouter` fallback chain; run the toybox tier |
| GitHub sign-in needs approval | Do not attempt live; show `GitHubAuth.kt` + `GitHubAuthTest.kt` and explain Device Flow |
| Judge asks for a feature you can't find | Check `FEATURES.md` **and** the code; if absent from code, say "documented but not implemented as described" |

---

## 9. One-paragraph abstract (for the poster)

> HMXHARNESS is an open-source AI coding agent that runs natively on Android. A
> single Compose activity hosts a multi-round agent loop that reads and edits
> workspace files, executes commands through a tiered shell (Toybox, app Linux,
> Shizuku, root, or Termux SSH), drives a real WebView browser, and connects to
> cloud models over four wire protocols or to on-device GGUF models through
> llama.cpp. Credentials are sealed in Keystore-backed encrypted storage, and
> GitHub access uses OAuth Device Flow so no client secret is shipped. Its
> distinguishing design choice is evidence-gated completion: build and test
> results are recorded and invalidated when files change, so the agent cannot
> report success without fresh verification. The project is an MIT-licensed
> derivative of AndroidHarness, extended here with a custom adaptive Liquid
> Glass material system, a cloud-only CI pipeline that tests, lints and
> publishes verified APKs, and HMX-owned verification and approval policies.
> The upstream 21-language localization and the GitHub Device Flow
> implementation are inherited, not HMX-authored.
