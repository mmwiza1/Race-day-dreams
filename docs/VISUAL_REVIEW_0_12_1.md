# Running refinement visual review — 0.12.1-test1

## Reviewed source and reference comparison

On 11 November, all **38** new API35 captures from source
`52774f61e32d3b0fe08c6bcb1d60321b864e56ea` were opened in labelled contact sheets, with
native-size inspection of dark live statistics and the narrow 200% long clock. Six retained
views were also opened: device-light Home, Progress, History, month Calendar, branded
200% Home and dark Strength Home. These are genuine production screens with test-only data.

Compared directly with the owner's three inline references:

- Live statistics lead with elapsed time, then real workout progress and legible distance,
  pace and optional HR. Paused state explicitly changes the action to Resume. Fixed Finish
  remains separate and confirmation is retained. Dark surfaces and semantic colours follow
  the existing theme rather than copying the reference's pink.
- Route mode prioritises the saved outline and keeps safe recording controls accessible.
  Missing sections remain blank; the view explicitly says it has no street map. It does
  not invent the reference's streets or require remote tiles.
- Trends use rounded selected bars, labelled axes, visible values and date ranges. Pace
  uses a labelled line; recorded zero, missing values and no-run weeks are distinguished.
  Ranked results show times, pace, dates and source. HR uses recorded per-run summaries;
  unsupported personal zones are honestly unavailable rather than fabricated.
- At 200%, cards stack and scroll, long times remain intact, selected splits and the third
  ranked result are reachable, and HR chart labels remain readable. Partial content at a
  viewport edge is scrollable, not a clipped fixed-height field. Navigation retains the
  Home, Plan, Calendar, Progress and Strength icons/order; source is unchanged.

**Issue found and corrected:** at coincident start/latest coordinates on the loop fixture,
  the latest ring hid the start square. The drawing now uses a larger outlined square drawn
  after the latest ring, without displacing coordinates or changing stored data. New-source
  runtime captures must verify this correction before final visual acceptance.

## Capture and artifact integrity

[Workflow42536379762](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42536379762)
passed API28/35/36: each **533** full instrumentation tests and **421** authentic frames.
Canonical bot `9ecc396` was safely fast-forwarded. All421 committed PNGs match the downloaded
API35 artifact13639819995 byte-for-byte; strict PNG validation passes. ZIP55,892,858bytes,
SHA256 `ef9bda72487a245e1cd5719f84bc3a025eec0aa1788e8a8779a2934ea2cacce6`.
Fingerprint `fd96516246f0867287503ba96c2b8654bc8e69693788cc39c17cf6690ff122f8`.
This is the reviewed pre-marker source; the small correction requires a fresh full matrix,
new canonical fingerprint and final route-image review. **Final acceptance remains pending.**

The reviewed names are every `live-refined-*`, `run-summary-refined*`, `run-splits-refined*`,
`running-records-*`, `running-trends-*` and `running-heart-rate*` frame: 38 in total.
References remain the owner's original inline images, separate from app captures.

Real GalaxyS24/Pixel/Xiaomi, humanTalkBack, realGPS/provider/locked-screen/audio/battery and
rotation/system-navigation acceptance remain unperformed. The baseline synthetic short-wide
blank platform navigation strip remains a documented physical/rotation acceptance limitation.
