# UI Redesign v2 handoff

**Piece 1 complete. Pause for owner review before Piece 2.** The redesign uses `feature/rdd-ui-v2`, based on published main `ce2efe6806caca0eeb26530f1edc20e1b31944ac` (0.17.0-test1 / 30). Keep subsequent pieces on this branch in one development session. Piece 1 is a documentation checkpoint with no PR.

## Eight-piece roadmap

The owner approved the visual benchmark and authorised Piece 1. The archive defines screen patterns without listing a numbered roadmap. The sequence below groups the requested areas into eight checkpoints for owner review. Each later piece requires explicit approval.

| Piece | Focus | Expected checkpoint |
| --- | --- | --- |
| 1 | Design foundations | Completed. Publish the original six concepts, benchmark, mandatory standards and recovery notes. |
| 2 | Running | Refine live recording hierarchy, glanceable metrics, trustworthy state, route presentation and safe controls. Review the running entry and Today surfaces as they relate to this flow. |
| 3 | Strength | Refine Gym Now set logging, previous performance, set state and finish or save. Review relevant historical logging interactions and preserve their dates and evidence. |
| 4 | Workout generation | Clarify setup inputs, preview, substitutions and start. Preserve advanced preferences and existing prescription logic. |
| 5 | Bouldering | Refine grade, attempt, outcome and session logging, with scannable recent climbs. |
| 6 | Progress | Refine trends, comparisons and drill-downs across the supported disciplines. Preserve metric definitions and data sources. |
| 7 | Exercise Library, Settings and diagnostics | Refine discovery, grouped preferences, colour controls and readable diagnostic review and export. |
| 8 | Integration and visual acceptance | Review consistency, accessibility and functional regressions across pieces. Run the verification appropriate to the approved delivery. |

## Piece 1 evidence

- The six approved PNGs in [design-reference](design-reference/) retain their original names and bytes. The [benchmark](RACE_DAY_DREAMS_DESIGN_BENCHMARK.md) links each image and describes its intended use.
- [UI design standards](UI_DESIGN_STANDARDS.md) establish the eight principles, screen patterns, eight mandatory review questions and actual-render checklist. [Repository instructions](../AGENTS.md) require both documents.
- Local checks cover archive integrity, PNG decoding, byte-for-byte image comparison, documentation links and the documentation-only diff. Actual app renders and Android/iOS testing belong to later UI pieces.

## Next safe step

Wait for approval. Then fetch and verify the existing branch, confirm the scope of Piece 2 with the owner’s instructions, and capture the current running flow before editing. Use both running concepts and the existing Material 3 design system. Preserve navigation icons and destinations, Dream Routes, branding, user-customisable discipline colours, recording behaviour and data contracts. Keep the [continuation guide](../CONTINUE_CODEX.md) current at meaningful checkpoints.
