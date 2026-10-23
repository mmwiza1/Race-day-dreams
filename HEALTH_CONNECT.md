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

The baseline flow requests only the read scopes above. Separate optional health
context, recording and totals write-back flows are described below; no background
health/location read is requested. Metrics are not invented when absent. With standard access the
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

Health Connect is the supported interface. Legacy provider preferences normalize on upgrade/restore without deleting existing activity records.
The provider-neutral reconciliation read model has deterministic synthetic tests;
real cross-provider ingestion/annotation reconciliation requires the future adapter.

## Health Intelligence / native run increment (0.6 test)

Optional READ_SLEEP, READ_RESTING_HEART_RATE, READ_HEART_RATE_VARIABILITY (RMSSD)
and READ_STEPS are used by Personal health context in Settings. Permission rationale
precedes the system chooser. No permission is required to use running/strength/manual
features. A bounded authorized read is staged, consent rechecked, then each permitted
metric window is replaced transactionally; denied metrics and older data remain.

Sleep-session duration may include awake time; stages do not control coaching. Raw
sleep sessions are not summed across apps. The longest comparable-source sleep session per day
is used; a later short nap cannot replace the main session. Naps/overlapping nights
are not synthesized into a daily sleep total. RHR/HRV
use the latest source and latest daily measurement only. Source changes restart evidence
coverage rather than mixing device ecosystems. Steps use Health Connect's aggregate
API and provider priority, never summed mirrored raw step records. Complete days only
are used for steps. Missing is null, not zero.

Trends compare seven recent days with the preceding 28, requiring four and fourteen
measured days respectively. Normal range uses mean absolute deviation and a documented
relative tolerance (HRV 15%, steps 25%, other signals 8%). These are explainable product
heuristics, not validated clinical thresholds. Each result exposes source/coverage and
limitations. No readiness score or automatic plan mutation is implemented. Trend models
are clean supporting inputs for deeper corroborated coaching in Prompt 2.

Native run write-back is separate and explicit: WRITE_EXERCISE and WRITE_DISTANCE only.
Stable client IDs prevent duplicate retries; records with this app's origin are excluded
from exercise import. Sleep/HRV/resting-HR/steps are read-only. Route, HR and cadence
write-back remain deliberately unimplemented. See docs/RUN_ENGINE.md.

## Corroborated coaching context (0.7)

RecoveryCoaching uses the measured personal trends plus actual running/RPE/strength
evidence. A low sleep trend alone does not cancel training. Suggestions require low
sleep + elevated RHR + high recent running load, low comparable-source HRV + repeated
high RPE, or low sleep + repeated high RPE + hard lower-body work. Steps remain an
independent activity observation, not invented running stress. These are original
product heuristics, not diagnoses. Missing/poor coverage contributes no concern.

The planned-workout screen explains corroboration and previews a runner-chosen easy
alternative. Acceptance creates one new plan version at the same distance/time ceiling;
other workouts, goal and recurring days stay unchanged. Existing move/skip/review
controls remain. There is no new Health Connect permission in this increment.

## 0.8 compatibility

Permissions, provider reconciliation and write-back remain unchanged. Recorder-only
onboarding and Run Now do not require Health Connect. Spontaneous origins and explicit
plan associations are app-owned annotations; they never replace provider evidence.
