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

adb shell wm size 540x1200
adb shell wm density 210
# Test-only identities and bounded stacks; never dump private application logs.
adb logcat -v raw -s RddGate:I '*:S' &
rdd_test_log_pid=$!

# Fail fast on the real native interaction and large-text disclosure regressions.
# A passing guard never substitutes for the complete unfiltered suite below.
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.timeout_msec=600000 \
    -Pandroid.testInstrumentationRunnerArguments.listener=org.racedaydreams.coach.GateProgressListener \
    '-Pandroid.testInstrumentationRunnerArguments.class=org.racedaydreams.coach.CoachOnboardingTest#canContinueWithoutHealthAndSaveGoalThroughExistingForm,org.racedaydreams.ui.OnboardingPaletteTest,org.racedaydreams.coach.StrengthAnalyticsScreenTest'

./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.timeout_msec=600000 \
    -Pandroid.testInstrumentationRunnerArguments.listener=org.racedaydreams.coach.GateProgressListener

# Only the named instrumentation fixture reports bounded resource measurements.
adb logcat -d -s GymBackupFixture:I '*:S'
mkdir -p build/canonical-screenshots
adb pull /data/local/tmp/racedaydreams-screenshots/. build/canonical-screenshots/
python3 scripts/verify-dream-screenshots.py build/canonical-screenshots
