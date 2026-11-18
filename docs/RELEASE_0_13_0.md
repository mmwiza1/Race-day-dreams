# Race Day Dreams 0.13.0-test1

The Android app now runs on one shared Kotlin codebase that also builds an experimental iPhone
app. Your training, history and backups carry over unchanged.

## What's new

- An experimental iPhone app built from the same shared code: running plans, run recording with
  GPS, Gym Now and strength programmes, calendar, progress, Dream Routes, backup and restore,
  GPX files and optional Apple Health imports. It is not yet signed or distributed; see Known
  limitations.
- On iPhone, spoken run cues lower your music while they play, pause podcasts or audiobooks,
  stay quiet during phone calls (vibrating instead for important cues) and can play with the
  screen locked.

## Improvements

- Training rules, recording, coaching and strength logic now live in shared Kotlin, checked
  against the original Android behaviour so plans and calculations stay the same.
- The Android app uses the same shared screens, theme and five-tab navigation (Home, Plan,
  Calendar, Progress, Strength) that the iPhone app uses.
- Backup, GPX import, restore and recording cleanup share one safety guard. Restore waits until
  any unfinished run or gym workout is finished, and a failed restore leaves your data unchanged.

## Fixes

- No user-visible Android defects are fixed specifically in this build; behaviour is intended
  to match 0.12.3-test1.

## Known limitations

- The iPhone app is an unsigned engineering build. There is no App Store or TestFlight version,
  and installing it needs your own Apple developer signing (see `TEST_BUILD.md`).
- iPhone Apple Health imports cover workouts, weight and height. Sleep, resting heart rate,
  heart-rate variability, steps and heart rate during imported workouts are Android-only for now.
- The iPhone app has no in-app "Delete local app data" option; deleting the app removes its data.
- iPhone GPS, Bluetooth heart-rate monitors, locked-screen recording and audio cues, Apple Health
  permissions, VoiceOver and backup restore have only been tested in the simulator, not on a
  real iPhone.
- Physical Galaxy S24/Pixel/Xiaomi, human TalkBack, real GPS/Health Connect/BLE and OEM
  behaviour remain separate owner checks on Android.
- GitHub cannot replace an APK/checksum pair atomically. If a download crosses an update and its
  checksum differs, download the pair again or use version-specific files.

## Data, privacy and licensing

SQLite schema 19, backup JSON 13, raw recording archive 3 and complete backup package 1 are
unchanged; existing data and older backups remain readable. Android permissions are unchanged.
The iPhone app reads Apple Health only for the categories you choose, uses location while you
record a run (including with the screen locked) and Bluetooth only for a heart-rate monitor you
select. Neither app connects to the Internet or has an account, advertising, analytics or a
backend; the Android app still has no Internet permission.
The iPhone app shows all bundled third-party, route-source and graphics-engine licences in
Settings.

## Verification

Before merge, the exact final source must pass the full Android API 28, 35 and 36 test matrix
with source-bound screenshots, shared domain, schema and backup checks, and the iPhone simulator
build, startup and UI tests, plus an unsigned iPhone device build. Final build identity is
injected below by the publisher; physical-device acceptance remains separate.
