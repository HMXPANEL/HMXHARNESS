#!/usr/bin/env bash
# Cloud-only: one screen, one fresh emulator. No Gradle invocation here.
set -u
SCREEN="$1"; THEME="$2"; OUT="$3"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
timeout 60 adb wait-for-device || true
timeout 900 adb shell 'while [ -z "$(getprop sys.boot_completed)" ]; do sleep 2; done' || true
timeout 180 adb install -r app/build/outputs/apk/debug/app-debug.apk || true
timeout 30 adb shell cmd uimode night no || true
timeout 30 adb shell settings put global window_animation_scale 0 || true
timeout 30 adb shell settings put global transition_animation_scale 0 || true
timeout 30 adb shell settings put global animator_duration_scale 0 || true
timeout 30 adb shell appops set com.hmx.harness.debug MANAGE_EXTERNAL_STORAGE allow || true
timeout 30 adb shell pm grant com.hmx.harness.debug android.permission.POST_NOTIFICATIONS || true
case "$SCREEN" in
  drawer) ARGS="--es screen appnav --es drawer open" ;;
  code) ARGS="--es screen appnav --es route chat?kind=CODE" ;;
  *) ARGS="--es screen $SCREEN" ;;
esac
timeout 45 adb shell am start -W -n "com.hmx.harness.debug/com.hmx.harness.ui.showcase.GlassShowcaseActivity" $ARGS --es theme "$THEME" >/dev/null || true
sleep 13
# Dismiss a System UI ANR prompt if the software emulator raised one.
timeout 20 adb shell uiautomator dump /sdcard/hmx-ui.xml >/dev/null || true
if timeout 10 adb shell grep -q 'Wait' /sdcard/hmx-ui.xml; then timeout 10 adb shell input tap 330 1330 || true; sleep 2; fi
timeout 30 adb emu screenrecord screenshot "$OUT" || true
sleep 2
timeout 30 adb logcat -d -v threadtime -t 800 > "logs/capture-${THEME}-${SCREEN}.txt" || true
ls -lh "$OUT" || true
