# Race Day Dreams 0.17.0-test1

This development build makes it easier to record indoor runs, resolve duplicate activities and plan or log a gym session. It adds per-discipline colours and more useful recording diagnostics.

## What's new

- Log treadmill distance and duration without creating a GPS route. Link the result to a scheduled run when appropriate.
- Compare overlapping recordings side by side. Choose which recording counts, keep both separate or restore automatic selection. The other copy remains in history unless you confirm removal of an eligible local copy.
- Design a one-off Gym Now workout around your available time, equipment and one or more muscle groups. Review it and choose compatible exercise alternatives before starting.
- Independently choose or reset accent colours for Running, Gym / Strength, Bouldering and Settings. Settings defaults to green.
- Review a recording diagnostic summary and export the detailed JSON when you choose. The summary covers GPS deliveries, gaps, rejected fixes and point-saving failures when captured.

## Improvements

- Enter sets first when logging a past gym workout. Use the workout date and leave duration blank when unknown.
- Start an eligible planned gym session early and have it count against its original scheduled session. Running also offers a shortcut to review moving a planned run to today.
- The exercise library adds 26 exercises, improves search aliases and treats resistance bands and secure anchors as separate equipment choices.
- The gym designer keeps optional effort scales and training priority under Advanced training preferences. It explains both effort scales, and the usual defaults still apply without opening the section.
- Treadmill duration stays readable with the keyboard open on compact Android screens. Workout previews and saving actions are easier to find.

## Fixes

- Training readiness wording now agrees with the rating. A two-day running schedule uses the same effective-frequency rules when deciding whether to include a harder session.
- Coaching evidence and shoe mileage use the counted copy of an overlapping run. Source records and personal-best provenance remain available.
- Gym Now recognises more compatible machine and cable exercises and alternatives from the equipment you select.

## Known limitations

- The reported outdoor GPS under-recording still needs a fresh physical-device trace. The new diagnostics help investigate it; GPS distance calculation is unchanged.
- Provider-owned records remain under the provider's control. An app backup does not contain the provider's full history or duplicate preferences for pairs composed entirely of provider records.
- The unsigned iPhone development build needs signing before installation. Physical outdoor GPS, provider import and screen-reader checks remain separate from automated verification.

## Data, privacy and licensing

Recording diagnostic reports stay on the device until you export them, contain no GPS coordinates and are excluded from app backups. Accent choices use the existing backup format. Database and backup format versions, location permissions and Android signing identity are unchanged.

## Verification

The release build information identifies the source revision and verification run.
