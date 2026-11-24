# Platform parity: Android and iOS

Race Day Dreams is Android-first. The iPhone app runs the same shared Kotlin domain, storage and
many shared Compose screens through `sharedUi/src/iosMain` (`AppleAppHost.kt` and its page
files), but it does **not** have complete parity with Android. This page lists the known
differences so that testers, release notes and future work describe them honestly.

- **Basis:** source inspection of the Android navigation graph (`app/.../coach/navigation/*`)
  and the iOS page model (`ApplePage` in `AppleAppHost.kt`), first written for audit finding
  D-4 (`docs/audit-2026-11/D-parity-ci-release.md`) on top of `main` aa6fa233.
- **Not device-verified:** nothing here has been checked on a physical iPhone. iOS code cannot
  be compiled outside macOS, so iOS changes are verified by the macOS CI jobs only.
- **Keep it current:** when a change adds, removes or moves a screen on either platform, update
  the matching row in the same pull request.

Status key: **Same** = shared screen or equivalent behaviour on both; **Partial** = available on
iOS with a different entry point or reduced scope; **Android only** = no iOS equivalent yet.

## Running

| Feature | Android | iOS | Status | Notes and follow-up |
|---|---|---|---|---|
| Free run recording, GPS, pause and resume | `record-run` | Record page (`AppleRecordingScreen`) | Same | Shared `RunMetricsEngine`. Outdoor GPS is unverified on a physical iPhone. |
| Run now: quick distance or time targets, custom workouts, favourites, repeat previous | `run-now`, `spontaneous-run` | Record page: quick targets, "Create custom workout", favourites and repeat previous structured runs | Partial | No separate Run now page on iOS; the same choices sit on the Record page. |
| Unplanned (spontaneous) run linked to the plan afterwards | Run detail | Run detail (`SharedSpontaneousAssociation`) | Same | |
| Planned and structured workout execution | `execute/{version}/{workout}` | Plan or Calendar starts the Record page with the planned workout (`plannedId`), using the shared execution coordinator | Partial | Entered from the plan instead of its own route. iOS run detail has no `StructuredRunSection` (planned-step review). Follow-up: share that section. |
| Race-day run with race target | `race`, `race-run/...` | `RACE` page and Record page with `raceVersion` | Same | |
| Recorded run summary, heart rate, route, GPX export, splits, notes and shoes | `activity/{id}` | `ACTIVITY` page (`AppleRunDetails`) | Same | |
| Run comparison | Run detail (`SharedRunComparisonSection`) | Run detail (`SharedRunComparisonSection`) | Same | Added to iOS for D-4. |
| Run Score and run feedback (read-only analysis) | Run detail (`SharedRunFeedbackCard`) | Run detail (`SharedRunFeedbackCard`) | Same | Added to iOS for D-4. |
| Programme adjustment proposals after a planned run | Run detail (`AdjustmentCard`) | Not on run detail | Android only | Follow-up: needs a shared proposal card; not a small change. |
| Manual "Link to a planned workout" on an unplanned run | Run detail | Only through `SharedSpontaneousAssociation` choices | Partial | |
| Long-run durability card | Run detail (`SharedRunDurabilityCard`) | Not on run detail | Android only | Follow-up: the shared card exists; wire it into `AppleRunDetails`. |
| Running PBs, entered results and PB progression | `progress/pbs` and related | `RESULTS` page (`AppleRecordedResults`) | Same | |
| Running trends | Progress | `RUNNING_TRENDS` page | Same | |

## Strength and other training

| Feature | Android | iOS | Status | Notes and follow-up |
|---|---|---|---|---|
| Strength Today, Gym now and dated gym sessions | `gym`, `gym-now`, `gym/session/{id}` | `GYM`, `DATED_GYM`, `STRENGTH_HOME` | Same | Shared screens. |
| Programme builder, templates and generation | `gym-programme-*` | `PROGRAMMES` (`AppleStrengthProgrammes`) | Same | |
| Exercise library and custom exercises | `exercises`, `exercise/{id}`, `strength-custom-exercise` | `EXERCISES` (`AppleExerciseLibrary`) | Same | |
| Strength analytics | `strength/analytics` | `STRENGTH_ANALYTICS` | Same | Shared screen. |
| Strength PB records screen | `strength/pbs` | None | Android only | Android screen is not shared. Follow-up: move it to `sharedUi`. |
| Strength PB history and historical PB entry | `strength/pbs/history` | None | Android only | Android-only `StrengthScreen` section; no shared screen exists. |
| Recorded strength workout source | `strength/source/{id}` | `STRENGTH_RECORD` (`AppleStrengthRecord`) | Partial | iOS shows the recorded sets only. |
| Strength history | `strength/history` | `STRENGTH_HISTORY` | Partial | iOS lists sessions and exercise names. |
| Training recovery | `strength/recovery` | `TRAINING_RECOVERY`, `RECOVERY_PREVIEW` | Same | |
| Bouldering | `bouldering/*` | `BOULDERING*` pages | Same | |
| Supporting routines | `routines`, `routine/{id}` | `ROUTINES`, `ROUTINE` | Same | |

## Health, progress and app

| Feature | Android | iOS | Status | Notes and follow-up |
|---|---|---|---|---|
| Health data import | Health Connect | Apple Health (HealthKit) | Partial | Different platform providers and permission flows. Real HealthKit import is unverified on a physical iPhone. |
| Health Intelligence trends (HRV, steps and other optional signals) | `health-intelligence` | None | Android only | Needs a HealthKit gateway and store for those signals (a new subsystem). The shared `SharedHealthTrendCard` is ready once data exists. |
| Home, Plan, Calendar, Weekly review, Progress, Achievements, Body | Yes | Yes | Same | Mostly shared screens. |
| Onboarding | Native Android onboarding (`welcome`, `goals`) | Shared starting-path choice (`SharedStartingPath`) | Partial | Already noted in release notes. |
| Dream Routes | Yes | Yes (`AppleSavedDreamRoute`, `AppleDreamSettings`) | Same | |
| Backup export and restore, detail retention | Yes | Yes (`AppleBackupDocuments`, Settings) | Same | |
| Evening reminders | Yes | Yes (`AppleCoachReminders`) | Same | Delivery on a locked iPhone is unverified. |
| In-app updates | Settings › App updates | None | Android only | iOS has no updater; testers install new simulator or unsigned builds manually. |
| Back navigation | System Back and predictive back; saved back stack | One current page, no back stack, fixed `onBack` targets, page not restored after the app is evicted | Partial | Audit risk R-5. Follow-up: a small saveable page stack with an edge-swipe or Back handler, tested from Calendar → dated Gym → detail. Not a trivial change, so it is not part of the D-4 fix. |

## Physical acceptance still outstanding

No row above has been accepted on hardware. See audit risk R-6: Android installs on Galaxy S24,
Pixel and Xiaomi, an iPhone install, outdoor GPS, locked-screen recording, real Health Connect,
HealthKit and Bluetooth heart rate, audio cues, battery and OEM background behaviour, rotation,
TalkBack and VoiceOver by a person, and the in-place update from 0.15.3-test1.
