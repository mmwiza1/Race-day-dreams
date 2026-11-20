#!/bin/sh
# Repeatable in-place upgrade verification on a connected emulator or device.
#
# Builds this source four times with THROWAWAY TEST KEYS generated here (never a release key):
#   old 9000 (key A) -> new 9001 (key A): Android must update in place and keep every row of data;
#   next 9002 (key A): the in-app verifier must accept it;
#   foreign 9003 (key B): Android must refuse it as an update, and the in-app verifier too.
# A lower versionCode must also be refused. The CI gate's verified APKs are restored afterwards
# and checked byte-for-byte, so publication can never pick up one of these test builds.
set -eu

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$root"
pkg=org.racedaydreams
runner=org.racedaydreams.test/androidx.test.runner.AndroidJUnitRunner
app_apk=app/build/outputs/apk/debug/app-debug.apk
test_apk=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
work=$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/rdd-upgrade.XXXXXX")

restore() {
    status=$?
    trap - 0 1 2 15
    adb uninstall "$pkg" >/dev/null 2>&1 || true
    adb uninstall "$pkg.test" >/dev/null 2>&1 || true
    adb shell rm -f /data/local/tmp/rdd-next.apk /data/local/tmp/rdd-foreign.apk >/dev/null 2>&1 || true
    if [ -f "$work/gate-app.apk" ]; then cp "$work/gate-app.apk" "$app_apk"; fi
    if [ -f "$work/gate-test.apk" ]; then cp "$work/gate-test.apk" "$test_apk"; fi
    if [ -f "$work/gate-app.apk" ] && ! cmp -s "$work/gate-app.apk" "$app_apk"; then
        echo "UPGRADE_CHECK the verified gate APK could not be restored" >&2
        status=1
    fi
    rm -rf "$work"
    exit "$status"
}
trap restore 0
trap 'exit 129' 1
trap 'exit 130' 2
trap 'exit 143' 15

if [ -f "$app_apk" ]; then cp "$app_apk" "$work/gate-app.apk"; fi
if [ -f "$test_apk" ]; then cp "$test_apk" "$work/gate-test.apk"; fi

password=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')
for key in a b; do
    keytool -genkeypair -keystore "$work/$key.p12" -storetype PKCS12 -alias upgrade-test \
        -keyalg RSA -keysize 2048 -validity 2 -dname "CN=Race Day Dreams upgrade test $key (throwaway)" \
        -storepass "$password" -keypass "$password" >/dev/null 2>&1
done
fingerprint_a=$(keytool -exportcert -keystore "$work/a.p12" -storepass "$password" -alias upgrade-test 2>/dev/null |
    sha256sum | cut -d ' ' -f 1)

build() { # versionCode key output-prefix
    RDD_UPGRADE_TEST_KEYSTORE="$work/$2.p12" RDD_UPGRADE_TEST_PASSWORD="$password" \
        ./gradlew -q :app:assembleDebug :app:assembleDebugAndroidTest \
        -PrddUpgradeTestVersionCode="$1" -PrddReleaseCertificate="$fingerprint_a"
    cp "$app_apk" "$work/$3.apk"
    cp "$test_apk" "$work/$3-test.apk"
}
build 9000 a old
build 9001 a new
build 9002 a next
build 9003 b foreign

installed_code() {
    adb shell dumpsys package "$pkg" | sed -n 's/.*versionCode=\([0-9]*\).*/\1/p' | head -n 1
}
instrument() { # phase, extra args...
    phase=$1
    shift
    adb shell am instrument -r -w -e upgradePhase "$phase" "$@" "$runner" > "$work/$phase.out"
    if ! grep -q 'INSTRUMENTATION_CODE: -1' "$work/$phase.out" || grep -q 'INSTRUMENTATION_STATUS_CODE: -2' "$work/$phase.out"; then
        cat "$work/$phase.out" >&2
        echo "UPGRADE_CHECK $phase phase failed" >&2
        exit 1
    fi
}
expect_refused() { # apk, reason
    if adb install -r "$1" > "$work/refused.out" 2>&1; then
        echo "UPGRADE_CHECK Android accepted $1 but should have refused it" >&2
        exit 1
    fi
    if ! grep -q "$2" "$work/refused.out"; then
        cat "$work/refused.out" >&2
        echo "UPGRADE_CHECK expected $2" >&2
        exit 1
    fi
}

adb uninstall "$pkg" >/dev/null 2>&1 || true
adb uninstall "$pkg.test" >/dev/null 2>&1 || true
adb install "$work/old.apk" >/dev/null
adb install "$work/old-test.apk" >/dev/null
instrument seed -e class org.racedaydreams.data.UpgradePathTest#seedRealUserDataForTheUpgrade
fingerprint=$(sed -n 's/^INSTRUMENTATION_STATUS: fingerprint=\([0-9a-f]\{64\}\).*/\1/p' "$work/seed.out" | tail -n 1)
test -n "$fingerprint"
echo "UPGRADE_CHECK seeded 9000 with data fingerprint $fingerprint"

adb install -r "$work/new.apk" | grep -q Success
test "$(installed_code)" = 9001
adb push "$work/next.apk" /data/local/tmp/rdd-next.apk >/dev/null
adb push "$work/foreign.apk" /data/local/tmp/rdd-foreign.apk >/dev/null
adb shell chmod 644 /data/local/tmp/rdd-next.apk /data/local/tmp/rdd-foreign.apk
instrument verify -e class org.racedaydreams.data.UpgradePathTest#userDataSurvivesTheUpdate \
    -e expectedFingerprint "$fingerprint" -e installedVersionCode 9001 \
    -e nextApk /data/local/tmp/rdd-next.apk -e foreignApk /data/local/tmp/rdd-foreign.apk
echo "UPGRADE_CHECK 9000 -> 9001 updated in place with identical data; in-app verifier accepted 9002 and rejected key B"

expect_refused "$work/foreign.apk" INSTALL_FAILED_UPDATE_INCOMPATIBLE
expect_refused "$work/old.apk" INSTALL_FAILED_VERSION_DOWNGRADE
test "$(installed_code)" = 9001
instrument verify -e class org.racedaydreams.data.UpgradePathTest#userDataSurvivesTheUpdate \
    -e expectedFingerprint "$fingerprint" -e installedVersionCode 9001
echo "UPGRADE_CHECK Android refused the differently signed and the older build; data unchanged"
