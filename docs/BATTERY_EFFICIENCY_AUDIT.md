# Race Day Dreams — Android and iOS battery efficiency audit

- **Scope:** assessment only. No application code was changed for this audit. The implementation that followed (0.15.3-test1) is recorded in Section 0.
- **Audited source:** `main` at `2196212` (0.13.1-test1 / versionCode 21; iOS 0.13.1 / build 21).
- **Date:** 19 November 2026.
- **Method:** static source review of every energy-relevant path (location, recording service, wake
  locks, Bluetooth, sensors, speech/audio, timers, notifications, alarms, health sync, refresh and
  dashboard computation), cross-checked against existing tests and `TEST_BUILD.md`.

> **No battery measurements were taken.** This environment has no physical Android phone or iPhone,
> no Battery Historian / Android Studio Energy Profiler session and no Xcode Instruments (Energy Log)
> session. Every "impact" below is a **code-level estimate** with a stated confidence, never a
> measured figure. No percentage savings are claimed. Section 11 gives the measurement procedure that
> would turn these estimates into evidence.

Constraint applied throughout: **never trade GPS accuracy, background recording reliability, Bluetooth
heart-rate monitoring or workout data integrity for battery.** Recommendations that touch the recording
pipeline are marked high-risk and deliberately separated from low-risk ones.

Finding labels:

| Label | Meaning |
| --- | --- |
| **Confirmed** | The inefficient behaviour is directly visible in the code (what happens and how often), even though its energy cost has not been measured. |
| **High-confidence risk** | The code very likely costs energy or CPU in a realistic scenario, but the size depends on device, data volume or OS behaviour. |
| **Needs profiling** | Plausible opportunity; do not change without measurements. |
| **Efficient — preserve** | Existing design that is already energy-conscious and should not be "optimised" away. |

---

## 0. Implementation status — 0.15.3-test1 (21 November 2026)

The owner authorised implementing the outstanding recommendations. Every finding was first
**revalidated on `main` at `e46d884` (0.15.2-test1)**, not on the audited `2196212`. All of A-1,
S-1, S-2, A-8, I-1, A-2, I-3 and I-5 were still present. S-2 had also grown: the duplicate-run
grouping added in 0.15.2 (`PhysicalRunSessions`) compared each record with every earlier group,
and it ran up to four times per dashboard. **No percentage battery saving is claimed: nothing here
was measured on a physical device.**

| ID | Decision | Change | Evidence (methodology below) |
| --- | --- | --- | --- |
| A-1 | **Implemented** | `RunRecordingService.publish()` posts only when the shown content changes. Content is the title plus the paused state, so Pause/Resume, the heart-rate foreground upgrade and a granted notification permission always post. `PendingIntent`s are created once. A delete intent re-shows a dismissed notification. `startForeground` is unchanged. Gate: `NotificationUpdateGate`. | 5K–marathon simulated runs (1 tick + 1 fix per second, one manual pause): **4 posts per run instead of 3,720–32,520**. `NotificationUpdateGateTest` (6 tests). |
| S-1 | **Implemented** | `LiveRecordedRoute` holds the decoded route and reads only `sequence >= held` through `RecordingStore.pointsFrom`. The store fails on any sequence gap, and any read failure discards the held copy, so the next refresh reads everything. A refresh with no new points does no projection work. The projection itself stays a full pass, because normalisation depends on the whole route's bounds. | Points decoded while the route is visible (flush and refresh every 5 s): 5K 324,900 → **1,800**; 10K 1,297,800 → **3,600**; half 5,187,600 → **7,200**; marathon 20,743,200 → **14,400**. One query per refresh, as before. Incremental route equals a full read, and so do its fit and render geometry, including gaps, segments and order. `IncrementalRoutePointsTest` (7 tests). |
| S-2 | **Implemented** | Weekly totals and running trend points bucket runs by ISO week once, computing each display date once. `PhysicalRunSessions.sessions` skips groups that ended before the next record starts, which is exact because records are visited by start time. `DashboardAnalytics.running` reuses its counted runs for the weekly trend. Strength analytics already bucket in one pass (`StrengthVolume.weekly`) and are unchanged. | Desktop JVM, median of 5 runs, 5-year history (1,872 records with duplicates): duplicate grouping 31.0 → **1.0 ms**; 52-week totals 2.5 → **0.2 ms**; the whole running dashboard (1 year) **1.6 ms**. 3 years: 7.3 → 0.7 ms and 1.6 → 0.3 ms. Identical results for 1/3/5 years and 4–104 weeks (`DashboardHistoryPerformanceTest`); the 12 dashboard contract tests and 10 duplicate-run tests pass unchanged. |
| S-2 (off-main) | **Not needed now** | After the linear rewrite, the heaviest running projection is about 1.6 ms on desktop for 5 years. Moving it to a background dispatcher would add a loading state and test idling risk for little gain. | Re-measure on a phone (scenario F) before revisiting. |
| A-8 | **Partly implemented** | Plain refreshes (no Health Connect import) are merged while one is still queued and has not started reading. Resume, permission results and screen entry often ask together, so one read replaces several. A request made after a read has started always queues another read, and import refreshes are never merged. | `CoachOperationRunnerTest`: four overlapping requests produce **1 read instead of 4**; a change during an in-progress read causes a second read that sees it; import and unmergeable refreshes all run. |
| A-8 (skip unchanged) | **Deferred** | Skipping the resume read when "nothing changed" is not safe yet. The read also derives time-dependent results (84-day baseline window, eligibility, adaptive and weekly reviews, reminder reconciliation), writes proposals and reviews, and rechecks Health permissions. The Android connection pool exposes no reliable database change counter without a schema change. | Needs a change-token design (for example triggers with a schema migration) and per-write-path tests. |
| I-1 | **Deferred (documented)** | While running, nearly every 1 s tick carries a new GPS point, so the commit is not redundant. During pauses or GPS gaps, the commit advances `last_mono`, which is the recovered end time and elapsed time after an iOS termination. No write is demonstrably unnecessary, so none was removed and the cadence was not lengthened. | Recommend Instruments (File Activity / Energy Log) on a real iPhone, scenario B, before an owner decision on pause-time cadence. |
| A-2 | **Deferred (documented)** | The wake lock and 1 Hz GNSS during a manual pause are unchanged, keeping Resume, auto-resume and splits instant and accurate. | Battery Historian, scenario B with a 10-minute manual pause, on a Galaxy S24. |
| I-5 | **Implemented** | The iOS gym workout screen drives the shared `setWorkoutVisible` from `UIApplicationDidEnterBackground` / `WillEnterForeground` (`AppleAppVisibility`), like Android's lifecycle. It checkpoints an unfinished workout and stops the 1 s display clock when backgrounded, then resumes on return. This replaces the screen's own always-running loop. | `AppleAppVisibilityTest` (iOS simulator); coordinator path shared with Android. Real iPhone background behaviour remains a physical check. |
| I-3 | **Deferred (separate PR)** | iOS heart-rate reconnection needs a real Bluetooth strap; the simulator has no Bluetooth. It is a reliability change, not a battery change. | Scenario C on a real iPhone with a strap. |

**Methodology:**

- **Notification posts (A-1):** the gate is driven exactly as `publish()` drives it, with 2 requests
  per simulated second.
- **Points decoded (S-1):** an append-only fake staging store mirrors `RecordingStore`'s gap-free
  sequences, with records added at 1 Hz and a refresh every 5 s. Full reloads decode
  `5·r(r+1)/2` points for `r` refreshes.
- **Dashboard timings (S-2):** `System.nanoTime` medians in `sharedDomain` `jvmTest` on the CI-class
  Linux container. The earlier algorithms are kept in the test as references, and outputs are
  compared for equality. The histories are synthetic:
  - four runs a week cycling through 5K, 10K, half-marathon and marathon lengths;
  - another app's Health Connect copy of the same run, manual copies, separate evening runs and
    runs without distance;
  - repeated syncs.

  Phone timings will be higher; only the ratios are meaningful.
- **Refreshes (A-8):** reads are counted on an instrumented `CoachOperationRunner` in the Android
  emulator matrix.

Physical-device measurements in Section 11 (scenarios B–F) remain **unperformed**.

---

## 1. Executive summary and overall assessment

Overall, the app is **energy-conscious by design**:

- **No idle background work:** there is no networking, analytics, polling service, periodic sync or
  WorkManager job.
- **Recording only while recording:** GPS, Bluetooth and the foreground service run only during a
  recording.
- **Gym timers use OS scheduling:** rest timers use operating-system alarms and notifications rather
  than a background service.
- **Screen never forced on:** nothing keeps the screen awake.

The dominant energy cost of a run, GNSS at 1 Hz with the screen off, is **inherent and appropriate**
for accurate running data.

The audit found **no P0 issue** (nothing that should block users or that drains battery while the app
is idle). It found a small number of **confirmed, low-risk inefficiencies** around a run:

- the foreground notification is rebuilt and re-posted roughly twice per second;
- the live route re-reads the entire run from SQLite every 5 seconds while visible;
- on iOS, the run is written to SQLite every second rather than every 5 seconds.

It also found **scaling risks with large histories**: every app resume re-reads the whole local
database, and the dashboards recompute over all records on the main thread.

Two **reliability observations** sit next to the battery findings:

- iOS never reconnects a dropped heart-rate monitor.
- On iOS, the gym screen does not checkpoint when the app is backgrounded.

Neither should be traded for battery; both are listed so they are not lost.

### Five most important findings

1. **A-1 (Confirmed, P1, low risk) — Android run notification re-posted about 2×/second.**
   - `RunRecordingService.publish()` calls `NotificationManager.notify()` on every 1-second tick and on
     every GPS fix, rebuilding the `Notification` and two `PendingIntent`s each time.
   - The notification text only changes on pause and resume.
   - Over a 90-minute run that is roughly 10,000 identical posts (system IPC and SystemUI work)
     with the screen off.
2. **S-1 (Confirmed, P1, low–medium risk) — Live route reloads the whole run every 5 s while on
   screen.**
   - `LiveRecordedRoute` re-queries and JSON-decodes **all** recorded points whenever the staged
     checkpoint changes. During a run, that is every flush (every 5 s).
   - The work grows linearly with run length and quadratically over a run.
   - It only happens while the screen is on and the route is visible.
3. **I-1 (Confirmed, P2, high risk — data integrity) — iOS writes recording state every second.**
   - `AppleRunRecorder` calls `flush()` (a SQLite transaction) on every 1-second tick. Android does so
     every 5 s.
   - This gives finer crash recovery on iOS, so any change needs an explicit durability decision and
     measurement, not a blind alignment.
4. **G-1 (High-confidence risk, P2) — Full database snapshot on every Android resume.**
   - `CoachActivity.onResume` → `refresh()` → `SharedCoachSnapshotReader.read()` loads every activity,
     strength session, weight, decision and more, then reconciles reminders.
   - Cost grows with history and repeats on every return to the app.
   - Dashboards then recompute over the full lists on the main thread; for example, the 1-year
     running trend scans all activities once per week, 52 times.
5. **A-2 (Needs profiling, P2, high risk) — The recording wake lock and 1 Hz GNSS stay fully active
   during a manual pause.**
   - Correct for auto-resume and for a fast "Resume", but a long manual pause keeps the CPU awake and
     GNSS at 1 Hz.
   - Only worth changing after measurement, and never at the expense of a reliable resume.

---

## 2. Android-specific findings

### A-1 — Foreground notification rebuilt and re-posted on every tick and every fix (Confirmed)

- **Evidence:**
  - `app/src/main/java/org/racedaydreams/recording/RunRecordingService.kt`: `publish()` (≈ lines
    415–431) calls `notify(104, notification(...))` each time.
  - It is called from the 1 s ticker (≈ lines 230–247) and from every location callback in
    `startLocation()` (≈ lines 250–278).
  - `notification()` (≈ lines 437–470) builds a new `Notification` plus a content `PendingIntent` and
    an action `PendingIntent` each call.
  - The visible title only changes between "Recording your run" and "Run paused".
- **Why it matters:** each `notify()` is a binder call into `NotificationManagerService`. SystemUI
  re-inflates the ongoing notification even when nothing changed. Android also rate-limits apps that
  post updates very frequently, which can delay a genuine pause/resume update.
- **Expected impact (estimate):** low-to-moderate CPU wakeups on top of GNSS during a run. Confidence
  in the *behaviour* is high; confidence in the *energy size* is low until measured.
- **Recommendation:**
  - Keep the notification for the whole run (it is required for the foreground service).
  - Post it only when its content actually changes: paused state, heart-rate foreground-type upgrade,
    or title.
  - Optionally cache the `PendingIntent`s.
- **Complexity / regression risk:** small; low risk.
  - Must keep `startForeground()` semantics and the Pause/Resume action intact.
  - Add a test that counts notification posts for an unchanged state.

### A-2 — Partial wake lock held for the whole recording, including manual pause (Needs profiling)

- **Evidence:**
  - `RunRecordingService.kt` ≈ lines 224–228 acquire `PARTIAL_WAKE_LOCK` "RaceDayDreams:recording"
    with a 30-minute timeout.
  - The ticker re-acquires it whenever it is not held (≈ line 235).
  - It is released in `onDestroy()` (≈ line 478) after finish or stop.
  - GNSS keeps running at 1 Hz during pause (`AndroidLocationSource` is never stopped by pause).
- **Assessment:**
  - Holding a partial wake lock during *active* recording is a deliberate reliability choice: 1 s
    timing, auto-pause evaluation, 5 s disk staging and cues with the screen off. It is justified.
  - During a long *manual* pause the CPU still cannot sleep between fixes.
- **Recommendation:**
  - Measure first (Section 11, scenario B with a 10-minute manual pause).
  - Only if a clear cost appears, consider relaxing the ticker cadence during manual pause while
    keeping GNSS on, so that "Resume" stays instant and accurate.
  - Do **not** stop GNSS during auto-pause: auto-resume depends on it.
- **Complexity / regression risk:** medium; **high regression risk** (timing, auto-pause, recovery).

### A-3 — GNSS request: GPS provider, 1000 ms, 0 m minimum distance (Efficient — preserve)

- **Evidence:** `app/src/main/java/org/racedaydreams/recording/LocationSource.kt` ≈ lines 23–31.
  It uses GNSS only, with no Play Services, network provider or background-location permission.
  Batched deliveries are sorted and qualified individually.
- **Assessment:** 1 Hz with no distance filter is the right accuracy/energy point for pace and splits.
  Reducing rate or adding a distance filter would degrade data quality and is **not recommended**.

### A-4 — Bluetooth heart rate (Efficient — preserve; one reliability note)

- **Evidence:** `app/src/main/java/org/racedaydreams/recording/BleHeartRateSource.kt`.
  - The scan is filtered to the Heart Rate service UUID, uses `SCAN_MODE_LOW_POWER` and auto-stops
    after 15 s.
  - The connection uses `autoConnect=false` over the LE transport, with notification-based
    measurements and no polling.
  - `stop()` closes the GATT connection and clears callbacks.
- **Note (reliability, not battery):** reconnection is attempted 3 times at 10 s intervals, then
  stops for the rest of the run. That is efficient; whether three attempts are enough is a product
  question for physical testing.

### A-5 — Cadence (Efficient — preserve)

- `MotionSource.kt` uses the hardware `TYPE_STEP_DETECTOR` at `SENSOR_DELAY_NORMAL`, with a bounded
  10 s window and no polling.
- The listener is unregistered on stop.

### A-6 — Speech and haptics (Efficient — preserve)

- **Evidence:** `CoachAudio.kt`.
  - Uses local TTS only.
  - Uses transient, duckable audio focus requested per cue and abandoned on completion or error.
  - Uses `QUEUE_FLUSH` with priority gating.
  - Speech is off by default.
  - `close()` shuts the engine down.
- The TTS engine stays bound for the run, which is normal and avoids per-cue start-up cost.

### A-7 — Rest timers and reminders (Efficient — preserve)

- **Rest timers:** `GymRestAlerts.kt` ≈ lines 82–87 schedule one
  `setAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP)` alarm per rest and cancel the previous one. There
  is no service and no wake lock.
- **Evening reminders:** `EveningReminders.java` ≈ line 148 uses a single
  `setAndAllowWhileIdle(RTC_WAKEUP)` alarm.
- **Gym display clock:** `SharedGymNowCoordinator.setWorkoutVisible` (≈ lines 789–825) ticks once a
  second **only while the workout screen is started**. It checkpoints and stops when hidden (driven
  by `GymWorkoutScreen.kt` lifecycle).

### A-8 — Full snapshot reload on every resume (High-confidence risk)

- **Evidence:**
  - `app/src/main/java/org/racedaydreams/coach/CoachActivity.kt` ≈ lines 57–60 call
    `model.refresh()` on every resume after the first.
  - That runs `SharedCoachSnapshotReader.read()`
    (`sharedUi/src/commonMain/kotlin/org/racedaydreams/coach/application/SharedCoachSnapshotReader.kt`,
    from ≈ line 37), which loads roughly 30 sources, including every activity, strength session and
    weight, and calls `reconcileReminders()`.
- **Assessment:** this is event-driven, not periodic, so it does not drain battery while idle. With
  years of history, each return to the app does a full read and re-projection.
- **Recommendation:**
  - Profile with a large synthetic history (Section 11, scenario F).
  - If significant, skip the refresh when no relevant data changed (change counter), or refresh
    lazily per screen.
- **Complexity / regression risk:** medium. A stale-state bug would be user-visible, so this needs
  tests for every write path.

### A-9 — No forced screen-on, no network, no periodic jobs (Efficient — preserve)

- No `FLAG_KEEP_SCREEN_ON` / `keepScreenOn` exists anywhere.
- There is no `INTERNET` permission, no WorkManager or JobScheduler, and no polling service.
- The foreground service declares `location|connectedDevice` and exists only during a recording.

---

## 3. iOS-specific findings

### I-1 — Recording state flushed to SQLite every second (Confirmed; data-integrity trade-off)

- **Evidence:**
  - `sharedUi/src/iosMain/kotlin/org/racedaydreams/recording/AppleRunRecorder.kt` ≈ lines 161–189:
    the 1 s ticker calls `missingFix`, `publish()`, `speech?.settle()` and **`active.flush()`** every
    second.
  - Android flushes every 5 ticks (`RunRecordingService.kt` ≈ line 238).
  - Each flush is a SQLite transaction. It is incremental (only new points/events plus the session
    row; see `SharedRecordingSessionCoordinator.flush`), but it commits even when no new points
    arrived, for example while paused.
- **Assessment:** about 5,400 small write transactions per 90-minute run versus about 1,080 on
  Android. Flash writes and fsyncs have an energy cost. However, iOS may terminate a backgrounded
  app with little warning, so per-second durability is a legitimate safety choice.
- **Recommendation:**
  - Do not change silently.
  - Options for owner decision: skip the commit when nothing changed (no new points/events and same
    paused state), which is low risk; or match Android's 5 s cadence while the app is backgrounded,
    which is higher risk.
  - Both require recovery tests (kill during run, then restore) and an Instruments Energy/Disk I/O
    comparison.
- **Complexity / regression risk:** small code change; **high integrity risk**.

### I-2 — Core Location configuration (Efficient — preserve)

- **Evidence:** `AppleRecordingLocation.kt` ≈ lines 92–97 and 109–122.
  - `kCLLocationAccuracyBest`, `CLActivityTypeFitness`, `pausesLocationUpdatesAutomatically = false`.
  - `allowsBackgroundLocationUpdates` is enabled only while listening and disabled on stop, with the
    background location indicator shown.
  - It only requests "When in use" permission.
- **Assessment:** correct for an accurate run. Do **not** switch to reduced accuracy, significant-change
  monitoring or automatic pausing; each would compromise recording reliability.

### I-3 — Heart-rate monitor: scan bounded, but no reconnection (Reliability observation)

- **Evidence:**
  - `AppleRunRecorder.kt` ≈ lines 54–62 bound the scan to 15 s with a service-UUID filter
    (`AppleHeartRateSource.kt` ≈ line 63). This is efficient.
  - On `didDisconnectPeripheral` (`AppleHeartRateSource.kt` ≈ lines 89–99) the monitor is cleared and
    **never reconnected**.
- **Assessment:** not a battery problem. A strap that briefly drops loses heart rate for the rest of
  the run on iOS, whereas Android retries 3 times.
- **Recommendation (separate reliability PR):** bounded reconnection matching Android, via a pending
  `connectPeripheral`, which iOS keeps cheaply. Out of scope for battery work.

### I-4 — Audio session (Efficient — preserve)

- `AppleCoachSpeech.kt` ≈ lines 34–75 activate a `.playback` session with `duckOthers` only while
  speaking. On completion they deactivate with `notifyOthersOnDeactivation`.
- The `audio` background mode in `iosApp/Info.plist` (≈ lines 52–57) is used only for spoken cues
  during a run.

### I-5 — Gym screen display loop (Observation)

- **Evidence:** `AppleGymScreen.kt` ≈ lines 75–79 run a `LaunchedEffect` loop calling
  `observeWorkoutTime()` every second while composed. iOS does not call `setWorkoutVisible`, so the
  Android-style "checkpoint and stop when hidden" path is unused.
- **Assessment:** without a background mode during gym sessions, iOS suspends the app shortly after
  backgrounding, so the battery impact is limited. The bigger point is **data integrity**: there is
  no checkpoint when the app is backgrounded.
- **Recommendation:** drive `setWorkoutVisible` from the iOS scene lifecycle in a later reliability PR.

### I-6 — Background modes (Efficient — preserve)

- `audio`, `bluetooth-central` and `location` are declared. Each is exercised only while recording.
  There are no background fetch or processing tasks.

---

## 4. Shared Kotlin / Compose findings

### S-1 — Live route reloads all points every 5 s while visible (Confirmed)

- **Evidence:** `app/src/main/java/org/racedaydreams/coach/LiveRecordedRoute.kt` ≈ lines 30–67.
  - While the host is STARTED, every 5 s, if the staged checkpoint `(id, last)` changed (it changes
    on every flush), it calls `RecordingStore.points(id)`.
  - `RecordingStore.kt` ≈ line 61 issues `SELECT payload … ORDER BY sequence` and JSON-decodes
    **every** point, then `PreparedLiveRoute.fromPoints` re-projects them all.
- **Assessment:** roughly 5,400 rows decoded every 5 s near the end of a 90-minute run while the
  screen is on. CPU only matters while the user is looking; with the screen locked the collector is
  stopped (`repeatOnLifecycle`). Still a clear, avoidable O(n)-per-refresh cost.
- **Recommendation:** append incrementally (read `sequence > last seen`) and extend the prepared
  projection. Keep the existing "read failure never stops the recorder" behaviour.
- **Complexity / regression risk:** medium; low risk to recording (read-only path). Needs projection
  tests for incremental equals full.

### S-2 — Dashboard computation over full history on the main thread (High-confidence risk)

- **Evidence:** `sharedUi/src/commonMain/kotlin/org/racedaydreams/coach/SharedDashboards.kt` ≈ lines
  76, 81, 188, 301 and 305. `DashboardAnalytics.running/strength`, `planCompletion`,
  `sharedRunningTrendPoints` and `programmeSchedule` run inside `remember` on the UI thread.
  - `sharedRunningTrendPoints` (`RunningTrendPoints.kt` ≈ lines 17–33) filters **all** activities for
    each week: 52 passes for "1 year".
  - `StrengthRecords.progression` processes every strength session.
  - The `remember` keys are whole lists, so a new snapshot instance (every resume, A-8) recomputes
    everything.
- **Assessment:** not continuous work, only on open, period change or refresh, but it can cause frame
  drops and wasted CPU with large histories.
- **Recommendation:**
  - Profile (scenario F).
  - If needed, bucket activities by week once (O(n)) and move heavy projections off the main thread
    with `produceState` on `Dispatchers.Default`.
  - Keep results identical (the existing 12 contract tests).
- **Complexity / regression risk:** medium; low data risk.

### S-3 — Per-tick coaching evaluation (Efficient enough — preserve)

- `updateExecution` / `cues.update` run on every tick and fix on both platforms. Each is in-memory,
  bounded work.
- `RunMetricsEngine.status()` walks back at most about 30 s of points. It does **not** scale with
  run length.
- No change recommended.

### S-4 — Incremental, transactional recording staging (Efficient — preserve)

- `SharedRecordingSessionCoordinator.flush` writes only new points and events in one transaction.
  Cursors advance only after acknowledgement.
- `RecordingStore.appendBatch` (`RecordingStore.kt` ≈ lines 39–58) is idempotent for retries. This
  is both efficient and integrity-preserving.

### S-5 — Health Connect / HealthKit (Efficient — preserve)

- Imports are on demand only. `HealthCoordinator.beforeRefresh` auto-imports only before the
  baseline is first reviewed (≈ lines 40–52).
- History reads are paginated with a hard cap (`HealthIntelligenceGateway.kt` ≈ line 29, 100 pages).
  Step counts use HC aggregation.
- There is no background sync, no observer query and no periodic job.

### S-6 — Coroutine and listener lifecycles (Efficient — preserve)

- Recording scopes, tickers, location, sensors and Bluetooth are all cancelled or closed in
  `onDestroy` / `finishRecording` (Android) and in the ticker `finally` (iOS).
- Compose collectors use `repeatOnLifecycle`.
- No leaked listeners were found in the audited paths.

---

## 5. Prioritised recommendations

There are **no P0 items**: nothing drains the battery while idle, and nothing is broken.

| Priority | ID | Recommendation | Platform | Risk |
| --- | --- | --- | --- | --- |
| **P1** | A-1 | Post the run notification only when its content changes | Android | Low |
| **P1** | S-1 | Incremental live-route loading instead of a full reload every 5 s | Android (shared projection) | Low–medium |
| **P2** | S-2 | Week-bucketed dashboard projections off the main thread | Shared | Medium |
| **P2** | A-8 | Avoid a full snapshot reload on resume when nothing changed | Shared / Android host | Medium |
| **P2** | I-1 | Skip no-change SQLite commits on iOS; decide flush cadence with measured evidence | iOS | High (integrity) |
| **P2** | A-2 | Measure the manual-pause wake-lock and GNSS cost before any change | Android | High |
| **P3** | I-3 | iOS heart-rate reconnection (reliability, not battery) | iOS | Medium |
| **P3** | I-5 | iOS gym checkpoint on backgrounding (integrity, minor battery) | iOS | Low–medium |

---

## 6. Source references (summary)

| Area | File | Approx. lines |
| --- | --- | --- |
| Android recording service, wake lock, ticker, notification | `app/src/main/java/org/racedaydreams/recording/RunRecordingService.kt` | 224–248, 250–278, 415–470, 472–486 |
| Android GNSS | `app/src/main/java/org/racedaydreams/recording/LocationSource.kt` | 17–60 |
| Android BLE heart rate | `app/src/main/java/org/racedaydreams/recording/BleHeartRateSource.kt` | 13–31 |
| Android cadence | `app/src/main/java/org/racedaydreams/recording/MotionSource.kt` | 5–11 |
| Android speech | `app/src/main/java/org/racedaydreams/recording/CoachAudio.kt` | 11–49 |
| Live route reload | `app/src/main/java/org/racedaydreams/coach/LiveRecordedRoute.kt`; `data/RecordingStore.kt` | 30–67; 61 |
| Staging writes | `sharedUi/.../recording/SharedRecordingSessionCoordinator.kt`; `data/RecordingStore.kt` | 58–82; 39–58 |
| Resume refresh | `app/src/main/java/org/racedaydreams/coach/CoachActivity.kt`; `sharedUi/.../application/SharedCoachSnapshotReader.kt` | 57–60; 37+ |
| Dashboards | `sharedUi/.../coach/SharedDashboards.kt`; `RunningTrendPoints.kt` | 76–81, 188, 301–312; 17–33 |
| Rest / reminder alarms | `coach/strength/GymRestAlerts.kt`; `reminders/EveningReminders.java` | 82–87; 148 |
| Gym display tick | `sharedUi/.../strength/SharedGymNowCoordinator.kt`; `iosMain/.../AppleGymScreen.kt` | 789–825; 75–79 |
| iOS location | `sharedUi/src/iosMain/kotlin/org/racedaydreams/recording/AppleRecordingLocation.kt` | 92–122 |
| iOS recorder ticker / flush / scan | `sharedUi/src/iosMain/kotlin/org/racedaydreams/recording/AppleRunRecorder.kt` | 54–62, 161–189, 286–322 |
| iOS heart rate | `sharedUi/src/iosMain/kotlin/org/racedaydreams/recording/AppleHeartRateSource.kt` | 26–99 |
| iOS speech | `sharedUi/src/iosMain/kotlin/org/racedaydreams/recording/AppleCoachSpeech.kt` | 34–75 |
| Manifest / background modes | `app/src/main/AndroidManifest.xml`; `iosApp/Info.plist` | 18–28, 47; 52–57 |

Line numbers refer to `main` at `2196212` and are approximate (±2) where noted.

---

## 7. Expected impact and confidence

All impacts are **estimates from code inspection**. "Behaviour confidence" is how sure we are the code
does this; "energy confidence" is how sure we are it matters on a real battery.

| ID | Behaviour confidence | Energy confidence | Estimated impact (unmeasured) |
| --- | --- | --- | --- |
| A-1 | High | Low–medium | Small-to-moderate extra wakeups during every run |
| S-1 | High | Medium (screen-on only) | Grows with run length while the route is visible |
| I-1 | High | Low–medium | Extra flash writes on iOS runs |
| A-8 | High | Low–medium | Per-resume cost grows with history |
| S-2 | High | Low (CPU bursts) | Frame time and CPU on dashboard open with large histories |
| A-2 | High | Unknown | Only during long manual pauses |

GNSS at 1 Hz, the screen and (when used) Bluetooth will dominate energy use during a run on both
platforms. None of the recommendations reduces those, by design.

---

## 8. Implementation complexity and regression risks

| ID | Complexity | Main regression risks | Required tests |
| --- | --- | --- | --- |
| A-1 | Small | Losing pause/resume notification updates; foreground-service compliance | Count posts per unchanged state; pause/resume updates still post |
| S-1 | Medium | Projection drift between incremental and full loads | Incremental equals full projection; recovery after app restart |
| S-2 | Medium | Changed dashboard figures | Existing 12 `DashboardAnalyticsContractTest` cases unchanged; new bucket tests |
| A-8 | Medium | Stale UI after writes from other screens or services | A refresh trigger for every write path |
| I-1 | Small code, high integrity | Losing up to N seconds of data on termination | Kill-and-restore tests at each cadence |
| A-2 | Medium | Missed auto-pause or resume, timing gaps | Pause/resume timelines on emulator and device |

---

## 9. Existing efficient design decisions worth preserving

- GNSS-only 1 Hz recording, without network location or Play Services (A-3, I-2).
- Foreground service and background location exist **only** during a recording; no background-location
  permission is requested.
- Bluetooth scans are UUID-filtered, low-power and stop after 15 s; heart rate is delivered by
  notification, never polled.
- The hardware step detector is used at normal rate.
- Speech uses transient audio focus or session activation per cue; speech is off by default.
- Rest timers and reminders use OS alarms and notifications, never a service.
- The gym display clock stops when hidden (Android).
- Recording staging is incremental and transactional; cursors advance only on acknowledgement.
- There is no network, analytics, account, WorkManager, periodic sync or forced screen-on.
- Health imports are on demand and paginated with a hard cap.

---

## 10. Recommended improvements ranked by benefit versus effort

1. **A-1** notification de-duplication: likely benefit on every Android run, smallest effort, lowest
   risk.
2. **S-1** incremental live route: clear CPU saving whenever the route is watched; moderate effort,
   read-only path.
3. **S-2** dashboard bucketing and off-main-thread projection: benefits heavy users; moderate effort.
4. **A-8** change-aware refresh: benefits heavy users on every resume; moderate effort, needs care.
5. **I-1** no-change commit skip on iOS: small effort but must follow an explicit durability decision.
6. **A-2** pause-time cadence: only after measurements show a meaningful cost.

---

## 11. Reproducible battery-testing instructions

Use the **same build** (record the version, commit and SHA-256 from the release build-info), a
charged battery (≥ 90 %), brightness fixed, and no other apps running. Repeat each scenario at least
twice. Record the device model, OS version, battery-saver and Low Power Mode state, and weather and
GPS conditions.

### Android (Pixel or Galaxy S24, API 34+)

1. `adb shell dumpsys batterystats --reset`; unplug, or use `adb shell dumpsys battery unplug`
   during short tests only.
2. Run the scenario. Then `adb bugreport bugreport.zip` and load it into
   **Battery Historian**. Alternatively, profile with **Android Studio → Energy Profiler** for
   attached sessions.
3. Capture:
   - wake-lock time for `RaceDayDreams:recording`;
   - GPS-on time;
   - BLE scan and connection time;
   - CPU-awake time;
   - `dumpsys notification` post counts (for A-1).
4. For SQLite and CPU: use `adb shell top -m 10 -d 5` and a **Perfetto** trace (CPU, sched, binder,
   disk) to see the S-1 and A-8 work.

### iPhone (iPhone 13 or later, iOS 18+)

1. Sign the unsigned build with your own team (see `TEST_BUILD.md`). Run it from Xcode with
   **Instruments → Energy Log** and **Time Profiler / File Activity**. For untethered runs, enable
   **Settings → Developer → Logging → Energy** and import the log afterwards.
2. Capture:
   - Location (GPS) activity;
   - CPU wakes;
   - disk writes per minute (I-1);
   - Bluetooth activity;
   - audio session time.
3. Compare **Settings → Battery** per-app percentages only as a coarse cross-check, never as the main
   evidence.

### Scenarios (both platforms)

| ID | Scenario | What to record |
| --- | --- | --- |
| A | App idle in foreground (5 min, screen on), then backgrounded (60 min) | CPU wakes and wake locks while idle; expected near zero in the background |
| B | 60–90 min outdoor GPS run, screen locked, includes a 10 min manual pause | GPS time, wake-lock time, notification posts (Android), disk writes (iOS) |
| C | B plus a Bluetooth heart-rate strap, including one deliberate 30 s disconnect | BLE time, reconnect behaviour (Android retries; iOS currently does not) |
| D | Live Coach structured workout with speech on (about 40 min) | Audio-session time, TTS CPU, cue count |
| E | Gym Now workout (about 60 min) with rest timers, phone mostly locked | Alarm wakeups, CPU while locked; expected minimal |
| F | Dashboard browsing with a large synthetic history (for example 3 years: about 600 runs and 400 gym sessions) imported from a test backup: open Progress and Strength, switch all periods, background and resume 10 times | Main-thread time, frame drops, CPU per resume |

---

## 12. Outstanding physical-device measurements

All of the following are **unperformed**:

- **Run scenarios:** B, C and D on a Galaxy S24, a Pixel and a recent iPhone.
- **Background and power modes:** 2–4 hour screen-off runs, and runs with battery saver / Low Power
  Mode enabled.
- **OEM behaviour:** background restrictions (Samsung, Xiaomi) affecting the foreground service.
- **Large-history profiling:** scenario F with a realistic large history.
- **Before/after measurements:** for any optimisation the owner authorises from Section 5.

Until then, no statement in this report should be read as a measured battery saving.

---

## 13. Suggested future PRs

Keep the low-risk changes separate from recording-pipeline changes. Do **not** start any of these
without owner approval of the specific recommendation.

1. **"Battery: de-duplicate run notification updates" (A-1)**: Android only, low risk, focused tests.
2. **"Battery: incremental live route loading" (S-1)**: read-only path, projection tests.
3. **"Performance: dashboard projections for large histories" (S-2, then A-8)**: shared, result-
   identical, with large-history fixtures.
4. **"Recording durability policy on iOS" (I-1)**: separate, high-risk PR with kill-and-restore tests
   and before/after Instruments evidence. It needs an explicit owner decision on acceptable data
   loss.
5. **"Recording: manual-pause energy" (A-2)**: only if scenario B measurements justify it.
6. **"Reliability: iOS heart-rate reconnection and gym backgrounding checkpoint" (I-3, I-5)**:
   reliability, not battery; keep separate.
