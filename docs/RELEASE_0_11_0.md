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

- Explicit recording-detail retention choices, protected existing-user defaults, preview and
  separate cleanup confirmation. Permanent workout/PB/programme history stays. Storage estimates
  distinguish raw payload bytes from physical database files; compatible raw backups can restore
  proven original detail.

- Forty-six fully illustrated catalogue additions cover hip machines, cable hip work, beginner
  pushing, chair sit-to-stand, timed core/inner-thigh holds, shoulder-blade control, unilateral
  bridges, a distance-based overhead carry and specific Smith-machine/shoulder-control options, floor presses, supported single-leg work,
  crawling, dedicated posterior-chain machines and distinct shoulder rotation tasks. Support-based
  substitutions explain changed leverage; loads never transfer between different exercises.

## Improvements

- The accepted0.10.1 GPS, coaching clarity and screenshot lifecycle foundations are retained.

## Fixes

- Single-repetition results stay measured records rather than being duplicated as estimates.
- New gym helpers use the existing Android-9-compatible immutable collection primitives.
- Superset skips remain incomplete targets and never appear as completed actual sets.

## Known limitations

- This is an incomplete development checkpoint. New retention cleanup/restore/UI require their
  current-source Android and visual gate. Final220-catalogue Android screenshots and visual review remain to finish; three six-month
  athlete simulations already pass4226domain assertions.
  This is not a software-complete0.11 release.
- Android may delay optional rest notifications during sleep/battery saving. A reboot or
  cross-device restore interrupts timer continuity and requires explicit review.
- Physical Galaxy S24 acceptance remains unperformed, including0.10.1 outdoor GPS checks.

## Data and privacy

- Local-first, account-free operation remains. No Internet permission or backend is added.
- Additive schema16 stores typed rest preferences/countdowns; schema17 adds normalized local
  milestone recognition and sources. Schema18 adds typed height observations and optional guidance
  preferences. Schema19 adds typed retention consent and removal receipts. Root JSON13 retains
  supported older readers through12; raw archive3 reads1–3. Complete package1 is unchanged.
  See DATA_LIFECYCLE_0_11.md for explicit bounds and raw-versus-summary backup semantics.
- Timer and warm-up suggestions never become recorded training automatically.
- Optional READ_HEIGHT is the only new body-feature permission. No background health access or
  runtime dependency is introduced. General protein ranges are not medical prescriptions.

## Verification

- Local220-guide source passes700107domain checks across47suites,75schema/migration tests,
  9release-note tests,17capture-validator tests,11route-source tests and14artwork tests.
  Strict220/220canonical and14/14routine guidance and all223original asset hashes pass.
- Three deterministic six-month athlete histories pass4226integration assertions using
  production domain calculations. They validate missing evidence, conservative advice,
  explicit user choice and retained history; they do not claim physiological outcomes.
  See ATHLETE_SIMULATIONS_0_11.md.
- The complete retention implementation passed506tests and215captures on each API28/35/36
  at sourcee39a308 (workflow42133056947). Source2cc10d0 passed508tests/296captures on API28
  and36; API35had one font-fixture restoration failure, now corrected with an explicit normal
  scale. No assertion or production training rule was relaxed.
- Final220-source508/API Android gate,359captures/API, direct visual review, comparable APK
  size and final rolling development artifact remain pending. Earlier checkpoints do not
  establish final release acceptance. Physical GalaxyS24 checks remain unperformed.
