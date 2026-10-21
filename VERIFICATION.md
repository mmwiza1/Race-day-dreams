# Phase 1 verification

## Repository baseline

Inspected the full recursive GitHub tree on 2026-10-21. Initial commit
`a1e970a348d38e70f8be715a3e6df36971962b96` contained only `README.md`.
There was no application, build system, test suite or existing implementation to
reuse. The exact baseline Git objects were restored through the connected GitHub
API after shell Git access failed; original object hashes were verified locally.

## Checks run locally

- `sh scripts/test-domain.sh`: **150 checks passed**. Covers all race distances,
  target time parsing, pace/unit conversion and rounding, goal modes, race dates
  (including today/tomorrow/year boundaries), availability, two/six running days,
  overlapping gym/running days, Performance eligibility, immutable data and
  separate planned/completed values. No Android classes or downloaded dependencies.
- `python3 scripts/test-schema.py`: **5 tests passed**, executing the actual DDL
  from `ProfileDatabase.java` with foreign keys enabled. Checks fresh schema,
  overlapping run/gym days, invalid/orphan rows, cascades and rollback. This is
  SQL verification, not Android repository execution.
- All Android XML parses. All 121 string references resolve; no duplicate string
  names. Main manifest requests no permissions.
- Reviewed the complete diff for lifecycle/data-loss paths, Android API 28 library
  compatibility, secrets, unsupported functionality and obvious resource issues.
- Verified the official Gradle 8.11.1 wrapper JAR SHA-256:
  `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.

## Android build and device verification

[GitHub Actions run 39677607836](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39677607836)
passed against commit `80fd7137c60222d759ecae07a5f91d58c4d2a098`:

- Android debug APK assembly and lint.
- Android instrumentation APK assembly.
- All 21 instrumentation tests on an Android 15 / API 35 emulator.
- All 150 domain checks and five SQLite schema tests.
- Debug APK and device-test reports uploaded as workflow artifacts.

The initial CI SDK setup attempted to install the retired `tools` package. The
workflow now requests `platform-tools` explicitly and builds with SDK 35. No
Android code compilation or test failures remained in the successful run.

## Local toolchain limitation

The workspace has Java 21 (including the compiler module) but no Android SDK,
Gradle distribution or emulator. Its normal Gradle cache is outside writable
paths, so the build attempt used a task-local `GRADLE_USER_HOME`.

```sh
GRADLE_USER_HOME=/tmp/rdd-gradle ./gradlew --offline \
  :domain:check :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
```

The wrapper needs a Gradle distribution even for `--offline`; bootstrapping failed
with `java.net.SocketException: Operation not permitted`. The blocked download
was not repeatedly retried. Local Android compilation and device checks remain
unavailable, but the CI build and emulator results above provide actual Android
verification. This environment limitation does not require changing the app or
adding a backend.

## Added device tests

There are 15 repository instrumentation tests for storage round trips, replacement,
optional event/target fields, gym overlap, draft handling, clear/reset, failed-write
rollback, inconsistent rows, unknown enum values, main-thread rejection, retaining
physically corrupt files and recovering through a confirmed database reset.
Eight onboarding draft tests cover incomplete input, JSON restoration, malformed
dates/versions, event-date requirements, eligibility, general fitness and routine
preferences. The original 21 tests passed in the run above. The two additional
corruption/reset tests bring the suite to 23. The workflow executes the full suite
using Android's platform test runner; see the [PR checks](https://github.com/mmwiza1/Race-day-dreams/pull/1/checks)
for the current commit's result.

## Before release

1. Run the Android build/lint and instrumentation commands in README with SDK 35.
2. Install on Android API 28 and API 35+. Check first launch, each onboarding path,
   save/relaunch, edit/cancel, delete, Back navigation and system-bar/keyboard insets.
3. Rotate/recreate at each step, during a save, and after a simulated persistence
   failure. Verify the prior saved profile survives and input remains available.
4. Check TalkBack, large fonts, dark mode, RTL layout and metric/imperial pace.
5. Confirm no health/network permission prompt and no generated coaching results.

The workflow builds, lints, runs the domain/schema checks and executes device
tests. Check the result for the exact commit under review. Automated persistence
and draft tests do not establish that every screen is usable with every font
scale or accessibility service; the manual checks above remain required.
