# HMXHARNESS — Complete Technology Stack

> Versions verified from `gradle/libs.versions.toml`, `app/build.gradle.kts`,
> `settings.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties` and
> `app/src/main/cpp/CMakeLists.txt` in the current repository. Build outcomes
> are from CI run `45338701934` on commit `ae41ac7`. Purpose statements describe
> how each dependency is actually used here, not generic marketing.

## 1. Languages and code footprint

| Language | Use | Evidence |
|---|---|---|
| Kotlin 2.4.10 | App, agent, tools, UI, data, networking | `gradle/libs.versions.toml`; 1,231 Kotlin files total (732 `app/src/main`, 352 `app/src/test`, 140 `app/src/androidTest`) |
| C++17 / C | Local inference, PTY, ADB pairing, device telemetry | `app/src/main/cpp/CMakeLists.txt` (`CMAKE_CXX_STANDARD 17`); 12 files under `app/src/main/cpp/` |
| XML | Manifest and Android resources | `app/src/main/res/`; 1,009 XML files repository-wide |
| Kotlin DSL | Gradle build scripts | `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `local_gpu/build.gradle.kts` |
| Other | 5 Java, 7 JS/MJS, 3 Python, 5 shell scripts | repository measurement; JS includes `app/src/main/assets/editor-tools/` |

## 2. Android and build toolchain

| Component | Version / value | Purpose | Evidence |
|---|---|---|---|
| Gradle wrapper | **9.7.0** | Build execution; executable bit restored to Git mode `100755` in commit `ae41ac7` | `gradle/wrapper/gradle-wrapper.properties`, `gradlew` |
| Android Gradle Plugin | **9.3.1** | App build | `gradle/libs.versions.toml` |
| Kotlin | **2.4.10** | Language and Compose plugin | `gradle/libs.versions.toml` |
| KSP | **2.3.11** | Room annotation processing | `gradle/libs.versions.toml` |
| JDK | **21 (Temurin)** in CI | Build and test runtime | `.github/workflows/android-ci.yml`, run `45338701934` |
| NDK | **27.2.12479018** (pinned) | Native build; installed explicitly in CI | `app/build.gradle.kts` (`hasNdk`), `.github/workflows/android-ci.yml` |
| CMake | **3.22.1** | Native build | `app/build.gradle.kts` |
| ABIs | `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` | Native coverage when NDK is present | `app/build.gradle.kts` (`abiFilters`) |
| Application ID | `com.hmx.harness` (debug `.debug`) | Identity | `app/build.gradle.kts` |
| Version | `versionCode 40`, `versionName "4.0"` | Release identity | `app/build.gradle.kts` |
| SDK targets | `compileSdk 37`, `targetSdk 36`, `minSdk 26` | Platform support | `app/build.gradle.kts` |
| Signing | Release uses the **debug** signing config | Installable alpha, not Play-ready | `app/build.gradle.kts` (`signingConfig = signingConfigs.getByName("debug")`) |
| Dynamic feature | `:local_gpu` only with `-Pdistribution=play` | Optional GPU runtime delivery | `settings.gradle.kts`, `local_gpu/` |

`app/build.gradle.kts` computes `hasNdk` and **omits the native block** when the
pinned NDK is not installed, so a Kotlin-only local build still produces a
working APK. A no-NDK build is not feature-identical to the CI artifact for
local inference.

## 3. UI

| Library | Version | Purpose | Evidence |
|---|---|---|---|
| Compose BOM | **2026.08.00** | All UI | `gradle/libs.versions.toml` |
| Material 3 (Expressive) | **1.5.0-alpha26** | Design system | `gradle/libs.versions.toml` |
| Material Icons Extended | BOM-managed | Icons | `gradle/libs.versions.toml` |
| Navigation Compose | 2.9.8 | Single-activity navigation | `ui/AppNav.kt` |
| Lifecycle Compose / ViewModel | 2.9.4 | State holders | `ui/chat/ChatViewModel.kt` |
| Activity Compose | 1.12.4 | `setContent`, edge-to-edge | `MainActivity.kt` |
| **PrismalAGSL** | 1.0.7 | Backdrop blur, refraction, specular glass rendering | `gradle/libs.versions.toml`, `ui/liquidglass/LiquidGlassModifiers.kt` |
| **kyant0 Backdrop** | 2.0.1 | Ambient backdrop layer | `gradle/libs.versions.toml`, `ui/liquidglass/LiquidGlassComponents.kt` |
| sora-editor | 0.23.6 | In-app code editor | `gradle/libs.versions.toml`, `ui/files/` |

The Liquid Glass **material system** (tokens, palette, quality tiers, wrappers)
is HMX code (`ui/liquidglass/`); the GPU primitives come from PrismalAGSL and
Backdrop. Do not describe PrismalAGSL as custom-built. Tier resolution is
unit-tested (`app/src/test/java/com/hmx/harness/ui/theme/HarnessGlassModeTest.kt`),
but the instrumented
`app/src/androidTest/java/com/hmx/harness/ui/liquidglass/LiquidGlassScreenshotTest.kt`
has no pixel assertions and logs failures instead of failing, so visual
correctness is **not** gated automatically.

## 4. Data and storage

| Component | Version | Purpose | Evidence |
|---|---|---|---|
| Room | 2.8.4 | Primary database: 14 entities, schema v27, 23 migrations; plus a separate debug-log database | `data/db/Database.kt` (`@Database` at `:832`) |
| DataStore Preferences | 1.2.1 | Settings persistence | `data/SettingsRepository.kt` |
| WorkManager | 2.10.5 | Scheduled automations and background work | `automation/AutomationWorker.kt` |
| AndroidX Security Crypto | 1.1.0 | `EncryptedSharedPreferences` with Keystore `AES256_GCM` | `data/KeyStoreManager.kt` |
| Biometric | 1.2.0-alpha05 | Approval for Full access | `ui/common/BiometricAuth.kt` |
| DocumentFile | 1.1.0 | SAF workspace folders | `workspace/` |

## 5. Networking

| Component | Version | Purpose | Evidence |
|---|---|---|---|
| OkHttp | 4.12.0 | Provider, GitHub, search and supporting HTTP traffic | `llm/Llm.kt` (`ProviderFactory.client`) |
| OkHttp SSE | 4.12.0 | Streaming model responses | `gradle/libs.versions.toml`, `llm/*Provider.kt` |
| kotlinx.serialization JSON | 1.9.0 | Wire models, config, tool schemas | `llm/Llm.kt`, `core/` |
| kotlinx.coroutines | 1.10.2 | Concurrency and `Flow<StreamEvent>` streaming | throughout `app/src/main` |
| Conscrypt | 2.8.0 | TLS provider | `app/build.gradle.kts` |
| BouncyCastle (bcprov / bcpkix) | 1.85 | Ed25519 SSH keys, self-signed ADB certificates | `app/build.gradle.kts` |
| JSch | 2.28.7 | SSH workspaces / SFTP | `app/build.gradle.kts` |

There is no HMX proxy or backend: requests go directly from the device to the
configured endpoint (`llm/Llm.kt`). No analytics or crash-reporting SDK is
present (`README.md`, `PRIVACY.md`).

## 6. AI model providers and APIs

`llm/` implements four wire protocols plus special providers:

| Provider path | Class | Notes |
|---|---|---|
| OpenAI-compatible `/chat/completions` | `llm/OpenAiCompatProvider.kt` | Default for most endpoints |
| OpenAI Responses `/v1/responses` | `llm/OpenAiResponsesProvider.kt` | Responses API |
| Anthropic `/v1/messages` | `llm/AnthropicProvider.kt` | Messages API |
| Gemini `:streamGenerateContent` | `llm/GeminiProvider.kt` | Gemini API |
| ChatGPT account | `chatgpt/` (`ChatGptProtocol`) | Account-based connection |
| Built-in free `harness` | `llm/HarnessProvider.kt`, `HarnessFreeLimit.kt` | Routes to third-party public gateways (`opencode.ai/zen`, `api.kilo.ai`, `text.pollinations.ai`); not an HMX-hosted model and not guaranteed free |
| Local GGUF | `local/LocalModelProvider.kt`, `local/LocalNative.kt` | llama.cpp JNI; CPU shipped, GPU opt-in experimental |
| Fallback / key rotation | `llm/FallbackProvider.kt`, `llm/RotatingKeyProvider.kt` | Cross-provider fallback and multiple keys |

Provider selection is `ProviderFactory.create` (`llm/Llm.kt:208`, `:224`). Model
metadata and spend estimation live in `llm/ModelCatalog.kt`, `llm/ModelPrices.kt`
and `llm/ModelsDev.kt`. 9Router local routing lives in `router/NineRouterManager.kt`
and `router/NineRouterNodeRuntime.kt`.

### Local inference detail

- `local/LocalNative.kt` loads `libharness_local`; implementation is in
  `app/src/main/cpp/local_model.cpp`, `local_chat.cpp` and `safetensors_import.cpp`.
- `app/src/main/cpp/CMakeLists.txt` fetches llama.cpp; 32-bit ABIs receive
  `local_unavailable.cpp` stubs.
- GPU execution requires `SettingsRepository.localModelExperimental = true`
  (default `false`, `data/SettingsRepository.kt:234`) and the policy in
  `local/LocalAccelerationPolicy.kt` (`useGpu(experimental, preference, available)`).
  It is **not** a default or hardware-verified capability.

## 7. Browser and execution

| Component | Purpose | Evidence |
|---|---|---|
| Android WebView + WebKit | Navigate, click, type, scroll, eval JS, screenshot | `browser/BrowserController.kt` |
| `WebViewAssetLoader` | Serve workspace files on a stable origin | `browser/WorkspacePathHandler.kt`, `app/build.gradle.kts` |
| Android JavaScriptEngine | JS execution for previews | `app/build.gradle.kts` |
| CEL | Validate per-model reasoning request mappings on-device | `app/build.gradle.kts` (`dev.cel:cel`) |
| LLVM strip | Strip native libraries at build time | `app/build.gradle.kts` (`PrepareGpuRuntime`) |

## 8. Device access

| Component | Version | Purpose | Evidence |
|---|---|---|---|
| Shizuku API / provider | 13.1.5 | Privileged shell without root | `data/env/ShizukuManager.kt` |
| libsu core / service | 6.0.0 | Root shell | `data/env/RootManager.kt` |
| Dhizuku API | 2.6.0 | Device-owner-style control | `data/env/DhizukuManager.kt` |
| Wireless ADB | in-repo + BoringSSL | Pairing (SPAKE2) and shell | `data/env/adb/`, `app/src/main/cpp/adb_pairing.cpp` (ADB files note adaptation from Shizuku, Apache 2.0) |
| Termux packages | remote repositories | Bash, Git, Python, Node with SHA-256 verification and mirrors | `data/env/LinuxEnvironment.kt`, `data/env/PackageMirrors.kt` |
| Shell tier router | in-repo | `TERMUX_SSH`, `ROOT`, `PRIVILEGED`, `APP_LINUX`, `TOYBOX` | `data/env/ShellTierRouter.kt` |

## 9. GitHub integration

- `github/GitHubAuth.kt` — OAuth App **Device Flow** (`/login/device/code`,
  `/login/oauth/access_token`), scopes `repo offline_access` (plus `workflow`
  when enabled), refresh via `offline_access`, PAT fallback. **Authorship:** the
  flow is inherited upstream; HMX added the Settings card
  (`ui/settings/ConnectedAccountsSection.kt`) and the CI Client-ID injection.
- `github/GitHubRepository.kt` — repository import/browse;
  `github/GitHubPullRequests.kt` — PR publishing; `github/GitChangeTree.kt` —
  change tracking; `tools/GitHubTools.kt` and `tools/GitHubPullRequestTool.kt` —
  agent tools.
- Client ID is injected at build time from `local.properties` or the
  `HMX_GITHUB_CLIENT_ID` Actions variable (`app/build.gradle.kts`,
  `.github/workflows/android-ci.yml`). **That variable is not currently set**:
  CI emits a warning and continues; Device Flow sign-in is disabled in the
  current artifact, PAT sign-in works, and no client secret was introduced.

## 10. CI/CD and deployment

| Workflow | Trigger | Purpose |
|---|---|---|
| `.github/workflows/android-ci.yml` | push to `main` / `feature/**`, PR, dispatch | Wrapper validation → JDK 21 → pinned NDK install → unit tests → lintDebug → assembleDebug + assembleRelease → APK verification → artifact upload |
| `.github/workflows/android-release.yml` | `v*` tags, dispatch | Release APK artifact |
| `.github/workflows/android-visual-qa.yml` | dispatch | Cloud-emulator screenshot / performance capture; not a gating visual test |
| `.github/workflows/lint-baseline.yml` | dispatch/maintenance | Lint baseline generation |

Current evidence:

- Run **`45338701934`** for `ae41ac7` completed with conclusion **success**;
  every step above passed.
- Artifact **`hmxharness-apks`**, id **`14898111209`**, size **110,461,001 bytes**,
  not expired. Authenticated download:
  `https://api.github.com/repos/HMXPANEL/HMXHARNESS/actions/artifacts/14898111209/zip`.
- An earlier run **`45337564227`** failed with `./gradlew: Permission denied`
  (exit 126) because ZIP extraction lost the executable bit; fixed by restoring
  Git mode `100755` on `gradlew` in `ae41ac7`.
- Runner: `ubuntu-latest`, Temurin JDK 21, pinned NDK 27.2.12479018, `glslc`,
  Gradle caching via `gradle/actions/setup-gradle@v4`. Workflow timeout is 90
  minutes with concurrency cancellation. CI **does not run instrumentation
  tests**.

## 11. Testing

| Layer | Technology | Count / status |
|---|---|---|
| Unit tests | JUnit 4 | **2,478** `@Test` methods across 352 Kotlin files in `app/src/test`; executed and passing in CI run `45338701934` |
| Instrumented tests | AndroidX Test runner | **555** `@Test` methods across 140 Kotlin files in `app/src/androidTest`; exist but **not run by CI** |
| Lint | Android lint with `app/lint-baseline.xml` | `lintDebug` passes; inherited findings frozen in the baseline |
| Visual QA | Emulator screenshot capture | Screenshots captured; `LiquidGlassScreenshotTest.kt` has no pixel assertions and suppresses failures |

All 23 investigated features have both source implementations and automated
test files; that is "implemented with automated test coverage", **not** the same
as hardware-verified. See `HMXHARNESS_FEATURE_VERIFICATION.md`.

## 12. Dependencies not present (do not claim)

- No TensorFlow/PyTorch training stack and no model training code.
- No Firebase, analytics, advertising or crash-reporting SDK.
- No custom backend server: provider calls originate on the device.
- No Ktor, Retrofit, Hilt/Dagger or Compose Multiplatform.
