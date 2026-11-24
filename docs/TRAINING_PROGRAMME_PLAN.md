# Training programme development — five-piece plan and handoff

Owner-approved on 24 November 2026. One integration branch and one draft PR for the whole
programme. Pieces are approval milestones within that branch, done strictly one at a time.

- **Integration branch:** `ccr-60f602a7-tfmok3-training`, created from `main` `60b5201b`.
- **Draft PR:** see the "Status" table. Do not merge until Piece 5's comprehensive validation passes
  **and** the owner explicitly approves the merge.
- **Version bump:** once, at final integration (Piece 5). Not per piece.
- **Session rules:** AGENTS.md, "AI development sessions — permanent owner rules". One session, no
  agents, stop after each piece for explicit approval. Documentation PR #50 stays parked.
- **Source reports:** Codex "Running programme review (RDR)" and Claude "Running programme review —
  all distances" (owner copies). Do not repeat that research.

## Status

| Piece | Scope | Class | Status |
| --- | --- | --- | --- |
| 1 | Training safety, correctness and transparency | Medium | **Approved at `067bf228`** |
| 2 | Strength dashboard usability | Small–medium | **Approved at `f9318590`** (one known UI-test failure deferred, see below) |
| 3 | Running progression engine | Major | **Implemented (draft PR #51); awaiting owner approval** |
| 4 | Race-specific workout engine | Major | Not started |
| 5 | Adaptive coaching and comprehensive validation | Major | Not started |

## Testing policy (Pieces 1–4)

Focused shared-domain tests for the changed logic, minimal regression tests for confirmed defects and
safety-critical behaviour, compile checks where a change could break compilation. No full suites,
emulator or simulator runs, screenshots or full platform builds unless one is needed to verify a
specific change. Never defer safety, compilation or data-integrity checks. GitHub Actions still runs
on each push; keep pushes to a minimum and trigger nothing manually.

## Piece 1 — what changed

- **Long-run demotion and lost kilometres (Codex finding, confirmed).** `OriginalTrainingPlanGenerator`
  now settles each day's role before allocating distance. A long run that would follow a demanding
  run (including Sunday quality before a Monday long run) moves to another available, well-spaced day
  in the week. If none exists, the week has no long run and its whole budget is shared by the other
  runs. Previously the long run was demoted but its kilometres were still subtracted, so they
  disappeared every affected week. Reproduced with Monday/Tuesday/Sunday running days and a Monday
  long run.
- **Budget reconciliation.** Planned runs are compared with the week's budget. Where single-run limits
  or the session budget make the week shorter, every workout in that week explains the shortfall;
  nothing is added elsewhere. (The limits themselves are reviewed in Piece 3.)
- **Usual session budget.** It was only applied to run/walk durations. With a pace estimate in the
  baseline, it now limits everyday runs; the long run is exempt by design and its estimated duration
  is explained. Without a pace estimate, behaviour and wording are unchanged.
- **Race readiness.** New `PlanFeasibility` rates each race plan Supported, Limited or Not
  supported. After the owner's correction, several factors must agree before a plan is Supported, and
  each factor can only lower the rating:
  - endurance: the longest run, long runs repeated near the peak (one long run is not enough) and the
    busiest week;
  - time until race day compared with the usual build for the distance;
  - running days (two days for a 10K-plus race lowers the rating);
  - run/walk-only plans, which never rate Supported, because session minutes are time on feet, not
    continuous running;
  - for a target time, a recent race result that supports it (a Riegel-style estimate). Without one,
    the plan is rated for finishing and the target is called unproven.

  Alternatives are offered for each shortfall: add history, choose a later race, build towards a
  shorter race first, add a running day, run a shorter race or switch to a completion goal, or run/walk
  with professional advice. Every rating carries a "guide, not a guarantee" caveat. The plan itself is
  never made more aggressive.
- **Plan screen.** New `PlanFeasibilityCard` on the shared Plan screen (Android and iOS), shown for
  Limited and Not-supported plans only. It shows the headline and first reason, with an expander for
  the other reasons, options, the caveat and "Review goal and routine".
- **Moved long runs.** `OriginalTrainingPlanGenerator.relocatedLongDay` picks only from the week's
  scheduled, available days. It keeps at least two days from the previous demanding run and the week's
  quality session, and applies the scheduler's soft recovery costs for hard lower-body strength days and
  the day after them. Among equally suitable days it picks the latest; if no day is suitable, the week
  has no long run.
- **Reference values** (in `PlanFeasibility`; contextual, not safety thresholds), for 5K / 10K / half /
  marathon:
  - longest run for Supported: 4, 8, 15 and 26 km; minimum: 0, 5, 10 and 20 km;
  - usual busiest week: 12, 20, 28 and 40 km;
  - run/walk minimum: 0, 20, 60 and 120 minutes;
  - usual build: 6, 8, 12 and 16 weeks.

## Piece 1 — tests

- **`TrainingPlanSafetyTest`** (shared domain, 10 tests) covers:
  - the moved long run and cross-week spacing, and the relocation choice (available days, recovery
    spacing, hard leg days);
  - budget reconciliation, with the reported half-marathon week 23 unchanged;
  - a beginner marathon rated Not supported;
  - ratings across distances, including the not-a-guarantee caveat;
  - short builds and two running days lowering the rating;
  - run/walk time not treated as continuous running;
  - one long run not being enough evidence;
  - target-time evidence;
  - the session budget.
- **Focused run:** `:sharedDomain:jvmTest` filtered to coaching, plan, adaptive, weekly and safety
  tests: 84 tests, 0 failures. `:sharedUi:compileDebugKotlinAndroid`, `:app:compileDebugKotlin`,
  `:app:compileDebugAndroidTestKotlin` and the format check pass.
- **First CI run on PR #51:** the iOS `apple-shared` job failed because the test used `toSortedMap()`,
  which is JVM-only. The test has been fixed and the shared files scanned for other JVM-only APIs.
  Kotlin/Native cannot be compiled locally, so iOS is checked by CI.

## Piece 2 — what changed

- **No tonnage headline.** The training-volume card (weekly load × repetitions chart, e.g. "1,722 kg")
  is no longer shown on the Strength tab or on Progress → Strength. Recent workouts no longer show a
  per-workout tonnage figure.
- **Owner clarification: no tonnage anywhere on the default dashboards.** Removed:
  - the four-week comparison (e.g. "4 weeks: 1,722 kg");
  - the per-workout totals;
  - the "Training volume (load × reps)" labels;
  - "By exercise" (Back Squat 930 kg, and so on) and its comparison bars.

  The volume card and its weekly-bar helper are deleted from the UI. The load × reps data and
  calculations stay in `DashboardAnalytics` (`loadVolumeKg`, volume by week and by exercise) for
  possible advanced analytics. The only remaining tonnage display is the separate Strength analytics
  screen (More → Records & trends), which is not part of the default dashboard.
- **Recent workouts** now show each workout's heaviest actual working set: "Top set: Back Squat ·
  100 kg × 5", in the user's mass unit. This uses the new `DashboardAnalytics.RecentWorkout.topSet`,
  with warm-ups excluded.
- **New order** on both screens, each answering a simple question:
  1. Exercise progression (am I improving?): heaviest set and estimated 1RM per exercise.
  2. At a glance (what did I accomplish?): Workouts, Working sets, New records (opens personal
     records), Programme.
  3. Recent workouts (what did I do?): exercises, date and working sets; each opens the workout.
  4. Muscle-group workload (where did the work go?): working sets per muscle group from the
     existing exercise classification, with unassigned and secondary involvement kept separate.
  5. Bouldering (for climbers) and More (records, analytics, milestones, recovery), unchanged.
- **Retained:** workout history, all domain calculations, including `DashboardAnalytics` volume by week
  and by exercise, programmes, exercise library, gym tracking, bouldering, navigation icons and Dream
  Routes. The `TrainingVolumeCard` composable is kept, unused, for optional advanced analytics later.
- **Tests and screenshots:**
  - `SharedDashboardScreenTest` now asserts no volume chart and no tonnage.
  - The volume-week test is replaced by an order test: progression, then figures, then recent
    workouts, then muscle groups.
  - The strength-focus test now expects no "Training volume".
  - The three `strength-volume-*` captures are removed from `CoachingScreenshotTest`, the screenshot
    verifier and its test (required captures 522 → 519), and their stale images are deleted.

## Piece 3 — what changed

All changes are in `OriginalTrainingPlanGenerator`. They take effect only when a plan is generated or
rebuilt; saved plan versions and training history are untouched.

- **An individual busiest-week target, replacing the fixed slope.**
  - It starts from a reference for the distance (5K 28, 10K 35, half 40, marathon 55 km), scaled by
    running days (2: ×0.7, 3: ×0.85, 4: ×1.0, 5+: ×1.15).
  - It is capped at about +80% (or +10 km) of the runner's own starting volume, and is never below it.
  - Weeks grow from the *current* level by about 7% (0.5–3 km a week), half as fast while the race is
    still more than 28 weeks away. Once the target is reached the week is held, not pushed further.
- **A separate long-run track.**
  - Each long run is at most about 10% (and 1.5 km) beyond the longest run of the previous 30 days,
    using the Frandsen 2025 risk signal as a guide.
  - The long run's share of the week depends on running days, replacing the universal 48%: 2 days 60%,
    3 days 50%, 4 days 45%, 5+ days 40%, plus 5 points for the marathon.
  - The usual ceilings are 5K 10, 10K 14, half 19 (21 from 40 km a week) and marathon 32 km. A ceiling
    is never below the runner's own longest run.
- **No 5K/10K caps on established runners.** Everyday runs are capped only at that week's long run.
  Established runners keep their tolerated long runs and volume: 50 km a week with 20 km long runs
  stays 50 / 20, where it was previously cut to 35 / 7.
- **Cutbacks are relative to the current week.** Every 4th week is about 80% of the current level, and
  its long run about 80% of the last full long run. A cutback never stacks with the taper.
- **The taper follows the race date and distance.**
  - Taper length is 7 days for a 5K, 10 for a 10K, 14 for a half, and 14 or 21 for a marathon
    (21 from 45 km a week).
  - Taper weeks run at 75% (85% three weeks out for a marathon). The race week is at 50% for a half
    or marathon and 60% for a 5K or 10K.
  - For a half or marathon, a shorter long run is kept until 10 days out, and a moved long run follows
    the same rule.
- **Beginners (run/walk).** Everyday sessions stay at 30 minutes or less. The weekly longer session
  grows by about 3 minutes a fortnight towards a ceiling for the distance (5K 35, 10K 50, half 70,
  marathon 90, general fitness 40 minutes), and is shortened in the taper. The runner's usual time
  budget, when set, still caps every session.
- **General fitness** progresses gently, by at most about 30% and to no more than 30 km a week, and
  never below the current volume.
- **Before and after, for the reported half marathon** (19.6 km/week, 3 runs, 25 weeks): the busiest
  week goes from 30.7 to 34 km and the longest run from 14.7 to 17 km. A 12.8 km long run is now
  kept 13 days out (previously no long run in the last 20 days), and the rating goes from Limited to
  Supported. With the 2-hour target and no recent race time, it is still Limited, for the target only.
- **Focused tests:**
  - New `RunningProgressionTest` (7 tests) covers:
    - the reported half: gradual build, usual long-run range and taper shape;
    - the 30-day long-run guide for a half and a marathon;
    - 5K/10K established runners keeping their volume and long runs;
    - cutbacks that follow the current week and don't stack with the taper;
    - taper length by distance;
    - beginner run/walk progression and time budget;
    - gentle general-fitness progression.
  - Updated, because the behaviour deliberately changed:
    - `TrainingPlanSafetyTest`: the reported week is now 17 / 8.5 / 8.5, the 5K is uncapped,
      beginners' longer session can exceed 30 minutes, and the reported target-time rating is updated.
    - `RunningPlanGenerationContractTest`: everyday run/walk sessions ≤ 30 minutes, the weekly longer
      one ≤ 35 minutes for a 5K.
  - Focused run: 91 tests, 0 failures. Android app, shared UI and Android test compilation and the
    format check pass.

## Known failing tests (tracked separately from new regressions)

| Test | First seen | Status |
| --- | --- | --- |
| `SharedDashboardScreenTest.strengthDashboardLeadsWithProgressAndAccomplishmentsNotTonnage` (Android, draft gate) | Piece 2, `f9318590` | **Known, deferred to Piece 5 by the owner.** Do not investigate, fix, disable or weaken it before Piece 5. Any other failing test is a new regression and must be reported separately. |

## Deferred to Piece 5

- Full shared-domain suite, Android API 28/35/36 matrix, iOS compilation and tests. iOS was not
  compiled locally; the changes are in shared code only.
- An instrumented test and screenshots of `PlanFeasibilityCard`. Existing Plan-screen tests use a
  general-fitness plan, which shows no card.
- Before/after programme comparisons across distances.
- Fix the known failing dashboard test above, and the optional wording change "Top set" → "Heaviest set".

## Known limitations after Piece 1

- **Plan shape:** progression rates, the long-run share, the 5K/10K caps, cutbacks and taper are
  addressed in Piece 3 (see above).
- **Feasibility is shown on the Plan screen only.** It is not on Home or during onboarding.
