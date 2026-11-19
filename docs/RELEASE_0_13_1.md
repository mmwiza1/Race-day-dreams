# Race Day Dreams 0.13.1-test1

The Progress and Strength tabs are now analytics dashboards: your key numbers first, charts you
can tap to inspect, and direct links to the runs and workouts behind them, on Android and iPhone.

## What's new

- Progress opens with distance this week, compared with the same days of last week so a week in
  progress is never compared with a full one.
- Choose a reporting period of 4 weeks, 12 weeks, 6 months or 1 year. Distance, average moving
  pace, longest run and plan completion follow the period; distance is compared with the same
  length of time before it only when your history goes back that far.
- Tap a bar in the weekly distance chart to see that week. Your three most recent runs show
  distance, moving pace, date and where each came from, and open the full run with its splits.
- Strength opens with Gym now, then working sets, recorded workouts, gym programme sessions
  completed out of those scheduled, and new records for the chosen period.
- An estimated one-rep max chart for the exercise you pick (only exercises the existing estimate
  suits). Tap a point to see the week, the estimate and the set it came from, and open that
  workout. Your heaviest actual set is shown separately, never mixed with the estimate.
- Muscle-group workload shares each working set between its main muscle groups without counting
  it twice, lists secondary involvement separately, and shows sets from custom exercises as
  "Unclassified" rather than guessing.

## Improvements

- Your three most recent strength workouts open the workout you actually recorded.
- Other Progress and Strength pages are grouped in one compact "More detail" list; nothing was
  removed.
- Tiles sit two per row on phones and one per row with very large text; charts and the donut
  have spoken descriptions for TalkBack and VoiceOver.
- New records count each improved record once per workout, so two better sets of the same lift in
  one session are one new record.

## Fixes

- No other user-visible behaviour changes in this build; Home, Plan and Calendar are unchanged.

## Known limitations

- Dashboards only use what you have recorded or imported. Runs without a distance or moving time
  are counted but left out of distance and pace, and are labelled as such.
- "6 months" and "1 year" are 26 and 52 whole weeks ending with the current week.
- The iPhone app is still an unsigned engineering build; see `TEST_BUILD.md`.
- Dashboards were checked by automated tests and screenshots on the Android emulator and iPhone
  simulator only. Physical-device, human TalkBack and VoiceOver checks remain separate.
- GitHub cannot replace an APK/checksum pair atomically. If a download crosses an update and its
  checksum differs, download the pair again or use version-specific files.

## Data, privacy and licensing

Nothing new is stored: the dashboards read existing local records only. SQLite schema 19, backup
JSON 13, raw recording archive 3 and complete backup package 1 are unchanged, and permissions,
networking (none), accounts and licences are unchanged.

## Verification

Before merge, the exact final source must pass shared calculation contract tests for periods,
part-week comparisons, missing data and plan rules, new dashboard interaction tests, the full
Android API 28, 35 and 36 matrix with light, dark, large-text and empty-state screenshots, and the
iPhone simulator build, startup and UI tests plus an unsigned iPhone device build. Final build
identity is injected below by the publisher; physical-device acceptance remains separate.

## Previous release highlights

- 0.13.0-test1: one shared Kotlin codebase for Android and an experimental, unsigned iPhone app,
  with training, history and backups carried over unchanged.
