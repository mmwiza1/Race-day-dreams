package org.racedaydreams.recording

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.recording.RunMetricsEngine

/** One immutable captured batch, including its matching execution evidence. */
internal data class RecordingBatch(
    val sessionId: String,
    val points: List<RunMetricsEngine.Point>,
    val events: List<RunMetricsEngine.Event>,
    val fromPoint: Int,
    val fromEvent: Int,
    val lastMonotonic: Long,
    val state: String,
    val executionContext: JSONObject?,
)

/** Shared-database boundary; appending one batch is an atomic operation. */
internal interface RecordingRepository {
    fun append(batch: RecordingBatch)

    fun unfinished(): RecordingStore.Session?

    fun finish(session: RecordingStore.Session, movingMillis: Long?, recovered: Boolean): String
}

/** Retains the existing SQLite helper and transaction; no second database or format. */
internal class SqliteRecordingRepository(private val store: RecordingStore) : RecordingRepository {
    override fun append(batch: RecordingBatch) {
        store.appendBatch(
            batch.sessionId, batch.points, batch.events, batch.fromPoint, batch.fromEvent,
            batch.lastMonotonic, batch.state, batch.executionContext,
        )
    }

    override fun unfinished() = store.unfinished()

    override fun finish(session: RecordingStore.Session, movingMillis: Long?, recovered: Boolean) =
        store.finish(session, movingMillis, recovered = recovered)
}

/**
 * Owns one active metrics engine and its durable staging cursor. The service calls
 * engine mutations and flush on Main. Only a captured batch crosses to serial IO;
 * neither UI state nor a live execution object is read by the repository worker.
 *
 * A failed append leaves both cursors untouched, allowing the existing idempotent
 * repository retry. New observations arriving during IO belong to the next batch.
 * Interrupted sessions are still recovered explicitly by RecordingStore; constructing
 * this coordinator never resumes a retained recording or starts a sensor.
 */
internal class RecordingSessionCoordinator(
    val session: RecordingStore.Session,
    private val repository: RecordingRepository,
    private val io: CoroutineDispatcher,
    private val monotonicMillis: () -> Long,
    private val executionContext: () -> JSONObject? = { null },
    private val executionEvents: () -> List<WorkoutExecutionEngine.Event> = { emptyList() },
) {
    val engine = RunMetricsEngine(session.mono, session.start)
    private val flushLock = Mutex()
    private var pointIndex = 0
    private var eventIndex = 0

    suspend fun flush() = flushLock.withLock {
        val fromPoint = pointIndex
        val fromEvent = eventIndex
        val points = engine.pointsFrom(fromPoint)
        val events = engine.eventsFrom(fromEvent)
        val now = monotonicMillis()
        val state = if (engine.status(now).paused()) "PAUSED" else "ACTIVE"
        val context = executionContext()?.let { retained ->
            val rows = JSONArray()
            executionEvents().forEach {
                rows.put(
                    JSONObject().put("type", it.type()).put("step", it.step())
                        .put("metres", it.metres()).put("active", it.activeMillis())
                )
            }
            JSONObject(retained.toString()).put("events", rows)
        }
        val batch = RecordingBatch(
            session.id, points, events, fromPoint, fromEvent, now, state, context,
        )
        withContext(io) { repository.append(batch) }
        pointIndex = fromPoint + points.size
        eventIndex = fromEvent + events.size
    }

    /** Called after sensors/ticker stop and the final observed batch is durable. */
    suspend fun finish(): String {
        val retained = withContext(io) { repository.unfinished() } ?: error("No staged recording")
        val movingMillis = engine.status(monotonicMillis()).movingMillis()
        return withContext(io) {
            repository.finish(retained, movingMillis, false)
        }
    }
}
