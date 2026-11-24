# HMXHARNESS — Final Security Re-Audit

Performed after all three batches, against the final pushed commit `7a8ab07` and successful CI run `45489000340`.

## Method

1. Re-read the original confirmed findings in `security-audit/SECURITY_AUDIT_FULL_REPORT.md`.
2. Reinspected every modified file at current source with line-precise static checks.
3. Searched for newly introduced vulnerabilities, plaintext secrets, and regressions.
4. Checked the three original high-risk areas (network config, backup rules, MCP storage), OAuth cleanup, WebView and LAN sharing restrictions, backup compatibility, and workflow SHA pinning.
5. Ran the full available test and build suite via the existing GitHub Actions workflow (unit tests, Android lint, debug + release APK assembly, APK verification, artifact upload).

## Finding-by-finding disposition

| ID | Status | Re-audit evidence |
|---|---|---|
| HMX-001 | NOT FIXED (out of scope) | `app/build.gradle.kts` is byte-identical to `main`; no signing config, keystore, or signing secret changes. The only change to `android-release.yml` is action SHA pinning. |
| HMX-002 | IMPLEMENTED — RUNTIME VALIDATION PENDING | `AndroidManifest.xml:83` now declares `networkSecurityConfig`; no `usesCleartextTraffic` remains. `network_security_config.xml` sets `cleartextTrafficPermitted=false` with loopback-only exceptions. `RemoteUrlPolicy.validate` is called from provider add/update and MCP transport; `secureTransportInterceptor` is installed on the provider and MCP clients. Unit tests confirm remote HTTP rejection and that HTTPS/loopback configurations survive. Device-level redirect/downgrade observation was not performed. |
| HMX-003 | IMPLEMENTED — RUNTIME VALIDATION PENDING | `backup_rules.xml` and both `data_extraction_rules.xml` sections contain database-domain and WAL/SHM/journal exclusions for `harness.db` and `debug-log.db`. Existing credential-file exclusions remain. Actual cloud/device-transfer restore was not performed (no device/account). |
| HMX-004 | IMPLEMENTED — RUNTIME VALIDATION PENDING | `McpManager` no longer contains `computeConfigHmac`/`configHmacFile` or any accept-on-null branch. Loading decrypts an AES-GCM blob or atomically migrates legacy plaintext; every failure calls `refuseConfig`, keeps the file, and exposes a non-secret `configError`. Persist/import encrypt before replacing. No unit test covers device Keystore failure; that path remains runtime-pending. |
| HMX-005 | PARTIALLY FIXED | `GitHubAuth.disconnect()` clears the master copy before `sync()`, reports sync failure honestly, verifies no local credential remains, and states that server-side revocation is unavailable for this public OAuth client. GitHub's revocation endpoints require a client secret that the app does not have; no credential was invented. Local cleanup is implemented; server-side revocation remains blocked. |
| HMX-006 | IMPLEMENTED — RUNTIME VALIDATION PENDING | `WebPreviewSheet.kt:1095` now uses `MIXED_CONTENT_NEVER_ALLOW`. `allowFileAccess`/`allowContentAccess` remain false; `BrowserController.bindActiveWebView` still adds no JavaScript interface. No browser-level mixed-content test was run on a device. |
| HMX-007 | IMPLEMENTED — RUNTIME VALIDATION PENDING | FTP share now generates a 24-byte random password per session, makes the field read-only, rotates it on Stop, and shows a red LAN warning. Read-only default, site-local restriction, 3-attempt limit and symlink restrictions are unchanged. Live LAN authentication/session-expiry testing remains runtime-pending. |
| HMX-008 | FIXED AND VERIFIED | `MIN_PASSWORD_LENGTH = 12` for exports, `MIN_DECRYPT_PASSWORD_LENGTH = 6` for legacy imports. Unit tests pass for short-password rejection, a real 12-character round trip, wrong-password rejection, tampering, truncation/oversize, and a reconstructed legacy six-character backup that still decrypts. |
| HMX-009 | FIXED AND VERIFIED | All 29 workflow `uses:` clauses are verified 40-character SHAs resolved from the upstream repositories' official `v4`/`v2` tags. YAML loaded successfully in CI and the pinned actions executed in run `45489000340`. |

## New-vulnerability search

- **Plaintext secrets:** no token-like string was found in the complete diff; credentials continue to live in `EncryptedSharedPreferences` and the new MCP config blob.
- **Backup rules:** database exclusions were added without removing any existing exclusion; the explicit encrypted settings export/import remains intact.
- **Network:** no new `http://` endpoint was introduced; only loopback exceptions remain. The interceptor's explicit-proxy exception is documented and preserves the pre-existing user-configured proxy feature.
- **WebView:** no JavaScript bridge was added; mixed content is now blocked by default.
- **LAN sharing:** no new exposure was added; scope and read-only behavior were preserved.
- **CI:** permissions remain `contents: read`; artifact upload behavior is unchanged; SHA pinning was verified against upstream tags.

## Full verification results

| Check | Result |
|---|---|
| Unit tests (2,480) | PASS — run `45489000340` |
| Android lint (debug) | PASS — run `45489000340` |
| Debug APK assembly | PASS — `HMXHARNESS-debug.apk` |
| Release APK assembly | PASS — `HMXHARNESS-release.apk` |
| APK verification step | PASS |
| Artifact upload | PASS — 110,483,564 bytes |
| Package/version check | `com.hmx.harness.debug` / `com.hmx.harness`, versionCode 40, versionName 4.0 |

## Overall conclusion

Eight of the nine in-scope findings are implemented: two (HMX-008, HMX-009) are fully verified at automated-test level, five are source-verified but need device runtime validation, and one (HMX-005) is partially fixed because server-side revocation is impossible without an OAuth client secret. Runtime validation on a physical device remains open for HMX-002, HMX-003, HMX-004, HMX-006 and HMX-007. HMX-001 was left untouched as instructed.
