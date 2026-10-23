# Geographic fallback clarification

Paris uses the explicitly labelled2024 Olympic marathon course under ODbL, not the
annual Paris Marathon. All five city choices now have sourced geographic revisions;
richer city presentation and final actual-build design acceptance remain open.
See DREAM_ROUTES.md for exact sources, licences and limitations.

# 0.8.1-test1 — design fidelity, guidance and reliability

Implementation checkpoint; not yet a completed/released build.

## What's new

- Original illustrated guidance for all174canonical exercises and14routine movements,
  bundled offline as177efficient WebP assets. Strict catalogue/resource audits reject
  missing guides, instructions or artwork. Actual-build visual review remains separate.
- Five versioned geographic Dream Routes with stable per-activity assignments,
  sourced landmark anchors, original city vignettes and equivalent-distance progress.
  Paris is explicitly the2024 Olympic course; see the clarification above.
- Week-first Plan with day/date rows and attached preparation links. Programme
  versions, rationale, evidence and rebuild actions remain under Programme details.
- A real weekly-distance chart with selectable weeks and accessible descriptions.

## Improvements

- Shared blue/navy guidance surfaces with distinct dark-mode tones, mint tips and
  pink mistake cards; large Back/Next controls for optional movement routines.
- Compact Calendar key; human-readable health summaries with exact evidence/source
  disclosure; shoe summaries with retained detail; clearer Run Now hierarchy.
- Live Coach target-range visual uses existing smoothed evidence and pause state.
  The authoritative execution engine and cue rules remain unchanged.
- Database-backed history pages replace the newest-100 ceiling in workout history.
  Lists query metadata, never raw recording points; details load on demand.

## Fixes

- Session start now saves the staging row, origin and exact prescription together.
- Exact sample/event batch retries are idempotent; conflicting retries, missing
  sequence positions, reversed clocks and reactivation of finished recordings fail
  without replacing retained evidence.
- Incremental recording flushes copy only new points/events rather than repeatedly
  copying a whole multi-hour trace.

## Known limitations

- This checkpoint has illustrated guidance for all 174 canonical exercises and all
  14 built-in routine movements. Strict catalogue and packaged-asset checks pass;
  final actual-build visual acceptance remains required.
- Geographic sources are approximate historical/community data, not certified current
  courses or navigation. London edition is unspecified; Paris is the2024 Olympic
  course, not the annual event. Final screenshot/reference acceptance remains open.
  Existing assignments and backups are preserved.
- Recovery saves the durable interrupted portion; it does not fabricate continuous
  tracking across process death/reboot. The last unflushed batch may still be lost.
- Physical S24 GNSS, Health Connect, BLE, TTS/music, battery and accessibility checks
  remain unverified. Do not treat an emulator run as the physical-device gate.

## Data compatibility

Schema10 adds two metadata-history indexes only. There are no new user-data tables
or destructive changes. JSON envelope7, recording archive2 and complete package1
remain unchanged, retaining supported old readers and transactional restore.
No new runtime dependencies, permissions, Internet access or data uploads.
