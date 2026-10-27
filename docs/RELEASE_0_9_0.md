# Race Day Dreams 0.9.0-test1

Architecture hardening and Strength Engine 2.0 development. Running Coach and Strength
Coach will each work independently, with additional context when an athlete uses both.

## What's new

- This checkpoint establishes feature-owned application operations and persistence
  boundaries for the Strength Engine 2.0 work. It does not yet deliver the full new
  standalone Strength experience; the remaining phases are tracked in CONTINUE_CODEX.md.

## Improvements

- Running, strength, progress, health and settings operations share one coherent read
  model and serialised command boundary, with feature-specific ownership.
- Activity, result, shoe, baseline, settings and backup repositories retain one shared
  SQLite database and transactional restore boundary.
- Feature navigation retains the existing destinations and back-stack behaviour.
- Recording execution, cues and sensor wiring have separate testable coordinators
  underneath the existing authoritative foreground service.

## Fixes

- No gym-programming behaviour fix is claimed in the architecture checkpoint. The
  confirmed six-week boundary, lower-session variation and fatigue-context defects
  are the next separate phase.

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
Architecture compile/lint/instrumentation results must be recorded as they are observed.
