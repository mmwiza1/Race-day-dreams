package org.racedaydreams.storage

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.SqlValue.Integer
import org.racedaydreams.storage.SqlValue.Text

/** Writes native in-progress evidence to the existing recording archive tables. */
class SqliteRecordingStaging(private val database: SqlDatabase) {
    data class Session(
        val id: String,
        val state: String,
        val start: Instant,
        val startedMono: Long,
        val lastMono: Long,
    )

    fun unfinished(): Session? =
        database
            .query(
                "SELECT id,state,started_at,started_mono,last_mono FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE')"
            )
            .singleOrNull()
            ?.let {
                Session(
                    (it[0] as Text).value,
                    (it[1] as Text).value,
                    StoredInstant.parse((it[2] as Text).value),
                    (it[3] as Integer).value,
                    (it[4] as Integer).value,
                )
            }

    fun start(id: String, wall: Instant, mono: Long, execution: String?, origin: String?) {
        require(id.length in 1..256 && mono >= 0)
        database.transaction {
            database.execute(
                "INSERT INTO recording_sessions(id,state,started_at,started_mono,last_mono) VALUES(?,'ACTIVE',?,?,?)",
                listOf(Text(id), Text(wall.toString()), Integer(mono), Integer(mono)),
            )
            writeExecution(id, execution)
            origin?.let {
                StoredJson.objectValue(it)
                database.execute(
                    "INSERT OR REPLACE INTO release08(kind,id,payload) VALUES('origin',?,?)",
                    listOf(Text(id), Text(it)),
                )
            }
        }
    }

    fun append(
        id: String,
        points: List<RunMetricsEngine.Point>,
        events: List<RunMetricsEngine.Event>,
        fromPoint: Int,
        fromEvent: Int,
        last: Long,
        state: String,
        execution: String?,
    ) {
        require(fromPoint >= 0 && fromEvent >= 0 && state in unfinishedStates)
        database.transaction {
            val row =
                database
                    .query(
                        "SELECT started_mono,last_mono,state FROM recording_sessions WHERE id=?",
                        listOf(Text(id)),
                    )
                    .singleOrNull() ?: error("No staged recording")
            check((row[2] as Text).value in unfinishedStates) { "Recording already finished" }
            val first = (row[0] as Integer).value
            require(last >= (row[1] as Integer).value) { "Staged time cannot move backwards" }
            require(fromPoint.toLong() <= nextSequence("recording_points", id))
            require(fromEvent.toLong() <= nextSequence("recording_events", id))
            points.forEachIndexed { index, point ->
                require(point.fix().monotonicMillis() in first..last)
                writeRow(
                    "recording_points",
                    id,
                    fromPoint.toLong() + index,
                    RecordingPointJson.encode(point),
                )
            }
            events.forEachIndexed { index, event ->
                require(event.monotonicMillis() in first..last)
                val payload =
                    buildJsonObject {
                            put("type", requireNotNull(event.type()))
                            put("mono", event.monotonicMillis())
                            put("wall", requireNotNull(event.timestamp()).toString())
                            event.value()?.let { put("value", it) }
                        }
                        .toString()
                writeRow("recording_events", id, fromEvent.toLong() + index, payload)
            }
            database.execute(
                "UPDATE recording_sessions SET last_mono=?,state=? WHERE id=?",
                listOf(Integer(last), Text(state), Text(id)),
            )
            writeExecution(id, execution)
        }
    }

    fun markRecoverable() {
        database.execute(
            "UPDATE recording_sessions SET state='RECOVERABLE' WHERE state IN ('ACTIVE','PAUSED')"
        )
    }

    private fun nextSequence(table: String, id: String): Long {
        val max =
            database
                .query("SELECT MAX(sequence) FROM $table WHERE session_id=?", listOf(Text(id)))
                .single()
                .single()
        return if (max is Integer) max.value + 1 else 0
    }

    private fun writeRow(table: String, id: String, sequence: Long, payload: String) {
        require(sequence <= Int.MAX_VALUE)
        val key = listOf(Text(id), Integer(sequence))
        val existing =
            database
                .query("SELECT payload FROM $table WHERE session_id=? AND sequence=?", key)
                .singleOrNull()
                ?.single()
        if (existing != null) {
            check((existing as Text).value == payload) {
                "Conflicting staged evidence; original samples retained"
            }
        } else {
            database.execute(
                "INSERT INTO $table(session_id,sequence,payload) VALUES(?,?,?)",
                key + Text(payload),
            )
        }
    }

    private fun writeExecution(id: String, payload: String?) {
        if (payload == null) return
        StoredJson.objectValue(payload)
        database.execute(
            "INSERT OR REPLACE INTO coach_support(kind,id,payload) VALUES('execution',?,?)",
            listOf(Text(id), Text(payload)),
        )
    }

    private companion object {
        val unfinishedStates = setOf("ACTIVE", "PAUSED", "RECOVERABLE")
    }
}
