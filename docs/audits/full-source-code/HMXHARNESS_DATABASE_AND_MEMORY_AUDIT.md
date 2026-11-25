# HMXHARNESS — Database and Memory Audit

**Audit branch:** `audit/hmxharness-full-source-audit` · HEAD `9188783`
**Method:** static source/test reading only. No schema or migration was changed; no
test or build was executed; no instrumented test result is claimed.

## 1. Main database — `data/db/Database.kt`

`@Database(..., version = 27, exportSchema = false)` at `:832-851`; `AppDatabase` at `:852`,
`dao()` at `:853`. Fourteen `@Entity` classes:

| Entity (line) | Table | Key columns/indices |
| --- | --- | --- |
| `SessionEntity` (18-40) | `sessions` | PK `id`; `updatedAt`, `projectId`, `workProjectId` (39), `conversationJson`, `pendingPlan`; no `@Index` |
| `MessageEntity` (43-64) | `messages` | PK `id`, `Index("sessionId")`; `createdAt`, `rowid`, `turnId`, `toolCallId`, `role`, `toolCallsJson`, `imagesJson` |
| `ChatAnswerVersionEntity` (66-73) | `chat_answer_versions` | PK, `Index("sessionId")`, `groupKey`, `messagesJson` |
| `SavedAnswerEntity` (80-87) | `saved_answers` | PK `messageId`, `Index("sessionId")`, `Index("savedAt")` |
| `MessageFtsEntity` (106-110) | `message_fts` | `@Fts4`, external content of `MessageEntity`, `TOKENIZER_UNICODE61` |
| `ProjectEntity` (113-120) | `projects` | PK, `kind` (APP/SAF/SHELL/SSH), `uri`, `lastUsedAt` |
| `WorkProjectEntity` (126-134) | `work_projects` | PK, `selectionJson`, `chatNamePattern` (v27) |
| `MemoryEntity` (137-155) | `memories` | PK, `Index("scope"/"projectId"/"enabled")`, `section`, `isSummary`, `consolidatedInto`, `agentId`, `pinned`, `category` |
| `CheckpointEntity` (157-167) | `checkpoints` | PK, `Index("sessionId")`, `contentB64`, `existedBefore` |
| `SnippetEntity` (169-174) | `snippets` | PK `id`, `name`, `body` |
| `UsageEventEntity` (177-193) | `usage_events` | PK `rowId`, `Index("sessionId")`, token/price columns |
| `FileEditEntity` (196-205) | `file_edits` | PK `rowId`, `Index("sessionId")`, `Index("turnId")` |
| `CodeActivityEntity` (208-215) | `code_activity` | PK, `Index("createdAt")`; intentionally survives chat deletion (comment 207) |
| `SessionFileChangeEntity` (228-247) | `session_file_changes` | composite PK, `Index("updatedAt")`, `baseGzip`, `isNew`, `isDeleted` |

`HarnessDao` (297-830): sessions 305-434; message paging 446-480 (`ORDER BY createdAt ASC, rowid ASC`); tool messages 518-522; deletion/rewind 533-540; FTS/LIKE recall 542-572; backup 582-594; workspaces 596-647; checkpoints 649-677; memories 766-813; usage 692-748; session file changes 815-829.

**Migrations:** 23 objects from `MIGRATION_4_5` (`:1012`) through `MIGRATION_26_27` (`:856`), including the FTS4 rebuild `8_9` (`:1088`) and `code_activity` creation `18_19` (`:915`). Plus `OVERSIZED_ROW_SANITIZER` (`:966-984`) truncating `messages.text`/`thinking` >250k chars on open. `HarnessApp.kt:199-209` registers all 23, the callback, and `.fallbackToDestructiveMigration(true)` (`:208`) with a comment that it is only for pre-v4 databases. **No 1→2/2→3/3→4 migrations exist.**

**Migration tests:** no dedicated `MigrationTest` class. Scattered coverage: `MemoryReliabilityDeviceTest.kt:51-71` runs `22_23`; `ChatAgentsDeviceTest.kt:233` runs `23_24`; `ConversationSectionsDeviceTest.kt:317` runs `19_20`; chain simulations at v14-v21 in `ModelLockingDeviceTest.kt:53-56`, `MultiWorkspaceDeviceTest.kt:44-50`, `ArtifactDeviceTest.kt:210-213`, `ReasoningDeviceTest.kt:361-363`, `ChatExperienceDeviceTest.kt:462-479`. Instrumented tests were not executed here.

## 2. Second database — `llm/debug/DebugLogDatabase.kt`

`DebugTraceEntity` `:23-62` (table `debug_traces`, indices on `startedAt`/`sessionId`), bodies encrypted via `DebugCrypto`. `DebugTraceDao` `:64-91` (upsert/query/prune/trim/clear). `@Database(version=2)` `:93`, `.fallbackToDestructiveMigration(true)` `:98-103` — disposable by design. Retention 7 days / 200 rows (`DebugLogRepository.kt:49-55,134-137`). JVM test `DebugTraceStoreTest.kt:23-47`.

## 3. Conversations, messages, streaming

`SessionRepository.addMessage` `:263-311` clamps fields to 250,000 chars and timestamps at `:305`. Ordering is `createdAt,rowid` (DAO 446,461,467,473,479); paging uses `MessagePosition` with full-turn completion (`SessionRepository.kt:83-104`); test `ChatHistoryPagingDeviceTest.kt:25-75`. Streaming deltas are **in-memory only** (`RunManager.kt:355-390`), committed once at `AssistantCommitted` (`:899-914`); live transcript rewrite `SessionRepository.kt:317-324`. Compaction `historyFor` `:169-180` and atomic `commitCompaction` `:192-204` (device test `CompactionDeviceTest.kt:141-182`).

## 4. Projects, checkpoints, grants

`ProjectCheckStore.kt:22-73` — plain `SharedPreferences("project_checks")`, running checks coerced to INTERRUPTED on reload. `GrantPersistence.kt:16-33` — plain JSON remembered grants; `GrantPolicy.canSave` `:56-65` blacklists installers/publish/open-ended commands. `CheckpointStore.kt:20-49` — full text Base64 into `contentB64`, one snapshot per turn/path, no size cap; `rewind` `:69-111` backs up current files first. `WorkspaceFileHistory.kt:71-106` — one `.gz`+`.json` per version, `MAX_BYTES=32MB` (`:159`), SHA-256 dedup, **no pruning**, shielded from cleanup (`StorageManager.kt:124`).

## 5. Memory subsystem

Scopes `GLOBAL/PROJECT` (`MemoryRepository.kt:10-16`); categories `PROFILE/PREFERENCE/PROJECT/DECISION/FACT` (`:18-25`); section/access rules `MemoryPolicy.kt:8-31`; agent-private ownership `ChatAgents.kt:10,35`. Write/dedup `rememberResult` `:165-205` with 0.8 overlap guard (`MemoryPolicy.kt:33-53`); concurrency device test `MemoryReliabilityDeviceTest.kt:147-162` (8 concurrent saves → 1 row). Pinning/search `DAO:797-804`, `search` `:316-328` (in-memory token relevance), `MemoryText.DEFAULT_MAX_ENTRY_CHARS=600` (`:28`), `MAX_ENABLED=400` with oldest-unpinned auto-disable (`:330-343`). Consolidation `MemoryPolicy.kt:56-87`, `MemoryRepository.kt:242-276`, originals retained as `consolidatedInto` rows. Prompt recall `MemoryPrompt` inside `MemoryRepository.kt:72-118`: `MAX_CHARS=5_000`, priority pinned → relevance → PROFILE/PREFERENCE → PROJECT → recent; tests `MemoryPromptTest.kt:31-63`, `MemoryReliabilityTest.kt:66-89`. Tools `UniversalMemoryTools.kt:14-124`, `MemoryNotes.kt:7-38` (8k core), `MemoryTopics.kt:9-39` (40k), `MemoryTools.kt:11-133`, `ProjectMemory.kt:6-17`.

## 6. Search

FTS4 MATCH for word mode (DAO `:543-547,556-560,565-569`), escaped LIKE for fuzzy (`:549-553,562,571`); query building `ChatSearch.kt:11-42`; snippet/highlighting `:45-107`. `ChatSearchTest.kt:10-84` tests pure helpers only — **no DB-backed FTS index test**. Higher-level `ChatHistoryDeviceTest.kt:37`, `ChatRecallToolsTest.kt:24-96`.

## 7. Backup and encryption

Chat backup `ChatBackup.kt:24-33` (`FORMAT="androidharness-chats"`, `VERSION=5`), sessions+messages+workspace ids/names+memories+snippets (`:162-219`), **plain unencrypted JSON** (`:216`). `ChatBackupCodec.decode` `:121-133` rejects foreign format or version >5; merge dedups by session id (`:140-147`), remaps workspaces (`:46-65`), `insertImportedMessages` uses IGNORE (DAO `:592-594`). Settings backup `SettingsBackup.kt:17-34` `VERSION=4`, validation accepts 1-4 and `legacyCodeStart` migration (`:128-213`), export `:221-250` includes providers/headers/key pools/proxy/MCP/search/voice keys but excludes device state (`:76-81`). Encryption `BackupEncryption.kt:10-49` — PBKDF2-HMAC-SHA256 600k, 16-byte salt, 12-byte nonce, AES-256-GCM with AAD header, 4MB cap. Credentials `KeyStoreManager.kt:9-42`; debug bodies `DebugCrypto.kt:21-64`.

## 8. Retention, deletion, orphans

`SessionRepository.deleteSessions` `:933-951` deletes per-session child tables in a transaction and cleans unreferenced images (`:965-979`); `deleteAllSessions` `:919-930` adds WAL truncate + VACUUM; untouched-chat prune `:899-914`. `code_activity` rows intentionally survive (entity comment `:207`). `memories.sourceSessionId` is never cleaned and project-scoped memories are not deleted with projects (`RunManager.kt:238-247`, DAO `:357-378`). `WorkspaceFileHistory` files are not removed on chat deletion but are protected from cleanup (`StorageManager.kt:121-130`).

## 9. Persistence after restart — tests found

`ArtifactDeviceTest.kt:199-244` (v16→current migration + backup round-trip), `CompactionDeviceTest.kt:141-182`, `ChatWorkspaceDeviceTest.kt:32-65`, `ProjectsDeviceTest.kt:31-59`, `MultiWorkspaceDeviceTest.kt:59-77`, `EditorPersistenceDeviceTest.kt:34,120` (rotation/process death), `MemoryReliabilityDeviceTest.kt:51-71`, `SettingsBackupDeviceTest.kt:24-83`, `MemoryEntryLimitTest.kt:48-70`. None was executed in this audit.

## 10. Possible issues (evidence-based; not confirmed by execution)

1. **Migration path starts at v4 with a global destructive fallback** (`:856-1103`, `HarnessApp.kt:208`); any unmapped upgrade would drop all tables.
2. **Migration tests appear stale relative to v27** — chain tests register only up to `21_22`/`22_23`; no test covers `8_9` FTS rebuild or `24_25`/`25_26`/`26_27`. Unproven without running instrumented tests.
3. **Missing composite indices** — `messages` has only `Index("sessionId")` despite paging/turn/tool filters; `usage_events` range queries lack `createdAt`; `projectSessions` uses `instr(...)` defeating indices.
4. **`clearMessages` is not atomic** (`SessionRepository.kt:873-880`) — interruption can leave compaction referencing deleted history or orphaned images.
5. **Read-modify-write race** in `recordFileChange` (`:734-759`) — parallel edits to the same `(sessionId, relPath)` can lose counters; REPLACE without transaction.
6. **Unbounded snapshot cost** — `CheckpointStore.kt:38-43` no cap; sanitizer covers only `messages`; `WorkspaceFileHistory` 32MB/version and no pruning.
7. **Chat backup is partial and plaintext** — omits `pendingPlan`, `workProjectId`, `usage_events`, `checkpoints`, `file_edits`, `session_file_changes`, `saved_answers`, `chat_answer_versions`, `work_projects`; image files not exported while references are (`ChatBackup.kt:204`, `ImageStore.kt:15,158-162`).
8. **Backup restore bypasses memory cap** — `importMissing` (`:293-302`) upserts without `trim()`; enabled memories can exceed `MAX_ENABLED=400`.
9. **Possible project-memory recall failure** — `projectIdForSession` reads only `sessions.projectId` (DAO `:785-786`); sessions using `workProjectId` may have null `projectId`, making PROJECT memories invisible to recall. Not verified at runtime.
10. **Orphan residue** — `memories.sourceSessionId`, `code_activity` (intentional) and history files survive deletion; project deletion does not remove project memories.
11. **Partial streamed answers are volatile** — deltas live in `LiveRunState` until `AssistantCommitted`; process death mid-stream loses them; `RunRecovery.missingResults` (`:583`) coverage of that exact case unverified.
12. **Encryption at rest is inconsistent** — `harness.db` and `debug-log.db` metadata are plaintext; `project_checks`/`remembered_grants` plain prefs; chat backups plain; only credentials, debug bodies and password-protected settings backups are encrypted.

## 11. Could not verify

Instrumented tests were not executed; Room-generated FTS SQL was not compared with the hand-written `MIGRATION_8_9`; `DebugRedact`, remote-image re-fetch on restore and mid-stream `RunRecovery` semantics were not inspected in detail.
