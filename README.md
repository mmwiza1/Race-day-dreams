# Race Day Dreams

A free, open-source, account-free Android running companion.

**The app recommends. The runner decides. The plan adapts.**

The app works locally and without Health Connect. It has no Internet permission,
advertising, analytics, subscriptions, backend or LLM dependency.

## Coaching increment — 0.4.0-test1

Original deterministic 5K, 10K, half-marathon, marathon and general aerobic plans
now have raw-evidence weekly reviews, bounded multi-week choices, repeated-day
patterns, explicit move/skip history, sample-qualified durability and return
bridges. The runner previews and decides changes. Race day can fall outside
normal running weekdays, and long horizons retain an early foundation.

Five primary tabs: Home, Plan, Calendar, Progress and Strength. Settings is in the
app bar. Setup uses concise choices and optional explanations; dates use natural
UK ordering and pickers. Calendar offers remembered List/Week/Month views.
Strength has structured sets, automatic workout PB history, historical PBs with
unknown/approximate dates, routine-session links and kg/lb. Progress includes
neutral body-weight measurements/graphs and separate optional Health Connect
weight access. Existing running history/PBs/splits, shoes, reminders and account-free
backup remain available, with non-destructive schema five and backup envelope three.

No fake samples, readiness or predictions are generated. Health Connect totals
cannot create splits. The Samsung direct provider is an **unavailable experimental
boundary**, not operational ingestion; no proprietary SDK ships. See
[Samsung constraints](docs/SAMSUNG_PROVIDER.md), [training rules](TRAINING_ENGINE.md),
[feature status](DEVELOPMENT_PLAN.md) and [phone checklist](TEST_BUILD.md).

## Install the test APK

Use a successful [GitHub Actions run](https://github.com/mmwiza1/Race-day-dreams/actions)
for the intended main commit. Its API-35 artifact contains
`Race-Day-Dreams-0.4.0-test1-debug.apk`. Extract the ZIP and install the APK.
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

| Body weight | Strength records | Provider options |
| --- | --- | --- |
| ![Actual weight measurements](docs/screenshots/body-weight.png) | ![Strength PB provenance](docs/screenshots/strength-pbs.png) | ![Optional provider settings](docs/screenshots/data-sources.png) |

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
