# HMXHARNESS — Tool System Audit

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** static source inspection only; no files modified, docs not used as evidence.

## 0. Architecture and registration

- Tool contract: `tools/Tool.kt:46-55` (name, description, schema, `isReadOnly`,
  `isAvailable`, `execute`).
- `ToolRegistry`: `byName = tools.associateBy { it.name }` → **last duplicate wins**
  (`:57-64`); `withExtra` appends without dedup (`:66-68`); `without`/`restrictedTo`/`filter`
  `:61-71`; `schemas(readOnlyOnly, context)` applies PLAN filter and
  `WorkspaceToolRouting.available` (`:73-78`).
- Master list `Tool.kt:81-217`: base `:108-165`, conditionals `:166-215`. App wiring
  `HarnessApp.kt:344-375` (registry) and `:405-448` (engine; `chatOnlyTools` `:433-446`).
  Chat/Code merge `AgentEngine.kt:895-916`; per-round filters `:951-980`; MCP extras
  `RunManager.kt:704-706`. No tools exist in `debug`, `githubDelivery` or `playDelivery`.

Approval legend: `auto-RO` = auto in CONFIRM_RISKY (`AgentEngine.kt:1824`);
`confirm` = ask in CONFIRM_ALL/CONFIRM_RISKY, auto in FULL_AUTO/FULL_ACCESS (`:1817-1830`);
`ALWAYS` = explicit card in every mode (`:1755-1816`); `grant` = ShellPolicy signature
grant (`:1722-1735,1819`); `engine` = intercepted, tool body never runs.

## 1. Tool families (representative entries)

| Family | Tools (definition line) | Approval | Key controls | Registration | Tests |
| --- | --- | --- | --- | --- | --- |
| Files | `list_dir` `FileTools.kt:68`, `read_file` `:116`, `file_info` `:351`, `write_file` `:419`, `edit_file` `:468`, `search_files` `:523`, `grep` `:789`, `read_image` `:948` | RO tools auto; writes confirm | `ctx.workspace.resolve` containment; case-collision warning; ReDoS check + 20M step/2s cap for `grep`; 2 MB/binary skips; 100k char cap | base `Tool.kt:109-117`; `read_image` conditional `:193-195` | `FileToolsTest.kt`, `FuzzyEditTest.kt`, `ToolReliabilityTest.kt` |
| Patch/history | `multi_edit` `PatchTools.kt:15`, `apply_patch` `:92`, `file_history` `FileHistoryTools.kt:7`, `file_restore` `:22`, `read_tool_output` `ReadToolOutputTool.kt:10` | confirm (mutations), RO reads | atomic write after all edits succeed; path normalization strips `a/`/`b/`; SHA-256 precondition for restore; session-scoped | base `:114-115`; conditionals `:184-188` | `PatchToolsTest.kt`, `ToolReportFixesTest.kt` |
| File mutations | `create_dir` `MoreTools.kt:21`, `delete_file` `:58`, `move_file` `:132` | confirm | `protectedDeleteReason` blocks workspace parent, `/`, `/storage*`, `/sdcard`, `/mnt`; root/self/ancestor guards; no silent overwrite | base `:139-141` | `ToolReportFixesTest.kt:26-50`, `ToolReliabilityTest.kt` |
| Shell/process | `shell` `ShellTool.kt:15`, `shell_background` `BgShell.kt:11`, `bg_list` `:87`, `bg_kill` `:138`, `device_owner_shell` `DeviceOwnerShellTool.kt:8` | confirm + grant | `validateShellAccess` SSH pin; `ShellPolicy.denyReason` unless `sandboxOff`; cwd containment; 100k output cap; SSH host-key pin; Dhizuku GRANTED for device-owner | base `:118-122`; device-owner conditional `:178` | `ShellPolicyTest.kt`, `ShellAccessTest.kt`, `DeviceOwnerAccessTest.kt`; bg tools: **no direct test** |
| Env/diagnostics | `env_status` `EnvStatusTool.kt:17`, `doctor` `DoctorTool.kt:27`, `read_logcat` `LogcatTool.kt:21`, `pkg_install` `PkgTools.kt:10`, `android_sdk_install` `AndroidSdkTools.kt:24`, `pkg_search` `:101`, `pkg_list` `:157` | RO auto; `pkg_install` and `android_sdk_install` **ALWAYS** | env report only; 15 s doctor GET; logcat 1,000-line/100k cap; SSH package allowlist + `sshQuote`; one SDK approval card | base `:120-126` | `LogcatToolTest.kt`, `PkgToolsTest.kt`, `AndroidSdkToolsTest.kt` |
| Git/PR | `git_status` `GitTools.kt:284`, `git_diff` `:299`, `git_log` `:381`, `git_show` `:409`, `git_commit` `:323`, `git_branch` `:434`, `git_branch_manage` `:454`, `git_checkout` `:493`, `git_push` `:536`, `git_pull` `:587`, `github_create_pull_request` `GitHubPullRequestTool.kt:56` | RO auto; mutations confirm | `gitCmd` safe.directory + gc.auto=0; **`git_commit` excludes `.harness/` at any depth** (`:43-73`); all args shell-quoted; upstream check; PR tool never pushes/merges and dedups | base `:129-138`; PR conditional `:179` | `CodeActivityGroupsTest.kt`, `RememberedGrantsTest.kt`, `GitHubPullRequestToolTest.kt` |
| Chat-only | `github_api` `GitHubTools.kt:297`, `github_search` `:327`, `github_file` `:362`, `read_chat_text` `ChatTools.kt:57`, `view_chat_image` `:7`, `zoom_chat_image` `ChatImageZoomTool.kt:124`, `offer_code_handoff` `ChatTools.kt:80`, `search_chats`/`recent_chats`/`read_chat_snippet` `ChatRecallTools.kt`, `browser_read` `BrowserTools.kt:241`, reminders/alarm/timer `ReminderTools.kt`, `search_agent_knowledge`/`skill_view` override `ChatAgentTools.kt` | mostly RO auto; reminders **ALWAYS**, cancels confirm | `api.github.com` HTTPS pinned; repo `owner/name` regex + `..` rejection; 2 MB file cap; image selector exactly-one; chat access `canAccess`; reminder approval prevents prompt-injection planting | `chatOnlyTools` `HarnessApp.kt:433-446`; image/zoom extras `AgentEngine.kt:908` | `ChatExperienceTest.kt`, `ChatHistoryTest.kt`, `ChatImageInventoryTest.kt` |
| Web/browser | `web_search` `NetTools.kt:27`, `web_fetch` `MoreTools.kt:242`, `http_request` `NetTools.kt:448`, 14 browser tools `BrowserTools.kt:68-649` + `BrowserOpenExternalTool.kt:7` | RO auto (research), browser mutations confirm | provider allowlist + source budget; `SecretRedactor.redact`; SSRF guard `PublicHttpAddresses.validate`, NO_PROXY, no redirects; browser sandbox per call | base `:142-148`; browser conditional `:196-215` | `WebFetchHtmlTest.kt`, `WebFetchFallbackTest.kt`, `BrowserToolActivityTest.kt`, search tests |
| Memory/skills/agents | `memory_write` `InteractiveTools.kt:46`, `memory_read`/`memory_search` `MemoryTools.kt`, `remember`/`recall` `UniversalMemoryTools.kt`, `ask_user` `InteractiveTools.kt:10`, `task` `SubagentTools.kt:10`, `subagent_*` `SubagentControlTools.kt`, `skill_view`/`skills_list`/`skill_manage` `SkillTools.kt`, `visual_guide`, `todo_write`, `request_storage_access`, artifacts | RO auto; `remember` **auto in every mode** (`:1821-1823`); `ask_user`/`task` intercepted by engine | strict topic paths; profile-scoped knowledge; **name collision**: chat-agent `skill_view` overrides base tool (last-wins `Tool.kt:58`); storage-access card never grants by itself | base `:149-159`; conditionals `:180-192`; `codeExtra` `AgentEngine.kt:908-910` | `MemoryReliabilityTest.kt`, `SubagentRunsTest.kt`, `ArtifactDeviceTest.kt` |
| Settings/plugins/automation/code intelligence | `app_settings_find/change` `AppSettingsTools.kt`, `app_plugins` `AppPluginsTool.kt:29`, `automation_create/list/manage` `AutomationTools.kt`, `codegraph_*` `CodeGraphTools.kt`, `lsp` `LanguageServerTool.kt:21`, `reference_read` `ConfigRunContext.kt:90` | RO auto; settings/PLUGIN/automation mutations confirm or **ALWAYS** | `AgentSettingsAccess.OFF` hard block; install requires prior inspect token; automation target must be saved Code chat; canonical path containment for `lsp` unless sandboxOff; index size caps | conditionals `:166-177`; `codeExtra` `AgentEngine.kt:908-910` | `AppSettingsToolsTest.kt`, `PluginFidelityTest.kt` |
| MCP dynamic | `mcp__<server>__<tool>` `McpToolAdapter.kt:18` (name `:25`, sanitized `McpModels.kt:55-68`); CodeGraph MCP tools `data/env/CodeGraphMcp.kt:84-121` | all MCP forced `isReadOnly=false` (`:31`) → gated and excluded from PLAN | `isAvailable = connection.isAlive`; OAuth bearer; **workspace-server approval gate** `McpManager.kt:314-335`; config policy startup check `:336-349` | `RunManager.kt:706` → `AgentEngine.kt:907/908` → `withExtra` `Tool.kt:67-68` | `McpTest.kt`, `McpRemoteTest.kt`, `McpAuthenticationDeviceTest.kt` |

## 2. Mode gating

- **Chat allowlist:** fixed 25 names in `core/ChatExperience.kt:9-17`, applied via
  `ConversationConfig.allowedTools` (`data/ConversationConfig.kt:43-45`) and
  `ToolRegistry.restrictedTo` (`AgentEngine.kt:911,2519`). Damaged config defaults to CHAT
  (`:50-57`) so it cannot unlock coding tools. `ChatAgentPolicy.tools` (`data/ChatAgents.kt:73-88`)
  subtracts capabilities and removes `remember`/`recall` for `AgentMemory.OFF`.
  Chat excludes all filesystem/shell/git/codegraph/artifact/subagent tools and the full
  browser automation set; Code receives only `search_chats`, `recent_chats`,
  `read_chat_snippet` from the chat-only set (`AgentEngine.kt:908,2518`).
- **Plan mode:** `schemas(readOnlyOnly = mode == PLAN)` `AgentEngine.kt:977-980` filters on
  `Tool.isReadOnly` (`Tool.kt:73-75`); modifying calls rejected at `:1585-1591`.
  **Inconsistencies:** `AppSettingsChangeTool` and `AppPluginsTool` are execution exceptions
  (`:1585-1586`) but hidden from the advertised schema. Plan-visible side effects exist in
  tools marked RO: `http_request` allows POST/PUT/PATCH/DELETE (`NetTools.kt:480,506-511`),
  `codegraph_sync` rebuilds the index (`CodeGraphTools.kt:104-112`),
  `browser_get_logs(clear=true)` clears a buffer (`BrowserTools.kt:511,519`), and
  `create_artifact`/`ask_user`/`visual_guide` produce UI content.
- **MCP merge:** `withExtra(tools + extra)` overrides same-named built-ins (`:58,66-68`);
  MCP extras are passed only for Code runs (`RunManager.kt:706`).

## 3. Permission gates

`PermissionMode` `AgentEngine.kt:42-48`; approval tree `:1755-1830` with always-confirm
branches at `:1756-1816`, unconditional `remember` allow `:1821-1823`, CONFIRM_RISKY+RO
`:1824`. `ShellPolicy.kt:11-707`: denylist (`:24-42`), raw+expanded containment
(`:44-344`), system prefixes (`:437-465`), skill-folder carve-out (`:688-716`), signature
grants (`:619-648`). `sandboxOff = FULL_ACCESS && !ChatWorkspaceFs` (`AgentEngine.kt:1575`).
`WorkspaceToolRouting.kt:9-98` classifies `fileTools`/`independentTools`/`requiresCommandPath`,
injects `workspace_id` and routes through `StorageGate`.

## 4. Dead code / registration findings

- **No implemented `Tool` class is unregistered.** Every `: Tool` implementation maps to a
  registration site.
- Dead references: `notebook_edit` in `config/PluginHooks.kt:328` has no implementation;
  `browser_clear_logs` is mentioned in `BrowserTools.kt:506` but is not a tool.
- A second `WebFetchTool` constructed at `Tool.kt:144-146` is used only as `SearchPageReader`
  for `WebSearchTool`; it is not registered.
- **Counts:** base static 52 (`Tool.kt:108-165`), conditional +30 (`:166-215`), **82 at app
  startup** (all conditionals satisfied in `HarnessApp.kt:344-375`); +15/16 chat-only;
  Code run ~85 plus conditionals plus N MCP tools; Chat advertised cap 25.
- Uncertain: MCP tool names/count are server-reported; some browser/Git/reminder/lsp tools
  have no dedicated test hit (absence of a hit does not prove absence of a test).

## 5. Limitations

Static audit only; no tool was executed, no device used, and `isReadOnly` side-effect
findings are source-level inferences, not observed behaviour.
