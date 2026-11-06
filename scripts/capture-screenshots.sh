#!/usr/bin/env bash
# Captures genuine screenshots from a connected device/emulator into docs/screenshots/
set -euo pipefail; cd "$(dirname "$0")/.."; mkdir -p docs/screenshots
cap(){ adb exec-out screencap -p > "docs/screenshots/$1.png"; echo "captured $1.png — $2"; sleep 1; }
adb shell am start -n app.freeink/.ui.MainActivity >/dev/null; sleep 2
cap 01-home "Home / library. TODO: also capture with documents present."
echo "Open a notebook, draw, open the whiteboard, a PDF, the text editor, dark mode and Settings,"
echo "running this script's cap() lines (or adb exec-out screencap) at each screen, numbered 02..11."
ls docs/screenshots/
