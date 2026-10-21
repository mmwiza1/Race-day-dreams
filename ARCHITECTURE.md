# Architecture

## Current implementation: Phase 1

The repository began with a README only. There was no Android framework, build
system, application architecture or working behavior to preserve.

The foundation uses native Android platform Views, Java 17 and SQLite. There
are no third-party runtime libraries, application servers or cloud accounts.
This is a deliberately small starting point; introducing a library later should
solve a concrete problem and include a licence review.

```text
:app
  Android Activity / Views and navigation
          ↓ validated domain values
  repository adapter and SQLiteOpenHelper
          ↓
  private on-device database

:domain
  immutable models · validation · pace/date calculations
  repository contracts
  no Android imports, database APIs, network clients or UI dependencies
```

The Android app depends on `:domain`; the domain does not depend on the app.
Repository calls run on an application I/O executor rather than the UI thread.
Android rendering and navigation must remain separate from training decisions.
`RaceDayDreamsApplication` owns the repository and `ProfileController`'s serial
I/O queue. The controller returns results to the main thread; it has no View or
Activity reference of its own. Activities discard callbacks after destruction.

## Domain

`UserProfile` contains the runner's `RaceGoal`, `TrainingPreferences` and an
explicit eligibility flag for future Performance training. A new profile has no
training history and cannot claim that eligibility.

`RaceGoal` distinguishes a general fitness goal from a race distance and an
optional dated event. A target finishing time is meaningful only in Target Time
mode for a race distance. `PaceCalculator` derives pace from exact race distance
and duration; `OnboardingValidator` validates date-dependent input against an
injected clock rather than a hidden global test date.

`TrainingPreferences` stores running and gym availability independently. A
preferred long-run day must be an available running day. The number of runs
cannot exceed the selected running days. Neither a gym day nor a flexible
schedule removes running availability.

Future-facing models cover plans, phases, planned workouts, actual workouts,
structured segments, gym sessions, metrics, predictions, readiness and adjustment
choices. A type is not an implemented feature. No training or prediction engine
runs in Phase 1, and the UI must not produce invented data for those models.

## Local persistence

The `UserPreferencesRepository` contract hides storage from domain consumers.
The Android adapter uses `SQLiteOpenHelper`, with a versioned schema, foreign
keys and transactional writes. It stores the singleton profile, race goal,
training preferences and selected days. Enum names, ISO dates and duration
seconds are explicit storage representations; changing these requires a
migration.

Incomplete onboarding is stored separately as a draft. Saving a validated
profile commits the profile and clears the draft together. Reading inconsistent
or corrupt persisted data must report an error rather than silently replacing
it with default or demo data. Database upgrades must use explicit migrations;
dropping user data is not an upgrade strategy.

An explicit corruption handler preserves unreadable database files instead of
using Android's default destructive recovery. Only the user-confirmed local-data
reset closes and deletes the database and its journal files. This reset does not
need to open SQLite, so it can recover from a physically unreadable database;
the next load creates an empty schema. Deletion failures remain visible errors.

The database belongs to the app's private storage. Backup is disabled and the
manifest does not request Internet access. See [PRIVACY.md](PRIVACY.md).

## Navigation and state

Onboarding leads into Home, Plan, Calendar, Progress and Settings. The foundation
shows the runner's saved goal and routine; screens that need future training or
workout data explain what is pending. Configuration changes and process restarts
must preserve entered onboarding values through saved state / the local draft.

Settings edits should pass through the same validation and repository path as
initial setup. Saving failures should keep the current input available for retry.
Do not hide persistence errors behind an apparently successful save.
Draft writes are debounced, flushed when the screen stops and suspended during a
final save. On recreation, a saved-state copy preserves even a form whose save
was in flight; loading is queued after that save and compares the saved profile
with the attempted form to resolve navigation.

## Extension boundaries

- Phase 2 adds Health Connect behind provider-neutral contracts.
- Phase 3 adds deterministic plan generation, consuming domain data only.
- Phase 4 adds workout persistence and calendar presentation.
- Phase 5 adds proposed adjustments with reasons and user-selected alternatives.
- Phase 6 adds prediction ranges, target reassessment and readiness explanations.

Completed workouts must reference, not overwrite, their planned prescription.
Goal changes and significant plan changes require a runner decision. No future
engine may turn a recommendation into a prohibition on exercising.

## Verification

`scripts/test-domain.sh` compiles and runs pure Java assertions without Android
or third-party dependencies. Gradle's domain check task runs the same suite.
Android instrumentation tests exercise SQLite behavior on a real Android runtime.
Both an Android build and device testing remain necessary before release.
