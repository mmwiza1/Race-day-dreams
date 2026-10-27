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

The full Gym Now flow, reusable/custom programmes and live gym execution remain later
checkpoints. The current spontaneous logging action works independently of a programme.

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

See VERIFICATION.md for observed CI results, including failures and subsequent fixes.
Domain metadata tests exercise every canonical ID, immutable collections, setup truth
tables, role/experience boundaries, invalid definitions and unchanged measurements.
Schema 10, JSON envelope 7, raw archive 2 and complete-backup package 1 remain unchanged.
New durable execution, exclusions and programme concepts require typed repositories and
explicit migration/transactional backup coverage before those features ship.
