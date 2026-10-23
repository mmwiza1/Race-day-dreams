# Race Day Dreams 0.8.1-test1

Design fidelity, complete exercise guidance and recording reliability.
This is a test release in preparation, not a Play Store production release.

## What's new

- Original illustrated guidance for all 174 canonical exercises and all 14 supporting
  routine movements. Every guide includes movement-specific instructions, cues,
  mistakes and accessible descriptions. All artwork is bundled offline.
- Five sourced geographic Dream Route revisions with stable per-activity assignments,
  equivalent-distance progress, numbered landmarks, original city vignettes and bundled
  street/water/green-space context. No runtime tile client or map SDK is needed.
- A week-first Plan with clear day/date rows and attached preparation links. Programme
  history, rationale and rebuild controls remain under Programme details.
- A selectable weekly-distance chart using actual recorded evidence.

## Improvements

- Blue/navy guidance surfaces, mint tip cards and pink mistake cards, with responsive
  layouts and large routine controls.
- Compact Calendar key, human-readable Health Intelligence summaries with evidence
  disclosure, concise shoe cards and clearer Run Now hierarchy.
- Live Coach target-range presentation uses existing smoothed evidence and pause state.
  The authoritative recorder, execution state machine and cue rules are preserved.
- Database-backed history paging replaces the newest-100 ceiling. List cards query
  metadata; raw recording samples load only for details.
- Large-font landmark labels stack, and navigation retains accessible icon actions.
- Version-tag and rolling development-release workflows package verified debug APKs,
  checksums and maintained human-readable notes after required automated checks pass.

## Fixes

- Recording start saves staging, origin and exact prescription atomically.
- Identical sample/event retries are idempotent; conflicting retries, sequence holes,
  reversed clocks and finished-session reactivation fail without replacing evidence.
- Incremental flushes copy only newly accepted samples/events, rather than copying
  an entire multi-hour recording again.
- History readiness remains pending while its parent source is refreshing.
- Dream Route lookups reuse immutable geometry instead of rebuilding coordinates for
  every card or redraw. Captures align semantic scroll containers deterministically.

## Known limitations

- Geographic data are approximate historical/community lines, not certified current
  courses or navigation. London edition and survey accuracy are unspecified.
- **Paris is explicitly the 2024 Olympic course, not the annual Paris Marathon.**
  No redistribution grant was established for an annual-course candidate. This
  geographic fallback is labelled in the primary UI and its source information.
- Recorder recovery preserves the durable interrupted portion. It does not fabricate
  uninterrupted tracking after process death/reboot; the last unflushed batch can
  still be lost.
- Physical Galaxy S24 GNSS, Health Connect, BLE, TTS/music, battery, TalkBack and
  meaningful outdoor-run acceptance remain unverified. Follow TEST_BUILD.md.
- Final actual-build visual acceptance remains a separate gate; completeness and
  screenshot consistency alone do not certify design fidelity. PR #10 stays draft
  until the full requested definition is satisfied.

## Data compatibility

Schema 10 adds two metadata-history indexes only, with no destructive migration.
JSON envelope 7, recording archive 2 and complete package 1 remain unchanged.
Supported old backups remain readable with transactional restore. Every stored Dream
Route assignment keeps its original geometry; preferences affect future runs only.

No new runtime dependencies, permissions, Internet access, tracking, accounts or data
uploads are introduced. Original artwork and application code are MIT; individual
geographic datasets retain their documented MIT, CC0 or ODbL licences.

### Offline city presentation follow-up

All five geographic Dream Routes now include sourced street, green-space and water
context with original light/dark styling. The data are bundled, so this works offline
without map downloads, a map SDK or Internet permission. Sources/ODbL rights are
recorded and credited. Actual full-viewport captures have been compared against the references. A final
Tokyo distance-label-placement follow-up still needs its fresh capture review.
Physical GalaxyS24 acceptance remains unverified; PR#10 stays draft.

## APK size at the verified geographic checkpoint

Source745b148/API35debugAPK:46,006,970bytes. Verified0.8.0baseline:34,566,254bytes;
increase11,440,716bytes(33.10%), including complete original exercise art and offline
geographic context. No runtime dependency or Internet permission added. Refresh final
size after any source follow-up. This is a debug/test build, not production signing.
