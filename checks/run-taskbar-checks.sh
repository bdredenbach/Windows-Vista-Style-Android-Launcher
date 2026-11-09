#!/usr/bin/env bash
set -euo pipefail
mkdir -p screenshots
collect() {
  for name in vista-taskbar-two-rows vista-taskbar-two-rows-landscape vista-taskbar-two-rows-start vista-taskbar-controls-bottom vista-taskbar-controls-bottom-landscape vista-taskbar-failure fidelity-failure; do
    capture=$(mktemp)
    timeout -k 2s 5s adb exec-out run-as com.nthexperience.desktop cat "files/$name.png" > "$capture" 2>/dev/null || true
    if python3 -c 'import pathlib,sys;sys.exit(pathlib.Path(sys.argv[1]).read_bytes()[:8] != bytes([137,80,78,71,13,10,26,10]))' "$capture"; then
      mv "$capture" "screenshots/$name.png"
    else
      rm -f "$capture"
    fi
  done
  adb exec-out run-as com.nthexperience.desktop cat files/vista-taskbar-tree.txt > screenshots/vista-taskbar-tree.txt 2>/dev/null || true
  adb logcat -d -t 500 > screenshots/logcat.txt || true
}
trap collect EXIT
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
timeout -k 3s 300s adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.ExtendedTaskbarSmoke | tee extended-taskbar-test.txt
grep -q 'PASS:' extended-taskbar-test.txt
collect
adb shell am force-stop com.nthexperience.desktop
adb shell pm clear com.nthexperience.desktop
# The existing fidelity suite reads isolated real storage fixtures, as in the full runner.
adb shell appops set --uid com.nthexperience.desktop MANAGE_EXTERNAL_STORAGE allow
timeout -k 3s 240s adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.FidelitySmoke | tee fidelity-test.txt
grep -q 'PASS:' fidelity-test.txt
