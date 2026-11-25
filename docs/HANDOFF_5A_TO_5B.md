# Handover: Piece 5A (Claude) → Piece 5B (Codex)

Written at the end of Piece 5A on 24 November 2026. Everything needed for Piece 5B is in this
repository; nothing depends on the Claude conversation. Read with `AGENTS.md`, `CLAUDE.md` and
`docs/TRAINING_PROGRAMME_PLAN.md`.

## 1. Repository state at the original handover

| Item | Value |
| --- | --- |
| Repository | `mmwiza1/Race-day-dreams` |
| Integration branch | `ccr-60f602a7-tfmok3-training` (created from `main` `60b5201b`) |
| Pull request | [#51](https://github.com/mmwiza1/Race-day-dreams/pull/51), **draft**, not merged |
| Piece 5A implementation commit | see the "Final commit and CI" table at the end of this file |
| Version | Unchanged (0.15.5-test1 / 28 on `main`). The bump happens once, in Piece 5B. |
| Documentation PR #50 | Parked draft, unrelated; leave it alone. |

Approval milestones on the branch: Piece 1 `067bf228`, Piece 2 `f9318590`, Piece 3 `f723eeec`,
Piece 4 `fe216621` (iOS test compile fixed in `35d5b20f`), Piece 5A (this handover).

## 2. What Pieces 1–5A implemented

Full per-piece detail, with reviewed snapshot changes, is in `docs/TRAINING_PROGRAMME_PLAN.md`.

- **Piece 1 — safety, correctness, transparency.**
  - A long run that would follow a demanding run moves to a well-spaced available day
    (`OriginalTrainingPlanGenerator.relocatedLongDay`) instead of losing its kilometres.
  - Weekly budget shortfalls are explained, and the session time budget is honoured.
  - `PlanFeasibility` rates each race plan as Supported, Limited or Not supported from several
    factors, with a guide-not-guarantee caveat. It is shown on the Plan screen
    (`sharedUi/.../PlanFeasibilityCard.kt`).
- **Piece 2 — Strength dashboard.** Tonnage displays are removed. The dashboard order is progression,
  then at a glance, recent workouts and sets by muscle group. Required screenshots went from 522 to
  519.
- **Piece 3 — running progression.**
  - Weekly targets are individual, with 7% growth clamped to 0.5–3 km.
  - The long run has its own track: shares are guidance, with a 70%-of-week safeguard and a 30-day
    +10% guide.
  - Cutbacks are 0.8× and never stack with the taper. Taper length depends on race distance.
  - General fitness maintains volume unless Distance First is chosen. The beginner run/walk cap is
    kept.
- **Piece 4 — race-specific workouts.**
  - `QualityWorkouts` provides threshold, 5K/10K intervals, half-marathon-pace repetitions and
    marathon-pace blocks, all with `RECOVERY` segments.
  - Paces come from race results only, never from goal times.
  - Readiness is graded: none, strides, controlled or Performance.
  - Returning runners get 3 easy weeks. There is at most one session a week, reduced around hard leg
    days.
  - The taper keeps a shorter familiar session; race week has strides only.
- **Piece 5A — adaptive coaching** (this handover, section 3).

## 3. Adaptive coaching architecture (Piece 5A)

Piece 5A extends the **existing** weekly review. It adds no new screen, service, storage table,
background job or polling.

```
Snapshot read (app open / refresh)
  └─ SharedCoachSnapshotReader (sharedUi/.../coach/application)
       └─ WeeklyCoaching.review(...)               sharedDomain/.../coaching/WeeklyCoaching.kt
            ├─ existing channels, observations, hold / reduce / return bridge / pace review
            └─ BlockReassessment.evidence + assess  sharedDomain/.../coaching/BlockReassessment.kt (new)
                 → extra observations, optional "rebuild" or "progress" choice
       └─ saved once per review id (content hash) → shown by SharedWeeklyReviewContent (sharedUi)
Runner chooses → SqliteCoachingContext.decide (sharedStorage) → WeeklyCoaching.apply
  → new PlanVersion ("Weekly review: <label>") + REVIEW_DECISION record
```

Relevant files:

| File | Role |
| --- | --- |
| `sharedDomain/src/commonMain/kotlin/org/racedaydreams/domain/coaching/BlockReassessment.kt` | **New.** Four-week evidence, rebuild and progress rules, race realism, structure-preserving `scale`. |
| `sharedDomain/src/commonMain/kotlin/org/racedaydreams/domain/coaching/WeeklyCoaching.kt` | Calls `BlockReassessment`. Hold/reduce now use `BlockReassessment.scale`, so Piece 4 repetitions survive. |
| `sharedDomain/.../coaching/AdaptiveCoaching.kt`, `PerformanceReassessment.kt` | Existing per-run proposals and the pace review (unchanged). |
| `sharedStorage/.../SqliteCoachingContext.kt` | Existing: stores reviews, accepts one decision per review, creates plan versions. Unchanged. |
| `sharedUi/.../coach/SharedWeeklyReviewContent.kt` | Existing generic UI: recommendation, observations, choice buttons, current-vs-proposed preview, accept or keep. Unchanged. |
| `sharedDomain/src/commonTest/.../AdaptiveBlockReviewTest.kt` | **New** focused tests (8). |

## 4. Evidence, decision rules, safeguards, acceptance

### Evidence

- **Counted:**
  - recorded runs (native, imported or manual), with duplicates of one physical run counted once
    (`PhysicalRunSessions.counted`);
  - only runs dated before the review week's Monday;
  - links to planned workouts through `RunExecution` or `plannedWorkoutId`.
- **Never counted as evidence:** planned or future workouts.
- **Four-week window:**
  - kilometres per week;
  - which weeks have recorded runs;
  - planned and completed sessions and planned kilometres;
  - the longest recorded run.
- **Missing data versus inactivity:**
  - With no runs recorded in 4 weeks, the review says data may not be syncing, asks the runner to
    record a break if there was one, and changes nothing.
  - A week with no recorded run is *unknown*, not rest. It is left out of the rebuild average,
    blocks progression, and is mentioned to the runner.
- **Interruptions:**
  - A recorded gap of 14 days or more, or a runner-entered interruption, uses the existing return
    bridge.
  - Recurring misses on the same weekday (3 of 4 weeks) are treated as reduced availability. The
    runner is pointed to editing their days, which regenerates the plan with all Piece 1–4 rules.
    No rebuild is offered for this.

### Decision rules (priority order)

1. **Existing review choices take priority:**
   - strain (rising effort with shortened runs, repeated hard RPE, or fading long runs) → hold or
     reduce;
   - a break → three-week return bridge.

   If any of these signals is present, neither rebuild nor progress is offered.
2. **Rebuild ("Rebuild from my recent running").** Offered when **all** of these hold:
   - at least 2 weeks have recorded distance;
   - the recorded average is below 75% of the next two planned weeks;
   - the race is at least 14 days away;
   - there are no recurring same-day misses.

   The next 4 weeks are capped at the recorded average × 1.00, 1.05, 1.10 and 1.15. Weeks are never
   increased, and the race is never touched. The weekly review then reassesses again.
3. **Progress ("Progress cautiously for two weeks").** Offered when **all** of these hold:
   - all 4 weeks are recorded;
   - every planned session was completed (no compensation for misses);
   - each week was at least 115% of the planned weekly average;
   - the race is at least 35 days away;
   - the goal is not general fitness, unless Distance First was chosen.

   Only easy and long runs in the next 2 weeks grow:
   - the weekly total rises at most 10%, and never above the recorded average;
   - a long run grows to at most 110% of the longest recent run;
   - quality sessions and intensity are never increased.
4. **Race realism** (observations only):
   - When fewer than half of the planned runs were recorded with 8 weeks or less to go, the review
     says the race (and any target time) may no longer be fully supported. It offers completion,
     run/walk or a later race, and says missed runs are not made up.
   - A rebuild that lowers the `PlanFeasibility` rating, judged as a finish goal, is explained. A
     target-time goal is told the target may no longer be realistic.

### Safeguards

- **Days and gym schedule:** changes resize existing workouts on their existing dates. The runner's
  days, long-run day, gym days, the Piece 1 spacing and the race date are never moved.
- **Structured sessions:** `BlockReassessment.scale` keeps the repetitions and recoveries and resizes
  only the warm-up, cool-down and easy running. A session that no longer fits (less than 2 km of easy
  running left) becomes an easy run. No segment drops below 0.1 km, because the execution engine
  rejects non-positive steps.
- **Wording:** no medical interpretation; the existing return-bridge text says "not medical
  clearance". The runner can always keep the plan and run independently of it.

### Acceptance and rejection

- **Proposals:** each review offers "Keep original plan" plus at most the applicable changes. A
  change opens a preview of current against proposed, per workout.
- **Accepting** calls `WeeklyCoaching.apply`, which:
  - requires that the review matches the current version;
  - refuses to change completed sessions;
  - preserves ids, dates and all workouts before the review week.

  The plan then moves to a new `PlanVersion`, with the reason "Weekly review: <label>".
- **Rejecting ("Keep")** records the decision without creating a version.
- **Prompt limits:** one decision per review id. The review id is a content hash of version, week,
  observations, channels and choices, so the same review is never prompted twice and there is at
  most one review per week.

## 5. Design decisions, limitations, trade-offs

- **Reuse over new infrastructure:** no new persistence, schema migration or UI. The risk is low, but
  rebuild and progress are only as visible as the existing weekly review.
- **Original 5A behaviour (superseded in 5B):** after a rebuild, week 5 returned to the original plan, and the next
  weekly reviews reassess. This avoids silently re-planning the whole block, but a runner who stays
  low will see a rebuild offered again. That is intended: it is periodic, and one choice per weekly
  review.
- **Regeneration is not used for rebuilds.** The generator creates new workout ids, and `apply` keeps
  ids to protect history. A full regeneration from evidence remains available through the existing
  "edit goal/routine" flow.
- **Progress is deliberately narrow:** 2 weeks, easy and long runs only, at most 10%.
- **Thresholds are heuristics:** the 75%, 115%, 8-week and 2-of-4-weeks thresholds are
  coaching heuristics chosen to be conservative, not validated values.
- **`PlanFeasibility` without a baseline:** the realism check judges preparation only, as a finish
  goal. Target-time realism is stated as a caution, not computed.
- **Inherited limitations:**
  - from Piece 4: recovery-jog distance is estimated at 0.18 km a minute when sessions are fitted;
  - runners with many short days, for example 30 km over 6 days, get strides only, with no
    structured session;
  - 4–6 km midweek days stay easy until they can hold 2 × 1.6 km.

## 6. Tests run in Piece 5A

All commands were run from the repository root with `ANDROID_HOME=/opt/android-sdk`.

| Command | Result |
| --- | --- |
| `./gradlew -q --max-workers=4 :sharedDomain:jvmTest --tests '*Coaching*' --tests '*Plan*' --tests '*Adaptive*' --tests '*Weekly*' --tests '*Review*' --tests '*TrainingPlanSafetyTest*' --tests '*RunningProgressionTest*' --tests '*QualityWorkout*'` | **118 tests, 0 failures.** Includes the new `AdaptiveBlockReviewTest` (8). |
| `./gradlew -q --max-workers=4 :domain:domainTest` | **Pass.** All legacy suites pass, including `CoachingTestSuite` (29,560 checks) and `WeeklyCoachingTestSuite` (96 checks). |
| `./gradlew -q --max-workers=4 :sharedUi:compileDebugKotlinAndroid :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` | **Compiles.** |
| `python3 scripts/format-code.py --check` | **Pass.** |
| `grep` scan of new common code for JVM-only APIs | Missed `minusWeeks` at first; the iOS CI caught it, and it is fixed. A broader scan of all new Piece 4/5A files against the shared `LocalDate` API now finds nothing. |

**Not run locally:** iOS compile and tests (macOS only; covered by CI `apple-shared`), the full
shared-domain suite, all instrumented, emulator and device tests, and screenshots.

The production snapshot fixtures were **not changed** by Piece 5A. Generation is untouched, and
`domainTest` confirms this.

## 7. Known defects, regressions and incomplete work

| Item | Class | Status |
| --- | --- | --- |
| `SharedDashboardScreenTest.strengthDashboardLeadsWithProgressAndAccomplishmentsNotTonnage` (Android instrumented, `android-shared`) | **Fixed in 5A at the owner's request** (test defect, introduced with Piece 2 `f9318590`). | Failed on every CI run from Piece 2 to `cd20f242`, always as the only `android-shared` failure. **Cause found and fix attempted at the owner's request** (after `f1883495`): the test scrolled to each card before reading its position, so each position was measured at a different scroll offset. Positions are now read in one pass without scrolling; the order checks are unchanged and now carry diagnostic messages. **Verified: passes in CI at `be7029f7`.** Fixed, not disabled or weakened. |
| Readiness card (`PlanFeasibilityCard`) has no instrumented or screenshot coverage | Test gap | Existing Plan-screen tests use a general-fitness plan, which shows no card. Add a race-plan UI test and screenshots in 5B. |
| Recovery segments (`WorkoutSegment.Kind.RECOVERY`, duration only) | Unverified on device | The execution engine supports duration-only steps (`WorkoutExecutionEngine`, step completion by `millis`), but live recording, cues, watch/notification display and post-run analysis of recoveries have **not** been exercised on a device or emulator. |
| Short running days and quality sessions | Product limitation | Days under about 5.6 km (threshold with a 1 km warm-up) cannot hold a session; they get strides. High-frequency, low-volume runners get no structured session. |
| Rebuild/progress UI copy | Unreviewed | The text goes through the existing generic review UI and has not been reviewed on screen or with large fonts. |
| `.claude/worktrees/` | Local clutter, not in git | Old agent worktrees exist in the original Claude container only. They are untracked and do not affect the repository. |

There are no other known failing tests. No signing, release, workflow or version files were changed in
Pieces 1–5A.

## 8. Deferred to Piece 5B

- Full test suites:
  - full `:sharedDomain:jvmTest`, `:domain:domainTest` and Android unit tests;
  - the Android instrumented matrix on **API 28, 35 and 36**;
  - **iOS** shared tests and the XCTest/UI suites, and an iPhone device build if available.
- **Screenshots:**
  - regenerate the required set (519);
  - capture the Plan screen with the readiness card (Limited and Not supported);
  - capture the weekly review with rebuild and progress previews;
  - capture Piece 4 workouts with repetitions and recoveries;
  - capture the Strength dashboard.
- **Multi-runner scenarios**, each generating the plan and then simulating 4–8 weeks of recorded
  runs:
  - beginner;
  - established 5K and 10K;
  - half marathon;
  - marathon;
  - returning;
  - heavy lower-body strength;
  - general fitness;
  - Zone 2;
  - Performance;
  - missing sync;
  - partial data;
  - recurring missed day;
  - over-achiever.

  For each, check that weekly reviews offer only the expected choices.
- **Live-workout interactions:**
  - starting a Piece 4 session;
  - warm-up, repetitions and duration-only recoveries;
  - cues, pausing, and ending early;
  - linking the recording to the planned workout;
  - the run appearing as evidence in the next weekly review;
  - Health Connect and HealthKit imports, with duplicates counted once.
- **Before/after programme comparisons** across distances (main against PR #51).
- Optional wording change: "Top set" → "Heaviest set". (The Strength dashboard test is fixed and passes.)

## 9. Independent full-app audit (5B)

Audit the whole app, not only the training programme:

1. **Data integrity and upgrades:**
   - install `main` (0.15.5-test1 / 28) with real-looking data — plans with history, completed and
     skipped workouts, reviews and decisions, gym sessions and programmes, bouldering, Dream Routes;
   - upgrade to the PR #51 build;
   - confirm nothing is lost or altered: existing plan versions, completed sessions and decisions;
   - confirm a stored review created before 5A still renders and resolves;
   - confirm backup/restore (portability) round-trips the new review choices.
2. **Plan regeneration:** editing a goal or routine regenerates with Pieces 1–4 and keeps history.
3. **Running features:** recording, live workouts, imports, run scores, race predictions, Training
   Trends, Progress.
4. **Gym and bouldering features:** Strength Today, Gym Now, programmes, the exercise library,
   strength progression charts, the dashboard order (Piece 2), bouldering logging and analytics.
5. **UI consistency:**
   - Material 3 / Material You, the bottom navigation icons, light and dark themes, large fonts and
     TalkBack/VoiceOver;
   - the readiness card;
   - the weekly review previews;
   - new workout and segment text, on Android and iOS.
6. **Platform integrations:** Health Connect and HealthKit permissions and imports, notifications and
   reminders, app updates (signing certificate `e7af758a…929d` must stay unchanged).
7. **Performance:** no new background work (5A adds none), and snapshot read time with long histories.

## 10. Acceptance criteria before PR #51 may be merged

All must hold, and the owner must then explicitly approve the merge:

1. All mandatory CI is green on the final head, **including** `SharedDashboardScreenTest`, which must
   be fixed, not disabled. No required check is bypassed.
2. The Android instrumented tests pass on API 28, 35 and 36; iOS shared and UI tests pass.
3. The required screenshots are regenerated and reviewed, and the counts match the scripts.
4. The multi-runner scenarios (section 8) behave as documented in section 4, with no catch-up volume,
   no intensity increase from progression, and no change without acceptance.
5. Upgrade from `main` preserves all user data, plan history and decisions; backup/restore works.
6. Live-workout execution of Piece 4 sessions, including duration-only recoveries, is verified on at
   least one Android device or emulator and the iOS simulator.
7. Every production snapshot change since `main` has been reviewed and is intended (see
   `docs/TRAINING_PROGRAMME_PLAN.md`).
8. The release version is bumped once, and the signing identity, package identity and update metadata
   are unchanged and verified. Nothing is published automatically as stable.
9. `docs/TRAINING_PROGRAMME_PLAN.md`, this file and `CONTINUE_CODEX.md` reflect the final state.

## Final commit and CI

| Commit | Content | CI |
| --- | --- | --- |
| `cd20f242` | Piece 5A implementation and handover | Android coaching test build: **passed**. `android-shared`: **failed only on the known** `SharedDashboardScreenTest` (1 failure; all other instrumented tests passed). `apple-shared`: **failed**, because the new test called `LocalDate.minusWeeks`, which is JVM-only (iOS test compile error, 3 lines). Main code compiled for iOS. `apple-device`: skipped by design. |
| `f1883495` | Test fix: `minusWeeks(n)` → `minusDays(7n)`; CI record | Superseded by `be7029f7` before its own CI was read. |
| `be7029f7` (**final 5A code head**) | Strength dashboard test fix (owner request) | **All green on 24 November 2026:** Android coaching test build passed; `android-shared` passed, **including `SharedDashboardScreenTest`**; `apple-shared` passed; `apple-device` skipped by design. |
| The docs-only commit that adds this row (PR #51 head when 5A stopped) | Records the CI above | Docs only; check the PR #51 checks. |

Lesson for 5B: `org.racedaydreams.domain.time.LocalDate` exposes only `getYear`, `getMonthValue`,
`getDayOfMonth`, `toEpochDay`, `plusDays`, `plusWeeks`, `minusDays` and `getDayOfWeek` in common code.
JVM tests compile against `java.time` and will not catch other calls; only the iOS compile does.


## Piece 5B validation checkpoint

- Started from PR #51 head `0bc2a4a0` and main `60b5201b`. GitHub's exact-head
  [draft run](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45473777369) passed
  Android shared and Apple shared; Apple device was skipped for the draft.
- The 5A week-five return was reproduced from 13 km of recorded weekly running and
  26 km of planned weekly running. Accepting the rebuild now caps the later week
  and each distance-based run by the final rebuilt week's distance and longest run.
  Dates, past sessions and the race remain unchanged. Keeping the original plan
  still applies nothing. The continuation is included in the approval preview.
- Timed recoveries stay duration-only. The estimated recovery contribution to the
  workout total is retained when scaling, and 90-second recoveries display as
  1 min 30 sec in the workout detail. Added focused tests for both corrections.
- Five canonical captures are required in addition to the existing 519: Limited
  and Not supported readiness, a timed recovery, and the rebuild and progress previews.
- Local verification includes 11 adaptive block tests, four successive reviews for each of six
  generated runner profiles, 78 schema tests, all 20 shared/Android schema boundaries, 29
  screenshot-contract checks and 12 release-note tests. The candidate's comparison with the
  pre-change generator is in the programme plan. These results do not replace platform checks.
- The release candidate bumps once to Android `0.16.0-test1` / 29 and iOS `0.16.0` / 29.
  Full matrix, iOS simulator coverage, actual capture review, final upgrade and
  publication verification are still pending. No post-merge audit is claimed here.


### Focused device-gate results (API 28 emulator)

- In-place install from the published main 0.15.5 source to the candidate:
  passed using a temporary emulator key on both copies. The full database
  and appearance fingerprint was identical. Readback included plan versions,
  completed and skipped workouts, weekly reviews and a decision, gym history
  and programme, bouldering, and the Dream Route assignment.
- Timed-recovery step evidence, retained run-to-plan link and complete backup
  round-trip: passed. Dark readiness interaction at 200% font: passed.
- The capture tests initially used Today, the page title, as the bottom-tab
  selector. They now use Home, the actual tab label, then await Today. Both
  passed and the five original captures were inspected locally. The full
  canonical capture set remains a release gate on GitHub.


### First full-gate findings (source head `813e6c15`)

- [Android gate](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45482622074):
  API 35 and 36 passed. API 35 upgrade same-key install and signer/downgrade
  rejection passed. All 524 API 35 originals passed integrity and the changed
  screens were reviewed. API 28 instrumentation process stopped without a stack
  during Gym Now at 200%; the failing test and three predecessors passed on the
  local API 28 emulator with the same display size. Only the failed API 28 job
  was retried; the app and existing assertions were unchanged.
- [Apple gate](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45482622072):
  shared Native build/tests, startup and the unsigned device build passed. Four
  existing XCTest cases passed. The new recovery case stopped at a label/type
  mismatch for the Compose time field. The follow-up taps the visible label
  before typing and still requires the timed recovery to advance in the live app.
- Local production-service probe (API 28): paused the timed recovery with
  simulated GPS, resumed, completed both repetitions and saved step evidence
  against the exact plan, all of which passed. Separate upgrade and backup probes
  passed a legacy review decision and retained a rebuild preview.

### Second full-gate findings (head `6340e671`)

- [Android](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45498117903)
  passed 857 tests and 524 capture integrity checks on each API 28, 35 and 36.
- [Apple](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45498117924)
  passed shared Native, startup, unsigned device and the four existing UI cases.
  The recovery test opened the native decimal keypad (digits were visible in
  the failed test's accessibility tree), but `typeText` required a native
  keyboard-focus property that the Compose editor did not expose. Use the
  real digit keys and retain all timed playback and interruption assertions.

### Third full-gate findings (head `eb1bb928`)

- [Android](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45508070892)
  passed 857 tests and 524 captures per API. The new exact-head API 35 captures
  match the five reviewed canonical captures below their status bars.
- [Apple](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45508070662)
  used the keypad, saved the workout and reached Step 1 of 3. The new case could
  not find Complete open step because Workout details / controls was still
  closed. The next candidate opens this disclosure and keeps the original
  playback assertions. The four existing XCTest cases, startup, shared Native
  and unsigned builds all passed.
- A separate 13-profile deterministic probe passed 65 weekly reviews and 324
  assertions. Cases include the missing/partial/duplicate sync and recurring
  missed day cases, return, heavy lower-body work and above-plan Performance.

### Piece 5B merge and post-merge audit

- Tested PR head `205d2918e1be7c8d1abc2030e8de056551dbdb37`. The
  [Android gate](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45559510034)
  passed 857 instrumented tests on each API 28, 35 and 36, with 524 authentic
  captures per API. Canonical source fingerprint is
  `e200244d13df7e525375a78dc4dc3680c17af4b9500e3057fc844dcf7baaba0f`.
- The [Apple gate](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45559510015)
  passed 1,320 shared Native tests (568 domain, 27 health, 683 storage, 42 UI),
  real iOS simulator startup, all five
  XCUITests (including the 30-second no-GPS recovery) and unsigned builds.
- Owner-authorised [PR #51](https://github.com/mmwiza1/Race-day-dreams/pull/51)
  was squash-merged at 07:20 UTC on 25 November 2026 as
  `7823372c81d3fe09c9485ed6de5ee461d546f593`. The merged tree matched the
  verified PR head exactly.
- Independent findings: [running](AUDIT_2026-11_RUNNING_PROGRAMMES.md),
  [gym](AUDIT_2026-11_GYM_PROGRAMMES.md) and [full app](AUDIT_2026-11_FULL_APP.md).
  The audits propose a separate remediation PR and make no application changes.
- [Post-merge CI and rolling publication](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45567338048)
  passed. Android 0.16.0-test1 / 29 with the unchanged pinned signer, update
  manifest and the three unsigned iOS 0.16.0 / 29 artifacts were independently
  verified from the public development assets. See [artifact hashes and checks](AUDIT_2026-11_FULL_APP.md#publication-and-physical-acceptance).
