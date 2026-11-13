# Race Day Dreams 0.12.3-test1

Clearer training instructions and an easier way to discover and download the app.

## What's new

- A screenshot-led GitHub showcase for Running, Strength, Progress, Health and data ownership, with downloads beside each feature.
- A consistent latest-test APK link and checksum, alongside version-specific downloads.

## Improvements

- Exercise details explain what to record for weight, repetitions, time and distance.
- Gym instructions put the task first, with exact targets and optional weight references available on demand.
- Running plans explain session purpose and effort; health summaries explain heart-rate variability.
- Logging explains perceived effort and repetitions left separately. Missing effort remains unknown.
- Backup and storage descriptions distinguish permanent history from detailed GPS/sensor data.
- Release previews use genuine, source-pinned app screenshots. Existing features, styling and navigation remain.

## Fixes

- Replace exposed internal measurement names and developer-oriented save wording with clear instructions.
- Keep reference weights, recorded lifts and estimated maximums distinct.
- Preserve valid versioned and owner-managed release assets when rolling downloads update.

## Known limitations

- Physical Galaxy S24/Pixel/Xiaomi, human TalkBack, real GPS/Health Connect/BLE and OEM behaviour remain separate owner checks.
- GitHub cannot replace an APK/checksum pair atomically. If a download crosses an update and its checksum differs, download the pair again or use version-specific files.

## Data, privacy and licensing

No training calculation, recorded history, database/backup format, permission, navigation destination,
icon or runtime dependency changes. MIT licensing and local/account-free/offline operation remain.
The app still has no Internet permission, advertising, subscriptions, analytics or backend.

## Software verification

The implementation passed700,189domain checks across48suites,75schema tests and543unique
instrumentation tests with451authentic captures on each API28/35/36. Backup/restore, lint,
formatting, guidance and release/download validation passed. Actual light/dark/200% views and
GitHub desktop/mobile layouts were reviewed. Final build identity is injected below by the
publisher; physical-device acceptance remains separate.
