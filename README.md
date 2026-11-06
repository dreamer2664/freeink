# FreeInk — Freeform Notes & Whiteboard for Android

**100% free and open source. No ads. No subscriptions. No accounts. No tracking. Works fully offline.**

FreeInk combines handwriting notebooks, an infinite whiteboard, PDF annotation, images and
rich text notes in one calm, macOS-inspired workspace.

![License: MIT](https://img.shields.io/badge/license-MIT-blue) ![Platform: Android 8+](https://img.shields.io/badge/Android-8.0%2B-green) ![APK size](https://img.shields.io/badge/APK-~100KB-lightgrey)

## Download & install
1. Download `FreeInk.apk` from [Releases](https://github.com/dreamer2664/freeink/releases) (or `dist/FreeInk.apk` in this repo).
2. Install: `adb install -r FreeInk.apk`, or open the APK on your phone and allow installs from your browser/files app.

## Features (implemented & working)
- **Handwriting** — pressure-sensitive pen (stylus & finger), marker/highlighter, stroke eraser, color palette, thickness slider, palm rejection option, 60-step undo/redo
- **Notebooks** — multi-page documents, add/duplicate/delete pages, templates (blank, ruled, dotted, grid, dark), pinch zoom & pan
- **Infinite whiteboard** — endless canvas with grid/dotted backgrounds, zoom 15%–500%
- **Shapes & objects** — rectangle, ellipse, line, arrow, triangle (optional fill); lasso group selection & move; select/move/resize/rotate/delete images, text, shapes and strokes
- **Text boxes on canvas** + a **rich text editor** (headings, bold/italic/underline, colors, bullet/numbered lists, checklists)
- **PDF import & annotation** — Android document picker, per-page rendering, draw/highlight/write on pages, export annotated multi-page PDF (original pages preserved as rendered background)
- **Image import** — from picker or Android share sheet; images become movable/resizable/rotatable canvas objects
- **Organization** — folders, favorites, recents, search (titles + text content), rename, duplicate, move, delete
- **Export & share** — PNG and PDF export through the Android share sheet
- **Autosave**, offline-first local storage, light/dark themes, phone & tablet friendly
- Settings for default template, pen thickness, finger drawing / palm rejection

## Honest limitations
- Lasso selection uses the lasso's bounding box; eraser is stroke-level (not pixel-split).
- PDF export renders pages (with annotations) to PDF pages rather than editing the original PDF object stream.
- OCR / handwriting recognition are **not** included (no dependency-light on-device engine met the licensing bar) — see roadmap.
- Screenshots: see `docs/screenshots/README.md` (captured on device; none are fabricated).

## Build from source
Requirements: JDK 17 + Android SDK 34.
```bash
bash scripts/build-apk.sh      # -> dist/FreeInk.apk  (SDK build-tools only, no Gradle download)
python3 tests/run_tests.py
```
Or open the project in Android Studio (standard Gradle files are included).

## Roadmap
Pixel eraser · lasso freeform hit-testing · PNG export per page picker · WebDAV/sync (optional, self-hosted) · on-device handwriting recognition · keyboard shortcuts

## License
MIT — see `LICENSE`. Third-party dependencies: **none** (Android framework only; PDF via `PdfRenderer`/`PdfDocument`).
