# Race Day Dreams 0.14.0-test1

Bouldering is now part of Strength: log climbing sessions with grades, attempts, sends and
flashes, review them before saving, and follow your progress over time, on Android and iPhone.

## What's new

- Open Strength › More detail › Bouldering to start a live session with a running timer, or log a
  past session by choosing its date and entering how long it lasted.
- Add each problem with its grade (or no grade), the number of attempts and whether you sent it,
  flashed it (sent on the first attempt) or did not send it. An optional location and notes can be
  added to the session.
- Review and correct a session before it is saved. Saved sessions can be opened, corrected or
  deleted.
- An unsaved session is kept on your device, so a closed app or a restarted phone does not lose
  it: open Bouldering and choose Resume session.
- Choose V-scale or Fontainebleau as your preferred grading system. Every problem keeps the grade
  you entered; where the other system is shown, it is marked as approximate (for example
  "6C+ (≈ V5)").
- Bouldering analytics use the same 4-week, 12-week, 6-month and 1-year periods as the dashboards:
  sessions, climbing time, problems, attempts, sends, flashes, highest send, a grade distribution
  and your hardest send by week.
- Once you have logged a climb, the Strength tab shows a compact bouldering card that opens it.

## Improvements

- Bouldering is kept separate from lifting: climbs never add working sets, estimated one-rep
  maxes, lifting records or muscle-group workload.
- Grades from your non-preferred system and ungraded problems are counted, never converted into
  the charts, and are listed below them.
- The bouldering screens support dark mode, very large text, TalkBack and VoiceOver labels and
  honest empty states.

## Fixes

- No other user-visible behaviour changes in this build; Home, Plan, Calendar, running and gym
  workouts are unchanged.

## Known limitations

- Only one unsaved bouldering session can exist at a time; finish or discard it before starting
  another. Replacing app data from a backup is refused until it is saved or discarded.
- Approximate conversions between V-scale and Fontainebleau are a common guide, not an exact
  equivalence, and are only used for display.
- The session timer counts while the screen is open and from the session's start time; it does
  not run a background service or notification.
- The iPhone app is still an unsigned engineering build; see `TEST_BUILD.md`.
- Bouldering was checked by automated tests and screenshots on the Android emulator and iPhone
  simulator only. Logging a real climbing session on a phone, human TalkBack and VoiceOver checks
  and restoring a real backup on a device remain separate.
- GitHub cannot replace an APK/checksum pair atomically. If a download crosses an update and its
  checksum differs, download the pair again or use version-specific files.

## Data, privacy and licensing

Bouldering sessions are stored only on your device. The SQLite schema moves from 19 to 20 with
three new tables (sessions, one unsaved session and your grading preference); an upgrade that
fails leaves the database at version 19. Backup JSON moves from 13 to 14 with a climbing section;
older backups restore with no climbing data, and an unsaved session is never included in a
backup. The raw recording archive (3) and complete backup package (1) are unchanged, and
permissions, networking (none), accounts and licences are unchanged.

## Verification

Before merge, the exact final source must pass shared bouldering contract tests (grades, flashes,
sessions and analytics), repository, schema migration and backup round-trip tests, new bouldering
screen tests including an interrupted session, the full Android API 28, 35 and 36 matrix with
light, dark, large-text and empty-state screenshots, and the iPhone simulator build, startup and
UI tests plus an unsigned iPhone device build. Final build identity is injected below by the
publisher; physical-device acceptance remains separate.

## Previous release highlights

- 0.13.1-test1: Progress and Strength analytics dashboards with reporting periods, tappable
  charts and links to the runs and workouts behind them.
- 0.13.0-test1: one shared Kotlin codebase for Android and an experimental, unsigned iPhone app,
  with training, history and backups carried over unchanged.
