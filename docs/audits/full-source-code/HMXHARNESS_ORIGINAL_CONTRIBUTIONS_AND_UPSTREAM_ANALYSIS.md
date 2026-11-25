# HMXHARNESS — Original Contributions and Upstream Analysis

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** source-code inspection only. Provenance is established from `LICENSE`,
`README.md`, per-file comments, bundled license notices and `app/build.gradle.kts`.
Old Git commit hashes/counts are **not** used: this repository is a fresh migration
(`README.md:319,355`). No file was modified during this audit.

## 1. Classification key

1. **Original HMXHARNESS** modification/addition
2. **Inherited upstream** AndroidHarness implementation
3. **Third-party** library integration or adapted/vendored component
4. **Experimental / incomplete / test-only**

## 2. Provenance evidence base

| Evidence | Source | Establishes |
| --- | --- | --- |
| Derivative status | `LICENSE:1-9`, `README.md:46,351` | MIT derivative, not a from-scratch rewrite |
| Copyright | `LICENSE:15` = `Copyright (c) 2026 Sanuu7`; `README.md:349` | Upstream authorship retained |
| HMX/upstream/third-party split | `README.md:352`, `README.md:353` | Only explicit in-tree statement of what is HMX |
| Fresh migration | `README.md:319,355` | Do not cite old commits |
| Upstream acknowledgement inside HMX code | `hmx/HmxTaskVerification.kt:9` ("upstream app already records real build/test/lint results"); `data/env/CodeGraphManager.kt:72,467,784`, `CodeGraphPatches.kt:7,514` ("vendored CodeGraph") | Inherited verification evidence; vendored CodeGraph |
| Shizuku-adapted ADB | `data/env/adb/AdbClient.kt:25,178`, `AdbKey.kt:46`, `AdbMdns.kt:19`, `AdbMessage.kt:6`, `AdbPairingClient.kt:21`, `AdbProtocol.kt:3`, `PairingContext.kt:5`; `app/src/main/cpp/adb_pairing.cpp:2`; `assets/licenses/shizuku.txt:1-5` | "Adapted from Shizuku (Apache 2.0)" |
| Caveman adapted | `caveman/CavemanPolicy.kt:15`, `ui/settings/SettingsScreen.kt:2794`, `assets/licenses/caveman.txt:1-5` | Third-party MIT skill (Julius Brussee), adapted |
| Other notices | `assets/licenses/{llama.cpp,backdrop,lucide,dhizuku-api,noto_sans_mono,vulkan-headers}.txt`; `assets/editor-tools/LICENSES.txt` (TypeScript 5.9.3 Apache 2.0) | Third-party inventory |
| Dependencies | `app/build.gradle.kts:242-269` | Library-backed features are integrations |

## 3. Component-by-component classification

| Subsystem | Class | Evidence |
| --- | --- | --- |
| Native single-activity Android architecture | 2 (+1 identity) | `AndroidManifest.xml:114-131` (one `.MainActivity`, `launchMode=singleTask`, `.ShareToChat` alias `:133-144`); `MainActivity.kt:59,98,118`; `app/build.gradle.kts:157-163,69-70` |
| No project-owned backend | 2 | `README.md:44,161,302`; `app/build.gradle.kts:234-236` (WebViewAssetLoader "without any sockets"); no server module in `app/src/main/java` |
| Agent execution loop | 2 | `agent/AgentEngine.kt:296` (`run`), `:463` (`while (true)`), `:842-850`, `:1114-1168`; `agent/RunManager.kt:41-50` |
| Tool registry / core tools | 2 (+1 verdict) + 3 | `tools/Tool.kt:81-216` (~60 base tools `:108-165`, conditional groups `:166-215`); MCP extras at `agent/RunManager.kt:706`; HMX verdict used only at `ui/chat/components/VerificationCard.kt:48` |
| Mobile file editing / editor | 2 + 3 | `README.md:78-79`, `FEATURES.md:97-98`, `tools/FileTools.kt:17-40`, `tools/PatchTools.kt`, `tools/FuzzyEdit.kt`, `tools/FileHistoryTools.kt`; sora-editor `app/build.gradle.kts:265` |
| Linux / tiered shell | 2 + 3 (partly 4) | `data/env/ShellTierRouter.kt:26-33,58,70-79,127-131,204-211`; `LinuxEnvironment.kt:1659-1723,1592-1598,1989`; `tools/ShellPolicy.kt:17-40,713`; `workspace/WorkspaceFs.kt:22-23,95-101`; Shizuku/libsu/Dhizuku/adapted ADB/vendored CodeGraph |
| GitHub cloud builds | 1 on 2 | HMX: `.github/workflows/android-ci.yml:1,61-73,75-97`, `android-release.yml:1,59-78`, `android-visual-qa.yml:1,91-140`, `lint-baseline.yml:1,22-28`, `app/lint-baseline.xml` (`app/build.gradle.kts:140-143`). Upstream: `github/GitHubAuth.kt:135-160,191`; Client ID injection `app/build.gradle.kts:27-35`, `android-ci.yml:30-43` |
| Local GGUF CPU | 3 | `local/LocalNative.kt:5-8` (`System.loadLibrary("harness_local")`, `libggml-cpu.so`); `app/src/main/cpp/CMakeLists.txt:85-90,102` (pinned llama.cpp `19cce691…`) |
| GPU inference | 4 | `app/build.gradle.kts:167-168` excludes `libggml-vulkan.so`, `:280-341` hash-pinned delivery; `local/LocalAccelerationPolicy.kt:11-12`; `data/SettingsRepository.kt:234` default false; `app/src/main/cpp/gpu_unavailable.cpp:1-3` stubs; `FEATURES.md:223,228` |
| Browser automation | 2 + 3 | `browser/BrowserController.kt:167-175,175,216-234,346-350,398-414`; `tools/BrowserTools.kt:71,108,146,429,466,551`; registered `tools/Tool.kt:196-215`; instrumented tests not run by CI |
| MCP | 2 + 3 (live OAuth 4) | `tools/mcp/McpManager.kt:31-36,319,372-376,475`; `McpOAuth.kt:63-79,191` (OAuth 2.1 PKCE, `hmxharness://mcp/oauth`); `AndroidManifest.xml:124-130`; `MainActivity.kt:92-95`; `McpToolAdapter.kt:18-52` |
| Subagents | 2 | `agent/SubagentRuns.kt:21,57,144,172,219,227-239`; `SubagentConfiguration.kt`, `SubagentModelResolver.kt`; `task` tool `tools/Tool.kt:152`, gating `AgentEngine.kt:1596-1608` |
| Memory | 2 | `data/MemoryRepository.kt:10,18,124,146,162-190`; `agent/MemoryNotes.kt`, `MemoryTopics.kt`; tools `Tool.kt:153-155,189-192` |
| Adaptive Liquid Glass | 1 + 3 (visual 4) | HMX: `ui/liquidglass/GlassQuality.kt:17,39-52,59-80,82-83`, `GlassTokens.kt`, `LiquidGlassPalette.kt`, `LiquidGlassComponents.kt`, `LiquidGlassModifiers.kt`. Third-party rendering: `app/build.gradle.kts:266-269`, `assets/licenses/backdrop.txt`; experimental label `FEATURES.md:214`; no pixel assertions `LiquidGlassScreenshotTest.kt:61-62` |

## 4. What genuinely distinguishes HMXHARNESS (defensible claims)

1. **A single integrated on-device stack** — agent + tools + shell + editor + WebView
   + local inference, no project backend (`README.md:44,161,302`;
   `AndroidManifest.xml:114-131`). *This integration is the upstream design; HMX
   maintains and rebrands it.*
2. **HMX-authored verification/approval artifacts** — `hmx/HmxTaskVerification.kt:14-52`
   and the Connected Accounts card — but **display/test-only** (no runtime caller for
   `canClaimCompletion()`/`HmxApprovalPolicy`).
3. **Explicit provenance and honesty layer** — `LICENSE:1-15`, `README.md:347-355`,
   per-file adaptation comments, `assets/licenses/*`, `app/lint-baseline.xml`.
4. **Cloud-build/verification infrastructure** — four GitHub Actions workflows and a
   lint baseline (`README.md:289-292`).
5. **Adaptive glass configuration** on device capability (`GlassQuality.kt:39-80`) over
   third-party renderers.

## 5. Claims that must NOT be made

- "World's first mobile coding agent" — no market survey; upstream AndroidHarness exists.
- "All code original" — `README.md:351` states the opposite.
- "Fully offline AI" — contradicted by `README.md:325,327`; only CPU GGUF is offline.
- "GPU verified" — experimental, default-off, no hardware benchmark.
- "Verification gate enforced" — `canClaimCompletion()` has no runtime caller.

## 6. Limitations of this analysis

- Per-file upstream headers are rare; classification relies on the explicit
  project-level statements in `README.md:351-353`, in-file adaptation comments and
  license notices. Some inherited files may not carry individual attribution.
- The fresh migration removed old Git history, so commit-level authorship evidence
  cannot be reproduced here.
- No third-party dependency was audited for its own licensing beyond bundled notices.
