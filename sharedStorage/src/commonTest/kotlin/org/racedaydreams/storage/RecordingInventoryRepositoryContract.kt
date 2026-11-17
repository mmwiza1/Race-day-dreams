package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.portability.TrackPoint
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.time.*

abstract class RecordingInventoryRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")

    private fun storage(block: (SqlDatabase) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "inventory-test")
            }
            block(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun gpxImportKeepsSegmentsAndDeduplicatesWithoutOverwritingAnOpenRun() = storage { db ->
        val importer = LocalBackupOperations(db, { at }) { error("No new identifier required") }
        fun point(seconds: Long, latitude: Double) =
            TrackPoint(
                Instants.ofEpochSecond(at.getEpochSecond() + seconds),
                latitude,
                -0.1,
                null,
                null,
                null,
                null,
            )
        val track =
            listOf(
                listOf(point(0, 51.5), point(10, 51.5002)),
                listOf(point(40, 51.51), point(50, 51.5102)),
            )
        val staging = SqliteRecordingStaging(db)
        staging.start("open", at, 0, null, null)
        assertFailsWith<IllegalArgumentException> { importer.importGpx(track) }
        assertEquals("open", staging.unfinished()?.id)
        assertTrue(SqliteActivities(db) { at }.activitySummaries().isEmpty())
        db.execute("DELETE FROM recording_sessions WHERE id='open'")

        val id = importer.importGpx(track)
        assertEquals(id, importer.importGpx(track))
        val activities = SqliteActivities(db) { at }
        val route = activities.recordedRoute(id)
        assertEquals(4, route.points.size)
        assertTrue(route.points[2].discontinuity())
        assertEquals(route.points[1].cumulativeMetres(), route.points[2].cumulativeMetres())
        val summary = activities.activitySummaries().single()
        assertEquals("gpx", summary.source().provider())
        assertNull(summary.movingTime())
        assertTrue(requireNotNull(summary.distanceMetres()) in 40.0..50.0)
        assertFailsWith<IllegalArgumentException> { importer.importGpx(track.reversed()) }
        assertEquals(1, activities.activitySummaries().size)
    }

    @Test
    fun stagingRetryPreservesEvidenceAndLateConflictRollsBackTheBatch() = storage { db ->
        val staging = SqliteRecordingStaging(db)
        staging.start("run", at, 0, null, null)
        val pause = RunMetricsEngine.Event("PAUSE", 10, at)
        staging.append("run", emptyList(), listOf(pause), 0, 0, 10, "PAUSED", null)
        staging.append("run", emptyList(), listOf(pause), 0, 0, 10, "PAUSED", null)
        val point =
            RunMetricsEngine.Point(
                RunMetricsEngine.Fix(at, 11, 51.5, -0.1, null, null, null, null),
                0.0,
                0,
                0,
                false,
                null,
                null,
            )
        assertFailsWith<IllegalStateException> {
            staging.append(
                "run",
                listOf(point),
                listOf(RunMetricsEngine.Event("RESUME", 11, at)),
                0,
                0,
                11,
                "ACTIVE",
                null,
            )
        }
        assertTrue(db.query("SELECT payload FROM recording_points").isEmpty())
        assertEquals(1, db.query("SELECT payload FROM recording_events").size)
        assertEquals(10L, staging.unfinished()?.lastMono)
        assertEquals("PAUSED", staging.unfinished()?.state)
        staging.markRecoverable()
        assertEquals("RECOVERABLE", staging.unfinished()?.state)
    }

    @Test
    fun finishingIsIdempotentAndRetainsNormalizedSamples() = storage { db ->
        val staging = SqliteRecordingStaging(db)
        val finish = SqliteRecordingFinalisation(db, { at }, { error("Unexpected ID request") })
        staging.start("native", at, 0, null, null)
        val points =
            listOf(0L, 10_000L).map { millis ->
                RunMetricsEngine.Point(
                    RunMetricsEngine.Fix(
                        Instants.ofEpochSecond(
                            at.getEpochSecond() + millis / 1000,
                            at.getNano().toLong(),
                        ),
                        millis,
                        51.5,
                        -0.1,
                        null,
                        null,
                        null,
                        null,
                    ),
                    millis / 1000.0,
                    millis,
                    0,
                    false,
                    null,
                    null,
                )
            }
        staging.append("native", points, emptyList(), 0, 0, 10_000, "ACTIVE", null)
        val session = assertNotNull(staging.unfinished())
        db.execute(
            "CREATE TRIGGER block_finish BEFORE UPDATE ON recording_sessions WHEN NEW.state='FINISHED' BEGIN SELECT RAISE(ABORT,'test'); END"
        )
        assertFails { finish.finish(session, null, false) }
        assertTrue(SqliteActivities(db) { at }.activitySummaries().isEmpty())
        assertEquals(session, staging.unfinished())
        db.execute("DROP TRIGGER block_finish")
        val id = finish.finish(session, null, false)
        assertEquals("rdd:native", id)
        assertEquals(id, finish.finish(session, 1, true))
        assertNull(staging.unfinished())
        val activity = assertNotNull(SqliteActivities(db) { at }.activityDetail(id))
        assertEquals(10.0, activity.distanceMetres())
        assertEquals(2, activity.samples().size)
        val retained = SqliteActivities(db) { at }.recordedRoute(id)
        assertEquals(points, retained.points)
        assertFalse(retained.detailRemoved)
        val exported =
            retained.points.map { point ->
                val fix = point.fix()
                TrackPoint(
                    fix.timestamp(),
                    fix.latitude(),
                    fix.longitude(),
                    fix.altitude(),
                    null,
                    null,
                    null,
                )
            }
        val imported = LocalBackupOperations(db, { at }) { error("Existing run should be reused") }
        assertEquals(id, imported.importGpx(listOf(exported)))
        assertEquals(1, SqliteActivities(db) { at }.activitySummaries().size)
        assertTrue(SqliteActivities(db) { at }.recordedRoute("missing").points.isEmpty())
    }

    @Test
    fun recoveryReportsOnlyRetainedTimeAndDiscardRequiresCurrentInterruptedHeader() =
        storage { db ->
            val staging = SqliteRecordingStaging(db)
            staging.start("run", at, 0, "{}", "{}")
            val pause = RunMetricsEngine.Event("PAUSE", 10, at)
            staging.append("run", emptyList(), listOf(pause), 0, 0, 30, "PAUSED", null)
            val live = assertNotNull(staging.unfinished())
            assertNull(staging.recovery())
            assertFalse(staging.discardRecovery(live))
            staging.markRecoverable()
            val recovery = assertNotNull(staging.recovery())
            assertEquals(30L, recovery.elapsedMillis)
            assertEquals(10L, recovery.activeMillis)
            assertEquals(0L, recovery.pointCount)
            assertTrue(recovery.paused)
            assertTrue(recovery.structured)
            assertFalse(staging.discardRecovery(recovery.session.copy(lastMono = 29)))
            assertTrue(staging.discardRecovery(recovery.session))
            assertFalse(staging.discardRecovery(recovery.session))
            assertNull(staging.unfinished())
            assertTrue(db.query("SELECT payload FROM recording_events").isEmpty())
            assertTrue(db.query("SELECT payload FROM coach_support WHERE id='run'").isEmpty())
            assertTrue(db.query("SELECT payload FROM release08 WHERE id='run'").isEmpty())
        }

    private fun seed(
        db: SqlDatabase,
        id: String = "run",
        provider: String = "rdd",
        summary: String = "Σ",
    ) {
        db.execute(
            "INSERT INTO activities(id,provider,record_id,origin_package,start_millis,end_millis,payload) VALUES(?,?,?,NULL,0,?,?)",
            listOf(
                SqlValue.Text("activity-$id"),
                SqlValue.Text(provider),
                SqlValue.Text(id),
                SqlValue.Integer(1000123),
                SqlValue.Text(summary),
            ),
        )
        db.execute(
            "INSERT INTO recording_sessions VALUES(?,'FINISHED',?,0,1000,?)",
            listOf(SqlValue.Text(id), SqlValue.Text(at.toString()), SqlValue.Text("activity-$id")),
        )
    }

    @Test
    fun independentAggregatesCountActualRowsAndUtf8BytesWithoutMultiplyingPointEventJoins() =
        storage { db ->
            seed(db)
            seed(db, "external", "health_connect", "A")
            repeat(3) {
                db.execute(
                    "INSERT INTO recording_points VALUES('run',?,?)",
                    listOf(SqlValue.Integer(it.toLong()), SqlValue.Text("π")),
                )
            }
            repeat(2) {
                db.execute(
                    "INSERT INTO recording_events VALUES('run',?,?)",
                    listOf(SqlValue.Integer(it.toLong()), SqlValue.Text("🏃")),
                )
            }
            var fileReads = 0
            val before = db.query("SELECT total_changes()")
            val result =
                SqliteRecordingInventory(db) {
                        fileReads++
                        2048
                    }
                    .inspect()
            assertEquals(1, fileReads)
            assertEquals(3L, result.usage.pointRows)
            assertEquals(2L, result.usage.eventRows)
            assertEquals(14L, result.usage.detailPayloadBytes)
            assertEquals(2L, result.usage.activityRows)
            assertEquals(3L, result.usage.activityPayloadBytes)
            assertEquals(2048L, result.usage.databaseFileBytes)
            val owned = result.recordings.single { it.sessionId() == "run" }
            assertEquals(3L, owned.points())
            assertEquals(2L, owned.events())
            assertEquals(0L, owned.denseSamples())
            val external = result.recordings.single { it.sessionId() == "external" }
            assertNull(external.denseSamples())
            val preview =
                DetailRetention.preview(
                    DetailRetention.Policy.LAST_24_MONTHS,
                    result.recordings,
                    at,
                    "epoch",
                )
            assertEquals(listOf(owned), preview.recordings())
            assertFalse(result.unfinishedRecording)
            assertEquals(before, db.query("SELECT total_changes()"))
            assertFails { (result.recordings as MutableList).clear() }
        }

    @Test
    fun missingActivityAndAllRecoveryStatesRemainUnknownAndProtected() = storage { db ->
        db.execute(
            "INSERT INTO recording_sessions VALUES('live','ACTIVE',?,0,1000,NULL)",
            listOf(SqlValue.Text(at.toString())),
        )
        for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) {
            db.execute("UPDATE recording_sessions SET state=?", listOf(SqlValue.Text(state)))
            val result = SqliteRecordingInventory(db).inspect()
            assertTrue(result.unfinishedRecording)
            val row = result.recordings.single()
            assertNull(row.activityId())
            assertNull(row.endedAt())
            assertNull(row.denseSamples())
            assertFalse(row.finished())
            assertEquals("", row.provider())
            assertTrue(
                DetailRetention.preview(
                        DetailRetention.Policy.LAST_24_MONTHS,
                        result.recordings,
                        at,
                        "epoch",
                    )
                    .recordings()
                    .isEmpty()
            )
        }
        db.execute("UPDATE recording_sessions SET state='FINISHED',activity_id='missing'")
        val orphan = SqliteRecordingInventory(db).inspect().recordings.single()
        assertNull(orphan.endedAt())
        assertEquals("", orphan.sourceRecordId())
        assertNull(orphan.denseSamples())
    }

    @Test
    fun indexedRecheckMatchesInventoryAndPreservesOriginalUpperBoundAtFullLongMillisecondRange() =
        storage { db ->
            seed(db)
            val repo = SqliteRecordingInventory(db)
            val cases =
                listOf(
                    Triple(1000123L, 1000L, 124000000L),
                    Triple(-1L, 0L, 0L),
                    Triple(-1000L, -1L, 1000000L),
                    Triple(999L, 1L, 0L),
                    Triple(Long.MAX_VALUE, 9223372036854775L, 808000000L),
                    Triple(Long.MIN_VALUE, -9223372036854776L, 193000000L),
                )
            for ((millis, seconds, nanos) in cases) {
                db.execute(
                    "UPDATE activities SET start_millis=?,end_millis=?",
                    listOf(SqlValue.Integer(minOf(0, millis)), SqlValue.Integer(millis)),
                )
                val header = repo.inspect().recordings.single()
                assertEquals(Instants.ofEpochSecond(seconds, nanos), header.endedAt())
                assertEquals(header, repo.recording("run"))
            }
            assertNull(repo.recording("missing"))
        }

    @Test
    fun readsShareOneSnapshotNeverLoadPayloadsAndUseConstantQueryCountAsHistoryGrows() =
        storage { db ->
            var depth = 0
            var queries = 0
            val watched =
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
                        assertFalse(sql.contains("SELECT payload"))
                        queries++
                        return db.query(sql, arguments)
                    }

                    override fun execute(sql: String, arguments: List<SqlValue>) =
                        error("Inventory cannot write")
                }
            seed(db)
            val one = SqliteRecordingInventory(watched).inspect()
            val baseline = queries
            for (i in 1..20) seed(db, "run-$i")
            queries = 0
            val many = SqliteRecordingInventory(watched).inspect()
            assertEquals(baseline, queries)
            assertTrue(queries <= 6)
            assertEquals(1, one.recordings.size)
            assertEquals(21, many.recordings.size)
        }

    @Test
    fun absentPhysicalFileObservationIsUnknownAndFailureCannotBecomeFalseZeroUsage() =
        storage { db ->
            seed(db)
            assertNull(SqliteRecordingInventory(db).inspect().usage.databaseFileBytes)
            val before = db.query("SELECT total_changes()")
            assertFails { SqliteRecordingInventory(db) { -1 }.inspect() }
            assertFails {
                SqliteRecordingInventory(db) { error("Disk observation unavailable") }.inspect()
            }
            assertEquals(before, db.query("SELECT total_changes()"))
        }

    @Test
    fun oversizeMetadataFailsBeforeHistoryReadWithoutReturningTruncatedInventory() = storage { db ->
        seed(db)
        for (bad in
            listOf(
                listOf(100001L, 3, 8, 12, 3, 3, 32),
                listOf(1L, 257, 8, 12, 3, 3, 32),
                listOf(1L, 3, 33, 12, 3, 3, 32),
                listOf(1L, 3, 8, 12, 3, 3, BackupEnvelopeJson.MAX_BYTES.toLong() + 1),
            )) {
            var historyReads = 0
            val watched =
                object : SqlDatabase by db {
                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        if (sql.startsWith("SELECT COUNT(*),COALESCE(MAX(LENGTH(s.id))"))
                            return listOf(bad.map { SqlValue.Integer(it) })
                        if (sql.startsWith("SELECT s.id,s.state")) historyReads++
                        return db.query(sql, arguments)
                    }
                }
            assertFails { SqliteRecordingInventory(watched).inspect() }
            assertEquals(0, historyReads)
        }
        assertEquals(1, SqliteRecordingInventory(db).inspect().recordings.size)
    }

    @Test
    fun overflowingLogicalByteTotalsFailInsteadOfWrappingOrClaimingReclaimedStorage() =
        storage { db ->
            val watched =
                object : SqlDatabase by db {
                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        if (sql.startsWith("SELECT COUNT(*),COALESCE(SUM")) {
                            if (sql.endsWith("FROM recording_points"))
                                return listOf(
                                    listOf(SqlValue.Integer(1), SqlValue.Integer(Long.MAX_VALUE))
                                )
                            if (sql.endsWith("FROM recording_events"))
                                return listOf(listOf(SqlValue.Integer(1), SqlValue.Integer(1)))
                        }
                        return db.query(sql, arguments)
                    }
                }
            assertFailsWith<ArithmeticException> { SqliteRecordingInventory(watched).inspect() }
        }

    @Test
    fun unsupportedRecordingStateCannotBeAssumedFinishedOrSafeToClean() = storage { db ->
        seed(db)
        db.execute("UPDATE recording_sessions SET state='UNKNOWN_FUTURE_STATE'")
        assertFails { SqliteRecordingInventory(db).inspect() }
        assertFails { SqliteRecordingInventory(db).recording("run") }
        assertEquals(
            "UNKNOWN_FUTURE_STATE",
            db.query("SELECT state FROM recording_sessions").single().single().sqlText(),
        )
    }
}
