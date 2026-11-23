#!/usr/bin/env python3
"""Exercise the CI shell's real process lifecycle with bounded local command stubs."""
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parent.parent

STUB = r'''
import json
import os
from pathlib import Path
import signal
import sys
import time

state = Path(os.environ['RDD_GATE_TEST_STATE'])
args = sys.argv[1:]

def event(name):
    with (state / 'events').open('a') as output:
        output.write(name + '\n')

if Path(sys.argv[0]).name == 'adb':
    if args[:3] == ['logcat', '-v', 'raw']:
        def stop(signum, frame):
            event('reader-stop')
            sys.exit(0)
        signal.signal(signal.SIGTERM, stop)
        (state / 'reader.pid').write_text(str(os.getpid()))
        event('reader-start')
        (state / 'reader-ready').touch()
        while True:
            signal.pause()
    elif args[:3] == ['shell', 'wm', 'size']:
        event('size:' + args[3])
        sys.exit(int(os.environ.get('RDD_GATE_TEST_SETUP_EXIT', '0')))
    elif args[:3] == ['shell', 'wm', 'density']:
        event('density:' + args[3])
    elif args == ['shell', 'am', 'wait-for-broadcast-idle']:
        event('broadcast-idle')
        sys.exit(int(os.environ.get('RDD_GATE_TEST_BROADCAST_EXIT', '0')))
    elif args == ['shell', 'svc', 'power', 'stayon', 'true']:
        event('keep-awake')
        sys.exit(int(os.environ.get('RDD_GATE_TEST_AWAKE_EXIT', '0')))
    elif args == ['shell', 'input', 'keyevent', 'KEYCODE_WAKEUP']:
        event('wake')
    elif args == ['shell', 'settings', 'put', 'system', 'screen_off_timeout', '1800000']:
        event('display-timeout')
    elif args == ['shell', 'wm', 'dismiss-keyguard']:
        event('unlock')
    elif args == ['shell', 'am', 'start', '-W', '-a', 'android.settings.SETTINGS']:
        event('platform-settings')
        (state / 'foreground').write_text('com.android.settings')
        sys.exit(int(os.environ.get('RDD_GATE_TEST_PLATFORM_START_EXIT', '0')))
    elif args == ['shell', 'am', 'force-stop', 'com.android.launcher3']:
        event('launcher-stop')
        (state / 'launcher-stopped').touch()
    elif args == ['shell', 'input', 'keyevent', 'KEYCODE_HOME']:
        event('platform-home')
        (state / 'foreground').write_text('com.android.launcher3')
    elif args == ['shell', 'dumpsys', 'power']:
        event('power-state')
        print('mWakefulness=Awake')
    elif args == ['shell', 'dumpsys', 'window']:
        event('window-focus')
        foreground = state / 'foreground'
        launcher_anr = os.environ.get('RDD_GATE_TEST_LAUNCHER_ANR')
        if os.environ.get('RDD_GATE_TEST_PLATFORM_ANR') == '1':
            print('mCurrentFocus=Window{1 u0 Application Not Responding: com.android.systemui}')
        elif launcher_anr == 'persist' or (launcher_anr == '1' and not (state / 'launcher-stopped').exists()):
            print('mCurrentFocus=Window{1 u0 Application Not Responding: com.android.launcher3}')
        elif foreground.exists():
            package = foreground.read_text()
            if os.environ.get('RDD_GATE_TEST_FOCUS_DELAY') == '1' and not (state / 'focus-polled').exists():
                (state / 'focus-polled').touch()
                print('mCurrentFocus=null')
                print('mFocusedApp=ActivityRecord{' + package + '/.SyntheticPlatformWindow}')
            else:
                print('mCurrentFocus=Window{1 u0 ' + package + '/.SyntheticPlatformWindow}')
        else:
            print('mCurrentFocus=synthetic-test-window')
    elif args[:2] == ['logcat', '-d']:
        event('fixture-log')
    elif args[:4] == ['logcat', '-b', 'events', '-d']:
        event('platform-anr-events')
        process = os.environ.get('RDD_GATE_TEST_ANR_PROCESS', 'com.android.systemui')
        print('I/am_anr: [0,123,' + process + ',1,synthetic platform dispatch timeout]')
        print('I/am_anr: [0,456,org.racedaydreams,1,unrelated-synthetic-private-reason]')
        print('I/am_anr: [0,456,org.racedaydreams,1,mentions,com.android.systemui,unrelated-synthetic-private-reason]')
    elif args[:1] == ['pull']:
        event('pull')
    else:
        raise AssertionError('Unexpected adb arguments: ' + repr(args))
elif Path(sys.argv[0]).name == 'gradlew':
    deadline = time.monotonic() + 3
    while not (state / 'reader-ready').exists():
        if time.monotonic() >= deadline:
            raise AssertionError('Diagnostic reader did not start')
        time.sleep(0.01)
    guard = any(arg.startswith('-Pandroid.testInstrumentationRunnerArguments.class=') for arg in args)
    (state / ('guard-args.json' if guard else 'gradle-args.json')).write_text(json.dumps(args))
    event('gradle-guard' if guard else 'gradle-full')
    sys.exit(int(os.environ.get('RDD_GATE_TEST_GUARD_EXIT' if guard else 'RDD_GATE_TEST_GRADLE_EXIT', '0')))
else:
    assert args == ['build/canonical-screenshots'], args
    event('verify')
    sys.exit(int(os.environ.get('RDD_GATE_TEST_VERIFY_EXIT', '0')))
'''


def instrumentation_test_classes(root):
    """Every androidTest class that JUnit runs: a file's own top-level class with a JUnit 4
    @Test method, or a JUnit 3 (Android)TestCase subclass. Helper classes are not runnable."""
    found = {}
    for source in sorted((root / 'app/src/androidTest').rglob('*')):
        if source.suffix not in ('.kt', '.java'):
            continue
        text = source.read_text(encoding='utf-8')
        package = re.search(r'^package\s+([\w.]+)', text, re.M)
        declared = re.search(
            r'^(?:(?:public|internal|open|abstract|final)\s+)*class\s+' + re.escape(source.stem) + r'\b(.*)$',
            text, re.M,
        )
        if not package or not declared:
            continue
        junit4 = '@Test' in text
        junit3 = re.search(r'(?::|extends)\s*(?:android\.test\.)?(?:Android)?TestCase\b', declared.group(1))
        if junit4 or junit3:
            found[package.group(1) + '.' + source.stem] = source
    return found


def draft_gate_contract(workflow_text):
    listed = re.search(r'RDD_FOCUSED_CLASSES:\s*(\S+)', workflow_text)
    classes = listed.group(1).split(',') if listed else []
    excluded = {}
    for line in workflow_text.splitlines():
        entry = re.match(r'\s*#\s*RDD_FOCUSED_EXCLUDED:\s*([\w.]+)\s*\|\s*(.*)$', line)
        if entry:
            excluded[entry.group(1)] = entry.group(2).strip()
    return classes, excluded


class AndroidGateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'scripts').mkdir()
        (self.root / 'bin').mkdir()
        self.state = self.root / 'state'
        self.state.mkdir()
        shutil.copyfile(ROOT / 'scripts/run-android-gate.sh', self.root / 'scripts/gate.sh')
        for relative in ('bin/adb', 'gradlew', 'scripts/verify-dream-screenshots.py'):
            path = self.root / relative
            path.write_text(f'#!{sys.executable}\n' + STUB)
            path.chmod(0o755)
        self.env = dict(os.environ, RDD_GATE_TEST_STATE=str(self.state))
        self.env['PATH'] = str(self.root / 'bin') + os.pathsep + self.env['PATH']

    def run_gate(self, **changes):
        process = subprocess.Popen(
            ['sh', 'scripts/gate.sh'], cwd=self.root, env=dict(self.env, **changes),
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, start_new_session=True,
        )
        try:
            stdout, stderr = process.communicate(timeout=8)
        except subprocess.TimeoutExpired:
            # A broken cleanup must fail the test without leaving a reader or held pipe.
            os.killpg(process.pid, signal.SIGKILL)
            process.communicate()
            self.fail('Gate or its diagnostic child did not exit within the test bound')
        return process.returncode, stdout, stderr

    def events(self):
        return (self.state / 'events').read_text().splitlines()

    def assert_reader_stopped(self):
        self.assertIn('reader-stop', self.events())
        pid = int((self.state / 'reader.pid').read_text())
        with self.assertRaises(ProcessLookupError):
            os.kill(pid, 0)

    def test_explicit_development_filter_keeps_focus_guards_and_reaps_reader(self):
        classes = 'org.racedaydreams.coach.MaterialThemeTest,org.racedaydreams.coach.PlainEnglishUiTest'
        code, _, stderr = self.run_gate(RDD_FOCUSED_CLASSES=classes)
        self.assertEqual(0, code, stderr)
        self.assertIn('platform-settings', self.events())
        self.assertIn('platform-home', self.events())
        self.assertIn('gradle-guard', self.events())
        self.assertNotIn('gradle-full', self.events())
        self.assertNotIn('pull', self.events())
        args = json.loads((self.state / 'guard-args.json').read_text())
        self.assertIn('-Pandroid.testInstrumentationRunnerArguments.class=' + classes, args)
        self.assert_reader_stopped()

    def test_development_filter_failure_preserves_exit_and_cleans_reader(self):
        code, _, _ = self.run_gate(RDD_FOCUSED_CLASSES='org.racedaydreams.coach.MaterialThemeTest', RDD_GATE_TEST_GUARD_EXIT='7')
        self.assertEqual(7, code)
        self.assertNotIn('gradle-full', self.events())
        self.assert_reader_stopped()

    def test_success_runs_full_gate_then_reaps_the_actual_reader(self):
        code, _, stderr = self.run_gate()
        self.assertEqual(0, code, stderr)
        self.assertEqual(
            ['broadcast-idle', 'size:540x1200', 'broadcast-idle', 'density:210', 'broadcast-idle', 'keep-awake', 'display-timeout', 'wake', 'unlock', 'platform-settings', 'window-focus', 'platform-home', 'window-focus', 'reader-start', 'gradle-guard', 'gradle-full', 'fixture-log', 'pull', 'verify', 'reader-stop'],
            self.events(),
        )
        self.assertEqual([
            ':app:connectedDebugAndroidTest',
            '-Pandroid.testInstrumentationRunnerArguments.timeout_msec=600000',
            '-Pandroid.testInstrumentationRunnerArguments.listener=org.racedaydreams.coach.GateProgressListener',
        ], json.loads((self.state / 'gradle-args.json').read_text()))
        guards = json.loads((self.state / 'guard-args.json').read_text())
        selected = next(arg for arg in guards if arg.startswith('-Pandroid.testInstrumentationRunnerArguments.class='))
        self.assertIn('org.racedaydreams.coach.ExerciseLibraryDesignTest', selected)
        self.assertIn('CoachingScreenshotTest#capturesCompactLibraryFiltersAndRealDetailTabsInAllModes', selected)
        self.assertIn('org.racedaydreams.coach.PlainEnglishUiTest', selected)
        self.assertIn('CoachingScreenshotTest#capturesPlainBackupExplanationInAllModes', selected)
        self.assertIn('AuthoredGymNowScreenTest#authoredTodayAndDetailsExposeExactPreservedTargets', selected)
        self.assertIn('AuthoredGymNowScreenTest#authoredTodayAndDetailsAreUsableInDarkModeAtTwoHundredPercent', selected)
        self.assertIn('org.racedaydreams.coach.StorageManagementScreenTest', selected)
        self.assertIn('CoachingScreenshotTest#capturesOptionalProgressionInLightDarkAndLargeText', selected)
        self.assertIn('CoachingScreenshotTest#capturesPlainHealthAndRunningInstructionsInAllModes', selected)
        self.assertIn('CoachOnboardingTest#canContinueWithoutHealthAndSaveGoalThroughExistingForm', selected)
        self.assertIn('CoachingScreenshotTest#capturesPublicPermissionRationaleInLightDarkAndLargeText', selected)
        self.assert_reader_stopped()

    def test_native_guard_failure_blocks_full_suite_and_exports(self):
        code, stdout, stderr = self.run_gate(RDD_GATE_TEST_GUARD_EXIT='17')
        self.assertEqual(17, code, stderr)
        self.assertIn('gradle-guard', self.events())
        self.assertNotIn('gradle-full', self.events())
        self.assertNotIn('pull', self.events())
        self.assertIn('power-state', self.events())
        self.assertIn('window-focus', self.events())
        self.assertIn('platform-anr-events', self.events())
        self.assertIn('[0,123,com.android.systemui,1,', stdout)
        self.assertIn('synthetic platform dispatch timeout', stdout)
        self.assertNotIn('unrelated-synthetic-private-reason', stdout)
        self.assert_reader_stopped()

    def test_gradle_failure_keeps_its_exit_and_skips_exports_but_reaps_reader(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_GRADLE_EXIT='42')
        self.assertEqual(42, code, stderr)
        self.assertNotIn('fixture-log', self.events())
        self.assertNotIn('pull', self.events())
        self.assertNotIn('verify', self.events())
        self.assert_reader_stopped()

    def test_launcher_reason_is_bounded_without_hiding_the_test_failure(self):
        code, stdout, stderr = self.run_gate(
            RDD_GATE_TEST_GUARD_EXIT='17', RDD_GATE_TEST_ANR_PROCESS='com.android.launcher3',
        )
        self.assertEqual(17, code, stderr)
        self.assertIn('[0,123,com.android.launcher3,1,', stdout)
        self.assertIn('synthetic platform dispatch timeout', stdout)
        self.assertNotIn('unrelated-synthetic-private-reason', stdout)
        self.assertNotIn('gradle-full', self.events())
        self.assert_reader_stopped()

    def test_non_platform_anr_content_is_not_printed(self):
        code, stdout, stderr = self.run_gate(
            RDD_GATE_TEST_GUARD_EXIT='17', RDD_GATE_TEST_ANR_PROCESS='org.racedaydreams',
        )
        self.assertEqual(17, code, stderr)
        self.assertNotIn('[0,123,org.racedaydreams,1,', stdout)
        self.assertNotIn('synthetic platform dispatch timeout', stdout)
        self.assertNotIn('unrelated-synthetic-private-reason', stdout)
        self.assert_reader_stopped()

    def test_verification_failure_keeps_its_exit_and_reaps_reader(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_VERIFY_EXIT='23')
        self.assertEqual(23, code, stderr)
        self.assertIn('verify', self.events())
        self.assert_reader_stopped()

    def test_setup_failure_never_starts_reader_or_training_tests(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_SETUP_EXIT='31')
        self.assertEqual(31, code, stderr)
        self.assertEqual(['broadcast-idle', 'size:540x1200', 'power-state', 'window-focus', 'platform-anr-events'], self.events())
        self.assertFalse((self.state / 'reader.pid').exists())

    def test_awake_setup_failure_never_starts_reader_or_tests(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_AWAKE_EXIT='29')
        self.assertEqual(29, code, stderr)
        self.assertEqual(['broadcast-idle', 'size:540x1200', 'broadcast-idle', 'density:210', 'broadcast-idle', 'keep-awake', 'power-state', 'window-focus', 'platform-anr-events'], self.events())
        self.assertFalse((self.state / 'reader.pid').exists())

    def test_broadcast_barrier_failure_preserves_exit_and_never_starts_tests(self):
        code, stdout, stderr = self.run_gate(RDD_GATE_TEST_BROADCAST_EXIT='27')
        self.assertEqual(27, code, stderr)
        self.assertEqual(['broadcast-idle', 'power-state', 'window-focus', 'platform-anr-events'], self.events())
        self.assertFalse((self.state / 'reader.pid').exists())
        self.assertNotIn('unrelated-synthetic-private-reason', stdout)

    def test_existing_platform_anr_blocks_preparation_without_dismissing_it(self):
        code, stdout, stderr = self.run_gate(RDD_GATE_TEST_PLATFORM_ANR='1')
        self.assertEqual(1, code, stderr)
        self.assertIn('PLATFORM_FOCUS_BLOCKED', stdout)
        self.assertNotIn('PLATFORM_FOCUS_READY', stdout)
        self.assertNotIn('platform-home', self.events())
        self.assertNotIn('gradle-guard', self.events())
        self.assertFalse((self.state / 'reader.pid').exists())

    def test_boot_launcher_anr_is_recovered_once_and_focus_is_proven_again(self):
        code, stdout, stderr = self.run_gate(RDD_GATE_TEST_LAUNCHER_ANR='1')
        self.assertEqual(0, code, stderr)
        self.assertIn('PLATFORM_LAUNCHER_ANR_RECOVERY', stdout)
        events = self.events()
        self.assertEqual(1, events.count('launcher-stop'))
        stop = events.index('launcher-stop')
        # Settings and launcher focus are both proven again after the recovery, before tests.
        self.assertEqual(2, events.count('platform-settings'))
        self.assertIn('platform-home', events[stop:])
        self.assertLess(stop, events.index('gradle-guard'))
        self.assertIn('PLATFORM_FOCUS_READY com.android.settings', stdout)
        self.assertIn('PLATFORM_FOCUS_READY com.android.launcher3', stdout)
        self.assert_reader_stopped()

    def test_persistent_launcher_anr_still_fails_before_tests(self):
        code, stdout, stderr = self.run_gate(RDD_GATE_TEST_LAUNCHER_ANR='persist')
        self.assertEqual(3, code, stderr)
        self.assertEqual(1, self.events().count('launcher-stop'))
        self.assertNotIn('PLATFORM_FOCUS_READY', stdout)
        self.assertNotIn('gradle-guard', self.events())
        self.assertFalse((self.state / 'reader.pid').exists())

    def test_systemui_anr_is_never_recovered(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_PLATFORM_ANR='1')
        self.assertEqual(1, code, stderr)
        self.assertNotIn('launcher-stop', self.events())

    def test_focused_app_is_not_accepted_until_its_actual_window_has_focus(self):
        code, stdout, stderr = self.run_gate(RDD_GATE_TEST_FOCUS_DELAY='1')
        self.assertEqual(0, code, stderr)
        self.assertEqual(3, self.events().count('window-focus'))
        self.assertIn('PLATFORM_FOCUS_READY com.android.settings', stdout)
        self.assertIn('PLATFORM_FOCUS_READY com.android.launcher3', stdout)
        self.assert_reader_stopped()

    def test_platform_launch_failure_preserves_exit_and_never_starts_tests(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_PLATFORM_START_EXIT='25')
        self.assertEqual(25, code, stderr)
        self.assertIn('platform-settings', self.events())
        self.assertNotIn('platform-home', self.events())
        self.assertNotIn('gradle-guard', self.events())
        self.assertFalse((self.state / 'reader.pid').exists())

    def test_action_calls_one_shell_without_changing_the_timeout(self):
        workflow = (ROOT / '.github/workflows/android.yml').read_text()
        self.assertIn('script: sh scripts/run-android-gate.sh', workflow)
        # A bound still exists for stalled emulators; 45 minutes was owner-approved when the full
        # capture matrix grew to about 30 minutes.
        self.assertIn('timeout-minutes: 45', workflow)
        self.assertIn('profile: pixel_2', workflow)
        self.assertIn('emulator-build: 16740729', workflow)
        self.assertIn('emulator-options: -no-window -gpu software -no-snapshot -noaudio -no-boot-anim', workflow)
        self.assertNotIn('rdd_test_log_pid', workflow)


class DraftGateCoverageTest(unittest.TestCase):
    """D-5: the draft gate's focused list cannot silently drift from the instrumentation suite."""

    def setUp(self):
        self.classes = instrumentation_test_classes(ROOT)
        self.listed, self.excluded = draft_gate_contract(
            (ROOT / '.github/workflows/multiplatform.yml').read_text(encoding='utf-8'))

    def test_discovery_finds_both_junit_styles_and_skips_helpers(self):
        self.assertIn('org.racedaydreams.AppPrivacyTest', self.classes)
        self.assertIn('org.racedaydreams.data.Release081ReliabilityTest', self.classes)
        self.assertIn('org.racedaydreams.coach.CoachOnboardingTest', self.classes)
        self.assertNotIn('org.racedaydreams.coach.GateProgressListener', self.classes)
        self.assertGreater(len(self.classes), 150)

    def test_every_instrumentation_class_is_listed_or_excluded_with_a_reason(self):
        missing = sorted(set(self.classes) - set(self.listed) - set(self.excluded))
        self.assertEqual([], missing, 'Add these to RDD_FOCUSED_CLASSES or to an RDD_FOCUSED_EXCLUDED line with a reason')
        for name, reason in self.excluded.items():
            self.assertGreaterEqual(len(reason), 20, name + ' needs a stated exclusion reason')

    def test_listed_and_excluded_entries_are_real_unique_and_disjoint(self):
        self.assertEqual(len(self.listed), len(set(self.listed)), 'Duplicate RDD_FOCUSED_CLASSES entry')
        self.assertEqual([], sorted(set(self.listed) - set(self.classes)), 'Stale RDD_FOCUSED_CLASSES entry')
        self.assertEqual([], sorted(set(self.excluded) - set(self.classes)), 'Stale RDD_FOCUSED_EXCLUDED entry')
        self.assertEqual([], sorted(set(self.listed) & set(self.excluded)))

    def test_d5_coverage_classes_stay_in_the_draft_gate(self):
        for name in (
            'org.racedaydreams.AppPrivacyTest',
            'org.racedaydreams.coach.PrimaryFocusOnboardingTest',
            'org.racedaydreams.data.PrimaryFocusRepositoryTest',
            'org.racedaydreams.ui.OnboardingDraftTest',
            'org.racedaydreams.ui.OnboardingPaletteTest',
            'org.racedaydreams.ui.OnboardingReviewTest',
            'org.racedaydreams.coach.StrengthTodayScreenTest',
            'org.racedaydreams.coach.TrainingTrendsScreenTest',
            'org.racedaydreams.data.DevelopmentStoreTest',
            'org.racedaydreams.data.StrengthReleaseStoreTest',
        ):
            self.assertIn(name, self.listed)
        self.assertIn('org.racedaydreams.coach.CoachingScreenshotTest', self.excluded)

    def test_draft_gate_timeout_covers_the_added_classes(self):
        workflow = (ROOT / '.github/workflows/multiplatform.yml').read_text(encoding='utf-8')
        step = workflow.split('- name: Exercise migrated UI, health and durable workout flows', 1)[1]
        self.assertIn('timeout-minutes: 20', step.split('uses: reactivecircus/android-emulator-runner@v2', 1)[0])

    def test_contract_parser_reports_an_unlisted_class(self):
        listed, excluded = draft_gate_contract(
            '  # RDD_FOCUSED_EXCLUDED: a.B | a sufficiently long stated reason\n'
            '  RDD_FOCUSED_CLASSES: a.C,a.D\n')
        self.assertEqual(['a.C', 'a.D'], listed)
        self.assertEqual({'a.B': 'a sufficiently long stated reason'}, excluded)
        self.assertEqual(['a.E'], sorted({'a.B', 'a.C', 'a.D', 'a.E'} - set(listed) - set(excluded)))


if __name__ == '__main__':
    unittest.main(verbosity=2)
