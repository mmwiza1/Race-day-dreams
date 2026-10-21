# Training calculations and plan-regression infrastructure

**The app recommends. The runner decides. The plan adapts.**

## What exists

The Android-independent Java domain currently implements target-pace/date
arithmetic, source-aware running-baseline summaries, explicit manual corrections,
assessment summaries, evidence-confidence and Performance-eligibility heuristics,
reproducible splits, rolling best-effort quality checks, PB progression and shoe
statistics. The respective domain tests exercise those calculations without UI.

Target pace is finishing duration divided by exact race distance. For example,
1:59:59 over 21.0975 km rounds to 5:41/km. This arithmetic does not establish
whether that target is realistic.

Baseline confidence describes evidence quality, not a probability of success.
Missing records are not proof of rest and missing metrics are not zero. Easy
pace is not inferred merely because a session had an average pace. A short
assessment alone does not establish long-distance endurance. Manually reviewed
values retain provenance and a review timestamp; reload/restore does not refresh
their apparent age. Performance eligibility is recomputed, not controlled by the
legacy persisted Boolean.

Split and rolling-effort calculations use retained cumulative distance/time
samples, not proportional slices of a session total. Long gaps, pauses, invalid
ordering and implausible sample jumps cannot yield automatically awarded PBs.
Quality thresholds are transparent product checks, not medical or scientific
guarantees. Source totals from Health Connect do not by themselves provide
rolling efforts, arbitrary splits, cadence or moving duration.

## Production training generation remains pending

`TrainingPlanGenerator` is an interface. There is no production implementation
of full-plan generation, adaptive coaching, race prediction, readiness scoring
or taper selection in this increment. Choosing a goal/strategy records runner
intent; it does not silently create training prescriptions. Existing plan and
workout records establish persistence/reminder boundaries for future work.

An original generator needs separately reviewed methodology and explainability.
Do not ship a few unvalidated mileage multipliers as a complete running coach.
Do not copy commercial/book-derived schedule catalogues. Beginner NHS Couch to
5K content remains pending verification of the current published schedule and
reuse/attribution terms. No NHS app code, UI, audio or branding is included.

## Implemented whole-plan regression tooling

`domain.plan.PlanSnapshot` renders every phase, workout and ordered structured
segment in a canonical, human-inspectable format. It sorts unordered phase/run
collections, preserves nullable guidance, escapes delimiters and adds no random
IDs, timestamps or locale-dependent formatting. Its line-numbered diff makes a
changed prescription visible in a failing test and normal Git review.

`domain.plan.PlanRegression` inspects a complete supplied plan against the
runner's availability, known hard lower-body dates and an explicit
`PlanRegressionPolicy`. It reports:

- incomplete/overlapping phases, duplicate IDs/days and empty prescriptions;
- running-day/frequency violations and a race that differs from the runner goal;
- absolute/relative weekly-distance changes, long-run spikes and concentrated
  single-run load;
- hard-session frequency/spacing and proximity to known hard lower-body work;
- missing or insufficiently reduced taper phases in long dated test contexts;
- unknown distance as **unverifiable**, rather than silently treating it as zero.

Gym availability alone is not evidence of a hard lower-body session. Running
and gym training may share a day. Race distance is kept separate from ordinary
training progression checks. Short/partial weeks are not compared as complete
weekly load, so a race tomorrow does not create a false full-week comparison.

Acceptance budgets belong to the test context. They are not a universal
ten-percent mileage rule, a safety score or clinical advice. A future generator
must justify its policies and add validation for the decisions it actually makes.

## Snapshot fixtures and honest scope

`domain/src/test/resources/plan-snapshots/` contains complete synthetic plans for
five test-only contexts: beginner 5K, sub-2 half marathon, marathon with gym
days, manual aerobic fitness and a returning runner. Additional tests inspect
two/six runs per week, five gym days, race tomorrow, race a year away, ambient
locale/timezone changes and isolated changes to one runner fixture.

The fixtures are deliberately constructed **test data**, not generated coaching
plans or an NHS programme. Every fixture prescription is labelled accordingly.
Mutation tests prove that the checks detect spikes, broken tapers, consecutive
hard days, ignored hard-gym context and impossible scheduling. These tests verify
the regression infrastructure; they do not claim that a production plan engine
exists or that a training methodology has been validated.

Run the complete offline domain suite with `sh scripts/test-domain.sh`, or
Gradle `:domain:check`. To deliberately regenerate the five golden snapshots
after inspecting a fixture change, run `PlanRegressionTestSuite` with
`--write-snapshots domain/src/test/resources/plan-snapshots` using the test
classpath. Normal tests never overwrite expected snapshots. Review the entire
snapshot diff before accepting it.

## Rules for future engines

- Keep decisions deterministic and independent of Android UI/provider types.
- Explain significant recommendations and offer alternatives.
- Preserve planned versus completed data and the runner's original goal.
- Treat missed or changed sessions as a changed week, without punishment.
- Consider running, gym, consistency and available recovery context together.
- Keep useful guidance available without HR, sleep or Health Connect.
- Use multiple signals for performance estimates and display ranges/limitations.
- Require a runner decision before changing a goal or accepting an adjustment.
- Never prevent a runner from choosing to run anyway.
- Do not diagnose, promise injury prevention, or market deterministic rules as AI.
