# HMXHARNESS — Documentation Consistency Matrix

**Purpose:** record file-by-file branding, accuracy, link, legal-attribution and
validation status after the documentation rewrite.
**Branch:** `docs/hmxharness-complete-documentation`
**Validation date:** 2026-11-24

Columns: previous branding → updated HMXHARNESS branding → accuracy verification →
link verification → legal attribution status → final validation.

| File path | Previous project branding | Updated HMXHARNESS branding | Accuracy verification | Link verification | Legal attribution status | Final validation |
| --- | --- | --- | --- | --- | --- | --- |
| `README.md` | HMXHARNESS title, broken "#attribution-and-upstream" anchor, Google Play link, old CI run IDs | Full 22-section HMXHARNESS README; working "License and legal attribution" section | Versions, package ID, SDK levels and CI evidence checked against `app/build.gradle.kts` and Actions API | All local `.md`, `docs/`, `.github/workflows/` targets and 10 screenshot paths exist | Upstream author, original copyright and third-party notices described; no from-scratch claim | PASS |
| `PRIVACY.md` | HMXHARNESS name; vague "data stays between you and those services"; no cloud/local distinction | 22-section HMXHARNESS privacy policy with explicit cloud transmission language | Behaviour checked against manifest, `data/`, `llm/`, `github/`, `tools/`, `browser/`, `local/` | Links to `SECURITY.md` and issue tracker resolve | No invented retention/email/address/compliance; upstream links retained | PASS |
| `LICENSE` | MIT text only; no project identity | Separated HMXHARNESS project-information header above the license | MIT block compared byte-for-byte with `git show HEAD:LICENSE` — identical | n/a | `Copyright (c) 2026 Sanuu7` preserved; MIT terms unchanged | PASS |
| `docs/expo/HMXHARNESS_PROJECT_OVERVIEW.md` | HMXHARNESS, but old-repo commit hashes, 961-commit history and old CI runs | Regenerated from current source and current CI evidence | Source paths and measured counts verified directly | Internal links checked after rewrite | Upstream AndroidHarness/Sanuu7 attribution retained | PASS (post-rewrite check) |
| `docs/expo/HMXHARNESS_ARCHITECTURE.md` | HMXHARNESS, old commit reference in header | Regenerated architecture description | Paths verified under `app/src/main/java/com/hmx/harness/` | n/a external links | Third-party and upstream components distinguished | PASS (post-rewrite check) |
| `docs/expo/HMXHARNESS_CUSTOM_MODULES.md` | HMXHARNESS; `CONTRIBUTING.md` reference; old commit history | Regenerated with accurate custom/upstream/third-party split | `HmxTaskVerification` runtime gap documented | Obsolete `CONTRIBUTING.md` reference removed | Upstream and third-party authorship preserved | PASS (post-rewrite check) |
| `docs/expo/HMXHARNESS_TECH_STACK.md` | HMXHARNESS; old commits and counts | Regenerated from version catalog and build files | Versions and counts verified from `libs.versions.toml`, build files, `find` | n/a | Dependency names unchanged | PASS (post-rewrite check) |
| `docs/expo/HMXHARNESS_FEATURE_VERIFICATION.md` | HMXHARNESS; old CI run IDs; stale "8 verified / 26 implemented" summary | Regenerated; separates CI-verified, instrumented-only and hardware-unverified | All 23 features have source and test files; runtime gate gaps disclosed | n/a | No false authorship claims | PASS (post-rewrite check) |
| `docs/expo/HMXHARNESS_UNIQUENESS_AND_COMPARISON.md` | HMXHARNESS; "828/961 commits" claim | Regenerated with evidence-backed, non-exclusive claims | Comparison claims checked against source; no "first/only" claims | n/a | Upstream attribution retained | PASS (post-rewrite check) |
| `docs/expo/HMXHARNESS_CHALLENGES_AND_COSTS.md` | HMXHARNESS; `CONTRIBUTING.md`, old runs, branch-history stories | Regenerated around current migration and recovery evidence | No invented dollar amounts or retention periods | Obsolete `CONTRIBUTING.md` reference removed | Upstream provenance retained | PASS (post-rewrite check) |
| `docs/expo/HMXHARNESS_EXPO_MASTER_FACTSHEET.md` | HMXHARNESS; old commit, artifact ID/size, 961-commit factsheet | Regenerated with current run `45338701934` and artifact `14898111209` | Facts checked against Actions API and source counts | GitHub Actions link valid | Legal/provenance claims corrected | PASS (post-rewrite check) |
| `docs/configuration/README.md` | HMXHARNESS in body, generic title | Title `HMXHARNESS — Workspace configuration on Android` | Technical content unchanged and accurate | `../harness.schema.json` and `plugin-manager.md` exist | n/a | PASS |
| `docs/configuration/plugin.schema.json` | `AndroidHarness plugin bundle` | `HMXHARNESS plugin bundle` | Schema content unchanged | n/a | Third-party/json identifiers unchanged | PASS |
| `docs/harness.schema.json` | Old Sanuu7 raw URL; `AndroidHarness workspace configuration` | Current repo URL; `HMXHARNESS workspace configuration` | Local validation does not fetch `$id` | URL points to current repo path | Upstream copyright not affected | PASS |
| `repowiki/README.md` | Stale generated commit `aae55c4` and date | Fresh-migration note + current repository URL | 75 page list unchanged | All listed page links exist (index format) | n/a | PASS |
| `.github/PULL_REQUEST_TEMPLATE.md` | Link to removed `CODE_OF_CONDUCT.md` | Link removed | File did not exist | No broken local link | n/a | PASS |
| `CHANGELOG.md` | `CONTRIBUTING.md` mention | Mention removed | Historical content preserved | n/a | n/a | PASS |
| `FEATURES.md` | `CONTRIBUTING.md` mention | Mention removed | Feature content preserved | n/a | n/a | PASS |
| `docs/audits/HMXHARNESS_BRANCH_CONSOLIDATION_REPORT.md` | `CONTRIBUTING`/`CODE_OF_CONDUCT` file names | Removed, counts adjusted | Historical report preserved | n/a | Upstream URL retained as labelled historical provenance | PASS |
| `docs/audits/HMXHARNESS_INTEGRATION_CHANGELOG.md` | `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` | Removed | Historical content preserved | n/a | n/a | PASS |
| `app/src/main/res/values/` + 21 locale `strings.xml` | `AHarness`/`Aharness`/localized variants in `glass_title` | Product token `HMXHARNESS` in all 22 files | XML inspected; translations and quoting preserved | n/a | No copyright text changed | PASS — 0 obsolete tokens remain |
| `docs/audits/HMXHARNESS_SECOND_FULL_REPOSITORY_AUDIT.md` | n/a (new) | HMXHARNESS audit with source evidence | Counts measured; 23-feature classification with caveats | n/a | Provenance section present | PASS |
| `docs/audits/HMXHARNESS_DOCUMENTATION_REWRITE_REPORT.md` | n/a (new) | HMXHARNESS rewrite scope and results | File list matches working tree | n/a | n/a | PASS |
| `docs/audits/HMXHARNESS_PRIVACY_REVIEW.md` | n/a (new) | HMXHARNESS privacy review | Behaviour cross-checked with source and policy | Links resolve | No invented claims | PASS |
| `docs/audits/HMXHARNESS_BRANDING_CONSISTENCY_REPORT.md` | n/a (new) | HMXHARNESS branding classes A–G | `git grep` verification recorded | n/a | Classes B/C/F preserved | PASS |
| `docs/audits/HMXHARNESS_LEGAL_ATTRIBUTION_REVIEW.md` | n/a (new) | HMXHARNESS legal review | LICENSE byte comparison recorded | n/a | MIT and upstream notices preserved | PASS |
| This matrix | n/a (new) | HMXHARNESS consistency matrix | Final validation checks below | Links resolve | n/a | PASS |

## Final validation checks

| Check | Result |
| --- | --- |
| Obsolete `CODE_OF_CONDUCT.md` / `CONTRIBUTING.md` file references in tracked docs/templates | 0 after corrections |
| `AHarness` product token in `app/src/main/res/values*` | 0 after corrections |
| Old CI run IDs in newly rewritten docs | 0 after corrections |
| Older audit files containing historical run IDs | 3 files, each now carries an explicit "Historical document" notice; current evidence is in the second full repository audit |
| Screenshot paths in README exist | 10/10 |
| README local documentation links resolve | 11/11 |
| Gradle build files modified | none |
| GitHub Actions workflow files modified | none |
| Credentials/tokens added to documentation | none |
| Application ID / database schema / provider routing / authentication modified | none |
| GitHub Actions cloud build evidence used | run `45338701934`, conclusion `success`, artifact `hmxharness-apks` |
