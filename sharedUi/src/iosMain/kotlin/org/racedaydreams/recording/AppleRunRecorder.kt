package org.racedaydreams.recording

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.execution.RecordingCueCoordinator
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.storage.SqliteRecordingStaging
import platform.Foundation.NSThread

/** One owner per application. Call commands from the application's main scope. */
class AppleRunRecorder(
    private val repository: SqliteRecordingStageRepository,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val nextId: () -> String,
) {
    private val commands = Mutex()
    private var cues = RecordingCueCoordinator()
    private var miles = false
    private var monitor: AppleHeartRateSource? = null
    private var scanTimeout: Job? = null
    private var lastHeartRateTime = 0L
    private val mutableMonitors = MutableStateFlow<Map<String, String>>(emptyMap())
    val monitors = mutableMonitors.asStateFlow()
    private val mutableHeartRate = MutableStateFlow<Double?>(null)
    val heartRate = mutableHeartRate.asStateFlow()

    fun scanHeartRateMonitors() {
        check(NSThread.isMainThread && session == null)
        val source =
            monitor
                ?: AppleHeartRateSource { bpm ->
                        val time = AppleRecordingClock.monotonicMillis()
                        session?.engine?.heartRate(bpm, time)
                        lastHeartRateTime = time
                        mutableHeartRate.value = bpm
                    }
                    .also { monitor = it }
        if (scanTimeout?.isActive == true) return
        source.scan()
        scanTimeout =
            scope.launch(Dispatchers.Main.immediate) {
                kotlinx.coroutines.withTimeoutOrNull(15_000) {
                    source.devices.collect { mutableMonitors.value = it }
                }
                source.stopScan()
            }
    }

    fun stopHeartRateScan() {
        scanTimeout?.cancel()
        scanTimeout = null
        monitor?.stopScan()
    }

    private var speech: AppleCoachSpeech? = null
    private val mutableCue = MutableStateFlow<String?>(null)
    val cue = mutableCue.asStateFlow()
    private val mutableRaceContext = MutableStateFlow<String?>(null)
    val raceContext = mutableRaceContext.asStateFlow()
    private var execution: PlannedRecordingExecution? = null
    private val mutableWorkout = MutableStateFlow<WorkoutExecutionEngine.State?>(null)
    val workout = mutableWorkout.asStateFlow()
    private var session: SharedRecordingSessionCoordinator? = null
    private var ticker: Job? = null
    private var finishing = false
    private var pendingFinish: RecordingStageSession? = null
    private var finalMovingMillis = 0L
    private var finalTime: Long? = null
    private val mutableStatus = MutableStateFlow<RunMetricsEngine.Status?>(null)
    private val mutableError = MutableStateFlow<String?>(null)
    private val mutablePermission = MutableStateFlow(false)
    val status = mutableStatus.asStateFlow()
    val error = mutableError.asStateFlow()
    val permission = mutablePermission.asStateFlow()
    private val mutableRecovery = MutableStateFlow<SqliteRecordingStaging.Recovery?>(null)
    val recovery = mutableRecovery.asStateFlow()

    fun routePoints(): List<RunMetricsEngine.Point> {
        check(NSThread.isMainThread)
        return session?.engine?.points().orEmpty()
    }

    private val location =
        AppleRecordingLocation(
            onFix = { fix, received, wall ->
                val engine = session?.engine
                if (engine != null && engine.qualifyDelivery(fix, received, wall))
                    engine.accept(fix)
                // Multiple fixes in a Core Location batch share one receipt time.
                publish(received)
            },
            onUnavailable = { time, wall ->
                session?.engine?.missingFix(time, wall)
                publish(time)
            },
            onAuthorizationChanged = { granted -> mutablePermission.value = granted },
        )

    init {
        mutablePermission.value = location.canStart()
    }

    fun requestLocationPermission() = location.requestPermission()

    suspend fun start(
        planned: PlannedRecordingExecution? = null,
        imperial: Boolean = false,
        spokenCues: Boolean = false,
        monitorId: String? = null,
    ) = command {
        check(session == null) { "A run is already recording." }
        check(location.canStart()) { "Location permission is required to record a run." }
        val id = nextId()
        val wall = AppleRecordingClock.now()
        val mono = AppleRecordingClock.monotonicMillis()
        val context = planned?.snapshot()
        withContext(io) { repository.start(id, wall, mono, context, null) }
        val active =
            SharedRecordingSessionCoordinator(
                RecordingStageSession(id, "ACTIVE", wall, mono, mono),
                repository,
                io,
                { finalTime ?: AppleRecordingClock.monotonicMillis() },
                { execution?.snapshot() },
            )
        cues = RecordingCueCoordinator()
        miles = imperial
        speech = if (spokenCues) AppleCoachSpeech() else null
        execution = planned
        session = active
        mutableError.value = null
        location.start()
        stopHeartRateScan()
        if (monitorId != null) monitor?.connect(monitorId)
        publish()
        ticker =
            scope.launch(Dispatchers.Main.immediate) {
                try {
                    while (isActive) {
                        delay(1_000)
                        commands.withLock {
                            if (session !== active || finishing) return@withLock
                            val time = AppleRecordingClock.monotonicMillis()
                            active.engine.missingFix(time, AppleRecordingClock.now())
                            publish()
                            try {
                                active.flush()
                                mutableError.value = null
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                mutableError.value =
                                    "Could not save the latest recording details. Retrying."
                            }
                        }
                    }
                } finally {
                    if (session === active) {
                        location.stop()
                        speech?.stop()
                        monitor?.stop()
                    }
                }
            }
    }

    /** Call during host startup, before presenting the first recording action. */
    suspend fun restoreInterrupted(): SqliteRecordingStaging.Recovery? = command {
        check(session == null)
        withContext(io) {
                repository.markRecoverable()
                repository.recovery()
            }
            .also { mutableRecovery.value = it }
    }

    suspend fun saveInterrupted(expected: SqliteRecordingStaging.Session): String = command {
        check(session == null)
        withContext(io) { repository.saveRecovery(expected) }.also { mutableRecovery.value = null }
    }

    suspend fun discardInterrupted(expected: SqliteRecordingStaging.Session): Boolean = command {
        check(session == null)
        withContext(io) { repository.discardRecovery(expected) to repository.recovery() }
            .let { (discarded, remaining) ->
                mutableRecovery.value = remaining
                discarded
            }
    }

    suspend fun pause() = command {
        val active = checkNotNull(session)
        check(!finishing)
        speech?.stop()
        active.engine.pause(AppleRecordingClock.monotonicMillis(), AppleRecordingClock.now())
        publish()
        active.flush()
    }

    suspend fun resume() = command {
        val active = checkNotNull(session)
        check(!finishing)
        active.engine.resume(AppleRecordingClock.monotonicMillis(), AppleRecordingClock.now())
        publish()
        active.flush()
    }

    suspend fun changeWorkoutStep(skip: Boolean) = command {
        val active = checkNotNull(session)
        val planned = checkNotNull(execution)
        check(!finishing)
        if (skip) planned.coordinator.skip() else planned.coordinator.completeOpen()
        publish()
        active.flush()
    }

    suspend fun finish(): String = command {
        val active = checkNotNull(session)
        check(active.engine.pointCount() >= 2) { "Waiting for usable GPS" }
        if (!finishing) {
            finishing = true
            speech?.stop()
            location.stop()
            monitor?.stop()
            ticker?.cancel()
            ticker = null
            execution?.coordinator?.finishEarly()
            publish()
            val time = AppleRecordingClock.monotonicMillis()
            finalTime = time
            active.engine.event("FINISH", time, AppleRecordingClock.now())
            val finalStatus = active.engine.status(time)
            finalMovingMillis = finalStatus.movingMillis()
            mutableStatus.value = finalStatus
        }
        val retained =
            pendingFinish
                ?: run {
                    active.flush()
                    withContext(io) { checkNotNull(repository.unfinished()) }
                        .also {
                            check(it.id == active.session.id)
                            pendingFinish = it
                        }
                }
        val id = withContext(io) { repository.finish(retained, finalMovingMillis, false) }
        pendingFinish = null
        finalTime = null
        session = null
        execution = null
        speech = null
        mutableWorkout.value = null
        mutableCue.value = null
        mutableRaceContext.value = null
        finishing = false
        mutableStatus.value = null
        mutableError.value = null
        id
    }

    private fun publish(time: Long = AppleRecordingClock.monotonicMillis()) {
        val engine = session?.engine ?: return
        if (time - lastHeartRateTime > 10_000) mutableHeartRate.value = null
        val status = engine.status(time)
        mutableStatus.value = status
        val planned = execution
        val workoutUpdate = planned?.coordinator?.update(status)
        workoutUpdate?.let { update ->
            mutableWorkout.value = update.state()
            update.newEvents().forEach { event ->
                engine.sensorEvent(
                    event.type(),
                    event.step().toDouble(),
                    time,
                    AppleRecordingClock.now(),
                )
            }
        }
        if (finishing) return
        val result =
            cues.update(
                status,
                workoutUpdate,
                planned?.coordinator?.steps().orEmpty(),
                planned?.raceTarget,
                hilly = false,
                sustainedMillis = 45_000L,
                miles = miles,
                announceSplits = true,
            )
        mutableRaceContext.value = result.raceContext()
        result.selected()?.let {
            mutableCue.value = it.text()
            if (!status.paused()) speech?.deliver(it.text())
        }
        result.paceEvent()?.let { engine.event(it, time, AppleRecordingClock.now()) }
    }

    private suspend fun <T> command(action: suspend () -> T): T {
        check(NSThread.isMainThread)
        return commands.withLock { action() }
    }
}
