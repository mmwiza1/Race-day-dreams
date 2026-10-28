# Strength Engine 2.0 — implementation checkpoints

The app recommends. The athlete decides. The plan adapts.

This is ongoing 0.9.0-test1 development, not the completed release. The complete remaining
scope is retained in CONTINUE_CODEX.md. The main rolling build remains the last verified
merged release until all delivery gates pass.

## Standalone focus and Today

Running, Strength and Hybrid are changeable presentation priorities over the same local
database. Strength can create a gym programme without a race goal or running profile.
Changing focus does not rewrite goals, programme priorities, versions or actual workouts.
The typed primary-focus adapter uses existing backed-up string settings, including legacy
starting-path interpretation; no schema or backup-envelope change is needed for it.

Strength-first Today shows the day's prescription, independent logging, actual workouts,
explicit programme completion, recent actual PB evidence and one optional coaching review.
A workout on the same date never implies prescription completion. Versioned associations
preserve the identity of the accepted programme. PB/coaching history is prepared off the
main thread in the coherent application snapshot. No running configuration is required.

Gym Now now connects planned, quick, custom-built, repeated, favourite and generated
workouts to the same durable gym execution flow. It is available from Strength-first
Today and Strength navigation, alongside independent manual logging. The connected UI
checkpoint still needs its own observed Android/API acceptance results; implementing
these screens does not complete the entire Strength Engine 2.0 release. See
[GYM_NOW.md](GYM_NOW.md) for the entry flows, actual-set controls and remaining phases.

## Corrected block allocation

Six programme weeks are six consecutive seven-day windows from the selected start date,
including a mid-week start. The requested frequency is allocated in each window. Lower
session variants use their own occurrence count, rather than overall session-index parity,
so Upper/Lower includes squat and hinge emphasis where equipment permits.

Scheduling classifies the retained exercises: meaningful lower-body work in a Full Body
session counts, while core-only work does not. Same-day and immediately-before demanding
runs carry conflict context; the following day carries a softer recovery penalty. These
are original inspectable product heuristics, not medical rules. Sessions are not silently
removed. Explanations name only movement types actually retained within the time budget.

## Explicit canonical exercise metadata

ExerciseMetadataCatalog version 1 covers every existing canonical exercise (currently
174). Stable IDs map to an explicit primary movement role, other relevant roles, broad
primary/secondary body areas, experience suitability and equipment requirements. This
does not alter historical StrengthExercise definitions, measurement profiles, sets or PBs.

Equipment is an OR of complete setups: a goblet squat can use dumbbells OR a kettlebell;
a barbell bench press requires its complete bar/bench/rack setup. Individual machines and
accessories are distinguished. Owning one machine does not imply owning all machines.
Requirements describe the taught canonical setup rather than every improvised variation.

Suitability describes complexity, not medical safety. Broad muscle areas describe
involvement, not measured stimulus or fractional set-equivalence weights. These original
product classifications support later browsing and intent-preserving selection; this
checkpoint does not change generator selection to use them yet. Unknown/custom exercise
metadata remains unknown until explicitly supplied. A new canonical ID without reviewed
metadata fails catalogue validation and the completeness test.

The 174 original canonical visual guides and 14 routine movements remain intact. New
exercise additions are quality-gated rather than a raw catalogue-count target.

## Verification and durability

Gym Now has one Android-independent GymExecutionEngine for all
origins. Immutable date-free GymWorkoutDefinition revisions preserve exercise-slot IDs,
frozen exercise definitions, measurement/effort targets, optional references and superset
groups. Programme adapters bind an exact programme/session/revision; repeated workouts
copy structure and intentions, never completed actual sets. Today-only replacements
retain original prescriptions and actual evidence and remove incompatible load references.

GymExecution snapshots hold actual entered sets separately, with idempotent stable IDs,
explicit pause/resume and recovery gaps, monotonic timing and partial-completion evidence.
Corrupted event sequences, mismatched actual references and invented completion are rejected.
Only an explicitly started planned execution can propose its preserved association.
Storage codecs must additionally validate the frozen source against retained versions.

The domain engine is connected through an activity-shared GymNowViewModel and one
GymWorkoutSessionCoordinator. Accepted actual sets are saved immediately; the UI clears
entered values only after acknowledgement. Exact pending writes can be retried without
new set IDs or events, while other mutations remain blocked. Reopening an interrupted
stage requires an explicit recovery choice; retained pause state and recovery gaps remain
honest. Finishing with target working sets missing requires an early-finish confirmation.
No unfinished actuals are put into CoachingStore, and no actual sets are fabricated from
targets. Strict v1 JSON codecs and typed storage provide durable staging independently
of Android's saved UI state.

See VERIFICATION.md for observed CI results, including failures and subsequent fixes.
Domain metadata tests exercise every canonical ID, immutable collections, setup truth
tables, role/experience boundaries, invalid definitions and unchanged measurements.
The typed durability checkpoint, source77fd0b4, passed its complete supported-API gate
with293Android tests on each API28/35/36 in workflow40601581173. It uses schema 11 and
JSON envelope 8; raw archive 2 and complete container 1 remain unchanged. These observed
foundation results do not certify the subsequently connected Gym Now screens. Their
build, interaction, screenshot and supported-API results must be recorded separately in
VERIFICATION.md. No physical Galaxy S24 acceptance has been performed.

## Typed durability increment

The repository foundation stores frozen workout revisions/preferences and normalised
execution headers, append-only entered sets and events on the existing shared database.
It uses revision-checked atomic saves, harmless identical retries, one unfinished stage,
and an atomic completed actual plus exact planned-source association. Summary reads do
not hydrate set/event histories. Original prescriptions and execution evidence remain
frozen; explicitly accepted edits retain a separate current actual and edit observation,
and PBs follow the current actual history. Repeat templates preserve validated source
proof even if the original completed actual is later deleted.

Schema 11 is limited to those five typed table families. Later exclusions, custom
exercises and programme/block data require explicit subsequent migrations rather than
silently extending a circulated schema. Envelope 8's independently versioned strength
section retains preferences, source proof, stages and accepted edits, while older readers
1–7 remain supported. Restore validates the whole graph before a shared transaction and
refuses to replace a local unfinished gym workout. Each storage row is bounded; sets and
events do not accumulate in an unbounded cursor cell.

This persistence foundation now backs the connected Gym Now and live actual-set UI.
See [STRENGTH_STORAGE_2.md](STRENGTH_STORAGE_2.md) for the precise format, recovery, bounds
and compatibility contract, and [GYM_NOW.md](GYM_NOW.md) for the UI boundary. Current
Gym Now integration is pending its own observed API28/35/36 gate; no expected test count
is a reported pass. Optional rest timers/notifications, warm-up ramps, superset execution,
ranked substitutions, custom programme templates and the richer multi-block generator
remain later checkpoints retained in CONTINUE_CODEX.md. The working actual-set flow does
not imply those features are complete.
