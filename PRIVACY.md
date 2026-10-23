# Privacy

## Local, account-free operation

Race Day Dreams has no Internet permission, server, account, analytics, advertising
or LLM service. Goals, preferences, drafts, manual baseline, activities, results,
shoes, annotations, generated plans/version history, evidence summaries, coaching proposals/decisions, strength sets, run responses/feedback and reminder preferences are stored in the app's private SQLite
database. Android cloud backup and device transfer are excluded. The database
uses Android application/device protection, not a separate encryption scheme.

## Optional Health Connect

Running exercise sessions and distance support history and recorded baseline.
Optional heart rate supplies available average/max HR. Optional extended history,
where supported and chosen, allows up to 84 days; standard access uses an explicit
29-day window. Source identities, timestamps and reported offsets are retained.
Separate optional health-context and native-recording flows are described below.
Explicit native run totals can be written after separate consent; the app never
deletes Health Connect records. No background health/location access is requested.

Declining, partially granting or revoking access leaves manual setup and local
features usable. Revocation stops future reads; previously imported data remains
locally until confirmed local-data deletion. See [HEALTH_CONNECT.md](HEALTH_CONNECT.md).
No Health Connect credentials or records are uploaded by the app.

## Backup/export and restore

A backup is an explicit user-chosen JSON file. It contains app-owned profile,
goals, preferences, manual/assessment entries, shoes, notes, results, derived PB
metadata and plan/settings data, including goal/evidence snapshots, complete coaching version/decision history, strength sessions/custom exercises and run responses. Raw imported Health Connect activities,
HR, samples and live checkpoints are excluded. Their local source references can
preserve annotations and derived PB history until records are re-imported.

Exported backups are not encrypted by the app. Choose a trusted destination.
Android's document picker may offer cloud providers: selecting one is your choice
and may cause that provider to sync the file. The app itself has no network access.
Restore is replace-only, versioned and prevalidated; database writes are one
transaction and roll back on failure. Unsupported versions/merge are rejected.

## Notifications and deletion

Evening preparation notifications are disabled by default, local and opt-in.
Android 13+ notification permission is requested only when enabling them. Current
workout/revision/completion/quiet-hours checks suppress stale messages. Reminders
contain general preparation advice, not diagnoses or fixed nutrition prescriptions.

Confirmed local reset cancels scheduled reminders, closes the shared store and
deletes its database/journal files. Unreadable data is retained until this explicit
choice; read errors and schema upgrades do not silently erase user data. Android
app-data clearing or uninstall also removes local data.

Development dependencies are downloaded by build tools; those downloads are not
application telemetry. Never commit real health records, secrets, signing keys or
local SDK paths. Automated fixtures contain synthetic data only.


Screenshot demo fixtures exist only in the instrumentation test APK. CI renders
isolated synthetic records and clears test storage afterward. The installable
APK contains no automatic demo-data import. Screenshots published in this repository
contain only that synthetic fixture, never a user's Health Connect or personal data.
No additional Health Connect permissions were introduced for coaching/strength.

Backup envelope version 4 includes coaching and optional strength-programming data and prevents old app versions
from silently discarding it. This reader also restores original version-1 and version-2
backups. Core store format 1 and coaching section version 1 are independently
validated. Export retains the existing 8 MiB/complexity limits: exceeding them
fails clearly before truncating a destination, without deleting local history.


Optional weight permission reads Health Connect WeightRecord only for the visible
body trend. Manual/imported measurements retain provenance and offsets. Weight
is not used in readiness or coaching. Historical strength PBs, weights, weekly
reviews, break reports and session decisions stay in private local storage.
Manual weights and historical PBs are exported only when you choose a backup;
raw provider weights are omitted. No Samsung SDK or direct Samsung access ships.

The 0.5 user-directed backup also includes exercise favourites/archive choices, PB
acknowledgements, generated strength prescriptions and runner progression decisions.
Actual workouts and chosen plan references remain separate. No data is uploaded.

## Native routes and optional health context

Explicit native recording uses precise GNSS location in a visible foreground service.
No background-location or Internet permission is added. Location/sensors stop when the
run finishes or service stops. Raw routes, timestamps, HR observations and events remain
in application-private SQLite storage. Optional BLE scan/connect permissions find a
runner-selected standard HR monitor; optional activity recognition reads genuine cadence.
No nearby-device data is uploaded. A run-scoped bounded wakelock supports locked-screen
recording; real battery behaviour requires physical testing.

Optional sleep-session duration, RHR, HRV RMSSD and steps are cached locally with origin
and timestamps. They are supporting personal context, not a diagnosis/readiness score.
Revoking permission stops future reads; retained data can be removed with explicit local
reset. Health caches are not included in portable backups.

Route export, the companion raw-recording ZIP archive and explicit Health Connect totals
write-back are user actions. Exported route/archive files are sensitive and unencrypted;
choose a trusted destination. Standard app JSON alone does not preserve raw coordinates;
export both app backup and raw recording archive before reinstall. See docs/RUN_ENGINE.md.

## Offline live coaching and support (0.7)

Local Android TTS uses an installed non-network voice; if unavailable, visual guidance
and optional vibration remain. Audio focus is requested temporarily and released;
RDD does not alter music volume. No voice/health/route payload is sent to a server.
VIBRATE is the only newly declared permission. No Internet permission is added.

Structured prescriptions/events, routine completions, official results and Dream Route
assignments stay private. Dream drawings are bundled original schematics, not remote
map requests. Complete backup format1 contains sensitive app JSON and recordings ZIP;
choose a trusted destination. It is not encrypted by RDD. Both checksums and all inner
validation precede core replacement, under one transaction. Earlier separate exports
are still readable. Provider health caches remain excluded and re-importable.

## 0.8 diagnostics and spontaneous training

Diagnostic export is explicit SAF output. Its allow-list includes app/schema/backup
versions, Android/device model and permission/Health Connect availability states. It
contains no GPS, health values, weight, personal notes, history or internal log dump.
Inspect the file before choosing to share it. No upload occurs. Custom workouts,
recording origins, association choices, Dream preferences, race checklist and optional
shoe observations stay private locally and are included in user-chosen backups.
No Internet permission, direct Samsung SDK, account or analytics is added.
