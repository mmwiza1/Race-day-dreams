# Audit area B: training, gym, bouldering and statistics (main aa6fa233, report only)

## 1. Scope

**What I inspected (source read only, no builds):**
- **Coaching and plans:** `OriginalTrainingPlanGenerator` (phases, taper, weekly budget, race-week trim), `AdaptiveCoaching`, `WeeklyCoaching` (review, hold/reduce/bridge/quality-pace choices), `PlanPreservation`, and `RunningPlanCoordinator.changeWorkout` (move and skip).
- **Calendar and status semantics:** `CalendarData` (completed, not completed, moved, skipped, rest, gym overlays), the Home and Strength "Your next 7 days" strip (`WeekAtAGlance`), and `WorkoutHistoryQuery` dating.
- **Dashboards:** `DashboardAnalytics` (running totals, plan and programme completion, strength trend, estimated 1RM, heaviest set, volume, muscle workload, previous-period comparison), `RecordedWeekAggregation`, `StrengthVolume`, `RunningTrendPoints`, `SharedDashboards`, and `StrengthProgression.kt` (V3 charts).
- **Gym:** `StrengthRecordPolicy` (Epley), `StrengthRecords`, `StrengthProgressionAdvisor`, `GymRestTimer`, the `GymWarmup` rounding, and `MassUnit` and how it is used.
- **Bouldering:** `Climbing.kt`, `ClimbingAnalytics`, and `SharedBoulderingCoordinator`.
- **Race:** `RaceCalendar`, `RacePredictionEngine`, `RaceStrategyEngine`, the `SharedRaceDay` countdown, and the Home `CountdownCard`.
- **Date maths:** the ISO date arithmetic in `time/LocalDate.kt`. Year starts and month/leap handling are correct.
- **Tests used as reference:** `DashboardAnalyticsContractTest`, `RunningCoachingReviewContractTest` and `RaceCountdownTest`.

**What I could not verify:**
- How the screens look on real devices.
- Real Health Connect and HealthKit time-zone payloads. Only the importer and its tests were read.
- Behaviour on an actual DST changeover.
- The full plan-generator output across every goal. I did not run the generator.

## 2. Confirmed bugs

| ID | Sev | Area/screen | Files (path:line) | Evidence | Impact | Proposed fix |
|---|---|---|---|---|---|---|
| B-01 | P1 | Calendar, weekly review, history, race prediction (iOS especially) | `sharedDomain/.../presentation/CalendarData.kt:264-265`; `coaching/CoachingPlatform.kt:35-36`; `activity/WorkoutHistoryQuery.kt:85-89`; `race/RacePredictionEngine.kt:67-68`; `sharedHealth/src/commonTest/.../HealthKitRunningImportTest.kt:51` | Each of these dates a run with `startOffset() ?: UTC`. HealthKit imports always have a null `startOffset` (the test asserts this), and Health Connect's offset is optional. Home and Progress instead date runs in the device zone (`AppleRunningToday.kt:32`, `DashboardAnalytics.running(displayDate)`). | Example: an iPhone user in New York runs on Monday at 20:00, which is 01:00 UTC on Tuesday. The run then appears on Tuesday in Calendar and History, can show as "completed on another day", and can fall into the wrong week of the weekly review. Progress and Home put it on Monday. In Australia (UTC+10/+11), a Monday-morning run lands on Sunday, in the previous review week. | Use one dating rule everywhere: the record's offset when it has one, otherwise the device zone. Pass a `displayDate` function into `CalendarData.day`, `WeeklyCoaching`, `WorkoutHistoryQuery` and `RacePredictionEngine`. |
| B-02 | P2 | Weekly review, race prediction, plan proposals | `coaching/WeeklyCoaching.kt:222-234` (deduplicates by id only); `race/RacePredictionEngine.kt:66-79`; `SharedRaceDay.kt` (passes `state.activities`); `sharedStorage/.../SqliteActivities.kt:39-67` (no deduplication) | `PhysicalRunSessions` exists because the same run often arrives from two sources. Dashboards, the calendar and the trend charts use it. The weekly review km and longest run, `scaled(... km)` for the hold/reduce ceilings, and the race prediction's weekly average and endurance gate all sum the raw records. | A run recorded natively and also imported from a watch through Health Connect counts twice. The weekly-review headline shows double the km that Progress shows. The "Hold volume" ceiling becomes too generous. A race prediction can pass the endurance gate (`RacePredictionEngine.kt:83-86`) on duplicated evidence. | Apply `PhysicalRunSessions.counted()` before these calculations, or to `coachingActivities` in the snapshot reader. |
| B-03 | P2 | Home race countdown | `sharedUi/.../SharedHomeScreen.kt:291-297`; `RddDesignSystem.kt:563-596` | Home passes `raceDate - today` unclamped to `CountdownCard`, which renders `"$days"` with the label "Race preparation". `RaceCalendar.daysUntil` clamps to zero and `raceCountdown()` handles past dates (`RaceCountdownTest`), but Home uses neither. | After the race, Home shows "-3 days · Race preparation". On race day it shows "0 days". | Use `raceCountdown`-style wording, or hide the card or switch to a "Log your result" state once `days <= 0`. |
| B-04 | P2 | Imperial distance units | `SharedHomeScreen.kt:280`; `SharedWeeklyReviewContent.kt:52-53`; `SharedRaceDay.kt:143-145`; `RacePredictionEngine.kt:81` | These lines hard-code `"… km"` and `"/km"`. Other screens honour `UnitSystem.IMPERIAL` (`SharedPlannedWorkoutScreen.kt:63`, `RunningTrendExplorer.kt:85`). | Imperial users see km on Home's upcoming runs, the weekly-review headline, the race pacing stages and paces, and the prediction reasons. | Route these through `presentation.distance` and the existing pace-factor helpers. |
| B-05 | P2 | Mass unit (lb) in the gym | `strength/SharedGymWorkoutScreen.kt:328` ("Actual load · kg"); `SharedGymWarmupGuidance.kt:44`; `SharedProgrammeProgressionSheet.kt:252`; `presentation/StrengthPrescriptionText.kt:79-87,173-174`; `strength/StrengthProgression.kt:84`; `strength/StrengthAnalytics.kt:132` | The `mass.unit` setting is applied only in `SharedDashboards` and `SharedExerciseDetail`. Set logging, warm-up ramps, progression advice and PB/record labels stay in kg. | A user who chooses lb sees lb on Progress but must log and read prescriptions in kg. A lb value typed into a field they expect to be lb would be stored as kg (2.2× too heavy) and would distort records and estimated 1RM. | Convert input and output with `state.massUnit` at the UI boundary. Keep storage in kg. |
| B-06 | P2 | Progress → Running → "Plan completion" skipped count | `DashboardAnalytics.kt:104-117`; `SharedDashboards.kt:71-80,169`; `application/RunningPlanCoordinator.kt:213-219` | Skipping a workout removes it from the new plan version (`movedTo == null -> null`). `planCompletion` iterates only the current plan's workouts, so the SKIPPED branch is never reached. The unit test (`DashboardAnalyticsContractTest:162-194`) passes skipped workouts that production never supplies. | "N skipped" never appears. Missed and skipped sessions from earlier plan versions or rebuilds also drop out of the due count. | Include the original workouts of SKIPPED decisions from plan history (as `WeeklyCoaching.kt:244-249` does), and align the test fixture with production. |
| B-07 | P3 | Home and Strength "Your next 7 days" strip | `sharedUi/.../WeekAtAGlance.kt:113-119,120-143` | (a) Runs come from raw `state.activities` without `PhysicalRunSessions`, so a duplicated run shows two "Run" done chips. An unlinked duplicate of a linked run adds an extra chip next to the planned one marked done. (b) A gym workout linked to a session on another day is counted on both days, as "Strength workout" on the recorded day and as a done "Strength session" on the scheduled day. Links allow ±3 days (`GymWorkoutAssociation.kt:160`). `CalendarData.withGym` avoids this. | The day strip over-reports completed sessions and disagrees with the Calendar. | Reuse the calendar's de-duplication and gym-link rule. Add a commonTest for `WeekAtAGlance`; none exists. |

## 3. Risks and recommendations

| ID | Priority | Area | Files (path:line) | Evidence | Impact | Proposed fix |
|---|---|---|---|---|---|---|
| B-R1 | P2 | Plan rebuild and completion history | `OriginalTrainingPlanGenerator.kt:39,343`; `PlanPreservation.kt`; `SharedDashboards.kt:71` | Workout ids are `nameUuid("$start:$goal:$prefs") + ":day"`, so a rebuild with a new start date creates new ids. Completion is computed only over the current plan. | After a rebuild, Progress plan completion for 12-week, 6-month and 1-year periods loses earlier due and completed sessions. | Compute completion over the plan-version history, as `WeeklyCoaching` already does. |
| B-R2 | P3 | Adaptive move proposal | `coaching/AdaptiveCoaching.kt:147-154` | The conflict check counts the workout being moved (`next`), because it is demanding and only one day away. The run-count check excludes it (`it.id() != next.id()`, line 159). As a result the day immediately after can never be offered. `RunningCoachingReviewContractTest:411-432` expects +2 days (15 → 17 Nov); I could not tell whether +1 was meant to be excluded. | The most natural move option (one day later) is never proposed. | Add `it.id() != next.id()` to the conflict check, or document the intent in the test. |
| B-R3 | P3 | Race pacing strategy | `race/RaceStrategyEngine.kt:44-66` | Opening 15% at 1.01–1.035× target pace and the middle 70% at 0.995–1.01×. At the midpoints of those ranges, the last 15% must be about 3.4% faster than target to finish on time, while the guidance says "no sprint is required". | Runners who follow the ranges finish slower than their target. | Centre the middle range slightly faster, or state the closing pace required. |
| B-R4 | P3 | Weekly review "Hold volume" with no distance data | `WeeklyCoaching.kt:505` | When last week's recorded km is 0 (no distances, for example treadmill runs without distance), `factor = .8`. "Hold" then cuts 20%, the same as "Reduce". | Confusing choices. | Fall back to 1.0 for "hold" when there is no evidence. |
| B-R5 | P3 | Weekly review completion boundary | `WeeklyCoaching.kt:203-206` | `tomorrow` is UTC midnight, not local midnight. | West of UTC, a run completed late today can be ignored when linking. | Use the local day boundary. |
| B-R6 | P3 | Bouldering live session over 24 hours | `climbing/Climbing.kt:180-183,225-227`; `SharedBoulderingCoordinator.kt:147-150,208` | A live timer that is left running past 24 hours cannot be stopped (the draft validation throws) or saved until the user edits the minutes. | A stuck-looking session with only an error message. | When stopping, clamp to `MAX_DURATION` and prompt the user to correct it. |
| B-R7 | P3 | Bouldering weekly progression | `ClimbingAnalytics.kt:93-98` | `sends` counts sends from both grading systems, but `highestSend` uses only the chosen system. | The chart's sends and grade come from different populations. | Filter both the same way, or label the difference. |
| B-R8 | P3 | Warm-up rounding | `strength/GymWarmup.kt:116-118` | Warm-up ramps round to 0.5 kg. | Loads that are not loadable on a typical 2.5 kg plate step, and they are shown in kg only. | Round to the available increment in the user's unit. |
| B-R9 | P3 | Tests | `sharedUi/src/commonTest` | There are no unit tests for `WeekAtAGlance`, the Home `CountdownCard` days, or HealthKit offset-less dating across zones. | The regressions above went unnoticed. | Add focused tests, including DST and year-end cases (31 Dec to 1 Jan week strip, Sunday 23:30 local in UTC±10). |

**Checked and found correct:**
- ISO epoch-day and month arithmetic, and the year-boundary range label.
- Monday week boundaries (`mondayEpochDay`).
- Previous-period windows.
- Division-by-zero guards in pace, volume and muscle-workload bars.
- Epley estimate bounds (2–10 reps).
- `GymRestTimer` remaining time never exceeding duration under adjust, and monotonic interruption.
- The skip and move rewrite in `RunningPlanCoordinator`.
- Moving pace (ms/m = s/km).

## 4. Future features and ideas

- One shared "display date" service for every activity and session type, which would fix B-01 by design.
- A unit-aware formatting façade (distance, pace, mass) that is required at compile time for all user-facing numbers.
- A post-race state on Home: log your result, compare it with the prediction, and suggest the next goal.
- Plan-completion history that survives rebuilds, with a per-version breakdown.
- An approximate cross-system bouldering grade progression (V/Font), clearly labelled.
