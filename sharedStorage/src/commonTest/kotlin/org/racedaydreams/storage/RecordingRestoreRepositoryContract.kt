package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.*
import org.racedaydreams.domain.storage.*
import org.racedaydreams.domain.time.*

abstract class RecordingRestoreRepositoryContract {
    abstract fun database(): SqlDatabase

    abstract fun directory(): String

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val mono = 9007199254740993L
    private var epochs = 0

    private fun restore(db: SqlDatabase) =
        SqliteRecordingArchiveRestore(db) { "restored-raw-${++epochs}" }

    private fun epoch(db: SqlDatabase) =
        db.query("SELECT epoch FROM detail_retention_state").single().single().sqlText()

    private fun storage(block: (SqlDatabase, SqlDatabase) -> Unit) {
        val source = database()
        val target = database()
        try {
            for (db in listOf(source, target)) ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "raw-restore-test")
            }
            block(source, target)
        } finally {
            source.close()
            target.close()
        }
    }

    private fun seed(db: SqlDatabase, state: String = "FINISHED", points: Int = 2) {
        db.execute(
            "INSERT INTO recording_sessions VALUES('run',?,?,?,?,'activity')",
            listOf(
                SqlValue.Text(state),
                SqlValue.Text(at.toString()),
                SqlValue.Integer(mono),
                SqlValue.Integer(mono + 60000),
            ),
        )
        if (state != "FINISHED") db.execute("UPDATE recording_sessions SET activity_id=NULL")
        for (i in 0 until points) {
            val p =
                Point(
                    Fix(
                        Instants.ofEpochSecond(at.getEpochSecond() + i * 20, at.getNano().toLong()),
                        mono + i * 20000,
                        51.5 + .0001 * i,
                        -.12,
                        4.0,
                        -5.0,
                        null,
                        null,
                    ),
                    i * 100.0,
                    i * 20000L,
                    0,
                    false,
                    null,
                    null,
                )
            db.execute(
                "INSERT INTO recording_points VALUES('run',?,?)",
                listOf(SqlValue.Integer(i.toLong()), SqlValue.Text(RecordingPointJson.encode(p))),
            )
        }
        db.execute(
            "INSERT INTO recording_events VALUES('run',0,?)",
            listOf(
                SqlValue.Text(
                    """{"type":"GPS_RECOVERED","mono":$mono,"wall":"$at","retainedNote":"source evidence"}"""
                )
            ),
        )
        db.execute(
            "INSERT INTO coach_support VALUES('execution','run',?)",
            listOf(
                SqlValue.Text(
                    """{"version":1,"mode":"quick","steps":[{"label":"Easy running","kind":"RUN","millis":60000}]}"""
                )
            ),
        )
        if (state == "FINISHED")
            SqliteActivities(db) { at }
                .upsertManualActivity(
                    ActivityRecord(
                        "activity",
                        SourceIdentity("rdd", "run", null),
                        at,
                        Instants.ofEpochSecond(at.getEpochSecond() + 60, at.getNano().toLong()),
                        null,
                        null,
                        200.0,
                        Durations.ofSeconds(60),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Original note",
                        emptyList(),
                        emptyList(),
                        null,
                        null,
                    )
                )
    }

    private fun archive(db: SqlDatabase): InspectedRecordingArchive {
        val file = SqliteRecordingArchive(db).export(directory())
        return try {
            InspectedRecordingArchive.inspect(file, directory())
        } finally {
            file.close()
        }
    }

    @Test
    fun supportedRawOneTwoAndThreeArchivesRestoreOriginalMetadataAndActualEvidence() {
        for (version in 1..3) storage { source, target ->
            seed(source)
            val original = SqliteRecordingArchive(source).export(directory())
            val rewritten =
                BackupStagingFile.create(
                    directory(),
                    CompleteBackupManifest.MAX_RECORDINGS_ZIP_BYTES,
                )
            try {
                val zip = BackupZipArchive.inspect(original, BackupZipArchive.Kind.RECORDINGS)
                fun entry(name: String): ByteArray {
                    val chunks = mutableListOf<Byte>()
                    zip.extract(name) { chunks.addAll(it.toList()) }
                    return chunks.toByteArray()
                }
                val manifest =
                    Json.parseToJsonElement(entry("manifest.json").decodeToString()).jsonObject
                val inputs =
                    listOf(
                        "manifest.json" to
                            JsonObject(manifest + ("version" to JsonPrimitive(version)))
                                .toString()
                                .encodeToByteArray(),
                        "recordings.jsonl" to entry("recordings.jsonl"),
                    )
                BackupZipWriter.write(
                    BackupZipArchive.Kind.RECORDINGS,
                    inputs.map { (name, bytes) ->
                        BackupZipWriter.Source(
                            name,
                            object : BackupArchiveSource {
                                override val size = bytes.size.toLong()

                                override fun read(offset: Long, maximum: Int) =
                                    bytes.copyOfRange(
                                        offset.toInt(),
                                        minOf(bytes.size, offset.toInt() + maximum),
                                    )
                            },
                        )
                    },
                    rewritten::append,
                )
                rewritten.seal()
                val checked = InspectedRecordingArchive.inspect(rewritten, directory())
                try {
                    assertEquals(version, checked.snapshot.version)
                    restore(target).merge(checked)
                    for (table in
                        listOf(
                            "recording_sessions",
                            "recording_points",
                            "recording_events",
                            "coach_support",
                        )) assertEquals(
                        source.query("SELECT * FROM $table"),
                        target.query("SELECT * FROM $table"),
                    )
                } finally {
                    checked.close()
                }
            } finally {
                original.close()
                rewritten.close()
            }
        }
    }

    @Test
    fun closedInspectedRawRejectsBeforeAnyDatabaseAccess() = storage { source, target ->
        seed(source)
        val file = archive(source)
        file.close()
        var touches = 0
        val watched =
            object : SqlDatabase by target {
                override fun <T> transaction(block: () -> T): T {
                    touches++
                    return target.transaction(block)
                }

                override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> {
                    touches++
                    return target.query(sql, arguments)
                }

                override fun execute(sql: String, arguments: List<SqlValue>) {
                    touches++
                    target.execute(sql, arguments)
                }
            }
        assertFails { restore(watched).merge(file) }
        assertEquals(0, touches)
    }

    private fun proof(db: SqlDatabase) =
        RemovedRecordingDetail(
            "run",
            "activity",
            "rdd",
            "run",
            Instants.ofEpochSecond(at.getEpochSecond() + 60, at.getNano().toLong()),
            Instants.ofEpochSecond(at.getEpochSecond() + 120),
            DetailRetention.Policy.LAST_24_MONTHS,
            at,
            mono,
            mono + 60000,
            2,
            1,
            SqliteRecordingEvidence(db).evidence("recording_points", "run").sha256,
            SqliteRecordingEvidence(db).evidence("recording_events", "run").sha256,
        )

    private fun prune(db: SqlDatabase, receipt: RemovedRecordingDetail) {
        db.execute("DELETE FROM recording_points")
        db.execute("DELETE FROM recording_events")
        SqliteRetentionRestore(db) { error("Receipt population cannot issue an epoch") }
            .insert(receipt)
    }

    private fun rows(db: SqlDatabase) =
        listOf(
                "recording_sessions",
                "recording_points",
                "recording_events",
                "activities",
                "activity_annotations",
                "coach_support",
                "best_efforts",
                "preserved_best_efforts",
                "recording_detail_removals",
            )
            .associateWith { db.query("SELECT * FROM $it") }

    @Test
    fun newRawMergeCopiesActualCoordinatesSourceClocksAndContextWithoutChangingExistingHistory() =
        storage { source, target ->
            seed(source)
            val file = archive(source)
            try {
                val old = epoch(target)
                restore(target).merge(file)
                for (table in
                    listOf(
                        "recording_sessions",
                        "recording_points",
                        "recording_events",
                        "coach_support",
                    )) assertEquals(
                    source.query("SELECT * FROM $table"),
                    target.query("SELECT * FROM $table"),
                )
                val actual =
                    SqliteActivities(target) { error("Read must not date evidence") }
                        .activityDetail("activity")!!
                assertEquals(200.0, actual.distanceMetres())
                assertEquals(2, actual.samples().size)
                assertEquals(-5.0, actual.samples().first().elevationMetres())
                assertNotEquals(old, epoch(target))
                assertTrue(target.query("PRAGMA foreign_key_check").isEmpty())
            } finally {
                file.close()
            }
        }

    @Test
    fun repeatedCompatibleMergeIsEvidenceIdempotentAndKeepsNewerAnnotationsAndPbs() =
        storage { source, target ->
            seed(source)
            val file = archive(source)
            try {
                restore(target).merge(file)
                SqliteActivities(target) { at }
                    .saveActivityNotes("activity", "Later annotation", 8.0)
                val before = rows(target)
                repeat(2) {
                    restore(target).merge(file)
                    assertEquals(before, rows(target))
                }
            } finally {
                file.close()
            }
        }

    @Test
    fun conflictingHeaderRejectsBeforeAnyReplayWrite() = storage { source, target ->
        seed(source)
        seed(target)
        target.execute("UPDATE recording_sessions SET last_mono=last_mono+1")
        val before = rows(target)
        val file = archive(source)
        var writes = 0
        val observed =
            object : SqlDatabase by target {
                override fun execute(sql: String, arguments: List<SqlValue>) {
                    writes++
                    target.execute(sql, arguments)
                }
            }
        try {
            assertFails { restore(observed).merge(file) }
            assertEquals(0, writes)
            assertEquals(before, rows(target))
        } finally {
            file.close()
        }
    }

    @Test
    fun changedPointOrExtraEvidenceCannotBeHiddenByCompatibleSessionHeader() =
        storage { source, target ->
            seed(source)
            seed(target, points = 3)
            val file = archive(source)
            try {
                val before = rows(target)
                val oldEpoch = epoch(target)
                assertFails { restore(target).merge(file) }
                assertEquals(before, rows(target))
                assertEquals(oldEpoch, epoch(target))
                target.execute("DELETE FROM recording_points WHERE sequence=2")
                target.execute(
                    "UPDATE recording_points SET payload=REPLACE(payload,'51.5','51.6') WHERE sequence=0"
                )
                val changed = rows(target)
                assertFails { restore(target).merge(file) }
                assertEquals(changed, rows(target))
            } finally {
                file.close()
            }
        }

    @Test
    fun inconsistentExistingReceiptCannotBeConsumedEvenWhenOriginalSampleHashesMatch() {
        for (change in listOf("provider", "end", "activity")) storage { source, target ->
            seed(source)
            seed(target)
            val original = proof(target)
            val inconsistent =
                when (change) {
                    "provider" -> original.copy(provider = "gpx")
                    "end" -> original.copy(endedAt = at)
                    else -> original.copy(activityId = "another-activity")
                }
            if (change == "activity") {
                val actual =
                    ActivityJson.decode(
                        target.query("SELECT payload FROM activities").single().single().sqlText()
                    )
                SqliteActivities(target) { at }
                    .upsertManualActivity(
                        actual.copy(
                            id = "another-activity",
                            source = SourceIdentity("rdd", "another-run", null),
                        )
                    )
            }
            prune(target, inconsistent)
            val before = rows(target)
            val old = epoch(target)
            val file = archive(source)
            try {
                assertFails { restore(target).merge(file) }
                assertEquals(before, rows(target))
                assertEquals(old, epoch(target))
            } finally {
                file.close()
            }
        }
    }

    @Test
    fun exactlyMatchingArchivedDetailRemovesOnlyItsOriginalRemovalReceipt() =
        storage { source, target ->
            seed(source)
            seed(target)
            val receipt = proof(target)
            prune(target, receipt)
            val summaries = target.query("SELECT * FROM activities")
            val prefs = SqliteRetentionPreferences(target).load()
            val file = archive(source)
            try {
                restore(target).merge(file)
                assertNull(SqliteRecordingDetailReceipts(target).removal("run"))
                assertEquals(
                    source.query("SELECT * FROM recording_points"),
                    target.query("SELECT * FROM recording_points"),
                )
                assertEquals(summaries, target.query("SELECT * FROM activities"))
                assertEquals(prefs, SqliteRetentionPreferences(target).load())
            } finally {
                file.close()
            }
        }

    @Test
    fun originalByteHashMismatchRollsBackAllRehydratedRowsAndKeepsReceipt() =
        storage { source, target ->
            seed(source)
            seed(target)
            // Numerically identical whitespace is STILL different original receipt evidence.
            target.execute("UPDATE recording_points SET payload=' '||payload WHERE sequence=0")
            val receipt = proof(target)
            prune(target, receipt)
            val before = rows(target)
            val old = epoch(target)
            val file = archive(source)
            try {
                assertFails { restore(target).merge(file) }
                assertEquals(before, rows(target))
                assertEquals(old, epoch(target))
                assertEquals(receipt, SqliteRecordingDetailReceipts(target).removal("run"))
            } finally {
                file.close()
            }
        }

    @Test
    fun incomingRemovedReceiptCannotDeleteAlreadyAvailableMatchingRawEvidence() =
        storage { source, target ->
            seed(source)
            seed(target)
            prune(source, proof(source))
            val before = rows(target)
            val file = archive(source)
            try {
                restore(target).merge(file)
                assertEquals(before, rows(target))
                assertNull(SqliteRecordingDetailReceipts(target).removal("run"))
            } finally {
                file.close()
            }
        }

    @Test
    fun newRemovedSessionRetainsSummaryAndProvenanceWithoutInventingPoints() =
        storage { source, target ->
            seed(source)
            val receipt = proof(source)
            prune(source, receipt)
            val file = archive(source)
            try {
                restore(target).merge(file)
                assertEquals(receipt, SqliteRecordingDetailReceipts(target).removal("run"))
                assertTrue(target.query("SELECT * FROM recording_points").isEmpty())
                val activity = SqliteActivities(target) { at }.activityDetail("activity")!!
                assertEquals(200.0, activity.distanceMetres())
                assertTrue(activity.samples().isEmpty())
            } finally {
                file.close()
            }
        }

    @Test
    fun importedUnfinishedRunRequiresRecoveryAndExistingUnfinishedRunBlocksAnotherMerge() {
        for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) storage { source, target ->
            seed(source, state)
            val file = archive(source)
            try {
                restore(target).merge(file)
                assertEquals(
                    "RECOVERABLE",
                    target.query("SELECT state FROM recording_sessions").single().single().sqlText(),
                )
                assertTrue(target.query("SELECT * FROM activities").isEmpty())
                val before = rows(target)
                val old = epoch(target)
                assertFails { restore(target).merge(file) }
                assertEquals(before, rows(target))
                assertEquals(old, epoch(target))
            } finally {
                file.close()
            }
        }
    }

    @Test
    fun contextConflictCannotReplaceOriginalWorkoutInstructions() = storage { source, target ->
        seed(source)
        seed(target)
        target.execute("UPDATE coach_support SET payload=REPLACE(payload,'60000','30000')")
        val before = rows(target)
        val file = archive(source)
        try {
            assertFails { restore(target).merge(file) }
            assertEquals(before, rows(target))
        } finally {
            file.close()
        }
    }

    @Test
    fun changedPhysicalSummaryOrInconsistentSourceIndexCannotOverwriteActuals() {
        for (change in listOf("measurement", "index")) storage { source, target ->
            seed(source)
            seed(target)
            if (change == "index") target.execute("UPDATE activities SET provider='manual'")
            else {
                val original =
                    ActivityJson.decode(
                        target.query("SELECT payload FROM activities").single().single().sqlText()
                    )
                target.execute(
                    "UPDATE activities SET payload=?",
                    listOf(
                        SqlValue.Text(ActivityJson.encode(original.copy(averageHeartRate = 144.0)))
                    ),
                )
            }
            val before = rows(target)
            val old = epoch(target)
            val file = archive(source)
            try {
                assertFails { restore(target).merge(file) }
                assertEquals(before, rows(target))
                assertEquals(old, epoch(target))
            } finally {
                file.close()
            }
        }
    }

    @Test
    fun reusedApprovalIdentityRejectsBeforeAnyEvidenceWrite() = storage { source, target ->
        seed(source)
        val file = archive(source)
        val before = rows(target)
        val old = epoch(target)
        var writes = 0
        val watched =
            object : SqlDatabase by target {
                override fun execute(sql: String, arguments: List<SqlValue>) {
                    writes++
                    target.execute(sql, arguments)
                }
            }
        try {
            assertFails { SqliteRecordingArchiveRestore(watched) { old }.merge(file) }
            assertEquals(0, writes)
            assertEquals(before, rows(target))
            assertEquals(old, epoch(target))
        } finally {
            file.close()
        }
    }

    @Test
    fun finalEpochFailureRollsBackNewSessionsActivitiesPointsAndQualifiedPbs() =
        storage { source, target ->
            seed(source)
            val file = archive(source)
            val before = rows(target)
            val old = epoch(target)
            val failing =
                object : SqlDatabase by target {
                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        if (sql.startsWith("UPDATE detail_retention_state"))
                            error("Final epoch write fails")
                        target.execute(sql, arguments)
                    }
                }
            try {
                assertFails { restore(failing).merge(file) }
                assertEquals(before, rows(target))
                assertEquals(old, epoch(target))
                restore(target).merge(file)
                assertEquals(2, target.query("SELECT * FROM recording_points").size)
            } finally {
                file.close()
            }
        }

    @Test
    fun callerRollbackCannotLeaveImportedDetailOrNewApprovalIdentityBehind() =
        storage { source, target ->
            seed(source)
            val file = archive(source)
            val before = rows(target)
            val old = epoch(target)
            try {
                assertFails {
                    target.transaction {
                        restore(target).merge(file)
                        error("Caller rejects")
                    }
                }
                assertEquals(before, rows(target))
                assertEquals(old, epoch(target))
            } finally {
                file.close()
            }
        }
}
