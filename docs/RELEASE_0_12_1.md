# Race Day Dreams 0.12.1-test1

A focused refinement of live running and recorded performance, building on the shared
Material You design. Development/test build.

## What's new

- Switch between elapsed-time-led running statistics and an optional offline outline of your accepted recorded route.
- Inspect four, eight or twelve weeks of recorded distance, elapsed pace, time and saved heart-rate readings.

## Improvements

- Larger live time and distance, readable pace/optional heart rate, real workout-step progress and existing pause/resume/Finish confirmation.
- Rounded selectable charts, keyboard-friendly week controls and clear missing-data states.
- Ranked retained running results preserve dates and source precedence; run summaries and splits gain clearer hierarchy.
- Existing navigation icons, labels, destinations and order remain unchanged.

## Fixes

- Preserve the existing authoritative best when several results have identical times.
- Keep workout-history filters within the scrolling viewport at large text sizes.
- Honour the selected distance units when displaying history pace.
- No GPS, coaching or historical-data recalculation is introduced.

## Known limitations

- Implementation and verification are in progress; no final screenshot or software pass is claimed.
- Heart-rate zones require supported individual boundaries and qualified sample evidence. They are not inferred from a run average or copied from a design reference.
- Galaxy S24, Pixel, Xiaomi, outdoor GPS and human TalkBack acceptance require hardware; none is claimed.

## Data, privacy and licensing

The existing recording, health, coaching, persistence, backup and retention systems remain
the foundation. No dependency, permission, account, network service or artwork is introduced.
Existing navigation icons, labels, ordering, destinations, branding and launcher icon remain.

## Verification

The merged 0.12 baseline passed its supported software matrix. This patch must pass the full
required software and authentic screenshot gates before delivery. See VERIFICATION.md.
