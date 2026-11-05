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

- Optional height and weight history with source labels, manual cm/inch and kg/pound entry,
  and independent Health Connect access. General adult protein guidance is off by default and
  requires an explicitly chosen, recent weight; imported measurements never silently replace it.

## Improvements

- The accepted0.10.1 GPS, coaching clarity and screenshot lifecycle foundations are retained.

## Fixes

- Single-repetition results stay measured records rather than being duplicated as estimates.
- New gym helpers use the existing Android-9-compatible immutable collection primitives.
- Superset skips remain incomplete targets and never appear as completed actual sets.

## Known limitations

- This is an incomplete development checkpoint. Retention cleanup/storage and catalogue/final-
  simulation milestones remain to implement; cleanup controls are not shipped. The read-only
  storage inventory never deletes data. This is not a software-complete0.11 release.
- Android may delay optional rest notifications during sleep/battery saving. A reboot or
  cross-device restore interrupts timer continuity and requires explicit review.
- Physical Galaxy S24 acceptance remains unperformed, including0.10.1 outdoor GPS checks.

## Data and privacy

- Local-first, account-free operation remains. No Internet permission or backend is added.
- Additive schema16 stores typed rest preferences/countdowns; schema17 adds normalized local
  milestone recognition and sources. Schema18 adds typed height observations and optional guidance
  preferences. Root JSON12 retains older supported readers through11.
  Raw archive2 and complete package1 remain unchanged.
- Timer and warm-up suggestions never become recorded training automatically.
- Optional READ_HEIGHT is the only new body-feature permission. No background health access or
  runtime dependency is introduced. General protein ranges are not medical prescriptions.

## Verification

- Unchanged baseline8796bc5 passed640893domain checks,64schema tests,9release tests,
  formatting and local Android build/lint/test compilation before implementation.
- Baseline main42046671050 passed438instrumentation cases and155captures per API28/35/36.
- Local current implementation:658597domain checks/45suites,72schema tests,9release-note
  tests and15capture-validator tests; Android debug build/lint/instrumentation compilation pass.
- Gym-only matrix runs exposed unsupported Java collection calls and new fixture entry/reset
  errors; corrections preserve assertions. Complete final-source automated/visual acceptance
  remains pending. Baseline/source results are distinguished in VERIFICATION.md.

- Verified M2 workflow42100006847 passed465tests and188authentic captures per API28/35/36.
  The following recovery/UI-refinement checkpoint still requires its own emulator and visual gate.

- M3 workflow42103872457 ran472cases per API, with two actual failures each: the new capture
  awaited the wrong focus mode, and a reduced-set proposal retained its old time estimate.
  Both are corrected on the following body checkpoint; existing assertions/safeguards remain.
  Its complete486case/203capture matrix must pass before any final-source acceptance claim.

- Recovery/body source7dc0b56 passed complete workflow42115688372:486tests and203authentic
  captures EACH API28/35/36. All203API35rawPNG identities verified against canonical6a3eb10;
  recovery/body and refreshed recorded-results/milestones reviewed in light/dark/200%.
- Following read-only inventory implementation compiles and passes lint locally; its four new
  database tests need the next full Android matrix. No retention deletion is enabled.
