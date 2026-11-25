# HMXHARNESS Release Signing Report (HMX-001)

Status: **FIXED AND VERIFIED**
Date: 2026-11-25
Repository: `HMXPANEL/HMXHARNESS`
Branch: `main`
Verified commit: `8206414c6911bac0662c4a7a145eea4cbdbdecfe`

## 1. Finding

HMX-001: the `release` build type was signed with the shared Android debug
keystore (`signingConfig = signingConfigs.getByName("debug")`). Any APK produced
from the release variant therefore carried a publicly known debug certificate,
which makes it impossible to distinguish an official build from a locally
rebuilt one and blocks Play distribution and safe upgrade paths.

## 2. Remediation

### 2.1 Gradle signing configuration (`app/build.gradle.kts`)

- Added a dedicated `release` signing config populated exclusively from
  protected environment variables:
  - `RELEASE_KEYSTORE_FILE`
  - `RELEASE_KEYSTORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`
- `buildTypes.release.signingConfig` now points at that config. It never
  references the debug signing config.
- The debug build type keeps its existing stable debug keystore behaviour, so
  local debug installs are unchanged.
- Added a configuration-time guard: when any
  `assembleRelease`/`bundleRelease`/`packageRelease`/`installRelease` task is
  requested and the signing environment is incomplete, configuration fails with
  a message naming the missing variables and stating that debug signing will not
  be used. The keystore path is also checked for existence.
- The guard runs at configuration time (not in a task action) because Gradle
  9.7 rejects task actions that capture build-script references when storing the
  configuration cache. The first implementation used a `doFirst` task and failed
  every release build with `cannot serialize Gradle script object references`;
  it was replaced with the configuration-time check.

### 2.2 Release workflow (`.github/workflows/android-release.yml`)

Runs on pushes to `main` and on `v*` tags, plus manual dispatch.

- Fails before building if any of the four signing secrets is empty
  (`::error::Missing required signing secret: <name>`).
- Decodes `RELEASE_KEYSTORE_BASE64` into `/tmp/hmxharness-release.jks`, outside
  the checked-out repository, under `umask 077`, then `chmod 600`.
- Builds `:app:assembleRelease`.
- Verifies the APK with `apksigner verify --print-certs`, extracts the signer
  SHA-256 digest and compares it to the pinned expected value; a mismatch fails
  the job.
- Verifies `aapt dump badging` reports `com.hmx.harness`, versionCode `40`,
  versionName `4.0`.
- Copies the APK to `dist/`, writes `dist/HMXHARNESS-release.sha256`, and uploads
  both as the `hmxharness-signed-release` artifact.
- Removes the temporary keystore in an `if: always()` step, so it is deleted on
  success and failure alike. The keystore is never uploaded as an artifact.
- Secret values are passed only to the steps that need them and are never
  echoed; the summary publishes only the signer fingerprint, APK checksum and
  commit metadata.

### 2.3 CI workflow (`.github/workflows/android-ci.yml`)

The existing CI gate keeps running unit tests, lint and both APK builds. It now
also receives the signing environment, so the release variant under test is
signed exactly like a real release, and it removes the temporary keystore in an
`if: always()` step. Debug builds continue to use the debug keystore.

## 3. Verification evidence

Run: https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45591728722
Workflow: `HMXHARNESS Android Release Build`
Commit: `8206414c6911bac0662c4a7a145eea4cbdbdecfe`
Result: **success**

Signing secret validation:

```
All release signing secrets are present.
```

Signer certificate reported by `apksigner`:

```
V2 Signer: certificate DN: CN=hmxpanel, OU=HMXPANEL, O=HMXPANEL, L=mumbai, ST=Maharashtra, C=IN
V2 Signer: certificate SHA-256 digest: eee6dd0e68a80af4a7270fe62bbf525b921280c3471fba1960fca976c0478fc7
V2 Signer: certificate SHA-1 digest: 518456ad29149b8de48e784064e0eec19b8f457a
V2 Signer: certificate MD5 digest: 721c0aa64cdb5c866523fbf3dcd4cc47
Release signer certificate fingerprint verified.
```

Expected certificate SHA-256, as supplied with the keystore:

```
EE:E6:DD:0E:68:A8:0A:F4:A7:27:0F:E6:2B:BF:52:5B:92:12:80:C3:47:1F:BA:19:60:FC:A9:76:C0:47:8F:C7
```

Comparison: **match**. The signed APK carries the intended release certificate,
not the debug certificate.

Published artifact:

| Field | Value |
| --- | --- |
| Artifact | `hmxharness-signed-release` (id `15012007313`) |
| Artifact size | 36,634,017 bytes (zip) |
| APK size | 69,108,588 bytes |
| Package | `com.hmx.harness` |
| versionCode / versionName | `40` / `4.0` |
| APK SHA-256 | `6ce5d60b1e9b8500a1b930692fb78fca1329d3260a1f5ee1e6409b6544f5c0e3` |
| Signer cert SHA-256 | `EEE6DD0E68A80AF4A7270FE62BBF525B921280C3471FBA1960FCA976C0478FC7` |

The downloaded artifact's `HMXHARNESS-release.sha256` contains the same digest,
recomputed locally after download:

```
6ce5d60b1e9b8500a1b930692fb78fca1329d3260a1f5ee1e6409b6544f5c0e3  HMXHARNESS-release.apk
```

The same commit passed the full CI gate in run
https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45591728571
(`HMXHARNESS Android CI`): release signing secret validation, unit tests,
`lintDebug`, `assembleDebug` + `assembleRelease`, APK presence verification and
keystore cleanup all succeeded. Both APKs were produced with the expected sizes
(debug 107,208,633 bytes, release 69,108,588 bytes).

## 4. Status

| Item | Status |
| --- | --- |
| HMX-001 release signing | FIXED AND VERIFIED |
| Release workflow on `main` | BUILD PASSED |
| Signer certificate fingerprint | CERTIFICATE VERIFIED |
| Debug signing preserved separately | PASSED |
| Unit tests and lint (CI gate) | PASSED — run https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45591728571 |
| Missing-secret failure path | IMPLEMENTED (fails closed, no debug fallback) |
| Device install / upgrade testing | PENDING (no test device in this environment) |

## 5. Operational notes and residual risk

1. **Key custody.** The release private key exists only in the GitHub Actions
   secrets and the owner's offline keystore. No keystore bytes, Base64, alias,
   or password appear in the repository, build output, logs, or this report.
2. **Debug-signed installs cannot upgrade.** APKs previously distributed with
   the debug certificate have a different signing identity. Users must uninstall
   the old build once before installing the first release-signed APK. The
   `applicationId` itself is unchanged (`com.hmx.harness`).
3. **Certificate rotation.** The expected fingerprint is pinned in
   `.github/workflows/android-release.yml`. Any future key rotation must update
   that constant in the same change, or the release job will fail closed.
4. **Play App Signing.** If the app is later published on Play, Google's upload
   key or Play App Signing enrollment may replace this certificate for store
   installs. The pinned fingerprint should then be updated deliberately.
5. **Credential rotation.** The CI secrets are unchanged by this work. Rotate
   the keystore passwords if they were ever shared outside the secret store.
6. **Reproducibility.** The APK is not bit-for-bit reproducible; verification is
   based on the signer certificate and the published SHA-256 of each build.
7. **No device validation performed.** Signing, packaging and certificate
   verification are proven on the runner. Installing and upgrading the APK on a
   real device remains outstanding.

## 6. Files changed

- `app/build.gradle.kts` — dedicated release signing config, configuration-time
  validation, release build type wired to it.
- `.github/workflows/android-release.yml` — secret validation, keystore decode,
  signed build, signature/fingerprint/package verification, checksum, artifact
  upload, guaranteed cleanup.
- `.github/workflows/android-ci.yml` — signing environment for the release
  variant under test, guaranteed keystore cleanup.
