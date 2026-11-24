# HMXHARNESS — Security Remediation Report

| Item | Value |
|---|---|
| Repository | `HMXPANEL/HMXHARNESS` |
| Branch | `security/hmxharness-remediation` (created from `e61aa94`; all work pushed here) |
| Final commit | `7a8ab07ee37536209e1aa82a84b6555586d8c714` |
| `main` at audit start and now | `ae41ac7399d2bdfa6248cbf4c00d362451675990` — **not modified** |
| Batches | 3 of 3 completed |
| Final CI run | [`45489000340`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45489000340) — **success** |
| Final APK artifact | `hmxharness-apks` ID `14967389297`, 110,483,564 bytes |
| HMX-001 | **Explicitly excluded; not touched** |

## Scope

Remediated findings HMX-002 through HMX-009 from `security-audit/SECURITY_AUDIT_FULL_REPORT.md`. HMX-001 (release debug signing) was excluded by instruction: no signing config, keystore, signing secret, or release behavior was changed.

## Batch 1 — HMX-002, HMX-003, HMX-004

| Finding | Status | Root cause | Fix | Files changed |
|---|---|---|---|---|
| HMX-002 | IMPLEMENTED — RUNTIME VALIDATION PENDING | `usesCleartextTraffic=true`, no network security config, provider/MCP URL validators accepted `http://`, no TLS restriction on credential-bearing clients | TLS-by-default network security config with narrow loopback exception; centralized `RemoteUrlPolicy` validating provider/MCP URLs; request-layer interceptor blocks downgrade redirects; UI inline validation and clearer import error; explicit-proxy routing preserved | `AndroidManifest.xml`, `res/xml/network_security_config.xml` (new), `config/ConfigValidation.kt`, `data/ProviderRepository.kt`, `data/SettingsBackup.kt`, `llm/Llm.kt`, `tools/mcp/McpTransport.kt`, `ui/settings/ProviderSheet.kt`, `network/RemoteUrlPolicy.kt` (new) |
| HMX-003 | IMPLEMENTED — RUNTIME VALIDATION PENDING | Backup rules excluded credential files but not `domain="database"` | Added database-domain exclusions plus WAL/SHM/journal entries for `harness.db` and `debug-log.db` to legacy rules and both Android 12+ sections | `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` |
| HMX-004 | IMPLEMENTED — RUNTIME VALIDATION PENDING | Plaintext `mcp-servers.json`; integrity sidecar adopted missing sidecars and degraded to accept on Keystore failure | AES-GCM config blob keyed by an AndroidKeyStore key, atomic migration of legacy plaintext with the original retained until replacement succeeds, fail-closed refusal with visible `configError`, no sidecar re-signing | `tools/mcp/McpConfigCrypto.kt` (new), `tools/mcp/McpManager.kt`, `ui/settings/SettingsScreen.kt` |

**Tests executed:** all 2,478 unit tests (first run); 2 initial failures were real regressions and were fixed in code/policy, not disabled. `SettingsBackupValidationTest` was updated to the new policy and gained an explicit remote-HTTP rejection test. `ProviderNetworkTest` passes via an explicit-proxy exception in the request interceptor.
**Result:** Batch 1 gate passed on run `45467272004` (`cdd5e0c`): unit tests, lint, APK assembly, verification and upload all green.

## Batch 2 — HMX-005, HMX-006, HMX-007

| Finding | Status | Root cause | Fix | Files changed |
|---|---|---|---|---|
| HMX-005 | PARTIALLY FIXED | Disconnect deleted only the local credential; no honest statement about server-side revocation | Clear master copy before toolchain re-materialization, remove all provisioned copies via `sync()`, verify no local credential remains, and state explicitly that a public OAuth client cannot revoke the GitHub grant server-side | `github/GitHubAuth.kt` |
| HMX-006 | IMPLEMENTED — RUNTIME VALIDATION PENDING | `MIXED_CONTENT_ALWAYS_ALLOW` in preview WebView | `MIXED_CONTENT_NEVER_ALLOW`; JS bridge, file/content access restrictions unchanged | `ui/chat/components/WebPreviewSheet.kt` |
| HMX-007 | IMPLEMENTED — RUNTIME VALIDATION PENDING | Optional LAN FTP share used a user-chosen password and only a per-session attempt limit | 24-byte random password generated per session, field made read-only with regenerate action, password rotated on Stop, explicit red LAN warning; read-only default and site-local scope unchanged | `ui/transfer/TransferSettings.kt`, `res/values/ui_extra_strings.xml` + 21 locale files |

**Tests executed:** full unit suite and lint. One failure was localization coverage for the two new FTP strings; resolved by adding English fallback values to all 21 locales.
**Result:** Batch 2 gate passed on run `45477429850` (`74e9cce`).

## Batch 3 — HMX-008, HMX-009

| Finding | Status | Root cause | Fix | Files changed |
|---|---|---|---|---|
| HMX-008 | FIXED AND VERIFIED | `MIN_PASSWORD_LENGTH = 6` for an offline credential-bearing backup | New exports require 12 characters; imports still accept six-character legacy backups via `MIN_DECRYPT_PASSWORD_LENGTH`; UI gate uses the correct minimum per mode; PBKDF2/AES-GCM/AAD unchanged | `data/BackupEncryption.kt`, `ui/settings/SettingsBackupSection.kt`, `data/BackupEncryptionTest.kt` |
| HMX-009 | FIXED AND VERIFIED | Workflow actions referenced mutable major tags | All 29 `uses:` references replaced with verified 40-character commit SHAs plus readable `# vN` comments; permissions and artifact steps unchanged | `.github/workflows/android-ci.yml`, `android-release.yml`, `android-visual-qa.yml`, `lint-baseline.yml` |

**Tests executed:** full unit suite including new `shortPasswordIsRejectedForNewBackups`, `twelveCharacterPasswordRoundTrip`, and `legacySixCharacterBackupStillDecrypts`; lint; APK build. One failure was an 11-character test fixture; corrected.
**Result:** Batch 3 gate passed on run `45489000340` (`7a8ab07`).

## Commit history pushed to the branch

| Commit | Description |
|---|---|
| `9ca377a` | security: fix network, backup and MCP secret handling |
| `0e72ec4` | security: align network policy tests and preserve explicit proxy routing |
| `379fae6`, `7b81cef`, `cdd5e0c` | lint baseline/line-alignment corrections; final state restores the original baseline |
| `ace1632` | security: harden GitHub credentials, WebView and LAN sharing |
| `74e9cce` | i18n: add FTP security strings to every locale |
| `d2bca9c` | security: strengthen backup passwords and pin GitHub Actions |
| `7a8ab07` | test: use a real 12-character backup password fixture |

No force-push, history rewrite, branch deletion, or merge into `main` was performed. No secret, keystore, or token was committed.

## APK verification

| Artifact | Size | Package | Version |
|---|---:|---|---|
| `HMXHARNESS-debug.apk` | 107,208,633 bytes | `com.hmx.harness.debug` | 40 / 4.0 |
| `HMXHARNESS-release.apk` | 69,108,588 bytes | `com.hmx.harness` | 40 / 4.0 |

The release signing configuration was not inspected or changed; the artifact verifies only that an APK was produced by the existing supported build target.
