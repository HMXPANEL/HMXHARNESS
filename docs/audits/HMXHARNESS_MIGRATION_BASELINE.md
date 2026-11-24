# HMXHARNESS — Migration Baseline Audit (PHASE 0)

**Date:** 2026-11-24
**Target repository:** https://github.com/HMXPANEL/HMXHARNESS.git
**Local project root inspected:** `/mnt/sdcard/AIProjects/HMXHARNESS-main`
(resolves to `/storage/emulated/0/AIProjects/HMXHARNESS-main`)

This is a **fresh repository migration** of an extracted project ZIP, not a Git history migration.
This document records the pre-change baseline only. No source files were modified during PHASE 0.

## 1. Android project root identification

The extracted ZIP root **is** the Android project root. Verified markers at top level:

| Required item | Status | Notes |
| --- | --- | --- |
| `settings.gradle.kts` | Present | `rootProject.name = "HMXHARNESS"`, `include(":app")`, conditional `include(":local_gpu")` when `-Pdistribution=play` |
| `build.gradle.kts` | Present | Version-catalog plugins, AGP buildscript dependency constraints |
| `gradlew` / `gradlew.bat` | Present | Executable Unix wrapper present |
| `gradle/wrapper/` | Present | `gradle-wrapper.jar` (43,764 B), `gradle-wrapper.properties` |
| `app/build.gradle.kts` | Present | Main application module |
| `.github/workflows/` | Present | 4 workflows (see §4) |
| `local.properties` | **Absent** | Correct: never commit; CI injects the Client ID at runtime |
| `.gitignore` | Present | Reviewed in §5 |
| `gradle/libs.versions.toml` | Present | Central version catalog |

**Nested project folders:** None. A full-tree search found exactly one `settings.gradle.kts` and no nested Gradle builds. `local_gpu/` is a conditional dynamic-feature module, not a second project root.

**Old `.git` directory:** **Absent.** No Git history to copy, and none will be recreated from the old repository.

## 2. Toolchain and build configuration (as found)

- Gradle wrapper: `gradle-9.7.0-bin.zip`, `validateDistributionUrl=true`, `networkTimeout=10000`.
- AGP `9.3.1`; Kotlin `2.4.10`; KSP `2.3.11`; Compose BOM `2026.08.00`; Material3 `1.5.0-alpha26`.
- JVM toolchain: Java 17 compile target; CI uses Temurin JDK 21. `org.gradle.jvmargs=-Xmx4g`, `kotlin.daemon.jvmargs=-Xmx8g`.
- `compileSdk = 37` with `android.suppressUnsupportedCompileSdk=37`; `minSdk = 26`; `targetSdk = 36`.
- Pinned NDK `27.2.12479018`; CMake `3.22.1`; `prefab = true` (BoringSSL / SPAKE2).
- Native ABI filters: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`.
- Application ID `com.hmx.harness`; `versionCode = 40`; `versionName = "4.0"`.
- Debug application ID suffix `.debug`.
- Release build type is signed with the **debug** signing config and `isMinifyEnabled = true`, `isShrinkResources = true`. → The release APK is **debug-signed alpha distribution, not Play Store ready**. This must be disclosed in final reporting.
- `gradle.properties` enables configuration cache, parallel builds and build cache.
- AGP/Kotlin/Gradle compatibility is to be proven by the cloud build (PHASE 2); no local Android build will be attempted.

## 3. Source preservation inventory

| Category | Count / location |
| --- | --- |
| Kotlin sources | 1,231 `.kt` files |
| Java sources | 5 `.java` files |
| Native C/C++ | 11 `.cpp/.h/.hpp` files under `app/src/main/cpp/` |
| XML resources / manifests | 1,009 `.xml` files |
| Total files in tree | 2,525 |
| Source sets | `main`, `debug`, `githubDelivery`, `playDelivery`, `test`, `androidTest` |
| AIDL | `app/src/main/aidl/com/...` |
| Assets | `editor-tools`, `learn`, `licenses`, `net`, `richtext`, `skills` |
| Tests | 1,231 Kotlin files include JVM unit tests (`app/src/test`) and instrumented tests (`app/src/androidTest`) |
| Localization | 24 `values-*` resource directories |
| Documentation | `docs/` (14 HMXHARNESS docs + `audits/`, `configuration/`, `expo/`), `README.md` (26,906 B), `FEATURES.md` (78,860 B), `CHANGELOG.md` (191,505 B), `PRIVACY.md`, `SECURITY.md` |
| Other | `repowiki/` (76 files), `scripts/`, `local_gpu/`, `helloworld.txt`, `.editorconfig` |

No files were excluded, deleted or rewritten during PHASE 0.

## 4. GitHub Actions workflows (as found)

| Workflow | Triggers | Purpose |
| --- | --- | --- |
| `android-ci.yml` | `push` to `main` and `feature/**`, `pull_request`, `workflow_dispatch` | Wrapper validation, JDK 21, NDK install, unit tests, `lintDebug`, `assembleDebug` + `assembleRelease`, APK verification, artifact `hmxharness-apks` |
| `android-release.yml` | `workflow_dispatch`, `push` tags `v*` | Release APK, artifact `hmxharness-release-apk` |
| `android-visual-qa.yml` | `workflow_dispatch`, `push` to `feature/hmxharness-liquid-glass-final` | Cloud-emulator screenshots/performance; artifact builds |
| `lint-baseline.yml` | `workflow_dispatch` only | Maintenance: regenerate `app/lint-baseline.xml` |

**Observation (non-blocking, informational):** `android-ci.yml` opens with a comment claiming the workflow
"deliberately does not install the NDK", but the workflow **does** contain an
`Install pinned NDK and shader compiler` step (`ndk;27.2.12479018` + `glslc`). The comment is stale;
the actual step is consistent with `app/build.gradle.kts`, which enables the native block when the pinned
NDK directory is present. CMake `3.22.1` is requested by Gradle from the SDK; no separate CMake install
step exists. → Cloud build will confirm whether additional CMake installation is required.

### HMX_GITHUB_CLIENT_ID injection contract

- Workflows read `vars.HMX_GITHUB_CLIENT_ID || secrets.HMX_GITHUB_CLIENT_ID` into `CLIENT_ID`, then
  append `GITHUB_CLIENT_ID=<value>` to the **gitignored** `local.properties` before Gradle runs.
- `app/build.gradle.kts` reads `GITHUB_CLIENT_ID` from `local.properties` (or env fallback) into
  `BuildConfig.GITHUB_CLIENT_ID`; it validates the value against `[A-Za-z0-9]*`.
- No client secret, private key or backend is introduced; GitHub Device Flow needs none.
- Repository variable `HMX_GITHUB_CLIENT_ID` must be configured in the new repository. If absent,
  CI emits a warning and builds with Device Flow sign-in disabled (PAT sign-in still available).
- **The value must never be printed or committed.** No `local.properties` exists in the tree.

## 5. `.gitignore` review

Ignores: build outputs (`build/`, `.gradle/`, `.cxx/`, `.kotlin/`), `local.properties`, `.env*`,
`secrets.properties`, `keystore.properties`, `key.properties`, `*.apk/*.aab/*.ap_`, IDE dirs,
OS noise, AI-assistant working dirs, signing material (`signing-keys/`, `*.jks`, `*.keystore`,
`*.p12`, `*.pfx`, `*.key`, `*-private.pem`, `id_rsa`, `id_ed25519`), `node_modules/`, `*.log`.

Kept intentionally (relevant to this migration):
- `docs/audits/` is explicitly **un-ignored** (`!docs/audits/`) → all six required audit reports will be tracked.
- `docs/configuration/`, `docs/harness.schema.json`, `docs/expo/` are un-ignored.
- Gradle wrapper files and `.github/workflows/` are **not** ignored.

Note: broad `docs/*` ignore means most root-level `docs/*.md` files are currently **untracked**.
This is preserved as found; no `.gitignore` change was made in PHASE 0.

## 6. Secret scan (PHASE 1 pre-commit check, performed early)

- Scanned all non-ignored files for `gh[pousr]_…`, `github_pat_…`, `sk-…`, `AIza…`, private-key PEM
  headers and `client_secret`/`GITHUB_CLIENT_ID` assignments.
- Findings: 3 synthetic token strings inside
  `app/src/test/java/com/hmx/harness/tools/SecretRedactorTest.kt` (unit-test fixtures for the
  redaction feature, e.g. `gho_A1b2C3…`, `ghp_ABCDEF…`, `github_pat_AAAA…`). These are deliberately
  fake test data, not credentials. All other matches are workflow `printf … "$CLIENT_ID"` lines and a
  README placeholder `GITHUB_CLIENT_ID=your_public_client_id`.
- **No real credentials found.** No private signing key present (no `signing-keys/`, no `*.jks`).

## 7. Legal / attribution baseline (must be preserved in PHASE 4)

- `LICENSE`: MIT, `Copyright (c) 2026 Sanuu7` — MIT terms and this notice must be preserved.
- Third-party license texts under `app/src/main/assets/licenses/`: `backdrop.txt`, `caveman.txt`,
  `dhizuku-api.txt`, `llama.cpp.txt`, `lucide.txt`, `noto_sans_mono.txt`, `shizuku.txt`,
  `vulkan-headers.txt` — preserve.
- `README.md` already states: "HMXHARNESS is an independently maintained project derived from the
  MIT-licensed AndroidHarness codebase." Upstream attribution must be retained.
- Existing branding is largely HMXHARNESS already (`app_name` = `HMXHARNESS`, workflow names, README),
  with at least one residual string `glass_title = "AHarness Glass (Experimental)"` at
  `app/src/main/res/values/strings.xml:1075` for PHASE 4 review.

## 8. Blocker discovered during PHASE 1 preparation

The authenticated GitHub account is `tanishka1233` (single configured `gh` account, token scopes:
`gist`, `read:org`, `repo`, `workflow`). The target repository `HMXPANEL/HMXHARNESS` exists, is empty,
but the authenticated account's effective permission is **READ only**
(`permissions: {pull: true, push: false, admin: false, ...}`; `viewerPermission: READ`).

→ **Pushing is not possible with the current credentials.** Per the task instructions, work stops at
the push step until the repository owner grants write access (see the final report for the exact step).
No force-push was attempted and the previous repository has not been touched.

## 9. PHASE 0 conclusion

- The extracted ZIP is a complete, buildable-by-CI Android project: all required Gradle files, wrapper,
  app module, native code, tests, workflows, documentation and license assets are present.
- No nested project, no old `.git`, no committed `local.properties`, no real secrets.
- Baseline established; no speculative changes made.
