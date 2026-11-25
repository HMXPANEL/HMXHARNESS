# HMXHARNESS — Privacy Review

**Scope:** privacy policy, in-app privacy/About strings, data-handling behaviour
in source, and privacy-related links.
**Branch:** `docs/hmxharness-complete-documentation`
**Evidence:** direct inspection of `PRIVACY.md`, `SECURITY.md`,
`app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`,
`app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`, and source
under `app/src/main/java/com/hmx/harness/` (`data/`, `llm/`, `github/`, `tools/`,
`browser/`, `local/`, `ui/settings/`). No previous audit was treated as evidence.

## 1. Changes performed

`PRIVACY.md` was completely rewritten from a 60-line generic statement into a
22-section policy that follows the actual implementation. The new sections are:
introduction and scope; information collected; information stored locally;
AI provider requests; third-party model providers; GitHub authentication; token
handling; shell and file access; browser access; MCP integrations; optional local
model processing; logs and diagnostics; crash reporting; analytics and telemetry;
data retention; data deletion; Android permissions; third-party services;
security safeguards; user choices and controls; children's privacy; contact and
reporting. An explicit "Unresolved and unverified items" section closes the
document.

## 2. Corrections to previous claims

| Previous claim or gap | Correction |
| --- | --- |
| "I built it so that your data stays between you and those services" | Replaced with a precise statement that cloud AI, search, GitHub and MCP **do** transmit data off-device; only local GGUF keeps a model request on the device. |
| Policy did not describe what is sent to AI providers | Section 4 now states that prompts and context — which may include file contents, diffs, command output and images — are sent to the configured provider. |
| Policy did not distinguish cloud vs local inference | Sections 5 and 11 separate third-party cloud providers, the built-in keyless gateways and optional on-device GGUF inference. |
| Policy did not cover tokens, MCP, browser, shell or backups in detail | Sections 7–10, 19 and 20 document them with source paths and documented limits. |
| Policy used first-person author voice and an undated "Last updated: September 6, 2026" | Replaced with a product-neutral, dated policy tied to this repository. |
| No explicit statement about analytics/crash reporting/telemetry | Sections 13 and 14 state that no such SDK or endpoint exists in this repository, with the platform caveat. |
| No contact mechanism beyond a bare URL | Section 22 points to the issue tracker and the private security-reporting route in SECURITY.md, without inventing an email or address. |
| No retention statement | Section 15 gives the actual behaviour (data remains on device until deleted) and explicitly claims no third-party period. |
| No compliance claims | No GDPR/other compliance, company address or dedicated privacy email was invented. |

## 3. Verified behaviour (evidence)

- Credentials use `EncryptedSharedPreferences` with Android Keystore master key
  (`data/KeyStoreManager.kt`); settings backups use AES-256-GCM / PBKDF2
  (`data/BackupEncryption.kt`).
- Room holds chats, projects, checkpoints and memory (`data/db/Database.kt`,
  schema version 27); a second database stores debug logs
  (`llm/debug/DebugLogDatabase.kt`).
- GitHub Device Flow (`github/GitHubAuth.kt`) uses only a public Client ID, no
  client secret; PAT is an alternative. Errors redact tokens (`SecretRedactor`,
  with `SecretRedactorTest.kt`).
- Workspace containment is checked in `workspace/WorkspaceFs.kt`; shell policy
  and approval modes in `tools/ShellPolicy.kt` and `agent/AgentEngine.kt`.
- Backup/device-transfer rules exist at `app/src/main/res/xml/backup_rules.xml`
  and `data_extraction_rules.xml`; SECURITY.md documents that they exclude
  selected credential and configuration locations, not all data.
- No analytics, advertising or crash-reporting dependency is declared in
  `app/build.gradle.kts`.
- Android permissions are declared in `app/src/main/AndroidManifest.xml` and are
  reproduced in the policy and README, including `POST_PROMOTED_NOTIFICATIONS`
  and the SDK-capped legacy storage permissions (`WRITE_EXTERNAL_STORAGE` max 29,
  `READ_EXTERNAL_STORAGE` max 32).
- Debug request/response capture is opt-in and field-encrypted with an Android
  Keystore key (`llm/debug/DebugCrypto.kt`); `local/DeviceTelemetry.kt` feeds only
  local UI and no telemetry upload endpoint was found.

## 4. In-app links

- About/screen strings expose source code, license, credits and author rows in
  `SettingsScreen.kt`; `REPO_URL` and `LICENSE_URL` point to
  `https://github.com/HMXPANEL/HMXHARNESS...`, which is correct for this
  repository.
- `settings_about_read_license` opens the full license; no obsolete
  privacy-policy URL was found in `strings.xml`.
- The `AUTHOR_URL`/`Sanuu` row is upstream attribution (see the legal attribution
  review) and was intentionally retained.

## 5. Documented limitations and unresolved questions

- **Cleartext traffic** is enabled in the manifest with no scoped network
  security configuration; SECURITY.md records this as a HIGH finding.
- **MCP static headers/environment values** are stored in a plaintext
  configuration file with an HMAC integrity check, not encryption.
- **GitHub CLI token material** may be materialised in an app-private `linux/`
  prefix (`.gh-token`).
- **Release APK signing** uses the debug key; this is a distribution concern,
  not a privacy guarantee.
- Live Device Flow approval, physical-device behaviour and third-party service
  practices were not verified in this documentation review.
- Android backup rules exclude credential, MCP configuration, toolchain and
  Telegram locations, but **not** the main `harness.db` chat database (messages,
  memory and file snapshots). Whether those records leave the device under
  Google auto-backup depends on device behaviour and is unresolved.
- No privacy-policy hyperlink exists inside the installed app; the repository
  `PRIVACY.md` is the policy, and no obsolete in-app policy URL was found.
- No server-side retention period exists because there is no HMXHARNESS server;
  third-party retention is outside this repository and is not claimed.

## 6. Validation

- `PRIVACY.md` uses HMXHARNESS as the product name throughout.
- It contains all 22 required sections plus an unverified-items section.
- It does not claim that no data leaves the device when cloud AI is used.
- It does not invent retention periods, company addresses, emails, compliance or
  security guarantees.
- README links to `PRIVACY.md` and `SECURITY.md` were verified to resolve to
  tracked files.
