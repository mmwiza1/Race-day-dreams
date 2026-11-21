# Race Day Dreams 0.15.3-test1

A battery and performance build. During a run the app does less repeated background work, and the
Progress dashboards stay quick with years of training history. GPS, heart rate, recording timing
and your data are unchanged. If you already have 0.15.1-test1 or later, Settings › App updates can
install this version over it, keeping your data.

## What's new

- No new features in this build; it is about efficiency and reliability.

## Improvements

- Android: the ongoing "Recording your run" notification is no longer rebuilt and re-posted about
  twice a second. It now changes only when it needs to, such as Pause becoming Resume after a
  manual or automatic pause. If you swipe it away, it comes back.
- Android: while the live route is on screen, the app reads only the GPS points saved since the
  last update, instead of re-reading the whole run from storage every 5 seconds. Long runs keep the
  same route drawing, gaps and start and latest-point markers.
- Weekly distance, running trends, run counts and the Progress dashboard are calculated in one pass
  over your history instead of once per week shown, and runs reported by several apps are grouped
  in a single pass. The figures are exactly the same as before.
- Android: when the app is resumed, a permission result arrives and a screen opens at almost the
  same moment, the app reads your data once instead of several times in a row. A change made
  after a read has started always triggers another read.
- iPhone: a gym workout is saved as a checkpoint when the app moves to the background, and its
  clock stops ticking until you return, as on Android.

## Fixes

- iPhone: the gym workout screen no longer keeps its own once-a-second loop while hidden; it uses
  the shared workout clock that pauses when the app is in the background.

## Known limitations

- No battery saving is claimed: these changes remove measured repeated work, but battery use has
  not yet been measured on a physical phone.
- GPS stays at once per second and the recording wake lock stays on during a manual pause, so that
  Resume and auto-resume stay instant and accurate. This will only be revisited after measurements
  on a real phone.
- On iPhone, the run is still saved to storage every second. It is not reduced in this build,
  because those saves also protect the run if iOS closes the app.
- On iPhone, a heart-rate strap that disconnects during a run is still not reconnected
  automatically. This needs testing with a real strap and is planned separately.
- These changes were checked with automated tests, emulators and the iPhone simulator, not yet on a
  physical Samsung Galaxy phone or iPhone.

## Data, privacy and licensing

Unchanged from 0.15.2-test1: no new permissions, network access or data leaves the device.
SQLite schema 20, backup JSON 14, raw recording archive 3 and complete backup package 1 are
unchanged, and no stored record is rewritten. Live route reading is read-only.

## Verification

The exact final source must pass the shared domain tests. These include new tests that count
notification posts during simulated 5K to marathon runs, compare incremental and full live-route
reads, check that dashboard figures match the earlier calculations on 1-, 3- and 5-year synthetic
histories, and confirm that merged refreshes still pick up every change. It must also pass the
Android API 28, 35 and 36 device matrix, screenshots, and the iPhone simulator, startup, UI and
unsigned device builds. Physical-device and battery measurements remain separate.

## Previous release highlights

- 0.15.2-test1: weekly run counts no longer count the same run twice when it reaches the app from
  more than one source; onboarding review buttons stay usable; clearer race distance.
- 0.15.1-test1: the first build signed with the permanent release key; later versions can be
  installed over it from Settings › App updates.
