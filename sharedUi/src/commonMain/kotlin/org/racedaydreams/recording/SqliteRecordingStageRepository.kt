package org.racedaydreams.recording

import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.SqlDatabase
import org.racedaydreams.storage.SqliteRecordingFinalisation
import org.racedaydreams.storage.SqliteRecordingStaging

/** Uses the application's existing SQLite connection for observations and canonical results. */
class SqliteRecordingStageRepository(
    database: SqlDatabase,
    now: () -> Instant,
    nextId: () -> String,
) : RecordingStageRepository {
    private val staging = SqliteRecordingStaging(database)
    private val finalisation = SqliteRecordingFinalisation(database, now, nextId)

    fun start(id: String, wall: Instant, mono: Long, execution: String?, origin: String?) {
        staging.start(id, wall, mono, execution, origin)
    }

    fun markRecoverable() = staging.markRecoverable()

    fun recovery(): SqliteRecordingStaging.Recovery? = staging.recovery()

    fun discardRecovery(expected: SqliteRecordingStaging.Session): Boolean =
        staging.discardRecovery(expected)

    fun saveRecovery(expected: SqliteRecordingStaging.Session): String {
        require(expected.state == "RECOVERABLE")
        return finalisation.finish(expected, movingMillis = null, recovered = true)
    }

    override fun unfinished(): RecordingStageSession? =
        staging.unfinished()?.let {
            RecordingStageSession(it.id, it.state, it.start, it.startedMono, it.lastMono)
        }

    override fun append(batch: RecordingStageBatch) {
        staging.append(
            batch.sessionId,
            batch.points,
            batch.events,
            batch.fromPoint,
            batch.fromEvent,
            batch.lastMonotonic,
            batch.state,
            batch.executionContextPayload,
        )
    }

    override fun finish(
        session: RecordingStageSession,
        movingMillis: Long?,
        recovered: Boolean,
    ): String =
        finalisation.finish(
            SqliteRecordingStaging.Session(
                session.id,
                session.state,
                session.start,
                session.mono,
                session.last,
            ),
            movingMillis,
            recovered,
        )
}
