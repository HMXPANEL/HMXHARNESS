# HMXHARNESS — Legal Attribution Review

**Scope:** MIT license presentation, upstream attribution, third-party notices,
copyright holders and license-related documentation in the current HMXHARNESS
repository.
**Branch:** `docs/hmxharness-complete-documentation`
**Evidence:** direct inspection of `LICENSE`, `README.md`, `app/src/main/assets/licenses/`,
`app/src/main/assets/richtext/`, `app/src/main/assets/editor-tools/`,
`app/src/main/res/values*/strings.xml`, schema headers and the About screen in
`app/src/main/java/com/hmx/harness/ui/settings/SettingsScreen.kt`.
**Method:** file reads and `git grep`; no previous audit report was treated as evidence.

## 1. Findings

### 1.1 Root LICENSE

- The root `LICENSE` is the MIT License and the original notice
  `Copyright (c) 2026 Sanuu7` was present before this work.
- **Change performed:** a clearly separated HMXHARNESS project-information header
  was added above the license text. It states that the project is an
  independently maintained derivative of the MIT-licensed AndroidHarness
  codebase and that the original copyright and terms are preserved.
- **Verification:** the remaining license block was compared byte-for-byte with
  `git show HEAD:LICENSE` and is identical. No MIT permission or disclaimer text
  was changed, removed or reworded.

### 1.2 Upstream attribution

| Location | Finding | Action |
| --- | --- | --- |
| `LICENSE:3` | `Copyright (c) 2026 Sanuu7` | Preserved unchanged. |
| `README.md` → "License and legal attribution" | Previously there was no matching section, although the introduction linked to `#attribution-and-upstream` (a broken anchor). | Added a full section that distinguishes original upstream authorship, the HMXHARNESS project identity and third-party components. The broken anchor was replaced with a working link. |
| `docs/expo/*` | Existing Expo documents contained upstream attribution mixed with unsupported old-repository commit-history claims. | Rewritten: upstream attribution is retained; commit hashes and commit-count claims that cannot be verified in this fresh repository were removed. |
| About screen (`SettingsScreen.kt`) | Contains an upstream-author row (`Sanuu`, `github.com/Sanuu7`, `AUTHOR_URL`) and a separate HMXHARNESS source-code row (`REPO_URL` = current repository). | Preserved. The author row is upstream attribution, not a product name, so replacing it with HMXHARNESS would falsify authorship. The product identity is carried by the app name, the source-code row and the license strings. |

### 1.3 Third-party notices

Bundled notices were found and left untouched:

- `app/src/main/assets/licenses/`: `backdrop.txt`, `caveman.txt`,
  `dhizuku-api.txt`, `llama.cpp.txt`, `lucide.txt`, `noto_sans_mono.txt`,
  `shizuku.txt`, `vulkan-headers.txt`.
- `app/src/main/assets/richtext/katex/LICENSE`,
  `app/src/main/assets/richtext/mermaid/LICENSE`.
- `app/src/main/assets/editor-tools/LICENSES.txt`.
- In-app strings such as `settings_caveman_attribution`
  ("Adapted from Julius Brussee's MIT-licensed Caveman skills. Copyright (c) 2026
  Julius Brussee …") and `settings_local_models_llama_license` ("llama.cpp · MIT
  license").

No third-party copyright notice or dependency license was renamed, removed or
replaced with HMXHARNESS.

### 1.4 Identifiers deliberately not renamed

- Application ID `com.hmx.harness` (namespace `com.hmx.harness`): unchanged.
- Room database schema version 27, 14 entities and 23 migrations: unchanged.
- Internal identifiers that preserve upstream or third-party meaning were left
  as-is, including the bundled skill `androidharness-environment`, plugin
  discovery keyword `androidharness-plugin`, backup format string
  `androidharness-chats`, `/data/local/tmp/androidharness` paths and
  `ui_androidharness_*` string-resource **names**. Renaming these would change
  internal behaviour or break compatibility without a technical reason.
- The localized string value `local_gpu_runtime_repository`
  ("Source: Sanuu7/AH-GPU-Runtime") is third-party attribution and was preserved.

### 1.5 Schema/documentation metadata

- `docs/harness.schema.json`: the `$id` canonical URL and `title` referred to the
  old upstream repository and product name. They were updated to the verified
  current repository URL and `HMXHARNESS workspace configuration`. Validation is
  performed locally by the app and does not download the schema, so this change
  does not alter runtime behaviour.
- `docs/configuration/plugin.schema.json`: `title` updated from
  `AndroidHarness plugin bundle` to `HMXHARNESS plugin bundle`.

## 2. Lawyer-facing summary

- MIT terms and the original copyright holder remain intact.
- HMXHARNESS is presented as the current **product identity**, not as the author
  of all inherited source code.
- Upstream authorship and third-party authorship are explicitly retained.
- No proprietary license, invented copyright year, invented company name or
  exclusive-ownership claim was introduced.
- Historical Git authorship from the previous repository could not be migrated
  and is not claimed; provenance is documented textually.

## 3. Remaining gaps

- The repository is a fresh migration with no original Git history, so commit-level
  upstream authorship evidence cannot be shown from this repository. Attribution
  relies on LICENSE, README, bundled notices and the About screen.
- No legal review by a qualified professional has been performed; this is an
  engineering audit, not legal advice.
