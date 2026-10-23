# Race Day Dreams

A free, open-source, account-free Android running companion.

**The app recommends. The runner decides. The plan adapts.**

The app works locally and without Health Connect. It has no Internet permission,
advertising, analytics, subscriptions, backend or LLM dependency.

## 0.5.0-test1

Strength and Progress use summary dashboards with dedicated detail screens. The
local catalogue has 174 canonical exercises, live name/alias search, favourites,
recents and hide/archive choices. Sets and PBs support repetitions, duration and
distance without invented reps. Twenty-five priority movements have original
schematic guides and local technique instructions.

Optional original six-week gym blocks support 1–6 days, equipment/time constraints,
experience, goals and running/strength priority. Versions retain old prescriptions;
moves, skips and reductions require runner choice. Actual workouts stay separate.
Progression advice uses actual sets and effort rather than weekly automatic loading.

Home reuses weekly evidence in “How am I doing?”. Run detail offers explainable
comparison; planned workouts show contextual briefings and one-off-move warnings.
Existing Calendar, running coaching, Health Connect, shoes, weight, reminders and
local backups remain supported. Direct Samsung scaffolding and its setting are
removed; legacy preferences normalize without deleting imported history.

See [feature status](DEVELOPMENT_PLAN.md), [programming rules](docs/STRENGTH_PROGRAMMING.md),
[training engine](TRAINING_ENGINE.md) and [phone checks](TEST_BUILD.md).
Actual build/CI results are recorded in [VERIFICATION.md](VERIFICATION.md).

## Install the test APK

Use a successful [GitHub Actions run](https://github.com/mmwiza1/Race-day-dreams/actions)
for the intended main commit. Its API-35 artifact contains
`Race-Day-Dreams-0.5.0-test1-debug.apk`. Extract the ZIP and install the APK.
GitHub artifact downloads require GitHub sign-in; the app requires no account.
Export a local backup before upgrading. [VERIFICATION.md](VERIFICATION.md) records
actual CI and merge results; an open PR is not evidence of a successful APK.

## App Preview

Screenshots are generated from the current Race Day Dreams build using
instrumentation-only demo data. They are real emulator renders, not UI mockups.
Canonical API-35 images and their source fingerprint are in
[docs/screenshots](docs/screenshots). Capture automation commits images only
after all three API jobs pass; see [SCREENSHOTS.md](docs/SCREENSHOTS.md).

| Home | Training plan | Workout detail |
| --- | --- | --- |
| ![Home](docs/screenshots/home.png) | ![Training plan](docs/screenshots/training-plan.png) | ![Planned workout](docs/screenshots/planned-workout.png) |

| Feedback | Progress | Optional strength |
| --- | --- | --- |
| ![Run feedback](docs/screenshots/run-feedback.png) | ![Progress](docs/screenshots/progress.png) | ![Strength](docs/screenshots/strength.png) |

| Setup | Weekly review | Calendar week | Calendar month |
| --- | --- | --- | --- |
| ![Setup](docs/screenshots/onboarding.png) | ![Weekly coaching](docs/screenshots/weekly-review.png) | ![Week](docs/screenshots/calendar-week.png) | ![Month](docs/screenshots/calendar-month.png) |

| Body weight | Strength records | Health settings |
| --- | --- | --- |
| ![Actual weight measurements](docs/screenshots/body-weight.png) | ![Strength PB provenance](docs/screenshots/strength-pbs.png) | ![Health settings](docs/screenshots/data-sources.png) |

| Exercise search | Offline technique guide | Optional gym block |
| --- | --- | --- |
| ![Exercise aliases](docs/screenshots/exercise-search.png) | ![Original RDL guide](docs/screenshots/exercise-rdl-howto.png) | ![Running-support strength block](docs/screenshots/gym-running-support.png) |

| Run comparison | Timed strength logging | Carry logging |
| --- | --- | --- |
| ![Comparable recorded runs](docs/screenshots/run-comparison.png) | ![Timed exercise entry](docs/screenshots/strength-log-timed.png) | ![Load and distance entry](docs/screenshots/strength-log-distance.png) |

## Build and tests

Use JDK 17 or 21, Android SDK 36 and Android Studio with Kotlin 2.0 support.
The minimum Android version is API 28. Gradle 8.11.1 / AGP 8.9.2 are pinned.
Set `ANDROID_HOME` or an untracked `local.properties` with your SDK directory.

```sh
./gradlew :domain:check :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

The first Android build needs dependency downloads. Pure Java domain tests and
actual schema-DDL checks can also run offline:

```sh
sh scripts/test-domain.sh
python3 scripts/test-schema.py
```

GitHub Actions builds, lints and runs the Android tests on API 28, API 35 and API 36.
Each job uploads a debug APK and device-test report. Exact verified results and
remaining manual checks are recorded in [VERIFICATION.md](VERIFICATION.md).

## Contributing and licensing

Keep calculations independent of Android UI. Preserve planned and actual data,
source identity and runner decisions. No commercial training schedules or GPL
source are incorporated. Original code is [MIT](LICENSE); incorporated libraries
and engineering references are tracked in [THIRD_PARTY.md](THIRD_PARTY.md).

Read [ARCHITECTURE.md](ARCHITECTURE.md), [HEALTH_CONNECT.md](HEALTH_CONNECT.md),
[TRAINING_ENGINE.md](TRAINING_ENGINE.md), [PRIVACY.md](PRIVACY.md),
[TRAINING_SAFETY.md](TRAINING_SAFETY.md), and
[LICENCES_AND_ATTRIBUTION.md](LICENCES_AND_ATTRIBUTION.md).

## Health Intelligence + Run Engine test increment (0.6.0-test1)

The current development branch adds optional personal sleep/RHR/HRV/steps context,
a minimal foreground phone-GNSS recorder, real timestamped samples and interpolated
splits, manual pause and optional conservative auto-pause, BLE HR/step-sensor cadence,
GPX import/export and explicit Health Connect run-total write-back. No online map,
voice coaching or account is required. See [recording and backup details](docs/RUN_ENGINE.md).

Use both the normal app backup and **Back up raw recordings** ZIP before reinstall:
standard JSON alone does not retain raw coordinates/events. Follow [phone checks](TEST_BUILD.md).
Only a successful intended-commit Actions artifact is a verified installable build;
0.6 artifacts are named `Race-Day-Dreams-0.6.0-test1-debug-api-35`. Preview images are
regenerated by real instrumentation; no live GNSS/health evidence is fabricated.
