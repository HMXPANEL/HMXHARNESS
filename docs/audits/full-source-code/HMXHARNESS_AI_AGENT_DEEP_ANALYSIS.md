# HMXHARNESS — AI Agent Deep Analysis

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** static read-only source inspection with `git grep`/`grep -n`. No tests
were compiled or run, and no docs were treated as evidence. "Runtime-wired" means a
real call path exists in `app/src/main`, proven by source inspection — not observed execution.

## 0. Critical wiring verdicts

| Symbol | Definition | Runtime callers | Verdict |
| --- | --- | --- | --- |
| `HmxTaskVerification.canClaimCompletion()` | `hmx/HmxTaskVerification.kt:47-52` | **None in `app/src/main`**; only `app/src/test/.../hmx/HmxTaskVerificationTest.kt:31,32,37,38` | **TEST-ONLY** — does not gate completion |
| `HmxApprovalPolicy`/`HmxRisk`/`classify()` | `hmx/HmxTaskVerification.kt:56-81` (`:75`) | **None in `app/src/main`**; only test `:45-61` | **TEST-ONLY** — real classification is `ShellPolicy` |
| `HmxTaskVerification.assess()` | `hmx/HmxTaskVerification.kt:27-40` | `ui/chat/components/VerificationCard.kt:48` (+ tests) | **UI-ONLY** — display verdict only |
| `VerificationEvidence.collect()` | `agent/VerificationEvidence.kt:19-46` | `tools/GitHubPullRequestTool.kt:102`, `ui/chat/ChatScreen.kt:1819`, `ui/github/PullRequestUi.kt:301` | Runtime-wired for display/PR body, **not** a completion gate |
| `ProjectCheckStore` | `data/ProjectCheckStore.kt:22-74` | `HarnessApp.kt:473`; `BuildTestScreen.kt:252`; `ProjectsScreen.kt:159,287-290` | **UI-ONLY** — terminal-screen commands, not agent tool calls |
| `AgentEvent.Finished` | `agent/AgentEngine.kt:244`, emitted `:375,:524,:837` | `RunManager.kt:715`, `:1109-1113` | **Runtime-wired — the real completion signal** |

**Conclusion for completion:** `AgentEvent.Finished` and `RunManager.finishedNormally`/`completed`
determine completion. `canClaimCompletion()` is dead relative to runtime.

## 1. Key function/line inventory

`agent/AgentEngine.kt` (2,863 lines): `PermissionMode` `:42-48`; `AgentMode` `:50-51`;
`ApprovalRequest.response` `:53-61`; `AgentEvent` `:158-245` (`Finished` `:244`,
`FileEdited` `:225-235`); constructor `:251-276`; **`run()` `:296-889`**;
`runRegistry()` `:895-917`; `roundRequest()` `:951-1005` (schemas `:977-980`);
`nudgeFor()` `:1018-1044`; `toolMessage()` `:1097-1105`; `executeCalls()` `:1112-1197`;
`executeWithFailureGuard()` `:1379-1412`; `executeWithPermission()` `:1477-1529`;
`performWithPermission()` `:1540-2033`; `checkpointTargets()` `:2036-2068`;
`computeDiffPreview()` `:2071-2117`; `runSubagent()` `:2159-2390`;
`compact()` `:2393-2440`; `estimateContext()` `:2468-2486`;
`chatSystemPrompt()` `:2565-2602`; `systemPrompt()` `:2604-2737`;
`runtimeEnvironment()` `:2759-2826`.

`agent/RunManager.kt` (1,789 lines): `LiveRunState` `:81-117`;
`startRun()` `:429-457` → `startRunLocked()` `:460-860`; context limit `:510-517`;
`engine.run()` invocation `:678-736`; `finishedNormally` `:715`; `completed` `:737`;
`TaskPaused`/cancellation `:738-742`; follow-ups `:815-852`; `handleEvent()` `:864-1116`;
`approve()` `:1122-1126`, `deny()` `:1128-1131`, `resolveApproval()` `:1137-1148`;
`stop()` `:1250-1257`; `rewindAndTruncate()` `:1448-1478`.

Other: `ui/chat/ChatViewModel.kt:839` (`send()`), `:1545-1735` (`startRun()`,
`c.runManager.startRun(...)` at `:1700-1717`), `:1741-1749` approve/deny, `:1774-1778`
stop; `llm/Llm.kt:211-219` OkHttp client, `:224-238` ProviderFactory, `:245-305` SSE;
`tools/Tool.kt:73-79` schemas, `:81-217` registry; `data/MemoryRepository.kt:73` cap,
`:146-157` prompt block; `agent/Steering.kt:11-33`; `agent/TodoStore.kt:33-40`;
`agent/ChatContextPolicy.kt:5` limit 250,000; `agent/AutoCompaction.kt:24-26`;
`agent/HistoryCompactor.kt:13` 24,000 input cap, `:16-94` summarize;
`agent/StreamRetrier.kt:60-81` stall guard, `:213-273` run; `agent/RetryPolicy.kt:11-50`;
`agent/ContextOverflow.kt` `MAX_RECOVERIES=2`; `tools/ShellPolicy.kt:17-58,620-641`;
`ui/common/BiometricAuth.kt:87-103`.

## 2. Answers to the 15 audit questions

**Q1 — How does the agent receive a task?** Runtime-wired.
`ChatViewModel.send()` `:839` → `dispatchAgentText()` `:1014` → private `startRun()`
`:1545` (validates provider/model/key `:1572-1622`, attachments `:1624-1636`, MCP
config gate `:1660-1676`, model resolve `:1677-1693`) → `RunManager.startRun(...)`
`:1700-1717` → `startRunLocked()` `:460` (session `:485`, prompt record `:585-598`,
app-scoped job `:627`) → `engine.run()` `:678-736`. Other entry points: automation
`AutomationManager.kt:253`, Telegram `TelegramAgentBridge.kt:111`, resume `:1398-1406`.

**Q2 — How does it understand the request?** Runtime-wired. `AgentMode` ACT/PLAN
(`AgentEngine.kt:50-51`), chosen at `ChatViewModel.kt:1707`, forced ACT for chats
`RunManager.kt:698`. Code system prompt `systemPrompt()` `:2604-2737` (identity `:2637`,
rules `:2640-2659`, FULL ACCESS notice `:2664-2668`, PLAN block `:2678-2688`, skills
`:2689-2693`, AGENTS.md `:2694-2696`, project config `:2697-2711`, memory `:2712-2727`,
repo map `:2728-2735`). Chat prompt `chatSystemPrompt()` `:2565-2602`. PLAN is enforced
in code: read-only tool rejection `:1585-1591`, schema filter `:977-980`.

**Q3 — How is context constructed?** Runtime-wired. Budget `ChatContextPolicy.runLimit()`
called `:386-388` and `RunManager.kt:514-517`; environment `runtimeEnvironment()` `:2759-2826`
(refreshed per request, tool index at `:2824`); memory `MemoryRepository.promptBlock()`
`:146-157` invoked `:2596` and `:2715` (cap 5,000 chars at `MemoryRepository.kt:73`);
repo map `RepoMapCache.getMap(maxChars=10_000)` `:2728-2735`, invalidated on edits
`RunManager.kt:982`; attachments joined `:422-423` and `ChatViewModel.kt:1632-1636`;
estimate emitted as `AgentEvent.EstimatedContext` `:529`.

**Q4 — How is a model selected?** Runtime-wired. Per-chat selection
`ChatViewModel.kt:1572-1609`; `providers.resolveHarnessConfig(config)` `RunManager.kt:501`;
`ProviderFactory.create()` `Llm.kt:224-231` and `create(type)` `:233-238`;
`HarnessApp.routedProvider()` wraps in `FallbackProvider` and is injected as the engine
factory. Subagent models via `SubagentModelResolver` built `RunManager.kt:664-673`.

**Q5 — How are tools selected?** Runtime-wired. `ToolRegistry.default()` `Tool.kt:81-217`
(~60 base tools + conditional groups `:166-215`); `AgentEngine.runRegistry()` `:895-917`
filters by conversation allowlist/search depth/memory/reminders; `roundRequest()` `:973-980`
applies config denies, `ConfigToolPolicy` and PLAN read-only filtering. Schemas
`ToolRegistry.schemas()` `Tool.kt:73-79`. Provider tool-call parsing: OpenAI compat
`OpenAiCompatProvider.kt:385-408`, Anthropic `AnthropicProvider.kt:193,213,226`,
Responses `OpenAiResponsesProvider.kt:277-349`, Gemini `GeminiProvider.kt:108`.

**Q6 — How does it execute multiple steps?** Runtime-wired. `while (true)` at
`AgentEngine.kt:463-882`. Iteration cap `:468-473` (default `maxIterations = 0`, i.e.
unlimited). `TaskBudget.check()` `:464` and `:1493,1862,1948,2222` → `TaskPaused`.
`executeCalls()` `:1112-1197`: contiguous `task` calls parallel `:1134-1165`; safe
read-only tools parallel `:1166-1185` (exclusions `PARALLEL_READ_EXCLUSIONS` `:2829`,
`browser_*` excluded); everything else serial `:1186-1195`. Subagent loop `runSubagent()`
`:2221-2389` has no step cap.

**Q7 — How are tool results returned?** Runtime-wired. `toolMessage()` `:1097-1105`
creates `ChatMessage(role=TOOL, toolCallId, toolName, isError, receipts)`; committed in
original call order `:853-858` after `executeCalls()` fills the ordered map `:842-848`;
failure hints `:1510-1514`; secret redaction `:1537-1538`; harness-only notes appended
`:864-881` (not shown on screen). Subagents validate invented calls `:2353-2361`.

**Q8 — How are failures detected?** Runtime-wired. `StreamEvent.Failure`
`Llm.kt:153-154` (`code`, `errorCode`, `retryable`, `retryAfterSeconds`), `ApiException`
`:168-176`; engine handling via `StreamRetrier.run()` `:714-746`, terminal error emit
`:816-820`. Tool-level: `exit code: N` parsed by `VerificationEvidence.collect()` `:17,37-42`;
`ProjectCheckStore` `:41-48`; `ToolResult.isError`. Missing toolchain `missingAndroidToolchain()`
`:128-144` (exit 127). Deterministic retry block `executeWithFailureGuard()` `:1379-1412`.
Stall `StreamRetrier.stallGuard()` `:60-81`. Empty answer `nudgeFor()` `:1018-1044`.

**Q9 — Can it recover from errors?** Runtime-wired. `StreamRetrier.run()`
`StreamRetrier.kt:213-273` called from `:714` (main), `:1270` (aside), `:2248` (subagent),
`HistoryCompactor.kt:42`; `RetryPolicy` `MAX_RETRIES=3` with 1/2/4s ±30% backoff
`:44-49`; `FallbackProvider.streamChat()` `FallbackProvider.kt:47-185`; vision degradation
`AgentEngine.kt:749-762`; overflow compaction `:767-770`; toolchain repair card
`:1906-1972`; question auto-continue `RunManager.watchQuestion()` `:1168-1179`.
`StreamRetrier.STALLED_STREAM_MESSAGE` (`:105`) is an **unused constant** — stub.

**Q10 — Can the user cancel?** Runtime-wired. `RunManager.stop()` `:1250-1257`,
`stopAllAndJoin()` `:1265-1276`; `ChatViewModel.stop()` `:1774-1778`; foreground service
stop `AgentService.kt:273-276`; `CancellationException` rethrown `AgentEngine.kt:1898-1899`;
notification actions `NotificationActionReceiver.onReceive()` `:15-46` call the same
RunManager methods; subagents are child jobs (`SubagentRuns.kt:144-232`).

**Q11 — How are modifications tracked?** Runtime-wired (one UI-only subsystem).
Pre-edit `checkpointTargets()` `:2036-2068` → `checkpointer.snapshot()` `:1848` (failure
aborts edit `:1850`); post-edit `saveVersion()` `:1889`; diffs `:1845-1860,1998-2031`;
`AgentEvent.FileEdited` `:2018-2026`; `RunManager.handleEvent` FileEdited `:973-998`
records changes and invalidates repo map; undo `rewindAndTruncate()` `:1448-1478`;
file-history tools registered `Tool.kt:185-188`. `ProjectCheckStore` is UI-only and
tracks terminal commands, not agent tool calls.

**Q12 — How are approvals enforced?** Real gates runtime-wired; HMX policy test-only.
`PermissionMode` `AgentEngine.kt:42-48`; shell denylist except FULL_ACCESS
`:1727-1733` → `ShellPolicy.denyReason()` `ShellPolicy.kt:17-58`; grant keys
`ShellPolicy.grantKey()` `:629-634` checked `:1819`; decision tree `:1755-1831`
(hook-ask, config ask, settings/plugins, reminders, automations, **package install
always confirms** `:1805-1816`, FULL_ACCESS `:1817`, remembered grants `:1819`,
FULL_AUTO `:1820`, confirm-risky `:1824`, else prompt `:1825-1830`); denial result
`:1832-1834`. `RunManager.approve/deny` `:1122-1148` and notification actions
`:15-46` complete `ApprovalRequest.response`. Biometric gating
`ui/common/BiometricAuth.kt:87-103` is called **only when enabling FULL_ACCESS** in
UI (`ChatWorkspaceScreen.kt:278`, `MainHeader.kt:372,479`, etc.), not per card.
`HmxApprovalPolicy.classify()` has zero runtime callers.

**Q13 — How is completion determined?** `AgentEvent.Finished` emitted `:375,:524,:837`;
`RunManager` sets `finishedNormally` `:715` and `completed = finishedNormally && error == null`
`:737`; abnormal reason `:1109-1113`. **No verification-based runtime gate exists.**

**Q14 — What happens when context is too large?** Runtime-wired.
`ChatContextPolicy.LIMIT=250_000` `ChatContextPolicy.kt:5`; `compactAt()` `:45-48`;
`AutoCompaction.threshold()` `:24-26` (80% default); trigger `AgentEngine.kt:528-533`;
`compact()` `:2393-2440` keeps ~8 recent messages `:2406-2412`, calls
`HistoryCompactor.summarize()` `:2420` (input cap 24,000 chars at `HistoryCompactor.kt:13`,
output `min(1,500, maxContext/8)` `:27`, merge rounds ≤8 `:81-92`). Server overflow
recovery `ContextOverflow` learns a smaller window, `MAX_RECOVERIES=2`, applied
`:767-770,2401-2405`. Manual `/compact` `ChatViewModel.forceCompact()` `:2278-2348`.

**Q15 — What happens on provider timeout?** Runtime-wired. OkHttp client
`Llm.kt:211-219`: `connectTimeout(30s)` `:216`, `writeTimeout(60s)` `:217`,
`readTimeout(0)` `:218` for SSE; Harness provider read 90s `:109-111` / 0 for streaming
`:190-207`. SSE `sseJson()` `:245-305` enqueues `:252-261`, throws `ApiException` on
non-2xx `:264-267`, skips malformed JSON `:286-288`, cancels on collector cancellation
`:301-304`. Stall timeout default 900s `StreamRetrier.kt:108`, first-output window
`:149-177` (reasoning 300s, self-hosted 600s). Retry classification `RetryPolicy.kt:32-41`
(408/429/5xx); backoff `:44-49`. User-facing `AgentEvent.Retrying` `:198-199` shown
`RunManager.kt:1073-1078`; "Wait longer" UI `TaskControls.kt:107`.

## 3. Classification summary

- **Runtime-wired (a):** task entry, prompts/modes, context, model selection, tool
  schemas/parsing, main and subagent loops, tool results, failure detection, retries/
  fallback/compaction, cancellation, checkpoints/diffs, PermissionMode/ApprovalRequest/
  ShellPolicy/biometric mode-enable, `Finished`, provider timeouts.
- **Test-only (b):** `canClaimCompletion()`, `HmxApprovalPolicy`.
- **UI-only (c):** `assess()`/`VerificationCard`, `VerificationEvidence.collect()` display,
  `ProjectCheckStore`, biometric FULL-ACCESS gating.
- **Unused (d):** `StreamRetrier.STALLED_STREAM_MESSAGE`.
- **Mock/stub implementations:** none found in main source; all providers perform real
  network I/O.

## 4. Limitations

Static audit only — no tests compiled/run, app not launched, ProGuard/R8 behaviour not
checked, third-party SDK internals out of scope, not every `androidTest` file read in
full. "Runtime-wired" is a source call-path finding, not observed execution.
