# HMXHARNESS — Branding Consistency Report

**Scope:** repository-wide search for obsolete product names, repository names,
URLs and user-facing descriptions, followed by classified corrections.
**Branch:** `docs/hmxharness-complete-documentation`
**Evidence:** `git grep`, `find` and direct file reads on the current tree. Existing
audit reports were read as ordinary files, not as evidence.

## 1. Classification used

| Class | Meaning | Action |
| --- | --- | --- |
| A | Incorrect user-facing branding | Replace with HMXHARNESS |
| B | Legal copyright attribution | Preserve original legal information |
| C | Third-party dependency name | Preserve |
| D | Historical reference | Retain only when necessary and label it |
| E | Internal identifier | Do not rename without technical justification |
| F | Upstream project attribution | Preserve and describe accurately |
| G | Old repository URL | Replace with the verified current URL where appropriate |

Verified current URL: `https://github.com/HMXPANEL/HMXHARNESS.git`.

## 2. Corrections performed (Class A / G)

| File | Before | After | Class |
| --- | --- | --- | --- |
| `app/src/main/res/values/strings.xml` + 21 locale `strings.xml` files | `glass_title` values containing `AHarness`, `Aharness`, `A하네스`, `एहार्नेस`, or the mistranslated Vietnamese `Kính khai thác` | Product token replaced with `HMXHARNESS` while keeping each localized phrase and its original quoting | A |
| `README.md` | Full rewrite: HMXHARNESS title, tagline, sections and links; broken `#attribution-and-upstream` anchor | New 22-section README with a working "License and legal attribution" section | A, G |
| `docs/expo/*.md` (8 files) | HMXHARNESS headings but unsupported old-repository commit hashes, run IDs and historical claims | Regenerated from current source and current CI evidence | A, D |
| `docs/harness.schema.json` | `$id` = old upstream raw URL; `title` = `AndroidHarness workspace configuration` | Current repository URL; `HMXHARNESS workspace configuration` | A, G |
| `docs/configuration/plugin.schema.json` | `title` = `AndroidHarness plugin bundle` | `HMXHARNESS plugin bundle` | A |
| `docs/configuration/README.md` | Title `Workspace configuration on Android` | `HMXHARNESS — Workspace configuration on Android` | A |
| `repowiki/README.md` | Stale `Last updated 07/11/2026 … commit aae55c4` | Replaced with a fresh-migration note and current repository URL | D, G |
| `docs/audits/HMXHARNESS_BRANCH_CONSOLIDATION_REPORT.md` | `CONTRIBUTING` / `CODE_OF_CONDUCT` in documentation lists | Removed; counts adjusted | D |
| `docs/audits/HMXHARNESS_INTEGRATION_CHANGELOG.md` | `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` in documentation list | Removed | D |
| `.github/PULL_REQUEST_TEMPLATE.md` | Checklist link to removed `../CODE_OF_CONDUCT.md` | Removed | D |
| `CHANGELOG.md`, `FEATURES.md` | Inline mentions of `CONTRIBUTING.md` | Removed | D |
| `LICENSE` | No HMXHARNESS identity | Added separated project-information header; MIT block unchanged | B, A |

## 3. Preserved occurrences (Classes B–F)

| Occurrence | Path(s) | Why preserved |
| --- | --- | --- |
| `Copyright (c) 2026 Sanuu7` | `LICENSE` | Legally required original copyright notice. |
| `AndroidHarness` as upstream attribution | `README.md` "License and legal attribution", `docs/expo/*`, migration baseline audit | Honest provenance: HMXHARNESS is a derivative, not the original author. |
| `Sanuu` author row and `https://github.com/Sanuu7` | `app/src/main/java/com/hmx/harness/ui/settings/SettingsScreen.kt` | Upstream author attribution in About; replacing it would falsify authorship. |
| `Source: Sanuu7/AH-GPU-Runtime` | `app/src/main/res/values*/ui_strings.xml` | Third-party dependency attribution. |
| `androidharness-environment` skill, `androidharness-plugin` keyword, `androidharness-chats` backup format, `/data/local/tmp/androidharness` paths, `ui_androidharness_*` resource names | source and assets | Internal identifiers; renaming would change behaviour or compatibility. |
| `Brave`, `Tavily`, `Shizuku`, `libsu`, `Dhizuku`, `llama.cpp`, `sora-editor`, `PrismalAGSL`, `kyant0 Backdrop`, etc. | `gradle/libs.versions.toml`, `app/build.gradle.kts`, docs | Third-party dependency names. |
| Old upstream URL in a historical audit line | `docs/audits/HMXHARNESS_BRANCH_CONSOLIDATION_REPORT.md` | Historical provenance of the upstream remote, explicitly labelled "original author — never pushed". |
| `android-harness-website/` ignore rule | `.gitignore:46` | Internal ignore pattern for a directory that is not part of the published source tree. |

## 4. Verification results

- `git grep` for `AHarness|Aharness|A하네스|एहार्नेस|Kính khai thác` in
  `app/src/main/res/values*`: **0 matches** after correction.
- No `CODE_OF_CONDUCT.md` or `CONTRIBUTING.md` file exists, and all known
  references to them in tracked documentation and templates were removed.
- All documentation repository URLs point to `HMXPANEL/HMXHARNESS`, except
  explicitly labelled upstream/third-party links.
- Product identity in titles, headings and user-facing documentation is
  HMXHARNESS.

## 5. Known constraints

- Legacy internal identifiers listed in section 3 intentionally remain; they are
  not product branding.
- Old screenshots were not replaced with fabricated images. The README uses the
  real screenshots committed under `.github/assets/showcase/`.
- Branding assets such as `.github/assets/icon.svg` were not redrawn because no
  obsolete product name was found in them and inventing new artwork would not
  improve accuracy.
