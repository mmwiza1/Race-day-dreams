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
| 2 | Strength dashboard usability | Small–medium | **Implemented (draft PR #51); awaiting owner approval** |
| 3 | Running progression engine | Major | Not started |
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

## Deferred to Piece 5

- Full shared-domain suite, Android API 28/35/36 matrix, iOS compilation and tests. iOS was not
  compiled locally; the changes are in shared code only.
- An instrumented test and screenshots of `PlanFeasibilityCard`. Existing Plan-screen tests use a
  general-fitness plan, which shows no card.
- Before/after programme comparisons across distances.

## Known limitations after Piece 1

- **Plan shape is unchanged:** progression rates, the 48% long-run share, the 5K/10K caps, cutbacks
  and taper are Piece 3. Your reported half marathon still peaks at 14.7 km, but is now labelled
  Limited, with reasons.
- **Feasibility is shown on the Plan screen only.** It is not on Home or during onboarding.
