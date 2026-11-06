#!/usr/bin/env python3
"""Smoke tests that run anywhere (no emulator): APK structure + source invariants."""
import pathlib, subprocess, sys, zipfile
root = pathlib.Path(__file__).resolve().parent.parent
apk = root / "dist/FreeInk.apk"
fails = []
def check(name, ok):
    print(("PASS " if ok else "FAIL ") + name)
    if not ok: fails.append(name)
check("APK exists", apk.exists())
if apk.exists():
    z = zipfile.ZipFile(apk); names = z.namelist()
    check("APK has classes.dex", "classes.dex" in names)
    check("APK has AndroidManifest.xml", "AndroidManifest.xml" in names)
    check("APK has launcher icon", any("ic_launcher" in n for n in names))
src = "".join(p.read_text() for p in (root/"app/src/main/java").rglob("*.java"))
for token in ["class InkView", "PdfRenderer", "pushUndo", "ACTION_OPEN_DOCUMENT", "PdfDocument"]:
    check(f"source contains {token}", token in src)
for banned in ["com.google.android.gms", "firebase", "ads", "INTERNET"]:
    check(f"no {banned}", banned not in src and banned not in (root/"app/src/main/AndroidManifest.xml").read_text())
sys.exit(1 if fails else 0)
