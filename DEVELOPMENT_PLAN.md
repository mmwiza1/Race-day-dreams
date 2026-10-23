# Development status — 0.5.0-test1

The app recommends. The runner decides. The plan adapts.

## Preserved foundations

Local-first Compose/Java architecture; Health Connect/manual baseline; original
running plans and versions; weekly reviews; runner-approved adaptations; Run Score;
qualified splits/PBs; shoes; reminders; body weight; Calendar List/Week/Month semantics.
No account, Internet permission, analytics, backend or runtime LLM requirement.

## This increment

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

Provider-independent rich activity data is future work: native recording, GPX/FIT or
structured imports could improve splits/rolling PBs/durability. None of these recorders,
parsers or maps is added here. Health Connect is supported; direct Samsung access is not.

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

## 0.6 Prompt 1 increment (current feature branch)

Health Intelligence and native recording are being verified on
`codex/health-intelligence-run-engine`, PR #7. Optional health records and personal
trends; foreground GNSS service and monotonic metrics; pause/auto-pause/gap policies;
crash staging; BLE HR/real cadence; GPX; separate write-back; companion recording archive
are implemented. This section supersedes earlier statements that GPX/native recording
are future-only boundaries. Hardware behaviour remains subject to TEST_BUILD.md checks.

See docs/RUN_ENGINE.md for genuine limits (recover-finalize rather than continue after
process death; two-file backup; no GPX sensor extensions; no route/HR HC write-back).
Prompt 2 Live Coach/structured execution/Race Day/routes/routines remains deferred.
No direct Samsung integration, map dependency, Internet or account was added.
