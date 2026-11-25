# HMXHARNESS — Test Coverage and Quality Audit

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** static inventory (`find`/`grep`), workflow inspection and committed CI
documents. No Gradle task was executed, no GitHub API query was made (no token in
this environment), and no emulator/device was used. Every "passed" statement below
is transcribed from committed evidence, not independently reproduced here.

## 1. Repository inventory (exact counts)

| Area | Count |
| --- | ---: |
| Kotlin `app/src/main` | 732 |
| Kotlin `app/src/test` | 352 (351 with `@Test`) |
| Kotlin `app/src/androidTest` | 140 (139 with `@Test`) |
| Kotlin `app/src/debug` | 4 |
| Kotlin `app/src/githubDelivery` | 2 |
| Kotlin `app/src/playDelivery` | 1 |
| Kotlin total | 1,231 |
| Java | 5 |
| C++ `.cpp` / C headers `.h` / CMake | 8 / 3 / 1 |
| JS / MJS / Python / shell | 5 / 2 / 3 / 5 |
| XML | 1,009 |
| Git-tracked files | 2,519 |

No Kotlin files exist outside `app/src`. No `.c`, `.cc`, `.hpp`, `.cxx` or `.cmake` files exist.

## 2. Test inventory

- **Unit tests:** 2,478 `@Test` methods in 351 Kotlin files (17 use `expected`/`timeout`).
- **Instrumented:** 555 `@Test` methods in 139 Kotlin files (7 with 90–420 s timeouts).
- **No** JUnit 5 parameterized/factory tests; **0** literal `assertTrue(true)`/`assert(true)`.

Largest unit-test packages: `tools` 442, `agent` 326, `llm` 233, `data.env` 192,
`data` 145, `ui.chat` 130, `core` 102, `ui.settings` 91, `local` 73,
`core.response` 57, `terminal` 54, `plugins` 50, `ui.files` 47, `workspace` 45,
`github` 38, `skills` 37, `browser` 31, `tools.mcp` 25, `hmx` 7.

Largest instrumented packages: `ui.chat` 164, `ui.settings` 151, `data.env` 37,
`ui.files` 17, `tools` 15, `browser` 14, `ui.chat.rich` 14, `agent` 13,
`data` 12, `ui.liquidglass` 2.

## 3. What CI actually executes

| Workflow / lines | Task | Effect |
| --- | --- | --- |
| `.github/workflows/android-ci.yml:62` | `:app:testDebugUnitTest` | Executes all 2,478 JVM tests |
| `android-ci.yml:73` | `:app:lintDebug` | Debug lint only, baseline applied |
| `android-ci.yml:76` | `:app:assembleDebug :app:assembleRelease` | Packages APKs; **does not compile test source sets** |
| `android-ci.yml:78-98` | shell `test -s` | Non-empty APK check + artifact `hmxharness-apks` |
| `android-release.yml:60` | `:app:assembleRelease` | No tests, no lint |
| `android-visual-qa.yml:58,91-118,178-202` | debug assemble + emulator capture/perf | Dispatch or one feature branch only; screenshot count, no assertions |
| `lint-baseline.yml:23` | `:app:updateLintBaseline` | Dispatch only |

**Gradle tasks referenced in no workflow:** `connectedDebugAndroidTest`,
`connectedAndroidTest`, `assembleDebugAndroidTest`, `assembleReleaseAndroidTest`,
`testReleaseUnitTest`, `lintRelease`, `lintVitalRelease` (explicitly disabled),
and distribution-variant unit tests.

**Important nuance:** the claim that the 555 instrumented tests "exist and compile"
is **not verified by CI** — no workflow compiles `androidTest` sources. This was not
compiled locally either, so it remains unverified.

**CI documentation inconsistency:** `android-ci.yml:3-6` says the workflow "deliberately
does not install the NDK", but `:53-56` installs `ndk;27.2.12479018` and `glslc`;
`app/build.gradle.kts:39-65,75-80` gates native code on that NDK, so CI uses the
**native** path. The comment is stale and should be corrected.

**Lint debt:** `app/build.gradle.kts:139` `checkReleaseBuilds = false`; `:143` applies
`app/lint-baseline.xml` (44,632 lines, 3,930 `<issue>` entries). Debug lint only fails
on *new* findings.

**Wrapper mode:** `git ls-files -s gradlew` → `100755` (fix commit `ae41ac7`).

## 4. Verified vs compiled vs unverified

| Item | Status |
| --- | --- |
| 2,478 JVM unit tests | Executed and gating per run `45338701934` (committed evidence) |
| Debug lint | Executed and gating, new findings only; 3,930 issues baselined |
| Debug + release APK | Compiled, `test -s` size check; no install/launch/smoke test |
| Release lint | Not run |
| 555 instrumented tests | Neither compiled nor executed by any workflow |
| Native C++ (8 `.cpp`, 3 `.h`) | Compiled when NDK present; **0 native unit tests** |
| Liquid Glass visual correctness | Not verified (see §5) |
| Visual QA on `main`/PRs | Not triggered |
| Device behaviours (Shizuku, GGUF, browser, voice, Telegram, root) | Not verified |
| HMX runtime verification gate | Unit-tested only; no production callers |

## 5. Tests that suppress or weakly verify behaviour

- **`app/src/androidTest/.../ui/liquidglass/LiquidGlassScreenshotTest.kt:59-63`** —
  `safely()` wraps screens in `runCatching{}.onFailure{ Log.w(...) }`; any exception,
  including a failed assertion, is downgraded to a warning. `:37-44` `capture()`
  compresses a PNG but never compares pixels; `:39` uses fixed `Thread.sleep(400)`.
  Both `@Test`s (`:65,71`) delegate entirely to `safely`.
- **`.github/workflows/android-visual-qa.yml:130-137`** — counts PNGs (`-size +1k`,
  ≥24 for `all`); no baseline/hash/pixel comparison.
- **`scripts/glass-capture.sh:3,6-27`** — uses `set -u` but not `set -e`; `adb install`,
  `am start` and capture all end in `|| true`. `glass-perf.sh:6-15` is similar.
- `app/src/test/.../data/SettingsBackupValidationTest.kt:41-43` — exception-only check.
- `app/src/test/.../data/audio/GroqWhisperClientTest.kt:61-111` — cancellation tests
  have no `assert*`; they rely on `CompletableFuture.get(timeout)`/`withTimeout`.
- `app/src/test/.../config/HarnessConfigTest.kt:155-160` — schema-string rejection only.
- `app/src/test/.../llm/GeminiToolSchemaTest.kt:66-97` — verifies request-body echo,
  not provider runtime behaviour (contrast `:29-64`, a real mocked-stream test).
- `app/src/test/.../agent/HarnessEnvironmentPromptTest.kt:9-17,30-37` — `text.contains`.
- `app/src/test/.../hmx/HmxTaskVerificationTest.kt:31-61` — 7 passing tests for code
  that has no production callers.

A heuristic scan produced 81 "no assertion" candidates; manual review found most are
false positives delegating to asserting helpers. Counterexample: `core/LocalPortProbeTest.kt:10-52`
matches "network" patterns but is pure string parsing with direct assertions.

## 6. Coverage gaps and flakiness risks (assessment, not observed failures)

**Gaps:** all 555 instrumented tests are invisible to CI; no native tests; no pixel
assertions; release path has no tests/lint/install smoke; the runtime approval gate is
test-only; 3,930 lint issues baselined; no hardware verification.

**Flakiness risks:**
- `router/NineRouterNodeRuntimeTest.kt:71-78` skips via `assumeNotNull` if Node is
  absent; `:62-68` waits 20 s on a subprocess.
- Timing-bound: `llm/SseKeepAliveTest.kt:55-68` (20 s timeout, 100/1,000 ms gaps),
  `llm/SseStreamTest.kt:19`, `llm/debug/DebugSseTapTest.kt:24,73`,
  `data/env/CancellableServiceCommandTest.kt:40,55`.
- Loopback/OkHttp tests in `GroqWhisperClientTest.kt`, `ProviderNetworkTest.kt:95`,
  `McpRemoteTest.kt:205`, `data/audio/*`, `data/transfer/FtpShareServerTest.kt`.
- 7 instrumented tests with 90–420 s timeouts; 477 `Thread.sleep`/`SystemClock.sleep`
  in 100 `androidTest` files; ~95 `assume*` gates.
- 27 unit-test files reference external `ProcessBuilder` tools (git/node/python/shell).
- Emulator QA uses SwiftShader, 1,800 s boot timeout and fixed 13/16 s sleeps.

## 7. Committed CI evidence (not independently re-queried)

- Run `45338701934` (commit `ae41ac7`) — success; wrapper validation, JDK 21, NDK
  install, unit tests, lint, both APKs, artifact `hmxharness-apks` id `14898111209`,
  110,461,001 bytes. Sources: `docs/expo/HMXHARNESS_CHALLENGES_AND_COSTS.md:44-51,232-240`,
  `docs/expo/HMXHARNESS_FEATURE_VERIFICATION.md:28`, `README.md:296`.
- Run `45337564227` — failure `./gradlew: Permission denied`, exit 126; fixed by
  restoring Git mode `100755` in `ae41ac7`.

## 8. Explicit limitations

No Gradle task was run; no test result XML parsed; no GitHub API call (no token); no
emulator/device used. Flakiness is a risk assessment only. The "instrumented tests
compile" claim and CI conclusions were not independently reproduced.
