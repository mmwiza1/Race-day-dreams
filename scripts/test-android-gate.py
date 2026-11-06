#!/usr/bin/env python3
"""Exercise the CI shell's real process lifecycle with bounded local command stubs."""
import json
import os
from pathlib import Path
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
    elif args == ['shell', 'svc', 'power', 'stayon', 'true']:
        event('keep-awake')
        sys.exit(int(os.environ.get('RDD_GATE_TEST_AWAKE_EXIT', '0')))
    elif args == ['shell', 'input', 'keyevent', 'KEYCODE_WAKEUP']:
        event('wake')
    elif args == ['shell', 'settings', 'put', 'system', 'screen_off_timeout', '1800000']:
        event('display-timeout')
    elif args == ['shell', 'wm', 'dismiss-keyguard']:
        event('unlock')
    elif args == ['shell', 'dumpsys', 'power']:
        event('power-state')
        print('mWakefulness=Awake')
    elif args == ['shell', 'dumpsys', 'window']:
        event('window-focus')
        print('mCurrentFocus=synthetic-test-window')
    elif args == ['shell', 'dumpsys', 'activity', 'lastanr']:
        event('systemui-anr')
        print('ANR in com.android.systemui')
        print('Reason: synthetic input dispatch timeout')
        print('unrelated-synthetic-details-must-not-be-printed')
    elif args[:2] == ['logcat', '-d']:
        event('fixture-log')
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

    def test_success_runs_full_gate_then_reaps_the_actual_reader(self):
        code, _, stderr = self.run_gate()
        self.assertEqual(0, code, stderr)
        self.assertEqual(
            ['size:540x1200', 'density:210', 'keep-awake', 'display-timeout', 'wake', 'unlock', 'reader-start', 'gradle-guard', 'gradle-full', 'fixture-log', 'pull', 'verify', 'reader-stop'],
            self.events(),
        )
        self.assertEqual([
            ':app:connectedDebugAndroidTest',
            '-Pandroid.testInstrumentationRunnerArguments.timeout_msec=600000',
            '-Pandroid.testInstrumentationRunnerArguments.listener=org.racedaydreams.coach.GateProgressListener',
        ], json.loads((self.state / 'gradle-args.json').read_text()))
        self.assert_reader_stopped()

    def test_native_guard_failure_blocks_full_suite_and_exports(self):
        code, stdout, stderr = self.run_gate(RDD_GATE_TEST_GUARD_EXIT='17')
        self.assertEqual(17, code, stderr)
        self.assertIn('gradle-guard', self.events())
        self.assertNotIn('gradle-full', self.events())
        self.assertNotIn('pull', self.events())
        self.assertIn('power-state', self.events())
        self.assertIn('window-focus', self.events())
        self.assertIn('systemui-anr', self.events())
        self.assertIn('ANR in com.android.systemui', stdout)
        self.assertIn('Reason: synthetic input dispatch timeout', stdout)
        self.assertNotIn('unrelated-synthetic-details', stdout)
        self.assert_reader_stopped()

    def test_gradle_failure_keeps_its_exit_and_skips_exports_but_reaps_reader(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_GRADLE_EXIT='42')
        self.assertEqual(42, code, stderr)
        self.assertNotIn('fixture-log', self.events())
        self.assertNotIn('pull', self.events())
        self.assertNotIn('verify', self.events())
        self.assert_reader_stopped()

    def test_verification_failure_keeps_its_exit_and_reaps_reader(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_VERIFY_EXIT='23')
        self.assertEqual(23, code, stderr)
        self.assertIn('verify', self.events())
        self.assert_reader_stopped()

    def test_setup_failure_never_starts_reader_or_training_tests(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_SETUP_EXIT='31')
        self.assertEqual(31, code, stderr)
        self.assertEqual(['size:540x1200'], self.events())
        self.assertFalse((self.state / 'reader.pid').exists())

    def test_awake_setup_failure_never_starts_reader_or_tests(self):
        code, _, stderr = self.run_gate(RDD_GATE_TEST_AWAKE_EXIT='29')
        self.assertEqual(29, code, stderr)
        self.assertEqual(['size:540x1200', 'density:210', 'keep-awake'], self.events())
        self.assertFalse((self.state / 'reader.pid').exists())

    def test_action_calls_one_shell_without_changing_the_timeout(self):
        workflow = (ROOT / '.github/workflows/android.yml').read_text()
        self.assertIn('script: sh scripts/run-android-gate.sh', workflow)
        self.assertIn('timeout-minutes: 30', workflow)
        self.assertIn('profile: pixel_2', workflow)
        self.assertIn('emulator-build: 16740729', workflow)
        self.assertNotIn('rdd_test_log_pid', workflow)


if __name__ == '__main__':
    unittest.main(verbosity=2)
