# Native run foundation — Prompt 1

Original implementation inspired by OpenTracks' foreground-service/recording-manager
separation. No OpenTracks source was copied. Android LocationManager provides GNSS
without Play Services or network access. Interfaces isolate LocationSource,
HeartRateSource and MotionSource. The Java RunMetricsEngine accepts explicit clocks
and trace evidence; no training rule is owned by Compose.

## Timing and quality

Monotonic elapsed realtime owns duration. Raw fixes retain wall time independently;
canonical activity end is start plus monotonic elapsed, so clock corrections cannot
produce a negative duration. Moving time means active elapsed time excluding manual
and explicitly recorded automatic pauses; it is not a physiological movement estimate.

Locations worse than 35 m accuracy are excluded. Speeds/spatial jumps over 12 m/s,
out-of-order and duplicate fixes are rejected. Gaps over 15 seconds create a segment
break and no bridging distance. These are conservative product heuristics, not proof
of GPS correctness. Route coordinates are never smoothed or invented. Live pace uses
up to 30 seconds of continuous accepted evidence, requiring 5 seconds and 10 metres.
Existing interpolation-based split/PB/durability calculators consume elapsed samples;
they refuse unsupported intervals rather than invent constant-pace splits.

Manual pause discards the distance bridge. Optional auto-pause defaults off: 30
seconds of speed <=0.25 m/s, accuracy <=8 m and displacement <=4 m are required;
resume requires >=1.2 m/s for five seconds. Missing/poor evidence does not trigger
transitions. Slow climbs, crowds and multipath still require device evaluation;
manual pause is the reliable default. Auto-pause never resumes a manual pause.

## Service and storage

An explicit user start enters a location foreground service synchronously. No
background-location permission is used. Notification controls pause/resume; finish is
in the run screen. GNSS and sensors stop at finish/destruction. A partial wakelock is
limited to active service lifetime, renewed in bounded 30-minute leases. Screen/UI
rendering is unnecessary while backgrounded.

Schema 7 adds recording_sessions / recording_points / recording_events and an optional
health cache. Five-second batches journal accepted points and events on serialized IO;
at most the last unflushed batch may be lost on abrupt kill. Pauses/finish flush
immediately. Finalization shares a database transaction with canonical activity/PBs;
a failure retains staging. Raw recording data remains after completion for export.
Process/reboot recovery offers saving only the observed portion, not automatic sensor
restart or invented downtime. The runner can explicitly discard with confirmation.
A run with no usable samples cannot become a fictitious completed activity.

The service retains the current trace in memory. Four-hour/marathon physical-device
battery/memory validation remains required. Interrupted runs can be finalized, but
continuing the same recording after process death is not implemented in this pass.

## Sensors and interoperability

BLE uses Bluetooth SIG Heart Rate service 180D / characteristic 2A37. The runner
selects a monitor; its address stays private. Connection retries are bounded and only
occur during recording. Disconnect clears HR and records an event; no HR interpolation.
Android step-detector timestamps can supply cadence with optional motion permission.
Sensor observations are retained as timestamped events even when GPS is absent.
No cadence is calculated from GPS. Sensor absence does not stop a run.

The original GPX adapter uses Android's platform XmlPullParser; no extra parser or
network dependency. It accepts one timestamped track of up to 24 hours, and bounds input/points/depth and rejects DTD/custom entities and missing
or non-increasing timestamps. Accuracy remains unknown for imported files. GPX segments
and long gaps break continuity; altitude is optional. Exact retained route/time matches
and repeated file imports do not create duplicate activities. GPX omits optional HR/
cadence extensions in this first adapter; raw archives preserve those observations.
A future ActivityFileImporter/FutureFitImporter boundary ships no FIT implementation
or Garmin SDK.

Optional explicit Health Connect write-back sends only native exercise/distance totals
using stable client record IDs/version. It requires separate write permissions. Retries
reuse IDs; imports exclude this app's origin. Routes/HR/cadence are not written in this
pass and no sleep/HRV/resting-HR data is ever written.

## Backup

The normal JSON envelope is now 5 (reads 1–4), retaining its 8 MiB limits and existing
transactional validation. It includes compact native/GPX metadata, not unbounded sample arrays or raw
coordinates. Retained raw samples are reattached transactionally on this device; a
fresh installation requires the companion archive to restore detailed evidence. A replacement that would remove retained native/GPX runs or interrupt an
active recording is rejected before changes.

Use the **companion recording archive** as well as the app JSON backup. Archive format
`rdd-owned-recordings`, version 1, contains manifest.json and recordings.jsonl in ZIP.
Raw sessions/points/events and canonical activities are streamed to private temporary
files; SHA-256, expanded-byte, entry, row and record limits are validated before a
transactional merge. Conflicting IDs reject the whole merge; identical evidence can be
restored repeatedly. Limits: 512 MiB expanded, 1000 sessions, 100000 points/session and
8 MiB/row. Health Connect caches are omitted. Export reports over-limit failure rather
than truncating data. Archives are not encrypted; routes are sensitive.

This is a two-file backup workflow, not a replacement full-app container. Preserve both
files before reinstall/replace. Unifying core and recording backups and database-backed
history paging are deliberate follow-ups. Do not describe the normal JSON alone as a
complete raw GPS/sensor backup.

## Prompt 2 boundary

The event type is extensible text with optional observed value. It can later retain
workout-step events; interfaces can later feed WorkoutExecutionEngine/TTS/haptics.
No Live Coach, interval execution, voice alerts, Race Day, maps/routes, mobility,
exercise illustrations, Wear OS or LLM behaviour is introduced here.
