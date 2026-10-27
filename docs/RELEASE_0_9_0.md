# Race Day Dreams 0.9.0-test1

Architecture hardening and Strength Engine 2.0 development. Running Coach and Strength
Coach will each work independently, with additional context when an athlete uses both.

## What's new

- Choose Running, Strength or Hybrid emphasis during welcome or later in Settings.
  Strength setup can create a gym programme without a race goal or running profile.
  Focus changes preserve existing goals, programmes, actual workouts and unfinished drafts.
- Feature-owned application operations and persistence boundaries support this work.
  The full Strength Engine 2.0 experience is still in development; remaining phases
  are tracked in CONTINUE_CODEX.md.
- Strength-focused Home leads with the day's gym session, independent logging, actual
  weekly completion, recorded PB evidence and one optional coaching review. A spontaneous
  session never completes a prescription merely because it occurred on the same date.

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
- The next data-foundation checkpoint adds typed, bounded gym execution staging and
  frozen saved workout revisions. Actual-set saves and final programme associations
  are transactional; accepted actual edits preserve the original execution evidence.
  This storage work still requires its own supported-API verification and does not
  yet expose the full Gym Now/live gym interface.

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

- Strength Engine 2.0 product phases remain in development. Do not treat this draft
  checkpoint as the completed release or merge it before all delivery gates pass.
- Real Galaxy S24 outdoor recording, sensors, audio, battery, accessibility and backup
  acceptance remain separate physical checks; no unperformed result is claimed.

## Data, privacy and licensing

Version 0.9.0-test1 / versionCode 12. The new durability source introduces additive
schema 11 and JSON envelope 8 with an independently versioned strength section. Readers
for envelopes 1–7 remain supported. Raw recording archive 2 and complete-backup container
1 remain unchanged. Validation precedes replacement in the shared transaction; a local
unfinished gym workout blocks destructive replacement until it is finished or discarded.
Existing records, plans, gym versions, PBs, favourites, recordings and Dream Route
assignments remain supported. No runtime dependency or permission is added. No account,
backend, advertising, subscription, analytics, Internet permission or runtime LLM is
introduced. Later durable concepts require explicit subsequent migrations and backup tests;
schema 11 will not be silently extended after distribution.

## Verification

Live-main baseline8b75755 passed workflow40330082255 before this increment. Architecture
source38fcc59 then passed240instrumentation cases on each API28/35/36.

The latest fully verified source3566499 passed workflow40598574304 with270instrumentation
cases on EACH API28/35/36, build/lint/domain/schema/source/guidance and actual pinned
formatting. All80authentic captures passed integrity and were adopted from bot7997dca.
Actual Home, StrengthHome light/dark/200% and annualParis2026 were opened and reviewed;
Home is fully loaded and the earlier capture-readiness defect is corrected. API35 APK
artifact12778150145 is48,272,173bytes; screenshots artifact12776884991. These are checkpoint
results, not completion of the entire StrengthEngine2.0 product scope.

Current local checks pass94,641domain checks/26suites,49schema tests,9release-note,
14formatter-harness,7screenshot-validator,11Dreamsource and14artwork-integrity cases.
Strict174/174canonical+14/14routine guidance passes and manifest/attribution checks match.
The next durability source
7f0738c has12newrepository and11newbackup Android cases; its expected293-case matrix is
pending and is not claimed as passed. Full GymNow coordinators/UI and the remaining
StrengthEngine2.0 product scope are still in development. Physical S24 acceptance remains
unperformed.
