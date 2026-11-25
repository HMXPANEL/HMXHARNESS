# HMXHARNESS — Speaker Notes (10 minutes)

These notes are embedded in the PPTX and reproduced here for rehearsal. They follow the
timing and evidence discipline of `docs/expo/presentation/HMXHARNESS_PRESENTATION_RESEARCH.md`.
Total time: **10 minutes**. Speak naturally; do not read paragraphs verbatim.

---

## Slide 1 — Product introduction (0:00–1:00)

Good morning. HMXHARNESS is a native Android application that runs an AI coding agent, chat,
a real tiered shell and optional local models directly on a phone. There is no project-owned
server behind it. Over the next ten minutes we will show the architecture, the tool system,
the cloud build, and then a real on-device demonstration where the agent builds an attendance
website. The college and team placeholders on this slide will be filled in before the expo.

*Visual cue:* hero artwork with the phone; do not claim the illustrated UI is a live capture.

## Slide 2 — The problem (1:00–2:00)

The problem is simple. Most development tools assume a laptop. A student with only a phone
cannot easily work on real project files, run commands, or use Git. Basic AI chat can answer
questions but cannot independently edit a workspace. So we asked: could the phone itself be
the development environment? We are not claiming it replaces a desktop IDE — we remove the
requirement to own one for a useful subset of work.

## Slide 3 — Our solution (2:00–3:00)

Our solution integrates the whole stack: an agent loop, tools, a file editor, terminal,
browser preview, GitHub operations, configurable model providers, and optional local GGUF
inference. The engine, tools and data run on the device. We are careful not to claim every
feature was independently verified on hardware — the architecture and code are real, and the
demonstration later will show what actually worked on a device.

## Slide 4 — System architecture (3:00–4:00)

This is the actual architecture from our source audit, with real class names. The UI sends a
message through ChatViewModel to RunManager, which starts AgentEngine. The engine builds the
context, asks a configured model for tool calls, executes them through the Tool Registry, and
persists results through SessionRepository into Room. There is no central cloud server: model
providers and GitHub are endpoints the user configures. The AppContainer wires everything
together manually — no dependency-injection framework — and credentials are protected by the
Android Keystore.

## Slide 5 — AI agent and tool ecosystem (4:00–5:00)

The agent runs a loop. It builds a system prompt with the environment, memory and repository
map; the model replies with text or tool calls; the agent parses those calls, checks
permissions, executes tools, and returns results as messages before continuing. Our audit
counted 82 tools registered at startup. Chat mode sees a fixed allowlist of 25 tools, and Plan
mode restricts schemas to read-only tools. For judges: the real approval gates are the
permission modes, ShellPolicy and workspace containment — the additional HMX approval objects
are unit-tested but are not wired into this loop, and we say so openly.

## Slide 6 — Live application demonstration (5:00–7:00)

This is the most important slide. These three screenshots were supplied by the developer from
a physical device. The agent was asked to build a student attendance website. The result shows
a dashboard with five enrolled students, four present and one absent, a date selector, a class
roster with working Present and Absent controls, and an add-student form. The banner reads
"Completed — Open page", meaning the generated page was opened in the in-app preview. We label
this as developer-supplied device evidence: we did not reproduce it in the audit, and we do not
claim a backend or persistence for the generated website. The four empty frames below are
placeholders for additional actual screenshots of chat, editor, terminal and browser preview —
we prefer real captures to fabricated ones.

## Slide 7 — Technology stack and AI models (7:00–8:00)

The stack is modern Android: Kotlin, Jetpack Compose, Room, DataStore, Coroutines and OkHttp
with server-sent events. Credentials use AndroidX Security and the Keystore. Native C++ and
CMake compile llama.cpp for local GGUF inference. Four provider protocols are supported plus
ChatGPT account integration and a third-party keyless relay. CPU GGUF is the shipped local
path; GPU is experimental and opt-in. We do not train a model, and we do not claim every
provider has been live-tested.

## Slide 8 — GitHub and cloud build (8:00–9:00)

GitHub integration uses OAuth Device Flow with only a public client ID — no client secret and
no authentication backend. Repository operations use reviewed commits and a plain push, never a
force-push. On the cloud, GitHub Actions runs unit tests, debug lint, and assembles both debug
and release APKs, then uploads the artifact. We verified a successful run and a 110-megabyte
artifact through the public GitHub API. These are real build artifacts, not a marketing claim.

## Slide 9 — Innovation, security and future scope (9:00–9:40)

We want to be transparent. What makes the project interesting is the combination: a native
Android agent, tiered shell, local GGUF, browser automation, MCP, subagents, memory and cloud
builds in one app with no project backend. HMX contributed the identity, CI, verification and
approval objects, the Connected Accounts card and the glass tier configuration; the core engine
is inherited from MIT-licensed AndroidHarness, and we preserve that attribution. Current
limitations: the HMX verification gate is test/display-only, the release APK is debug-signed,
four high-severity security findings were identified, GPU is experimental, and instrumented
tests have not been run by CI. Our roadmap addresses these directly.

## Slide 10 — Conclusion (9:40–10:00)

To close: HMXHARNESS explores a future where capable AI-assisted software development is
accessible directly from an Android device. We showed a real architecture, a real cloud build
and a real on-device demonstration. The repository link is on this slide. Thank you — we are
happy to take questions.

---

## Rehearsal checklist

- **Keep within 10 minutes** — slide 6 is the longest; do not exceed 2 minutes there.
- **Live demo is stronger than screenshots** — re-run the attendance flow if a device is available.
- **Fill placeholders** for college name and four team members before presenting.
- **Never say**: world's first, all code original, trained model, verified GPU, enforced gate,
  production-ready release, or that all providers were live-tested.
