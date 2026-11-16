package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.*
import org.racedaydreams.domain.time.*

abstract class CompleteBackupRepositoryContract {
    abstract fun database(): SqlDatabase

    abstract fun directory(): String

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val mono = 9007199254740993L

    private fun <T> storage(block: (SqlDatabase) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "complete-backup-test")
            }
            return block(db)
        } finally {
            db.close()
        }
    }

    private fun seed(db: SqlDatabase): List<Point> {
        val end = Instants.ofEpochSecond(at.getEpochSecond() + 60, at.getNano().toLong())
        val actual =
            ActivityRecord(
                "activity",
                SourceIdentity("rdd", "run", null),
                at,
                end,
                null,
                null,
                100.0,
                Durations.ofSeconds(60),
                null,
                null,
                null,
                null,
                null,
                "Stored summary",
                emptyList(),
                emptyList(),
                null,
                null,
            )
        SqliteActivities(db) { at }.upsertManualActivity(actual)
        db.execute(
            "INSERT INTO recording_sessions VALUES('run','FINISHED',?,?,?,'activity')",
            listOf(
                SqlValue.Text(at.toString()),
                SqlValue.Integer(mono),
                SqlValue.Integer(mono + 60000),
            ),
        )
        val points =
            listOf(
                Point(
                    Fix(at, mono, 51.5, -.12, 4.0, null, null, null),
                    0.0,
                    0,
                    0,
                    false,
                    null,
                    null,
                ),
                Point(
                    Fix(end, mono + 60000, 51.5008, -.12, 4.0, null, null, null),
                    100.0,
                    60000,
                    0,
                    false,
                    null,
                    null,
                ),
            )
        points.forEachIndexed { i, p ->
            db.execute(
                "INSERT INTO recording_points VALUES('run',?,?)",
                listOf(SqlValue.Integer(i.toLong()), SqlValue.Text(RecordingPointJson.encode(p))),
            )
        }
        return points
    }

    @Test
    fun completeExportValidatesMeasuredNestedPayloadsAndPreservesExactRecordedCoordinatesAndClocks() =
        storage { db ->
            val expected = seed(db)
            val before = db.query("SELECT total_changes()")
            val file = SqliteCompleteBackup(db).export(directory(), at)
            try {
                val inspected = InspectedBackupPackage.inspect(file, directory())
                try {
                    assertEquals(13, inspected.application.version)
                    val summary = inspected.application.snapshot.core.activities.single()
                    assertEquals(100.0, summary.distanceMetres())
                    assertTrue(summary.samples().isEmpty())
                    val run = inspected.recordings.sessions.getValue("run")
                    assertEquals(mono, run.startedMono)
                    assertEquals(mono + 60000, run.lastMono)
                    assertEquals(2, run.points)
                    val zip =
                        BackupZipArchive.inspect(
                            inspected.recordingArchive(),
                            BackupZipArchive.Kind.RECORDINGS,
                        )
                    val text = StringBuilder()
                    zip.extract("recordings.jsonl") { text.append(it.decodeToString()) }
                    val points =
                        text
                            .lineSequence()
                            .filter { it.isNotBlank() }
                            .map { Json.parseToJsonElement(it).jsonObject }
                            .filter { it["kind"] == JsonPrimitive("point") }
                            .map { RecordingPointJson.decode(it.getValue("payload").jsonObject) }
                            .toList()
                    assertEquals(expected, points)
                } finally {
                    inspected.close()
                }
            } finally {
                file.close()
            }
            assertEquals(before, db.query("SELECT total_changes()"))
            assertEquals(
                2L,
                db.query("SELECT COUNT(*) FROM recording_points").single().single().sqlLong(),
            )
        }

    @Test
    fun activePausedAndRecoverableRunsBlockCompleteExportWithoutChangingRecoveryEvidence() =
        storage { db ->
            db.execute(
                "INSERT INTO recording_sessions VALUES('live','ACTIVE',?,0,1000,NULL)",
                listOf(SqlValue.Text(at.toString())),
            )
            for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) {
                db.execute("UPDATE recording_sessions SET state=?", listOf(SqlValue.Text(state)))
                val before = db.query("SELECT total_changes()")
                assertFails { SqliteCompleteBackup(db).export(directory(), at) }
                assertEquals(before, db.query("SELECT total_changes()"))
                assertEquals(
                    state,
                    db.query("SELECT state FROM recording_sessions").single().single().sqlText(),
                )
                // Standalone raw export remains usable and honest for interrupted recovery.
                val raw = SqliteRecordingArchive(db).export(directory())
                try {
                    assertEquals(
                        2,
                        BackupZipArchive.inspect(raw, BackupZipArchive.Kind.RECORDINGS).entries.size,
                    )
                } finally {
                    raw.close()
                }
            }
        }

    @Test
    fun completeSourceReadsShareOneOuterTransactionAndCannotCommitRejectedChanges() =
        storage { db ->
            seed(db)
            var depth = 0
            val readonly =
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
                        assertTrue(depth > 0)
                        return db.query(sql, arguments)
                    }

                    override fun execute(sql: String, arguments: List<SqlValue>) =
                        error("Export must not write")
                }
            assertFails {
                db.transaction {
                    db.execute("INSERT INTO settings VALUES('pending','retained only in snapshot')")
                    val file = SqliteCompleteBackup(readonly).export(directory(), at)
                    try {
                        val inspected = InspectedBackupPackage.inspect(file, directory())
                        try {
                            assertEquals(
                                "retained only in snapshot",
                                inspected.application.snapshot.core.settings["pending"],
                            )
                        } finally {
                            inspected.close()
                        }
                    } finally {
                        file.close()
                    }
                    error("reject caller transaction")
                }
            }
            assertTrue(db.query("SELECT * FROM settings WHERE setting_key='pending'").isEmpty())
        }

    @Test
    fun failedPrivateStagingCannotChangeSourceDataOrPreventLaterValidExport() = storage { db ->
        seed(db)
        val before = db.query("SELECT total_changes()")
        assertFails { SqliteCompleteBackup(db).export(directory() + "/missing/parent", at) }
        assertEquals(before, db.query("SELECT total_changes()"))
        val file = SqliteCompleteBackup(db).export(directory(), at)
        try {
            assertEquals(
                3,
                BackupZipArchive.inspect(file, BackupZipArchive.Kind.COMPLETE).entries.size,
            )
        } finally {
            file.close()
        }
        assertEquals(before, db.query("SELECT total_changes()"))
    }
}
