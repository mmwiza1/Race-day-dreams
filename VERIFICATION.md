# Verification record

## Resumed baseline

Continued the existing `codex/phase-1-android-foundation` branch and interrupted
working tree, rather than recreating the project. Phase 1 commit `3011e037` had
the Java domain, native goal form, SQLite schema 1 and existing persistence/draft
tests. Interrupted activity/baseline/storage/Compose/integration code was audited,
completed and tested. The original main commit was `a1e970a` (README only).

## Local verification, 2026-10-21

- `sh scripts/test-domain.sh`: **615 checks passed**: core 151, reminders 60,
  activity/PB/splits/shoes 138, baseline/assessment 157, provider abstraction 33,
  whole-plan regression 76. Whole-plan inputs are explicitly synthetic test-only
  fixtures; they do not constitute an implemented production plan generator.
- `python3 scripts/test-schema.py`: **24 tests passed**, executing actual schema
  DDL and frozen schema-1/schema-2 migration fixtures with foreign keys enabled. Includes
  original-row preservation, failed-migration/transaction rollback, import identity,
  checkpoint atomicity, annotations, shoe defaults and PB invalidation constraints.
  Both draft schema-2 variants migrate to schema 3 without resetting data;
  failed copies and late failures during 1→3 upgrades roll back completely.
- `git diff --check`: passed. Android XML parsed successfully. Reviewed changed
  source for accidental credentials, SDK paths, destructive upgrades, invented
  metrics, abandoned entry points and licence issues.
- Gradle wrapper JAR remains the verified official Gradle 8.11.1 wrapper:
  SHA-256 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.

The workspace provides Java 21's compiler module, but no Android SDK, emulator
or installed Gradle distribution. The local offline wrapper bootstrap failed
with `java.net.SocketException: Operation not permitted`; blocked downloads were
not repeatedly retried. Android results below therefore come from GitHub Actions,
not an unavailable local device/toolchain.

## Android CI

[Integration run 39688833480](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39688833480)
passed (attempt 2) for commit `05ef331e6816e57ed4b02f2ab51783a1d7c53730`
with schema 3. All three jobs are green.

| Runtime | Domain/schema | Debug APK + test APK | Lint | Instrumentation |
| --- | --- | --- | --- | --- |
| API 28 / Android 9 | 615 / 24 passed | Passed | Passed | 86 passed, 0 skipped, 0 failed |
| API 35 / Android 15 | 615 / 24 passed | Passed | Passed | 86 passed, 0 skipped, 0 failed |
| API 36 / Android 16 | 615 / 24 passed | Passed | Passed | 86 passed, 0 skipped, 0 failed |

Commands executed by CI:

```sh
sh scripts/test-domain.sh
python3 scripts/test-schema.py
./gradlew :domain:check :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:dependencies --configuration debugRuntimeClasspath
./gradlew :app:connectedDebugAndroidTest
```

The resolved runtime dependency graph was reviewed and its permissive transitive
licence notices were added to THIRD_PARTY.md and offline application assets.
APKs and device-test reports are uploaded for each successful emulator job.

Earlier integration failures were fixed before merging: compile SDK was raised
to 36 for stable Health Connect 1.1.0; the Compose/native View call was corrected;
the initial navigation graph was stabilized; and the baseline-review snackbar
that could intercept the next screen's first Continue tap was removed. Tests
retain their behavioral assertions and now report the actual form step and
view hierarchy on a timeout. The first API 36 attempt in the run above failed
before tests while downloading an invalid emulator-image ZIP; only that failed
job was retried. Such infrastructure failure is not counted as a passing test.

Minimum SDK remains 28 and target SDK remains 35. The workflow runs on every PR
and main update; use [PR checks](https://github.com/mmwiza1/Race-day-dreams/pull/1/checks)
for the exact final head and the Actions main-branch run after merge. Older
Phase 1 results are not verification of this implementation.

## Automated Android coverage

The suite retains original profile/draft tests and adds real local-store and UI
checks for migration, import deduplication/updates/deletions, transactional backup
round-trip/rejection/rollback, PB preservation and invalidation, annotations,
shoe assignment/mileage, shared-helper reset, Health Connect availability and
denied-permission boundaries, reminder replacement/cancellation/stale delivery,
manifest privacy and offline notices. Compose/native-form tests exercise manual
fallback, reviewed recorded history, target-time draft recreation, saving goals,
manual results and shoes. Exact executed counts belong to the CI results above.

## Manual/device checks still required before release

- Real Health Connect consent, partial/revoked permissions, extended history,
  updates/deletions and Samsung Health-synchronized records. Emulator/fake-provider
  tests cannot establish vendor interoperability or provider data quality.
- All onboarding paths on physical devices, external document providers, battery
  restrictions, notification delivery after reboot/time-zone change and Android
  notification settings. Inexact alarms may be delayed by the operating system.
- TalkBack traversal, 200% font scale, small screens, dark mode, RTL, keyboard
  interaction, system bars and process death while editing/saving/importing.
- Calendar week/month UI, production plan generation/adaptation, actual GPS/GPX,
  route maps and Wear OS are pending and cannot be reported as device-verified.

No production readiness probability, race prediction, generated plan, GPS track
or NHS programme is fabricated. See DEVELOPMENT_PLAN.md for operational versus
scaffolded features and TRAINING_ENGINE.md for test-fixture limitations.
