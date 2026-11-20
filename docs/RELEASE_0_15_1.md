# Race Day Dreams 0.15.1-test1

The first Race Day Dreams build signed with the project's permanent release key. From this version
on, new versions can be installed over your current copy from Settings › App updates, keeping your
training data.

## What's new

- This build is signed with the permanent Race Day Dreams release key. Every later version will be
  signed with the same key, so it can update this copy in place.
- Settings › App updates can now find, download, check and install newer versions for you. Android
  always asks you to approve an installation.
- Builds signed with the release key check for updates once a day when you open the app. You can
  turn this off, or turn off the update notification, in Settings › App updates.

## Improvements

- Every published update is checked before it is offered: its version, checksum and signing
  certificate must match, or it is not shown.

## Fixes

- No other user-visible behaviour changes in this build; training, recording, gym, bouldering,
  backups and the iPhone app are unchanged.

## Known limitations

- **Moving to this version is a one-time step.** Every earlier build (0.15.0-test1 and older) was
  signed with a temporary key, so Android cannot install this version over it. In the app:
  Settings › App updates › "How to move to the update-ready version safely". In short: finish or
  save any workout, make a Complete backup (app + recordings) in Record a run and keep the file
  safe, uninstall the old copy, install this APK, then Restore complete backup… and Validate and
  restore.
- Android may ask you once to allow Race Day Dreams to "Install unknown apps". Some work or
  managed devices block installs from apps.
- A real phone update through Android's approval screen has not yet been performed; it was checked
  with automated emulator tests and an in-place upgrade using throwaway test keys.
- The iPhone app is still an unsigned engineering build without in-app updates; see `TEST_BUILD.md`.
- GitHub cannot replace an APK/checksum pair atomically. If a download crosses an update and its
  checksum differs, download the pair again or use version-specific files.

## Data, privacy and licensing

Unchanged from 0.15.0-test1: the only network access is the optional update check of the official
GitHub release and the download you choose; no account, identifier or training data is sent.
Permissions, SQLite schema 20, backup JSON 14, raw recording archive 3 and complete backup package 1
are unchanged.

## Verification

Before merge, the exact final source must pass the full Android API 28, 35 and 36 matrix (including
the update and installer tests, screenshots and the emulator in-place upgrade with throwaway keys)
and the iPhone simulator, startup, UI and unsigned device builds. At publication, the APK is signed
with the release key only after its certificate matches the committed fingerprint and its contents
match the verified build; the update manifest is checked again after upload. Final build identity
is injected below by the publisher; physical-device acceptance remains separate.

## Previous release highlights

- 0.15.0-test1: in-app update checking, verified downloads and installation through Android's
  approval screen, ready for the permanent release key.
- 0.14.0-test1: bouldering in Strength with V-scale and Fontainebleau grades, sends, flashes and
  progress over time.
