# Training programme development — five-piece plan and handoff

Owner-approved on 24 November 2026. One integration branch and one draft PR for the whole
programme. Pieces are approval milestones within that branch, done strictly one at a time.

- **Integration branch:** `ccr-60f602a7-tfmok3-training`, created from `main` `60b5201b`.
- **Integration PR:** [#51](https://github.com/mmwiza1/Race-day-dreams/pull/51), squash-merged as
  [`7823372c`](https://github.com/mmwiza1/Race-day-dreams/commit/7823372c81d3fe09c9485ed6de5ee461d546f593)
  after owner approval and green exact-head release checks.
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
| 3 | Running progression engine | Major | **Approved at `f723eeec`** |
| 4 | Race-specific workout engine | Major | **Approved at `fe216621`** (iOS test compile fixed in `35d5b20f`; final CI shows only the known failure) |
| 5A | Adaptive coaching (Claude) | Major | **Implemented and approved for 5B validation on PR #51.** Handover: `docs/HANDOFF_5A_TO_5B.md` |
| 5B | Comprehensive validation and development release preparation | Major | **Merged at `7823372c`.** Final Android matrix, iOS shared/Simulator and visual checks passed. The independent [running](AUDIT_2026-11_RUNNING_PROGRAMMES.md), [gym](AUDIT_2026-11_GYM_PROGRAMMES.md) and [full-app](AUDIT_2026-11_FULL_APP.md) audits follow the merge. |

## Testing policy (Pieces 1–4)

Focused shared-domain tests for the changed logic, minimal regression tests for confirmed defects and
safety-critical behaviour, compile checks where a change could break compilation. No full suites,
emulator or simulator runs, screenshots or full platform builds unless one is needed to verify a
specific change. Never defer safety, compilation or data-integrity checks. GitHub Actions still runs
on each push; keep pushes to a minimum and trigger nothing manually.

## Piece 5B — merged development release

The runner previews and explicitly accepts the four-week rebuild and its continuation. Subsequent
weeks and individual distance sessions stay within the last rebuilt week's workload pending fresh
evidence and another decision. This supersedes the original week-five return described during 5A.
Timed recoveries retain their active-time targets and use an estimated distance allowance for budget
reconciliation. See the [5B checkpoint](HANDOFF_5A_TO_5B.md#piece-5b-validation-checkpoint)
for verification status.

### Candidate comparison against the previous generator

These deterministic inputs use the same availability and recent baseline on both versions.
Peak weeks exclude the event. They test behaviour and explain changed snapshots; they do not
predict race results or prescribe a plan for an individual.

| Fixture | Previous peak week / longest | 5B peak week / longest |
| --- | --- | --- |
| 5K with no recorded base | No distance target; up to 23 min | No distance target; up to 27 min |
| Established 5K, 28 km/wk, three days | 21.0 / 7.0 km | 30.0 / 10.0 km |
| 10K, 24 km/wk, two days | 22.0 / 11.0 km | 28.0 / 14.0 km |
| 10K, 32 km/wk, four days | 32.0 / 11.0 km | 41.6 / 14.0 km |
| Half, 19.6 km/wk, Monday/Thursday/Friday | 30.7 / 14.7 km | 34.0 / 17.0 km |
| Marathon, 45 km/wk, four days | 60.1 / 27.1 km | 58.7 / 29.3 km |

In the fixed-weekday half-marathon case, week 23 becomes a 17.0 km long run, 8.5 km easy run
and 8.5 km session including half-marathon effort. The candidate does not derive a pace from the
two-hour target alone. In the flexible-weekday counterpart the long run moves when recent
demanding work would make Monday a poor fit.

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
- **General fitness:** maintains by default; see the corrections below.
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

### Piece 3 corrections (owner review)

- **Long-run shares are guidance, not cuts.**
  - The plan starts from the runner's credible recent long run (up to 70% of their week). Each week's
    long run is the larger of the share-guided long run and the one they already do (scaled in the
    taper), always within the 30-day guide, the distance ceiling, and a 70%-of-week recovery safeguard.
  - Example: 30 km a week, an 18 km long run and 4 days keeps 18 km, where it was previously cut to
    13.5 km.
  - A long run kept in the taper is at most 70% of the last full one.
- **General fitness maintains by default.** Weekly volume and the long run stay at the runner's level.
  Gentle progression happens only if they chose the existing Distance First approach, so no new setting
  was added. General-fitness run/walk without Distance First is unchanged from before Piece 3.
- **Restored foundation safeguard.** While a race is more than 28 weeks away, weekly volume stays within
  6 km and the long run within 3 km of the start. This is the existing `WeeklyCoachingTestSuite` rule,
  which Piece 3 had weakened.
- **Experienced marathoners:** the busiest-week target may reach about 130% of the runner's own volume
  when that is above the reference.
- **Production snapshots** (`domain/src/test/resources/coaching/*.txt`) were reviewed plan by plan before
  being updated. 11 changed and the general-fitness run/walk one is identical. Every change is intended:
  - higher, gradual half-marathon builds, peaking at 18 km;
  - 10K long runs up to 14 km instead of 11 km;
  - marathon long runs of 29.3 km three weeks out, then 20.5 km two weeks out;
  - beginner run/walk longer sessions of up to 30 minutes, previously 25;
  - a 3-week marathon keeping a short taper long run.

  Accepted trade-off: a runner dropping from 4 to 3 days whose credible 10 km long run is held
  (`three-days`) gets short other runs, so one early quality session is dropped; quality resumes from
  week 5.
- **Focused checks:**
  - `:domain:domainTest` passes; it was not in the original Piece 3 run, and is now part of the focused
    set for generator changes.
  - 92 shared-domain tests pass, including the new `aCredibleExistingLongRunIsNotCutByTheShareGuide`
    and `generalFitnessMaintainsUnlessDistanceFirstWasChosen`.

## Piece 4 — what changed

New `QualityWorkouts` (shared domain) builds structured sessions from the existing segment model:
warm-up, work repetitions separated by `RECOVERY` segments (time only), and cool-down. The generator
chooses and doses them; the Piece 1 scheduling and Piece 3 volume rules are unchanged, and every
session fits inside the day's planned distance, so weekly totals are identical to Piece 3.

- **Sessions by distance and phase.** Base and endurance phases use threshold first. Race-specific and
  peak phases use the distance's own session two weeks in three, with threshold in the third:

  | Session | Shape (smallest–usual–Performance) | Used for |
  | --- | --- | --- |
  | Threshold | 2–3–5 × 1.6 km, 2 min jog | All distances |
  | 5K intervals | 4–5–7 × 800 m at 5K effort, 2 min jog | 5K |
  | 10K intervals | 4–5–7 × 1 km at 10K effort, 90 s jog | 10K |
  | Half-marathon pace | 2–3–4 × 2 km, 2 min jog | Half marathon |
  | Marathon pace | 5–8–14 km steady block inside an easy run | Marathon only |

  5K and 10K plans never get a continuous near-race-effort block: hard pieces are at most 1 km at race
  effort or 1.6 km at threshold. If a distance-specific session does not fit the day, threshold is
  tried; if neither fits, the day stays easy with strides.
- **Intensity from evidence, not goals.** Pace guides come from the closest recent 5K, 10K or half
  result (Riegel estimate, threshold ≈ one-hour pace), shown as a range with "effort comes first".
  Without a result, sessions are prescribed by effort and say so; a goal time is never used for paces.
- **Graded readiness, reassessed at every generation or rebuild** (replaces the single 20 km/week
  switch), from actual recorded or entered evidence only. Planned future runs never count:
  - none — no usable history, Zone 2 or general fitness;
  - strides — established but light or short-lived running; the reason explains when sessions unlock
    (about 15 km a week over 6 consistent weeks on 3+ days) and to rebuild the plan then;
  - controlled — one session a week, usual doses;
  - Performance — more repetitions, only where the existing Performance gate supports it.
- **Returning runners** (a recorded gap of 14+ days ending in the last six weeks): easy running only
  for three weeks, and no strides in the first week.
- **Recovery and strength.** At most one session a week, on the Piece 1 quality day (never next to the
  long run). A session on or the day after a hard lower-body strength day becomes the smallest
  threshold version.
- **Taper.** Outside race week, a shorter version of the last familiar session; race week has strides
  only, at least two days before the race.
- **Explanations** name the session and its purpose in plain words, and state that every other run
  stays easy.

### Piece 4 checks

- New `QualityWorkoutEngineTest` (8 tests): sessions and recoveries by distance; no continuous
  near-race blocks for 5K/10K (Balanced and Performance); paces from results, never from a goal time;
  graded readiness including the strides fallback; returning runners; one session a week away from
  the long run with hard running under 12% of plan distance; taper and race week; session dosing and
  fit.
- Focused shared-domain set: 100 tests, 0 failures. `:domain:domainTest` passes.
- Compile: `:sharedUi`, `:app` and Android test sources. Format check passes.
- **Production snapshots reviewed plan by plan.** 8 changed; beginner, returning (low volume),
  short-horizon and run/walk are unchanged. Weekly and total distances are identical in every
  fixture. Intended changes:
  - 4–6 km "tempo" days with one continuous 2–4 km block now stay easy, often with strides, until the
    day can hold 2 × 1.6 km with a recovery;
  - sessions use repetitions with recoveries; hard running fell (for example `sub2-half` 64 → 40 km
    over 18 weeks, `marathon` 127 → 96 km);
  - `six-days` gains 10K intervals; `sub2-half` gains half-marathon pace;
  - `heavy-lower-body` (30 km over 6 days of 3.6–4 km runs): strides only;
  - the taper keeps a shorter familiar session; race week has strides only.

## Piece 5A — what changed

Adaptive coaching extends the existing weekly review (`WeeklyCoaching.review`) with a new
`BlockReassessment`. There is no new service, storage, screen or background work.

- **Evidence.** The last four weeks of recorded runs, with duplicates counted once and only before
  the review week. Planned future workouts never count. Weeks without recorded runs are unknown, not
  rest. With no runs recorded at all, the review suggests checking sync or recording a break, and
  changes nothing.
- **Rebuild** (runner-approved). When recorded running is below 75% of the next two planned weeks,
  the next four weeks are capped at the recorded average, rising about 5% a week. Missed sessions are
  never made up. Not offered within 14 days of the race, or when the misses are a recurring
  unavailable day; that case points to editing the runner's days instead.
- **Progress** (runner-approved). Only after four fully recorded weeks with every planned run
  completed and at least 115% of planned volume. The next two weeks' easy and long runs grow by at most
  10%. Quality sessions and intensity are unchanged. Never for general fitness without Distance First,
  and not within 35 days of the race.
- **Priority.** Strain (hold or reduce) and a break (return bridge) override both changes.
- **Realism.** If fewer than half the planned runs were recorded within 8 weeks of the race, the
  review explains that the race or target may no longer be supported. A rebuild that lowers the
  readiness rating is explained.
- **Structure kept.** Hold, reduce and rebuild now resize only easy parts. Piece 4 repetitions and
  recoveries stay intact, or the day becomes an easy run.
- **Unchanged.** Acceptance, history and versioning work as before: preview, accept or keep, one
  decision per review, completed sessions locked, and a new plan version on accept.
- **Tests.** New `AdaptiveBlockReviewTest` (8). The focused shared-domain set has 118 tests and 0
  failures, and `:domain:domainTest` passes. No production snapshot changed.

See `docs/HANDOFF_5A_TO_5B.md` for the full rules, limitations and the Piece 5B plan.

## Known failing tests (tracked separately from new regressions)

| Test | First seen | Status |
| --- | --- | --- |
| `SharedDashboardScreenTest.strengthDashboardLeadsWithProgressAndAccomplishmentsNotTonnage` (Android, draft gate) | Piece 2, `f9318590` | **Fix attempted at the owner's request** (test measured positions at different scroll offsets; see `docs/HANDOFF_5A_TO_5B.md`, section 7). **Fixed: passes in CI at `be7029f7`.** Any other failing test is a new regression and must be reported separately. |

## Work handed to Piece 5B (historical; see its completed verification above)

- Full shared-domain suite, Android API 28/35/36 matrix, iOS compilation and tests. iOS was not
  compiled locally; the changes are in shared code only.
- An instrumented test and screenshots of `PlanFeasibilityCard`. Existing Plan-screen tests use a
  general-fitness plan, which shows no card.
- Before/after programme comparisons across distances.
- Piece 4: compile and run `QualityWorkoutEngineTest` on iOS; review generated plans on device;
  consider whether runners with many short days should get a longer quality day (today they get
  strides only); recovery-jog distance is an estimate (0.18 km a minute).
- Optional wording change "Top set" → "Heaviest set" (the dashboard test above is fixed).

## Known limitations after Piece 1

- **Plan shape:** progression rates, the long-run share, the 5K/10K caps, cutbacks and taper are
  addressed in Piece 3 (see above).
- **Feasibility is shown on the Plan screen only.** It is not on Home or during onboarding.
