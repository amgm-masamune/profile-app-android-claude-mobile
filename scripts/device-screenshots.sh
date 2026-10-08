#!/usr/bin/env bash
# エミュレータ(または接続した実機)で GlassScreenshotTest を動かし、撮った画像を shots/ に取り出す。
# 先に ./gradlew assembleDebug assembleDebugAndroidTest を済ませておくこと。
set -euo pipefail

PKG=com.example.businesscard
RUNNER="$PKG.test/androidx.test.runner.AndroidJUnitRunner"
CLASS="$PKG.ui.GlassScreenshotTest"
LOG=instrument.log
: > "$LOG"

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

run_tests() {
  # am instrument はテストが失敗しても終了コード0なので、出力に「OK (n tests)」があるかで判定する
  local out
  out=$(adb shell am instrument -w -e class "$1" "$RUNNER" | tee -a "$LOG")
  if ! echo "$out" | grep -q "^OK ("; then
    echo "::group::logcat"; adb logcat -d -t 400 '*:E' | tee -a "$LOG"; echo "::endgroup::"
    return 1
  fi
}

# 縦画面
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
run_tests "$CLASS#catalog,$CLASS#list,$CLASS#listEmpty,$CLASS#edit,$CLASS#editNewWithError"

# 横画面(表示画面は横画面固定)
adb shell settings put system user_rotation 1
sleep 3
run_tests "$CLASS#detail"

mkdir -p shots
adb exec-out run-as "$PKG" tar -cf - -C files screenshots | tar -xf - -C shots
ls -la shots/screenshots
