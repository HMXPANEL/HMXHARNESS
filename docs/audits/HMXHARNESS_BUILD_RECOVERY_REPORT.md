# HMXHARNESS — Build Recovery Report

> **Historical document.** This report records build-recovery work in the
> **previous** repository. Its commit hashes, CI run IDs and artifact IDs are
> historical and are not evidence for the current repository. Current CI evidence
> is run `45338701934` and artifact `14898111209` in
> `HMXHARNESS_SECOND_FULL_REPOSITORY_AUDIT.md`.

- **Repository:** https://github.com/HMXPANEL/HMXHARNESS
- **Branch:** `main`
- **Application:** HMXHARNESS (`com.hmx.harness`)
- **Verified commit:** `32127b31`
- **Final successful run:** https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45204023911
- **Build environment:** GitHub Actions `ubuntu-latest`, cloud only (no local
  Gradle or device build)

---

## 1. Objective

Restore a green cloud build for the consolidated `main` branch and produce a
real, downloadable APK, fixing root causes — not by weakening tests, skipping
compilation, disabling lint, or faking success.

---

## 2. Build toolchain discovered

| Component | Value |
|---|---|
| Gradle wrapper | `9.7.0` (`gradle-9.7.0-bin.zip`) |
| Android Gradle Plugin | `9.3.1` |
| Kotlin | `2.4.10` |
| Compose BOM | `2026.08.00` |
| JDK (CI) | Temurin 21 |
| NDK | `27.2.12479018` (installed by workflow) |
| Shader compiler | `glslc` (installed by workflow) |
| `namespace` / `applicationId` | `com.hmx.harness` |
| `versionCode` / `versionName` | `40` / `4.0` |
| min / target SDK | 26 / 36 |
| ABI filters | `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` |
| OAuth Client ID source | `local.properties` ← `HMX_GITHUB_CLIENT_ID` (env) ← `GITHUB_CLIENT_ID` (env) |

### Workflows inspected

- `.github/workflows/android-ci.yml` — the authoritative build:
  unit tests → `lintDebug` → `assembleDebug` + `assembleRelease` → verify
  outputs → upload artifact. Triggers on `main`, `feature/**`, PRs, dispatch.
- `.github/workflows/android-release.yml` — `workflow_dispatch` and `v*` tags;
  assembles and uploads the release APK.
- `.github/workflows/android-visual-qa.yml` — manual + `feature/hmxharness-liquid-glass-final`
  push; cloud-emulator screenshots/performance (not part of `main` CI).

`step 3` of CI injects `HMX_GITHUB_CLIENT_ID` from
`vars.HMX_GITHUB_CLIENT_ID || secrets.HMX_GITHUB_CLIENT_ID` into
`local.properties` without printing it. The repository has the Client ID stored
as a secret; either form works.

---

## 3. Failure timeline and root-cause analysis

Runs below are on the feature branch while recovering the build, followed by the
consolidated `main` run.

### Failure 1 — Duplicate resource (`f4de6fed`, run `45169730349`)

**Symptom:** `> Task :app:packageDebugResources FAILED` /
`:app:mergeDebugResources FAILED`.

**First root cause:**
```
ERROR: app/src/main/res/values-*/strings.xml:
Resource and asset merger: Found item String/settings_page_accounts_desc more than one time
```

`settings_page_accounts_desc` already existed in all 21 locale files (the
existing ChatGPT-account description). The Device Flow change appended a second
copy of the same key, which the Android resource merger rejects.

**Fix:** Restore the clean resource files and **update the existing
`settings_page_accounts_desc` in place** instead of re-adding it. Add only the
two genuinely new keys (`settings_github_scope_note`, `ui_could_not_copy_code`)
with translations across all 21 locales. Commit `0c833e64`.

### Failure 2 — Non-`.xml` resource files (`0c833e64`, run `45172441538`)

**Symptom:** same merge tasks failed, this time with one error per locale.

**First root cause:**
```
ERROR: app/src/main/res/values-*/strings.xml.bak:
Resource and asset merger: The file name must end with .xml
```

Earlier scripting left 21 tracked `strings.xml.bak` backups inside `res/`.
Android's resource merger rejects any file whose name does not end in `.xml`.

**Fix:** `git rm` all 21 `.bak` files and add `*.bak` to `.gitignore` so they
cannot be recommitted. Commit `f1f2e7b8`.

### Failure 3 — Android lint (`f1f2e7b8`, run `45173192336`)

**Symptom:** unit tests passed; `> Task :app:lintDebug FAILED`.

**First root cause:**
```
ConnectedAccountsSection.kt:135: Error: Querying resource values using
LocalContext.current is not configuration-aware and may return stale values if
the Configuration changes [LocalContextGetResourceValueCall from androidx.compose.ui]
```
Five occurrences, all in the new GitHub account card, from
`LocalContext.current.getString(...)` used inside `open()` / `copyCode()`
callbacks.

**Fix:** Capture the five strings with `stringResource(...)` during composition
and format them with `String.format`. This is the correct configuration-aware
pattern; **no suppression and no baseline entry** were added. Commit `32127b31`.

> Note: `String.format` here formats an already-localized resource string; it
> introduced no build error and lint reports only 5 warnings (0 errors).

### Result

| Run | Commit | Unit tests | Lint | APK build | Artifact |
|---|---|---|---|---|---|
| `45169730349` | `f4de6fed` | ❌ resource merge | skipped | skipped | — |
| `45172441538` | `0c833e64` | ❌ resource merge | skipped | skipped | — |
| `45173192336` | `f1f2e7b8` | ✅ | ❌ 5 lint errors | skipped | — |
| `45180142866` | `32127b31` (feature) | ✅ | ✅ | ✅ | ✅ |
| **`45204023911`** | **`32127b31` (main)** | ✅ | ✅ | ✅ | ✅ |

### Consolidated `main` run `45204023911` — all steps

| # | Step | Result |
|---:|---|---|
| 1 | Set up job | success |
| 2 | Checkout source | success |
| 3 | Configure GitHub OAuth Client ID | success |
| 4 | Validate Gradle wrapper | success |
| 5 | Set up JDK 21 | success |
| 6 | Install pinned NDK and shader compiler | success |
| 7 | Set up Gradle with caching | success |
| 8 | Run unit tests | success |
| 9 | Upload unit test report on failure | skipped (no failure) |
| 10 | Run Android lint (debug) | success |
| 11 | Build debug and release APKs | success |
| 12 | Verify APK outputs | success |
| 13 | Upload APK artifacts | success |
| 14 | Publish build summary | success |

Gradle sub-builds: unit tests `BUILD SUCCESSFUL in 5m 35s`; lint
`BUILD SUCCESSFUL in 9m 24s`; APK assembly `BUILD SUCCESSFUL in 34m 51s`.
Lint: **5 warnings, 0 errors** (2,497 errors / 1,420 warnings / 9 hints
filtered by the pre-existing baseline).

---

## 4. Phase 6 — APK artifact verification

**Artifact:** `hmxharness-apks`
- Artifact ID: `14837630763`
- Size: `110,461,382` bytes (zip)
- Contents: `HMXHARNESS-debug.apk` (107,205,992 bytes), `HMXHARNESS-release.apk` (69,106,020 bytes)
- Retention: until `2027-02-21`
- Download (authenticated API): `https://api.github.com/repos/HMXPANEL/HMXHARNESS/actions/artifacts/14837630763/zip`
- Download (UI): the **Artifacts** section of run `45204023911`

The artifact was downloaded and the APKs were inspected with `aapt` and
`apksigner` in this environment:

### Release APK — `HMXHARNESS-release.apk`

```
package: name='com.hmx.harness' versionCode='40' versionName='4.0'
         compileSdkVersion='37'
sdkVersion:'26'
targetSdkVersion:'36'
application-label:'HMXHARNESS'
launchable-activity: name='com.hmx.harness.MainActivity'
native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
Signer #1 certificate SHA-256: 4f76a232184501ea4725bdf2381df3f893281e15ff21e69fc4701dc21e680912
```

### Debug APK — `HMXHARNESS-debug.apk`

```
package: name='com.hmx.harness.debug' versionCode='40' versionName='4.0'
sdkVersion:'26'
targetSdkVersion:'36'
application-label:'AH-debug'
launchable-activity: name='com.hmx.harness.MainActivity'
native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
```

### Verification checklist

| Requirement | Result |
|---|---|
| Workflow completed successfully | ✅ |
| APK actually generated | ✅ debug + release |
| Artifact exists and non-empty | ✅ 110.4 MB zip |
| Correct application ID | ✅ `com.hmx.harness` (release) |
| Expected version present | ✅ `4.0` / code `40` |
| APK inspectable with Android tools | ✅ `aapt` / `apksigner` |
| Artifact downloadable from successful run | ✅ |
| No required build step skipped | ✅ (only failure-only upload skipped) |
| Signing certificate inspected | ✅ debug certificate (documented) |
| Installed/tested on a physical device | ❌ not performed |

> **Not claimed:** the app was not installed or exercised on a physical device.

---

## 5. Phase 7 — Regression checks

| Check | Status | Notes |
|---|---|---|
| Kotlin compilation (debug/release) | ✅ passed | part of APK assembly |
| Unit tests (`:app:testDebugUnitTest`) | ✅ passed | full suite, 352 test source files |
| GitHub authentication tests (`GitHubAuthTest`) | ✅ passed | in the unit-test suite |
| Android lint (`:app:lintDebug`) | ✅ passed | 0 errors, 5 warnings |
| Android resource linking / merge | ✅ passed | all 21 locales |
| Kotlin duplicate declarations / unresolved refs | ✅ none | clean build |
| Manifest merge | ✅ passed | debug + release |
| Native build (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) | ✅ passed | NDK 27.2.12479018 |
| Instrumentation tests (`app/src/androidTest`, 148 files) | ⚪ not run | no `connectedAndroidTest` step in CI |
| Cloud-emulator visual QA | ⚪ not run for `main` | manual workflow, feature-branch trigger |
| APK install on physical device | ⚪ unavailable | device `pm install` fails (`INTER_ACROSS_USERS_FULL`/AppOps NPE) |

Legend: ✅ passed · ⚪ not available/not executed.

---

## 6. Remaining blockers (accurate, external)

1. **Live Device Flow authorization** must be approved by a human at
   github.com. The OAuth App must have **Device Flow enabled**. Not automatable.
2. **Release signing** uses the Android debug certificate (inherited upstream
   behavior). A production-signed APK requires a keystore and CI secrets.
3. **Instrumentation tests** are not wired into CI and were not executed; they
   require device/emulator fixtures and network resources.
4. **Physical-device installation** is blocked by the device's package-manager
   restrictions in this environment.

No build-blocking issue remains: the consolidated `main` produces a valid APK on
every push via `android-ci.yml`.
