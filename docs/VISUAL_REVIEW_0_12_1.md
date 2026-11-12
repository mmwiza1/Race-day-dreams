# Running refinement visual review — 0.12.1-test1

**Final source visual review completed, 12 November 2026.**

Reviewed source `7c4f534e8a02083848807132a88fd3bffc53313d`.
All38new API35 frames were reopened in labelled contact sheets, including light/dark/200%
statistics, route, paused, summaries, splits, ranked records, trends and HR/empty states.
Distance-chart light/dark/200% frames were also opened at native size. Six retained views
were reopened: device-lightHome, Progress, History, monthCalendar, branded200%Home and
darkStrengthHome. Contact sheets are review aids; the canonical PNGs remain unedited.

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
A subsequent measured check found unselected weekly bars too pale in source765:
light1.17:1/dark1.84:1. This corrected the earlier subjective contrast acceptance. The final
source uses the dynamic theme secondary foreground; the existing interaction test now also
requires actual rendered-pixel contrast >=3:1 in light and dark without weakening other assertions.

Final screenshot pixels show:

| Mode | Unselected bar RGB | Surface RGB | Contrast |
| --- | --- | --- | --- |
| Light | 88,94,113 | 244,243,250 | 5.85:1 |
| Dark | 192,198,221 | 26,27,33 | 10.11:1 |
| Branded200% | 83,96,120 | 242,243,250 | 5.73:1 |

These are measured solid fills from actual captures, not a universal guarantee for every
wallpaper palette. Selection remains identifiable by dot/outline and the labelled summary.
Against the references, the time hero, persistent controls, rounded charts and ranked results
provide the intended hierarchy. No blocking clipping, overlap or inaccessible action was
observed in this reviewed set. Scroll-edge content remains reachable by real scrolling.

## Capture integrity and coverage

[Workflow43182469277](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43182469277)
passed EACH API28/35/36:534full tests and421authentic frames on the first attempt for this
source, including reset concurrency and the rendered chart-contrast assertions. Earlier source-specific failures and fixes
are recorded in VERIFICATION.md.
Canonical botf2d501b carries421PNGs matching reviewed rawAPI35artifact13926919951 byte-for-byte.
Strict PNG validation passes. ZIP56,038,884bytes, SHA256
`b9c2f49ab9659209f693fc0b8177cc2ef670413eea1649005e89a86b1f62f7c4`.
Source fingerprint `dd72813b42f73e07b1e291bef014bda72361950bb6d9f34d3a03d50a5addd91a`.

Reviewed new names: every `live-refined-*`, `run-summary-refined*`, `run-splits-refined*`,
`running-records-*`, `running-trends-*` and `running-heart-rate*` frame:38total. This includes
normal/dark/200%, empty route/HR, paused, selected split, narrow clock/metrics and large-text tails.
Source equality and a passing validator are integrity checks; the direct pixel/reference review
above is the separate design gate. Exact delivery-head/mainCI/publication remain delivery checks.

Real GalaxyS24/Pixel/Xiaomi, humanTalkBack, realGPS/provider/BLE/locked-screen/audio/battery and
rotation/system-navigation acceptance remain unperformed. The existing synthetic short-wide
blank platform navigation strip remains a documented physical/rotation acceptance limitation.
