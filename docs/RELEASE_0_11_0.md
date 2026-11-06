# Race Day Dreams 0.11.0-test1

Gym, Health & Recovery Intelligence adds practical gym tools, evidence-backed training insight
and explicit control over long-term recording detail. Running, Strength and Hybrid remain
independently useful, private and account-free.

## What's new

- Optional Gym Now rest timers with start, pause, resume, reset, adjustment and skip. Rest is
  guidance, never a condition for saving your next set. Sound and vibration are optional.
- Optional warm-up suggestions. Load examples require recent working sets for the exact exercise;
  otherwise preparation guidance avoids invented weights. Only saved actual sets become history.
- Supersets in workout and programme drafts, with ordered rounds, independent actual sets,
  today-only substitutions, skipped movements and the option to break the group.
- Strength trends based on recorded working sets, with explained primary-area attribution and
  separate secondary involvement. Warm-ups and unsupported custom anatomy are not fabricated volume.
- Recorded performance and history clearly distinguish actual lifts, repetitions, holds and
  distances from bounded estimated one-repetition results. Original historical PB entry remains.
- A small set of local, evidence-backed training milestones. Recognition survives backup/restore;
  celebrations are optional and there are no streak penalties.
- Conservative recovery guidance and optional lighter-week or lower-conflict Hybrid schedule
  previews. Keeping your programme is valid; saving and activating changes remain separate.
- Optional Health Connect height and weight history, manual metric/imperial entry and source
  labels. General adult protein guidance is off by default and requires a chosen recent weight.
- Storage management with 24-month, five-year and Keep forever choices. Existing users keep
  historical detail until informed acknowledgement. Saving a policy never deletes data: a fresh
  preview and separate confirmation precede bounded, transactional cleanup.
- 220 fully illustrated canonical exercises, including 46 useful additions. New options include
  beginner pushing, hip machines and cable work, supported single-leg training, shoulder control,
  timed holds and carries. All 14 supporting routine movements retain illustrated guidance.

## Improvements

- Clear instructions lead; evidence and calculations appear behind working disclosures.
- Every new exercise has explicit equipment, movement and measurement metadata, searchable aliases,
  original technique artwork, written cues, common mistakes and accessible descriptions.
- Busy-equipment alternatives include floor presses without assuming a free bench. Different
  exercises retain independent load evidence; exclusions and historical actuals remain intact.
- Storage figures distinguish logical raw payloads from physical SQLite files. Activities explain
  removed detail honestly; compatible original archives can restore proven matching evidence.
- The accepted GPS, running plans, strength programming, Gym Now and reviewed-proposal architecture
  remain in place. No new runtime dependency or Internet permission is introduced.

## Fixes

- Single-repetition actual results are not duplicated as estimated maximal lifts.
- Warm-ups cannot qualify as working-set volume, progression evidence or normal working-set PBs.
- Superset skips remain incomplete targets rather than completed actual sets.
- Screenshot fixtures restore the effective font scale and await real Android configuration,
  preventing large-text state from leaking into later navigation tests.

## Known limitations

- Galaxy S24 acceptance is unperformed, including outdoor GPS accuracy, locked-screen battery
  behaviour, notification/audio interaction, real Health Connect providers and human TalkBack.
- Android may delay optional rest alerts during sleep or battery saving. Reboot or cross-device
  restore interrupts timer continuity and requires explicit review.
- Removed raw routes cannot be reconstructed from totals. Export a suitable complete backup first;
  deleting SQLite rows does not necessarily reclaim device storage immediately.
- Protein guidance is general adult information, not a medical prescription. Individual medical
  or dietary circumstances may change requirements. No fresh chosen weight means no numerical range.

## Data and privacy

Additive migrations advance SQLite to schema 19. Root JSON backup 13 reads supported older roots
1–12; raw recording archive 3 reads formats 1–3; complete package 1 is unchanged. Typed retention
preferences, consent and removal receipts are backed up. Malformed, conflicting and future-format
restores fail before destructive replacement or roll back transactionally.

Cleanup removes only eligible finished app-owned dense detail. Summaries, actual gym sets, PBs,
race results, programmes, decisions, milestones and ordinary body observations remain. Active,
paused and recoverable recordings are protected. No Health Connect provider record is deleted.

The app remains local-first, free and account-free, with no Internet permission, backend,
advertising, analytics, subscriptions or runtime LLM. Optional READ_HEIGHT is the only new body
permission; there is no background Health Connect access.

## Verification

Source `ee760bf` passed [the complete software workflow](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42148419418):
508 instrumentation tests and 359 authentic captures on each Android API 28, 35 and 36;
700,108 domain checks across 47 suites; 75 schema/migration tests; complete illustrated guidance
for all 220 canonical exercises and 14 routine movements; 223 reviewed artwork hashes; and
pinned formatting, lint, build, source and release-note checks.

The three six-month athlete histories pass 4,226 assertions. Actual screenshots were inspected
in light, dark and 200% modes, including complete scrolled guide mistakes and cleanup confirmation.
The verified feature API 35 APK is 55,250,287 bytes, an increase of 6.2923% over the verified
0.10.1 CI package. New exercise artwork adds 2,435,370 bytes.

Exact final-head and main CI, and development publication, are tracked in VERIFICATION.md and
PR #18. No physical acceptance or already-published main artifact is implied. See
VERIFICATION.md, docs/VISUAL_REVIEW_0_11.md and TEST_BUILD.md for evidence and the remaining
physical-device checklist. Synthetic histories validate software invariants; they do not
establish physiological outcomes or outdoor GPS accuracy.
