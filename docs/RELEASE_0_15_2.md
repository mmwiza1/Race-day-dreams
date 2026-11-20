# Race Day Dreams 0.15.2-test1

A bug-fix build: weekly run counts no longer count the same run twice when it reaches the app from
more than one source, the onboarding review buttons stay usable, and the selected race distance is
clearer when it does not match the race name. If you already have 0.15.1-test1, Settings › App
updates can install this version over it, keeping your data.

## What's new

- Progress › Weekly distance: tap a week, then **Show the runs in this week** to see each run
  counted, which app or source it was counted from, and any copies of the same run from other
  sources that were not counted again.

## Improvements

- The onboarding review now shows **Race distance** with the distance in kilometres, and the target
  race pace says which distance it is calculated over, for example "5:27/km over Half Marathon
  (21.1 km)".
- If a race name clearly names a different distance from the one selected, such as "Hackney Half"
  with 10K selected, the goal step and review explain the mismatch and offer **Change race
  distance**. The app never changes your selected distance from the race name.
- The onboarding buttons are spaced further apart so the button below **Create my programme** is not
  tapped by mistake.

## Fixes

- Weekly distance, run counts, running days, the dashboard totals, recent runs, trends, the
  calendar and running achievements now count each physical run once. The same run recorded by
  Samsung Health and written by another app into Health Connect, or recorded here and also entered
  manually, previously counted as two or more runs with its distance added each time.
- **Create my programme** and **Back** on the onboarding review no longer turn into a disabled
  "Saving…" state, or jump back to the top of the page, while the app refreshes in the background
  after you return to it.
- If your programme cannot be saved, the reason is now shown on the review page with your answers
  kept, instead of only a brief message at the bottom of the screen.

## Known limitations

- Two records count as the same run only when they come from different sources and overlap in time
  for at least half of the shorter one. Two overlapping records from the same app are both counted,
  as before; check them in the week's run list.
- Your stored activities are not changed, merged or deleted. Weekly totals are corrected
  automatically; nothing needs to be repaired.
- Running achievements already earned are kept. Future running achievements count each physical
  run once.
- Changing from two to three runs a week in Settings rebuilds the programme from today and keeps
  every recorded run, total and the previous programme version. Planned workouts from earlier weeks
  are not carried into the rebuilt programme's planned-versus-completed view.
- These fixes were checked with automated tests and emulators, not yet on a physical Samsung
  Galaxy phone with Samsung Health.

## Data, privacy and licensing

Unchanged from 0.15.1-test1: no new permissions, network access or data leaves the device.
SQLite schema 20, backup JSON 14, raw recording archive 3 and complete backup package 1 are
unchanged, and no stored record is rewritten.

## Verification

The exact final source must pass the shared domain tests (including new tests for duplicate
imports, several providers, manual entries, repeated syncs, same-day runs and race-name distance
checks), the Android API 28, 35 and 36 device matrix with the new onboarding review and resume
tests, screenshots, and the iPhone simulator, startup, UI and unsigned device builds. Physical-device
acceptance remains separate.

## Previous release highlights

- 0.15.1-test1: the first build signed with the permanent release key; later versions can be
  installed over it from Settings › App updates.
- 0.15.0-test1: in-app update checking, verified downloads and installation through Android's
  approval screen.
