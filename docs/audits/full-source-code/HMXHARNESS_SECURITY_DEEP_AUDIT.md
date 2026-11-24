# HMXHARNESS — Security Deep Audit

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** independent source verification (`grep`/`sed`/file reads). Existing
`SECURITY.md` claims were not trusted. No file was modified and no security setting
was changed.

Verified negative facts:
- `networkSecurityConfig`: absent under `app/src/main/` (confirmed).
- `domain="database"` backup exclusion: absent in both backup XMLs (confirmed).
- `Room.databaseBuilder(..., "harness.db")` at `HarnessApp.kt:199`;
  `"debug-log.db"` at `llm/debug/DebugLogDatabase.kt:99`.

## Findings by severity

### HIGH-1 — Release builds use the debug keystore; update verification trusts a debug signer

- **Evidence:** `app/build.gradle.kts:87-99` (debug signing config, falls back to AGP's generated debug key), `:101-106` (`release { signingConfig = signingConfigs.getByName("debug") }` with the alpha comment); `data/update/UpdateManager.kt:196-216` (`validateApk()` accepts any signer matching the installed certificate set); `data/update/UpdateInstallPolicy.kt:22-26` (package/version/signer comparison). No keystore is tracked in Git.
- **Explanation:** the in-app updater's only trust anchor is "same signing certificate as installed". A debug key is not an access-controlled release key; if the stock debug credential is used, a third party can mint an APK with the same identity.
- **Reproduction:** obtain/copy the debug keystore (or use the well-known default); publish a malicious APK as a GitHub release asset or MITM the asset download; user taps Update. `validateApk()` passes and silent root/Shizuku install proceeds (`UpdateManager.kt:228-240`).
- **Impact:** arbitrary code installed with the app's identity, with access to Keystore-wrapped credentials.
- **Remediation:** add a dedicated release signing config from env/local.properties (never the debug alias); set `release.signingConfig` to it; pin an expected signer SHA-256 in `BuildConfig` and compare in `validateApk()`.
- **Tests:** `app/src/test/.../data/update/UpdateInstallPolicyTest.kt:17` covers rotation compatibility; no test pins the release signing config or a signer digest.

### HIGH-2 — App-wide cleartext traffic, no Network Security Config, configurable `http://` endpoints

- **Evidence:** `AndroidManifest.xml:83` `usesCleartextTraffic="true"`; no `networkSecurityConfig`; `SettingsBackup.kt:178-182` accepts `http`/`https`; `tools/mcp/McpModels.kt:31,33` free-form remote URL/headers; `McpTransport.kt:136-147,199-207,267-278` attaches auth and static headers to the raw URL; `llm/Llm.kt:211-219` shared OkHttp client has no `ConnectionSpec` or cleartext restriction; `NetTls.kt:49-107` CA bundle applies to shell only.
- **Explanation:** provider/MCP URLs using `http://` are used verbatim while `Authorization`, `x-api-key` and static MCP headers carry credentials. No OS-level backstop exists.
- **Reproduction:** configure an `http://` provider or MCP URL (or a server advertising `http://` OAuth metadata), then send a chat/connect; credentials and bodies travel plaintext.
- **Impact:** disclosure of provider keys, GitHub PAT, MCP tokens and prompt/response bodies to a network observer.
- **Remediation:** remove `usesCleartextTraffic`; add `@xml/network_security_config` with `cleartextTrafficPermitted="false"` (scoped loopback exception if needed); require `https` in `SettingsBackup.kt:180`, `McpModels.kt:118-147` and MCP OAuth discovery/token endpoints unless explicitly allowed loopback.
- **Tests:** `SettingsBackupValidationTest.kt` accepts `http`; `McpRemoteTest.kt:25-110` has no scheme rejection; no manifest test.

### HIGH-3 — Chat/checkpoint/memory DB excluded from neither cloud backup nor device transfer

- **Evidence:** `AndroidManifest.xml:76-78` `allowBackup="true"` + backup rules; `res/xml/backup_rules.xml:12-22`, `data_extraction_rules.xml:13-34` exclude sharedprefs/files only — no `domain="database"` (confirmed); `HarnessApp.kt:199` and `data/storage/StorageManager.kt:84` name `harness.db`; `Database.kt:43-64` (messages, toolCallsJson, thinking, imagesJson), `:157-167` (checkpoints.contentB64 = full file snapshots), `:137-155` (memories), `:126-134` (workspace selections), `:18-40` (sessions); `DebugLogDatabase.kt:99` `debug-log.db` likewise not excluded, with plaintext `url`, `error`, headers (`:27-59`).
- **Explanation:** `allowBackup=true` includes everything not explicitly excluded. SQLite is unencrypted; Android Backup transports it to Google Drive and device migration.
- **Reproduction:** with Android backup enabled, `adb backup` (pre-12) or device migration, then open `harness.db` with sqlite3 — chat text, file snapshots, memories and workspace URIs are readable.
- **Impact:** silent transfer of the whole conversation history and code snapshots outside the app's encrypted export mechanism.
- **Remediation:** add `<exclude domain="database" path="harness.db" />` (plus `-wal/-shm/-journal` and `debug-log.db`) to both XMLs, or set `allowBackup="false"`; consider SQLCipher.
- **Tests:** `SettingsBackupDeviceTest.kt` covers encrypted settings export, not OS backup rules.

### HIGH-4 — MCP config stores plaintext tokens with integrity-only HMAC; two bypass paths

- **Evidence:** `tools/mcp/McpManager.kt:45` `mcp-servers.json`; `McpModels.kt:20-37` plain-string `env`/`headers`; `McpManager.kt:116-137` — missing `.hmac` sidecar is **adopted** and re-signed (`:133-136`), parse failure returns `emptyList()` silently (`:137`); `:161-184` `computeConfigHmac()` returns `null` when Keystore unavailable and `loadServers()` treats null as accept; `:509` env passed to child process; `McpTransport.kt:145,205,276` replay static headers; `SettingsBackup.kt:236` exports servers verbatim into the encrypted settings file.
- **Explanation:** HMAC is integrity, not confidentiality. Deleting the sidecar or breaking Keystore makes tampered config trusted.
- **Reproduction:** with file access, replace `mcp-servers.json` with an attacker server and delete `.hmac`; restart — config is adopted and tools auto-reconnect (`McpManager.kt:419-432`).
- **Impact:** tool poisoning / command execution and disclosure of stored tokens.
- **Remediation:** encrypt `env`/`headers` via `EncryptedSharedPreferences`; make a missing sidecar a hard failure (`configTampered`, empty server list); fail closed when HMAC key is unavailable; version the config to prevent rollback.
- **Tests:** `McpRemoteTest.kt`, `McpTest.kt` cover parsing/transports; no HMAC/tamper/missing-sidecar test.

### MEDIUM-1 — Debug `error` strings stored unredacted and unencrypted

- **Evidence:** `llm/debug/DebugInterceptor.kt:75` stores raw `Throwable.message`; `DebugLogRepository.kt:76` writes it to Room unencrypted while bodies use `DebugCrypto.encrypt` (`:68,74,75,82`); `DebugLogDatabase.kt:46` plain `error` column; `DebugTrace.kt:106` renders it in "copied" text; contrast `DebugInterceptor.kt:58-59,72,111,129` redacts URL/headers/bodies.
- **Reproduction:** enable debug console + persist + capture bodies; trigger an exception whose message embeds a credential (proxy auth failure / secret in query string); read `debug-log.db` or copy the trace.
- **Impact:** credential disclosure into plaintext, backed-up SQLite and clipboard. `DebugTrace` KDoc (`:82-84`) claims every field is redacted — false for `error`.
- **Remediation:** route `error` through `DebugRedact.body(...)` and encrypt the column; exclude `debug-log.db` from backups (see HIGH-3).
- **Tests:** `DebugRedactTest.kt:17-73` covers headers/body/URL, not exception messages.

### MEDIUM-2 — Settings backup aggregates all credentials behind a 6-character minimum password

- **Evidence:** `data/BackupEncryption.kt:11` `MIN_PASSWORD_LENGTH = 6`, `:14` 600k PBKDF2, `:18-26` salt/nonce, `:39-48` AES-256-GCM with header AAD (construction sound); `SettingsBackup.kt:221-249` exports API keys, pools, headers, proxy credentials, search/voice keys and MCP config; `BackupEncryptionTest.kt:47` confirms 6-char acceptance.
- **Impact:** offline brute-force of the backup file yields all long-lived credentials.
- **Remediation:** raise minimum to ≥12 with a strength estimate; offer a Keystore-bound same-device restore key.
- **Tests:** existing tests codify the weak policy rather than testing strength.

### MEDIUM-3 — Debug/benchmark builds expose exported activities

- **Evidence:** `app/src/debug/AndroidManifest.xml:6-13` `GlassShowcaseActivity`/`GlassPerfActivity` `exported="true"` with no permission; `app/build.gradle.kts:111-113` debug suffix `.debug`, `:114-132` benchmark variant uses the same manifest. Main manifest exports only `.MainActivity` (`:117`), `.ShareToChat` (`:136`) and `ShizukuProvider` (`:150`).

- **Impact:** any installed app can launch developer/test surfaces on devices running debug/benchmark builds.
- **Remediation:** set `android:exported="false"` (instrumentation can still launch within the same app), or guard with a signature-level permission.

### MEDIUM-4 — MCP OAuth state is a single process-global slot

- **Evidence:** `McpManager.kt:107-114` one `@Volatile pendingAuth`, `:575-578` unconditional overwrite, `:587-618` state comparison and token save, mismatch does not clear; `MainActivity.kt:321-334` handles any `hmxharness://mcp` intent with `state`/`code`; `McpOAuth.kt:79,179-189` 48-byte random state/verifier + PKCE.
- **Reproduction:** start OAuth for two MCP servers quickly; the first flow fails or tokens are written to the wrong server; process death loses the flow entirely.
- **Remediation:** key pending auth by a nonce embedded in `state`, atomically remove on completion, bind state to `serverName`, clear on mismatch, persist if needed.
- **Tests:** `McpAuthenticationDeviceTest.kt` covers only the happy path.

### MEDIUM-5 — Update checksum verification is optional

- **Evidence:** `UpdateManager.kt:188-189` `check(release.apkSha256 == null || ...)` — no digest means no check; `:327` digest parsed permissively; `UpdateInstallPolicy.kt:17-21` correctly restricts asset URL; `:196-216` signature check is the real gate.
- **Remediation:** make the digest mandatory and pin an app-bundled signer digest.
- **Tests:** `UpdateManagerTest.kt`, `UpdateInstallPolicyTest.kt` do not cover the absent-digest branch.

### LOW findings

- **LOW-1:** MCP tamper reporting is opportunistic — `configTampered` set only when a sidecar exists and mismatches (`McpManager.kt:125-131`).
- **LOW-2:** ADB pairing code travels through a mutable notification `RemoteInput` and the port comes from the intent (`AdbPairingService.kt:179-188,78-84,113-118`); crypto itself is sound (`AdbPairingClient.kt:55-82`).
- **LOW-3:** redaction is shape/name-based; `DebugRedact.maskValue()` (`:54-63`) reveals a 4-char prefix and exact length; novel token shapes pass.
- **LOW-4:** `ShellPolicy` is lexical, not an OS sandbox (`ShellPolicy.kt:20-57,289-341,437-464`); interpreter `-c/-e` arguments can reference arbitrary paths. `WorkspaceFs.kt:95-101` is robust for file tools.
- **LOW-5:** custom-scheme OAuth redirect can be registered by other apps; PKCE/state mitigate but do not eliminate interference.
- **LOW-6:** Keystore loss silently deletes all credentials (`KeyStoreManager.kt:27-34`), and backups exclude `api_keys.xml`, so nothing restores them.

## Controls verified as reasonable (no finding)

- `artifacts/ArtifactWebView.kt:28-104` — JS on but file/network access off, dialogs/downloads/windows denied, no JS bridge, navigation restricted.
- `ui/files/SmartEditorEngine.kt:49-93` — only the appassets origin served; other requests 403; single size-capped JSON bridge.
- `browser/WorkspacePathHandler.kt:59-77` + `WorkspaceFs.kt:95-101` — traversal rejected, paths canonicalized.
- MCP workspace approval gate `McpManager.kt:255-307,319-336` (content fingerprint approval).
- MCP PKCE primitives (`McpOAuth.kt:179-189`), Keystore credential storage (`KeyStoreManager.kt:36-42`), backup crypto construction (`BackupEncryption.kt:18-49`), debug body encryption (`DebugCrypto.kt:29-64`), ADB pairing TLS+SPAKE2 (`AdbPairingClient.kt:55-82,177-190`), update asset URL trust (`UpdateInstallPolicy.kt:17-21`), NetTls CA bundle (`NetTls.kt:49-107`).
- No tracked hardcoded secrets; GitHub Client ID is injected at build time (`app/build.gradle.kts:29-34,73`).

## Remediation order

1. Dedicated release signing + pinned signer digest (HIGH-1, MED-5).
2. Disable cleartext and validate provider/MCP/OAuth URL schemes (HIGH-2).
3. Add database exclusions to both backup XMLs (HIGH-3).
4. Encrypt MCP `env`/`headers` and fail closed on HMAC (HIGH-4, LOW-1).
5. Redact/encrypt debug `error` and exclude `debug-log.db` (MED-1, HIGH-3).
6. Raise backup password minimum (MED-2), fix OAuth state map (MED-4), lock down debug activities (MED-3).

No claim is made that the application is completely secure. No fixes were implemented during this audit.
