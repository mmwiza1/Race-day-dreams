# Race Day Dreams 0.11.0-test1

Gym, Health & Recovery Intelligence is in development. This document records implemented
behaviour only; the complete agreed release is not yet ready for review.

## What's new

- Optional Gym Now rest timer with pause/resume, reset, adjustment and skip. Prescribed rest
  is guidance; it never blocks saving a set. Completion notifications are opt-in.
- Optional warm-up guidance. Light load examples require recent exact-exercise working-set
  evidence; otherwise the app offers movement preparation without invented weights.
- Manually group adjacent exercises in workout and programme drafts. Superset rounds keep
  actual sets separate and support today-only skips, substitutions and breaking the group.
- Recorded working-set trends, exercise frequency and a transparent primary-area breakdown.
  Secondary involvement stays separate, and unsupported anatomy remains unclassified.
- A recorded-performance page separates actual working sets from bounded single-repetition
  estimates. Historical/manual PB entry and the original progression history remain available.
- A restrained set of local training milestones, backed by retained source references.
  Optional in-app celebrations are off by default; there are no streak penalties.

- Optional recovery guidance from recorded effort and fresh comparable supporting evidence.
  Lighter-week and lower-conflict gym-date previews reuse separate review and activation.

## Improvements

- The accepted0.10.1 GPS, coaching clarity and screenshot lifecycle foundations are retained.

## Fixes

- Single-repetition results stay measured records rather than being duplicated as estimates.
- New gym helpers use the existing Android-9-compatible immutable collection primitives.
- Superset skips remain incomplete targets and never appear as completed actual sets.

## Known limitations

- This is an incomplete development checkpoint. New gym and strength-intelligence screens
  still require their complete final-source API28/35/36 and authentic visual review.
- Recovery/hybrid proposals still need their Android and visual gate; body/protein guidance, retention/storage and the
  catalogue/final-simulation milestones remain to implement. They are not shipped controls.
- Android may delay optional rest notifications during sleep/battery saving. A reboot or
  cross-device restore interrupts timer continuity and requires explicit review.
- Physical Galaxy S24 acceptance remains unperformed, including0.10.1 outdoor GPS checks.

## Data and privacy

- Local-first, account-free operation remains. No Internet permission or backend is added.
- Additive schema16 stores typed rest preferences/countdowns; schema17 adds normalized local
  milestone recognition and sources. Root JSON11 retains older supported readers through10.
  Raw archive2 and complete package1 remain unchanged.
- Timer and warm-up suggestions never become recorded training automatically.

## Verification

- Unchanged baseline8796bc5 passed640893domain checks,64schema tests,9release tests,
  formatting and local Android build/lint/test compilation before implementation.
- Baseline main42046671050 passed438instrumentation cases and155captures per API28/35/36.
- Local current implementation:649731domain checks/43suites,70schema tests,9release-note
  tests and15capture-validator tests; Android debug build/lint/instrumentation compilation pass.
- Gym-only matrix runs exposed unsupported Java collection calls and new fixture entry/reset
  errors; corrections preserve assertions. Complete final-source automated/visual acceptance
  remains pending. Baseline/source results are distinguished in VERIFICATION.md.

- Verified M2 workflow42100006847 passed465tests and188authentic captures per API28/35/36.
  The following recovery/UI-refinement checkpoint still requires its own emulator and visual gate.
