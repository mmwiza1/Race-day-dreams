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

## Known limitations

- Strength Engine 2.0 product phases remain in development. Do not treat this draft
  checkpoint as the completed release or merge it before all delivery gates pass.
- Real Galaxy S24 outdoor recording, sensors, audio, battery, accessibility and backup
  acceptance remain separate physical checks; no unperformed result is claimed.

## Data, privacy and licensing

Version 0.9.0-test1 / versionCode 12. The behaviour-preserving architecture phase retains
schema 10, JSON envelope 7, raw recording archive 2 and complete-backup package 1.
Existing records, plans, gym versions, PBs, favourites, recordings and Dream Route
assignments remain supported. No runtime dependency or permission is added. No account,
backend, advertising, subscription, analytics, Internet permission or runtime LLM is
introduced. Future persistence changes require explicit migrations and backup tests.

## Verification

Live-main baseline8b75755 passed workflow40330082255 before this increment. Architecture
source38fcc59 then passed240instrumentation cases on each API28/35/36.

The latest execution-foundation checkpoint171a210 passed workflow40595241800 with255
instrumentation cases on EACH API28/35/36, build/lint/domain/schema/source/guidance and
actual pinned formatting (36Kotlin+44Javafiles). The80newauthentic captures are published;
their exact-image visual review remains to be recorded. API35 checkpoint APK
artifact12777070406 is48,239,405bytes; screenshots artifact12775783471. These are checkpoint
results, not completion of the entire StrengthEngine2.0 product scope. Earlier standalone
StrengthHome light/dark/200% and representative RunningHome/Plan/Paris renders from the
253-case checkpoint were reviewed separately.

Local checks also passed strict174/174canonical+14/14routine guidance,43schema tests,
9release-note tests,11Dreamsource tests,14artwork-integrity and7screenshot-validator cases.
The GymNow domain foundation adds169checks (94,241across25suites) and two Android runtime
cases, now passed on all supported APIs. Full GymNow storage, backup and UI wiring remain
in development. Formatter convergence handling passes14harness cases and actual pinned-tool
CI. Later scheduling, strict JSON codec and progression-recency changes require their own
verification. Physical S24 acceptance remains unperformed.
