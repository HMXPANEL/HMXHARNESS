# HMXHARNESS — Integration Changelog

- **Repository:** https://github.com/HMXPANEL/HMXHARNESS
- **Branch:** `main`
- **Date:** 2026-11-23
- **Consolidated application commit:** `32127b31`
- **Verified run:** https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45204023911

This changelog records every branch, commit range and change that became part of
`main`, plus what was deliberately left out and why.

---

## 1. Integration events

| # | Source | Range | Method | Result |
|---:|---|---|---|---|
| 1 | `origin/main` | `c2f12baf..3df84a94` | `merge --ff-only` | local `main` synced to remote `main` |
| 2 | `origin/feature/github-device-flow-open-source` | `3df84a94..32127b31` (38 commits) | `merge --ff-only` | `main` = `32127b31` |
| 3 | Reports commit (this document set) | `32127b31..HEAD` | commit + push | docs only, no code change |

- **Merge conflicts:** none (linear history).
- **Cherry-picks:** none.
- **Force-push / history rewrite:** none.
- **Branch deletions:** none.

The 38 commits of event 2 include the two prior feature branches, which are
therefore integrated transitively:

- `feature/hmxharness-liquid-glass-ui` (`5fa33b2b`, 19 commits)
- `feature/hmxharness-liquid-glass-final` (`1719471d`, 8 commits)
- `feature/github-device-flow-open-source` (11 own commits)

---

## 2. Commits integrated — Liquid Glass UI (`3df84a94..5fa33b2b`, 19)

```
53573441 feat(ui): replace existing interface with Liquid Glass design
7f64a238 fix(ci): run each screenshot command in its own shell
865ea07a fix(ci): capture screenshots via device file and keep diagnostics on failure
a8f137e2 fix(ci): bound every adb call so emulator QA cannot hang
cac90368 fix(ci): use GL swiftshader and capture earlier
2ea2d627 fix(ci): reduce capture framebuffer and give emulator more RAM
a7374d7f fix(ci): capture screens in-app to avoid emulator screencap crash
d72d9746 feat(ui): neutral liquid glass palettes and working light/dark themes
b94f8b6c ci(visual): capture host-side screenshots in both themes
4a97a1d6 ci(visual): split themes across fresh emulators and save host screenshots by absolute path
4dc4de20 ci(visual): warm shader cache and force dark theme via showcase
66038dfd ci(visual): allow single-theme capture runs
efa6a1d3 ci(visual): allow reusing a prior APK for fast theme captures
85073b27 fix(visual): open drawer deterministically and capture chat earlier
755a2d8a ci(visual): keep one showcase process and switch screens by broadcast
4e53c41f fix(visual): keep drawer open for deterministic screenshot capture
7e6eb44a ci(visual): dismiss ANR prompts and capture providers earlier
fc446add ci(visual): add single-shot light drawer capture
5fa33b2b docs: record real light/dark visual QA and final cloud build
```

## 3. Commits integrated — Liquid Glass final (`5fa33b2b..1719471d`, 8)

```
29ada625 feat(ui): balanced glass opacity, adaptive rendering tiers and shared components
cf989930 fix(ui): correct glass alias and canvas blur types
6856d3b6 feat(qa): category screenshot matrix and measured performance profiles
46f843b3 fix(qa): correct perf math, sheet args and SettingsScreen visibility
fde393d9 fix(qa): use android.view.Choreographer
53987768 ci(qa): consolidate category and performance matrices into visual QA workflow
0959773e feat(qa): menu category, forced glass tiers and filtered captures
1719471d fix(qa): move matrix filter to step level
```

## 4. Commits integrated — GitHub Device Flow + open source (`1719471d..32127b31`, 11)

```
e169ef40 feat(github): add Device Flow card to connected accounts
f9dccd39 ci(github): inject HMX_GITHUB_CLIENT_ID into cloud builds
ce148634 docs: rewrite README and open-source policies
5e100284 ci(github): accept the Client ID as a variable or secret
3aea0366 refactor(github): avoid local name shadowing the identity helper
5030a1da docs: add two new translatable strings
5c090d5d docs: add two new translatable strings across all locales
f4de6fed trigger: new CI run for APK build test
0c833e64 fix(res): remove duplicate settings_page_accounts_desc, add only new strings
f1f2e7b8 fix(res): delete stray strings.xml.bak files breaking resource merge
32127b31 fix(lint): read GitHub card strings via stringResource
```

> `5c090d5d` / `5030a1da` were intermediate localization attempts later corrected
> by `0c833e64`. They are retained to preserve history; the final tree is
> validated and free of duplicate keys. `f4de6fed` is a CI trigger commit.

---

## 5. Change inventory (`3df84a94..32127b31`)

**75 files changed, 2,949 insertions(+), 594 deletions(-)**

### Features
- Full Liquid Glass UI replacement (tokens, components, modifiers, palette,
  quality tiers, glass-aware bubbles, header, tools sheet, settings, setup).
- GitHub OAuth Device Flow sign-in in Connected Accounts with avatar, status,
  reconnect, confirmed disconnect and loading/error states.
- `HMX_GITHUB_CLIENT_ID` resolution from `local.properties` or environment.

### Bug fixes
- Correct glass alias and canvas blur types; perf math; `Choreographer` use;
  matrix filter; settings search/visibility.
- Resource merge: duplicate key removed; stray `.bak` files removed.
- Lint: configuration-aware `stringResource` usage.

### CI/CD
- `android-ci.yml`: Client ID injection from variable or secret.
- `android-release.yml`: Client ID injection.
- `android-visual-qa.yml`: `github` screen, category/performance matrices,
  host-side screenshot capture, hardened emulator handling.

### Localization
- 21 locales updated; two new keys added; existing accounts description updated
  in place.

### Documentation
- `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`,
  `CHANGELOG.md`, `.editorconfig`, `.github/PULL_REQUEST_TEMPLATE.md`.
- UI/feature/visual reports under `docs/`.

### Debug tooling
- `GlassShowcaseActivity`, `GlassPerfActivity`, `scripts/glass-capture.sh`,
  `scripts/glass-perf.sh`.

---

## 6. Intentional exclusions

| Branch / ref | Why not integrated |
|---|---|
| `feature/hmxharness-transformation` (`bbd2f8a1`) | Ancestor of `main`; already fully present. |
| `lint-baseline-gen` (`5e418195`) | Ancestor of `main`; baseline already present. |
| `upstream/main` (`c2f12baf`) | Original author's repository; read-only, never pushed. |

No useful unmerged work was left behind: every commit reachable from any
`origin` branch is now reachable from `main`, except the intentional exclusions
above which are already contained.

---

## 7. Verification

| Check | Result |
|---|---|
| `main` == `origin/main` == `32127b31` (before docs) | ✅ |
| Tree clean after integration | ✅ |
| No duplicate classes/declarations | ✅ |
| Dependency/manifest consistency | ✅ |
| Unit tests | ✅ pass |
| Lint | ✅ 0 errors |
| Debug + release APK | ✅ built |
| APK artifact uploaded | ✅ `hmxharness-apks` (ID `14837630763`) |
| Run URL | https://github.com/HMXPANEL/HMXHARNESS/actions/runs/45204023911 |

---

## 8. Final state

- **Final consolidated application commit:** `32127b31`
- **Documentation commit:** the commit that adds this file (child of `32127b31`)
- **`main` HEAD after push:** reported in the accompanying summary; it equals the
  documentation commit.
- **Artifact:** `hmxharness-apks` — debug 107,205,992 bytes, release 69,106,020 bytes.
- **Release applicationId:** `com.hmx.harness`, version `4.0` (`40`).
