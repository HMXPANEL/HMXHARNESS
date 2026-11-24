#!/usr/bin/env bash
# Cloud-only performance sampling on a real emulator profile.
set -u
PROFILE="$1"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
timeout 60 adb wait-for-device || true
timeout 900 adb shell 'while [ -z "$(getprop sys.boot_completed)" ]; do sleep 2; done' || true
timeout 180 adb install -r app/build/outputs/apk/debug/app-debug.apk || true
timeout 30 adb shell cmd uimode night no || true
mkdir -p perf logs
timeout 45 adb shell am start -W -n "com.hmx.harness.debug/com.hmx.harness.ui.showcase.GlassPerfActivity" --es profile "$PROFILE" --es tier "$PROFILE" >/dev/null || true
sleep 16
timeout 60 adb pull "/sdcard/Android/data/com.hmx.harness.debug/files/liquidglass-perf/$PROFILE.txt" "perf/$PROFILE.txt" || true
timeout 30 adb logcat -d -v threadtime -t 800 > "logs/perf-$PROFILE.txt" || true
cat "perf/$PROFILE.txt" 2>/dev/null || true
