# Privacy

## Local, account-free operation

Race Day Dreams has no Internet permission, server, account, analytics, advertising
or LLM service. Goals, preferences, drafts, manual baseline, activities, results,
shoes, annotations and reminder preferences are stored in the app's private SQLite
database. Android cloud backup and device transfer are excluded. The database
uses Android application/device protection, not a separate encryption scheme.

## Optional Health Connect

Running exercise sessions and distance support history and recorded baseline.
Optional heart rate supplies available average/max HR. Optional extended history,
where supported and chosen, allows up to 84 days; standard access uses an explicit
29-day window. Source identities, timestamps and reported offsets are retained.
No write, background-read, location, sleep or other unused health permission is
requested. The app never writes or deletes records in Health Connect.

Declining, partially granting or revoking access leaves manual setup and local
features usable. Revocation stops future reads; previously imported data remains
locally until confirmed local-data deletion. See [HEALTH_CONNECT.md](HEALTH_CONNECT.md).
No Health Connect credentials or records are uploaded by the app.

## Backup/export and restore

A backup is an explicit user-chosen JSON file. It contains app-owned profile,
goals, preferences, manual/assessment entries, shoes, notes, results, derived PB
metadata and relevant plan/settings data. Raw imported Health Connect activities,
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
