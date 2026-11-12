#!/usr/bin/env bash
set -euo pipefail
mkdir -p screenshots
collect() {
  adb exec-out run-as com.nthexperience.desktop cat files/vista-start-failure.png > screenshots/vista-start-failure.png 2>/dev/null || true
  for name in portrait landscape; do
    adb exec-out run-as com.nthexperience.desktop cat "files/vista-start-menu-$name.png" > "screenshots/vista-start-menu-$name.png" 2>/dev/null || true
  done
  adb logcat -d -t 700 > screenshots/start-logcat.txt || true
}
trap collect EXIT
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
timeout -k 3s 240s adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.StartMenuSmoke | tee start-menu-test.txt
grep -q 'PASS:' start-menu-test.txt
collect
adb shell am force-stop com.nthexperience.desktop
adb shell pm clear com.nthexperience.desktop
timeout -k 3s 300s adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.ExtendedTaskbarSmoke | tee extended-taskbar-test.txt
grep -q 'PASS:' extended-taskbar-test.txt
adb shell am force-stop com.nthexperience.desktop
adb shell pm clear com.nthexperience.desktop
adb shell appops set --uid com.nthexperience.desktop MANAGE_EXTERNAL_STORAGE allow
timeout -k 3s 240s adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.FidelitySmoke | tee fidelity-test.txt
grep -q 'PASS:' fidelity-test.txt
