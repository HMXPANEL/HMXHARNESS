# HMXHARNESS — Detailed Application Architecture

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** static source inspection only; no runtime observation, no files modified.
"Unverified" marks conclusions not provable from checked-in source.

## 1. Startup sequence

| # | Call | Location | Notes |
| --- | --- | --- | --- |
| 1 | `HarnessApp.attachBaseContext` → `GpuDeliveryFactory.attach(this)` | `HarnessApp.kt:34-37` | no-op in `githubDelivery:14`; `SplitCompat.install` in `playDelivery:15` |
| 2 | `HarnessApp.onCreate()` | `:42-43` | |
| 3 | `ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = false` | `:45` | LazyColumn prefetch crash workaround |
| 4 | `WebView.enableSlowWholeDocumentDraw()` | `:46` | |
| 5 | `StorageGate.install(...)` | `:48-49`; `StorageGate.kt:59-65` | before any workspace resolution |
| 6 | **`container = AppContainer(this)`** | `:50`; class `:72` | manual DI, synchronous on main thread |
| 7 | `StorageGate.installPrivilegedRoute { liveTier(cwd) }` | `:51-55` | ROOT/PRIVILEGED bypass |
| 8 | `memoryScope.launch { settings.ensureRumbleCombo() }` | `:56` | |
| 9 | `memoryScope.launch { reminders.rescheduleAll() }` | `:58` | re-arm alarms after force-stop |
| 10 | `onTrimMemory`/`onLowMemory` → `localModels.releaseCachedModel()` | `:60-69` | |

**AppContainer construction order** (declaration order matters): flows/channels `:74-89`;
`keys`/`settings` `:91-93`; debug-capture collectors `:104-131`; configuration/plugins
`:132-159`; runtime policy/voice/editor/local models/chatGPT/providers `:160-196`;
**Room `harness.db`** `:199-209`; repositories `:210-236`; device/env stack `:241-269`;
updates/sync/transfer `:271-280`; search `:284-331`; skills `:334-342`; browser `:343`;
**`ToolRegistry.default`** `:344-375`; MCP `:376`; GitHub `:377-378`; `routedProvider()`
`:379-403`; **`AgentEngine`** `:405-448`; **`RunManager`** `:449-463`; remaining managers
`:464-481`; final `init` starts runtime policy, models.dev load and ~11 background observers
`:485-594`.

**Activity → UI:** `MainActivity.onCreate:68` (locale canonicalization `:69-75`, edge-to-edge
`:77`, intents `:94-96`, `setContent` `:98-237`, `AppNav(container)` `:199`, lock/FLAG_SECURE
`:133-192`, update dialog `:204-234`); `onStart/onPause/onResume/onNewIntent` `:251-344`;
`AppNav` start destination from DataStore/ConversationStartup `:125-161`, NavHost `:668-1042`.

## 2. Component communication map

| Caller → callee | Evidence |
| --- | --- |
| `HarnessApp.onCreate` → StorageGate, AppContainer, reminders | `HarnessApp.kt:48-58` |
| AppContainer → SettingsRepository, KeyStoreManager, Room DAO, ToolRegistry, AgentEngine, RunManager, all managers | `HarnessApp.kt:91-473` |
| AppContainer → AppRuntimePolicy → AgentService/Telegram/RunManager/Ftp/Terminal/BgProcess | `:485-496`; `AppRuntimePolicy.kt:63-82` |
| MainActivity → AppNav, ScreenshotPolicy, UpdateManager, `McpManager.completeAuthentication`, pending channels | `MainActivity.kt:94-96,144-152,199-234,321-334` |
| AppNav → `ChatViewModel.factory(container, ...)`; other screens take container directly | `AppNav.kt:704-711,745-762,810-1041` |
| `ChatViewModel.send` → SlashCommands → `startRun` → `RunManager.startRun` | `ChatViewModel.kt:839-960,1014-1028,1545-1735` |
| `RunManager` → `AgentEngine.run` → `handleEvent` → `SessionRepository` → Room | `RunManager.kt:429-457,627-736,864-1116` |
| AgentEngine → ToolRegistry → Tool.execute → CheckpointStore → events | `AgentEngine.kt:1477-1529,1540-1566,1835-1899` |
| RunManager → RuntimeNotifier → AgentService → notifications → NotificationActionReceiver → approve/deny | `RunManager.kt:1740`; `AgentService.kt:36-78,212-262`; `NotificationActionReceiver.kt:19-42` |
| AutomationManager → WorkManager → AutomationWorker → RunManager | `AutomationManager.kt:55-96,253-261`; `AutomationWorker.kt:17-53` |
| StorageGate → StorageAccessPrompt → OS permission → refresh | `StorageAccessPrompt.kt:32-66`, `StorageAccessState.kt:31-68` |

**ViewModels:** only two in main source — `ChatViewModel` (`ChatViewModel.kt:285-289`) and
`EditorBufferViewModel` (`EditorBufferViewModel.kt:7-16`). All other screens are plain
Composables reading `AppContainer` directly.

## 3. Architecture diagram (real classes only)

```
                         Android process
  ┌──────────────────────────────────────────────────────────────────────────┐
  │ HarnessApp  attachBaseContext:34 / onCreate:42                          │
  │   StorageGate.install:48 ─► StorageAccess.isGranted                     │
  │   AppContainer(this):50                                                │
  └───────────────────────────────┬──────────────────────────────────────────┘
                                  │ manual DI (no Hilt/Dagger/Koin)
  ┌───────────────────────────────▼──────────────────────────────────────────┐
  │ AppContainer (HarnessApp.kt:72-595)                                     │
  │  SettingsRepository  KeyStoreManager  ProviderRepository                │
  │  AppDatabase/HarnessDao  SessionRepository  MemoryRepository            │
  │  CheckpointStore  WorkspaceManager  LinuxEnvironment  DeviceAccess      │
  │  ToolRegistry  SkillStore  AgentEngine ─► RunManager ─► KeepaliveTracker│
  │  Automation  Reminders  Telegram  UpdateManager  LocalModelManager      │
  │  channels: pendingSessionId / pendingShare / pendingSubagentChat / …    │
  └───────┬──────────────────────────┬──────────────────────────┬───────────┘
          │                          │                          │
  ┌───────▼────────┐        ┌────────▼─────────┐      ┌─────────▼──────────┐
  │ MainActivity   │        │ AgentService     │      │ Broadcast receivers│
  │ onCreate:68    │        │ TelegramService  │      │ NotificationAction │
  │ FLAG_SECURE:146│        │ FtpShareService  │      │ AutomationAction   │
  │ setContent:98  │        │ AdbPairing/WADB  │      │ ReminderReceiver   │
  └───────┬────────┘        │ AutomationWorker │      │ TelegramBoot       │
          │                 └────────┬─────────┘      └─────────┬──────────┘
  ┌───────▼────────┐                 │                          │
  │ AppNav NavHost │◄──── channels ──┼──────────────────────────┘
  └───────┬────────┘                 │
  ┌───────▼────────┐                 │
  │ ChatViewModel  │                 │
  │ send:839       │                 │
  └───────┬────────┘                 │
          │ RunManager.startRun:429  │
  ┌───────▼────────┐  AgentEvent ┌────▼────────────────────────┐
  │ RunManager     │ ──────────► │ SessionRepository ──► Room   │
  │ handleEvent:864│ ◄────────── │ (messages, usage, edits)    │
  └───────┬────────┘             └─────────────────────────────┘
          │ engine.run:678
  ┌───────▼────────┐  Tool.execute  ┌────────────────────────────┐
  │ AgentEngine    │ ─────────────► │ ToolRegistry.default:81    │
  │ run:296        │ ◄───────────── │ File/Shell/Git/Net/MCP/…   │
  │ executeWith-   │  ToolResult    │ CheckpointStore.snapshot   │
  │ Permission:1477│               └────────────────────────────┘
  └────────────────┘
```

## 4. Dependency injection

**No Hilt/Dagger/Koin** — grep over `app/src` for `Hilt|Dagger|Koin|@Inject|@Module|
@Singleton|javax.inject` returned zero matches; no DI library in version catalog. One
container (`HarnessApp.kt:38-39`), constructed `:50`; `MainActivity` casts application `:78`;
`AppNav` receives it as parameter; `ChatViewModel` gets it via factory. Constructor
injection with lambdas for late/cyclic dependencies (`:359,373,447,462`); `by lazy` for
`debugLog` `:96-100`, `chatBackup` `:230`, `sync`/`ftpShare` `:272-275`, `nineRouter` `:480`.
Global static service locator side effects: `ProviderNetwork.settings` `:92`,
`RotatingKeyProvider.pools` `:189`, `ProviderFactory.chatGptProvider/localProvider`
`:190,193`, `ModelCatalog.chatGptModels` `:192`, `HarnessProvider.pins` `:517`, `StorageGate`.

## 5. UI → agent → tools → DB data flow

Composer → `ChatViewModel.send:839` → SlashCommands → `dispatchAgentText:1014` →
`startRun:1545` (validation `:1553-1637`, MCP gate `:1663-1676`) → `RunManager.startRun:429`
→ `startRunLocked:460` (session `:485`, workspace `:518-522`, persisted user message
`:585-588`, keepalive/wakelock `:612-615`) → `appScope.launch:627` → `AgentEngine.run:678`
(channelFlow `:336`) → events `:158-244` → tool path `executeWithPermission:1477` →
`performWithPermission:1540` → `Tool.execute:1882` with `CheckpointStore.snapshot:1848` and
redaction `:1897` → `ToolMessageCommitted:1521` → `RunManager.handleEvent:864` →
`SessionRepository` writes → Room DAO. UI reads back via `messageWindowFlow` (`:529-567`)
and `RunManager.live` (`:584-610`); run end `finally:745-812` → `notifyFinished:805` →
notification with deep link → `MainActivity.handleSessionIntent:308-319`.

## 6. Background services and WorkManager

Manifest components: `AgentService` (`:164-171`, specialUse, not exported), TelegramService
(`:188-191`), FtpShareService (`:87`, dataSync), AdbPairingService (`:90-96`),
WirelessAdbService (`:97-100`), NotificationActionReceiver (`:175-177`), AutomationActionReceiver
(`:178`), ReminderReceiver (`:179-187`), TelegramBootReceiver (`:192-197`),
WirelessAdbReconnectReceiver (`:101-103`), SystemForegroundService (`:198`), MainActivity
(`:114-131`, singleTask exported), ShareToChat alias (`:133-144`), providers (`:146-162`).
Debug activities `app/src/debug/AndroidManifest.xml:5-18`.

Workers: `AutomationWorker.kt:17-74` (DATA_SYNC, enqueued `AutomationManager.kt:55-96`);
`LocalModelDownloadWorker.kt:18-75` (resumable, enqueued `LocalModelManager.kt:182-183`);
`GpuDownloadWorker.kt:14-70` (github flavor, hash/size verified; Play uses SplitInstall).

## 7. State restoration and process death

**Durable:** TaskControlStore atomic JSON with `fd.sync()`+rename (`TaskControl.kt:72-102`,
running→interrupted `:75-83`); Room sessions/messages/pendingPlan/usage/memories/checkpoints;
CheckpointStore rewind `:69-111`; EditorDraftStore atomic writes
(`EditorDraftStore.kt:84-127`); reminders re-armed on start/boot; startup destination from
DataStore (`ConversationStartup.kt:43-53`); pending-plan card re-armed (`ChatViewModel.kt:521-528`).
Tests: `TaskControlTest.kt:42-106`, `CheckpointStoreTest.kt:62-118`, force-stop device tests
`RuntimeRecoveryDeviceTest.kt:17`, `EditorPersistenceDeviceTest.kt:119-155`.

**Lossy:** in-flight runs live only in `appScope` (`RunManager.kt:125`); `resumeTask` is never
called at startup (call sites `ChatViewModel.kt:1095`, `AutomationManager.kt:261`,
`TelegramAgentBridge.kt:104`); `RuntimeNotifier` is in-memory so notification buttons become
no-ops after death; `pendingSessionId` is non-replaying `MutableSharedFlow` (`HarnessApp.kt:74`)
whose collector is in AppNav — not composed while the biometric lock screen is shown;
`ChatViewModel` has no `SavedStateHandle`. Unverified: NavController back-stack persistence,
and any force-stop-mid-agent-run instrumentation test (none found).

## 8. Permissions and error handling

Permissions listed at `AndroidManifest.xml:4-71` (including POST_PROMOTED_NOTIFICATIONS,
legacy storage capped by maxSdk, Shizuku, MANAGE_EXTERNAL_STORAGE). StorageGate central gate
`:75-141` with 180 s wait/10 min quiet period. Runtime requests in MainActivity/Compose.
Privilege policy `DeviceAccessPolicy.kt:51-104`; temporary root grants memory-only `:110-143`;
ScreenshotPolicy FLAG_SECURE `data/ScreenshotPolicy.kt:15-38`, `MainActivity.kt:144-152`.
Error handling: `ToolFailure` (`Tool.kt:44`), `ToolFailurePolicy.kt:10-32`, AgentEvent error
stream, RetryPolicy/StreamRetrier, `RunRecovery` closes orphan calls `:134-165`, broad
`runCatching` at service/observer boundaries. **Verified absence:** no default uncaught
exception handler, crash reporter or telemetry dependency; the only process kill is the
user-initiated Stop (`AgentService.kt:411`).

## 9. Architectural weaknesses (evidence-based)

1. **God-object composition root constructed synchronously on main thread** — AppContainer
   ~524 lines (`HarnessApp.kt:72-595`), builds Room and dozens of managers, launches ~12
   untracked `CoroutineScope.launch` observers and sets global statics.
2. **Fragile implicit init order / cyclic lambdas** — constructors capture properties
   declared later (`:358-374` vs `:464-465`, `:446` vs `:466`, `:272-277` vs `:449`);
   source order is the only guarantee.
3. **Agent runs do not survive process death and are not resumed** (`RunManager.kt:49-56,125`;
   `TaskControl.kt:75-83`; `AppRuntimePolicy.kt:71-80`).
4. **Deep-link loss under biometric gate** — non-replaying `pendingSessionId` (`HarnessApp.kt:74`)
   emitted while AppNav is not composed (`MainActivity.kt:154-201`).
5. **API-34 foreground-service type mismatch (static finding, runtime impact unverified)** —
   `AgentService` declares `specialUse` but calls 2-arg `startForeground` (`AgentService.kt:216,258`);
   `FtpShareService` declares `dataSync` but does likewise (`:22`); `targetSdk 36`.
6. **Main-thread blocking read in `MainActivity.onResume`** — `runBlocking { settings.first() }`
   at `:269-281`.
7. **Stop action kills entire process** — `Process.killProcess(Process.myPid())`
   (`AgentService.kt:401-412`) with no graceful drain.
8. **NotificationActionReceiver does work on main thread without `goAsync()`**
   (`NotificationActionReceiver.kt:19-42` → `RememberedGrants`/prefs `.apply()`).
9. **No crash/uncaught-exception handling or telemetry** (verified absence).
10. **Duplicate FQN `GpuDeliveryFactory` per flavor** (`githubDelivery:13`, `playDelivery:13`);
    compiling one channel cannot detect divergence in the other.

**Explicitly unverified:** runtime consequence of #5; Navigation back-stack persistence;
existence of force-stop-mid-agent-run tests; whether `onTrimMemory` release can interrupt an
active local model run.
