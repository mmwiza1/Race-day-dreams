#!/bin/sh
# One shell owns the filtered diagnostic reader and its cleanup. The emulator action
# executes each YAML script line in a separate shell, so keep this sequence here.
set -eu

rdd_project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$rdd_project_dir"
rdd_test_log_pid=

rdd_cleanup() {
    rdd_gate_status=$?
    trap - 0 1 2 15
    if [ "$rdd_gate_status" -ne 0 ]; then
        # Public CI-device state only, so an overlay/keyguard failure is distinguishable
        # from app layout. Bound diagnostics and retain the original test exit status.
        timeout 10 adb shell dumpsys power | awk '/mWakefulness=|mInteractive=|Display Power/' || true
        timeout 10 adb shell dumpsys window | awk '/mCurrentFocus=|mFocusedApp=|mShowingLockscreen=/' || true
        # The lastanr dump has a Reason but no leading process identity on these APIs.
        # Use real AMS events instead: exact platform process identity and reason, capped
        # at five. Never print other application events or an unfiltered lastanr dump.
        timeout 10 adb logcat -b events -d -v brief -s am_anr '*:S' |
            awk '/\[[[:space:]]*[0-9]+,[[:space:]]*[0-9]+,[[:space:]]*com\.android\.(systemui|launcher3),/ { print }' | tail -n 5 || true
    fi
    if [ -n "$rdd_test_log_pid" ]; then
        kill "$rdd_test_log_pid" 2>/dev/null || true
        wait "$rdd_test_log_pid" 2>/dev/null || true
    fi
    exit "$rdd_gate_status"
}
trap rdd_cleanup 0
trap 'exit 129' 1
trap 'exit 130' 2
trap 'exit 143' 15

# sys.boot_completed precedes outstanding system broadcasts. Drain actual delivery
# before changing display configuration and between its independent size/density updates.
# This supported API28+ barrier is bounded; it is not an arbitrary settling sleep.
timeout 20 adb shell am wait-for-broadcast-idle
adb shell wm size 540x1200
timeout 20 adb shell am wait-for-broadcast-idle
adb shell wm density 210
timeout 20 adb shell am wait-for-broadcast-idle
# UI capture requires an awake, unlocked virtual device. This changes CI setup only;
# it is not evidence that locked-screen recording has been physically accepted.
adb shell svc power stayon true
adb shell settings put system screen_off_timeout 1800000
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard

rdd_wait_for_platform_focus() {
    # Activity launch/broadcast completion does not prove an actual input window exists.
    # Poll the real focused window within a fixed bound; do not accept mFocusedApp alone.
    timeout 20 sh -c '
        set -eu
        while :; do
            focus=$(timeout 5 adb shell dumpsys window | awk "/mCurrentFocus=/")
            case "$focus" in
                *"Application Not Responding:"*)
                    printf "%s\n" "PLATFORM_FOCUS_BLOCKED $focus"
                    exit 1
                    ;;
                *"$1/"*)
                    printf "%s\n" "PLATFORM_FOCUS_READY $1"
                    exit 0
                    ;;
            esac
            sleep 0.1
        done
    ' sh "$1"
}

# Warm a real platform window, then return to the default AOSP launcher. Existing ANR
# dialogs prevent these checks from passing; none is dismissed or force-stopped here.
timeout 20 adb shell am start -W -a android.settings.SETTINGS
rdd_wait_for_platform_focus com.android.settings
adb shell input keyevent KEYCODE_HOME
rdd_wait_for_platform_focus com.android.launcher3
# Test-only identities and bounded stacks; never dump private application logs.
adb logcat -v raw -s RddGate:I '*:S' &
rdd_test_log_pid=$!

# Fail fast on native interaction, disclosures and the actual multi-window capture lifecycle.
# A passing guard never substitutes for the complete unfiltered suite below.
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.timeout_msec=600000 \
    -Pandroid.testInstrumentationRunnerArguments.listener=org.racedaydreams.coach.GateProgressListener \
    '-Pandroid.testInstrumentationRunnerArguments.class=org.racedaydreams.coach.CoachOnboardingTest#canContinueWithoutHealthAndSaveGoalThroughExistingForm,org.racedaydreams.ui.OnboardingPaletteTest,org.racedaydreams.coach.StrengthAnalyticsScreenTest,org.racedaydreams.coach.CoachingScreenshotTest#capturesMaterialDeviceAndBrandColoursWithAccessibleLiveControls,org.racedaydreams.coach.CoachingScreenshotTest#capturesWideWindowNavigationAndBoundedContent,org.racedaydreams.coach.CoachingScreenshotTest#capturesPublicPermissionRationaleInLightDarkAndLargeText,org.racedaydreams.coach.ExerciseLibraryDesignTest,org.racedaydreams.coach.PlainEnglishUiTest,org.racedaydreams.coach.CoachingScreenshotTest#capturesPlainBackupExplanationInAllModes,org.racedaydreams.coach.CoachingScreenshotTest#capturesCompactLibraryFiltersAndRealDetailTabsInAllModes'

./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.timeout_msec=600000 \
    -Pandroid.testInstrumentationRunnerArguments.listener=org.racedaydreams.coach.GateProgressListener

# Only the named instrumentation fixture reports bounded resource measurements.
adb logcat -d -s GymBackupFixture:I '*:S'
mkdir -p build/canonical-screenshots
adb pull /data/local/tmp/racedaydreams-screenshots/. build/canonical-screenshots/
python3 scripts/verify-dream-screenshots.py build/canonical-screenshots
