package org.racedaydreams.recording

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.time.Instant

/** Existing staging header, independent of the Android SQLite owner. */
data class RecordingStageSession(
    val id: String,
    val state: String,
    val start: Instant,
    val mono: Long,
    val last: Long,
)

/** Frozen observations plus the existing encoded execution context captured on the owner thread. */
data class RecordingStageBatch(
    val sessionId: String,
    val points: List<RunMetricsEngine.Point>,
    val events: List<RunMetricsEngine.Event>,
    val fromPoint: Int,
    val fromEvent: Int,
    val lastMonotonic: Long,
    val state: String,
    val executionContextPayload: String?,
)

/** One existing pool: append is atomic and replayable; finish retains its original transaction. */
interface RecordingStageRepository {
    fun append(batch: RecordingStageBatch)

    fun unfinished(): RecordingStageSession?

    fun finish(session: RecordingStageSession, movingMillis: Long?, recovered: Boolean): String
}

/**
 * Original active engine and staging cursor protocol. The real service/host owns sensors, Main
 * mutations and its monotonic clock. Only frozen captured values cross to serial IO. Construction
 * never reads storage, resumes an interrupted session or starts a sensor. The required context
 * snapshot callback uses the platform's actual existing codec, not invented execution evidence.
 */
class SharedRecordingSessionCoordinator(
    val session: RecordingStageSession,
    private val repository: RecordingStageRepository,
    private val io: CoroutineDispatcher,
    private val monotonicMillis: () -> Long,
    private val executionContextSnapshot: () -> String?,
) {
    val engine = RunMetricsEngine(session.mono, session.start)
    private val flushLock = Mutex()
    private var pointIndex = 0
    private var eventIndex = 0
    val acknowledgedPointCount: Int
        get() = pointIndex

    suspend fun flush() =
        flushLock.withLock {
            val fromPoint = pointIndex
            val fromEvent = eventIndex
            val points = engine.pointsFrom(fromPoint)
            val events = engine.eventsFrom(fromEvent)
            val now = monotonicMillis()
            val state = if (engine.status(now).paused()) "PAUSED" else "ACTIVE"
            val context = executionContextSnapshot()
            val batch =
                RecordingStageBatch(
                    session.id,
                    points,
                    events,
                    fromPoint,
                    fromEvent,
                    now,
                    state,
                    context,
                )
            withContext(io) { repository.append(batch) }
            // A failed or cancelled acknowledgement never advances either durable cursor.
            pointIndex = fromPoint + points.size
            eventIndex = fromEvent + events.size
        }

    /** What a Finish request may do now. */
    enum class FinishRequest {
        /** Enough GPS evidence exists: stop sensors and save. */
        SAVE,
        /** No GPS lock yet: keep recording and explain; nothing is stopped or saved. */
        NEEDS_GPS,
        /** A second Finish shortly after [NEEDS_GPS]: the runner chose to drop the empty run. */
        DISCARD_EMPTY,
    }

    private var emptyFinishAt = -1L

    /** A run can be saved only with the points storage requires (audit A-B1). */
    fun canFinish(): Boolean = engine.pointCount() >= MIN_FINISH_POINTS

    /**
     * Decides a Finish request before any state changes, so a premature Finish never stops the
     * service into an unsaved run that recovery cannot finish either (audit A-B1).
     */
    fun requestFinish(now: Long): FinishRequest {
        if (canFinish()) {
            emptyFinishAt = -1
            return FinishRequest.SAVE
        }
        val confirmed = emptyFinishAt >= 0 && now - emptyFinishAt in 0..DISCARD_CONFIRM_MILLIS
        emptyFinishAt = if (confirmed) -1 else now
        return if (confirmed) FinishRequest.DISCARD_EMPTY else FinishRequest.NEEDS_GPS
    }

    /** Caller stops sensors/ticker and durably flushes the final observed batch first. */
    suspend fun finish(): String {
        check(canFinish()) { NEEDS_GPS_MESSAGE }
        val retained = withContext(io) { repository.unfinished() } ?: error("No staged recording")
        val movingMillis = engine.recordedMovingMillis(monotonicMillis())
        return withContext(io) { repository.finish(retained, movingMillis, false) }
    }

    companion object {
        /** Matches the storage rule: a saved run needs at least two usable GPS points. */
        const val MIN_FINISH_POINTS = 2
        const val DISCARD_CONFIRM_MILLIS = 10_000L
        const val NEEDS_GPS_MESSAGE =
            "Waiting for a GPS lock: at least two GPS positions are needed to save this run. " +
                "Recording continues. Tap Finish again within 10 seconds to discard it instead."
        const val DISCARDED_EMPTY_MESSAGE =
            "Run discarded. Without a GPS lock there was no route to save."
    }
}
