# HMXHARNESS — Uniqueness and Comparison

> **Rule for this document:** HMXHARNESS is **not** claimed to be the first or
> only product with any feature. Competitor characteristics are general public
> knowledge as of this audit and may change; only HMXHARNESS claims are verified
> from this repository.

## 1. What is verifiably distinctive about this combination

The individual capabilities are not novel. The **combination** below is what a
Project Expo can defend:

| Axis | HMXHARNESS evidence | Why it matters |
|---|---|---|
| **Native Android coding agent** | Single-activity Compose app, `MainActivity.kt`; agent runs in `AgentEngine` on the device | Not a desktop CLI, not a web wrapper |
| **On-device shell with tiered privilege** | `ShellTierRouter` (`TOYBOX`, `APP_LINUX`, `PRIVILEGED`/Shizuku, `ROOT`, `TERMUX_SSH`); `LinuxEnvironment.kt` bootstraps Termux bash/git/python/node | Real command execution without a PC |
| **On-device GGUF inference** | `local/LocalNative.kt` + `app/src/main/cpp/local_model.cpp`; llama.cpp fetched by CMake; optional Vulkan | Can work without cloud model calls |
| **GitHub Device Flow, no client secret** | `github/GitHubAuth.kt`; scopes `repo offline_access`; refresh via `offline_access` | No auth backend, no private key (flow inherited from upstream; HMX added the Settings card and CI wiring) |
| **Verification-first completion** | `VerificationEvidence` + `HmxTaskVerification` | The agent cannot claim success from empty/stale evidence |
| **Custom Liquid Glass material with adaptive tiers** | `ui/liquidglass/*`, `GlassQuality.detectGlassTier` | Same identity on low/mid/high devices |
| **21-language UI** | 21 `values-*` directories (upstream); localization tests | Broad accessibility |
| **In-app project context** | `config/HarnessConfig.kt` (instructions, skills, agents, MCP, hooks, memory) | Project-specific behaviour, not just a chat box |

## 2. Comparison with related tools

Legend: ✅ documented capability · ◐ partial/conditional · — not a focus ·
? unverified here. "Documented" for competitors means public product
documentation, not tested in this audit.

| Capability | HMXHARNESS | AndroidHarness (upstream) | Claude Code | OpenCode | Cline / Roo Code | Aider | Termux |
|---|---|---|---|---|---|---|---|
| Primary platform | **Android phone** | Android phone | Desktop terminal | Desktop terminal / TUI | VS Code extension | Desktop terminal | Android terminal |
| Native mobile UI | ✅ Compose | ✅ | — | — | — | — | ◐ terminal UI |
| Agent edits files | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| Runs shell on device | ✅ tiered | ✅ tiered | ✅ host shell | ✅ host shell | ◐ host shell | ✅ host shell | ✅ |
| On-device local LLM | ✅ GGUF (64-bit) | ✅ | — | ◐ via local endpoints | ◐ via local endpoints | ◐ via local endpoints | ◐ via packages |
| GitHub Device Flow | ✅ | ? (upstream varies) | ◐ device/browser login | ◐ | ◐ | — | — |
| Linux toolchain bootstrap on Android | ✅ Termux packages | ✅ | — | — | — | — | ✅ |
| Adaptive glass UI | ✅ custom tiers | ◐ base | — | — | — | — | — |
| MCP support | ✅ | ✅ | ✅ | ✅ | ✅ | ◐ | — |
| Web research tools | ✅ 9 keyed + keyless | ✅ | ◐ | ◐ | ◐ | ◐ | — |
| Open source | ✅ MIT derivative | ✅ MIT | ◐ client | ✅ | ✅ | ✅ | ✅ |

> This table compares **focus areas**, not feature parity. Claude Code and
> OpenCode are mature desktop agents with capabilities HMXHARNESS does not have
> (e.g., large desktop tool ecosystems, IDE integrations). Do not claim HMXHARNESS
> is "better" overall.

## 3. Honest differentiators (safe to say)

1. **"A coding agent that runs natively on an Android phone, including a real
   shell and optional local models."** Verifiable from code.
2. **"GitHub sign-in with Device Flow, so there is no client secret or auth
   server."** Verifiable from `GitHubAuth.kt` (flow inherited from upstream; the
   Settings card and CI wiring are HMX work).
3. **"Completion is tied to recorded verification evidence."** Verifiable from
   `HmxTaskVerification.kt`.
4. **"A custom Liquid Glass material that degrades by device tier."** Verifiable
   from `GlassQuality.kt`.
5. **"Derived from an MIT Android agent and extended with cloud CI, a glass
   design system and GitHub integration."** Verifiable from `README.md` and git
   history.

## 4. Claims that must NOT be made (no evidence)

| Claim | Why it is unsafe |
|---|---|
| "First/only AI coding agent on Android" | No exhaustive market survey; AndroidHarness and others exist. |
| "We built the AI model / trained a model" | No training code; models come from providers or llama.cpp. |
| "Fully offline AI coding" | Cloud providers are the default path; only local GGUF is offline, and it needs model downloads. |
| "Works on every Android phone" | `minSdk 26`; local inference needs 64-bit ABIs; Shizuku/root need setup. |
| "Production-ready / Play-ready signing" | Release uses the Android debug certificate. |
| "Zero security issues" | Documented HIGH/MEDIUM findings (cleartext traffic, plaintext MCP headers). |
| "Faster than Claude Code / Cline" | No benchmark performed; performance is unmeasured on device. |
| "Enterprise-grade / audited by a third party" | Only an internal repository audit exists. |
| "Custom-built llama.cpp / Shizuku / sora-editor / PrismalAGSL" | Third-party components. |
| "The whole codebase is our original work" | 828 of 961 commits are upstream AndroidHarness. |
| "Live GitHub OAuth verified end-to-end" | Device approval requires a human; not performed here. |
| "We built the GitHub Device Flow / the 21 translations" | Both are upstream (`2207914579`, `6243c2c7`); HMX added the Settings card, CI wiring and two strings. |

## 5. Suggested positioning statement

> "HMXHARNESS is an open-source, Android-native AI coding agent built on the
> MIT-licensed AndroidHarness. It runs the agent, a real Linux toolchain, Git and
> optional on-device GGUF models on the phone, with verification-backed
> completion, GitHub Device Flow sign-in without a client secret, and a custom
> adaptive Liquid Glass interface."

This statement is fully supported by files cited in this document set.
