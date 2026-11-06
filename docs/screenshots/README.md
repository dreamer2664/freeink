# Screenshots

Screenshots in this folder must be **genuine captures of the running app** — never mockups.

## Status
⚠️ Not yet captured: the development sandbox used to build v1.0.0 has no Android emulator
(no KVM, 2 GB RAM), so no honest device screenshots could be produced there.

## How to capture (on any device/emulator)
```bash
adb install -r dist/FreeInk.apk
./scripts/capture-screenshots.sh   # walks the main screens and pulls PNGs here
```
Then commit the PNGs with descriptive names, e.g. `01-home-light.png`, `02-library-folders.png`,
`03-handwriting-editor.png`, `04-toolbar-colors.png`, `05-whiteboard.png`, `06-pdf-viewer.png`,
`07-pdf-annotation.png`, `08-image-import.png`, `09-text-editor.png`, `10-dark-mode.png`, `11-settings.png`.
