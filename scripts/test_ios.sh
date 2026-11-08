#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${DEVELOPER_DIR:-}" && -d /Applications/Xcode.app/Contents/Developer ]]; then
  export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
fi
if [[ "${MYCOACH_RUN_LOCAL_E2E:-0}" == 1 ]]; then
  python3 - <<'PY'
import json, urllib.request
with urllib.request.urlopen('http://localhost:8080/actuator/health', timeout=5) as response:
    assert json.load(response)['status'] == 'UP', 'Start Docker Compose first'
PY
fi
if [[ -z "${MYCOACH_TEST_DEVICE:-}" ]]; then
  MYCOACH_TEST_DEVICE=$(xcrun simctl list devices available -j | python3 -c 'import json,sys; d=json.load(sys.stdin)["devices"]; print(next(x["udid"] for r,a in d.items() if "iOS" in r for x in a if x["name"].startswith("iPhone") and x["isAvailable"]))')
fi
TEST_RUNNER_MYCOACH_E2E="${MYCOACH_RUN_LOCAL_E2E:-0}" xcodebuild test \
  -project MyCoach/MyCoach.xcodeproj -scheme MyCoach \
  -destination "platform=iOS Simulator,id=$MYCOACH_TEST_DEVICE" \
  -derivedDataPath "${MYCOACH_DERIVED_DATA:-/tmp/mycoach-ios-derived}" \
  -clonedSourcePackagesDirPath "${MYCOACH_SPM_PATH:-/tmp/mycoach-spm}" \
  -disableAutomaticPackageResolution -parallel-testing-enabled NO \
  -resultBundlePath "${1:-/tmp/mycoach-tests-$(date +%Y%m%d-%H%M%S).xcresult}" \
  CODE_SIGNING_ALLOWED=YES CODE_SIGN_IDENTITY=-
