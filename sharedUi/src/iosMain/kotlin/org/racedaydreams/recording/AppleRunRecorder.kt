package org.racedaydreams.recording

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
                publish()
            },
            onUnavailable = { time, wall ->
                session?.engine?.missingFix(time, wall)
                publish()
            },
            onAuthorizationChanged = { granted -> mutablePermission.value = granted },
        )

    init {
        mutablePermission.value = location.canStart()
    }

    fun requestLocationPermission() = location.requestPermission()

    suspend fun start(executionContext: () -> String?, origin: String?) = command {
        check(session == null) { "A run is already recording." }
        check(location.canStart()) { "Location permission is required to record a run." }
        val id = nextId()
        val wall = AppleRecordingClock.now()
        val mono = AppleRecordingClock.monotonicMillis()
        val context = executionContext()
        withContext(io) { repository.start(id, wall, mono, context, origin) }
        val active =
            SharedRecordingSessionCoordinator(
                RecordingStageSession(id, "ACTIVE", wall, mono, mono),
                repository,
                io,
                { finalTime ?: AppleRecordingClock.monotonicMillis() },
                executionContext,
            )
        session = active
        mutableError.value = null
        location.start()
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
                    if (session === active) location.stop()
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

    suspend fun finish(): String = command {
        val active = checkNotNull(session)
        check(active.engine.pointCount() >= 2) { "Waiting for usable GPS" }
        if (!finishing) {
            finishing = true
            location.stop()
            ticker?.cancel()
            ticker = null
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
        finishing = false
        mutableStatus.value = null
        mutableError.value = null
        id
    }

    private fun publish() {
        mutableStatus.value = session?.engine?.status(AppleRecordingClock.monotonicMillis())
    }

    private suspend fun <T> command(action: suspend () -> T): T {
        check(NSThread.isMainThread)
        return commands.withLock { action() }
    }
}
