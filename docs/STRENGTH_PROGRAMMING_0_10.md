# Strength Programming 2.0 — 0.10 implementation and verification boundary

0.10 remains an in-progress draft release. The manual/custom, template and future-review
checkpoints have passed their source-bound supported Android gates. New permanent swap,
progression and history UI require their own gate and actual capture review. See
VERIFICATION.md and CONTINUE_CODEX.md; no physical Galaxy S24 acceptance is implied.

## One durable prescription model

`StrengthProgramme` preserves dated blocks, exact frozen `GymWorkoutDefinition` targets,
revision lineage, effective date, origin/template identity, notes and explanations.
Programme weeks anchor to the actual start, including mid-week starts. Manual authoring
supports up to52weeks/12blocks/12exercises per session; generation supports26weeks. These
are product/resource bounds, not physiological advice.

The phone manual builder saves typed optimistic drafts, including incomplete drafts. Users
configure name, goal, days/frequency, equipment, duration, blocks/phases, workout names and
notes, exercise order, sets, rep/time/distance targets, optional fractional RPE/RIR and rest.
It supports adding/removing/replacing/moving exercises, duplicating workouts/blocks, ordering
blocks, saved recovery, preview and explicit activation. Targets are never actual entered sets.

Structured custom entry uses stable identities, broad area/role, explicit equipment,
measurement and notes. Supported logging profiles remain load+reps, reps, time, load+time,
distance and load+distance, including weighted-bodyweight where applicable. Old UUID/custom
history remains readable. No technique guide, estimated1RM or automatic progression is
invented for custom tasks; canonical generation uses the retained174exercise catalogue.

## Nine editable starting structures

Full Body2/3, Upper/Lower4, Push/Pull/Legs3/6, Strength3, Muscle4 and Running Support2/3 are
original Race Day Dreams templates. Each explains intended users, time and trade-offs.
Configure or generate a concrete saved preview, inspect every dated block/workout and edit
before acceptance. Saved concrete previews preserve all generated weeks, targets, notes,
phases and trade-offs; the first week is never flattened into a repeated substitute.
Another generated preview preserves earlier saved drafts. Accepted prescriptions are read-only.

## Original programming policy

Strength prioritises repeatable main compounds, lower reps and longer rest, with different
secondary/accessory prescriptions. Muscle uses greater working exposure and accessories;
power uses lower-fatigue quality work and greater rest where a suitable exercise exists.
Running support deliberately covers squat/hinge, unilateral, calf/lower-leg, trunk and useful
upper-body maintenance. General remains balanced. These are transparent original product
heuristics, not individually validated physiological prescriptions.

Beginner choices use simpler stable movements, fewer exercises, lower volume and more
reserve. Intermediate/experienced choices allow suitable specificity and volume. Longer
programmes use meaningful Foundation/Development/Intensification phases when appropriate;
optional Recovery/Assessment uses less work and more reserve without forced maximal testing.
Not every goal/experience needs every phase. Experienced power has genuine classified power
exposure. The catalogue has no beginner-friendly POWER-primary movement; beginner power
previews explicitly explain movement practice rather than falsely claiming ballistic work.
No Olympic derivative is automatically assigned to beginners.

Exact equipment/exclusions are hard candidate filters. Infeasible configurations fail with
an explanation. A short session cannot honestly fit every possible exposure. Work/rest/
transition time is an approximate planning budget, not measured speed. Generation invents
no load. Exercise roles bound primary/accessory balance, overlap and duplication.

Hybrid placement considers demanding run dates and race-week context using soft penalties
and explained trade-offs. It never rewrites running plans. Strength-only users need no race
or running setup. Date-specific conflicts remain in workout notes even when the top-level
trade-off summary is bounded.

## Future changes are proposals

Prepare a next revision from accepted authored programming, edit dated future targets or
regenerate using changed goal, frequency, days, equipment, priority, length or time budget.
The original start and exact earlier/completed prescriptions remain. A frequency change
can drop an incompatible template identity and explain the new goal-based split. Completed
future-dated actual-linked workouts are protected too; they are not inferred from dates.

A forward revision cannot activate while a native workout from that programme is unfinished,
paused or awaiting recovery. Finish/discard it explicitly first. Current active prescriptions
are protected during proposal edits and bulk replacements without calling them completed.
Previewing changes never stops a workout. Activation rechecks actual-linked completions,
current ownership and fresh eligibility in one transaction. Competing proposals remain
separate; accepted contents become read-only and superseded alternatives never replace them.

## Permanent and today-only replacements

Gym Now retains its concise equipment-busy/unavailable today-only flow and frozen source.
Programme review separately offers ranked, explained options using the reviewed goal,
experience, equipment and fresh exclusions/archive preferences. A saved proposal changes one
dated exercise or eligible future occurrences. Each phase keeps its own rep/effort/rest
prescription; every replacement clears load references. A different measurement requires a
fresh target for that dated workout. Custom/unsupported tasks get no invented automatic match.

Pure substitution rules preserve accepted-base→final provenance, undo and protected history.
SQL activation revalidates the original role/measurement intent and fresh candidate eligibility.
Only explicit activation accepts the immutable new programme revision. Nothing rewrites actuals.

## Conservative optional progression

The preview's progression review calls the existing pure `StrengthTargetProgression` over up
to20recent logged sessions. It requires two distinct recent exact-exercise sessions with
enough comparable working sets, same proven load and intended recorded effort. Warm-ups,
stale/future evidence, missing effort and insufficient work do not support an increase.
A bounded recent query can conservatively miss older evidence; it never invents it.

Weighted rep tasks reuse the established small optional load rule. Bodyweight can propose
reps, time tasks a small duration increase, and carries/distance a small distance increase
at proven loading. Weighted bodyweight uses its supported exact task; no unrelated exercise
load transfer or estimated1RM is invented. Unsupported custom tasks remain without automatic
progression. The existing StartingLoad safeguards remain unchanged.

Review shows current/optional target and explanation. Keep the target or explicitly save a
proposal for that date. Save rechecks exact reviewed draft/target and fresh actual evidence;
changed evidence requires review again. The immutable revision reason retains the chosen
proposal explanation/supporting session IDs. Actual sets, other dates and active programming
remain separate until explicit activation. This is not an automatic progression programme.

## Read-only accepted history

Programme revision history pages bounded indexed SQLite summaries. Opening a revision loads
its preserved blocks/dated targets on IO. Inspecting it cannot reactivate or edit it. The
list distinguishes prescriptions from actuals; workout actual/history remains in Gym Now.
No raw recording samples are read for these views.

## One execution and transaction boundary

`GymWorkoutAdapter.fromAuthoredProgramme` and the existing source validator pass exact
frozen targets to the original `GymExecutionEngine`. No second engine/source kind is added.
Strength Today, hybrid Home, programme details, calendar and planned Gym Now starts read
the typed current owner. Optional/null/fractional targets are never coerced to legacy RPE.
Spontaneous Count/Keep is an explicit reviewed association, not assumed completion.

Schema14 added typed accepted revision/header/block/session/current/decision/manual draft/
custom metadata storage. Schema15 adds separate concrete preview header/block/workout tables.
Summaries avoid payload hydration; detail reads and activation use shared SQL transactions.
Exact reviewed draft/current-pointer CAS and fresh protection precede acceptance. Lost-ack
retries remain idempotent; failures roll back. Historical actuals and custom logging mirrors
remain separate and intact.

JSON envelope9 writes nested strengthProgramming2 and reads1–2; older supported root1–8
backups remain supported. Strict validation checks fields, references, lineage, decisions,
custom/logging agreement and size bounds before transactional replacement. Corrupt/future
archives must not replace local data. strengthEngine2, strengthState2, raw2 and complete1
remain unchanged. No new catch-all JSON bucket, runtime dependency or permission is added.

## Verification

The deterministic2430configuration simulator exercises goals, priority, experience, frequency,
time, available weekdays, three equipment setups, exclusions/favourites and hybrid context.
Invariants cover valid equipment/tasks, excluded choices, unique dates/exercises, duration,
primary balance, controlled power volume, runner-support exposure and beginner suitability.
Explicit constrained failure is preferable to an invented eligible exercise. The domain
progression suite covers supported modalities, stale/missing/high effort and duplicate identity.

Android cases cover typed restore/migration, exact reviewed acceptance, native actual preservation,
competing drafts, active interruption, late exclusions, fresh measurement swaps, optional targets
and read-only history. New real production captures include templates, blocks, future review,
replacement choices, progression and history in light/dark/200% text. Passing source compilation
is not APK/lint/device verification; use source-bound matrix results in VERIFICATION.md.

0.11 nutrition, analytics, PB redesign, achievements, deload intelligence, advanced hybrid,
rest timer, generated warm-ups, supersets and catalogue-for-breadth remain out of scope.
