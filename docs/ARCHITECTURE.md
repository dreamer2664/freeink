# FreeInk Architecture

## Principles
Offline-first · zero third-party runtime dependencies · Android framework only · no accounts, no tracking, no ads.

## Stack decision
FreeInk is written in Java 17 against the Android framework (`minSdk 26`, `targetSdk 34`).
The brief preferred Kotlin/Compose; a framework-only implementation was chosen deliberately:
the custom ink canvas needs direct `Canvas`/`MotionEvent` control anyway, and avoiding
Compose/AndroidX keeps the APK at ~100 KB, builds in seconds with only SDK build-tools
(`scripts/build-apk.sh`), and removes all dependency/supply-chain risk. The project also
ships standard Gradle files (`build.gradle`, `app/build.gradle`) for Android Studio users.

## Modules
| Package | Responsibility |
|---|---|
| `app.freeink.data` | `CanvasDoc` (strokes/text/shapes/images/pages + JSON serialization), `DocStore` (library index, folders, import) |
| `app.freeink.canvas` | `InkView` — pressure-sensitive ink, pinch zoom/pan, lasso, selection, shapes, templates, undo/redo |
| `app.freeink.pdf` | `PdfHelper` — `PdfRenderer` page rendering with a small bitmap cache |
| `app.freeink.util` | `Ui` (macOS-inspired design tokens), `ExportUtil` (PNG/PDF export + share), `ShareProvider` (own FileProvider) |
| `app.freeink.ui` | `MainActivity` (library), `EditorActivity` (canvas), `TextNoteActivity` (rich text), `SettingsActivity` |

## Storage
`files/freeink/index.json` (library metadata) + one JSON file per canvas document,
media and imported PDFs stored alongside. Autosave debounced (600 ms) + on pause.

## Rendering / performance
Strokes store per-point pressure and are drawn as width-varying segments directly on the
hardware canvas. PDF pages render at page width into a 4-page bitmap cache. Undo/redo uses
JSON snapshots (max 60), simple and corruption-proof for document sizes this app targets.
