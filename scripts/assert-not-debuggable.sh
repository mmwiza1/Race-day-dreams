#!/bin/sh
# Refuse an APK that Android would treat as debuggable (audit D-3).
#
# Usage: assert-not-debuggable.sh APK            -> exit 0 only for a non-debuggable Race Day Dreams APK
#        assert-not-debuggable.sh --expect-debuggable APK
#                                                  -> exit 0 only for a debuggable one (the CI debug
#                                                     build used for instrumentation, as a self-check)
#
# A debuggable APK lets `adb shell run-as org.racedaydreams` or a JDWP debugger read the private
# SQLite database (GPS routes, heart rate, sleep, weight) on any phone with USB debugging enabled,
# so it must never be published. Re-signing does not change the manifest, so this checks the bytes
# that are actually published. Uses `aapt dump badging` from the pinned build-tools.
set -eu

expect=release
if [ "${1:-}" = --expect-debuggable ]; then
    expect=debug
    shift
fi
apk=${1:?usage: assert-not-debuggable.sh [--expect-debuggable] APK}
aapt=${AAPT:-$(ls -d "${ANDROID_HOME:?ANDROID_HOME is required}"/build-tools/*/ | sort -V | tail -n 1)aapt}

badging=$("$aapt" dump badging "$apk")
if ! printf '%s\n' "$badging" | grep -q "^package: name='org.racedaydreams' "; then
    echo "DEBUGGABLE_CHECK $apk is not a Race Day Dreams APK" >&2
    exit 1
fi
if printf '%s\n' "$badging" | grep -qx 'application-debuggable'; then
    debuggable=true
else
    debuggable=false
fi
if [ "$expect" = release ] && [ "$debuggable" = true ]; then
    echo "DEBUGGABLE_CHECK refusing $apk: android:debuggable is set; only the release build may be published" >&2
    exit 1
fi
if [ "$expect" = debug ] && [ "$debuggable" = false ]; then
    echo "DEBUGGABLE_CHECK $apk was expected to be the debuggable instrumentation build" >&2
    exit 1
fi
echo "DEBUGGABLE_CHECK $apk debuggable=$debuggable"
