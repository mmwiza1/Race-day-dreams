# Audit area A — Running, health data and battery (main aa6fa233, report only)

## 1. Scope

**What I inspected (static source review only):**

- **Android recording:**
  - `RunRecordingService.kt`
  - `RecordingSessionCoordinator.kt`
  - `LocationSource.kt`
  - `RecordingSensorCoordinator.kt`
  - `MotionSource.kt`
  - `BleHeartRateSource.kt`
  - `data/RecordingStore.kt`
  - `coach/RecordingScreen.kt`
  - `ActiveRunPanel.kt`
  - `LiveRecordedRoute.kt`
- **Shared recording code:**
  - `SharedRecordingSessionCoordinator.kt`
  - `SharedActiveRunPanel.kt`
  - `RunMetricsEngine.kt`
  - `AutoPausePolicy.kt`
  - `RecordingTimeline.kt`
  - `SplitCalculator.kt` and `SampleSeries.kt` (segment rules)
- **iOS recording:**
  - `AppleRunRecorder.kt`
  - `AppleRecordingLocation.kt`
  - `AppleHeartRateSource.kt`
  - `AppleRecordingScreen.kt` (route tab)
  - `AppleCoachSession.kt` (startup recovery)
  - `Info.plist` background modes
- **Health:**
  - `HealthConnectGateway.kt`
  - `RecordedRunWriter.kt`
  - `HealthImportPolicy.kt`
  - `SharedAppleHealthGateway.kt`
  - `HealthKitRunningImport.kt`
  - `HealthCoordinator.kt`
  - `CoachViewModel.refresh`
- **Storage:**
  - `SqliteActivities.kt` (import, upsert, delete and annotations)
  - `SchemaStatements.kt`, `ApplicationSchema.kt` and `ProfileDatabase.java` (migrations)
  - `SqliteRetentionPolicy.kt` and the storage screen
  - Restore guards against active runs and gym sessions
- **Battery:**
  - `docs/BATTERY_EFFICIENCY_AUDIT.md`, section 0 (implementation status), checked against the code
  - Alarms, receivers and the update check in `AndroidManifest.xml`, `CoachActivity.kt`, `EveningReminders.java` and `GymRestAlerts.kt`

**What I could not verify:**

- No physical device was available, so I could not check:
  - real GNSS or Core Location behaviour while locked or in the background;
  - OEM process killing and sticky restart;
  - battery drain;
  - real Bluetooth heart-rate straps;
  - real Health Connect or HealthKit data;
  - whether an iPhone's system clock pauses during an active background location session.
- I ran no builds or tests (read-only brief).

**Battery-audit status, verified in code:**

- **A-1 (notification gate):** implemented. See `RunRecordingService.kt:65-79` and `:442-460`.
- **S-1 (incremental live route):** implemented on Android only. See `LiveRecordedRoute.kt:30` and `RecordingStore.pointsFrom`.
- **A-8 (merged refreshes):** implemented in `CoachOperationRunner`.
- **I-5 (`AppleAppVisibility`):** implemented in the gym screen.
- **Still open, as the document says:**
  - A-2: wake lock and 1 Hz GNSS during a manual pause. See `RunRecordingService.kt:249-259`.
  - I-1: iOS flush every second. See `AppleRunRecorder.kt:160-181`.
  - I-3: no iOS heart-rate reconnection. See `AppleHeartRateSource.kt:89-99`.
  - A-8: skipping unchanged reads.
- **Newly noticed:** the iOS live route tab has the same pattern S-1 described (see R-5), and the audit's "no networking" statement is now out of date (see R-8).

## 2. Confirmed bugs

| ID | Sev | Area | Files (path:line) | Evidence | Impact | Proposed fix |
|---|---|---|---|---|---|---|
| A-B1 | P2 | Android live run: Finish before GPS lock | `app/.../recording/RunRecordingService.kt:174, 180-188, 307`; `sharedUi/.../SharedActiveRunPanel.kt:276-299`; `app/.../data/RecordingStore.kt:72` | "Finish & save" is always enabled. `finishRecording()` starts with `check(pointCount() >= 2)`. The failure is caught, and because `intent.action == FINISH` the catch block sets `recording = null`, removes the foreground notification and calls `stopSelf()`. The session row stays ACTIVE with fewer than 2 points. The recovery card then appears, but "Recover recorded portion" fails because `finish` needs at least 2 points. The runner can only discard. iOS does not behave this way: `AppleRunRecorder.finish` checks before it changes any state (`AppleRunRecorder.kt:244`), so the run continues. | Tapping Finish early (indoors, or while waiting for GPS) ends the recording and shows a misleading "Saved staging is retained for recovery" message. It leaves a stub that blocks the next Start ("Recover the unfinished run…") until the runner discards it. | Treat a failed precondition as non-fatal: keep the service running and show "Waiting for usable GPS". Or offer "Discard run" when there are fewer than 2 points, and disable or relabel Finish until 2 points exist. |
| A-B2 | P2 | Android auto-pause (opt-in): auto-resume is stricter than recording | `sharedDomain/.../recording/AutoPausePolicy.kt:36-40, 52-58`; `RunMetricsEngine.kt:260, 303, 313-316`; `RunRecordingService.kt:285-298` | Auto-resume needs fixes with accuracy ≤ 8 m and speed ≥ 1.2 m/s for 5 continuous seconds. Any fix with accuracy over 8 m resets `movingSince`. The engine itself accepts fixes up to 35 m, but drops every fix while paused (`pausedSince >= 0 → return false`). | In typical urban or tree-covered GPS (8–35 m), an auto-paused run may never auto-resume. The runner keeps running while the run shows "Run paused", and that distance and moving time are not recorded. This is silent data loss until the runner notices and taps Resume. | Use the same accuracy ceiling for resume as the engine accepts (for example ≤ 20–35 m), or resume on speed alone after several consecutive moving fixes. Add a spoken or haptic cue on auto-pause and auto-resume. Add a test for resume under 10–20 m accuracy. |
| A-B3 | P3 | Auto-pause timing accuracy | `AutoPausePolicy.kt:46-49`; `RunRecordingService.kt:291-294`; `RunMetricsEngine.kt:216-218, 297` | Pause takes effect at detection time, after 30 s of standing still, so those 30 s count as moving time. Resume takes effect only after 5 s of movement. The triggering fix is then rejected (`f.mono < minimumFixTime = receivedMillis`), so the distance covered in those 5 s is lost. | Moving time is inflated by about 30 s per stop. Each stop also loses about 6+ m of distance plus the first fix, which skews average pace. | Backdate the pause to the start of the stationary window (`first.monotonicMillis()`) and the resume to `movingSince`, keeping the anchor fix. |

## 3. Risks and recommendations

| ID | Prio | Area | Files (path:line) | Evidence | Impact | Recommendation |
|---|---|---|---|---|---|---|
| R-1 | P1 (needs device) | iOS recording clock | `sharedUi/src/iosMain/.../AppleRecordingLocation.kt:21, 61-69`; `AppleRunRecorder.kt:133-146, 167` | The monotonic time base is `NSProcessInfo.systemUptime`, which Apple documents as time the system has been *awake*. Elapsed time, moving time, fix monotonic times (`received − age`) and `last_mono` all depend on it. Android uses `elapsedRealtime`, which includes deep sleep. | If the iPhone sleeps between 1 Hz deliveries while the screen is locked, elapsed and moving time and pace would be under-reported. Fix times derived from the wall clock would also drift against the uptime base. This cannot be confirmed without a device. | Use `mach_continuous_time()` or `clock_gettime_nsec_np(CLOCK_MONOTONIC_RAW)`. Add this to the physical checklist: on a locked iPhone, compare recorded elapsed time against a stopwatch. |
| R-2 | P2 | iOS crash safety in delegate callbacks | `AppleRunRecorder.kt:101-111`; `RunMetricsEngine.kt:240, 248` | Core Location and Core Bluetooth delegate callbacks run engine code (`qualifyDelivery`, `accept`, `publish`, which runs workout and cue updates and `require` checks) without try/catch. In Kotlin/Native an uncaught exception terminates the app. Android wraps the same path (`RunRecordingService.kt:277-302`). | A single unexpected `require` failure (clock ordering, invalid execution state) would crash the app mid-run. Recovery would still preserve data up to 1 s earlier. | Wrap the `onFix`, `onUnavailable` and heart-rate callbacks in `runCatching`, report the error through `mutableError`, and add a test with an out-of-order fix. |
| R-3 | P2 | Splits around pauses | `sharedDomain/.../SampleSeries.kt:69-78`; `SplitCalculator.kt:40-48` | Every pause or resume (engine `gap = true`) starts a new segment, and "No split or effort crosses them", so any kilometre that contains a pause is dropped. | Runners who pause at traffic lights, or use auto-pause, see gaps in the split list (marked UNCERTAIN), even though no distance was missing. | Treat explicit pause segments as continuous for distance (moving time excludes the pause) and keep exclusion only for real GPS gaps. Alternatively, show the split with a pause marker. |
| R-4 | P2 | Health Connect / Apple Health freshness | `sharedUi/.../HealthCoordinator.kt:40-51`; `app/.../CoachViewModel.kt:55-59` | An automatic import runs only while `baseline.reviewed != "true"`. After onboarding, new runs from other apps (Garmin, Samsung, Strava) arrive only when the runner manually syncs. | Weekly totals, adaptive reviews and duplicate grouping go stale without notice. Testers may report "my watch run is missing". | Offer an optional low-cost import on app start or resume (for example at most once every 6 h; the change token keeps it incremental), or show a clear "Last synced …" prompt on Home and Progress. |
| R-5 | P3 | iOS live route cost (S-1 equivalent) | `sharedUi/src/iosMain/.../AppleRecordingScreen.kt:120-129`; `AppleRunRecorder.kt:94-97` | While the route tab is visible, every 2 s it copies all engine points and rebuilds the whole projection, which is O(n) each time and quadratic over a run. The S-1 incremental fix applies only to Android `LiveRecordedRoute`. | Extra CPU with the screen on during long runs (about 14,400 points per refresh at marathon length). Screen-on only, so the impact is moderate. | Reuse `IncrementalRoutePoints`, or append only new points, and refresh every 5 s like Android. Measure first. |
| R-6 | P3 | Android start failure leaves a stub | `RunRecordingService.kt:199-236, 180-188` | `store.start()` commits the ACTIVE row before location, wake lock and sensors start. Any later exception stops the service and leaves a session with no points, which cannot be recovered. The next Start is then blocked until the runner discards it. | Confusing recovery card after a failed start (for example a permission revoked between the check and the start). | When a START fails, discard a session with zero points inside the same catch block. |
| R-7 | P3 | Still-open audit items | `RunRecordingService.kt:249-259` (A-2); `AppleRunRecorder.kt:160-181` (I-1); `AppleHeartRateSource.kt:89-99` (I-3) | Unchanged and recorded as deferred in the audit. iOS still does not reconnect a dropped strap (Android retries 3×: `BleHeartRateSource.kt:24`). | Battery during long manual pauses (unmeasured). iOS heart rate is lost for the rest of the run after a dropout. | Keep these deferred until the physical scenarios B and C are run. I-3 is a reliability item worth scheduling. |
| R-8 | P3 | Documentation accuracy | `docs/BATTERY_EFFICIENCY_AUDIT.md` §1 and A-9 ("no networking"); `app/src/main/AndroidManifest.xml:3` vs `:30`; `CoachActivity.kt:64-68` | The app now has the INTERNET permission and an opt-in daily update check when the app starts (not a background job). The manifest comment still says "No network". | Misleading privacy and battery statements. The behaviour itself is fine (foreground only, at most daily). | Update the audit and the manifest comment to describe the update check. |
| R-9 | P3 | iOS / Android parity | `AutoPausePolicy` is used only in `RunRecordingService.kt` | iOS has no auto-pause option. | Feature mismatch between platforms. | Port it after the A-B2 and A-B3 fixes. |

**Checked and found sound:**

- Staging appends are idempotent and the cursor advances only after a successful commit (`SharedRecordingSessionCoordinator.kt:58-82`, `RecordingStore.appendBatch`).
- A unique index allows only one unfinished session.
- Restore rejects ACTIVE, PAUSED and RECOVERABLE runs and active gym sessions.
- Schema migrations are additive and fail closed; the corruption handler retains the database files.
- The Health Connect import rolls back if permissions change mid-read, and the change token is committed atomically.
- Apple Health imports never infer deletions.
- Local notes and shoes for imported runs live in `activity_annotations`, so they survive re-sync.
- Retention cleanup runs only after an explicit preview and confirmation.
- Bluetooth heart rate is range-filtered (25–250 bpm) on both platforms.
- Alarms are inexact `setAndAllowWhileIdle`; there is no WorkManager job or periodic background work.

## 4. Future features and ideas

- Auto-pause on iOS, with optional audio feedback for pause and resume on both platforms.
- An optional automatic Health sync (throttled) with a "Last synced" indicator.
- A "Discard this run" action in the live panel before GPS lock.
- Split display that keeps paused kilometres, with pause markers.
- Battery-measurement scenarios B–F from the audit, run on a Galaxy S24 and an iPhone, plus a locked-screen elapsed-time check on iPhone (R-1).
