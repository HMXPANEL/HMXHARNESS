# HMXHARNESS — Presentation Asset Manifest

**Deliverables** (in `docs/expo/presentation/final/`):

| File | Description |
| --- | --- |
| `HMXHARNESS_PROJECT_EXPO.pptx` | Editable 10-slide PowerPoint, 16:9, with embedded speaker notes |
| `HMXHARNESS_PROJECT_EXPO.pdf` | Matching 10-page PDF generated from the same slide specification |
| `HMXHARNESS_SPEAKER_NOTES.md` | Full 10-minute script, one section per slide |
| `HMXHARNESS_PROJECT_EXPO_JUDGE_QA.md` | 14 evidence-backed judge questions and answers |
| `HMXHARNESS_PRESENTATION_ASSET_MANIFEST.md` | This file |
| `slides-preview/slide-01.png … slide-10.png` | Rendered previews of all ten slides (1600×900) |
| `scripts/deck_spec.js` | Shared slide specification: all text, coordinates, colors, shapes |
| `scripts/build_deck.js` | Three renderers: PPTX (PptxGenJS), PDF (pdfkit), PNG (@napi-rs/canvas) |
| `scripts/build_assets.py` | Original artwork generator (PIL) — 7 decorative 3D-style images |
| `assets/fonts/Inter-Regular.ttf`, `assets/fonts/SpaceGrotesk.ttf` | Downloaded from Google Fonts (SIL Open Font Licence); used consistently in PPTX, PDF and previews |
| `assets/generated/*.png` | 7 original programmatic 3D-style artworks |
| `assets/screens/Screenshot_2026-11-24-14-02-*.jpg` | 3 developer-supplied device screenshots of the attendance demo (copied, originals untouched) |

## Original visual assets (7)

| Asset | Used on | Description |
| --- | --- | --- |
| `hero_phone.png` | Slide 1 | Floating phone with holographic AI interface, purple/cyan lighting |
| `problem_split.png` | Slide 2 | Desktop-dependent workflow vs mobile-first workflow |
| `solution_exploded.png` | Slide 3 | Phone with floating glass feature cards |
| `agent_brain.png` | Slide 5 | 3D neural network connected to tool cards |
| `cloud_phone.png` | Slide 8 | Cloud-to-phone data stream with APK delivery |
| `roadmap.png` | Slide 9 | Ascending isometric glass milestones |
| `closing.png` | Slide 10 | Phone with dissolving code particles |

All artwork was generated programmatically with PIL (gradients, glow, isometric geometry,
rounded glass panels). No stock photographs, no stock illustrations, no borrowed product
renderings.

## Actual application screenshots used (3)

Source: developer-supplied device screenshots found untracked in the repository root during
research; copied into `assets/screens/` without modifying the originals.

1. `Screenshot_2026-11-24-14-02-31-05_28c2859092379cd2f7b967065d899178.jpg` — Attendance dashboard (5 enrolled, 4 present, 1 absent, date selector, roster heading).
2. `Screenshot_2026-11-24-14-02-33-92_28c2859092379cd2f7b967065d899178.jpg` — Class roster with Present/Absent toggles for five students.
3. `Screenshot_2026-11-24-14-02-35-72_28c2859092379cd2f7b967065d899178.jpg` — Add-student form (name, roll number, Add student).

Slides 6 also contains four **empty placeholder frames** labelled `+ CHAT SCREENSHOT`,
`+ CODE EDITOR SCREENSHOT`, `+ TERMINAL SCREENSHOT`, `+ BROWSER PREVIEW SCREENSHOT`. These are
intentionally unfilled — no additional screenshots were fabricated.

## Design system

- **Canvas:** 13.333 × 7.5 in (16:9), 1920×1080 design reference; previews 1600×900.
- **Palette:** background `#080910`, secondary `#111629`, primary `#8B5CF6`, violet `#A78BFA`,
  cyan `#38BDF8`, text `#F8FAFC`, secondary text `#A1A1AA`.
- **Typography:** Inter (body/UI), Space Grotesk (display/headings), consistent across all three renderers.
- **Reusable components:** section header with accent bar, title and underline; glass cards;
  numbered step circles; connector lines; stat panels; footer rule with branding and slide number.
- **Branding:** HMXHARNESS throughout; upstream MIT attribution to AndroidHarness/Sanuu7 is
  retained on slide 9 and in the closing slide footer, as legally and factually required.

## Editability

PPTX content is native text boxes, shapes, lines, ellipses and images — no slide is a single
flattened image. Speaker notes are embedded in `ppt/notesSlides/`. Fonts are referenced by name
(Inter / Space Grotesk); the TTF files are bundled here so the PDF and previews render identically.
If a viewing machine lacks Inter or Space Grotesk, PowerPoint will substitute a similar sans font;
the PDF is unaffected.

## Generation and validation

```
cd docs/expo/presentation/final/scripts
python3 build_assets.py
NODE_PATH=/tmp/opencode/hmx_deck/node_modules node build_deck.js
```

Required node modules (`pptxgenjs`, `pdfkit`, `@napi-rs/canvas`) were installed in
`/tmp/opencode/hmx_deck` because the sdcard filesystem rejects npm writes. They are not
vendored into the repository; the scripts and all generated outputs are.

Validation performed:
- PPTX: 10 slides, 10 notes slides, images present, all text/shapes native.
- PDF: 10 pages, MediaBox `0 0 959.976 540` (13.333×7.5 in).
- Previews: 10 PNGs generated and inspected individually; slide 3 footnote overlap and slide 6
  placeholder spacing were corrected and re-rendered.

## Known limitations

- Image-generation credits were unavailable, so "3D" artwork is programmatic vector-style
  illustration rather than photorealistic rendering. It is original, on-palette and consistent.
- No LibreOffice/PDF renderer was available to convert the PPTX itself; the PDF is rendered from
  the same slide specification, so content and layout match by construction, but it is not a
  byte-level export of the PPTX.
- College name and team member names remain editable placeholders, as required.
- The attendance screenshots are developer-supplied and are labelled as such; the audit did not
  reproduce them.
