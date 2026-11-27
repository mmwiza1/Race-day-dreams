# UI Redesign v2 handoff

**Piece 3 is complete. Pause for owner review before Piece 4.** The redesign uses `feature/rdd-ui-v2`, based on published main `ce2efe6806caca0eeb26530f1edc20e1b31944ac` (0.17.0-test1 / 30). Keep subsequent pieces on this branch in one development session. The branch has no PR.

## Eight-piece roadmap

The owner approved Design Foundations, Themes and Shared Components, and Strength Experience as Pieces 1–3. The archive defines screen patterns without listing a numbered roadmap. The remaining sequence is proposed for review. Each later piece requires explicit approval.

| Piece | Focus | Expected checkpoint |
| --- | --- | --- |
| 1 | Design foundations | Completed. Publish the original six concepts, benchmark, mandatory standards and recovery notes. |
| 2 | Themes and shared components | Give Progress its own portable accent, update the default identities, and refine shared component layout and states. |
| 3 | Strength experience | Refine historical and planned logging, Gym Now, previous performance, draft and saved sets, exercise replacement context and workout review. |
| 4 | Running | Proposed. Refine live recording hierarchy, glanceable metrics, trustworthy state, route presentation and safe controls. Review the running entry and Today surfaces as they relate to this flow. |
| 5 | Bouldering | Refine grade, attempt, outcome and session logging, with scannable recent climbs. |
| 6 | Progress | Refine trends, comparisons and drill-downs across the supported disciplines. Preserve metric definitions and data sources. |
| 7 | Supporting flows | Refine workout generation, Exercise Library, Settings and diagnostics within the approved scope. |
| 8 | Integration and visual acceptance | Review consistency, accessibility and functional regressions across pieces. Run the verification appropriate to the approved delivery. |

## Piece 1 evidence

- The six approved PNGs in [design-reference](design-reference/) retain their original names and bytes. The [benchmark](RACE_DAY_DREAMS_DESIGN_BENCHMARK.md) links each image and describes its intended use.
- [UI design standards](UI_DESIGN_STANDARDS.md) establish the eight principles, screen patterns, eight mandatory review questions and actual-render checklist. [Repository instructions](../AGENTS.md) require both documents.
- Local checks cover archive integrity, PNG decoding, byte-for-byte image comparison, documentation links and the documentation-only diff. Actual app renders and Android/iOS testing belong to later UI pieces.

## Piece 2 changes

- Progress has an independent green accent and Settings defaults to amber. Running follows the existing blue branded or Material You primary. Strength uses violet and Bouldering plum. Explicit selections, including existing green Settings, continue to win. Reset removes only that discipline’s single key.
- The same settings repository and backup envelope carry the optional `appearance.accent.progress` key. Older backups without it use the default. Android and iOS apply the preference to the Progress page and training trends. Chart metrics keep their individual hues and semantic warnings use the error palette.
- Shared headers, panels, key metrics, primary and secondary actions, inputs, selection and disclosure use consistent spacing and colours. Long summaries and values reflow at large text sizes. Loading, empty and error examples reuse the shared components and existing data states.

## Piece 2 verification and visual review

- Android debug app and instrumentation APKs built. Debug lint, pinned formatting for changed in-scope files and handoff links passed. Nine SQLite core backup/restore tests passed. Coverage includes an old export without Progress, preservation of explicit green Settings, five independent choices, reset and unknown stored values.
- Compact Android API 28 passed five focused checks in light and two in dark at 200% text. The checks cover preference selection and relaunch, contrast of the curated palettes in both themes, unchanged chart and warning roles, component interaction and the calendar legend regression. The captures are in the `Race-Day-Dreams-UI-v2-Piece-2-Android.zip` review bundle. The older Settings picker and published large-text Progress screenshot were used for comparison.
- **Task clarity and hierarchy:** Inspected the green Progress title, metrics and primary save button. **Interaction efficiency:** The chooser toggles and resets one discipline at a time, and a separate secondary action stays visible. **Progressive disclosure:** The advanced control starts collapsed and reveals the editable notes field. **Consistency and craft:** Inspected common surfaces, matching text roles and clear selected chips in both themes. **Accessibility:** Metrics and the long title summary reflow at 200%. Enlarged text users can scroll to the input and feedback. **Functional integrity:** The relaunch and backup exercises retain the independent choices, and warnings remain semantically distinct from the active accent.
- Native iOS build and visual checks, manual screen-reader checks and the full Android/iOS matrix remain outside the verified evidence for this checkpoint. The shared UI and the iOS route mapping are included.

## Piece 3 changes

- Historical logging puts the session date and exercise with its last working performance above the set editor. All copied, added or changed sets are labelled as drafts until workout save succeeds. Set editing is direct and removal lives in a set-specific menu. The session summary counts the exercise blocks and sets in the draft.
- Gym Now uses the same exercise header, set row, responsive input layout and disclosure patterns. It displays acknowledged sets separately from the current entry. Optional effort and warm-up stay collapsed until needed. Prescribed and replacement exercise controls keep their existing routes and commands.
- Workout history uses the shared set rows for recorded measurements, warm-up status and entered effort. Save feedback follows the existing success callback. Persistence, timing, programme links, PB calculation, recovery and backup code are unchanged.

## Piece 3 verification and visual review

- Android debug and instrumentation APKs built. Debug lint, focused formatting and diff checks passed. All 67 focused SQLite tests passed across workout execution, associations, core backup and gym backup.
- Nine focused API 28 executions passed across the checkpoint. Light coverage includes historical copying and saving, calendar date and PB evidence, source editing and reopening, Gym Now save and explicit pause/resume, planned completion and duplicate rejection, and original evidence after exercise substitution. Historical copying, Gym Now and historical source editing also passed in dark at 200% text. Reopening confirmed preservation of the original timestamp, duration, set ID and notes.
- Reviewed compact, real Android captures of date entry, last performance, current and draft sets, optional effort, saved sets, completion and history. The before/after bundle is `Race-Day-Dreams-UI-v2-Piece-3-Android.zip`. Historical baseline captures came from the Piece 2 APK. Gym Now baseline captures came from the published canonical gallery. The light and dark Gym Now comparisons use the shared production screen in the test host.
- **Task clarity and hierarchy:** Date, exercise and last performance are grouped above the set workflow, with Add set and Save distinct. **Repeat-task efficiency:** Review and duplication retain draft values, and direct editing plus the set menu keep repetitive actions compact. **Progressive disclosure:** Set options and session details are collapsed by default. **Consistency and visual quality:** Both loggers share neutral rows, accent-led actions and a legible workflow. **Accessibility:** At 200%, inputs stack, effort chips wrap and the editing scroll targets the fields. **Functional integrity:** UI and storage checks cover save, reopen, programme association, duplicate prevention and replacement provenance.
- Native iOS rendering, manual screen-reader checks and the full device matrix remain unverified. The existing PB-history snackbar can briefly accompany the historical save confirmation.

## Next safe step

Wait for owner approval before Piece 4. Fetch and verify the existing branch and follow the owner’s exact scope. If authorised to work on Running, capture the existing flow first and use both running concepts. Preserve navigation, Dream Routes, branding, user-customisable discipline colours, recording behaviour and data contracts. Update the [continuation guide](../CONTINUE_CODEX.md) at meaningful checkpoints.
