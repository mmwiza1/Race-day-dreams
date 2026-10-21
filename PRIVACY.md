# Privacy

## What Phase 1 stores

Race Day Dreams stores the goal distance, optional race name/date, goal mode,
optional finishing-time target, training strategy, running availability, gym
availability, schedule preference and unit preference. It also stores an
incomplete onboarding draft so setup can be resumed.

These values are held in the application's private SQLite database on the
device. Saving completed setup replaces the saved profile transactionally and
clears its draft. Local reset, after your confirmation, deletes the database and
its journal files, including the stored profile and draft. Unreadable or corrupt
data is retained until you choose that reset; the app does not silently delete
it after a read error. Android's app-data clearing or uninstall also removes
application data.

## What is accessed or sent

Phase 1 does not access Health Connect, workout history, heart rate, sleep,
location, contacts or advertising identifiers. It does not request health or
Internet permissions.

There are no accounts, advertisements, analytics, trackers, cloud databases or
LLM API calls. The application does not send your entered information to a
server. Android backup is disabled for the app to keep its stored data out of
automatic platform backups.

The database uses Android application-private storage, not a separate
application-level encryption layer. Avoid adding sensitive information to race
names or notes unnecessarily. Developer builds and test devices should use
synthetic data.

## Future health integration

Health Connect access will be optional and limited to records used by implemented
features. The intended flow is Health Connect → local database → local training
engine. No server is necessary for the planned coaching rules.

Before adding an integration, document exactly what is read, why it is needed,
how long it is kept, how it can be removed and whether anything leaves the
device. A future change to data handling must not be silently introduced.

## Development tooling

Gradle and Android Studio download development dependencies when building the
project. Those downloads are build-tool activity, not application telemetry.
Never commit personal health records, production databases, signing keys, tokens
or local SDK paths to this repository.
