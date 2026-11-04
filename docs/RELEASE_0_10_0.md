# Race Day Dreams 0.10.0-test1

Strength Programming 2.0 is in development on a dedicated draft pull request.
The existing 0.9 execution and history guarantees remain the baseline; the full
0.10 workflow is not yet ready for release.

## What's new

- The manual programme builder now connects typed local drafts, settings, blocks, workouts,
  exercise targets and dated previews. Activation is an explicit choice; the retained manual workflow passed the previous
  supported Android gate. New integration changes require their own verification.
- Android-independent programme previews and editable draft operations model explicit training blocks,
  immutable revision lineage and preserved exercise/measurement targets.
- Structured custom-exercise entry now records body area, movement role, equipment,
  measurement and notes for manual programming and logging. Existing custom identities and
  history remain readable; no technique guide or estimated strength is invented.
- Nine original starter templates and goal/experience-specific generation now connect to
  equipment/day/goal configuration, saved concrete previews, distinct training blocks and
  dated-workout editing. Review and activation remain explicit. The template and future-review checkpoint passed the supported Android matrix and
  scoped light/dark/200% capture review.

## Improvements

- Manual programme previews and details present dated workouts in chronological order,
  with calculation assumptions available through optional evidence disclosures.
- Authored current programmes now appear in Strength Today, the hybrid Home summary,
  programme/block/workout details and the calendar without converting their optional
  or fractional targets into legacy prescriptions.
- Gym Now starts the exact reviewed authored revision through the existing workout engine.
  Changed programme selections require a fresh review, and equipment alternatives use
  the settings preserved with the started workout. Actual set fields remain empty until entered.

- Reviewed extra-workout associations retain exact authored targets, including optional
  effort/rest and fractional RIR, while reading old legacy decisions.
- Future-review controls prepare immutable revisions for edits and regeneration of upcoming
  programming. Completed and earlier prescriptions remain protected, fresh evidence is
  rechecked on activation, and competing unaccepted drafts remain distinct from history.
  The future-review checkpoint passed the supported Android matrix.
- Programme replacement review now offers ranked, explained options for one dated workout
  or eligible future occurrences. Each phase retains its own targets, load references clear,
  changed measurement tasks require fresh targets, and activation stays explicit. Current
  Android/capture verification of these new controls is pending.

## Fixes

- Current preview acceptance rechecks archived exercise preferences as well as exclusions.
  Programme substitutions are checked against their original training intent at acceptance.
- Future-review screenshot navigation uses the actual large-font accessibility description
  while retaining all production-screen, font and evidence assertions.

- Editing sets, adding or removing exercises in an unaccepted concrete preview recalculates
  the workout-duration estimate before activation. Other dated workouts are unchanged.

- Calendar day selection in the UI test now matches an exact complete date rather than a
  substring that conflated the 4th with the 14th/24th. The test checks all42visible day
  identities while retaining category, accessibility and workout-detail assertions.

- Exercise discovery uses the current authored programme for its programme inventory,
  with legacy inventory retained when appropriate. Custom area/role/equipment filters
  use the metadata the athlete actually supplied.

- Dream artwork publication now distinguishes pending loading from unavailable
  illustrations and keeps readiness tied to the current city. The strict screenshot
  deadline remains unchanged; new fault-injection tests and API verification are required.
- Saved gym-workout reviews remain stable across display-clock ticks. Reopening a completed
  workout no longer lets an empty active coordinator remove its confirmation dialog.
  The regression forces that real tick and retains all explicit-choice/history assertions.
- Existing 0.9 fixes and evidence-preservation contracts are retained.

## Known limitations

- Programme replacement controls require their current Android/capture gate.
  Modality-progression controls and final programme-history navigation remain in progress.
  The manual/custom workflows passed the supported automated gate and scoped capture
  review; physical Galaxy S24 acceptance remains unperformed.
- Beginner power previews explicitly report the current catalogue's lack of a
  beginner-classified ballistic movement instead of inventing power evidence.
- This branch is not ready for release or merge. Current-build Android/accessibility
  gates must pass after integration. Physical Galaxy S24 checks remain unperformed.

## Data, privacy and licensing

- Schema 15 / JSON backup 9 retain typed authored programmes, drafts, decisions and custom
  metadata, adding concrete dated preview storage. The nested authoring section writes 2 and
  reads 1–2; supported older root backups remain readable. Raw archive 2 and complete package 1
  are unchanged. The original 174 exercise guides
  and 14 routine visuals remain unchanged. No Internet permission, backend,
  account, advertising, analytics or runtime LLM is introduced.
- New planning heuristics are original product decisions, not physiological
  guarantees or individually prescribed medical advice.

## Verification

- See CONTINUE_CODEX.md for source-bound checkpoint results. Domain simulation
  exercises the real catalogue; passing it does not establish finished UI or
  physical-device acceptance.
