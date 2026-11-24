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
| 1 | Training safety, correctness and transparency | Medium | **Implemented; awaiting owner approval** |
| 2 | Strength dashboard usability | Small–medium | Not started (needs approval) |
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
- **Race readiness.** New `PlanFeasibility` (shared domain) assesses each race plan against what plans
  for the distance usually build to: Supported, Limited or Not supported, with reasons (missing history,
  short build, few running days, target time) and alternatives (add history, later race, shorter race
  first, extra running day, completion goal, run/walk and professional advice). A beginner marathon or
  half with 30-minute run/walk sessions is reported as **Not supported**. The plan itself is never made
  more aggressive.
- **Plan screen.** New `PlanFeasibilityCard` on the shared Plan screen (Android and iOS) for Limited and
  Not-supported plans only: headline, first reason, and an expander with the other reasons, options and
  a "Review goal and routine" button.
- **Reference values** (in `PlanFeasibility`, contextual, not safety thresholds): longest planned run
  for "supported" 5K 4 km, 10K 8 km, half 15 km, marathon 26 km; run/walk minutes 20, 60, 120, 180; usual
  build 6, 8, 12, 16 weeks.

## Piece 1 — tests

- **New:** `sharedDomain/src/commonTest/.../TrainingPlanSafetyTest.kt` (5 tests). It covers:
  - the demotion fix and cross-week spacing;
  - budget reconciliation, with the reported half-marathon week 23 unchanged;
  - a beginner marathon reported as Not supported;
  - the feasibility levels and reasons across distances;
  - the session budget.
- **Focused run:** `:sharedDomain:jvmTest` filtered to coaching, plan, adaptive, weekly and safety
  tests: 79 tests, 0 failures. `:sharedUi:compileDebugKotlinAndroid`, `:app:compileDebugKotlin`,
  `:app:compileDebugAndroidTestKotlin` and the format check pass.

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
