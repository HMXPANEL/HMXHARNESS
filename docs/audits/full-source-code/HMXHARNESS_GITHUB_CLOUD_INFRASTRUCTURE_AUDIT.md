# HMXHARNESS — GitHub and Cloud Infrastructure Audit

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** read-only source/config inspection plus **anonymous public GitHub API queries**
(no token was available or printed). No files modified, no destructive Git operations.

## 1. Authentication — `github/GitHubAuth.kt`

The `GitHubApi` class lives inside `GitHubAuth.kt:39-91` (there is no separate
`GitHubApi.kt`).

- **Endpoints:** `POST https://github.com/login/device/code` (`:142-143`) and
  `/login/oauth/access_token` for exchange (`:155-156`) and refresh (`:232-233`);
  API calls to `https://api.github.com` with `Accept: application/vnd.github+json`
  and `X-GitHub-Api-Version: 2022-11-28` (`:49-54,65-69`).
- **Scopes:** `repo offline_access`, plus `workflow` when enabled (`:142-143`;
  `ui/settings/GitHubIntegrationSection.kt:77-80`). No client-side fine-grained scope check.
- **Hardening:** verification URI pinned to `https://github.com/login/device` (`:148`);
  polling honours `interval`/`slow_down` and expiry (`:150-174`); error whitelist
  `oauthError` (`:274-280`). Refresh only within 60 s of expiry, under a `Mutex`, with
  login match (`:211-241`) and 60 s backoff (`:214-217`).
- **PAT fallback:** `connectPat()` trims/rejects whitespace, verifies via `GET /user`
  **before** saving, clears OAuth state (`:186-197`); UI uses a password field and links
  to fine-grained token creation (`GitHubIntegrationSection.kt:81-104`).
- **Cancellation safety:** generation counter + `ensureActive()` prevents a late token or
  logout from reconnecting (`:128-133,162-168,188-196`); `GitHubAuthTest.kt:124-149`.
- **Redaction:** `safeMessage()` replaces saved access/refresh tokens with `[hidden]`,
  then `redactGitHubSecrets()` strips userinfo URLs and masks `gh[pousr]_…` /
  `github_pat_…` (`:264-284`). `GitHubAuthTest.kt:69-96` asserts **no `client_secret`**
  in any device request.
- **Tests:** `GitHubAuthTest` covers polling, identity, scope, single-flight refresh, PAT
  validation, backoff, and missing-Client-ID behaviour. Device tests exist
  (`GitHubDeviceTest.kt:31-206`, etc.) but are **not run by CI**.

## 2. Token storage

- Master copy: `HarnessApp.kt:377-378` passes `keys::githubCredential` /
  `putGitHubCredential`; `KeyStoreManager.kt:9-12,27-42` uses EncryptedSharedPreferences
  with Keystore `MasterKey` `AES256_GCM` (AES256_SIV/GCM key/value schemes); credential
  JSON written atomically with `commit()` under `github_connection` (`:136-151`).
  Keystore-loss recovery deletes undecryptable prefs (`:20-34`).
- Toolchain copies: `data/env/GitHubProvision.kt:23-29` writes `home/.gh-token`,
  `home/.config/gh/hosts.yml` and an `etc/gitconfig` `insteadOf` rewrite injecting
  `x-access-token` into HTTPS GitHub URLs (`:40-48`). `writePrivate()` creates files
  0600 from creation (`:101-114`); `LinuxEnvironment.kt:1179-1189` re-materializes on
  auth changes; staging tarballs contain no auth (`:1191-1198`).
- `data/transfer/` contains no Git operations; it only excludes `.git` from transfers
  (`TransferFiles.kt:32`).

## 3. Repository operations and review gating

- URL validation accepts only `owner/repo`, rejecting credentials/flags/other hosts
  (`GitHubRepository.kt:15-22`; test `:155-160`); args single-quote escaped (`:24-25`).
- `inspect()` blocks nested workspaces, alternate push URLs, detached HEAD and unresolved
  conflicts (`:76-104`).
- `publish()` re-reads state, requires branch/remote/head to match the reviewed snapshot
  and selected files to still exist, uses `commit --only`, and **never uses `--force`** —
  plain `push --porcelain origin <sha>:refs/heads/<branch>` then `ls-remote` verification
  (`:118-143`). `.harness`/`.git` paths can never be committed (`:41-46,128`).
- PR flow is read-only locally (`fetch` into `FETCH_HEAD`, no ref moves, `:155-186`);
  PR creation never merges/closes and dedups existing PRs (`GitHubPullRequests.kt:54-93`).
- Agent Git tools `git_commit`, `git_branch_manage`, `git_checkout`, `git_push`, `git_pull`
  are non-read-only (`tools/GitTools.kt:323-619`), hence rejected in Plan mode and gated
  by approval. Push uses plain push/`-u` only (`:130-139,536-585`); pull defaults to
  `--ff-only`. The only `force` option is branch deletion `-D` (`:454-490`), not force push.
- Repo-wide grep found **no** `push --force`, `-f` push or forced `+refspec` in app Git code.

## 4. Client ID injection / no client secret

- `app/build.gradle.kts:24-38` reads `local.properties`, then env `HMX_GITHUB_CLIENT_ID`,
  then `GITHUB_CLIENT_ID`; validates `[A-Za-z0-9]*`; injects `BuildConfig.GITHUB_CLIENT_ID`
  at `:73`; comment `:27-30` states it is public with no secret/key/backend.
- Workflows append `GITHUB_CLIENT_ID=…` to **gitignored** `local.properties` and print only
  a status line (`android-ci.yml:30-42`, `android-release.yml:28-40`,
  `android-visual-qa.yml:37-49`); `local.properties` is untracked (`.gitignore:6`).
- Repo-wide search found `client_secret` only in MCP dynamic client registration
  (`tools/mcp/McpOAuth.kt:49-50,135,165,224,240`), tests asserting GitHub's absence, and
  docs. **No GitHub client secret literal or private key exists.**

## 5. Workflows

| Workflow | Trigger | Toolchain | Steps | Artifacts |
| --- | --- | --- | --- | --- |
| `android-ci.yml` | push `main`/`feature/**`, PR, dispatch; `contents: read`; cancel-in-progress; 90 min `:7-25` | JDK 21; **installs NDK 27.2.12479018 + glslc** `:53-56`; Gradle cache `:58-59` | wrapper validation, Client ID, `testDebugUnitTest`, `lintDebug`, debug+release assemble, APK verification `:61-90` | `hmxharness-apks`, 90 days, `if-no-files-found: error` `:92-98` |
| `android-release.yml` | dispatch, `v*` tags; 90 min `:7-23` | JDK 21, NDK+glslc, cache `:45-57` | `assembleRelease`, verify/stage `:59-70` | `hmxharness-release-apk`, 90 days `:72-78` |
| `android-visual-qa.yml` | dispatch (mode/screen) + one feature branch `:5-29` | JDK 21, NDK, emulator API 33 SwiftShader, KVM `:50-63,85-118,164-193` | build → capture 2×13 → collect PNG count → measure 3 profiles | APK 7 d, per-shot 14 d, screenshots 90 d, perf 90 d |
| `lint-baseline.yml` | dispatch only `:6-15` | JDK 21, Gradle cache, **no NDK** `:17-21` | `updateLintBaseline` `:22-23` | `lint-baseline`, default retention `:24-29` |

**Stale comment confirmed:** `android-ci.yml:3-6` says NDK is deliberately not installed,
contradicted by `:53-56`. Build evidence shows the install step succeeded.

## 6. Build evidence — live public API verification

- Run **`45338701934`**: `conclusion: success`, head `ae41ac7`, created
  `2026-11-24T06:12:15Z`, single job `build` (06:12:35→07:09:00Z). All steps succeeded:
  wrapper validation, JDK 21, NDK install, unit tests, debug lint, both APKs, APK
  verification, artifact upload, summary. Only failure-report upload was skipped.
- Artifact **`14898111209`**: `hmxharness-apks`, **110,461,001 bytes**, not expired,
  expires `2027-02-22T06:12:15Z`.
- Run **`45337564227`**: `failure`, head `3aa4f115…`, failed at "Run unit tests" with
  `./gradlew: Permission denied` / exit 126; later steps skipped. Fix: `ae41ac7` restored
  Git mode `100755` on `gradlew` (confirmed locally).

## 7. Update endpoints

`UpdateManager.RELEASES_API = https://api.github.com/repos/HMXPANEL/HMXHARNESS/releases`
(`data/update/UpdateManager.kt:259-260`); fetch skips drafts, selects newest parseable
compatible version (`:132-158,294-332`). `UpdateInstallPolicy.trustedAsset()` requires
HTTPS `github.com`, exact `/HMXPANEL/HMXHARNESS/releases/download/` prefix, no query/
fragment/userinfo (`:17-21`); 512 MiB cap, byte-count check, **optional** SHA-256 (`:160-194`),
signer/version/ABI validation (`:196-216`; `UpdateInstallPolicy.kt:22-26`). Third-party
endpoints: CodeGraph (`data/env/CodeGraphManager.kt:141-171`), GPU runtime
(`Sanuu7/AH-GPU-Runtime`, `GpuDownloadWorker.kt:65`).

## 8. Gaps

1. Stale CI comment about NDK (docs only).
2. 555 instrumented tests not gated by CI (`android-ci.yml:61-62` runs only unit tests).
3. `lint-baseline.yml` omits NDK → baseline may drift from native-enabled main CI.
4. Release is debug-signed; `checkReleaseBuilds = false` → no release lint gate.
5. Broad `repo` OAuth scope; PAT code accepts any verified token (no client-side scope check).
6. Token material in toolchain `gitconfig`; `gitGlobalConfig()` uses plain `writeText`
   (`LinuxEnvironment.kt:1119-1127`) and shell sync `> path && chmod 600` (`:1200-1212`)
   creates a brief window before chmod, unlike `writePrivate()` 0600-from-creation.
7. Redaction is shape/name-based; novel secrets in errors may survive.
8. Release digest is optional (`UpdateManager.kt:189,327`).

## 9. Verification summary

| Check | Result |
| --- | --- |
| No client secret in source | PASS — only MCP DCR field, absence assertions and docs |
| No force-push in app Git tools | PASS — plain explicit-refspec push; `-D` only for branch delete |
| Artifact upload config | PASS — 90-day CI/release artifacts; live artifact 110,461,001 B on success run |
| Client ID only in gitignored local.properties | PASS — all injectors append to it; env fallbacks exist; no literal committed |
