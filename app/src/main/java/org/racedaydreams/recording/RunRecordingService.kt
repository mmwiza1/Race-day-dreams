package org.racedaydreams.recording

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.racedaydreams.coach.CoachActivity
import org.racedaydreams.data.RecordingDiagnostics
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.data.Release08Store
import org.racedaydreams.data.SupportStore
import org.racedaydreams.domain.execution.CoachCues
import org.racedaydreams.domain.execution.RecordingCueCoordinator
import org.racedaydreams.domain.execution.WorkoutExecutionCoordinator
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.recording.AutoPausePolicy
import org.racedaydreams.domain.recording.NotificationUpdateGate
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.recording.RunTroubleshooting
import org.racedaydreams.domain.support.DreamRoutes

/** The service owns the Android recording lifetime; UI sends explicit actions only. */
class RunRecordingService : Service() {
    private var execution: WorkoutExecutionCoordinator? = null
    private var executionContext: JSONObject? = null
    private val cues = RecordingCueCoordinator()
    private var audio: CoachAudio? = null
    private var wake: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val io = Dispatchers.IO.limitedParallelism(1)
    private lateinit var store: RecordingStore
    private lateinit var location: LocationSource
    private lateinit var sensors: RecordingSensorCoordinator
    private val autoPause = AutoPausePolicy()
    private var manualPause = false
    private var recording: RecordingSessionCoordinator? = null

    private data class DiagnosticCheckpoint(
        val id: String,
        val start: Instant,
        val state: String,
        val snapshot: RunTroubleshooting.Snapshot,
    )

    private val diagnosticCheckpoints = Channel<DiagnosticCheckpoint>(Channel.CONFLATED)
    private var troubleshooting: RunTroubleshooting? = null
    private var lastDiagnosticCheckpoint: Long? = null
    private val engine: RunMetricsEngine?
        get() = recording?.engine

    private val commandLock = Mutex()
    private var ticker: Job? = null

    /** What the ongoing notification shows; it is re-posted only when this changes. */
    private data class Shown(val title: String, val paused: Boolean)

    private val notificationGate = NotificationUpdateGate<Shown>()
    private val openIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, CoachActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
    private val pauseIntent by lazy { serviceIntent(PAUSE) }
    private val resumeIntent by lazy { serviceIntent(RESUME) }
    private val dismissedIntent by lazy { serviceIntent(NOTIFICATION_DISMISSED) }

    companion object {
        val dreamRoute = MutableStateFlow<String?>(null)
        val savedActivity = MutableStateFlow<String?>(null)
        val workoutState = MutableStateFlow<WorkoutExecutionEngine.State?>(null)
        val cue = MutableStateFlow<String?>(null)
        val raceContext = MutableStateFlow<String?>(null)
        val running = MutableStateFlow(false)
        val heartRate = MutableStateFlow<Double?>(null)
        val status = MutableStateFlow<RunMetricsEngine.Status?>(null)
        val message = MutableStateFlow<String?>(null)
        const val SKIP = "SKIP_STEP"
        const val START = "START"
        const val PAUSE = "PAUSE"
        const val RESUME = "RESUME"
        const val FINISH = "FINISH"
        /** Sent when the runner swipes the notification away; it is shown again. */
        const val NOTIFICATION_DISMISSED = "NOTIFICATION_DISMISSED"
    }

    override fun onCreate() {
        super.onCreate()
        store = RecordingStore(this)
        val diagnosticStore = RecordingDiagnostics(applicationContext)
        val checkpoints = diagnosticCheckpoints
        // A stalled diagnostic write never holds up the recording journal. Keep at most one
        // waiting snapshot, since the newest one contains all counters and retained events.
        CoroutineScope(Dispatchers.IO).launch {
            for (checkpoint in checkpoints) runCatching {
                diagnosticStore.write(
                    checkpoint.id,
                    checkpoint.start,
                    checkpoint.state,
                    checkpoint.snapshot,
                )
            }
        }
        location = AndroidLocationSource(this)
        sensors =
            RecordingSensorCoordinator({ StepMotionSource(this) }, { BleHeartRateSource(this) })
        audio = runCatching { CoachAudio(this) }.getOrNull()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel("recording", "Active run", NotificationManager.IMPORTANCE_LOW)
            )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            scope.launch {
                withContext(io) { store.recoverable() }
                message.value = "An interrupted recording is available to recover."
                stopSelf()
            }
            return START_NOT_STICKY
        }
        if (engine == null && intent.action != START) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Foreground entry remains synchronous, before any explicit START disk work.
        if (intent.action == START) {
            running.value = true
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    104,
                    notification("Preparing GPS recording"),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            } else {
                startForeground(104, notification("Preparing GPS recording"))
            }
            notificationGate.posted(shown("Preparing GPS recording"))
        }
        scope.launch {
            commandLock.withLock {
                try {
                    when (intent.action) {
                        START -> if (engine == null) startRecording(intent)
                        "COMPLETE_OPEN" -> {
                            execution?.completeOpen()
                            publish()
                            flush()
                        }
                        SKIP -> {
                            execution?.skip()
                            publish()
                            flush()
                        }
                        PAUSE -> {
                            audio?.silence()
                            announce("Run paused.", 0)
                            manualPause = true
                            autoPause.reset()
                            engine?.pause(SystemClock.elapsedRealtime(), Instant.now())
                            diagnosticEvent("manual_pause")
                            flush()
                            publish()
                        }
                        RESUME -> {
                            announce("Run resumed.", 0)
                            manualPause = false
                            autoPause.reset()
                            engine?.resume(SystemClock.elapsedRealtime(), Instant.now())
                            diagnosticEvent("manual_resume")
                            flush()
                            publish()
                        }
                        FINISH -> requestFinish()
                        NOTIFICATION_DISMISSED -> {
                            diagnosticEvent("notification_dismissed")
                            notificationGate.invalidate()
                            publish()
                        }
                    }
                } catch (_: Exception) {
                    diagnosticEvent("recording_command_failed")
                    checkpointDiagnostics("operation_failed", force = true)
                    message.value =
                        "Recording operation could not complete. Saved staging is retained for recovery."
                    // A start that failed before any GPS point leaves nothing to recover; drop its
                    // empty staging so the next Start is not blocked (audit A-R6).
                    if (intent.action == START) discardEmptyUnfinished()
                    if (engine == null || intent.action == FINISH || intent.action == START) {
                        recording = null
                        status.value = null
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
        return START_STICKY
    }

    private suspend fun startRecording(intent: Intent) {
        message.value = null
        cue.value = null
        savedActivity.value = null
        if (withContext(io) { store.unfinished() } != null) {
            message.value = "Recover the unfinished run before starting another."
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val wall = Instant.now()
        val session = RecordingStore.Session(UUID.randomUUID().toString(), "ACTIVE", wall, now, now)
        withContext(io) {
            val version = intent.getStringExtra("planVersion")
            val workout = intent.getStringExtra("workoutId")
            if (version != null && workout != null) {
                executionContext =
                    SupportStore(this@RunRecordingService)
                        .prepare(version, workout, intent.getBooleanExtra("hilly", false))
            }
            intent.getStringExtra("customSnapshot")?.let {
                require(it.length < 300000)
                val snapshot = JSONObject(it)
                SupportStore.validatePayload("execution", snapshot)
                require(snapshot.getString("mode") in listOf("quick", "custom", "repeat"))
                executionContext = snapshot
            }
            val preferences = Release08Store(this@RunRecordingService).dreamPreferences()
            dreamRoute.value =
                if (preferences.first && preferences.second.isNotEmpty()) {
                    DreamRoutes.assign(42195.0, java.security.SecureRandom(), preferences.second)
                        .routeId()
                } else null
            val origin =
                JSONObject()
                    .put("version", 1)
                    .put("mode", executionContext?.optString("mode") ?: "free")
                    .put("at", wall.toString())
                    .put("dreamRoute", dreamRoute.value)
            store.start(session.id, wall, now, executionContext, origin)
        }
        executionContext?.let { execution = WorkoutExecutionCoordinator(SupportStore.steps(it)) }
        recording =
            RecordingSessionCoordinator(
                session,
                SqliteRecordingRepository(store),
                io,
                { SystemClock.elapsedRealtime() },
                { executionContext },
                { execution?.events() ?: emptyList() },
            )
        troubleshooting = RunTroubleshooting(now)
        diagnosticEvent("service_started")
        diagnosticEvent("foreground_service_entered")
        publish()
        startLocation()
        diagnosticEvent("location_requested")
        wake =
            getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RaceDayDreams:recording")
                .apply { acquire(30 * 60 * 1000L) }
        startSensors()
        ticker =
            scope.launch {
                var ticks = 0
                while (isActive) {
                    delay(1000)
                    if (wake?.isHeld == false) wake?.acquire(30 * 60 * 1000L)
                    engine?.missingFix(SystemClock.elapsedRealtime(), Instant.now())
                    publish()
                    if (++ticks % 5 == 0) {
                        try {
                            flush()
                        } catch (_: Exception) {
                            message.value =
                                "Disk staging delayed; recording remains in memory. Free storage and finish the run."
                        }
                    }
                }
            }
        flush()
    }

    private fun startLocation() {
        location.start(
            onBatch = { count, oldest ->
                troubleshooting?.callback(SystemClock.elapsedRealtime(), count, oldest)
            }
        ) { fix ->
            try {
                val currentEngine = engine ?: return@start
                val receivedMillis = SystemClock.elapsedRealtime()
                val receivedWall = Instant.now()
                if (!currentEngine.qualifyDelivery(fix, receivedMillis, receivedWall)) {
                    troubleshooting?.fix(
                        receivedMillis,
                        fix.monotonicMillis(),
                        false,
                        currentEngine.deliveryRejection(),
                    )
                    publish()
                    return@start
                }
                if (
                    !manualPause &&
                        getSharedPreferences("run-sensors", MODE_PRIVATE)
                            .getBoolean("auto-pause", false)
                ) {
                    // Transitions apply from when they happened, not when they were confirmed:
                    // the start of the still window, or the first moving fix (audit A-B3).
                    when (autoPause.observe(fix)) {
                        AutoPausePolicy.Decision.PAUSE -> {
                            diagnosticEvent("auto_pause")
                            currentEngine.pause(
                                receivedMillis,
                                receivedWall,
                                true,
                                autoPause.transitionMillis(),
                            )
                        }
                        AutoPausePolicy.Decision.RESUME -> {
                            diagnosticEvent("auto_resume")
                            currentEngine.resume(
                                receivedMillis,
                                receivedWall,
                                true,
                                autoPause.transitionMillis(),
                            )
                        }
                        else -> Unit
                    }
                }
                val accepted = currentEngine.accept(fix)
                troubleshooting?.fix(
                    receivedMillis,
                    fix.monotonicMillis(),
                    accepted,
                    if (!accepted && currentEngine.status(receivedMillis).paused()) "paused"
                    else null,
                )
                publish()
            } catch (_: Exception) {
                diagnosticEvent("location_processing_failed")
                message.value = "GPS observation rejected."
            }
        }
    }

    /**
     * Finish never stops the service before the save precondition holds (audit A-B1). Without a GPS
     * lock recording continues with an explanation; a second Finish within the confirmation window
     * discards the empty run, so an indoor or no-signal start can still be ended cleanly.
     */
    private suspend fun requestFinish() {
        val active = recording ?: return finishRecording()
        when (active.requestFinish(SystemClock.elapsedRealtime())) {
            SharedRecordingSessionCoordinator.FinishRequest.SAVE -> finishRecording()
            SharedRecordingSessionCoordinator.FinishRequest.NEEDS_GPS -> {
                message.value = SharedRecordingSessionCoordinator.NEEDS_GPS_MESSAGE
                publish()
            }
            SharedRecordingSessionCoordinator.FinishRequest.DISCARD_EMPTY -> {
                sensors.stop()
                location.stop()
                ticker?.cancel()
                withContext(io) { store.discardUnfinished(active.session.id) }
                diagnosticEvent("empty_session_discarded")
                diagnosticEvent("service_stopping")
                checkpointDiagnostics("discarded", force = true)
                message.value = SharedRecordingSessionCoordinator.DISCARDED_EMPTY_MESSAGE
                recording = null
                status.value = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun discardEmptyUnfinished() {
        runCatching {
            withContext(io) {
                store.unfinished()?.let {
                    if (store.points(it.id).isEmpty()) store.discardUnfinished(it.id)
                }
            }
        }
    }

    private suspend fun finishRecording() {
        check((engine?.pointCount() ?: 0) >= 2) { "Waiting for usable GPS" }
        sensors.stop()
        location.stop()
        ticker?.cancel()
        execution?.finishEarly()
        publish()
        engine?.event("FINISH", SystemClock.elapsedRealtime(), Instant.now())
        flush()
        val saved = recording?.finish() ?: error("No staged recording")
        diagnosticEvent("session_saved")
        diagnosticEvent("service_stopping")
        checkpointDiagnostics("saved", force = true)
        savedActivity.value = saved
        message.value = "Run saved. Open Calendar to view it."
        recording = null
        status.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startSensors() {
        val motionAllowed =
            Build.VERSION.SDK_INT < 29 ||
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
                    PackageManager.PERMISSION_GRANTED
        val preferred =
            getSharedPreferences("run-sensors", MODE_PRIVATE).getString("hr-device", null)
        val heartRateAllowed =
            Build.VERSION.SDK_INT < 31 ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        sensors.start(
            motionEnabled = motionAllowed,
            heartRateAddress = preferred?.takeIf { heartRateAllowed },
            onCadence = { value, time ->
                engine?.cadence(value, time)
                value?.let {
                    engine?.sensorEvent("CADENCE", it, SystemClock.elapsedRealtime(), Instant.now())
                }
            },
            onHeartRate = { value ->
                heartRate.value = value
                engine?.heartRate(value, SystemClock.elapsedRealtime())
                value?.let {
                    engine?.sensorEvent(
                        "HEART_RATE",
                        it,
                        SystemClock.elapsedRealtime(),
                        Instant.now(),
                    )
                }
            },
            onConnection = { connected ->
                if (cues.heartRateConnection(connected)) {
                    announce("Heart-rate monitor disconnected. Recording continues.", 0, false)
                }
                engine?.event(
                    if (connected) "HR_CONNECTED" else "HR_DISCONNECTED",
                    SystemClock.elapsedRealtime(),
                    Instant.now(),
                )
            },
            beforeHeartRate = {
                if (Build.VERSION.SDK_INT >= 29) {
                    startForeground(
                        104,
                        notification("Recording with heart-rate monitor"),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                    )
                    notificationGate.posted(shown("Recording with heart-rate monitor"))
                }
            },
            onUnavailable = { message.value = it },
        )
    }

    private fun announce(text: String, priority: Int = 1, haptic: Boolean = true) {
        cue.value = text
        audio?.deliver(
            CoachCues.Cue(
                "state-${SystemClock.elapsedRealtime()}",
                text,
                priority,
                SystemClock.elapsedRealtime(),
                haptic,
            )
        )
    }

    private fun updateExecution(metrics: RunMetricsEngine.Status) {
        val update = execution?.update(metrics)
        if (update != null) {
            workoutState.value = update.state()
            for (event in update.newEvents()) {
                engine?.sensorEvent(
                    event.type(),
                    event.step().toDouble(),
                    SystemClock.elapsedRealtime(),
                    Instant.now(),
                )
            }
        }
        val context = executionContext
        val race =
            context
                ?.takeIf { it.optString("mode") == "race" }
                ?.let {
                    RecordingCueCoordinator.RaceTarget(
                        it.getDouble("goalMetres"),
                        if (it.has("targetSeconds") && !it.isNull("targetSeconds"))
                            it.getLong("targetSeconds")
                        else null,
                    )
                }
        val hilly = context?.optBoolean("hilly", false) == true
        val preferences = getSharedPreferences("live-coach", MODE_PRIVATE)
        val result =
            cues.update(
                metrics,
                update,
                execution?.steps() ?: emptyList(),
                race,
                hilly,
                if (update != null && !hilly) preferences.getLong("sustained", 45000L) else 45000L,
                preferences.getBoolean("miles", false),
                preferences.getBoolean("splits", true),
            )
        if (race != null) raceContext.value = result.raceContext()
        result.paceEvent()?.let { engine?.event(it, SystemClock.elapsedRealtime(), Instant.now()) }
        result.selected()?.let {
            cue.value = it.text()
            audio?.deliver(it)
        }
    }

    private fun shown(title: String) = Shown(title, status.value?.paused() == true)

    private fun publish() {
        status.value = engine?.status(SystemClock.elapsedRealtime())
        status.value?.let {
            troubleshooting?.status(it.gps(), SystemClock.elapsedRealtime())
            updateExecution(it)
        }
        // Live metrics are shown in the app, not the notification. Its content changes only with
        // pause and resume, so an unchanged notification is not rebuilt or posted again on every
        // tick and fix.
        val content =
            shown(if (status.value?.paused() == true) "Run paused" else "Recording your run")
        if (!notificationGate.shouldPost(content)) return
        if (
            Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        ) {
            getSystemService(NotificationManager::class.java)
                .notify(104, notification(content.title))
            notificationGate.posted(content)
            diagnosticEvent("notification_updated")
        }
    }

    private suspend fun flush() {
        try {
            recording?.flush()
        } catch (failure: Exception) {
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            troubleshooting?.flushFailed(SystemClock.elapsedRealtime())
            checkpointDiagnostics("staging_failed", force = true)
            throw failure
        }
        checkpointDiagnostics("recording")
    }

    private fun diagnosticEvent(kind: String) {
        troubleshooting?.event(kind, SystemClock.elapsedRealtime())
    }

    private fun checkpointDiagnostics(state: String, force: Boolean = false) {
        val active = recording ?: return
        val now = SystemClock.elapsedRealtime()
        if (!force && lastDiagnosticCheckpoint?.let { now - it < 15_000 } == true) return
        val snapshot =
            troubleshooting?.snapshot(
                now,
                active.engine.pointCount(),
                active.acknowledgedPointCount,
            ) ?: return
        lastDiagnosticCheckpoint = now
        diagnosticCheckpoints.trySend(
            DiagnosticCheckpoint(active.session.id, active.session.start, state, snapshot)
        )
    }

    private fun serviceIntent(type: String) =
        PendingIntent.getService(
            this,
            type.hashCode(),
            Intent(this, RunRecordingService::class.java).setAction(type),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun notification(text: String): Notification {
        val paused = status.value?.paused() == true
        return Notification.Builder(this, "recording")
            .setContentTitle(text)
            .setContentText(
                "Location is used only during this run. Open the app to review or finish."
            )
            .setSmallIcon(org.racedaydreams.R.drawable.ic_nav_progress)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setDeleteIntent(dismissedIntent)
            .addAction(
                Notification.Action.Builder(
                        null,
                        if (paused) "Resume" else "Pause",
                        if (paused) resumeIntent else pauseIntent,
                    )
                    .build()
            )
            .build()
    }

    override fun onDestroy() {
        if (recording != null) {
            diagnosticEvent("service_destroyed_with_unfinished_run")
            checkpointDiagnostics("interrupted", force = true)
        }
        diagnosticCheckpoints.close()
        dreamRoute.value = null
        audio?.close()
        workoutState.value = null
        raceContext.value = null
        running.value = false
        if (wake?.isHeld == true) wake?.release()
        heartRate.value = null
        status.value = null
        sensors.stop()
        location.stop()
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
