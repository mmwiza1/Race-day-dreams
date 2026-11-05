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

## Improvements

- The accepted0.10.1 GPS, coaching clarity and screenshot lifecycle foundations are retained.

## Fixes

- Single-repetition results stay measured records rather than being duplicated as estimates.
- New gym helpers use the existing Android-9-compatible immutable collection primitives.
- Superset skips remain incomplete targets and never appear as completed actual sets.

## Known limitations

- This is an incomplete development checkpoint. New retention cleanup/restore/UI require their
  current-source Android and visual gate. Catalogue and final athlete simulations remain to finish.
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

- The unchanged0.10.1 baseline was tested before implementation and its history is preserved.
- Recovery/body source7dc0b56 passed [complete Android verification](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42115688372):486tests and203authentic captures EACH API28/35/36.
  All203API35original PNG identities match canonical6a3eb10. Recovery/body and refreshed
  records/milestones were directly reviewed in light/dark/200% text.
- Following read-only inventory and consent-expiry safeguards pass658601domain checks across
  45suites,72schema/migration tests,9release-note,15capture-validator,11route-source and14artwork
  tests, pinned formatting and strict174exercise/14routine guidance checks. Actual Android debug
  build, lint and instrumentation compilation pass locally. The four new database tests require
  the following full490/API runtime gate before their acceptance.
- The verified intermediate API35 debug APK is available in [workflow artifacts](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42115688372/artifacts/13457712005).
  It is52,618,954bytes (+1.230% versus0.10.1), with SHA256
  `8fd6c063f413e851d0f7c79380141f35ae390acc4538ed71cb5409dcd7815388`.
  This is not the final0.11 delivery. No physical-device acceptance is claimed.

- Pre-retention workflow42120919432 passed490tests/API28/35/36 and203captures/API. Following
  retention source passes local658610domain checks,75schema,9release,16capture-validator,
  formatting and actual debug build/lint/test compilation. Its506test/215capture Android matrix
  is still required; no cleanup runtime or visual acceptance is implied by compilation.
