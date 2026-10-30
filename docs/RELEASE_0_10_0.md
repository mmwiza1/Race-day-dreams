# Race Day Dreams 0.10.0-test1

Strength Programming 2.0 is in development on a dedicated draft pull request.
The existing 0.9 execution and history guarantees remain the baseline; the full
0.10 workflow is not yet ready for release.

## What's new

- The manual programme builder now connects typed local drafts, settings, blocks, workouts,
  exercise targets and dated previews. Activation is an explicit choice; Android acceptance
  verification for this new workflow is pending.
- Android-independent programme previews and editable draft operations model explicit training blocks,
  immutable revision lineage and preserved exercise/measurement targets.
- Structured custom-exercise entry now records body area, movement role, equipment,
  measurement and notes for manual programming and logging. Existing custom identities and
  history remain readable; no technique guide or estimated strength is invented.
- Nine original starter structures and goal/experience-specific preview policies
  are under automated development. These are not yet exposed as app controls.

## Improvements

- Authored current programmes now appear in Strength Today, the hybrid Home summary,
  programme/block/workout details and the calendar without converting their optional
  or fractional targets into legacy prescriptions.
- Gym Now starts the exact reviewed authored revision through the existing workout engine.
  Changed programme selections require a fresh review, and equipment alternatives use
  the settings preserved with the started workout. Actual set fields remain empty until entered.

- Reviewed extra-workout associations retain exact authored targets, including optional
  effort/rest and fractional RIR, while reading old legacy decisions.
- Programme proposals can validate future-only changes without rewriting completed
  prescriptions. Permanent-substitution proposals reuse existing exclusion,
  equipment and intent rules and discard incompatible load references.

## Fixes

- Dream artwork publication now distinguishes pending loading from unavailable
  illustrations and keeps readiness tied to the current city. The strict screenshot
  deadline remains unchanged; new fault-injection tests and API verification are required.
- Saved gym-workout reviews remain stable across display-clock ticks. Reopening a completed
  workout no longer lets an empty active coordinator remove its confirmation dialog.
  The regression forces that real tick and retains all explicit-choice/history assertions.
- Existing 0.9 fixes and evidence-preservation contracts are retained.

## Known limitations

- Editable template/generated-preview screens, future regeneration,
  permanent-swap UI and modality-progression controls remain in progress.
  Their domain and typed persistence foundations are not yet a finished user workflow.
  The new custom-entry and manual-builder workflows also require their final API/capture verification.
- Beginner power previews explicitly report the current catalogue's lack of a
  beginner-classified ballistic movement instead of inventing power evidence.
- This branch is not ready for release or merge. Current-build Android/accessibility
  gates must pass after integration. Physical Galaxy S24 checks remain unperformed.

## Data, privacy and licensing

- Schema 14 / JSON backup 9 add typed authored programmes, drafts, decisions and custom
  metadata. Supported older backup readers remain. Raw archive 2 and complete package 1
  are unchanged. The original 174 exercise guides
  and 14 routine visuals remain unchanged. No Internet permission, backend,
  account, advertising, analytics or runtime LLM is introduced.
- New planning heuristics are original product decisions, not physiological
  guarantees or individually prescribed medical advice.

## Verification

- See CONTINUE_CODEX.md for source-bound checkpoint results. Domain simulation
  exercises the real catalogue; passing it does not establish finished UI or
  physical-device acceptance.
