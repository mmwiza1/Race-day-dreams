# Privacy

## Local, account-free operation

Race Day Dreams has no server, account, analytics, advertising or LLM service. The iOS app has
no networking code. The Android app's only network access is the optional update check and the
update download you choose, described under "Android app updates"; your training data is never
sent anywhere. Goals, preferences, drafts, manual baseline, activities, results,
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
deletes Health Connect records. On Android, no background health/location permission is
requested; recording uses a visible foreground service.

Declining, partially granting or revoking access leaves manual setup and local
features usable. Revocation stops future reads; previously imported data remains
locally until confirmed local-data deletion. See [HEALTH_CONNECT.md](HEALTH_CONNECT.md).
No Health Connect credentials or records are uploaded by the app.

## Experimental iPhone app

The iPhone app (unsigned engineering build, PR #24) keeps the same local-first rules. Data is
stored in an app-private SQLite database protected by iOS data protection, not a separate
encryption scheme.

- **Apple Health (optional):** only the categories you choose are read: running workouts,
  weight and height. Apple does not tell apps whether read access was denied, so the app treats
  missing data as unknown, never as a full grant. Nothing is written to Apple Health. Turning
  access off stops future reads; previously imported local records stay until you delete them.
- **Location:** requested "while using the app" only when you start an outdoor run. During a
  run you started, recording continues with the screen locked using iOS background location;
  iOS shows its location indicator while this happens. Location is not used at any other time.
- **Bluetooth:** used only to connect a heart-rate monitor you choose for a run, including with
  the screen locked during that run.
- **Audio:** spoken run cues briefly lower other audio and can play with the screen locked
  during a run. They are skipped during phone calls.
- **Notifications:** evening reminders and gym rest alerts are local, opt-in notifications.
- **Files:** backups and GPX files are read or written only through the system Files picker you
  open. As on Android, exported backups are not encrypted by the app and a cloud-backed
  destination you choose may sync them.
- There is no in-app "Delete local app data" on iPhone yet; deleting the app removes its data.

## Backup/export and restore

Bouldering sessions (date, climbing time, optional gym or location name, problems, grades,
attempts, outcomes and notes) and your preferred grading system stay on this device and are
included in app backups from backup version 14. An unsaved bouldering session is kept only on the
device so it can be resumed; it is not exported, and a restore waits until it is saved or
discarded.

A backup is an explicit user-chosen JSON file. It contains app-owned profile,
goals, preferences, manual/assessment entries, shoes, notes, results, derived PB
metadata and plan/settings data, including goal/evidence snapshots, complete coaching version/decision history, strength sessions/custom exercises and run responses. Raw imported Health Connect activities,
HR, samples and live checkpoints are excluded. Their local source references can
preserve annotations and derived PB history until records are re-imported.

Exported backups are not encrypted by the app. Choose a trusted destination.
Android's document picker may offer cloud providers: selecting one is your choice
and may cause that provider to sync the file. The app itself never uploads backups or any
training data.
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

## Retained0.9 Strength preferences and choices

Training focus, explicit exercise exclusions (ID, non-medical reason and timestamp), saved
workout intentions, actual sets and reviewed programme-association decisions stay in the
shared private local database. They are included only in user-requested backups/exports;
there is no automatic upload. Excluding/restoring an exercise does not delete recorded
history or PBs. Diagnostics retain their existing allow-list; actual sets, exclusion reasons and reviewed
decision payloads are not added. No Internet permission or runtime network dependency is added.

## 0.11 local gym tools and recognition (development)

Optional rest timing uses a private installation-identity file that is not restored from backups.
Timer backups retain opaque epoch/clock anchors for continuity checks; another installation cannot
adopt that identity and treats the timer as interrupted. These are app-local markers, not hardware IDs.
Opt-in Android rest alerts use generic text rather than exercise loads, GPS or health values. They
may be delayed by Android; no additional continuously running service or Internet access is added.
Strength analytics read recorded actuals locally. Milestone recognition retains small source
references/times and relevant recorded distance in typed local tables and explicit user backups.
It does not hydrate GPS routes for achievement counts. Optional celebrations stay in-app; there are
no achievement uploads, system streak notifications or telemetry. Diagnostic allow-lists remain
separate from these private records.

### Optional body guidance (0.11 development)

Height observations and nutrition preferences stay in the local typed database and explicit
user backups. The optional Health Connect READ_HEIGHT grant supports this feature only; manual
entry remains available. Protein guidance is off by default and requires an explicitly chosen
weight. Imported values do not become automatically authoritative. No body values are added
to diagnostics, analytics, remote APIs or background uploads. See docs/BODY_GUIDANCE_0_11.md.

## Explicit recording-detail cleanup

Settings → Storage & recording detail saves a typed24month/5year/forever preference. Existing
installations and older backups remain protected until informed acknowledgement. Saving a choice
never deletes data. A fresh preview and separate confirmation are required for foreground cleanup;
there is no automatic expiry worker. Only eligible finished app-owned dense GPS/sensor/event rows
are removed. Workout summaries, actual gym sets, PBs, programmes, awards and coaching history stay.
Health Connect provider records and ordinary body measurements are untouched. Local receipts and
fingerprints document removed detail and allow compatible original archives to be restored without
guessing. No data is uploaded. JSON13 preserves preferences/receipts; raw3 and complete1 preserve
available detail with explicit limits. See docs/DATA_LIFECYCLE_0_11.md. SQLite deletion does not
promise immediate device-space reclamation. Physical S24 cleanup/restore tests remain unperformed.

## Android app updates (optional)

Android builds signed with the Race Day Dreams release key can check the project's official GitHub
release for a newer version (Settings › App updates). This is the app's only network access:

- **What is contacted:** `github.com` and GitHub's download host, over HTTPS only (cleartext
  traffic is disabled). Requests go to the `development` release of `mmwiza1/Race-day-dreams`.
- **What is sent:** an ordinary HTTPS request for a public file, with a `User-Agent` naming the app
  and its version. GitHub, like any web server, receives your IP address. No account, identifier,
  device ID, location, health or training data is sent, and nothing is uploaded.
- **When:** a check runs only when you open the app, at most once a day, and only on builds signed
  with the release key; or when you tap **Check for updates**. It never runs during a recording,
  gym workout or unsaved bouldering session. There is no background polling, alarm, wake lock or
  service. Turn automatic checks off in Settings; manual checks still work.
- **Downloads** happen only when you tap **Download update**. The file stays in the app's private
  storage, is checked for size, SHA-256, package, newer version and the installed app's signing
  certificate, and is then handed to Android, which always asks you to approve the installation.
- **Notifications:** optional, at most one per version, and only if you already allow Race Day
  Dreams notifications. Update preferences stay on this device and are not included in backups.
- **Permissions:** `INTERNET` for the check and download, and `REQUEST_INSTALL_PACKAGES` so Android
  can ask you to approve an update. Android asks you separately before the app may install updates.

Test builds signed with temporary keys never check for updates on their own.
