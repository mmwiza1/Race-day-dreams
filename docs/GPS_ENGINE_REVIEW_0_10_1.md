# GPS engine review — 0.10.1-test1

Reviewed 4 November 2026, before changing Race Day Dreams' recording calculations.
This is a source-level comparison, not a device accuracy benchmark. No external
source code was copied, translated or incorporated. Race Day Dreams remains MIT.

## Reproducible reference snapshots

| Project | Reviewed revision | Licence and scope |
| --- | --- | --- |
| [OpenTracks](https://github.com/OpenTracksApp/OpenTracks/tree/3c23a9f5400c7b5b914a221469d76cc019a18296) | `3c23a9f5400c7b5b914a221469d76cc019a18296` | Apache-2.0; inspected file headers include Google copyright notices. This GitHub repository is archived. Current [Codeberg upstream](https://codeberg.org/OpenTracksApp/OpenTracks) could not be accessed through the available research tool; findings apply only to this snapshot. |
| [RunnerUp](https://github.com/jonasoreland/runnerup/tree/434d704aef61c8b378998b48e4c895304d0338e8) | `434d704aef61c8b378998b48e4c895304d0338e8` | GPL-3.0, reviewed tracker files GPL-3.0-or-later. Behavioural study only: no implementation reuse or dependency. |
| [GPSTest](https://github.com/barbeau/gpstest/tree/189686abf0b5236e654e17b5f637fa8072d7892c) | `189686abf0b5236e654e17b5f637fa8072d7892c` | Apache-2.0; inspected Android/Open Source Project, Sean Barbeau and University of South Florida notices. Diagnostic tool, not a run-distance oracle. |

Short OpenTracks paths below are relative to `src/main/java/de/dennisguse/opentracks/`;
RunnerUp paths are relative to `app/src/main/org/runnerup/`. GPSTest service paths are
under `GPSTest/src/main/java/com/android/gpstest/`; its library paths are stated separately.
These findings do not establish which app is more accurate on a Galaxy S24.

## Comparison and decisions

| Concern | Reference evidence | Race Day Dreams decision |
| --- | --- | --- |
| Acquisition | OpenTracks `services/handlers/TrackPointCreator.java` combines observations with sensor updates; `sensors/sensorData/AggregatorGPS.java` prevents repeated emission of the same position. RunnerUp `tracker/component/TrackerGPS.java` uses Android location requests and can inspect last-known providers. GPSTest `library/src/main/java/com/android/gpstest/library/data/SharedLocationManager.kt` requests `GPS_PROVIDER` and removes updates when collection ends. | **Retain** native `LocationManager`, service ownership and explicit cleanup. **Reject** last-known/network fallback as fresh recorded evidence. No Play Services dependency. |
| Observation age | GPSTest `library/src/main/java/com/android/gpstest/library/util/LocationUtils.java` describes age using elapsed-realtime nanoseconds. RunnerUp `tracker/Tracker.java` handles observation clocks and configurable alternatives. | **Adapt** monotonic receipt-age qualification independently. Wall-clock changes must not affect run duration. Historic GPX import has no live receipt clock and remains a separate path. |
| Accuracy and status | OpenTracks `sensors/GpsStatusManager.java` considers accuracy and a signal-loss timeout (30 seconds plus interval). RunnerUp `tracker/GpsStatus.java` considers horizontal accuracy and satellite/status history. GPSTest exposes diagnostics rather than promising positional truth. | **Retain** uncertainty reporting and RDD's existing 15-second segment-break boundary. **Reject** treating good reported accuracy or satellites as proof. Separate stale evidence from weak positional evidence. No satellite dashboard. |
| Distance and gaps | OpenTracks `services/TrackRecordingManager.java` uses storage intervals, distance limits and automatic segments; `stats/TrackStatisticsUpdater.java` resets position at segment boundaries and can use sensor distance. RunnerUp `tracker/Tracker.java` calculates pairwise distance. | **Retain** cumulative qualified distance, raw accepted coordinates and explicit discontinuities. **Adapt** bounded stationary-cluster qualification after a reproduced jitter failure. Do not invent bridges or replace GPS distance with unsupported sensor estimates. Storage thinning alone does not solve jitter. |
| Pace | RunnerUp `tracker/Tracker.java` includes a speed low-pass stage. OpenTracks retains point speed and statistics separately. | **Retain** RDD's qualified 30-second distance/time window and cue hysteresis. **Reject** copying GPL smoothing or adding Kalman filtering without measured benefit. Suppress precise pace when current GPS evidence is inadequate. |
| Persistence/export | OpenTracks `io/file/exporter/GPXTrackExporter.java` distinguishes segment boundaries and exports only points with coordinates; its sensor-only observations have export limitations. | **Retain** RDD's transactional staging, raw archive and exact accepted-coordinate GPX export. No fabricated coordinate for missing observations. Old stored totals remain unchanged. |
| Service/background | OpenTracks `services/TrackRecordingService.java` starts foreground recording, owns sensor lifecycle and releases its wake lock on stop. RunnerUp `tracker/Tracker.java` owns recording state and a timed wake lock. GPSTest `ForegroundOnlyLocationService.kt` owns foreground notification and flow cleanup. | **Retain** one foreground service, timed/renewed wake lock and explicit unfinished-run recovery. No speculative background-location permission. Locked-screen/battery behaviour still requires S24 testing. |
| Coaching and diagnostics | RunnerUp coordinates tracker state, workout and audio components. GPSTest includes extensive GNSS/NMEA diagnostic logging. | **Retain** service-owned execution and existing GPS-loss cue suppression. **Reject** broad raw GNSS/telemetry logging. Any additional rejection reasons stay bounded, local and coordinate-free. |

## Why targeted changes

Baseline RDD traces reproduced stationary jitter inflating distance, an old observation
reopening an already-declared gap, and auto-pause inferring continuous stillness across
a minute without observations. See [diagnosis](GPS_DIAGNOSIS_0_10_1.md). None requires
replacing the recorder or adding an external library. Source inspection found useful
patterns, not a proven universal accuracy algorithm. No upstream issue report is treated
as a demonstrated RDD defect, and inaccessible Codeberg sources were not inferred.

## Licensing boundary

The links are research references, not bundled third-party code. No GPL source was
copied into this MIT project. No new licence runtime obligation or dependency was
introduced by this review. If future work incorporates external source, it requires a
separate file-level compatibility and attribution review.
