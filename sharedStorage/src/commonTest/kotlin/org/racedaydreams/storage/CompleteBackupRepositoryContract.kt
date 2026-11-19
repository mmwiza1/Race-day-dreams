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

    private var epochs = 0

    private fun replacement(db: SqlDatabase) =
        SqliteCompleteBackupRestore(db) { "complete-restored-${++epochs}" }

    private fun epoch(db: SqlDatabase) =
        db.query("SELECT epoch FROM detail_retention_state").single().single().sqlText()

    private fun state(db: SqlDatabase) =
        listOf(
                "activities",
                "recording_sessions",
                "recording_points",
                "recording_events",
                "settings",
                "coach_support",
                "recording_detail_removals",
                "best_efforts",
                "preserved_best_efforts",
            )
            .associateWith { db.query("SELECT * FROM $it") }

    private fun target(block: (SqlDatabase) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "complete-target")
            }
            block(db)
        } finally {
            db.close()
        }
    }

    private fun complete(db: SqlDatabase): InspectedBackupPackage {
        val file = SqliteCompleteBackup(db).export(directory(), at)
        return try {
            InspectedBackupPackage.inspect(file, directory())
        } finally {
            file.close()
        }
    }

    @Test
    fun completeReplacementRestoresParentAndRawInOneTransactionAndIssuesOnlyOneNewIdentity() =
        storage { source ->
            seed(source)
            SqliteSettings(source).putSetting("retained-setting", "original")
            val file = SqliteCompleteBackup(source).export(directory(), at)
            try {
                target { target ->
                    SqliteSettings(target).putSetting("old-setting", "replace")
                    val old = epoch(target)
                    var issued = 0
                    val owner =
                        LocalBackupOperations(target, { at }) {
                            issued++
                            "complete-final-epoch"
                        }
                    val reviewed = owner.reviewComplete(file, directory())
                    try {
                        assertTrue(reviewed.includesRecordings)
                        assertEquals("replace", SqliteSettings(target).getSetting("old-setting"))
                        owner.restoreReviewed(reviewed)
                    } finally {
                        reviewed.close()
                    }
                    assertEquals(1, issued)
                    assertNotEquals(old, epoch(target))
                    assertEquals("original", SqliteSettings(target).getSetting("retained-setting"))
                    assertNull(SqliteSettings(target).getSetting("old-setting"))
                    for (table in
                        listOf(
                            "recording_sessions",
                            "recording_points",
                            "recording_events",
                        )) assertEquals(
                        source.query("SELECT * FROM $table"),
                        target.query("SELECT * FROM $table"),
                    )
                    assertEquals(
                        2,
                        SqliteActivities(target) { at }.activityDetail("activity")!!.samples().size,
                    )
                    assertTrue(target.query("PRAGMA foreign_key_check").isEmpty())
                }
            } finally {
                file.close()
            }
        }

    @Test
    fun finalRawWriteFailureRollsBackAlreadyReplacedParentAndNewDatasetIdentity() =
        storage { source ->
            seed(source)
            SqliteSettings(source).putSetting("incoming", "new")
            val file = complete(source)
            try {
                target { target ->
                    SqliteSettings(target).putSetting("current", "must survive")
                    val before = state(target)
                    val old = epoch(target)
                    val failing =
                        object : SqlDatabase by target {
                            override fun execute(sql: String, arguments: List<SqlValue>) {
                                if (
                                    sql.startsWith("INSERT INTO recording_points") &&
                                        arguments[1] == SqlValue.Integer(1)
                                )
                                    error("Final raw point write fails")
                                target.execute(sql, arguments)
                            }
                        }
                    assertFails { replacement(failing).replace(file) }
                    assertEquals(before, state(target))
                    assertEquals(old, epoch(target))
                    replacement(target).replace(file)
                    assertEquals("new", SqliteSettings(target).getSetting("incoming"))
                }
            } finally {
                file.close()
            }
        }

    @Test
    fun callerRollbackCannotCommitEitherHalfOfCompleteReplacement() = storage { source ->
        seed(source)
        val file = complete(source)
        try {
            target { target ->
                SqliteSettings(target).putSetting("current", "retained")
                val before = state(target)
                val old = epoch(target)
                assertFails {
                    target.transaction {
                        replacement(target).replace(file)
                        error("Caller rejects")
                    }
                }
                assertEquals(before, state(target))
                assertEquals(old, epoch(target))
            }
        } finally {
            file.close()
        }
    }

    @Test
    fun completeReplacementCannotRemoveAnExistingOwnedRecordingMissingFromItsParent() =
        storage { source ->
            val file = complete(source)
            try {
                target { target ->
                    seed(target)
                    val before = state(target)
                    val old = epoch(target)
                    assertFails { replacement(target).replace(file) }
                    assertEquals(before, state(target))
                    assertEquals(old, epoch(target))
                }
            } finally {
                file.close()
            }
        }

    @Test
    fun allOutgoingRunRecoveryStatesBlockCompleteReplacementWithoutWrites() = storage { source ->
        seed(source)
        val file = complete(source)
        try {
            for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) target { target ->
                target.execute(
                    "INSERT INTO recording_sessions VALUES('live',?,?,0,1000,NULL)",
                    listOf(SqlValue.Text(state), SqlValue.Text(at.toString())),
                )
                val before = this.state(target)
                val old = epoch(target)
                assertFails { replacement(target).replace(file) }
                assertEquals(before, this.state(target))
                assertEquals(old, epoch(target))
            }
        } finally {
            file.close()
        }
    }

    @Test
    fun closedInspectedPackageRejectsBeforeAnyDatabaseAccess() = storage { source ->
        seed(source)
        val file = complete(source)
        file.close()
        var touches = 0
        val watched =
            object : SqlDatabase by source {
                override fun <T> transaction(block: () -> T): T {
                    touches++
                    return source.transaction(block)
                }

                override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> {
                    touches++
                    return source.query(sql, arguments)
                }

                override fun execute(sql: String, arguments: List<SqlValue>) {
                    touches++
                    source.execute(sql, arguments)
                }
            }
        assertFails { replacement(watched).replace(file) }
        assertEquals(0, touches)
    }

    private fun source(bytes: ByteArray) =
        object : BackupArchiveSource {
            override val size = bytes.size.toLong()

            override fun read(offset: Long, maximum: Int) =
                bytes.copyOfRange(offset.toInt(), minOf(bytes.size, offset.toInt() + maximum))
        }

    /**
     * Deliberately combine independently VALID, correctly hashed inner files from different states.
     */
    private fun mixed(app: ByteArray, raw: BackupStagingFile): BackupStagingFile {
        val rawBytes =
            raw.read(0, raw.size.toInt()) // Tiny fixture only, not a production route buffer.
        fun hash(bytes: ByteArray) =
            legacyEvidenceSha256(bytes).joinToString("") {
                it.toUByte().toString(16).padStart(2, '0')
            }
        val manifest =
            buildJsonObject {
                    put("format", "rdd-complete-backup")
                    put("version", 1)
                    put(
                        "files",
                        buildJsonObject {
                            for ((name, bytes) in
                                listOf("app.json" to app, "recordings.zip" to rawBytes)) put(
                                name,
                                buildJsonObject {
                                    put("bytes", bytes.size)
                                    put("sha256", hash(bytes))
                                },
                            )
                        },
                    )
                }
                .toString()
                .encodeToByteArray()
        val file = BackupStagingFile.create(directory(), BackupStagingFile.MAX_BYTES)
        try {
            BackupZipWriter.write(
                BackupZipArchive.Kind.COMPLETE,
                listOf(
                    BackupZipWriter.Source("manifest.json", source(manifest)),
                    BackupZipWriter.Source("app.json", source(app)),
                    BackupZipWriter.Source("recordings.zip", source(rawBytes)),
                ),
                file::append,
            )
            file.seal()
            return file
        } catch (e: Throwable) {
            file.close()
            throw e
        }
    }

    @Test
    fun completeInspectionRejectsContradictoryValidInnerSummariesAndWorkoutContexts() {
        for (change in listOf("measurement", "instruction", "missing-context")) storage { db ->
            seed(db)
            db.execute(
                "INSERT INTO coach_support VALUES('execution','run',?)",
                listOf(
                    SqlValue.Text(
                        """{"version":1,"mode":"quick","steps":[{"label":"Easy running","kind":"RUN","millis":60000}]}"""
                    )
                ),
            )
            val app = SqliteApplicationBackup(db).encode(at)
            when (change) {
                "measurement" ->
                    db.execute("UPDATE activities SET payload=REPLACE(payload,'100.0','101.0')")
                "instruction" ->
                    db.execute("UPDATE coach_support SET payload=REPLACE(payload,'60000','30000')")
                "missing-context" -> db.execute("DELETE FROM coach_support")
            }
            val raw = SqliteRecordingArchive(db).export(directory())
            try {
                val file = mixed(app, raw)
                try {
                    assertFails { InspectedBackupPackage.inspect(file, directory()) }
                } finally {
                    file.close()
                }
            } finally {
                raw.close()
            }
        }
    }

    @Test
    fun completeInspectionRejectsContradictoryRemovalProvenanceDespiteCorrectContainerHashes():
        Unit = storage { db ->
        seed(db)
        val app = SqliteApplicationBackup(db).encode(at)
        val receipt =
            org.racedaydreams.domain.storage.RemovedRecordingDetail(
                "run",
                "activity",
                "rdd",
                "run",
                Instants.ofEpochSecond(at.getEpochSecond() + 60, at.getNano().toLong()),
                Instants.ofEpochSecond(at.getEpochSecond() + 120),
                org.racedaydreams.domain.storage.DetailRetention.Policy.LAST_24_MONTHS,
                at,
                mono,
                mono + 60000,
                2,
                0,
                SqliteRecordingEvidence(db).evidence("recording_points", "run").sha256,
                SqliteRecordingEvidence(db).evidence("recording_events", "run").sha256,
            )
        db.execute("DELETE FROM recording_points")
        SqliteRetentionRestore(db) { error("No approval from receipt") }.insert(receipt)
        val raw = SqliteRecordingArchive(db).export(directory())
        try {
            val file = mixed(app, raw)
            try {
                assertFails { InspectedBackupPackage.inspect(file, directory()) }
            } finally {
                file.close()
            }
        } finally {
            raw.close()
        }
    }

    @Test
    fun compatibleCompletePackageRestoresRemovedDetailWithoutLosingSummaryOrIssuingConsent() =
        storage { source ->
            seed(source)
            val file = complete(source)
            try {
                target { target ->
                    replacement(target).replace(file)
                    val indexedEnd =
                        target
                            .query("SELECT end_millis FROM activities WHERE id='activity'")
                            .single()
                            .single()
                            .sqlLong() + 1
                    val receipt =
                        org.racedaydreams.domain.storage.RemovedRecordingDetail(
                            "run",
                            "activity",
                            "rdd",
                            "run",
                            Instants.ofEpochSecond(indexedEnd / 1000, indexedEnd % 1000 * 1000000),
                            Instants.ofEpochSecond(at.getEpochSecond() + 120),
                            org.racedaydreams.domain.storage.DetailRetention.Policy.LAST_24_MONTHS,
                            at,
                            mono,
                            mono + 60000,
                            2,
                            0,
                            SqliteRecordingEvidence(target)
                                .evidence("recording_points", "run")
                                .sha256,
                            SqliteRecordingEvidence(target)
                                .evidence("recording_events", "run")
                                .sha256,
                        )
                    target.execute("DELETE FROM recording_points")
                    SqliteRetentionRestore(target) { error("No consent from receipt") }
                        .insert(receipt)
                    val summaries = target.query("SELECT * FROM activities")
                    replacement(target).replace(file)
                    assertEquals(summaries, target.query("SELECT * FROM activities"))
                    assertNull(SqliteRecordingDetailReceipts(target).removal("run"))
                    assertEquals(2, target.query("SELECT * FROM recording_points").size)
                    assertEquals(
                        SqliteRetentionPreferences(source).load(),
                        SqliteRetentionPreferences(target).load(),
                    )
                }
            } finally {
                file.close()
            }
        }

    @Test
    fun inspectedCompletePackageReplaysItsOwnValidatedRawWithoutSecondDenseCopyOrDatabaseWrites():
        Unit = storage { db ->
        seed(db)
        val exported = SqliteCompleteBackup(db).export(directory(), at)
        val file =
            try {
                InspectedBackupPackage.inspect(exported, directory())
            } finally {
                exported.close()
            }
        try {
            val before = db.query("SELECT total_changes()")
            val points = mutableListOf<Point>()
            val graph =
                file.replayRecordings { line ->
                    val row = Json.parseToJsonElement(line).jsonObject
                    if (row["kind"] == JsonPrimitive("point"))
                        points += RecordingPointJson.decode(row.getValue("payload").jsonObject)
                }
            assertEquals(file.recordings.sessions, graph.sessions)
            assertEquals(2, points.size)
            assertEquals(mono, points.first().fix().monotonicMillis())
            assertEquals(mono + 60000, points.last().fix().monotonicMillis())
            assertEquals(before, db.query("SELECT total_changes()"))
        } finally {
            file.close()
        }
        assertFails { file.replayRecordings { error("Closed package must not replay") } }
    }

    @Test
    fun supportedOlderParentWithoutSupportCanRecoverOriginalContextFromItsRawCompanion() =
        storage { db ->
            seed(db)
            val context =
                """{"version":1,"mode":"quick","steps":[{"label":"Easy running","kind":"RUN","millis":60000}]}"""
            db.execute(
                "INSERT INTO coach_support VALUES('execution','run',?)",
                listOf(SqlValue.Text(context)),
            )
            val current = UserBackupDataJson.element(SqliteApplicationBackup(db).snapshot())
            val oldData =
                JsonObject(
                    current -
                        setOf(
                            "support",
                            "release08",
                            "strengthEngine",
                            "strengthProgramming",
                            "gymTools",
                            "achievements",
                            "bodyMeasurements",
                            "detailRetention",
                        )
                )
            val app =
                buildJsonObject {
                        put("format", BackupEnvelopeJson.FORMAT)
                        put("version", 5)
                        put("createdAt", at.toString())
                        put("data", oldData)
                    }
                    .toString()
                    .encodeToByteArray()
            UserBackupDataJson.inspect(app)
            val raw = SqliteRecordingArchive(db).export(directory())
            try {
                val file = mixed(app, raw)
                try {
                    val inspected = InspectedBackupPackage.inspect(file, directory())
                    try {
                        target { target ->
                            replacement(target).replace(inspected)
                            assertEquals(
                                context,
                                target
                                    .query(
                                        "SELECT payload FROM coach_support WHERE kind='execution' AND id='run'"
                                    )
                                    .single()
                                    .single()
                                    .sqlText(),
                            )
                            assertEquals(2, target.query("SELECT * FROM recording_points").size)
                        }
                    } finally {
                        inspected.close()
                    }
                } finally {
                    file.close()
                }
            } finally {
                raw.close()
            }
        }

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
