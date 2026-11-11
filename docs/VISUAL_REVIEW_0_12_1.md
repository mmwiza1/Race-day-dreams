# Running refinement visual review — 0.12.1-test1

**Visual/source gate PASS**,11November2026, source
`fd3f1afbc046e0706e911484331f718d8677ba43`.
All38new API35 frames were opened in labelled contact sheets; light/dark/200% route frames
were additionally opened at native size. Six retained views were reopened: device-lightHome,
Progress, History, monthCalendar, branded200%Home and darkStrengthHome.

## Comparison with the owner's references

The three original inline references remain separate from actual app captures. Their targets
are Expressive prominent statistics/rounded selected charts, immersive time-led live Stats/Map,
and ranked recorded results/trends/HR. Their branding, icons, navigation and colours are not copied.

- Live Statistics leads with elapsed time, then real workout progress and readable distance,
  pace and optional HR. Paused state clearly changes the action to Resume. Fixed Finish is
  separate and retains confirmation. Dark surfaces and semantic dynamic/branded colours are
  cohesive with the existing app; the reference's pink is not copied.
- Route mode emphasises genuine saved geometry, with blank GPS gaps and distinct start/latest
  shapes. It honestly labels the offline outline as having no street map. No streets, tiles or
  coordinates are fabricated to imitate the reference. Recording controls remain accessible.
- Trends use rounded selected bars, axes, dates, visible values and week selection. Pace has a
  labelled line. Recorded zero, unknown data and no-run weeks remain distinct. Ranked records
  expose times, pace, dates and source; recorded actuals are not estimates. HR shows saved
  per-run summaries and missing-data states, without invented personal zones or zone pace.
- At200%, cards stack and scroll naturally. The320dp long10:28:14 clock remains intact and
  controls remain within the viewport. Third-ranked result, HR chart tail and selected split
  have meaningful captures. Content at a viewport edge is scrollable rather than truncated
  inside a fixed field. No blocking horizontal overflow or inaccessible fixed action was found.
- Retained Home/Plan/Calendar/Progress/Strength icons and ordering match the existing shell.
  CoachActivity/navigation source is byte-identical to main. No unrelated navigation is copied.

## Found issue and verified correction

The prior52774f61 loop capture exposed a real presentation issue: the latest-point ring hid
  the start square when coordinates coincided. The start outline now surrounds and is drawn
  after the latest ring. Native corrected light/dark/200% frames show both shapes clearly,
  with no coordinate displacement, stored-data change or recorder command.
No further blocking layout, contrast or missing-primary-action issue was found in the reviewed set.

## Capture integrity and coverage

[Workflow43110530782](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43110530782)
passed EACH API28/35/36:533full tests and421authentic frames. The API36 pre-test Launcher ANR
failed closed; one investigated fresh-runner retry passed with unchanged source/assertions.
Canonical bot1743a7b carries421PNGs matching reviewed rawAPI35artifact13897288491 byte-for-byte.
Strict PNG validation passes. ZIP56,027,725bytes, SHA256
`4ec003cd83dcf13bd66c3c74dbb9b135ad48be91bb6d59e415eda7364c967f03`.
Source fingerprint `b6b0545d473fda91f629ef3907f998724451f83b6c48002d03921173bcfd3e89`.

Reviewed new names: every `live-refined-*`, `run-summary-refined*`, `run-splits-refined*`,
`running-records-*`, `running-trends-*` and `running-heart-rate*` frame:38total. This includes
normal/dark/200%, empty route/HR, paused, selected split, narrow clock/metrics and large-text tails.
Source equality and a passing validator are integrity checks; the direct pixel/reference review
above is the separate design gate. Exact delivery-head/mainCI/publication remain delivery checks.

Real GalaxyS24/Pixel/Xiaomi, humanTalkBack, realGPS/provider/BLE/locked-screen/audio/battery and
rotation/system-navigation acceptance remain unperformed. The existing synthetic short-wide
blank platform navigation strip remains a documented physical/rotation acceptance limitation.
