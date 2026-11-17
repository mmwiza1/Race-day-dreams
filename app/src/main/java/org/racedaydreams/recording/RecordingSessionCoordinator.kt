package org.racedaydreams.recording

import kotlinx.coroutines.CoroutineDispatcher
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
            batch.sessionId,
            batch.points,
            batch.events,
            batch.fromPoint,
            batch.fromEvent,
            batch.lastMonotonic,
            batch.state,
            batch.executionContext,
        )
    }

    override fun unfinished() = store.unfinished()

    override fun finish(session: RecordingStore.Session, movingMillis: Long?, recovered: Boolean) =
        store.finish(session, movingMillis, recovered = recovered)
}

/**
 * Owns one active metrics engine and its durable staging cursor. The service calls engine mutations
 * and flush on Main. Only a captured batch crosses to serial IO; neither UI state nor a live
 * execution object is read by the repository worker.
 *
 * A failed append leaves both cursors untouched, allowing the existing idempotent repository retry.
 * New observations arriving during IO belong to the next batch. Interrupted sessions are still
 * recovered explicitly by RecordingStore; constructing this coordinator never resumes a retained
 * recording or starts a sensor.
 */
internal class RecordingSessionCoordinator(
    val session: RecordingStore.Session,
    repository: RecordingRepository,
    io: CoroutineDispatcher,
    monotonicMillis: () -> Long,
    executionContext: () -> JSONObject? = { null },
    executionEvents: () -> List<WorkoutExecutionEngine.Event> = { emptyList() },
) {
    private val shared =
        SharedRecordingSessionCoordinator(
            session.shared(),
            object : RecordingStageRepository {
                override fun append(batch: RecordingStageBatch) =
                    repository.append(
                        RecordingBatch(
                            batch.sessionId,
                            batch.points,
                            batch.events,
                            batch.fromPoint,
                            batch.fromEvent,
                            batch.lastMonotonic,
                            batch.state,
                            batch.executionContextPayload?.let(::JSONObject),
                        )
                    )

                override fun unfinished() = repository.unfinished()?.shared()

                override fun finish(
                    session: RecordingStageSession,
                    movingMillis: Long?,
                    recovered: Boolean,
                ) =
                    repository.finish(
                        RecordingStore.Session(
                            session.id,
                            session.state,
                            session.start,
                            session.mono,
                            session.last,
                        ),
                        movingMillis,
                        recovered,
                    )
            },
            io,
            monotonicMillis,
            {
                // Original Android codec and caller-thread capture, byte-for-byte existing shape.
                executionContext()?.let { retained ->
                    val rows = JSONArray()
                    executionEvents().forEach {
                        rows.put(
                            JSONObject()
                                .put("type", it.type())
                                .put("step", it.step())
                                .put("metres", it.metres())
                                .put("active", it.activeMillis())
                        )
                    }
                    JSONObject(retained.toString()).put("events", rows).toString()
                }
            },
        )
    val engine
        get() = shared.engine

    suspend fun flush() = shared.flush()

    suspend fun finish(): String = shared.finish()
}

private fun RecordingStore.Session.shared() = RecordingStageSession(id, state, start, mono, last)
