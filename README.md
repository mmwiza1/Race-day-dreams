# Race Day Dreams

A free, open-source, account-free Android running companion.

**The app recommends. The runner decides. The plan adapts.**

The app works locally and without Health Connect. It has no Internet permission,
advertising, analytics, subscriptions, backend or LLM dependency.

## Current scope

The current implementation builds on the original Phase 1 foundation:

- Kotlin/Compose/Material 3 navigation and ViewModel/StateFlow presentation, with
  the tested Java goal-onboarding form retained during incremental migration.
- Optional Health Connect running import, source identity, atomic checkpoints,
  updates/deletions, partial-permission handling and recorded-baseline review.
- Optional manual baseline corrections and manually recorded 1–5 km controlled
  or best-effort assessments. Assessments do not record GPS in this version.
- Provenance, baseline confidence and evidence-derived Performance eligibility.
- Activity history/detail, notes, RPE, laps, and reproducible splits/rolling best
  efforts when sufficiently reliable cumulative distance samples exist.
- Personal bests and progression, manually entered/official race results, shoe
  assignments and mileage, and accessible recorded-distance summaries.
- Local, opt-in evening preparation reminders tied to stored planned workouts.
  No plan is generated in this version; without planned workouts there are no
  workout reminders. Notification time is approximate, not an exact alarm.
- Versioned user-chosen backup/export and transactional replace restore.

Health Connect session totals do **not** supply continuous distance samples.
They therefore do not produce invented kilometre splits or rolling PBs. Missing
metrics stay unknown. General GPS recording, a GPX runtime adapter, full plan
and adaptation engines, race predictions, maps and Wear OS remain pending.
See [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) for the detailed status matrix.

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
