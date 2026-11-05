# Recorded strength intelligence — 0.11 development

The app recommends. The athlete decides. The plan adapts.

## Working sets and primary areas

`StrengthVolume` reads completed actual `StrengthSession` records, never planned targets or
unfinished gym staging. Warm-ups remain separate. Windows start on Monday using each recorded
session's time-zone offset; future-dated observations are excluded. Empty weeks mean no working
sets were recorded, not proof that the athlete rested. The current week is explicitly incomplete.

One working set is divided equally among the canonical exercise's documented primary body areas.
Secondary involvement is counted separately as exposure and is never added as another full working
set. This is an original, inspectable product taxonomy, not measured muscle stimulus or a universal
stress score. Areas can overlap during training without pretending biological precision. Custom or
changed exercise definitions without supported canonical anatomy remain unclassified; their actual
sets, duration, repetitions and distance are still retained.

Frequency counts distinct recorded workouts containing working sets for that exercise. External
load × repetitions is provided only per exact external-load repetition exercise. Bodyweight work,
loaded bodyweight, holds, carries and distance tasks are not converted into equivalent kilograms.
The projection runs off the main thread and cannot change prescriptions, actuals or sources.

## Recorded records versus estimates

`StrengthRecords` retains workout ID, set ID, actual measurements, timestamp and original local
record date. It provides heaviest recorded working sets, best weight at exact repetition counts,
most repetitions at a particular load, bodyweight repetitions, timed efforts and distance efforts.
Loaded holds and carries remain separate tasks at different loads. Tiny conversion residue is
normalised only in comparison keys; original entered measurements remain untouched.

Eligible external-load working sets of 2–10 repetitions can also produce a separately labelled
Epley estimate: load × (1 + repetitions / 30). A single recorded repetition remains a measured lift;
it does not create another estimated record in this projection. Warm-ups and unsupported exercise
modalities produce no estimate. The calculation does not assess technique, imply a maximal test,
recommend lifting the estimate, or replace the existing conservative progression/starting-load rules.

The new records page leads with actual recorded performance. Estimates have separate cards and
explanations. Historical/manual entries and their original editing/progression UI remain accessible
through “Historical entries & full PB history”; self-reported records are not relabelled as gym actuals.

## Restrained local milestones

`TrainingMilestones` recognises a fixed small set: first/five/fifty recorded strength workouts,
first/ten recorded runs, a recorded run of at least 5 km, and training across twelve distinct weeks.
Warm-up-only sessions, manual PB entries and future observations do not qualify. Missing distance
cannot establish a distance milestone. Missing time-zone information cannot establish a distinct
local training week. Weeks need not be consecutive; there are no rest penalties or forced streaks.

Recognition is idempotent and includes eligible retained history. Typed normalized SQLite rows keep
source identities and times, recognition time, and the relevant recorded distance where applicable.
Importing older history does not issue the same award again or silently redate existing recognition.
If a source is explicitly deleted or is not available after restore/provider changes, the UI labels
its retained reference honestly. An award does not imply deleted detailed evidence can be restored.
Raw pruning must not delete ordinary workouts, actual sets or these permanent recognition records.

Celebrations are optional in-app notices, disabled by default. No achievement system notification,
forced streak reminder, network service or engagement penalty is introduced. Refreshing milestones
never changes running plans, strength programming, completed sets or coaching decisions.

## Persistence and verification boundary

Schema 16 introduced typed advisory rest state. Additive schema 17 introduces milestone headers,
ordered source references and an opt-in celebration preference. Root JSON 11 requires its strict
versioned achievement section and retains readers for older supported backups through 10; missing
older achievement data restores to empty recognition/celebrations off. Raw archive 2 and complete
package 1 are unchanged. Restore validates before replacement and uses the existing shared SQLite
transaction. These new screens still require the final-source API 28/35/36 and authentic visual gate;
consult `CONTINUE_CODEX.md` and `VERIFICATION.md` for observed results rather than assuming passes.
