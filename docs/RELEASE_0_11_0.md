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

## Improvements

- The accepted0.10.1 GPS, coaching clarity and screenshot lifecycle foundations are retained.

## Fixes

- No additional runtime correction is claimed at this checkpoint.

## Known limitations

- This is an incomplete development checkpoint. New gym controls still require the full
  API28/35/36 and authentic screenshot review. Analytics/PBs/achievements, recovery/hybrid,
  body/protein guidance, retention and catalogue milestones remain to implement.
- Android may delay optional rest notifications during sleep/battery saving. A reboot or
  cross-device restore interrupts timer continuity and requires explicit review.
- Physical Galaxy S24 acceptance remains unperformed, including0.10.1 outdoor GPS checks.

## Data and privacy

- Local-first, account-free operation remains. No Internet permission or backend is added.
- Additive schema16 stores typed rest preferences/countdowns. Root JSON10 retains older
  supported backup readers; raw archive2 and complete package1 remain unchanged.
- Timer and warm-up suggestions never become recorded training automatically.

## Verification

- Unchanged baseline8796bc5 passed640893domain checks,64schema tests,9release tests,
  formatting and local Android build/lint/test compilation before implementation.
- Baseline main42046671050 passed438instrumentation cases and155captures per API28/35/36.
- New-release automated and visual acceptance remains pending.
