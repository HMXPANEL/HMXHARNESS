# HMXHARNESS — UI/UX and Performance Audit

**Audit branch:** `audit/hmxharness-full-source-audit`
**Evidence classes:** all sections below are **CODE-INSPECTED ONLY**. No device or
emulator was available, so no pixel rendering, contrast, jank, frame time or TalkBack
traversal was performed. Emulator screenshots in
`docs/HMXHARNESS_VISUAL_VERIFICATION.md:1-77` are **historical, not reproduced here**,
and the underlying screenshot test is non-gating. Manual physical-device testing of
chat, file editing, terminal and website preview is **USER-REPORTED**, not reproduced.

## 1. Navigation — `ui/AppNav.kt` (1,351 lines)

Destinations (all `composable(...)`): `chat?kind=` `:704-744`; `chat/{sessionId}` `:745-795`;
`subagent/{...}` `:796-809`; `terminal` `:810-814`; `files` `:815-835`; `changes/{id}` `:836-847`;
`viewer/{path}` `:848-886`; `automation` `:887-891,955-965`; `work-projects` `:892-912`;
`projects`/`project/{id}` `:913-954`; `build-test` `:966-993`; `my-ai` redirect `:995-999`;
`settings`/`skills`/`stats`/`providers`/`setup` `:1000-1041`. Start destination is computed
once from DataStore (`:125-161`), with nullable/startup gates rendering an empty `Box`
(`:128-154`) to prevent a setup-screen flash.

**Deep links are in-memory pending flows**, not Navigation `deepLink{}`: pending subagent
`333-345`, session `346-348`, imported workspace `349-358`, settings scroll `359-363`
(consumed `SettingsScreen.kt:290-302`), share sheet `561-610`. External entry points are
the `hmxharness://mcp` OAuth redirect (`AndroidManifest.xml:124-130`, `MainActivity.kt:321-334`)
and the `ACTION_SEND` alias (`:133-144`, `MainActivity.kt:336-344`). `initialRoute` is a
debug/test seam only (`:108,113-115`, used by `GlassShowcaseActivity.kt:243`).

**Strengths:** `navigateToConversation` applies `popUpTo(graph.id)` + `launchSingleTop`
(`:1345-1351`); per-section draft routes preserved with `rememberSaveable` (`:235-236,440-463`);
RTL-aware transitions (`:464-476,673-702`); centralized drawer gesture policy
(`NavigationGestures.kt:4-6`, enforced `:289-294`); predictive-back drawer (`:628-646`).

**Gaps:** (1) many `nav.navigate(...)` calls omit `launchSingleTop`/`popUpTo`
(`:518-529,722-787,825-945,1004-1017`) → duplicate back-stack entries; (2) `initialRoute`
uses `runCatching` that silently swallows failures (`:113-115`); (3) pending deep-link
flows are process-memory only and can be lost after process death.

## 2. Chat UI — `ChatScreen.kt` (3,647 lines) + components

- **List:** keyed, typed `LazyColumn` (`ChatScreen.kt:1907-1927`, `indexedItem` `:1935-1955`,
  `ChatListContentType` `ChatListState.kt:34-37`). Lookup maps precomputed in `remember`
  blocks (`:1763-1840`) to avoid O(N²). Streaming-aware scroll helpers at `:3301-3340`
  and a 48.dp "Scroll to latest" FAB (`:3342-3387`).
- **Trade-off:** `ChatListPrefetchStrategy` disables all lazy-list prefetching
  (`ChatListState.kt:12-31`) to avoid AndroidPrefetchScheduler crashes on streaming row
  reuse. Plausible fast-scroll jank source, unmeasured here.
- **States:** shimmer skeleton `:1900-1905,3486-3554`; older-history loading `:1974-1981`;
  provider-aware empty state `:1983-2020`; failure toasts/cards (TTS `:312-314`, share `:603`,
  voice `:1532-1533`, `WorkState.Failed` `ToolCallCard.kt:144-151,198-203`). The skeleton's
  infinite animation ignores system animations-off (`:3488-3497`, contrast `MotionBlur.kt:97-101`);
  no in-place retry affordance for a failed turn was found.
- **Cards:** tool cards offload formatting to `Dispatchers.Default` (`ToolCallCard.kt:127-141`),
  cap output at 4,000 chars (`:194-203`); diff view memoizes parsing and shows truncation
  (`VisualDiffViewer.kt:89,149-163,117-130`); verification card uses `rememberSaveable`
  (`VerificationCard.kt:47`); question cards use `selectableGroup` + radio/checkbox roles
  with 56.dp rows (`QuestionCard.kt:148-181`); `CodingCardKit` caches morph pairs and only
  animates while running/waiting (`:96-100,154-229`).
- **Composer:** `rememberTextFieldState` with two-way echo bridge for pasted images
  (`MessageComposer.kt:237-265`); action buttons carry `contentDescription`
  (`:408,430,548,573,598,614,627,674,736,1018,1210,1286`), but `actionSize` is 44.dp
  mobile / 36.dp desktop (`:341`), below the 48.dp recommendation.
- **State restoration:** composer `rememberSaveable` + DB draft mirror (`ChatScreen.kt:384-396`,
  `ChatViewModel.kt:2246-2254`); `ChatViewModel` uses `MutableStateFlow` with **no
  `SavedStateHandle`** (`:291-314,404-411`); chat `LazyListState` is not saveable (`:325`).

## 3. Files and editor — `ui/files/`

- `FilesScreen.kt`: path state `rememberSaveable` (`:152-176`), IO-dispatcher listing with
  `loadError` (`:224-239`), loading/error/empty states (`:500-513,573-579`), keyed
  `LazyColumn` with `animateItem` (`:515-570`). Filter strings are plain `remember`
  (`:165-166`) → lost on rotation; per-row `activeChanges.firstOrNull` (`:534-538`) is O(N×M).
- `CodeEditorScreen.kt`: explicit `Loading/Failed/Binary/TooLarge/Ready` state machine
  (`:135-200`),  `MAX_EDIT_BYTES` guard (`:173-177`), draft recovery (`:155-165`), on-disk
  conflict dialog (`:751-787`), unsaved-changes guard (`:254-269,788-815`); drafts persisted
  on `ON_STOP`/dispose via `EditorDraftStore` (`:241-251`). `Load.Failed` has no retry
  action (`:502-504`).
- `SmartEditorEngine.kt:24-136`: sandboxed WebView worker (network/file/content access off,
  asset allowlist `:49-93`), `Mutex` serialized requests (`:96-118`), 20 s timeout with
  actionable error (`:120-127`).

## 4. Other screens

- **Projects:** keyed lists with inline error (`ProjectsScreen.kt:91-124`), empty states
  (`WorkProjectsScreen.kt:98-108`, `ProjectsScreen.kt:254`); list states not saveable.
- **Settings:** page/history/query are `rememberSaveable` (`SettingsScreen.kt:225-271`) with
  `rememberSaveableStateHolder` (`:252`); About `:2812-2909` shows version, author/source/
  license links and credits. The "Read license" row is a bare ~24.dp `.clickable` Row
  (`:2889-2896`) without role/label.
- **Connected accounts:** ChatGPT selection error mapping (`ConnectedAccountsSection.kt:46-104`),
  GitHub Device Flow card with code copy/open/disconnect (`:113-251`); pending state not
  saveable (`:53-55`).
- **Providers:** keyed list, saveable search/show-disabled, empty states, save-failure
  dialog (`ProvidersScreen.kt:36-287`).
- **Terminal:** input/history `rememberSaveable`, auto-scroll, access-mode/root dialog
  (`TerminalScreen.kt:92-101,143-145,154-168`); shell intentionally runs in background
  (`:147-152`).
- **Setup:** five steps, storage access **required** (`SetupScreen.kt:107-113,115-153`),
  notifications/Shizuku/env/battery optional with grant/refresh buttons (`:156-271`),
  permission re-read on `ON_RESUME` (`:96-105`).

## 5. Localization (code-inspected)

`locales_config.xml:8-31` declares en + 21 locales; exactly 21 `values-*` folders exist.
Every locale has 1,128 `<string>` entries vs 1,129 in default `values/` — the sole
default-only name is `app_name` (brand name intentionally untranslated).
`AppLanguage.kt:6-27` canonicalizes `tl→fil`, `zh` variants, `in/id`, unknown→English,
applied every `MainActivity.onCreate` (`:69-75`). RTL-aware motion and diff LTR wrapper
present (`AppNav.kt:464-476`, `VisualDiffViewer.kt:146`).

## 6. Theme / Liquid Glass / performance design

- **Tiers:** `GlassQuality.kt:17` LOW/MID/HIGH; profiles `:39-52`; detection `:59-80` uses
  `maxMemory()`, cores, low-RAM flag, GLES/Vulkan and SDK. LOW if `lowRam || memory 1..255MB
  || cores<=4` (`:75`); HIGH needs ≥512MB, ≥8 cores, GLES3+/Vulkan, SDK≥33 (`:76-78`).
  Caveat: a hard "≤4 cores" rule may classify capable 4-core devices LOW.
- **What LOW changes:** real PrismalAGSL path requires backdrop + support + shader
  (`LiquidGlassModifiers.kt:66-127`); LOW uses gradient-only fallback (`:129-169`).
  **But** `HarnessTheme` creates `rememberPrismalGlassLayer()` unconditionally
  (`Theme.kt:87`), and `HarnessGlassRoot` allocates full-screen backdrop/capture layers
  whenever glass mode ≠ OFF (`HarnessGlass.kt:211-214,219-247`). Demand-driven capture
  (`:78-98,168-198`) and drawer-capture guards (`AppNav.kt:618-623,639-644`) mitigate cost.
- **Motion blur:** framework-aware — disabled below API 31 and when "Remove animations"
  is set (`MotionBlur.kt:95-101`), radius quantized and `RenderEffect` cached
  (`:152-178`), velocity from real content movement (`:113-145,201-258`). Strongest
  performance-conscious UI subsystem.
- **Debug perf host:** `GlassPerfActivity.kt` (debug-only) scrolls 80 real glass cards and
  samples Choreographer intervals for ~12 s into avg/p95/jank/heap (`:44-48,76-82,91-139,141-157`).
  No results were produced or inspected here — no measured numbers are claimed.

## 7. Accessibility

**Strengths:** 400 `contentDescription` usages and 136 `semantics{}` blocks across `ui/`;
decorative icons consistently pass `null`; correct radio/checkbox `selectableGroup`
(`QuestionCard.kt:148-181`) and heading semantics (`:143`); 56.dp question rows (`:176`),
48.dp editor control bar (`CodeEditorScreen.kt:489`), 48.dp FAB (`ChatScreen.kt:3374`).

**Gaps:** `LiquidGlassCard`/`Chip` use `.clickable(role=Button)` without `onClickLabel`
(`LiquidGlassComponents.kt:115-116,195`); sub-48dp targets (chip 36.dp `:191`, diff copy
40.dp `VisualDiffViewer.kt:220`, composer 36-44.dp `MessageComposer.kt:341`, About license
row ~24.dp); clickable rows without explicit role/long-press label (diff hunk header
`VisualDiffViewer.kt:241-246`, `ExpressiveList.kt:109-113`); no expand/collapse
`stateDescription` on `CodeCard` expanders; skeleton/editor spinners lack explicit
descriptions (`ChatScreen.kt:3530`, `CodeEditorScreen.kt:499`).

## 8. Gap summary (no redesign proposed)

1. Duplicate back-stack entries from unguarded `nav.navigate(...)`.
2. Process-memory deep links can be lost on process death.
3. Chat list prefetch fully disabled (crash mitigation, unmeasured scroll trade-off).
4. Chat/subagent/project list states and VM transient state not saveable.
5. Diff hunk collapse and plan-approval expand reset on rotation.
6. FilesScreen filter and ConnectedAccounts dialogs not saveable.
7. Editor load-failure has no retry action.
8. Chat skeleton infinite animation ignores animations-off.
9. LOW glass tier still allocates full-screen capture layers.
10. Accessibility gaps in §7.
11. O(N×M) change lookup in FilesScreen rows.
12. Debug perf host exported in debug builds; no perf artifacts produced here.

## 9. Non-claims

No emulator/device was opened; no pixel, contrast, jank or frame-time verification was
performed. Historical screenshots are not reproduced. Physical-device observations are
USER-REPORTED. No files were modified.
