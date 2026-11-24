# HMXHARNESS — Android Coding Environment Audit

**Audit branch:** `audit/hmxharness-full-source-audit`
**Legend:** `[SRC]` source verified · `[UNIT]` has JVM unit test · `[USER-REPORTED]`
from the project brief, **not reproduced here** · `[UNVERIFIED]` no evidence found.
No runtime/device execution was performed.

## 1. Project types supported — and not supported

There is **no project-type registry**. A workspace is one folder, with four kinds
(`workspace/WorkspaceManager.kt:36-46`, constants `:478-482`, `fsFor()` `:238-253`):

| Kind | Backend | Shell | Evidence |
| --- | --- | --- | --- |
| APP | `FileFs(appPrivateRoot)` | yes (app UID) | `WorkspaceManager.kt:47-50,252,424-428` |
| SAF | `SafFs` or upgraded `FileFs` if URI maps to a writable path | only if mapped | `:241-249,283-288,435-444`; `SafPathResolver.kt:17-49` (cloud → null) |
| SHELL | `FileFs` (`UnboundedFileFs` in Full access) | yes, needs grant/Shizuku | `:250-251,393-409,429-433`; `WorkspaceFs.kt:270-308` |
| SSH | `SshFs` (Termux SSH default `127.0.0.1:8022`) | yes, remote | `:238-240,378-391`; `SshConnections.kt:18-38`; `SshFs.kt:11-21` |

Chat grants can layer multiple workspaces (`ChatWorkspaces.kt:9-31,118-141`); `shellRoot` is
deliberately null for SAF/SSH/chat-multi wrappers. `agent/GrantPersistence.kt:41-44` refuses
to persist "Always" approvals for `pkg_install`, `android_sdk_install`, `git_push`, etc.

**Workable `[SRC]`:** generic repos/any folder tree (browse/edit/search/git —
`FilesScreen.kt:122-125`, `tools/GitTools.kt`); GitHub import via shell `git clone`
(`GitHubWorkspaces.kt:109-139`, requires `gitToolsReady` `:50-52,111`); web HTML/CSS/JS/TS
(editor tools + static preview); Gradle-less Java+XML Android (§6); Android Gradle with
documented caveats.

**Not supported / not guaranteed `[SRC]`:**
- Gradle-less route is **Java only** — `skills/android-build/SKILL.md:35`: "AARs are not. Kotlin, Compose or AndroidX means route B."
- "Every Android project" — `AndroidBuildToolchain.kt:84-85` says readiness "does not check" SDK/platform/project compatibility; Gradle ref warns <4 GB RAM kills builds (`:44`), aapt2/AGP alignment (`:64`), SDK license acceptance (`:24-28`), JDK matching (`:63`). AAB is Gradle-only (`SKILL.md:49`).
- Server-side execution in the preview origin — `WorkspacePathHandler.kt:32-46,79-95` serves files with MIME types only, no execution; dynamic sites need a dev server run in the terminal (`BrowserController.kt:685-690,733`).
- Language-server features for Kotlin/Java/Python/C/C++/Go/Rust/Swift — absent; keyword colouring only (§2).
- iOS/other platform builds — no code path found `[UNVERIFIED]`.

## 2. Editor capabilities `[SRC]`

sora-editor core (`app/build.gradle.kts:264-265`): gutter line numbers, undo/redo
(`CodeEditorScreen.kt:348-349,364-370`), find bar with case/regex/replace
(`:677-690,917-1010`), go-to-line (`:736`), word wrap, themed colours (`:818-850`).
Tabs and durable drafts: `WorkspaceEditorScreen.kt:67-98,165-173`, `EditorBufferViewModel.kt:18-58`,
`EditorFileCodec.kt:14-100` (lossless BOM/charset/EOL), 16 MB ceiling (`CodeEditorScreen.kt:89-90`).
Syntax colouring for 8 modes (`CodeTokenizer.kt:32-41,126-151`; `BasicCodeLanguage.kt:62-116`).
Project-wide bounded search (512 KB/file), quick open, command palette
(`WorkspaceSearch.kt:23,45-75`). Diff: on-disk conflict compare/reload/keep-mine
(`CodeEditorScreen.kt:751-787`), per-file `+N −M` badges (`FilesScreen.kt:122-129,477,563`).

**Semantic features are file-type gated** (`SmartEditorSupport.kt:9-21`):
- completion: js/jsx/ts/tsx/mjs/cjs/mts/cts + html/htm/css/scss/less/json/jsonc
- navigation (definition/references): scripts + css/scss/less
- rename: scripts only; outline: completion set + md; format: outline set + yaml
- implementation: locked-down WebView worker (`SmartEditorEngine.kt:24-93`, 20 s timeout
  `:120-127`), bundled TypeScript 5.9.3 / Prettier 3.9.6 / vscode language services
  (`assets/editor-tools/LICENSES.txt:1`, `scripts/editor-tools/package.json`); indexing
  ≤500 files/8 MB (`SmartEditorWorkspace.kt:38-85`).

**Guarded against over-advertising `[UNIT]`:** `SmartEditorEditsTest.kt:29-38` asserts
`rename("styles.scss")==false`, `completion("guide.md")==false`, `format("Main.kt")==false`,
`navigation("Main.kt")==false`. UI hides actions behind these predicates and the
"Language support" dialog says other languages keep syntax highlighting only.

## 3. Toolchain bootstrap `[SRC]`

Self-contained Termux-derived userspace in app-private storage
(`LinuxEnvironment.kt:294-298,307`), deployed to `/data/local/tmp/<pkg>/linux` via
Shizuku untar (`:309-311,1598-1628`). Packages: `corePackages = bash, busybox,
ca-certificates, termux-exec, git` (`:405`); `fullPackages` adds `gh, python, python-pip,
nodejs, npm` (`:408`); Android build adds `openjdk-21, d8, apksigner, aapt2` + optional
Gradle (`tools/AndroidSdkTools.kt:111,120`). `android.jar` API 26-36 (default 35) from
`dl.google.com`, SHA-1 verified (`data/env/AndroidSdk.kt:13-38,44-137`).

`install()`/`installLocked()` resolves dependency closure, downloads `.deb`s in parallel
(4 at a time, 8-lookahead), verifies **SHA-256 from the official Termux index**, unpacks,
repairs launchers, then marks installed (`LinuxEnvironment.kt:411-432,571-682,1966-1971`);
ranked mirrors are checksum-gated (`PackageMirrors.kt:15-47,59-74`). Repairs include
symlink/shebang/git-SHELL_PATH fixes (`:722-733,740-788,916-954`). Limitations: network +
approval required, Linux env must be Ready before SDK setup, aapt2 must be the aarch64
Termux package, `android_sdk_install` rejects SSH workspaces (`AndroidSdkTools.kt:65-67`).
`[UNIT]` `AndroidSdkToolsTest.kt:65-135`, `AndroidBuildToolchainTest.kt:11-84`.

## 4. Terminal tiers and requirements `[SRC]`

`ShellTierRouter.kt:25-37` defines `TERMUX_SSH | ROOT | PRIVILEGED | APP_LINUX | TOYBOX`.
`resolveTier()` `:68-82`: app-data cwd → app-UID toolchain (APP_LINUX) or toybox; shared
storage/system paths → PRIVILEGED if Shizuku granted, else APP_LINUX/TOYBOX; SSH bypasses
routing (`:136-140`). PRIVILEGED = Shizuku shell/root UID via deployed toolchain else
`/system/bin/sh` (`:166-219`); provisioning via Shizuku/Wireless ADB
(`DeviceAccessManager.kt:193-248,315-336`). ROOT needs a verified backend + per-project
permission, no silent fallback (`ShellTool.kt:33,129-134`). Interactive PTY requires the
NDK-built `harness_pty` (`terminal/Pty.kt:58-78`; unavailable on no-NDK builds). Wireless
ADB pairing stack under `data/env/adb/*` (`AdbClient.kt`, `AdbPairingClient.kt`, etc.).
Limits: shell output capped at 100,000 chars (`ShellTool.kt:13,123-125`), timeout default
120 s / max 600 s (`:38,66-67`); SAF workspaces lack a real path and run in a fallback dir.

## 5. Preview workflow `[SRC]`

`browser/WorkspacePathHandler.kt` serves the active workspace at fixed origin
`https://harness.workspace/ws/...` via `WebViewAssetLoader` interception — "no sockets,
ports, health checks" (`:8-19`); used by both headless agent WebView and visible sheet
(`BrowserController.kt:215-250`). Directories map to `index.html`; traversal/404 handled
(`:32-46,59-77`); static MIME map (`:79-95`). `navigate()` accepts external URLs,
localhost dev servers (`LocalPortProbe.normalizeLocalUrl`, `:733`) and workspace-relative
HTML files (`:685-719`). **It is static file serving, not server-side execution.**

**[USER-REPORTED, not reproduced]:** a "Student Attendance Management website preview"
was exercised on a physical device. Source confirms a preview mechanism exists but does
not confirm that site or its runtime behaviour.

## 6. On-device build workflow `[SRC]` — not CI-verified

`skills/android-build/SKILL.md`: (1) one approval card runs `android_sdk_install`
(OpenJDK 21, D8, aapt2, apksigner, optional Gradle + `android.jar`) — needs Linux env and
network; (2) **Route A (default, no Gradle)**: copy `templates/hello-java/*`, run
`sh build-apk.sh` → aapt2 → javac → d8 → debug-sign → `apksigner verify` (`SKILL.md:30-35`,
`scripts/build-apk.sh:1-100`), Java only, no AAR/Kotlin/Compose; (3) **Route B (Gradle)**:
`./gradlew :app:assembleDebug` with aapt2-from-Maven override, `--no-daemon`, capped heap
and workers (`references/gradle-on-device.md:30-46`), needs SDK licenses/JDK/network;
(4) install via `pm install -r` with Shizuku/root or a file manager (`SKILL.md:40-46`).

**CI does not verify the on-device build.** `.github/workflows/android-ci.yml:61-86`
builds **HMXHARNESS itself** on ubuntu-latest; no CI job executes `build-apk.sh` or an
on-device Gradle build `[UNVERIFIED / no evidence]`. Unit tests exercise parsing/launcher
logic only.

## 7. Other items `[SRC]`

- **File sync:** `data/transfer/SyncConnections.kt:18-52` FTP/SFTP profiles (Commons Net /
  JSch); `SyncManager.kt:25-67` preview then apply (copy, no deletes); SSH workspaces expose
  remote FS (`SshFs.kt:52-138`, 32 MiB per-file cap at `:138`).
- **Skills catalogue:** `android-build`, `android-compose`, `android-crash`,
  `androidharness-environment`, `linux-env`, `git`, `browser`, `react-frontend`, `node-api`,
  `web-design`, `popular-web-designs`, `design-tokens`, `web-docs` plus process skills —
  agent guidance documents, not runtime capability by themselves.
- **Git tools** (`tools/GitTools.kt`): `git_status`, `git_diff`, `git_commit` (excludes
  `.harness/`, `:44-72`), `git_log`, `git_show`, `git_branch`, `git_branch_manage`,
  `git_checkout`, `git_push`, `git_pull` (ff/merge/rebase). Push/pull are approval-gated;
  HTTPS auth via stored token (`GitTools.kt:541-546`). No dedicated `git clone`/`git init`
  tool — clone happens in `GitHubWorkspaces.importRepository()` via shell.
