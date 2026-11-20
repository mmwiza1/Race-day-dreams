# Race Day Dreams 0.15.0-test1

Android app updates: once Race Day Dreams is signed with its release key, the app can tell you a
new version is available, download it, check it and hand it to Android to install over your
current copy, keeping your training data.

## What's new

- Settings › App updates shows your installed version, checks GitHub for a newer release and shows
  what's new in it.
- Download an update from inside the app with a progress bar, then install it through Android's
  own approval screen. Your runs, plans, gym and bouldering history, shoes, measurements and
  settings stay on your phone.
- Every download is checked before installation: its size and checksum must match the release,
  it must be newer than your version, and it must be signed with the same key as your current
  copy. Anything else is rejected and nothing changes.
- Choose "Remind me later" or "Skip this version", turn the once-a-day automatic check on or off,
  and choose whether to get a notification when an update is available.
- Updates never start during a recorded run, an unfinished gym workout or an unsaved bouldering
  session.

## Improvements

- Releases can now be signed with one consistent Race Day Dreams key from protected GitHub
  settings. Publication stops if the signature, certificate or file contents do not match the
  verified build, or if a lower version would be announced.
- Clear, step-by-step guidance for moving from an older test build to an update-ready build
  without losing data.

## Fixes

- No other user-visible behaviour changes in this build; training, recording, gym, bouldering,
  backups and the iPhone app are unchanged.

## Known limitations

- In-app updates begin with the first build signed with the Race Day Dreams release key. Setting
  up that key is a separate owner step; until it is done, published builds are still test-signed
  and Settings › App updates explains that the copy cannot be updated in place.
- Every earlier build, including 0.14.0-test1, was signed with a different temporary key, so moving
  to the first update-ready build needs a one-time backup, reinstall and restore (in the app:
  Settings › App updates › "How to move to the update-ready version safely").
- Android always asks you to approve an installation, and you may need to allow Race Day Dreams to
  "Install unknown apps" once. Some work or managed devices block installs from apps.
- Updates were checked with automated tests and an emulator in-place upgrade using throwaway test
  keys. A real phone update through Android's approval screen has not yet been performed.
- The iPhone app is still an unsigned engineering build without in-app updates; see `TEST_BUILD.md`.
- GitHub cannot replace an APK/checksum pair atomically. If a download crosses an update and its
  checksum differs, download the pair again or use version-specific files.

## Data, privacy and licensing

This is the first Android build with network access, used only for updates: an optional HTTPS
check of the official GitHub release (at most once a day when you open a release-key build, or
when you tap Check for updates) and the download you choose. No account, identifier or training
data is sent. New Android permissions: `INTERNET` and `REQUEST_INSTALL_PACKAGES`. Update choices
stay on the device and are not in backups. SQLite schema 20, backup JSON 14, raw recording
archive 3 and complete backup package 1 are unchanged; there is no new dependency or licence.

## Verification

Before merge, the exact final source must pass the shared update-rule and manifest tests, emulator
tests of APK verification, downloads, failures and active-workout safety, an emulator in-place
upgrade with throwaway test keys that keeps every database row and refuses a differently signed or
older build, the full Android API 28, 35 and 36 matrix with update screenshots in light, dark and
large text, and the iPhone simulator build, startup and UI tests plus an unsigned iPhone device
build. Final build identity is injected below by the publisher; physical-device acceptance remains
separate.

## Previous release highlights

- 0.14.0-test1: bouldering in Strength with V-scale and Fontainebleau grades, sends, flashes and
  progress over time.
- 0.13.1-test1: Progress and Strength analytics dashboards with reporting periods and tappable
  charts.
