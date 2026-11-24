# HMXHARNESS — Engineering Challenges and Costs

> Two rules govern this document:
> 1. **Costs are only reported where measured.** No dollar figure, pricing tier or
>    cloud bill is estimated or invented. Where a cost is user-specific (model API
>    spending), the mechanism is described instead of a number.
> 2. **Challenges are the ones actually encountered** while building and auditing
>    this repository, cross-referenced to the reports in `docs/audits/`.

---

## Part A — Engineering challenges

### A1. Consolidating a divergent branch history without losing work

**Problem.** Work was spread across `glass-ui`, `glass-final`, `github`,
`feature/hmxharness-transformation` and `lint-baseline-gen`, with `main` behind.

**What was done.** Inventoried every branch by commit, proved that
`feature/hmxharness-transformation` (`bbd2f8a1`) and `lint-baseline-gen`
(`5e418195`) were **ancestors** of `main` (already merged, so re-merging would
create noise), then confirmed the remaining chain was linear —
`main → glass-ui (5fa33b2b) → glass-final (1719471d) → github (32127b31)` — and
**fast-forwarded** `main` instead of cherry-picking. No force-push, no branch
deletion.

**Residual risk.** Fast-forwarding keeps history truthful but means the
consolidated tree carries every intermediate commit, including a failed lint
state; that is documented rather than rewritten.

*Evidence:* `docs/audits/HMXHARNESS_BRANCH_CONSOLIDATION_REPORT.md`,
`docs/audits/HMXHARNESS_INTEGRATION_CHANGELOG.md`.

### A2. Rebranching a 961-commit MIT project without breaking attribution

**Problem.** The app had to be rebranded from `com.android.harness` to
`com.hmx.harness` across ~1,200 Kotlin files, the manifest, resources, deep-link
scheme and authorities — while remaining a legally clean MIT derivative.

**What was done.** A single rebrand commit (`2cb00a3d`) plus a follow-up fixing
four unit tests whose expectations encoded the old package name. `LICENSE`
retains `Copyright (c) 2026 Sanuu7`; `README.md` has an explicit "Attribution and
upstream" section; `SECURITY.md`/`CONTRIBUTING.md`/issue templates were updated
rather than deleted.

**Residual risk.** `git shortlog` still shows 828 of 961 commits by upstream
author `Sanuu` — this is a feature of honest history, and the Expo documents
state it openly instead of implying original authorship of the whole codebase.

### A3. Android resource merge failing on leftover `.bak` files

**Problem.** CI failed because `*.xml.bak` files sat inside `res/`, and the
Android resource merger rejects non-`.xml` files in resource directories.

**What was done.** Deleted the stray backups, added `*.bak` to `.gitignore`, and
removed the duplicate `settings_page_accounts_desc` string that had been
introduced twice (`f1f2e7b8`, `0c833e64`).

**Lesson worth presenting.** A build failure that looks like "Gradle is broken"
turned out to be a file-hygiene issue in `res/`; the fix was three small commits,
not a toolchain change.

*Evidence:* `docs/audits/HMXHARNESS_BUILD_RECOVERY_REPORT.md`.

### A4. Lint failures caused by using `LocalContext.getString` in Compose

**Problem.** `lintDebug` failed on `LocalContext.current.getString(...)` inside
composables (localization/lint rules), blocking the release build.

**What was done.** Read strings with `stringResource(...)` at composition time and
`String.format` for arguments (`32127b31`), rather than calling `getString` in
composition. 0 lint errors afterwards, with the pre-existing lint **baseline**
left in place.

### A5. Native build fragility: NDK present vs. absent

**Problem.** The app has a real native layer (`app/src/main/cpp/`, llama.cpp via
`FetchContent`, Vulkan on 64-bit ABIs), but requiring an NDK on every machine
would make the project unbuildable on a phone-only workflow.

**What was done.** `app/build.gradle.kts` computes `hasNdk` and **omits the
native block** when the pinned NDK (27.2.12479018) is not installed, so a
Kotlin-only build still produces a working APK; CI installs the NDK explicitly,
so cloud builds do include native code.

**Trade-off to state honestly.** A no-NDK local build is not feature-identical to
the CI artifact for local inference.

### A6. No physical device available for verification

**Problem.** `adb install` on the available phone failed with
`INTER_ACROSS_USERS_FULL` / an AppOps `NullPointerException`, so the APK could
not be installed and device behaviours could not be executed.

**Consequence for this audit.** Every device-dependent feature is labelled
**implemented / not verified on hardware** (`HMXHARNESS_FEATURE_VERIFICATION.md`).
This is the single largest honesty constraint in the project, and it is stated
rather than hidden.

### A7. Emulator instability in the cloud visual-QA workflow

**Problem.** The Ubuntu-hosted emulator runs on SwiftShader, which becomes
unstable after several GPU-heavy Compose launches; frames were captured across
multiple runs instead of one.

**What was done.** 12 screenshots (6 dark, 6 light) were captured from real
rendered frames in several cloud runs, and each image is labelled with its run
ID. No mockups.

**Residual risk.** Visual QA is not a jank/frame-time benchmark; performance on
the target OPPO A38 remains **unmeasured** (`docs/HMXHARNESS_PERFORMANCE_REPORT.md`).

> **Stale-document warning.** `docs/HMXHARNESS_PERFORMANCE_REPORT.md` describes an
> earlier glass design (`LiquidGlassQuality.LITE`, `Runtime.maxMemory() < 320 MB`,
> "no `RenderEffect` blur and no backdrop capture"). The current source uses
> `GlassTier.LOW/MID/HIGH` with `detectGlassTier` thresholds of ≤255 MB / ≥512 MB
> (`ui/liquidglass/GlassQuality.kt`) and real PrismalAGSL backdrop sampling with
> blur, lens refraction and specular layers (`LiquidGlassModifiers.kt`). Treat the
> performance report as historical design intent, **not** as current
> implementation evidence.

### A8. GitHub OAuth without a client secret, and without a human to approve

**Problem.** Device Flow needs (a) an OAuth App with Device Flow enabled and
(b) a human to type the code at `github.com/login/device`. Neither can be done
by an automated build.

**Authorship note.** The flow implementation is **upstream** (`2207914579`,
2026-10-09). HMX's part is the Settings connection card (`ui/settings/
ConnectedAccountsSection.kt`, `e169ef40`) and CI injection of the Client ID
(`f9dccd39`, `5e100284`). Do not credit HMX with the flow itself.

**What is in the app.** `github/GitHubAuth.kt` implements the full flow
(`/login/device/code` → poll `/login/oauth/access_token` → handle
`authorization_pending`, `slow_down`, `expired_token`, `access_denied` → refresh
via `offline_access`), plus a clear failure message when no Client ID is injected,
plus PAT fallback. Logic is covered by `GitHubAuthTest.kt`; the **live approval
step is marked unverified**.

### A9. Privileged shell without root: three different providers

**Problem.** "Run a real command on the phone" has no single API. Root may not
exist; Shizuku needs a one-time ADB/Wireless-debugging setup; nothing may be
available at all.

**What was done.** `data/env/ShellTierRouter.kt` selects an `ExecutionTier`
(`TERMUX_SSH`, `ROOT`, `PRIVILEGED` = Shizuku, `APP_LINUX`, `TOYBOX`) and
`tools/ShellPolicy.kt` applies a denylist **before** execution regardless of tier,
including recursive expansion of `$(...)`, backticks and variables, canonical
workspace containment and redirection/symlink checks.

**Residual risk.** More tiers mean more paths to test; the tier matrix is not
exhaustively covered by instrumentation tests.

### A10. Security debt that was documented, not hidden

| Finding | Severity | Status |
|---|---|---|
| `android:usesCleartextTraffic="true"` with no network security config | HIGH | Documented; scoped NSC proposed as P1 |
| MCP static headers/env stored in plaintext `mcp-servers.json` (HMAC integrity only) | MEDIUM | Documented; move to `KeyStoreManager` proposed |
| GitHub CLI token materialised in `linux/` prefix (`.gh-token`) | MEDIUM | Documented; keyring/credential-helper hardening proposed |
| GPU runtime `.so` downloaded at runtime from a fixed upstream URL (SHA-256 pinned) | LOW | Documented; fork should publish its own package |
| Release APK signed with the **Android debug key** | Risk, not a CVE | Documented; not Play-ready |
| Backup password minimum 6 characters | LOW | Documented (600k PBKDF2 iterations mitigate) |

*Evidence:* `docs/HMXHARNESS_SECURITY_AUDIT.md`.

### A11. Maintaining a 158k-line codebase

| Metric | Value |
|---|---|
| Kotlin files in `app/src/main/java` | 732 |
| Kotlin lines in `app/src/main/java` | 158,276 |
| Kotlin lines in tests | 35,422 |
| Unit `@Test` methods | 2,478 across 351 files |
| Instrumentation `@Test` methods | 555 across 139 files (not run in CI) |
| Room entities / schema version / migrations | 14 / 27 / 23 |
| Locales | 21 |
| Tool files in `tools/` | 69 |

The practical challenge is not writing features but keeping a large derived
codebase compiling, lint-clean and test-green after a package rename — which is
what the build-recovery work in `docs/audits/` is about.

---

## Part B — Costs (measured, not estimated)

### B1. What the project does **not** pay for

| Item | Reality | Evidence |
|---|---|---|
| Backend server | **None exists.** Provider requests go from the device to the configured endpoint. | `llm/Llm.kt`, `llm/*Provider.kt`; no server module in `settings.gradle.kts` |
| Database hosting | **None.** Room is on-device. | `data/db/Database.kt` |
| Push/analytics/crash SDK | **None present.** | `app/build.gradle.kts`, `PRIVACY.md` |
| Model training | **None performed.** No training code in the repository. | audit finding; only `safetensors_import.cpp` (format conversion) |

### B2. Measured storage and artifact sizes

| Artifact | Size | Source |
|---|---|---|
| CI APK artifact (debug + release bundle) | **110,461,382 bytes** (~105 MiB) | artifact `hmxharness-apks`, id `14837630763` |
| Git object pack | **26.17 MiB** | `git count-objects -vH` |
| Working tree including `.git` | **109 MB** | `du -sh` |
| Local model download — SmolLM2 135M Q4_K_M | **105,454,432 bytes** (~100 MiB) | `local/LocalModelRecommendations.kt` |
| Local model — SmolLM2 360M Q4_K_M | **270,590,880 bytes** | same |
| Local model — Qwen 3 0.6B Q4_K_M | **484,220,320 bytes** | same |
| Local model — SmolLM2 1.7B Q4_K_M | **1,055,609,824 bytes** | same |

Local models therefore cost **storage and battery**, not money, but the smallest
useful one is still a ~100 MB download.

### B3. Cloud build cost (measured runtime, billed minutes not estimated)

`ubuntu-latest` runners, JDK 21, no emulator, `timeout-minutes: 90`,
`concurrency` cancels superseded runs.

| Run | Commit | Outcome | Duration |
|---|---|---|---|
| `45219214615` | `22e674e9` (docs) | success | **27 m 47 s** |
| `45204023911` | `32127b31` | success | **52 m 35 s** (cold cache) |
| earlier green run | `32127b31` | success | **33 m 30 s** (warm cache) |

Interpretation to present honestly:

- A green pipeline costs **~30–55 minutes of standard-runner compute**.
- Warm Gradle caching roughly halves it; the cache action is
  `gradle/actions/setup-gradle@v4`.
- On a **public** repository, standard GitHub-hosted runner minutes are not
  billed (GitHub's published policy at the time of writing); on a private
  repository they consume the account's included minutes. **No dollar amount is
  claimed here** because the billing tier is account-specific.
- Failed runs are the expensive ones: the recovery day on 2026-11-23 produced
  several short failures (1.5–16 min) before each green build, which is the real
  cost driver of a CI-based workflow.

### B4. Runtime cost the *user* pays, and how the app exposes it

- **Cloud model usage** is charged by the user's chosen provider, not by
  HMXHARNESS. The app estimates spend locally from published list prices:
  `llm/ModelPrices.kt` (`costFor`, "Rough list prices ($ per 1M tokens) ... backed
  by models.dev", incl. cache read/write multipliers) and `ModelCatalog.kt`,
  `ModelsDev.kt`.
- **Free path exists:** the built-in `harness` provider (`llm/HarnessProvider.kt`,
  `HarnessFreeLimit.kt`) routes to free public gateways
  (`opencode.ai/zen`, `api.kilo.ai`, `text.pollinations.ai`). This is a
  **third-party free tier**, not an HMX-hosted model — it can rate-limit or disappear, and must not be presented as guaranteed
  free inference.
- **Keyless web search:** `tools/SearchProviders.kt` (`KEYLESS`, scraping
  DuckDuckGo/Bing/Brave/Google result pages) needs no API key but is slower and
  can be blocked.
- **Keyed search providers** (Brave, Tavily, Serper, Linkup, Jina, LangSearch,
  SerpApi, Firecrawl, Marginalia) are the user's own accounts.

### B5. Hardware requirements implied by the code

| Requirement | Evidence |
|---|---|
| Android 8.0+ (`minSdk 26`) | `app/build.gradle.kts` |
| 64-bit ABI for local inference (`arm64-v8a`/`x86_64`; 32-bit gets stubs) | `app/src/main/cpp/CMakeLists.txt`, `local_unavailable.cpp` |
| ≥ 2–4 GiB RAM for recommended local models | `LocalModelSpec.minimumRamGiB` (2 GiB for 135M, 3 GiB for Qwen3 0.6B, 4 GiB for 1.7B) |
| Shizuku **or** root **or** Wireless ADB for privileged shell | `data/env/ShizukuManager.kt`, `RootManager.kt`, `data/env/adb/` |
| Network for cloud models, web search, GitHub | provider and tool code |

### B6. What would cost money to take this further (clearly labelled as plan)

These are **not** current costs; they are the predictable next expenses. They are
listed so judges see the project understands its own economics:

1. **A real release keystore** — free to create, but required before any store
   distribution (today the release APK uses the debug key).
2. **A Play Console account** — a one-off Google fee; not paid here.
3. **Paid model API budget** for reproducible benchmarks — variable, user-chosen.
4. **A physical test device or accelerated emulator** — needed for the currently
   unmeasured frame-time, local-inference and shell-tier results.
5. **Third-party free-tier dependence** — free gateways and keyless search have
   no SLA; a paid key is the fallback.

---

## Part C — The three sentences to say out loud

1. "There is no server bill, because there is no server: the agent, the database
   and the credentials all live on the phone."
2. "Our only recurring infrastructure cost is cloud CI, roughly half an hour of
   standard runner time per green build — and failed builds are what actually
   cost time."
3. "What we have **not** measured is device performance and on-hardware
   behaviour; those are pending, not passed."
