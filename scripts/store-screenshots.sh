#!/usr/bin/env bash
# Captures store screenshots of the debug build on a running emulator, in light and dark mode.
# Used by .github/workflows/screenshots.yml; run locally with an emulator attached via adb.
set -euo pipefail

apk=${1:-app/build/outputs/apk/debug/app-debug.apk}
out=${2:-screenshots}
pkg=ir.mhdolatabadi.atid
mkdir -p "$out"

adb install -r "$apk"
# Grant up front so the permission dialog never covers a screenshot.
adb shell pm grant "$pkg" android.permission.POST_NOTIFICATIONS || true

# A clean status bar: fixed clock, full battery and signal, no notification icons.
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo clock -e hhmm 0930
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4
demo notifications -e visible false

# Taps the on-screen element whose text is exactly $1, found in the accessibility tree.
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null
  adb pull /sdcard/ui.xml "$RUNNER_TMP/ui.xml" >/dev/null
  read -r x y < <(python3 - "$RUNNER_TMP/ui.xml" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for node in ET.parse(sys.argv[1]).iter('node'):
    if node.get('text') == sys.argv[2]:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
else:
    sys.exit(f'"{sys.argv[2]}" is not on screen')
PY
)
  adb shell input tap "$x" "$y"
}

shot() {
  sleep 3
  adb exec-out screencap -p > "$out/$1.png"
  echo "captured $out/$1.png"
}

RUNNER_TMP=$(mktemp -d)
for mode in light dark; do
  if [ "$mode" = dark ]; then adb shell cmd uimode night yes; else adb shell cmd uimode night no; fi
  adb shell am force-stop "$pkg"
  adb shell am start -W -n "$pkg/.MainActivity" >/dev/null
  shot "1-calendar-$mode"
  adb shell input swipe 540 1900 540 700 600
  shot "3-calendar-scrolled-$mode"
  tap_text "اوقات شرعی"
  shot "2-prayer-times-$mode"
done
demo exit
