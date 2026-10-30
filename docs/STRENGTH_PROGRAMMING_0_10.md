# Strength Programming 2.0 — implementation checkpoint

This is an in-progress 0.10 programming/durability checkpoint, **not** a completed
programme-builder release. Existing 0.9 behaviour and historical prescriptions remain
supported; schema/backup additions are described below.

## Programme authoring and proposals

`StrengthProgramme` describes concrete dated blocks, exact frozen workout definitions,
explicit revision lineage, effective date, origin/template identity and explanations.
Blocks are contiguous programme weeks anchored to their actual start, including a
mid-week start. Supported authoring bounds are 52 weeks, 12 blocks and 12 exercises per
session. A generated preview is bounded to 26 weeks. These are product/resource bounds.

`StrengthProgrammeDraft` permits incomplete authoring without inventing completed work.
It supports replacement/deletion/duplication of workouts and blocks, block ordering,
exercise ordering and moves between days. A complete preview validates available days,
frequency, targets, equipment and exclusions. Custom exercise definitions have explicit
body-area/movement/equipment/task metadata, but no invented technique or automatic
progression eligibility. Canonical guidance remains unchanged.

`StrengthProgrammeEditor` returns proposals only. It protects every historical and
completed session, including actual-linked future prescriptions when availability changes.
Regeneration cannot shorten a programme past retained history. Substitution proposals
reuse the existing intent/equipment/experience/exclusion ranker; incompatible measurements
require a reviewed new target and every replacement clears load references.

## Original programming policy

Nine starter structures cover Full Body 2/3, Upper/Lower 4, Push/Pull/Legs 3/6,
Strength 3, Muscle 4, and Running Support 2/3. They are editable starting structures,
not branded programmes or individually assessed recommendations.

Generation distinguishes primary strength prescriptions, muscle volume, controlled
power/support volume, unilateral/lower-leg/trunk running support, experience suitability,
and optional Foundation/Development/Intensification/Recovery-Assessment phases. The
optional assessment does not prescribe maximal testing. Longer beginner programmes use
more practice and do not force general-purpose intensification; Running First support
also avoids a compulsory intensification block. Power specificity uses fewer quality
repetitions/sets and recovery retains the lower-effort range. Power work stops when speed
or control deteriorates. Beginner power currently has an explicit catalogue limitation:
no existing POWER-primary exercise is classified beginner-friendly. The preview describes
movement practice honestly; it does not relabel another movement as ballistic exposure.

Exact equipment capabilities and exclusions are hard candidate filters. Infeasible setups
fail explicitly, while missing exposures and constrained time/scheduling are explained.
Hybrid placement uses bounded subset search over available days and soft directional
penalties for demanding runs. No running plan is changed. Date-specific conflicts remain
in workout notes even when a long programme needs a compact top-level summary.

Time budgeting is an openly documented planning estimate using prescribed work/rest and
transitions. It is not observed exercise speed or physiological precision. Loads are never
generated without real exact-exercise evidence.

## Execution and progression boundary

`GymWorkoutAdapter.fromAuthoredProgramme` and
`GymWorkoutSourceValidator.authoredProgramme` pass exact frozen targets into the existing
`GymExecutionEngine`. No second execution engine or source kind is introduced. Domain
tests verify that starting an authored prescription creates no actual sets.

The Android repository now resolves exact legacy and authored PROGRAMME sources through
the same execution engine, preserving frozen targets and finalisation links. Reviewed spontaneous Count/Keep now accepts exact authored workouts through the existing
association coordinator/projection. New decision payload2 retains the frozen definition,
nullable/fractional RIR and optional rest; legacy decision1 remains readable. Current
programme display and activation/start UI still need integration before the builder is
exposed. No legacy RPE approximation is used.
In particular, do not coerce optional/null/fractional effort into fake legacy RPE values.

`StrengthTargetProgression` supplies optional domain proposals. It requires two distinct,
recent exact-exercise sessions with enough actual working sets, matching task/loading and
recorded intended effort. Warm-ups, stale/future evidence and unknown effort do not drive
increases. Weighted advice delegates to the established conservative load rule; other
supported tasks can suggest a small rep/time/distance increase at the same proven loading.
Custom-only tasks receive no invented progression. This policy is not yet connected to
new programme-edit controls; the existing 0.9 advice remains unchanged.

## Durability — next integration phase

`StrengthProgrammingJson` uses independently versioned programme/draft/custom payloads,
the existing strict bounded gym parser and frozen-workout codec. It rejects unknown/future
fields, duplicate/coerced fields and invalid nested structures. Android codec tests are
required on all supported APIs. This is a serialization boundary, not a SQL repository.

Schema 14 adds typed revision/header, block, per-session, current-pointer, decision,
draft/header, draft-block and custom-metadata tables over the shared database. Summaries
query scalar columns with a history index; detail loads bounded session rows. Draft saves
use optimistic revisions. Activation compares the current pointer, verifies fresh
actual-linked protected sessions inside the transaction, appends an immutable revision and
records the athlete's decision atomically. Stopping removes the pointer and retains history.
Custom metadata mirrors the logging definition without inventing technique or progression.

JSON envelope 9 requires the version-one strengthProgramming section and retains readers
for envelopes 1–8. Restore checks references, lineage, ordered decisions, custom/logging
agreement and row-size limits before replacing anything; all reconstruction uses the shared
restore transaction. strengthEngine 2, strengthState 2, raw archive 2 and complete package 1
are unchanged. Current source-bound test results belong in VERIFICATION.md; no UI completion
or physical acceptance follows merely from this persistence boundary.

## Verification boundary

Current local checks: 587,615 domain assertions across 33 suites; 60 schema cases;
9 release-note cases; strict 174/174 canonical and 14/14 routine guidance. The new
2,430-configuration simulator checks actual catalogue metadata, equipment, exclusions,
duration, primary balance, controlled power volume, support roles, determinism and
weekday boundaries. See VERIFICATION.md and CONTINUE_CODEX.md for exact CI state.
No physical Galaxy S24 acceptance is claimed.

## Association and terminal display hardening

The association proposal carries exactly one preserved family (legacy Session or authored
Workout). Discovery requires a nearby date and an exact working measurement task in common;
it never implies that every prescribed set was completed. Current-version and occupied-target
checks happen inside the acceptance transaction. Keeping extra can retain or reverse an old
review without rewriting actuals or prescriptions. Backup validation checks each family
against its exact retained version, including the original snapshot after later actual edits.

Completed-workout history is a terminal projection, independent of an active execution
coordinator. Display-clock observations now run only for unfinished displayed sessions;
reopening a finished workout must not let a later empty coordinator tick remove its review.
The original UI test forces that real tick while the confirmation is open. This production
fix addresses the reappearing API35 failure after the earlier appearance-wait-only repair.

## Authored current-programme integration

The current typed revision now projects into Strength Today, the hybrid Home summary,
programme/block/workout details, calendar entries and Gym Now's planned-session list.
Optional rest and fractional RIR are displayed directly from frozen slots. Dates use the
existing UK presentation. The calendar suppresses a duplicate planned entry only after
an explicit valid actual/prescription link; a spontaneous gym visit is not completion.

Planned start compares the displayed programme identity/revision with the typed current
pointer and refreshes a stale selection rather than silently changing targets. Execution,
actual entry, pause/recovery and finalisation still use the original Gym Now coordinator.
Equipment-busy alternatives resolve the started authored version's goal, experience and
specific equipment capabilities. Today-only swaps clear incompatible reference loads and
preserve the original prescription/source. The manual builder now saves and previews typed recurring blocks and explicitly activates
reviewed drafts. Editable generated/template previews and future-edit controls remain the
next phase. Structured custom metadata entry reuses the typed custom repository/logging
mirror, supports all existing measurement tasks, retains older UUID history and keeps
custom movements out of automatic generated selection. No fake guide or estimated1RM is
attached. Current source-specific Android/capture status is recorded in CONTINUE_CODEX.md.

## Current programme inventory

The exercise browser reads authored prescriptions before legacy programme inventory and
refreshes when the current family changes. This is a read-only projection; historical
programme rows and actual sets are unchanged. Typed custom metadata supplies body-area,
movement and equipment filters without inferring classification from an exercise name.
