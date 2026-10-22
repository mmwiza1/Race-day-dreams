# Verification record — coaching test build

## Final functional verification — 2026-10-22

[Run 39751194589](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39751194589)
passed for final functional source
`03d8a3085d38211d2cd37ddb4636980fdc80549d`.

| Emulator | Domain assertions | Schema tests | Debug/test APK + lint | Instrumentation |
| --- | --- | --- | --- | --- |
| API 28 / Android 9 | 30,554 passed | 27 passed | Passed | 100 passed, 0 skipped, 0 failed |
| API 35 / Android 15 | 30,554 passed | 27 passed | Passed | 100 passed, 0 skipped, 0 failed |
| API 36 / Android 16 | 30,554 passed | 27 passed | Passed | 100 passed, 0 skipped, 0 failed |

Commands: `sh scripts/test-domain.sh`, `python3 scripts/test-schema.py`,
`./gradlew :domain:check :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`,
and `./gradlew :app:connectedDebugAndroidTest` on each emulator. Lint succeeded;
this does not claim zero informational warnings.

The tests include production plan invariants and eleven full-prescription fixtures,
raw-evidence adaptation/accept/reject, purpose-based Run Scores without HR/RPE,
interval/progression boundaries, strength PB/estimated-1RM/deletion, baseline/provider
fallbacks, real SQLite migration/restore, reminder policy, real coaching forms,
metric/imperial completion and preservation of completed targets during rebuilds.
There are 30,554 domain assertions, not that many independently named JUnit cases.

`Race-Day-Dreams-0.3.0-test1-debug.apk` was uploaded after tests passed. The
[verified API-35 artifact for this functional source](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39751194589/artifacts/12414220991)
contains the installable APK. A documentation-only follow-up may have a newer
successful artifact; check the final [draft PR checks](https://github.com/mmwiza1/Race-day-dreams/pull/2/checks).
No APK is represented as locally built: this workspace has no Android SDK.

Ten canonical screenshots were rendered by the actual app using test-only fixtures,
published after the complete matrix passed, retrieved and visually reviewed.
Feedback and workout captures show real computed values with ordinary distance
precision. Source digest/commit are stored beside the PNGs. Screenshot publication
is limited to the test branch. Bot-generated capture commits may require GitHub
workflow approval; a reviewed follow-up commit runs full verification of the final
branch head without changing application source.

Schema **4** upgrades explicitly from 1/2/3 and retains prior data. Backup envelope
**2** includes coaching history; original envelope-1 backups remain readable, and
older app versions reject the new format instead of dropping unknown history.
The core store format and coaching subsection retain their own version 1.

Resolved findings during this pass: partial-week mileage concentration, transient
snackbar interception of the rebuild action, screenshot collection after AGP
uninstall, floating-point display precision, backup-version compatibility and
same-day completed-prescription immutability. No new runtime dependencies or
permissions were introduced. The third-party inventory/embedded copy were updated.
Final diff/secret-pattern review and whitespace checks found no credentials,
SDK paths, proprietary/GPL source or production demo-seeding path in the changes.

This is a draft physical-device evaluation build. Main remains the baseline SHA;
no merge is authorised. Remaining feature boundaries and Galaxy S24 checks are
listed in TEST_BUILD.md, DEVELOPMENT_PLAN.md and TRAINING_ENGINE.md.

## Earlier integration records

## Base and delivery restriction

Started from exact latest main `7624a662c5a52b6cd0ab427c09e17b354b50c62e`.
Work is on `codex/production-coach-test-build`, draft PR #2. Main must remain
unchanged. Version code 3 / name `0.3.0-test1`; application ID unchanged.

## Verified integration candidate

[Run 39744482333](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39744482333)
passed for source `5a9ee8953970f182a9bd7dbbe4794ce8238b77e5`.
API 28, 35 and 36 each passed:

- domain suite: **30,540 assertions** (615 existing plus 29,925 coaching checks);
- schema suite: **27 tests**, including v3→v4 preservation, one-current-plan
  constraints and failed-v4 migration rollback;
- `:domain:check :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`;
- **93 instrumentation tests**, 0 skipped, 0 failed, including new real SQLite
  coaching persistence/restore/migration and actual-build screenshot navigation;
- named debug APK and real screenshots uploaded after device tests;
- canonical API-35 screenshot publisher succeeded on the test branch.

The subsequent documentation/restore-hardening source
`6a83530a072437f53261552ace8aa32783a7b661` also passed the three API jobs and
canonical publisher in [run 39746040498](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39746040498).
Further interaction/regression/viewport changes require their own successful CI
before being considered the final candidate. No unobserved result is claimed here.

## Local checks

The current environment has Java's compiler module but no Android SDK/emulator
or installed Gradle distribution. One Android repository connectivity check
failed because the configured proxy was unreachable. It was not retried.
Android results above come from GitHub Actions, not a local Android build.

`sh scripts/test-domain.sh` currently passes **30,554 assertions**: core 151,
reminders 60, activity/PB/splits/shoes 138, baseline/assessment 157, provider 33,
synthetic invariant harness 76, production coaching **29,939**. These are assertion
counts, not 30,554 independently named JUnit tests. Coaching exercises 75 generated
constraint scenarios, focused score/adaptation/strength/load boundaries and eleven
readable full-plan fixtures plus a compact half-marathon regression fixture.
`python3 scripts/test-schema.py`: **27 tests passed**. `git diff --check`: passed.

Reviewed new code for preserved original prescriptions, explicit runner decisions,
missing-data handling, same-day gym/run, source identity and backup exclusion of
raw provider records. No new runtime dependencies/permissions, GPL source,
commercial schedules, SDK paths, credentials or signing keys were introduced.
MIT implementation and existing third-party inventory/notices are retained.

## Physical-device evaluation still required

See [TEST_BUILD.md](TEST_BUILD.md). CI does not establish Samsung Health/Samsung
Health Connect interoperability, OEM reminder timing, real GPS behaviour, physical
TalkBack/large-font/RTL usability or coaching efficacy. GPS recording remains out
of scope. Strength editing/lb conversion, advanced adaptation and multi-evidence
race predictions remain explicitly pending. Canonical image review checks actual
renders; source fingerprints are not automatic visual approval.

## Historical foundation verification


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
