# Issue #54 — S24 background run under-recorded (investigation, 25 November 2026)

Report: on a Galaxy S24, the same outdoor run of about 33 minutes was recorded twice. Fitbit showed
5.1 km in 33:30. Race Day Dreams 0.16.0-test1 / 29, running in the background, showed 1.5 km in
32:57. Fitbit is a comparison, not ground truth.

**Status:** a reproducible software mechanism has been found that produces this size of shortfall.
It is **not yet confirmed** as the cause of this run, and **no GPS code has been changed**. The
evidence needed and the proposed correction are below.

## Pipeline inspected

| Component | Role |
| --- | --- |
| `app/.../recording/LocationSource.kt` | `GPS_PROVIDER` updates at 1 s and 0 m. Each observation in a batch is forwarded in order. |
| `app/.../recording/RunRecordingService.kt` | The location foreground service (`FOREGROUND_SERVICE_LOCATION`, `location\|connectedDevice`), with a 30-minute partial wake lock that is re-acquired. A 1 s ticker calls `missingFix`. Each delivery goes through `qualifyDelivery`, then auto-pause (if enabled), then `accept`. |
| `sharedDomain/.../recording/RunMetricsEngine.kt` | The checks described below. |
| `RecordingSessionCoordinator`, `RecordingStore` | Stage the points and events; the final total is the stored cumulative distance. No recalculation was found. |

`RunMetricsEngine` applies these checks:
- **Receipt freshness:** a fix delivered more than 10 s after it was observed is rejected
  (`GPS_FIX_STALE`).
- **Barrier:** fixes before the current barrier are rejected.
- **Order and accuracy:** fixes out of order are rejected, as is accuracy over 35 m.
- **Speed:** speeds over 12 m/s are rejected.
- **Gaps:** gaps over 15 s break the segment, and no distance is bridged across them.
- **Declared loss:** after 15 s with no delivery, `missingFix` declares a loss and sets the barrier
  to the **receipt time** at which the loss was noticed.

## Reproduced mechanism: late, batched delivery

Suppose Android delivers GNSS fixes late and in batches. That can happen when the device holds back
location callbacks for a backgrounded or screen-off app. Each batch then goes through this sequence:

1. **Loss declared:** after 15 s without a delivery, the ticker declares GPS lost and moves the
   barrier to the current receipt time.
2. **Batch rejected:** when the batch arrives, every fix observed before that barrier, or more than
   10 s before delivery, is rejected as `GPS_FIX_STALE`. These are genuine fixes that are ordered and
   newer than the last accepted one.
3. **Remainder kept:** only the newest few seconds of each batch are accepted, as a new segment.

A throwaway engine-level simulation used the real `qualifyDelivery`, `accept` and 1 s `missingFix`
sequence. It modelled a 33-minute run at 6:34/km (5.03 km), with one fix a second at 5 m accuracy:

| Delivery | Recorded distance | Events |
| --- | --- | --- |
| Every 1–12 s | 5.02 km (99%) | No loss |
| Batches every 16 s | 0.02 km | 123 `GPS_FIX_STALE`, 122 `GPS_LOST` |
| Batches every 20 s | 1.02 km (20%) | ~99 stale, lost and returned each |
| **Batches every 30 s** | **1.34 km (26%)** | 66 stale, 65 lost |
| Batches every 60 s | 0.67 km (13%) | 33 stale, 32 lost |

A 30 s delivery pattern gives almost exactly the reported shortfall (1.5 of 5.1 km, 29%). This is a
software defect **if** the device delivers late: genuine evidence is discarded, and no distance is
invented either way.

## Not established without device evidence

- **Whether the S24 delivered fixes late in the background.** Alternatives produce the same headline
  number:
  - no callbacks at all, from background throttling or a stopped service;
  - many weak fixes (`GPS_FIX_ACCURACY`);
  - auto-pause;
  - speed rejections (`GPS_FIX_JUMP`).
- **Whether the foreground notification stayed visible** with the screen off, and the phone's
  battery mode for Race Day Dreams (Unrestricted, Optimised or Restricted), and Precise location.

## Evidence that decides it

Preserve the original activity; do not edit or recalculate it. Then provide either of these.

1. **GPX export of the 25 November run** (Activity → Export GPX). Each `<trkseg>` is one continuous
   segment, and every point has its observation `<time>`. Strip or round coordinates before sharing
   publicly; timestamps alone are enough.
   - **Late batching (this hypothesis):** dozens of short segments at a regular rhythm (about one
     every 20–60 s). Each holds about 10 points one second apart, followed by a hole of 10–50 s.
     There are roughly 300–700 points in total rather than about 1,980.
   - **No callbacks:** one or a few long holes, of minutes, with no points.
   - **Weak signal:** irregular holes in an urban or covered setting, with no regular rhythm.
2. **A full app backup** (Settings → Backup). It holds the recording's event log
   (`recording_events`). Regular `GPS_FIX_STALE` → `GPS_LOST` → `GPS_RETURNED` cycles confirm the
   hypothesis. `GPS_FIX_ACCURACY`, `GPS_FIX_JUMP` or `AUTO_PAUSE` events point to the alternatives.

Also useful: a screenshot of the route, the battery setting for the app, whether auto-pause was on,
and whether the notification stayed visible. If needed, one controlled 10-minute test on a known
route: phone locked in a pocket, compared with the same route in the foreground.

## Proposed correction, if confirmed (not implemented)

The aim is smallest-safe; nothing below relaxes the accuracy or speed filters, or bridges distance
across real gaps.

1. **Accept late-but-ordered fixes as evidence.** A fix delivered late is accepted when it is newer
   than the last accepted observation, after the start and any user or auto pause, within 35 m
   accuracy and plausible in speed. Distance still accrues only between observations at most 15 s
   apart.
2. **Move the loss barrier.** `missingFix` sets the barrier to the last accepted observation time,
   not to the receipt time. The declared loss still drives the live status ("GPS temporarily lost").
   That status is computed from receipt time and stays honest, because late fixes are old by
   definition.
3. **Keep auto-pause on fresh fixes only.** Late fixes never drive auto-pause. The 0.10.1 sparse-fix
   finding still holds.
4. **Keep the timeline consistent.** A `GPS_LOST` event declared at receipt time must not end up
   after `GPS_RETURNED` events for data that was in fact observed. Either withdraw or redate the
   declared loss when late data covers it, or record a distinct "delivery delayed" event. This is the
   main design point to review, because `RecordingTimeline` sorts events by time.
5. **Update tests.** Three existing GPS safety tests encode the current rejection:
   - `GpsReliabilityTestSuite.oldFixAfterLoss` and `receiptQuality`, in `domain/src/test/...`;
   - `RecordingMetricsContractTest.receiptQualification...`.

   They would need owner-approved updates that keep their intent: no reopened live status, no
   bridging of a real gap, pause barriers still enforced. A new test would cover the batched-delivery
   simulation above.
6. **Validate on a device.** Validate on the S24 (locked, background) and an API 28 device before
   any release.

Estimated scope: medium–large, a major/risky GPS change. Roughly one focused session for the engine,
the Android service and the tests, plus a physical-device check. No historical activity would be
recalculated.

## Related risk (separate, issue #53)

`PhysicalRunSessions` prefers a plan-linked or native `rdd` record over an overlapping Health Connect
copy, whichever has the more credible distance. If the Fitbit run is imported, the counted session
for 25 November may stay the 1.5 km recording. This is not changed here.
