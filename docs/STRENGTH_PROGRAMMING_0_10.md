# Strength Programming 2.0 — implementation checkpoint

This is an in-progress 0.10 domain/codec checkpoint, **not** a completed programme-builder
release. Existing 0.9 application behaviour, storage and backups remain authoritative.

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

**The Android repository currently resolves PROGRAMME sources through legacy
`GymProgrammeStore`; it has not yet been connected to authored-programme storage.**
Do not expose modern activation/start controls until source resolution, finalisation,
associations and full backup graph validation support both retained source families.
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

Schema 13, JSON envelope 8, strengthEngine 2, strengthState 2, raw archive 2 and complete
package 1 are unchanged. Next work must add typed tables over the shared SQLite helper,
optimistic drafts, immutable revisions/decisions and bounded per-session rows; integrate
old/new source resolution and backup validation transactionally before activation UI.
Avoid storing a whole long programme in a single CursorWindow-sized payload.

## Verification boundary

Current local checks: 459,664 domain assertions across 33 suites; 55 schema cases;
9 release-note cases; strict 174/174 canonical and 14/14 routine guidance. The new
1,620-configuration simulator checks actual catalogue metadata, equipment, exclusions,
duration, primary balance, controlled power volume, support roles, determinism and
weekday boundaries. See VERIFICATION.md and CONTINUE_CODEX.md for exact CI state.
No physical Galaxy S24 acceptance is claimed.
