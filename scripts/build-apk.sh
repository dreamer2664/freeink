#!/usr/bin/env bash
# Builds FreeInk-debug.apk using only the Android SDK build-tools (no Gradle download needed).
# Requirements: JDK 17, Android SDK platform 34 + build-tools 34.0.0.
# Set ANDROID_SDK_ROOT (or ANDROID_HOME) if the SDK is not in a default location.
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
[ -d "$SDK" ] || SDK=/home/user/tools/sdk
BT="$SDK/build-tools/34.0.0"; AJAR="$SDK/platforms/android-34/android.jar"
export JAVA_HOME="${JAVA_HOME:-/home/user/tools/jdk17}"; export PATH="$JAVA_HOME/bin:$PATH"
OUT=app/build/manual; rm -rf "$OUT"; mkdir -p "$OUT/classes" dist
echo "== aapt2 compile + link =="
"$BT/aapt2" compile --dir app/src/main/res -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/unsigned.apk" -I "$AJAR" --manifest app/src/main/AndroidManifest.xml "$OUT/res.zip" --java "$OUT/gen" --auto-add-overlay
echo "== javac =="
find app/src/main/java "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -source 17 -target 17 -classpath "$AJAR" -d "$OUT/classes" @"$OUT/sources.txt"
echo "== d8 =="
"$BT/d8" --min-api 26 --output "$OUT" $(find "$OUT/classes" -name '*.class')
echo "== package + align + sign =="
python3 - "$OUT/unsigned.apk" "$OUT/classes.dex" <<'PY'
import zipfile,sys
with zipfile.ZipFile(sys.argv[1],'a') as z: z.write(sys.argv[2],'classes.dex')
PY
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
[ -f "$OUT/debug.keystore" ] || keytool -genkeypair -keystore "$OUT/debug.keystore" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=FreeInk Debug,O=FreeInk,C=US" >/dev/null 2>&1
cp "$OUT/debug.keystore" dist/debug.keystore 2>/dev/null || true
"$BT/apksigner" sign --ks dist/debug.keystore --ks-pass pass:android --key-pass pass:android --out dist/FreeInk.apk "$OUT/aligned.apk"
"$BT/apksigner" verify --verbose dist/FreeInk.apk | head -5
ls -lh dist/FreeInk.apk
echo "OK -> dist/FreeInk.apk (install: adb install -r dist/FreeInk.apk)"
