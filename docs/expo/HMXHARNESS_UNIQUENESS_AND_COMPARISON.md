# HMXHARNESS — Uniqueness and Comparison

> **Rule for this document:** HMXHARNESS is **not** claimed to be the first or
> only product with any feature. Competitor characteristics are general public
> knowledge and may change; only HMXHARNESS claims are verified from this
> repository. HMXHARNESS is an MIT-licensed derivative of AndroidHarness by
> Sanuu7 — that upstream name appears here only as attribution, never as a
> product identity for this app. Provenance is per component: explicit upstream
> headers are rare, some ADB files are adapted from Shizuku (Apache 2.0),
> CodeGraph is vendored with HMX patches, and the Caveman skill is adapted from
> Julius Brussee's MIT work (`app/src/main/assets/licenses/caveman.txt`).

## 1. What is verifiably distinctive about the combination

No single capability below is unique. The **combination** on one Android phone
is what a Project Expo can defend; every row cites a real file.

| Axis | HMXHARNESS evidence | Why it matters |
|---|---|---|
| Native Android coding agent | `MainActivity.kt` (single Compose activity), `agent/AgentEngine.kt` (`run` :296, loop :463) | Not a desktop CLI or a web wrapper |
| On-device shell with tiered privilege | `data/env/ShellTierRouter.kt` (`TOYBOX`, `APP_LINUX`, `PRIVILEGED`, `ROOT`, `TERMUX_SSH`), `tools/ShellTool.kt`, `tools/ShellPolicy.kt` | Real command execution without a PC; tier behaviour not hardware-verified |
| Linux toolchain bootstrap | `data/env/LinuxEnvironment.kt`, `data/env/PackageMirrors.kt` | Bash, Git, Python, Node installed with SHA-256 verification |
| On-device GGUF (CPU) | `local/LocalNative.kt`, `app/src/main/cpp/local_model.cpp`, `local/LocalModelProvider.kt` | Local inference path shipped; **GPU is opt-in experimental** (`local/LocalAccelerationPolicy.kt`, default `false` in `data/SettingsRepository.kt:234`) |
| GitHub Device Flow, no client secret | `github/GitHubAuth.kt`; scopes `repo offline_access`, refresh via `offline_access` | No auth backend or private key (flow inherited upstream; HMX added `ui/settings/ConnectedAccountsSection.kt` and CI wiring). Variable not set in current builds |
| Recorded verification evidence | `agent/VerificationEvidence.kt` (`PASSED/FAILED/UNKNOWN`, `STALE` after edits), `ui/chat/components/VerificationCard.kt` | Checks are visible and stale-marked — **but no runtime completion gate** (see §4) |
| Custom Liquid Glass material with adaptive tiers | `ui/liquidglass/GlassQuality.kt` (`GlassTier`, `detectGlassTier`), `GlassTokens.kt`, `LiquidGlassModifiers.kt` | One identity across low/mid/high devices; tier resolution unit-tested, visual pixel correctness **not** gated |
| In-app project context | `config/HarnessConfig.kt`, `skills/SkillStore.kt`, `tools/mcp/McpManager.kt`, `data/MemoryRepository.kt` | Project instructions, skills, MCP servers and memory travel with the workspace |
| 21-language UI | `app/src/main/res/values-*` (21 locales), 1,129 strings in `values/strings.xml` | Broad accessibility (locales inherited upstream) |

## 2. Comparison with related tools

Legend: ✅ documented capability · ◐ partial/conditional · — not a focus ·
? unverified here. "Documented" for other tools means public product
documentation, not testing in this audit.

| Capability | HMXHARNESS | AndroidHarness (upstream) | Claude Code | OpenCode | Cline / Roo Code | Aider | Termux |
|---|---|---|---|---|---|---|---|
| Primary platform | **Android phone** | Android phone | Desktop terminal | Desktop terminal / TUI | VS Code extension | Desktop terminal | Android terminal |
| Native mobile UI | ✅ Compose | ✅ | — | — | — | — | ◐ terminal UI |
| Agent edits files | ✅ `tools/FileTools.kt` | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| Runs shell on device | ✅ tiered (`ShellTierRouter.kt`) | ✅ tiered | ✅ host shell | ✅ host shell | ◐ host shell | ✅ host shell | ✅ |
| On-device local LLM | ✅ GGUF CPU; GPU opt-in experimental | ✅ | — | ◐ local endpoints | ◐ local endpoints | ◐ local endpoints | ◐ packages |
| GitHub Device Flow, no client secret | ✅ `github/GitHubAuth.kt` (upstream flow + HMX UI/CI) | ✅ | ◐ browser login | ◐ | ◐ | — | — |
| Linux toolchain bootstrap on Android | ✅ `LinuxEnvironment.kt` | ✅ | — | — | — | — | ✅ |
| Adaptive glass material | ✅ custom tiers | ◐ base | — | — | — | — | — |
| MCP support | ✅ `tools/mcp/McpManager.kt` | ✅ | ✅ | ✅ | ✅ | ◐ | — |
| Web research tools | ✅ keyed + keyless providers | ✅ | ◐ | ◐ | ◐ | ◐ | — |
| Automated tests present | ✅ 2,478 unit + 555 instrumented methods | ✅ (upstream) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Cloud CI producing APK artifacts | ✅ run `45338701934` | ◐ varies | — | ✅ | ✅ | ✅ | ◐ |
| Open source | ✅ MIT derivative | ✅ MIT | ◐ client | ✅ | ✅ | ✅ | ✅ |

> This table compares **focus areas**, not overall superiority. Desktop agents
> have mature ecosystems HMXHARNESS does not have. Do not claim HMXHARNESS is
> "better" overall, and do not claim performance advantages: no benchmark was
> run and no device measurement exists.

## 3. Honest differentiators (safe to say)

1. **"A coding agent that runs natively on an Android phone, with a real
   tiered shell and optional local GGUF models."** Verifiable from
   `agent/AgentEngine.kt`, `data/env/ShellTierRouter.kt`, `local/LocalNative.kt`;
   device behaviour not hardware-verified.
2. **"GitHub sign-in with Device Flow, so no client secret or auth server is
   needed."** Verifiable from `github/GitHubAuth.kt`. State that the flow is
   inherited upstream and HMX added the Settings card and CI Client-ID wiring;
   also state the variable is currently unset.
3. **"Build/test/lint outcomes are recorded and marked `STALE` after edits, and
   the chat shows a verification card."** Verifiable from
   `agent/VerificationEvidence.kt` and `ui/chat/components/VerificationCard.kt`.
4. **"A custom Liquid Glass material with three device-capability tiers."**
   Verifiable from `ui/liquidglass/GlassQuality.kt`; say "tier resolution is
   unit-tested", not "visual correctness is verified".
5. **"An MIT-licensed derivative that adds cloud CI producing real APKs, its own
   identity, verification-display objects and documentation."** Verifiable from
   `LICENSE`, `README.md`, `.github/workflows/android-ci.yml`, run `45338701934`.

## 4. Claims that must NOT be made

| Claim | Why it is unsafe |
|---|---|
| "First/only AI coding agent on Android" | No exhaustive market survey; upstream and other tools exist. |
| "We built / trained an AI model" | No training code; models come from providers or llama.cpp. |
| "Fully offline AI coding" | Cloud providers are the default path; local GGUF needs a model download and the GPU path is opt-in experimental. |
| "Works on every Android phone" | `minSdk 26`; 32-bit ABIs get inference stubs; privileged shell needs Shizuku/root/ADB setup. |
| "Production-ready / Play-ready signing" | Release uses the debug signing config (`app/build.gradle.kts`); it is an installable alpha. |
| "Zero security issues / third-party audited" | `SECURITY.md` documents cleartext traffic, plaintext MCP headers and other findings; no third-party audit exists. |
| "Faster than Claude Code / Cline / Aider" | No benchmark; no on-device performance measurement. |
| "Custom-built llama.cpp / Shizuku / sora-editor / PrismalAGSL" | Third-party components; ADB files are adapted from Shizuku (Apache 2.0). |
| "The whole codebase is our original work" | It is an MIT derivative; upstream authorship and notices are preserved in `LICENSE` and `README.md`. |
| "All engine code is upstream" | Provenance is per component and explicit headers are rare; do not overstate either direction. |
| "HMX verification/approval gate blocks uncompleted work" | `canClaimCompletion()` and `HmxApprovalPolicy` in `hmx/HmxTaskVerification.kt` are unit-test-only, with no runtime caller. Real gates: `PermissionMode`, `RunManager.approve/deny`, `tools/ShellPolicy.kt`. |
| "Visual correctness is automatically verified" | `LiquidGlassScreenshotTest.kt` has no pixel assertions and suppresses failures; visual QA captures images without gating. |
| "Vulkan GPU inference is shipped and verified" | GPU requires `localModelExperimental = true` and is not hardware-verified. |
| "Live GitHub OAuth verified end-to-end" | Needs human approval; only flow logic is unit-tested, and the Client-ID variable is unset. |
| "We wrote the Device Flow / the 21 translations" | Both are inherited upstream; HMX added the Settings card, CI wiring and documentation. |
| "Git history proves our authorship" | This is a fresh migration; the old repository's history was not carried over. |

## 5. Suggested positioning statement

> "HMXHARNESS is an open-source Android-native AI coding agent built on the
> MIT-licensed AndroidHarness. It runs the agent, a tiered Linux toolchain, Git
> and optional on-device GGUF models on the phone; records build and test
> evidence in chat; supports GitHub Device Flow sign-in without a client secret
> (flow inherited from upstream, with HMX-added UI and CI wiring); and ships a
> custom adaptive Liquid Glass material. CPU local inference is the shipped
> path, GPU is opt-in experimental, and device-dependent behaviour is
> implemented with automated test coverage but has not been hardware-verified
> in this repository."

This statement is supported by files cited in this document set.
