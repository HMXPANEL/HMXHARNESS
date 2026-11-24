# HMXHARNESS — Complete Technology Stack

> Versions and dependencies verified from `gradle/libs.versions.toml`,
> `app/build.gradle.kts`, `settings.gradle.kts` and `app/src/main/cpp/CMakeLists.txt`
> at `main` (`22e674e9`; later commits are documentation only). Purpose statements describe how each dependency is used
> in this repository, not generic marketing.

## 1. Languages

| Language | Use | Evidence |
|---|---|---|
| **Kotlin 2.4.10** | Entire app: UI, agent, tools, data, networking | `libs.versions.toml` (`kotlin = "2.4.10"`); 732 Kotlin files in `app/src/main/java` |
| **C++17 / C** | Local LLM inference, PTY, ADB pairing, device telemetry | `app/src/main/cpp/CMakeLists.txt` (`CMAKE_CXX_STANDARD 17`); 11 files in `app/src/main/cpp/` |
| **XML** | Android resources, manifest, layouts (1,001 XML files) | `app/src/main/res` |
| **Kotlin DSL** | Gradle build scripts | `app/build.gradle.kts`, `settings.gradle.kts` |

## 2. Android & build

| Component | Version / value | Purpose | Evidence |
|---|---|---|---|
| Android Gradle Plugin | 9.3.1 | App build | `libs.versions.toml` |
| Gradle wrapper | 9.7.0 | Build execution | `gradle/wrapper/gradle-wrapper.properties` |
| KSP | 2.3.11 | Room annotation processing | `libs.versions.toml` |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 | Platform targets | `app/build.gradle.kts` |
| NDK | 27.2.12479018 | Native build; optional (Kotlin-only build if absent) | `app/build.gradle.kts` (`hasNdk`) |
| CMake | 3.22.1 | Native build | `app/build.gradle.kts` |
| ABIs | `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` | APK native coverage | `app/build.gradle.kts` |
| Application ID | `com.hmx.harness` (debug `.debug`) | Identity | `app/build.gradle.kts` |
| Dynamic feature | `:local_gpu` (only for `-Pdistribution=play`) | Optional GPU runtime delivery | `settings.gradle.kts`, `local_gpu/` |

## 3. UI

| Library | Version | Purpose | Evidence |
|---|---|---|---|
| Jetpack Compose BOM | 2026.08.00 | All UI | `libs.versions.toml` |
| Material 3 (Expressive) | 1.5.0-alpha26 | Design system components | `libs.versions.toml` |
| Material Icons Extended | (BOM) | Icons | `libs.versions.toml` |
| Navigation Compose | 2.9.8 | Single-activity navigation (21 `composable(...)` destinations) | `ui/AppNav.kt` |
| Lifecycle Compose / ViewModel | 2.9.4 | State holders | `ui/chat/ChatViewModel.kt` |
| Activity Compose | 1.12.4 | `setContent`, edge-to-edge | `MainActivity.kt` |
| **PrismalAGSL** | 1.0.7 | Real backdrop sampling, blur, AGSL refraction/specular for Liquid Glass | `libs.versions.toml`, `ui/liquidglass/LiquidGlassModifiers.kt` |
| **kyant0 Backdrop** | 2.0.1 | Ambient backdrop layer | `libs.versions.toml`, `LiquidGlassComponents.kt` |
| sora-editor | 0.23.6 | In-app code editor (gutter, undo/redo, search) | `libs.versions.toml`, `ui/files/` |

> The Liquid Glass **material system** (tokens, palette, quality tiers, wrappers)
> is custom code; the GPU rendering primitives come from PrismalAGSL/Backdrop.
> Do not describe PrismalAGSL as custom-built.

## 4. Data & storage

| Component | Version | Purpose | Evidence |
|---|---|---|---|
| Room | 2.8.4 | Local database, 14 entities, schema version 27, 23 migrations | `data/db/Database.kt` |
| DataStore Preferences | 1.2.1 | Settings persistence | `data/SettingsRepository.kt` |
| WorkManager | 2.10.5 | Scheduled automations, background work | `automation/AutomationWorker.kt` |
| AndroidX Security Crypto | 1.1.0 | `EncryptedSharedPreferences`, Keystore `AES256_GCM` | `data/KeyStoreManager.kt` |
| Biometric | 1.2.0-alpha05 | Approval for Full access | `ui/common/BiometricAuth.kt` |
| DocumentFile | 1.1.0 | SAF workspace folders | `workspace/` |

## 5. Networking

| Component | Version | Purpose | Evidence |
|---|---|---|---|
| OkHttp | 4.12.0 | All HTTP/SSE provider traffic, GitHub, search, browser support | `llm/Llm.kt` (`ProviderFactory.client`) |
| OkHttp SSE | 4.12.0 | Streaming model responses | `libs.versions.toml`, provider classes |
| kotlinx.serialization JSON | 1.9.0 | Wire models, config, tool schemas | `llm/`, `core/` |
| kotlinx.coroutines | 1.10.2 | Concurrency, Flow-based streaming | throughout |
| Conscrypt | 2.8.0 | TLS provider | `app/build.gradle.kts` |
| BouncyCastle (bcprov/bcpkix) | 1.85 | Ed25519 SSH keys, self-signed ADB certs | `app/build.gradle.kts` |
| JSch | 2.28.7 | SSH workspaces / SFTP | `app/build.gradle.kts` |

## 6. AI model providers and APIs

`llm/` implements four wire protocols plus special providers:

| Provider path | Class | Notes |
|---|---|---|
| OpenAI-compatible (`/chat/completions`) | `OpenAiCompatProvider.kt` | Default for most endpoints |
| OpenAI Responses (`/v1/responses`) | `OpenAiResponsesProvider.kt` | Responses API |
| Anthropic (`/v1/messages`) | `AnthropicProvider.kt` | Anthropic Messages API |
| Google Gemini (`:streamGenerateContent`) | `GeminiProvider.kt` | Gemini API |
| ChatGPT account | `chatgpt/` (`ChatGptProtocol`) | Account-based connection |
| Built-in free "Harness" | `llm/HarnessProvider.kt` | Routes to free public gateways (`opencode.ai/zen`, `api.kilo.ai`, `text.pollinations.ai`); **not a model hosted by HMX** |
| Local GGUF | `local/LocalModelProvider.kt`, `local/LocalNative.kt` | llama.cpp JNI; CPU + optional Vulkan GPU |
| Fallback / key rotation | `llm/FallbackProvider.kt`, `RotatingKeyProvider.kt` | Fallback chains, multiple keys |

Provider selection: `llm/Llm.kt` (`ProviderFactory.create`), model catalog and
pricing helpers in `ModelCatalog.kt`, `ModelPrices.kt`, `ModelsDev.kt`.
9Router local routing: `router/NineRouterManager.kt`, `router/NineRouterNodeRuntime.kt`.

## 7. Browser & execution

| Component | Purpose | Evidence |
|---|---|---|
| Android WebView + WebKit | Agent browser: navigate, click, type, scroll, eval JS, screenshot, DOM | `browser/BrowserController.kt` |
| `WebViewAssetLoader` | Serve workspace files over a stable https origin without sockets | `app/build.gradle.kts` comment; `browser/WorkspacePathHandler.kt` |
| Android JavaScriptEngine | In-app JS execution for previews | `app/build.gradle.kts` |
| CEL | Validates per-model reasoning request mappings on-device | `app/build.gradle.kts` (`dev.cel:cel`) |
| LLVM strip | Strips native libs at build time | `app/build.gradle.kts` (`PrepareGpuRuntime`) |

## 8. Device access

| Component | Version | Purpose | Evidence |
|---|---|---|---|
| Shizuku API/provider | 13.1.5 | Privileged shell without root | `data/env/ShizukuManager.kt` |
| libsu core/service | 6.0.0 | Root shell | `data/env/RootManager.kt` |
| Dhizuku API | 2.6.0 | Device-owner-style control | `data/env/DhizukuManager.kt` |
| Wireless ADB | in-repo + BoringSSL | ADB pairing (SPAKE2) and shell | `data/env/adb/`, `app/src/main/cpp/adb_pairing.cpp` |
| Termux packages | remote repository | Bash, Git, Python, Node toolchain, SHA-256 verified | `data/env/LinuxEnvironment.kt`, `data/env/PackageMirrors.kt` |

## 9. GitHub integration

- `github/GitHubAuth.kt` — OAuth App **Device Flow**
  (`/login/device/code`, `/login/oauth/access_token`), scopes
  `repo offline_access` (plus `workflow` when enabled), refresh via
  `offline_access`, PAT fallback. **Authorship:** this flow was added upstream
  (`2207914579`, 2026-10-09); HMX added the connection card in Settings and the
  CI Client-ID injection.
- `ui/settings/ConnectedAccountsSection.kt` — HMX-added Settings card for the
  device code, URL, busy/polling state and PAT fallback (`e169ef40`).
- `github/GitHubRepository.kt` — repository import/browse.
- `github/GitHubPullRequests.kt` — pull-request publishing.
- `github/GitChangeTree.kt` — change tracking for commits.
- `tools/GitHubTools.kt`, `tools/GitHubPullRequestTool.kt` — agent tools.
- Client ID injected at build time from `local.properties` or
  `HMX_GITHUB_CLIENT_ID` (`app/build.gradle.kts`, `.github/workflows/*`).

## 10. CI/CD and deployment

| Workflow | Trigger | Purpose |
|---|---|---|
| `.github/workflows/android-ci.yml` | push to `main`/`feature/**`, PR, dispatch | unit tests → lint → debug+release APKs → verify → upload |
| `.github/workflows/android-release.yml` | `v*` tags, dispatch | release APK artifact |
| `.github/workflows/android-visual-qa.yml` | dispatch, selected feature branch | cloud-emulator screenshots and performance profiles |

CI toolchain: `ubuntu-latest`, Temurin **JDK 21**, NDK 27.2.12479018, `glslc`,
Gradle caching. Artifacts: `hmxharness-apks` (debug + release). Secrets:
`HMX_GITHUB_CLIENT_ID` (public OAuth Client ID; photographed as a secret but not a
real secret). No analytics or advertising SDK is present (`README.md`,
`PRIVACY.md`).

## 11. Testing

| Layer | Technology | Count |
|---|---|---|
| Unit tests | JUnit 4 | 2,478 `@Test` methods (`app/src/test`, 351 files) |
| Instrumentation | AndroidX Test runner | 555 `@Test` methods (`app/src/androidTest`, 139 files) — not run in CI |
| Lint | Android lint with baseline | 0 errors after recovery |

## 12. Dependencies not present (do not claim)

- No TensorFlow/PyTorch training stack; no model training code.
- No Firebase/analytics/crash-reporting SDK.
- No custom backend server; provider calls go directly from the device.
- No Ktor, Retrofit, Hilt/Dagger, or Compose Multiplatform.
