# HMXHARNESS — CI Build Report

Workflow used: [`.github/workflows/android-ci.yml`](../../.github/workflows/android-ci.yml), triggered with `workflow_dispatch` on branch `security/hmxharness-remediation` because the existing workflow triggers only on `main`, `feature/**`, and pull requests. No new build architecture was introduced and no release signing was changed.

## Run history

| Run | Commit | Conclusion | Cause / action |
|---|---|---|---|
| [`45458229280`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45458229280) | `9ca377a` | failure | 2 unit-test failures: one encoded the old HTTP-permissive policy, one exercised custom proxy routing. Updated the policy test and added an explicit-proxy exception. |
| [`45459847086`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45459847086) | `0e72ec4` | failure | One lint error whose baseline line reference had shifted. Tried a baseline line update. |
| [`45462945739`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45462945739) | `379fae6` | failure | Same lint error persisted. Changed the code to a Compose-aware call. |
| [`45466412218`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45466412218) | `7b81cef` | failure | `stringResource` called from a non-composable lambda. Reverted the screen to its original form; inline validation in `ProviderSheet` already provided the specific message. |
| [`45467272004`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45467272004) | `cdd5e0c` | **success** | Batch 1 gate passed. |
| [`45475790127`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45475790127) | `ace1632` | failure | 30 localization tests failed: two new FTP strings lacked translations. |
| [`45477429850`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45477429850) | `74e9cce` | **success** | Batch 2 gate passed. |
| [`45487832898`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45487832898) | `d2bca9c` | failure | One test fixture had 11 characters instead of 12. |
| [`45489000340`](https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45489000340) | `7a8ab07` | **success** | Batch 3 gate and final build passed. |

No test, lint check, or APK assembly step was disabled to obtain a green build.

## Final run details

- **Run:** `45489000340`, job `138781946047`, duration 41m17s.
- **Steps passed:** checkout, OAuth client ID configuration, Gradle wrapper validation, JDK 21, NDK and shader compiler, Gradle setup, unit tests, Android lint (debug), debug + release APK assembly, APK verification, artifact upload, build summary.
- **Unit tests:** 2,480 completed, 0 failed.
- **Lint:** no unsuppressed errors.
- **Annotations:** upstream Node 20 deprecation and `setup-java@v4` deprecation notices only; not failures and not caused by this work.

## Artifacts

| Batch | Run | Artifact ID | Size |
|---|---|---:|---:|
| 1 | `45467272004` | `14958602743` | 110,480,759 bytes |
| 2 | `45477429850` | `14962519590` | 110,481,926 bytes |
| 3 (final) | `45489000340` | `14967389297` | 110,483,564 bytes |

Final artifact contents:

| File | Size | SHA-256 |
|---|---:|---|
| `HMXHARNESS-debug.apk` | 107,208,633 bytes | `9bb702ff95f3b40608982bdd3063c7c6c95475c63f1e0a41529e2e281a7f51a8` |
| `HMXHARNESS-release.apk` | 69,108,588 bytes | `bcb455c535bc6549c2141a15cebf1d0266c6af0b90de33a215058da50563d2fd` |

Verified with `aapt dump badging`:

- debug APK: `package: name='com.hmx.harness.debug' versionCode='40' versionName='4.0'`
- release APK: `package: name='com.hmx.harness' versionCode='40' versionName='4.0'`

Both are non-empty and the artifact is not expired. The debug APK carries the `.debug` suffix from the existing build type; the release APK was produced by the existing release configuration, which was deliberately left unchanged.
