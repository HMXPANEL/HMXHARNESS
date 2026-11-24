# HMXHARNESS — Documentation Rewrite Report

**Scope:** all documentation rewritten or corrected in the current HMXHARNESS
repository during the complete documentation pass.
**Branch:** `docs/hmxharness-complete-documentation`
**Base commit:** `ae41ac7399d2bdfa6248cbf4c00d362451675990` on `main`.
**Method:** direct inspection of the working tree; existing reports were not
treated as implementation evidence.

## 1. Inventory before rewriting

Direct measurement found:

- 2,526 files on disk, 2,513 tracked by Git; 13 documentation files under `docs/`
  are present but ignored by the `docs/*` rule in `.gitignore`.
- 143 Markdown files on disk, 130 tracked.
- 3 README variants: `README.md`, `docs/configuration/README.md`,
  `repowiki/README.md`.
- 8 Markdown files in `docs/expo/` (1,310 lines total).
- 4 tracked reports in `docs/audits/` before this pass.
- 75 generated wiki pages under `repowiki/` plus its README.
- 39 bundled skill Markdown files under `app/src/main/assets/skills/`.
- `CODE_OF_CONDUCT.md` and `CONTRIBUTING.md` files did **not** exist, although
  broken references to them did.

## 2. Rewrites performed

### 2.1 Root README (`README.md`)

Fully rewritten with the requested 22-section structure: overview, problem
statement, target audience, core features, screenshots, technology stack,
architecture overview, AI models and providers, local GGUF support, installation,
configuration, GitHub integration, cloud build instructions, privacy and
security, known limitations, FAQ, roadmap, license and legal attribution, issues
and releases, and acknowledgements.

Corrections made:
- Removed the broken `#attribution-and-upstream` anchor and replaced it with a
  real "License and legal attribution" section.
- Removed broken links to `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md`.
- Removed the Google Play install link (Play publishing is not part of this
  project) and pointed to GitHub Releases/Actions instead.
- Replaced old CI evidence with the verified current run
  `45338701934` (success) and the current artifact.
- Stated that current builds are debug-signed alpha APKs, not Play Store ready.
- Disclosed that `HMX_GITHUB_CLIENT_ID` is not currently configured.
- Used only screenshot paths that exist under `.github/assets/showcase/`.

### 2.2 Expo documentation (`docs/expo/`, 8 files)

All eight Markdown files were regenerated from the current repository:
`HMXHARNESS_PROJECT_OVERVIEW.md`, `HMXHARNESS_ARCHITECTURE.md`,
`HMXHARNESS_CUSTOM_MODULES.md`, `HMXHARNESS_TECH_STACK.md`,
`HMXHARNESS_FEATURE_VERIFICATION.md`,
`HMXHARNESS_UNIQUENESS_AND_COMPARISON.md`,
`HMXHARNESS_CHALLENGES_AND_COSTS.md`,
`HMXHARNESS_EXPO_MASTER_FACTSHEET.md`.

Previous claims corrected: old-repository commit hashes and commit counts,
branch-consolidation stories, obsolete CI run IDs/artifact IDs/sizes, and
"from scratch" implications. Upstream attribution to AndroidHarness/Sanuu7 was
retained where it is legally and factually required.

### 2.3 Privacy policy (`PRIVACY.md`)

Rewritten from 60 lines to a 22-section policy. See
[HMXHARNESS_PRIVACY_REVIEW.md](HMXHARNESS_PRIVACY_REVIEW.md).

### 2.4 Other READMEs and metadata

- `docs/configuration/README.md`: HMXHARNESS added to the title.
- `repowiki/README.md`: stale generated commit hash/date replaced with a
  fresh-migration note and the current repository URL.
- `docs/harness.schema.json`, `docs/configuration/plugin.schema.json`: titles
  (and the schema `$id`) changed to HMXHARNESS/current repository.
- `LICENSE`: separated HMXHARNESS project-information header added; MIT block
  byte-identical to the original.
- `.github/PULL_REQUEST_TEMPLATE.md`: broken Code of Conduct checklist item
  removed.
- `CHANGELOG.md`, `FEATURES.md`: obsolete `CONTRIBUTING.md` mentions removed.
- Two older audit files: obsolete `CONTRIBUTING.md`/`CODE_OF_CONDUCT.md` entries
  removed.
- 22 locale `strings.xml` files: `glass_title` product token corrected from
  AHarness/Aharness variants to HMXHARNESS while preserving translations.

## 3. Files deliberately not rewritten

- `SECURITY.md`: already HMXHARNESS-branded, source-referenced and current; only
  reviewed, not structurally rewritten.
- `FEATURES.md` and `CHANGELOG.md`: large historical references; only the
  obsolete CONTRIBUTING mentions were removed, not the content.
- The 39 bundled skill Markdown files and 75 `repowiki/` pages: product
  documentation that describes real source APIs; a global rename would risk
  changing instructions and internal identifiers. They were scanned for obsolete
  branding and left intact apart from the wiki README.
- The 13 untracked `docs/` root reports: preserved as existing user work and not
  overwritten.

## 4. Validation results

- All README, Expo, privacy and audit files use HMXHARNESS as the product name.
- No references to the removed `CODE_OF_CONDUCT.md` or `CONTRIBUTING.md` remain
  in tracked files.
- No obsolete `AHarness` product token remains in `app/src/main/res/values*`.
- README links to local files resolve to tracked paths.
- No credentials or tokens were added to any documentation file.
- Gradle configuration and GitHub Actions workflow files were not modified.

## 5. Remaining gaps

- The 13 ignored `docs/` root reports are not tracked in Git and therefore are
  not part of this branch's committed documentation set. They were left as found.
- No application source behaviour was changed except 22 localized string values
  for the glass feature title. Package ID, database schema, AI provider routing,
  authentication and internal APIs are unchanged.
- No physical-device verification of the rewritten user-facing strings was
  performed; correctness was checked by XML inspection only.
