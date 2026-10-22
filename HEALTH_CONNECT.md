# Health Connect

## Implemented integration

Health Connect is the optional primary baseline provider. The adapter follows
the official Google Android Health sample API patterns and sits behind
provider-neutral HealthGateway / HealthImportRepository contracts. No Google Fit
API, provider account, direct Samsung connection or application server is used.
Samsung Health contributes only records it has synchronized to Health Connect.

The UI detects available, unavailable and update-required states. It explains
permissions before the platform request. Declining, granting partially or
revoking access leaves manual baseline, goals, shoes and results usable.

| Permission | Implemented purpose |
| --- | --- |
| Read exercise | Running sessions, times, source metadata and available native laps |
| Read distance | Recorded session distance and baseline mileage |
| Read heart rate (optional) | Available session average and maximum HR |
| Read history (optional, feature guarded) | Up to 84 days of baseline evidence |

No write, background-read, sleep, location, cadence or elevation permission is
requested. Those metrics are not invented when absent. With standard access the
adapter uses an explicit conservative 29-day window; Health Connect may otherwise
silently omit older data. Extended history is requested only when supported and
chosen. The review screen reports its actual window and limitations.

## Import integrity

Initial import acquires a changes token before the paginated snapshot. Follow-up
imports process paginated changes, updates and deletion IDs, refresh associated
measurements, and reconcile an authorized snapshot. Source identity is a neutral
provider + record ID with origin metadata, not a Health Connect object in the
domain. Repeating the same import does not create duplicate records.

A complete staged batch and next checkpoint commit atomically. Mid-read
revocation or any failed read does not advance the checkpoint or write a partial
snapshot. Permission changes/token expiry trigger bounded reconciliation.
Inaccessible older records are retained and the limitation is reported rather
than treated as deleted. Associated metrics are constrained to the session's
source writer to avoid summing mirrored providers.

Distinct source records can describe the same physical run. Baseline analysis
counts exact mirrored start/end/distance recordings once and explains that
choice while retaining raw provenance. Approximate cross-provider matching is
not a verified identity merge. Review remains important.

## Honest metric availability

Session totals do not contain the continuous distance/time samples needed for
rolling PBs or arbitrary kilometre/mile splits. Import does not reconstruct
samples from a total or assume constant pace. Native exercise laps remain
separate. Cadence, routes, moving time and elevation are blank unless later
supported by retained reliable data and the corresponding implemented feature.

## Remaining verification and extension

Automated tests cover provider-neutral availability/permission/import policy,
identity, deletion, checkpoints and local storage. A physical device with actual
Health Connect records is still required to exercise vendor synchronization,
platform consent/revocation UI and history availability. Emulator fake-provider
checks do not establish that Samsung Health synchronization is enabled.

Future providers should implement the same contract. Add only permissions tied
to an implemented feature and update this document before collecting new data.


## Coaching test-build use

Imported records feed the existing baseline derivation and explicit plan-generation
request. The runner reviews evidence before entering intentions. Optional manual
baseline data fills gaps. Coaching, strength logging and scores work with no Health
Connect/HR/sleep data; no new permission was added. Imported runs are explicitly
linked by the runner to a preserved prescription version before planned execution
scoring. Provider payload/source identity remain separate from this app-owned link.
Strength logging is manual in this build; no provider-specific gym assumptions.


## Optional body weight (0.4)

Settings requests READ_WEIGHT separately. It is not added to baseline running
permissions and denying it does not block manual tracking or running. A staged,
paginated read covers 29 recent days (84 with extended-history permission), then
replaces only that provider window transactionally after rechecking consent.
Metadata IDs deduplicate updates; deletions within the refreshed window disappear.
Older retained measurements are not deleted by a short window. Missing timezone
uses explicit UTC rather than guessing the current device offset. No measurement
is turned into a readiness judgement or exported as unnecessary raw provider data.
Physical-device permissions and vendor measurement availability remain to verify.

Samsung direct SDK is not included. See [provider caveat](docs/SAMSUNG_PROVIDER.md).
The provider-neutral reconciliation read model has deterministic synthetic tests;
real cross-provider ingestion/annotation reconciliation requires the future adapter.
