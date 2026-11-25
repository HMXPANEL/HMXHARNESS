# HMXHARNESS — Remaining Risks and Runtime Validation Backlog

This document records what is **not** proven by the completed remediation. Source checks and CI are green, but no Android device, emulator, live Google backup account, live GitHub OAuth application secret, or on-path network capture was available.

## Blocked / partially fixed

| ID | Status | What remains blocked | Why | Suggested owner action |
|---|---|---|---|---|
| HMX-005 | PARTIALLY FIXED | Server-side revocation of the GitHub OAuth grant | GitHub's revocation endpoints require an OAuth client secret; this app is a public Device Flow client and no secret was invented | Revoke the grant in GitHub settings, or add a confidential-client secret with a backend and implement `DELETE /applications/{client_id}/token` |
| HMX-001 | FIXED AND VERIFIED (follow-up) | Nothing for signing; only device upgrade testing from a debug-signed install remains PENDING | Completed after the remediation branch was merged, in a separate release-signing change; certificate fingerprint verified in CI | Uninstall any older debug-signed build once before installing the release-signed APK. See `HMXHARNESS_RELEASE_SIGNING_REPORT.md` |

## Runtime tests requiring a physical device

| ID | Test | Preconditions | Expected result |
|---|---|---|---|
| HMX-002 | Configure a non-loopback `http://` provider and MCP server | Test build with synthetic keys | Save/reject with the clear validation message; no credential-bearing request leaves the device. Confirm HTTPS custom providers and `http://localhost` development servers still work |
| HMX-002 | Induce an HTTPS→HTTP redirect | Controlled proxy/endpoint | Request is blocked by the interceptor/network security config without sending Authorization |
| HMX-003 | Cloud backup and device-to-device transfer | Device with backup enabled and a test Google account | `harness.db`/`debug-log.db` and sidecars are absent from the backup; encrypted Settings export/import still round-trips |
| HMX-004 | Fresh config; legacy plaintext migration; app restart | Test build with a pre-remediation `mcp-servers.json` | Config migrates atomically, survives restart, and contains no plaintext tokens afterwards |
| HMX-004 | Missing/tampered HMAC sidecar and Keystore unavailable | Controlled test hooks | Config is refused (not adopted/re-signed), file retained, visible error shown |
| HMX-006 | HTTPS preview page with an HTTP subresource; localhost preview | WebView test page | HTTP subresource blocked; localhost preview and normal HTTPS preview still work; no bridge present |
| HMX-007 | Start/stop FTP share, wrong password, LAN mode, read-only write attempt | Two devices on an isolated LAN | Generated password works only for one session, is invalid after Stop, LAN warning shown, read-only enforced, credentials absent from logs |
| HMX-008 | Import a legacy six-character backup and a corrupted backup | Existing encrypted backup file | Legacy backup restores; corruption/GCM failure yields a clean error; no password logged |

## Residual risks accepted in this implementation

- **Explicit HTTP proxy exception:** `RemoteUrlPolicy.secureTransportInterceptor` permits a non-direct proxy route so the existing custom-proxy provider feature keeps working. A user-configured proxy can therefore still receive an HTTP target request; the proxy is explicitly configured by the user and is outside the app's threat model for this finding.
- **Untranslated fallback strings:** the two new FTP security strings are present in all 21 locale resource files with English text; professional translations are a follow-up UX task.
- **Action pinning maintenance:** SHA-pinned actions will not update automatically; a reviewed dependency-update bot is recommended.
- **Node 20 deprecation annotations:** GitHub Actions remains operational but upstream has announced Node 20→24 enforcement. Migrating to major action versions that target Node 24 is a separate, reviewed update.
- **No live CVE/advisory scan** of the Gradle dependency catalog was performed; versions were not upgraded.
- **No penetration test** of backend services, provider APIs, or GitHub servers was performed.

## Confirmation

No secret, token, keystore, or password value was written to the repository, logs, or these reports. The remediation branch did not modify `main`, and HMX-001 was left untouched there; HMX-001 was completed afterwards in a separate release-signing change documented in `HMXHARNESS_RELEASE_SIGNING_REPORT.md`.
