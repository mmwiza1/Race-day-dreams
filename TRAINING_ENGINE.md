# Original coaching engine — 0.4.0-test1

The app recommends. The runner decides. The plan adapts.

## Implemented

`OriginalTrainingPlanGenerator` implements the Android-independent
`TrainingPlanGenerator` interface. No schedule catalogue, commercial training
plan, LLM or online service is used. Given the same `PlanRequest`, the complete
programme, IDs, dates, prescriptions and explanations are deterministic.

Inputs are the saved goal/routine, provenance-aware baseline, start date,
explicit unavailable dates and recorded strength sessions. The baseline already
combines recorded history with optional manual gap filling. A recent assessment
contributes observed training evidence; it does not prove endurance for a longer
race. No race goal or recorded activity is changed by generation.

Supported goals: 5K, 10K, half marathon, marathon and general aerobic fitness;
Finish, Target Time and Maximum Performance remain runner-owned choices.
Without a dated event the engine generates a clearly projected 12-week rolling
programme. A dated event uses its actual horizon, including multi-year events. Past dates
and explicitly blocked one-off race dates are rejected with an actionable message,
leaving the existing plan intact.

### Workload rules (product heuristics, not scientific safety thresholds)

- Missing, low or more-than-21-day-old workload evidence starts with 15-minute
  conversational run/walk sessions, progressing toward 30 minutes, at at most
  three sessions/week. Walk breaks remain optional. This is an original simple
  progression, **not** an implementation of NHS Couch to 5K.
- Established starting volume uses recent weekly mileage and longest run.
  Reducing available frequency reduces the starting weekly budget. Partial
  weeks and unavailable days reduce training; missing mileage is never packed
  into the remaining sessions.
- Build increments are absolute, bounded context heuristics (0.5–2 km per build
  week based on starting volume), with an easier fourth week. There is no
  universal 10% rule. Session distance is also bounded by recent long-run
  evidence plus modest projected development. Race-distance caps limit long
  runs and projected weekly volume; they are not mandatory achievements.
- Quality requires established volume, at least six consistent weeks and three
  available sessions. At most one controlled quality session is prescribed in
  a week. Zone 2/general aerobic plans contain no unnecessary quality work.
  Distance First delays quality until the race-specific phase. Performance
  uses the existing evidence-derived eligibility evaluator and a higher intended
  controlled quality effort; losing eligibility never deletes the stored preference.
- Foundation/build/specific/peak/taper phases are allocated from actual dates
  and baseline. Established short horizons omit unnecessary foundation. Taper
  windows vary by distance and workload (6 days for 5/10K, 10 for HM, 14–21 for
  marathon, clipped to available time), with reduced volume and conversational
  sessions. Advanced intensity-retaining tapers are still pending.
- A short marathon/HM horizon with limited endurance produces a clear caution
  and conservative development, never an accelerated catch-up programme.
- Pace is optional. Race-specific segments may show the runner's target pace;
  controlled RPE takes precedence. Personalised threshold/interval pace models
  and conservative multi-evidence race predictions remain pending.

### Scheduling

`ConstraintScheduler` enumerates the small weekly search space (at most 128 day
subsets), filters unavailable dates, availability, race proximity and frequency,
then deterministically scores feasible long/quality placements. The race is
reserved on its chosen date and counts toward the weekly maximum. No duplicate
placement or post-race workout is allowed.

Soft costs: missing preferred long-run day +20; omitting an eligible quality
session +80; consecutive running days +2; demanding running the day after
recorded hard lower-body work +120 (same day +90); a preferred gym day before
such running +4. A gym preference is not evidence of actual hard work. Quality
and long runs are spaced by at least two calendar days; cross-week conflicts
fall back to easy running. Each generated workout retains the winning score,
phase and actual workload decision in its explanation.

Choco/Timefold-style hard/soft constraint separation informed the design.
A solver dependency was rejected for this small problem: enumeration is bounded,
requires no additional Android/Java runtime, adds no APK dependency, and is
straightforward to test. Global multi-week optimisation is not claimed.

### One current plan and runner choice

`PlanVersion` retains identity, monotonic per-plan version, creation time,
reason, goal snapshot, evidence summary, phases and original workouts. One
SQLite pointer identifies the authoritative current version. Plans are generated
only on an explicit request. A confirmation is required before rebuilding future
training. Old versions remain immutable, including prescriptions linked to
completed runs. Same-day rebuilds freeze targets already linked to completions;
the database also rejects attempts to overwrite those targets. Restored response
links stay protected while provider records await re-import. A rebuild begins today; earlier completed runs remain attached
to their original versions, not rewritten into the new programme.

Current-week sessions are concrete; the next weeks remain adaptable; distant
weeks are projections. Merely loading an activity does not regenerate the plan.

`AdaptiveCoachingEngine` consumes raw actual distance/RPE, preserved
prescriptions, response links and strength context. **Run Score is not an input.**
The first implementation proposes a change to one upcoming quality workout when:

- at least two linked recent sessions have effort above their intended range;
- a linked run combines under-75% completion with RPE 8+; or
- recorded hard lower-body work falls the day before a quality session.

A single missed easy run, an isolated poor quality session or excellent
completion alone does not trigger restructuring. Options include keep original,
run easy, rest, and a feasible move when available. Moves preserve availability,
maximum frequency and demanding-session spacing. The proposal retains evidence,
confidence, explanation and impact. Accepting changes only the selected workout;
rejecting records the decision and prevents repeating the same evidence proposal.
No lost mileage is redistributed. Changed goals/availability or a prolonged
interruption require the runner to review and explicitly rebuild; automatic
multi-week restructuring is not yet implemented.

## Planned, actual, response, outcome

`PlannedWorkout` is the preserved prescription. `ActivityRecord` is the actual
measured/imported or manually entered result. `RunExecution` records the explicit
link and optional runner-reported rep/recovery information. RPE and notes are
response evidence. Completing a session does not establish an improvement in
fitness; there is no fabricated outcome/readiness/prediction.

## Run Score and feedback

`RunScoreEngine` rule version 2 scores planned **purpose**, not raw speed.
Components are completion (weight 3), effort match (4), and available pace,
pacing/objective, reps and recoveries (2 each). Missing components are excluded,
not zero-filled. Completion alone does not produce a numeric score. The displayed
1–10 score is rounded; detailed component explanations and evidence remain visible.

Easy/Zone 2/long runs favour conversational RPE up to 4; recovery up to 3.
Controlled quality work uses RPE 5–8. Going harder earns no bonus. Distance above
115% begins reducing completion-match credit. Whole-run average pace is compared
only with a whole-run pace prescription; recovery-inclusive interval averages
cannot establish rep pace. Reliable continuous splits can identify late fade or
lack of progression. Missing progression or substantial missing reps caps the
score at 7. Rep/recovery completion is used only when explicitly reported;
provider laps are not automatically assumed to be workout reps.

An unplanned run shows **Run Analysis**, with no adherence score. Feedback
separates what went well, what differed, what was learned and the next step.
Low scores use neutral language. HR is never required. These weights are product
heuristics, not validated physiology. Detailed per-rep pace matching, terrain
normalisation and automatic mapping of imported laps to structured reps remain
pending. Feedback displayed from an imported run is recomputed from current
retained evidence; stored rule-versioned feedback is an historical snapshot.

## Optional strength

Definitions are extensible, not a closed exercise enum. Sessions retain exercise
snapshots and ordered sets (reps, added kg, optional RPE, warm-up flag), time/offset,
duration and notes. The UI supports multiple exercises/custom definitions.
Current UI logs working sets; editing a session/default definition is a storage
boundary rather than a finished editor. Delete/re-log is available.

Classification uses exercise region and recorded RPE. Three or more lower-body
working sets plus session RPE >=7 or a working-set RPE >=8 produces HIGH
lower-body involvement. Fewer/lower-effort lower-body sets produce MODERATE;
upper-only work LOW. Unknown effort remains unknown. This is explainable context,
not an exact fatigue measurement.

Strength PB progression is rebuilt chronologically from retained non-warm-up
sets, with session/set/date/source retained. Categories include heaviest load,
exact 1/3/5/8/10 reps, bodyweight reps and weighted-bodyweight added load. Edits or
deletions cannot leave unsupported PBs. Estimated 1RM uses Epley:
`kg × (1 + reps / 30)` for suitable weighted 2–10-rep sets; a single rep uses
recorded load. UI rounds to whole kg and labels estimates. High-rep, bodyweight,
weighted-bodyweight and opted-out custom lifts have no estimate. This does not
account for reps in reserve or equipment differences and never asks for a max test.

Session-RPE context is `duration in minutes × reported RPE`, with data coverage.
Running and strength are kept separate. No ACWR injury gate, CTL/ATL medical
readiness score or prediction of injury is implemented.

## Evidence and limitations

Public primary research was reviewed for the direction of the design, not copied
into a schedule or presented as validation of our constants:

- [Foster et al. (2001), A new approach to monitoring exercise training](https://pubmed.ncbi.nlm.nih.gov/11708692/): session-RPE monitoring. Our app uses elapsed duration and user-entered session RPE, without claiming protocol-standardised measurement.
- [Foster et al. (1996), Athletic performance in relation to training load](https://pubmed.ncbi.nlm.nih.gov/8693756/): integration of perceived effort and duration; does not establish a universal individual injury threshold.
- [Bosquet et al. (2007), Effects of tapering on performance](https://pubmed.ncbi.nlm.nih.gov/17762369/): reduced volume before competition, with substantial context dependence. Our distance-specific windows are original conservative product choices, not a replication of the studied interventions.
- [LeSuer et al. (1997), accuracy of 1RM prediction equations](https://doi.org/10.1519/00124278-199711000-00001): equation estimates differ by lift/population; Epley's conventional arithmetic is used only as an approximate progression view.

No source tables, proprietary plan prescriptions or third-party source code were
copied. NHS programme reuse remains a separate task requiring current schedule
and licensing verification. Real-world coaching efficacy has not been established.

## Verification

`CoachingTestSuite` exercises production generation across distances, three/six-day
availability and short/long horizons, plus one/two-day reductions, blocked race
and unavailable dates, no duplicate placements, frequency, spacing, phase coverage,
Zone 2, beginner fallback, eleven readable complete-programme regression fixtures plus a compact HM fixture,
raw-evidence proposals and runner decisions, scores without HR/RPE, intervals,
progression, strength PB/history/deletion/e1RM and workload coverage. The old
synthetic regression fixtures remain as independent validation utilities.

## 0.4 weekly review and multi-week decisions

`WeeklyCoaching` evaluates the last fully finished Monday–Sunday week and a rolling
four-week context. It uses actual linked activities, optional RPE, qualified
long-run samples, retained prescriptions, recorded strength sets and comparable
results. Run Score and body weight are deliberately absent from its inputs.
The ViewModel recomputes on evidence refresh/mutation off the main thread, not on
screen opening. Reviews, replacement prescriptions, explicit moves/skips and
runner decisions are retained in schema five and in local backups.

Most reviews offer only **Keep original plan / no adjustment required**. Existing
planned progression continues; a good week does not manufacture extra mileage.
Repeated high effort (three sessions), or rising mean RPE (at least three recent
and two earlier responses, +1.5) together with two shortened sessions (<80%), or
two qualified fading long runs can suggest holding/reducing the next two weeks.
Hold caps future weekly distance to the reviewed week's observed distance;
reduce uses 80% of that ceiling. Neither adds distance elsewhere. These thresholds
are transparent product heuristics, not clinically validated risk limits.

Three or more distinct weeks with a weekday missed/moved produce a routine-review
suggestion. Moved-and-completed runs are distinguished from unrecorded and
explicitly skipped sessions. The runner can edit their normal days in Settings;
there is no automatic recurring-availability rewrite. Manual moves validate normal
availability, race boundaries, duplicate dates and maximum frequency. Skips/moves
create plan versions and refresh reminders. Completed prescriptions are protected.

Performance reassessment v2 uses comparable results from the last four weeks
against evidence from the preceding six months. Gains below 2% are ignored.
A substantial gain (>=5%) or multiple corroborating gains (>=2.5% maximum)
may support review, only with >=80% completion, at least three controlled RPE
responses, no elevated-effort concern and at least moderate baseline confidence.
Official/manual race results and deliberate best-effort assessments carry more
weight than incidental rolling efforts; controlled assessments are not treated as
races. Short-distance and older evidence is down-weighted. Same-activity results
count once. Confidence is Strong, Moderate or Limited, with component reasoning.
These are documented product heuristics, not scientifically validated thresholds.

Only existing numerical quality-session ranges in the next four weeks are
proposed faster, by 40% of the observed comparable gain capped at 3%. The dialog
shows current/proposed workouts, dates, pace ranges, confidence and unchanged
sessions before explicit acceptance. Easy/recovery running, volume and the race
goal remain unchanged. Short (<3K) evidence caps proposals at 1.5%; race-pace
work requires evidence covering at least half the target distance, capped at 10K.
Easy-effort segments inside quality workouts are also excluded. No numerical ranges are fabricated when none exist.
Terrain/effort-matched performance modelling remains future work.

## Durability and return guidance

`LongRunDurability` requires >=30 minutes, continuous qualified distance/time
samples covering the run, and the shared gap/jump/total-agreement gates. It
interpolates the time midpoint, compares seconds/metre between halves and, only
with >=80% sensor coverage per half, reports HR-to-pace and cadence changes.
HR-to-pace drift = `(HR2 × pace2 / (HR1 × pace1) − 1) × 100`.
Pace fade above 8% is a review flag, not proof of fatigue: terrain, weather,
intention and pauses can explain changes. Missing/corrupt samples mean
**insufficient evidence**, never totals-derived split/durability estimates.
Late-run RPE by timestamp is not currently collected; session RPE remains separate.

An observed absence or explicitly reported illness/injury/life interruption of
>=14 days can propose a three-week easy return bridge. First-week distance is
65% of existing prescriptions (50% after >=28 days), followed by 75% and 85%.
Frequency does not increase, quality is replaced by easy effort, race day is
retained, and nothing is applied without runner choice. A one-week gap alone
need not change training. This is return-to-training guidance, not medical
clearance. User reports do not diagnose conditions or authorise exercise with
concerning symptoms.

Evidence channels remain separate: volume, intensity, durability, performance,
strength interference, consistency and subjective response. There is no universal
readiness number. Recorded hard lower-body sessions can affect any upcoming
non-race demanding workout, including long runs. Planned strength availability
is never treated as an actually completed hard session.

Race day overrides recurring weekday availability, but not an explicitly blocked
one-off date. Long horizons retain early foundation work, with quality limited
to the final 140 days. Distant foundation volume is capped at baseline +6 km/week
and long-run progression at +3 km, with the later build index reset so crossing
the 196-day boundary cannot jump straight to peak mileage. Horizons beyond 280 days allocate phases backwards from
the race. The generator guards obviously erroneous dates beyond a century;
very distant projections are not fitness promises. Race/time-trial Run Scores
allow RPE 9–10; easy/recovery scores still reward controlled execution.

## 0.5 contextual running and strength

See docs/STRENGTH_PROGRAMMING.md for research, original rules and limitations.
Home projects existing weekly channels without a readiness score. WorkoutContext
uses retained run/gym evidence for briefings and move warnings; it never changes
availability. ComparableRuns matches purpose/distance/duration/recency/available
elevation, rejects weak matches and qualifies faster/harder interpretations. It
never invents route matching, missing HR or guaranteed fitness improvement.
