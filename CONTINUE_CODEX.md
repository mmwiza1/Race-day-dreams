# Continue Codex — current main to 0.10.0-test1

## Purpose

This file is the canonical continuation handoff for Race Day Dreams.

When instructed with:

`Read CONTINUE_CODEX.md and continue from the current repo state`

continue from the repository exactly as it exists. Inspect the live repo before changing anything. Preserve completed work, infer unfinished work from the current branch/diff/log, complete partial implementation, run the relevant tests/lint/build, fix regressions, and leave a concise summary of what was completed and what remains.

Do not blindly reset, force-push, discard valid changes, or recreate already-completed work.

## Active release — 0.10.0-test1: Strength Programming 2.0

The latest owner brief supersedes the earlier broad Strength Engine 2.0 backlog.
0.9 is COMPLETE and merged, not unfinished work. Do not reset/restart it.

- Repository: mmwiza1/Race-day-dreams.
- Feature branch: feature/0.10-strength-programming, based on live main c6299e2
  (documentation/rules update after 0.9 merge 1fd0d2d).
- Version: 0.10.0-test1 / code13. Schema13, JSON8, strengthEngine2,
  strengthState2, raw2 and complete1 are currently UNCHANGED.
- Baseline: clean source tree; 219,527 domain assertions/31 suites,55 schema,
  9 release-note cases passed locally before implementation. Merged 0.9 main
  workflow40760699485 passed331 instrumentation cases EACH API28/35/36 and
  publication. Main APK50,799,716bytes. S24 physical tests remain unperformed.

## Permanent continuation rules

Inspect status/diff/log, current remote main and PR before working. Preserve valid
local and remote changes. Use branch+PR, no force/reset, no main development.
Domain rules stay Android-independent; typed repositories share one database and
restore transaction. Reuse Gym Now's authoritative execution model. Prescriptions,
actual sets, historical versions and explicit choices remain distinct. No Internet
permission, account, backend, analytics, ads, subscriptions or runtime LLM.
The app recommends. The athlete decides. The plan adapts.

## Current phase / decisions

Phase A: domain authoring/preview foundation implemented and locally tested. StrengthProgramme models
immutable lineage, contiguous dated blocks and frozen GymWorkoutDefinition targets;
StrengthProgrammeEditor validates future-only revisions and permanent substitution
proposals through the existing metadata/equipment/exclusion ranker, clearing loads.
StrengthProgrammeGenerator produces explicit dated previews with nine original
editable template structures, goal/experience prescriptions, optional block phases,
time budgeting, hybrid soft-placement/context and race-week preview reductions.
Draft authoring supports partial saveable state, ordering/moves, duplicate workouts/blocks,
custom targets/notes and strict complete-preview validation. Future regeneration protects
past/actual-linked sessions even after availability changes. A shared transparent time
budget explains custom session estimates and budget overruns.
No persistence/activation/UI is connected to this model yet. Legacy programmes and
Gym Now behaviour are unchanged. Do not describe this domain checkpoint as a shipped
builder or a complete 0.10 release.

New programme-quality suite currently covers1,620 configurations and209,755 assertions. It verifies real metadata, exclusions, equipment, timing,
repeatability, templates/blocks, weekday boundaries and future-only substitution rules.
A canonical catalogue gap is explicit: no BEGINNER_FRIENDLY POWER-primary movement
exists in the current174. Beginner power previews honestly describe stable strength
practice rather than mislabelling another exercise; do not invent guidance/artwork.
Review this limitation against final product acceptance before marking complete.

## Remaining 0.10 work (in order)

1. Domain draft operations, custom-exercise metadata, time-budget explanations and
   protected future regeneration are now implemented and tested. Extend quality checks
   further during integration for movement balance, goal/experience/priority and
   constrained contexts; do not remove the existing simulator invariants.
2. Typed repositories and additive schema from13 for authored programme revisions,
   blocks/sessions/targets, drafts, explicit activation/change decisions and modest custom
   exercise metadata. Resolve existing programme IDs/history through a compatibility
   boundary; never refreeze history from today's catalogue. Define and test strict codecs.
3. Wire preview/activation and custom programme/template builder to scoped application
   coordinator/ViewModel/navigation. Support draft save/reload, equipment, days, effort,
   rest, notes, add/remove/reorder/move/duplicate exercises/workouts/blocks, editing only
   future work and explained regeneration. Every visible control must work.
4. Connect modern programme sources to the ONE existing Gym Now execution model, actual
   history/association and conservative starting-load guidance. Add permanent-vs-today
   substitution choice with durable provenance and no load transfer; preserve existing
   today-only path and exclusions. Modest custom exercises log/history without invented
   technique or unsupported automated progression.
5. Conservative modality progression (load/reps/bodyweight/weighted-bodyweight/duration/
   carries/distance) from recent comparable real working sets and recorded effort; unknown
   effort remains unknown; explicit optional proposals only. No automatic rewriting.
6. Complete backup/version graph integration once schema is settled. Bump only as needed;
   keep old supported readers, validate before replacement, one transaction, test all
   old schemas including13, actual/history/exclusions/substitutions preservation,
   corrupt/future rollback, current/old round-trips and realistic large histories.
7. Full regression/API28/35/36, formatting/lint/build/release/source/catalogue integrity;
   add authentic builder/template/preview/blocks/custom/permanent-swap captures and
   review light/dark/200%/TalkBack semantics. Keep existing174+14 guidance truthful.
8. Update release/architecture/storage/TEST_BUILD/VERIFICATION/README docs with actual
   results; keep draft PR until ALL acceptance criteria green. Prepare for owner review;
   do not merge prematurely or create a stable tag. S24 manual checks separate.

## Explicitly OUT of 0.10 (0.11)

Nutrition/protein; new height/weight intelligence; muscle-volume dashboard; major PB
redesign; achievements; intelligent deload/recovery engine; advanced analytics/hybrid;
rest timer; generated warm-up sets; supersets; major live-workout polish; catalogue
expansion for breadth. Do not revive these from older handoff revisions. Only genuine
catalogue gaps may be filled with complete metadata/guidance and tests.

## Gates / exact next action

Inspect actual git state first. Phase A local gate:429,282 assertions/32 suites,
55schema and9release-note cases passed; pinned format/check and diff check pass.
Commit/push this coherent checkpoint and open a DRAFT PR targeting main, then continue
with Phase B typed codecs/repositories/backup integration before adding UI controls. Workflow capture branch/artifact version
has been advanced to0.10; new source requires fresh91canonical captures before acceptance.
No local Android SDK/adb is verified; use actual GitHub CI rather than claiming local or
physical tests. Record exact commit/run IDs, failures and next files here at each checkpoint.

## Physical S24 checks — unperformed

Outdoor/locked/background/long GPS, poor GPS return, battery/reboot/process recovery;
real Health Connect providers/write/reimport/revocation; BLE HR/cadence hardware;
TTS/music/calls/audio focus; TalkBack/200% real use; complete backup/restore on hardware.
