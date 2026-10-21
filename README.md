# Race Day Dreams

A free, open-source Android app to assist runners in hitting their race goals.

**The app recommends. The runner decides. The plan adapts.**

Race Day Dreams is being built as a local-first running coach, without accounts,
advertising, subscriptions, analytics or an LLM dependency. The current release
implements **Phase 1: the application foundation**. It does not yet generate
training plans, import workouts, predict race results or provide adaptive advice.

## Phase 1

- Native Android application with Home, Plan, Calendar, Progress and Settings
  navigation. Future features have explicit empty states, not simulated results.
- Structured onboarding for 5K, 10K, half marathon, marathon and general
  fitness / aerobic development.
- Optional race event, name and date; Finish, Target Time and Maximum Performance
  goal modes; target race pace calculated from the entered finishing time.
- Distance First, Balanced and Zone 2 / Aerobic strategies. Performance is
  represented in the domain but unavailable until training history can support it.
- Running days, runs per week, preferred long-run day, gym days, fixed/flexible
  schedules and metric/imperial units. Running and gym days may overlap.
- On-device SQLite storage for the profile, goal, preferences and onboarding draft.
- A separate Java domain module containing validation, pace/date calculations,
  repository contracts and immutable models for later training features.

There is no Health Connect access or health permission request in this phase.

## Build and test

The Android project uses Java 17, Gradle 8.11.1 and Android Gradle Plugin 8.9.2.
Use JDK 17 or 21 and Android SDK platform 35. The minimum supported Android
version is Android 9 (API 28).

Open the project in Android Studio, install the requested SDK packages and let
Gradle sync. Alternatively, set `ANDROID_HOME` or create a local, untracked
`local.properties` with `sdk.dir=/path/to/Android/Sdk`.

```sh
./gradlew :app:assembleDebug :domain:check
```

The first Gradle run requires network access to download Gradle, the Android
plugin and build tools. Do not commit SDK paths, signing keys or credentials.

The domain suite has no third-party test dependency and can run without an
Android SDK or Gradle download:

```sh
./scripts/test-domain.sh
python3 scripts/test-schema.py
```

The second command checks the actual schema DDL using Python's SQLite engine;
it does not replace Android repository instrumentation. The current offline
results are 150 domain checks and five schema tests passing.

Run persistence instrumentation tests on an emulator or connected API 28+
Android device:

```sh
./gradlew :app:connectedDebugAndroidTest
```

The Android foundation GitHub Actions workflow builds the debug app, runs domain
checks and lint, and compiles instrumentation tests. Device instrumentation still
requires an emulator or connected device; compiling tests does not execute them.

The development workspace used to establish Phase 1 did not have an Android SDK
or a cached Gradle distribution, and shell network access was unavailable.
Android build and device verification require the toolchain above; a successful
domain test run alone does not establish that the APK builds or launches.
See [VERIFICATION.md](VERIFICATION.md) for the Phase 1 checks and remaining device
verification.

## Contributing

Read [ARCHITECTURE.md](ARCHITECTURE.md) and
[DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) before adding features. Keep training
rules out of Android Views, preserve planned and completed workouts separately,
and explain recommendations in ordinary language. Do not change a runner's goal
or plan without their decision.

The repository initially contained only its purpose statement. There was no
existing application, framework or test suite to migrate.

## Documentation

- [Architecture](ARCHITECTURE.md)
- [Phased development plan](DEVELOPMENT_PLAN.md)
- [Training engine scope and principles](TRAINING_ENGINE.md)
- [Health Connect integration plan](HEALTH_CONNECT.md)
- [Privacy](PRIVACY.md)
- [Training safety](TRAINING_SAFETY.md)
- [Licences and attribution](LICENCES_AND_ATTRIBUTION.md)

Original project code is available under the [MIT licence](LICENSE).
