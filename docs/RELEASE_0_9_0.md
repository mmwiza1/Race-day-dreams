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

The live 0.8.2 baseline is main 8b75755a0b74074047408b6444a11db3d6fef6f3, whose
workflow 40330082255 passed. Before architecture edits, local checks passed 61,295 domain
checks across 19 suites, 43 schema tests, 9 release-note tests, 11 Dream source tests,
14 artwork-integrity tests, 6 screenshot-integrity tests and all packaged asset checks.
Architecture source38fcc59 passed workflow40578388101:240instrumentation tests EACH
API28/35/36, build/lint/format and77authentic captures. Phase1localchecks:61,369domain
checks/20suites and43schema tests. Phase2localchecks now pass91,439/21suites; its
Androidgate and all subsequent product phases remain pending. These checkpoint results
do not claim the full StrengthEngine2.0 release is complete.

The Phase3 focus implementation adds 19 domain checks (91,458 across 22 suites) and
eight Android cases for onboarding, focus changes, legacy race drafts, SQLite and backup
rollback. Those Android cases are awaiting actual supported-API CI execution. The focus
adapter uses the existing settings table and preserves schema/backup versions.
