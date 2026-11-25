# HMXHARNESS — Critical Findings and Recommendations

**Audit branch:** `audit/hmxharness-full-source-audit`
**Basis:** all 15 source-audit reports on this branch. This document is prioritised for
decision-making; it does not duplicate full evidence tables (see the per-area reports).
**No fixes were implemented during this audit.**

## P0 — Credibility / correctness before an Expo demonstration

### P0-1 — The "verification gate" is not runtime-wired

- **Evidence:** `HmxTaskVerification.canClaimCompletion():47-52` and `HmxApprovalPolicy:56-81`
  have **zero runtime callers** (only `HmxTaskVerificationTest.kt:31-61`). `assess()` is used
  only by `VerificationCard.kt:48`. Real completion is `AgentEvent.Finished`
  (`AgentEngine.kt:244,837` → `RunManager.kt:715,737`). Real approval is `PermissionMode`
  (`AgentEngine.kt:42-48`), the decision tree `:1755-1830`, `ShellPolicy.kt` and
  `WorkspaceFs.kt:95-101`.
- **Impact:** any claim that the app enforces verification-gated completion is false; a judge
  can disprove it from source.
- **Recommendation:** either wire `canClaimCompletion()` into the completion path, or stop
  calling it a gate and present it as an evidence receipt. Reconcile the contradiction between
  `docs/HMXHARNESS_FINAL_BUILD_REPORT.md:29` and
  `docs/audits/HMXHARNESS_SECOND_FULL_REPOSITORY_AUDIT.md:77`.

### P0-2 — No independent hardware verification artifact

- **Evidence:** CI runs `testDebugUnitTest`, `lintDebug`, `assembleDebug/Release` only
  (`android-ci.yml:61-76`); 555 instrumented tests are never compiled or executed by any
  workflow. No emulator/device was used in this audit. Chat, file editing, terminal and
  website preview are **USER-REPORTED** only.
- **Impact:** shell tiers, GGUF, browser automation, Shizuku/root, voice and Telegram cannot be
  claimed as verified; visible demo is the only credible proof.
- **Recommendation:** rehearse one complete live demo (chat → edit → terminal → preview) and
  record at least one instrumented smoke result on a device for shell tier + CPU GGUF.

### P0-3 — Provenance must be explicit

- **Evidence:** `README.md:351-353`, `LICENSE:1-15`, per-file adaptation comments and
  `assets/licenses/*`. The loop, tools, memory, subagents and locales are inherited upstream
  AndroidHarness; HMX additions are narrow (identity, CI, `hmx/` objects, Connected Accounts
  card, lint baseline, docs).
- **Impact:** a slide implying wholly original work contradicts the project's own legal section.
- **Recommendation:** prepare a one-slide provenance table and never claim "world's first",
  "all original", "fully offline AI", "GPU verified" or "gate enforced".

## P1 — Security (see `HMXHARNESS_SECURITY_DEEP_AUDIT.md` for full evidence)

| ID | Severity | Finding | Recommended fix |
| --- | --- | --- | --- |
| HIGH-1 | High | Release APK signed with debug keystore; updater trusts matching signer (`app/build.gradle.kts:101-106`, `UpdateManager.kt:196-216`) | Dedicated release signing config from CI secrets; pin an app-bundled signer SHA-256 |
| HIGH-2 | High | `usesCleartextTraffic=true`, no Network Security Config, `http://` provider/MCP endpoints accepted (`AndroidManifest.xml:83`, `SettingsBackup.kt:178-182`, `McpModels.kt:31,33`) | Disable cleartext globally (scoped loopback exception), validate URL schemes for provider/MCP/OAuth |
| HIGH-3 | High | `harness.db` and `debug-log.db` not excluded from Android backup/device transfer (`backup_rules.xml:12-22`, `data_extraction_rules.xml:13-34`) | Add `domain="database"` exclusions or set `allowBackup=false` |
| HIGH-4 | High | MCP tokens in plaintext config; HMAC integrity only; missing sidecar is adopted and Keystore failure degrades to accept (`McpManager.kt:45,116-184`) | Encrypt `env`/`headers`; fail closed on missing/invalid HMAC; version config |
| MED-1 | Medium | Debug `error` field stored unredacted and unencrypted (`DebugInterceptor.kt:75`, `DebugLogRepository.kt:76`) | Route through `DebugRedact` and encrypt; exclude DB from backup |
| MED-2 | Medium | 6-character minimum password for backup containing all credentials (`BackupEncryption.kt:11`) | Raise to ≥12 with strength estimate; Keystore-bound restore |
| MED-3 | Medium | Debug/benchmark exported activities (`app/src/debug/AndroidManifest.xml:6-13`) | `exported=false` or signature permission |
| MED-4 | Medium | Single global MCP OAuth state slot, clobbered by second flow (`McpManager.kt:107-114,575-578`) | Per-attempt nonce-keyed state map, atomic removal, bind server |
| MED-5 | Medium | Release digest optional (`UpdateManager.kt:189,327`) | Make digest mandatory; pin signer |
| LOW-1..6 | Low | Tamper reporting, ADB pairing RemoteInput, shape-based redaction, lexical shell policy, custom OAuth scheme, silent Keystore credential wipe | See security report §LOW findings |

## P1 — Quality gates

- **Lint:** `checkReleaseBuilds = false` and 3,930 baselined issues (`app/build.gradle.kts:139,143`);
  re-enable a release gate or document the toolchain bug.
- **Visual:** `LiquidGlassScreenshotTest.kt:59-63` swallows failures and has no pixel assertions;
  `android-visual-qa.yml:130-137` counts PNGs only. Add a real golden assertion or stop
  presenting screenshots as proof.
- **CI drift:** `android-ci.yml:3-6` stale "no NDK" comment vs install step `:53-56`;
  `lint-baseline.yml` omits NDK while main CI uses native code. Fix comment; align baseline job.
- **Instrumented tests:** not compiled or run in CI; no native unit tests (8 `.cpp`, 3 `.h`).

## P1 — Architecture robustness

- **Process death:** agent runs are app-scoped only and are never auto-resumed
  (`RunManager.kt:49-56,125`; `TaskControl.kt:75-83`; `AppRuntimePolicy.kt:71-80`); notification
  buttons then no-op because `RuntimeNotifier` is in-memory.
- **Deep-link loss:** non-replaying `pendingSessionId` (`HarnessApp.kt:74`) emitted while AppNav
  is not composed behind the biometric lock.
- **Main-thread blocking:** `runBlocking` DataStore read in `MainActivity.onResume:269-281`.
- **Stop kills process:** `Process.killProcess` (`AgentService.kt:411`) with no graceful drain.
- **FGS type mismatch (static, unverified at runtime):** `AgentService`/`FtpShareService`
  declare typed FGS but use 2-arg `startForeground` (`AgentService.kt:216,258`, `FtpShareService.kt:22`)
  with `targetSdk 36`.
- **God-object AppContainer** built synchronously on main thread with ~12 untracked coroutine
  observers and implicit init order (`HarnessApp.kt:72-595`).

## P2 — Database and memory (see database report §10)

- Migration path starts at v4 with global destructive fallback (`HarnessApp.kt:208`);
  migration tests appear stale relative to v27; no FTS DB-backed test.
- `clearMessages` not atomic (`SessionRepository.kt:873-880`); read-modify-write race in
  `recordFileChange` (`:734-759`); unbounded checkpoint/history snapshots.
- Chat backup partial and plaintext; restore bypasses memory cap; possible project-memory
  recall failure for sessions using `workProjectId`; partial streamed answers volatile.
- Encryption at rest inconsistent: main/debug SQLite, plain prefs and chat backups vs
  encrypted credentials/debug bodies/settings backups.

## P2 — Expo demonstration guidance

**Demonstrate:** integrated on-device chat→edit→diff→terminal→preview; tiered shell with
doctor card; editor undo/file history; Device Flow UI state (not live auth); CI run + APK
artifact; adaptive glass tiers on two profiles; optional CPU GGUF.

**Avoid/caveat:** "verification gate blocks completion"; GPU as shipped/verified; live OAuth;
screenshots as visual proof; "production-ready release"; device tiers as tested; "fully offline AI".

**Likely judge questions and safe answers:** see
`HMXHARNESS_PROJECT_EXPO_TECHNICAL_ASSESSMENT.md` §5 (14 evidence-backed answers).

## Recommended sequence (not executed)

1. Settle the verification-gate claim (wire or rename) — credibility blocker.
2. Rehearse and capture one live end-to-end demo.
3. Add database backup exclusions, disable cleartext, add URL scheme validation.
4. Add a release signing config; make update digest mandatory.
5. Encrypt MCP config secrets and fail closed on HMAC.
6. Re-enable/document release lint; add pixel assertion; align CI comments/baseline job.
7. Run one instrumented device smoke for shell tier and CPU GGUF and archive results.

## Closing statement

This is an investigation, not an implementation phase. No fix, schema change, dependency
change, provider/auth change or UI redesign was made. All findings are source-evidenced; items
that could not be verified (runtime FGS behaviour, hardware performance, instrumented test
results, live network calls) are labelled as such throughout the report set.
