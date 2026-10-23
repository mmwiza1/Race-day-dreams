# Development status — 0.7.0-test1

The app recommends. The runner decides. The plan adapts.

## Preserved foundations

Local-first Compose/Java architecture; Health Connect/manual baseline; original
running plans and versions; weekly reviews; runner-approved adaptations; Run Score;
qualified splits/PBs; shoes; reminders; body weight; Calendar List/Week/Month semantics.
No account, Internet permission, analytics, backend or runtime LLM requirement.

## Preserved 0.5 increment

- Direct Samsung scaffolding removed; old preferences normalize to Health Connect.
  No imported activity data is deleted. Schema 6; backup envelope 4 reads versions 1–3.
- 174 canonical exercise IDs, aliases/ranked search, favourites/recents, hide/archive,
  custom definitions, measurement profiles and kg/metres/seconds storage.
- Strength dashboard and exercise detail; source-preserving PB progression; manual
  PB edits/deletion; source-workout edits; timed/distance logging; previous-session
  reference; persistent explicit PB acknowledgement.
- 25 offline original schematic guides with instructions, cues, mistakes, equipment,
  muscles and alternatives. Expert technique review and richer artwork remain desirable.
- Optional original six-week gym blocks, goals/priorities, 1–6 days, equipment/time
  constraints, RPE/RIR guidance, immutable versions, move/skip/reduction previews.
- Progress dashboard; existing-result-first PBs; bounded history filters; run comparison;
  Home summary reusing weekly evidence; pre-run context and one-off move warnings.

## Verification gate

Android build/lint, migrations/backup/UI tests, API 28/35/36 instrumentation, genuine
screenshots and final API-35 APK remain gates. VERIFICATION.md records actual results.

## Deliberate limits

No automatic technical Olympic lifting or task-specific timed/distance progression.
Session length is approximate. Programming is a test-build coaching heuristic, not
an individually validated physiological model or promise of optimal outcomes.

Native recording and GPX now supply provider-independent rich activity evidence
(see the 0.6 section below). FIT, maps and additional structured adapters remain
future work. Health Connect is supported; direct Samsung access is not.

## Test-build limitations to keep visible

History renders bounded results (up to 100 matches per selected date range); narrow
the range to browse older entries. The background snapshot still reads retained
history; database-backed paging is a future performance improvement.

Generated gym moves/skips preserve their prescriptions and reasons in block versions.
The Calendar shows current gym placements; additional old-date gym-edit annotations
remain a follow-up. Existing running moved/skipped/completion semantics are retained.

Automatic time/distance strength progression stays conservative (hold/review);
repetition/load suggestions have explicit runner approval and retained decisions.
Technique figures are original schematics, not individual form assessment.

## Preserved 0.6 Prompt 1 increment

Health Intelligence and native recording merged via PR #7, verified on main
`e381231f8a4c7483dea1c869df2f114a2bf2ba5f`. Optional health records and personal
trends; foreground GNSS service and monotonic metrics; pause/auto-pause/gap policies;
crash staging; BLE HR/real cadence; GPX; separate write-back; companion recording archive
are implemented. This section supersedes earlier statements that GPX/native recording
are future-only boundaries. Hardware behaviour remains subject to TEST_BUILD.md checks.

See docs/RUN_ENGINE.md for genuine limits (recover-finalize rather than continue after
process death; two-file backup; no GPX sensor extensions; no route/HR HC write-back).
Prompt 2 implementation and remaining limits are recorded below.
No direct Samsung integration, map dependency, Internet or account was added.

## 0.7 Prompt 2 increment

Branch codex/live-coach-race-day, PR #8. Deterministic service-owned execution,
qualified cues/local TTS/haptics, corroborated optional recovery alternatives, race
ranges/strategy/official result links, support routines/original figures, persisted
Dream Route equivalents, combined backup and bounded native sample hydration.
Schema8, JSON6 (reads1–5), recording archive2 (reads1), complete ZIP1.

Verification is incremental; consult VERIFICATION.md for actual observed source/run
results. Physical audio/GPS/BLE/OEM battery/accessibility behaviour remains unverified.
Course drawings are original city-inspired schematics, not verified event geometry.
The planner's existing prescriptions remain intact; arbitrary interval authoring,
repeat/restart-rep UI, configurable race checklist and targeted late-run RPE are
not implemented. Richer course data needs a separately verified provenance/licence.
Wear OS, maps/tiles, public release, cloud/accounts/LLM/social remain out of scope.

## 0.8 Today / Run Now / offline polish

See docs/RELEASE_0_8.md for implemented functionality and explicit limits. New work uses
the existing recorder, execution and versioning rules. Dream v2 drawings are original,
course-inspired assets; docs/DREAM_ROUTES.md records factual sources and why unlicensed
exact geometry is not bundled. Known-duration plans respect a saved time budget;
distance-only prescriptions disclose uncertainty rather than inventing pace. Source,
CI, capture and delivery results belong in VERIFICATION.md.
