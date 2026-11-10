#!/usr/bin/env bash
set -euo pipefail
launcher=com.nthexperience.desktop
collect() {
  mkdir -p screenshots
  timeout -k 2s 15s adb logcat -d > screenshots/vista-initialization-log.txt || true
  for name in $(timeout -k 2s 10s adb shell run-as "$launcher" ls files 2>/dev/null | tr -d '\r'); do
    case "$name" in vista-usage-*.png|vista-usage-tree.txt)
      timeout -k 2s 10s adb exec-out run-as "$launcher" cat "files/$name" > "screenshots/$name" 2>/dev/null || break;;
    esac
  done
}
trap collect EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear "$launcher"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell appops set --uid "$launcher" GET_USAGE_STATS deny
timeout -k 3s 300s adb shell am instrument -w "$launcher.test/com.nthexperience.desktop.VistaInitializationSmoke" | tee vista-initialization-test.txt
grep -q 'PASS:' vista-initialization-test.txt
! grep -Eq 'FAIL:|shortMsg=|Process crashed|INSTRUMENTATION_FAILED' vista-initialization-test.txt
