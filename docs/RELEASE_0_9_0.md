# Race Day Dreams 0.9.0-test1

Architecture Hardening + Standalone Strength + Gym Now + Exercise Discovery, Exclusions
and Smart Substitution. Running and Strength can be used independently.

## What's new

- Choose Running, Strength or Hybrid emphasis during welcome or later in Settings.
  Strength setup can create a gym programme without a race goal or running profile.
  Focus changes preserve existing goals, programmes, actual workouts and unfinished drafts.
- Feature-owned application operations and persistence boundaries support this work.
  The retained 0.9 scope and remaining release gates are tracked in CONTINUE_CODEX.md.
- Strength-focused Home leads with the day's gym session, independent logging, actual
  weekly completion, recorded PB evidence and one optional coaching review. A spontaneous
  session never completes a prescription merely because it occurred on the same date.

- Browse exercises by body area, movement and equipment, with personal shortcuts and relevance-first search. Today-only swaps explain alternatives and preserve original targets and actual sets; reference loads are cleared when changing exercises.

- Exclude exercises from future programme/alternative selection and restore them at any
  time from Exercises → Excluded. Existing targets, history, favourites and PBs remain.

## Improvements

- Running, strength, progress, health and settings operations share one coherent read
  model and serialised command boundary, with feature-specific ownership.
- Activity, result, shoe, baseline, settings and backup repositories retain one shared
  SQLite database and transactional restore boundary.
- Feature navigation retains the existing destinations and back-stack behaviour.
- Recording execution, cues and sensor wiring have separate testable coordinators
  underneath the existing authoritative foreground service.
- All 174 canonical exercises now have explicit movement/body-area, equipment-setup
  and experience metadata for later Strength selection work. Existing guidance, actual
  sets, measurements and PB history are unchanged; programme selection is not altered
  by this metadata checkpoint.
- Typed, bounded gym execution staging retains
  frozen saved workout revisions. Actual-set saves and final programme associations
  are transactional; accepted actual edits preserve the original execution evidence.
  This storage work passed its supported-API verification. The full Gym Now/live gym
  interface now connects planned, quick, custom, favourite, repeat and generated
  workouts through one durable engine; its new end-to-end verification is pending.
  Targets never prefill actual sets. Interrupted stages require explicit recovery.

## Fixes

- Six-week gym blocks contain six complete programme weeks anchored to the selected
  start date. Starting mid-week no longer creates an extra calendar-week allocation.
- Upper/Lower blocks use distinct squat and hinge emphases. Scheduling recognises
  meaningful lower-body work in Full Body sessions, including context immediately
  after a demanding run, and explanations describe the exercises actually retained.
- Saving focus uses persistent Settings status rather than a transient confirmation
  that could cover the next action or reappear after rotation.
- Strength Today uses collection calls compatible with the supported Android runtimes;
  the API 28 instrumentation gate guards the new summary and metadata initialisation.
- Consecutive lower-body sessions are considered across programme-week boundaries;
  unavoidable spacing is explained without removing requested sessions.
- Dated progression requires both supporting sessions to be recent and nonfuture.
  Missing effort and one successful session still do not justify a load increase.
- Hybrid Today shows both same-day disciplines, and Strength-focused Plan stays
  gym-first when a previous running profile is retained. Running remains accessible.

## Known limitations

- The draft remains gated on retained 0.9 completion and automated verification. Full
  programme builders, mesocycles, rest timers, supersets, catalogue expansion and advanced
  Strength Engine 2.0 analytics/programming are deferred to later releases.
- Real Galaxy S24 outdoor recording, sensors, audio, battery, accessibility and backup
  acceptance remain separate physical checks; no unperformed result is claimed.

## Data, privacy and licensing

Version 0.9.0-test1 / versionCode 12. The new durability source introduces additive
schemas 11–13 and JSON envelope 8 with strengthEngine section 2 (reads section 1). Exercise choices section 2 retains explicit exclusions and reads old section 1. Readers
for envelopes 1–7 remain supported. Raw recording archive 2 and complete-backup container
1 remain unchanged. Validation precedes replacement in the shared transaction; a local
unfinished gym workout blocks destructive replacement until it is finished or discarded.
Existing records, plans, gym versions, PBs, favourites, recordings and Dream Route
assignments remain supported. No runtime dependency or permission is added. No account,
backend, advertising, subscription, analytics, Internet permission or runtime LLM is
introduced. Later durable concepts require explicit subsequent migrations and backup tests;
published schemas will not be silently extended after distribution.

## Verification

Live-main baseline8b75755 passed workflow40330082255 before this increment. Architecture
source38fcc59 then passed240instrumentation cases on each API28/35/36.

The latest fully verified source77fd0b4 passed workflow40601581173 with293instrumentation
cases on EACH API28/35/36, build/lint/domain/schema/source/guidance and actual pinned
formatting, including12repository and11backup cases. All80authentic captures passed
integrity and were adopted from bot09cb5e8; their metadata matches the retained source.
Actual Home, StrengthHome light/dark/200% and annualParis2026 were opened and reviewed;
Home is fully loaded and the earlier capture-readiness defect is corrected. API35 APK
artifact12778274454 is48,321,325bytes; screenshots artifact12778319076. These are checkpoint
results, not completion of the entire StrengthEngine2.0 product scope.

Verified-source checks pass94,641domain checks/26suites,49schema tests,9release-note,
14formatter-harness,7screenshot-validator,11Dreamsource and14artwork-integrity cases.
Strict174/174canonical+14/14routine guidance passes and manifest/attribution checks match.
The synthetic450workout/2250working-set backup round-trip passed on all three APIs,
producing approximately19.1MB. Before/after heap samples are not peaks and do not prove
that every32MiB backup fits memory. Current uncommitted coordinator, one-off-workout,
selection-query and export-budget additions require their own gate. The latter rejects
provably oversized gym data before JSON construction; it is not a full memory guarantee.
Full Gym Now UI and the remaining StrengthEngine2.0 product scope are still in development.
Physical S24 acceptance remains unperformed.

## Explicit workout association storage

Schema 12 adds immutable reviewed Keep as extra / Count toward session decisions and an
indexed target projection. Retries are idempotent, actual edits retain reviewed history,
and replacement is validated before the shared restore transaction. Spontaneous origins
are never rewritten. This backend is pending its supported-API gate; association UI is
not yet enabled. Existing planned-origin completion remains unchanged.
