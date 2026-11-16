package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.*
import org.racedaydreams.domain.storage.*
import org.racedaydreams.domain.time.*

abstract class RecordingArchiveRepositoryContract {
    abstract fun database(): SqlDatabase

    abstract fun directory(): String

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val mono = 9007199254740993L

    private fun <T> storage(block: (SqlDatabase) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "recording-archive-test")
            }
            return block(db)
        } finally {
            db.close()
        }
    }

    private fun activity(id: String) =
        ActivityRecord(
            "activity-$id",
            SourceIdentity("rdd", id, "local"),
            at,
            Instants.ofEpochSecond(at.getEpochSecond() + 60, at.getNano().toLong()),
            null,
            null,
            600.0,
            Durations.ofSeconds(60),
            null,
            null,
            null,
            null,
            null,
            "Retained source summary",
            emptyList(),
            emptyList(),
            null,
            null,
        )

    private fun session(db: SqlDatabase, id: String, state: String = "FINISHED") {
        if (state == "FINISHED") SqliteActivities(db) { at }.upsertManualActivity(activity(id))
        db.execute(
            "INSERT INTO recording_sessions VALUES(?,?,?,?,?,?)",
            listOf(
                SqlValue.Text(id),
                SqlValue.Text(state),
                SqlValue.Text(at.toString()),
                SqlValue.Integer(mono),
                SqlValue.Integer(mono + 60000),
                if (state == "FINISHED") SqlValue.Text("activity-$id") else SqlValue.Null,
            ),
        )
    }

    private fun point(sequence: Int): String =
        RecordingPointJson.encode(
            Point(
                Fix(
                    at,
                    mono + (sequence + 1) * 100,
                    51.5 + sequence * .000001,
                    -.12,
                    4.0,
                    null,
                    null,
                    null,
                ),
                (sequence + 1) * .5,
                (sequence + 1) * 100L,
                0,
                false,
                null,
                null,
            )
        )

    private fun inspect(file: BackupStagingFile): RecordingArchiveRows.Snapshot {
        val zip = BackupZipArchive.inspect(file, BackupZipArchive.Kind.RECORDINGS)
        val bytes = mutableListOf<Byte>()
        zip.extract("manifest.json") { bytes.addAll(it.toList()) }
        val reader = RecordingArchiveReader(RecordingArchiveManifest.decode(bytes.toByteArray()))
        try {
            zip.extract("recordings.jsonl", reader::accept)
            return reader.finish()
        } finally {
            reader.close()
        }
    }

    private fun export(db: SqlDatabase): RecordingArchiveRows.Snapshot {
        val file = SqliteRecordingArchive(db).export(directory())
        try {
            return inspect(file)
        } finally {
            file.close()
        }
    }

    @Test
    fun realSqlSnapshotPreservesCrossPageSessionsDenseEvidenceOrderAndUnknownPayloadFields() =
        storage { db ->
            db.transaction {
                for (i in 0..513) session(db, "run-${i.toString().padStart(3, '0')}")
                for (i in 0..513) {
                    val payload = Json.parseToJsonElement(point(i)).jsonObject
                    val retained =
                        JsonObject(payload + ("retainedNote" to JsonPrimitive("x".repeat(3000))))
                    db.execute(
                        "INSERT INTO recording_points VALUES('run-000',?,?)",
                        listOf(SqlValue.Integer(i.toLong()), SqlValue.Text(retained.toString())),
                    )
                    val event = buildJsonObject {
                        put("type", "GPS_RECOVERED")
                        put("mono", mono + i * 100)
                        put("wall", at.toString())
                        put("retainedNote", "y".repeat(3000))
                    }
                    db.execute(
                        "INSERT INTO recording_events VALUES('run-000',?,?)",
                        listOf(SqlValue.Integer(i.toLong()), SqlValue.Text(event.toString())),
                    )
                }
            }
            val before = db.query("SELECT total_changes()")
            val file = SqliteRecordingArchive(db).export(directory())
            try {
                val snapshot = inspect(file)
                assertEquals(514, snapshot.sessions.size)
                val run = snapshot.sessions.getValue("run-000")
                assertEquals(514, run.points)
                assertEquals(514L, run.events)
                assertEquals(mono, run.startedMono)
                assertEquals(mono + 60000, run.lastMono)
                assertEquals(
                    (0..513).map { "run-${it.toString().padStart(3, '0')}" },
                    snapshot.sessions.keys.toList(),
                )
                // Check actual expanded bytes too: unknown stored fields must not disappear in
                // export.
                val zip = BackupZipArchive.inspect(file, BackupZipArchive.Kind.RECORDINGS)
                val chunks = mutableListOf<Byte>()
                zip.extract("recordings.jsonl") { chunks.addAll(it.toList()) }
                val lines =
                    chunks.toByteArray().decodeToString().lineSequence().filter { it.isNotEmpty() }
                var points = 0
                var events = 0
                for (line in lines) {
                    val row = Json.parseToJsonElement(line).jsonObject
                    if (row["kind"] == JsonPrimitive("point")) {
                        assertEquals(
                            "x".repeat(3000),
                            row.getValue("payload")
                                .jsonObject["retainedNote"]
                                ?.jsonPrimitive
                                ?.content,
                        )
                        points++
                    }
                    if (row["kind"] == JsonPrimitive("event")) events++
                }
                assertEquals(514, points)
                assertEquals(514, events)
            } finally {
                file.close()
            }
            assertEquals(before, db.query("SELECT total_changes()"))
            assertEquals(
                514L,
                db.query("SELECT COUNT(*) FROM activities").single().single().sqlLong(),
            )
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun unfinishedStatesAndExecutionContextRemainHonestAndExportNeverWritesToTheDatabase() =
        storage { db ->
            session(db, "live", "ACTIVE")
            val context =
                """{"version":1,"mode":"quick","steps":[{"label":"Easy running","kind":"RUN","millis":60000}]}"""
            db.execute(
                "INSERT INTO coach_support VALUES('execution','live',?)",
                listOf(SqlValue.Text(context)),
            )
            var depth = 0
            val guarded =
                object : SqlDatabase by db {
                    override fun <T> transaction(block: () -> T): T =
                        db.transaction {
                            depth++
                            try {
                                block()
                            } finally {
                                depth--
                            }
                        }

                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        assertTrue(depth > 0, "All source reads must share one snapshot")
                        return db.query(sql, arguments)
                    }

                    override fun execute(sql: String, arguments: List<SqlValue>) =
                        error("Export must not write")
                }
            for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) {
                db.execute(
                    "UPDATE recording_sessions SET state=? WHERE id='live'",
                    listOf(SqlValue.Text(state)),
                )
                val before = db.query("SELECT total_changes()")
                val run = export(guarded).sessions.getValue("live")
                assertEquals(state, run.state)
                assertTrue(run.hasContext)
                assertFalse(run.hasActivity)
                assertEquals(0, run.points)
                assertEquals(0L, run.events)
                assertEquals(before, db.query("SELECT total_changes()"))
            }
        }

    @Test
    fun removedDetailProvenanceAndSummarySurviveWithoutFabricatedRoutesOrConsent() = storage { db ->
        session(db, "pruned")
        val actual = activity("pruned")
        val proof =
            RemovedRecordingDetail(
                "pruned",
                actual.id(),
                "rdd",
                "pruned",
                actual.end(),
                actual.end(),
                DetailRetention.Policy.LAST_24_MONTHS,
                at,
                mono,
                mono + 60000,
                2,
                1,
                "ab".repeat(32),
                "cd".repeat(32),
            )
        db.execute(
            "INSERT INTO recording_detail_removals VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            listOf(
                SqlValue.Text(proof.sessionId()),
                SqlValue.Text(proof.activityId()),
                SqlValue.Text(proof.provider()),
                SqlValue.Text(proof.recordId()),
                SqlValue.Text(proof.endedAt().toString()),
                SqlValue.Text(proof.removedAt().toString()),
                SqlValue.Text(proof.policy().name),
                SqlValue.Text(at.toString()),
                SqlValue.Integer(mono),
                SqlValue.Integer(mono + 60000),
                SqlValue.Integer(2),
                SqlValue.Integer(1),
                SqlValue.Text(proof.pointsHash()),
                SqlValue.Text(proof.eventsHash()),
            ),
        )
        val preferences = SqliteRetentionPreferences(db).load()
        val before = db.query("SELECT payload FROM activities")
        val run = export(db).sessions.getValue("pruned")
        assertEquals(proof, run.removal)
        assertTrue(run.hasActivity)
        assertEquals(0, run.points)
        assertEquals(0L, run.events)
        assertEquals(before, db.query("SELECT payload FROM activities"))
        assertEquals(preferences, SqliteRetentionPreferences(db).load())
    }

    @Test
    fun invalidSequencesMalformedAndOversizedPayloadsRejectWithoutChangingSourceOrReturningPartialEvidence() =
        storage { db ->
            session(db, "run")
            db.execute(
                "INSERT INTO recording_points VALUES('run',0,?)",
                listOf(SqlValue.Text(point(0))),
            )
            assertEquals(1, export(db).sessions.getValue("run").points)
            for (invalid in listOf("{", "[]", "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[0")) {
                db.execute("UPDATE recording_points SET payload=?", listOf(SqlValue.Text(invalid)))
                val before = db.query("SELECT total_changes()")
                assertFails { export(db) }
                assertEquals(before, db.query("SELECT total_changes()"))
                assertEquals(
                    invalid,
                    db.query("SELECT payload FROM recording_points").single().single().sqlText(),
                )
            }
            db.execute(
                "UPDATE recording_points SET payload=zeroblob(?)",
                listOf(SqlValue.Integer(RecordingArchiveRows.MAX_LINE * 4L + 1)),
            )
            val before = db.query("SELECT total_changes()")
            assertFails { export(db) }
            assertEquals(before, db.query("SELECT total_changes()"))
            db.execute(
                "UPDATE recording_points SET sequence=1,payload=?",
                listOf(SqlValue.Text(point(0))),
            )
            assertFails { export(db) }
            db.execute("UPDATE recording_points SET sequence=0")
            assertEquals(1, export(db).sessions.getValue("run").points)
        }

    @Test
    fun emptyArchivesValidateAndNestedExportDoesNotCommitTheCallersUncommittedChanges() =
        storage { db ->
            assertTrue(export(db).sessions.isEmpty())
            assertFails {
                db.transaction {
                    session(db, "uncommitted", "RECOVERABLE")
                    assertEquals("RECOVERABLE", export(db).sessions.getValue("uncommitted").state)
                    error("rollback caller")
                }
            }
            assertTrue(db.query("SELECT * FROM recording_sessions").isEmpty())
            assertTrue(export(db).sessions.isEmpty())
        }
}
