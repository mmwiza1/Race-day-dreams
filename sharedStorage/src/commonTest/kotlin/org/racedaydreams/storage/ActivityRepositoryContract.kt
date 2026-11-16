package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.Fix
import org.racedaydreams.domain.recording.RunMetricsEngine.Point
import org.racedaydreams.domain.time.*

/** Real SQLite contract on JVM and Apple; source authority/history/PBs commit together. */
abstract class ActivityRepositoryContract {
    protected abstract fun database(): SqlDatabase

    private val today = CalendarDates.of(2026, 11, 15)
    private val clock = StoredInstant.parse("2026-11-15T12:00:00Z")

    @Test
    fun retentionMetadataSnapshotPreservesConsentProofsAndActualHistoryWithoutWriting() =
        storage { db, activities, results ->
            val expected = mutableListOf<org.racedaydreams.domain.storage.RemovedRecordingDetail>()
            for (id in listOf("b", "a")) {
                val actual = activity(id, "rdd", sampled = false)
                activities.upsertManualActivity(actual)
                val proof =
                    org.racedaydreams.domain.storage.RemovedRecordingDetail(
                        "record-$id",
                        id,
                        "rdd",
                        "record-$id",
                        actual.end(),
                        clock,
                        org.racedaydreams.domain.storage.DetailRetention.Policy.LAST_24_MONTHS,
                        actual.start(),
                        9007199254740993L,
                        9007199254741993L,
                        2,
                        1,
                        "ab".repeat(32),
                        "cd".repeat(32),
                    )
                expected.add(proof)
                db.execute(
                    "INSERT INTO recording_detail_removals VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    listOf(
                        SqlValue.Text(proof.sessionId()),
                        SqlValue.Text(id),
                        SqlValue.Text("rdd"),
                        SqlValue.Text(proof.recordId()),
                        SqlValue.Text(proof.endedAt().toString()),
                        SqlValue.Text(clock.toString()),
                        SqlValue.Text(proof.policy().name),
                        SqlValue.Text(proof.startedAt().toString()),
                        SqlValue.Integer(proof.startedMono()),
                        SqlValue.Integer(proof.lastMono()),
                        SqlValue.Integer(2),
                        SqlValue.Integer(1),
                        SqlValue.Text(proof.pointsHash()),
                        SqlValue.Text(proof.eventsHash()),
                    ),
                )
            }
            results.saveResult(manualResult())
            db.execute(
                "UPDATE detail_retention_preferences SET policy='KEEP_FOREVER',choice='EXISTING_UNREVIEWED',chosen_at=NULL,choice_id=NULL"
            )
            val history = activities.activitySummaries()
            val bests = results.results()
            val totalChanges = db.query("SELECT total_changes()")
            val repository = SqliteRecordingDetailReceipts(db)
            val snapshot = repository.snapshot()
            val decoded =
                DetailRetentionArchiveJson.decode(DetailRetentionArchiveJson.element(snapshot))
            assertEquals(
                org.racedaydreams.domain.storage.DetailRetention.Preferences.existing(),
                decoded.preferences,
            )
            assertEquals(expected.sortedBy { it.sessionId() }, decoded.removals)
            assertEquals(decoded.removals.first(), repository.removal("record-a"))
            assertEquals(history, activities.activitySummaries())
            assertEquals(bests, results.results())
            assertEquals(totalChanges, db.query("SELECT total_changes()"))
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun retentionSnapshotFailsClosedForMissingConsentRowWithoutInventingNewInstallationChoice() =
        storage { db, _, _ ->
            db.execute("DELETE FROM detail_retention_preferences")
            assertFails { SqliteRecordingDetailReceipts(db).snapshot() }
            assertTrue(db.query("SELECT * FROM detail_retention_preferences").isEmpty())
        }

    private fun <T> storage(block: (SqlDatabase, SqliteActivities, SqliteResults) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(clock.toString(), "test-install")
            }
            return block(db, SqliteActivities(db) { clock }, SqliteResults(db))
        } finally {
            db.close()
        }
    }

    protected fun activity(
        id: String = "actual",
        provider: String = "manual",
        start: Instant = StoredInstant.parse("2026-11-15T09:00:00.123456789Z"),
        sampled: Boolean = true,
    ): ActivityRecord =
        ActivityRecord(
            id,
            SourceIdentity(provider, "record-$id", "local"),
            start,
            Instants.ofEpochSecond(start.getEpochSecond() + 600, start.getNano().toLong()),
            Offsets.ofTotalSeconds(0),
            null,
            3000.0,
            Durations.ofSeconds(600),
            null,
            null,
            null,
            null,
            6.0,
            "provider note",
            if (sampled)
                (0..60).map {
                    DistanceSample(
                        Durations.ofSeconds(it * 10L),
                        it * 50.0,
                        null,
                        null,
                        null,
                        false,
                    )
                }
            else emptyList(),
            emptyList(),
            null,
            null,
        )

    private fun shoe(id: String = "trainer") =
        Shoe(id, "Brand", "Daily", null, 0.0, false, null, true)

    private fun manualResult(
        id: String = "manual-result",
        distance: PerformanceDistance = PerformanceDistance.FIVE_K,
    ) =
        RecordedResult(
            id,
            distance,
            Durations.ofMinutes(25),
            today,
            "Local race",
            ResultSource.OFFICIAL_CHIP,
            null,
            "manual evidence",
        )

    private fun stage(
        db: SqlDatabase,
        activity: ActivityRecord,
        payloads: List<String>,
        state: String = "FINISHED",
    ) {
        db.execute(
            "INSERT INTO recording_sessions VALUES(?,?,?,?,?,?)",
            listOf(
                SqlValue.Text("session-${activity.id()}"),
                SqlValue.Text(state),
                SqlValue.Text(activity.start().toString()),
                SqlValue.Integer(1000),
                SqlValue.Integer(601000),
                SqlValue.Text(activity.id()),
            ),
        )
        payloads.forEachIndexed { index, payload ->
            db.execute(
                "INSERT INTO recording_points VALUES(?,?,?)",
                listOf(
                    SqlValue.Text("session-${activity.id()}"),
                    SqlValue.Integer(index * 2L),
                    SqlValue.Text(payload),
                ),
            )
        }
    }

    private fun point(activity: ActivityRecord, index: Int): Point =
        Point(
            Fix(
                Instants.ofEpochSecond(
                    activity.start().getEpochSecond() + index,
                    activity.start().getNano().toLong(),
                ),
                1000L + index * 1000,
                51.0 + index * .00001,
                -.1,
                5.0,
                null,
                null,
                null,
            ),
            index * 5.0,
            index * 1000L,
            if (index >= 250) 1 else 0,
            index == 250,
            null,
            null,
        )

    @Test
    fun manualUpdatesAndAnnotationsPreserveDefaultAndActualEvidence(): Unit =
        storage { db, activities, results ->
            SqliteShoes(db).saveShoe(shoe())
            val actual = activity()
            activities.upsertManualActivity(actual)
            assertEquals("trainer", activities.activityDetail(actual.id())!!.shoeId())
            assertTrue(
                results.results().any {
                    it.source() == ResultSource.ROLLING && it.distance().code() == "1k"
                }
            )
            activities.saveActivityNotes(actual.id(), "explicit note", 7.5)
            activities.assignShoe(actual.id(), null)
            val annotated = activities.activityDetail(actual.id())!!
            assertEquals("explicit note", annotated.notes())
            assertEquals(7.5, annotated.rpe())
            assertNull(annotated.shoeId())
            assertEquals(actual.samples(), annotated.samples())
            activities.saveActivityNotes(actual.id(), null, null)
            assertEquals("", activities.activityDetail(actual.id())!!.notes())
            assertNull(activities.activityDetail(actual.id())!!.rpe())
            assertEquals(
                "provider note",
                ActivityJson.decode(
                        db.query("SELECT payload FROM activities").single().single().sqlText()
                    )
                    .notes(),
            )
        }

    @Test
    fun importsRetainUserOverridesAndOnlyChooseDefaultOnFirstObservation(): Unit =
        storage { db, activities, _ ->
            SqliteShoes(db).saveShoe(shoe())
            val imported = activity(provider = "health-connect")
            activities.applyImport("health-connect", listOf(imported), emptyList(), "one")
            assertEquals("trainer", activities.activityDetail(imported.id())!!.shoeId())
            assertNull(activities.activityDetail(imported.id())!!.rpe())
            activities.saveActivityNotes(imported.id(), "athlete note", 8.0)
            SqliteShoes(db).saveShoe(shoe("new-default"))
            activities.applyImport(
                "health-connect",
                listOf(imported.copy(notes = "provider changed", rpe = 1.0)),
                emptyList(),
                "two",
            )
            val actual = activities.activityDetail(imported.id())!!
            assertEquals("athlete note", actual.notes())
            assertEquals(8.0, actual.rpe())
            assertEquals("trainer", actual.shoeId())
            assertEquals("two", activities.importToken("health-connect"))
            assertEquals(
                clock.toString(),
                db.query("SELECT updated_at FROM import_checkpoints").single().single().sqlText(),
            )
        }

    @Test
    fun importFailureRollsBackDeletionsPbsAnnotationsAndCheckpoint(): Unit =
        storage { db, activities, results ->
            val original = activity(provider = "health-connect")
            activities.applyImport(
                "health-connect",
                listOf(original),
                emptyList(),
                "retained-token",
            )
            activities.saveActivityNotes(original.id(), "retained note", 7.0)
            val oldResults = results.results()
            db.execute(
                "CREATE TRIGGER reject_checkpoint BEFORE INSERT ON import_checkpoints BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails {
                activities.applyImport(
                    "health-connect",
                    listOf(activity("new", "health-connect")),
                    listOf(original.source().recordId()),
                    "bad-token",
                )
            }
            assertEquals(listOf(original.id()), activities.activities().map { it.id() })
            assertEquals("retained note", activities.activityDetail(original.id())!!.notes())
            assertEquals(oldResults, results.results())
            assertEquals("retained-token", activities.importToken("health-connect"))
            assertTrue(
                db.query("SELECT * FROM activity_annotations WHERE record_id='record-new'")
                    .isEmpty()
            )
        }

    @Test
    fun sourceIdentityConflictsAndInvalidAuthorityNeverReplaceRecords(): Unit =
        storage { _, activities, results ->
            val original = activity()
            activities.upsertManualActivity(original)
            val prior = results.results()
            assertFailsWith<IllegalArgumentException> {
                activities.upsertManualActivity(
                    original.copy(source = SourceIdentity("manual", "different", null))
                )
            }
            assertFailsWith<IllegalArgumentException> {
                activities.upsertManualActivity(original.copy(id = "collision"))
            }
            assertFailsWith<IllegalArgumentException> {
                activities.upsertManualActivity(activity("provider", "health-connect"))
            }
            assertFailsWith<IllegalArgumentException> {
                activities.applyImport("manual", emptyList(), emptyList(), null)
            }
            assertFailsWith<IllegalArgumentException> {
                activities.applyImport(
                    "healthkit",
                    listOf(activity("provider", "health-connect")),
                    emptyList(),
                    null,
                )
            }
            assertEquals(listOf(original), activities.activities())
            assertEquals(prior, results.results())
        }

    @Test
    fun sourceDeletionKeepsOverridesButExplicitDeletionRemovesThem(): Unit =
        storage { db, activities, _ ->
            val original = activity(provider = "healthkit")
            activities.applyImport("healthkit", listOf(original), emptyList(), "one")
            activities.saveActivityNotes(original.id(), "kept for source", 5.0)
            activities.applyImport(
                "healthkit",
                emptyList(),
                listOf(original.source().recordId()),
                null,
            )
            assertTrue(activities.activities().isEmpty())
            assertNull(activities.importToken("healthkit"))
            assertEquals(1, db.query("SELECT * FROM activity_annotations").size)
            activities.applyImport("healthkit", listOf(original), emptyList(), "three")
            assertEquals("kept for source", activities.activityDetail(original.id())!!.notes())
            activities.deleteActivity(original.id())
            assertTrue(db.query("SELECT * FROM activity_annotations").isEmpty())
        }

    @Test
    fun checkpointClockFailureCannotPartiallyImport(): Unit = storage { db, _, _ ->
        val activities = SqliteActivities(db) { error("Clock unavailable") }
        assertFails {
            activities.applyImport(
                "healthkit",
                listOf(activity(provider = "healthkit")),
                emptyList(),
                "bad",
            )
        }
        assertTrue(db.query("SELECT * FROM activities").isEmpty())
        assertTrue(db.query("SELECT * FROM import_checkpoints").isEmpty())
        assertTrue(db.query("SELECT * FROM best_efforts").isEmpty())
        assertTrue(db.query("SELECT * FROM activity_annotations").isEmpty())
    }

    @Test
    fun historyCursorUsesStoredMillisecondsAndStableIdentityTies(): Unit =
        storage { _, activities, _ ->
            val base = StoredInstant.parse("1969-12-31T23:59:59.999000001Z")
            for (id in listOf("cc", "aa", "bb")) activities.upsertManualActivity(
                activity(id, start = base, sampled = false)
            )
            val first = activities.activitySummaryPage(null, null, 2)
            assertEquals(listOf("aa", "bb"), first.map { it.id() })
            val next = activities.activitySummaryPage(first.last().start(), first.last().id(), 2)
            assertEquals(listOf("cc"), next.map { it.id() })
            assertFailsWith<IllegalArgumentException> {
                activities.activitySummaryPage(base, null, 1)
            }
            assertFailsWith<IllegalArgumentException> {
                activities.activitySummaryPage(null, "id", 1)
            }
            assertFailsWith<IllegalArgumentException> {
                activities.activitySummaryPage(null, null, 101)
            }
        }

    @Test
    fun nativeSummariesNeverDecodeRawPointsAndHydrationPreservesTotalsAndGaps(): Unit =
        storage { db, activities, _ ->
            val original = activity(provider = "rdd")
            activities.upsertManualActivity(original)
            val points = (0..510).map { point(original, it) }
            stage(db, original, points.map { RecordingPointJson.encode(it) })
            assertTrue(activities.activitySummaries().single().samples().isEmpty())
            val detail = activities.activityDetail(original.id())!!
            assertEquals(511, detail.samples().size)
            assertTrue(detail.samples()[250].discontinuity())
            assertNull(detail.samples().first().heartRate())
            assertEquals(original.distanceMetres(), detail.distanceMetres())
            assertEquals(original.movingTime(), detail.movingTime())
            assertEquals(points.last().cumulativeMetres(), detail.samples().last().distanceMetres())
            assertEquals(Durations.ofMillis(510000), detail.samples().last().elapsed())
            db.execute("UPDATE recording_points SET payload='corrupt' WHERE sequence=0")
            assertEquals(original.id(), activities.activitySummaryPage(null, null, 1).single().id())
            assertFails { activities.activityDetail(original.id()) }
        }

    @Test
    fun missingOrRecoverableRawEvidenceIsNotInvented(): Unit = storage { db, activities, _ ->
        val original = activity(provider = "gpx")
        activities.upsertManualActivity(original)
        assertTrue(activities.activityDetail(original.id())!!.samples().isEmpty())
        stage(db, original, listOf("corrupt"), "RECOVERABLE")
        assertTrue(activities.activityDetail(original.id())!!.samples().isEmpty())
        assertEquals(
            original.distanceMetres(),
            activities.activityDetail(original.id())!!.distanceMetres(),
        )
    }

    @Test
    fun coachingOnlyHydratesTwelveRecentRecordings(): Unit = storage { db, activities, _ ->
        val base = StoredInstant.parse("2026-11-15T09:00:00Z")
        for (index in 0..12) {
            val start = Instants.ofEpochSecond(base.getEpochSecond() - index * 3600)
            val value = activity("run-$index", "rdd", start, false)
            activities.upsertManualActivity(value)
            stage(
                db,
                value,
                if (index == 12) listOf("corrupt")
                else listOf(RecordingPointJson.encode(point(value, 0))),
            )
        }
        val old = activity("old", "gpx", StoredInstant.parse("2026-10-01T09:00:00Z"), false)
        activities.upsertManualActivity(old)
        stage(db, old, listOf("corrupt"))
        val actual = activities.coachingActivities(today)
        assertEquals(14, actual.size)
        assertEquals(12, actual.count { it.samples().isNotEmpty() })
        assertTrue(actual.first { it.id() == "run-12" }.samples().isEmpty())
        assertTrue(actual.first { it.id() == "old" }.samples().isEmpty())
    }

    @Test
    fun coachingBudgetDoesNotHydrateOversizedRawStream(): Unit = storage { db, activities, _ ->
        val value = activity(provider = "rdd", sampled = false)
        activities.upsertManualActivity(value)
        stage(db, value, emptyList())
        db.execute(
            "WITH RECURSIVE points(n) AS (VALUES(0) UNION ALL SELECT n+1 FROM points WHERE n<60000) INSERT INTO recording_points SELECT 'session-actual',n,'corrupt' FROM points"
        )
        val actual = activities.coachingActivities(today).single()
        assertTrue(actual.samples().isEmpty())
        assertEquals(value.distanceMetres(), actual.distanceMetres())
    }

    @Test
    fun explicitDeletionRemovesRawAndDerivedButKeepsManualResults(): Unit =
        storage { db, activities, results ->
            val value = activity(provider = "rdd")
            activities.upsertManualActivity(value)
            stage(db, value, listOf(RecordingPointJson.encode(point(value, 0))))
            db.execute("INSERT INTO recording_events VALUES('session-actual',0,'retained event')")
            results.saveResult(manualResult())
            assertTrue(results.results().size > 1)
            activities.deleteActivity(value.id())
            for (table in
                listOf(
                    "activities",
                    "recording_sessions",
                    "recording_points",
                    "recording_events",
                    "activity_annotations",
                    "best_efforts",
                )) assertTrue(db.query("SELECT * FROM $table").isEmpty(), table)
            assertEquals(listOf(manualResult()), results.results())
        }

    @Test
    fun manualResultRulesProtectDerivedIdentityAndDistanceCatalog(): Unit =
        storage { _, activities, results ->
            activities.upsertManualActivity(activity())
            val rolling = results.results().first()
            assertFailsWith<IllegalArgumentException> { results.saveResult(rolling) }
            assertFailsWith<IllegalArgumentException> {
                results.saveResult(manualResult(rolling.id()))
            }
            assertFailsWith<IllegalArgumentException> {
                results.saveResult(
                    manualResult(distance = PerformanceDistance("5k", 4000.0, "Wrong"))
                )
            }
            val custom = manualResult("custom", PerformanceDistance("track", 400.0, "Track"))
            results.saveResult(custom)
            assertFailsWith<IllegalArgumentException> {
                results.saveResult(
                    manualResult("conflict", PerformanceDistance("track", 500.0, "Other"))
                )
            }
            results.saveResult(
                custom.copy(distance = PerformanceDistance("track", 500.0, "Updated"))
            )
            results.deleteResult(rolling.id())
            assertTrue(results.results().any { it.id() == rolling.id() })
        }

    @Test
    fun allActivityAndResultChangesParticipateInOuterRollback(): Unit =
        storage { db, activities, results ->
            assertFails {
                db.transaction {
                    SqliteShoes(db).saveShoe(shoe())
                    activities.upsertManualActivity(activity())
                    results.saveResult(manualResult())
                    activities.saveActivityNotes("actual", "temporary", 8.0)
                    error("Reject the batch")
                }
            }
            for (table in
                listOf(
                    "activities",
                    "activity_annotations",
                    "best_efforts",
                    "manual_results",
                    "shoes",
                )) assertTrue(db.query("SELECT * FROM $table").isEmpty(), table)
        }

    @Test
    fun providerRefreshReplacesDerivedEffortsButNeverManualResults(): Unit =
        storage { _, activities, results ->
            val original = activity(provider = "health-connect")
            activities.applyImport("health-connect", listOf(original), emptyList(), "one")
            results.saveResult(manualResult())
            val old =
                results.results().first {
                    it.distance().code() == "1k" && it.source() == ResultSource.ROLLING
                }
            val faster =
                original.copy(
                    movingTime = Durations.ofSeconds(300),
                    samples =
                        (0..60).map {
                            DistanceSample(
                                Durations.ofSeconds(it * 5L),
                                it * 50.0,
                                null,
                                null,
                                null,
                                false,
                            )
                        },
                )
            activities.applyImport("health-connect", listOf(faster), emptyList(), "two")
            val new = results.results().first { it.id() == old.id() }
            assertTrue(new.duration() < old.duration())
            assertEquals(1, results.results().count { it.id() == old.id() })
            assertTrue(results.results().contains(manualResult()))
        }

    @Test
    fun preservedEffortsHaveStableDedupAndSourceDeletionSemantics(): Unit =
        storage { db, activities, results ->
            val activity = activity(provider = "healthkit")
            activities.applyImport("healthkit", listOf(activity), emptyList(), "one")
            val rolling = results.results().first()
            db.execute(
                "INSERT INTO preserved_best_efforts(id,activity_id,distance_code,provider,record_id,origin_package,payload) VALUES(?,?,?,?,?,?,?)",
                listOf(
                    SqlValue.Text(rolling.id()),
                    SqlValue.Text(activity.id()),
                    SqlValue.Text(rolling.distance().code()),
                    SqlValue.Text("healthkit"),
                    SqlValue.Text(activity.source().recordId()),
                    SqlValue.Null,
                    SqlValue.Text(ResultJson.encode(rolling)),
                ),
            )
            assertEquals(1, results.results().count { it.id() == rolling.id() })
            assertFailsWith<IllegalArgumentException> {
                results.saveResult(manualResult(rolling.id()))
            }
            activities.applyImport(
                "healthkit",
                emptyList(),
                listOf(activity.source().recordId()),
                "deleted",
            )
            assertTrue(results.results().isEmpty())
            assertTrue(db.query("SELECT * FROM preserved_best_efforts").isEmpty())
        }

    @Test
    fun removedRawDetailKeepsTruthfulReceiptSummaryAndPermanentPbs(): Unit =
        storage { db, activities, results ->
            val activity =
                activity(provider = "rdd", start = StoredInstant.parse("2019-01-01T09:00:00Z"))
                    .copy(source = SourceIdentity("rdd", "session-actual", null))
            activities.upsertManualActivity(activity)
            val originalResults = results.results()
            stage(db, activity, emptyList())
            db.execute(
                "INSERT INTO recording_detail_removals VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                listOf(
                    SqlValue.Text("session-actual"),
                    SqlValue.Text(activity.id()),
                    SqlValue.Text("rdd"),
                    SqlValue.Text("session-actual"),
                    SqlValue.Text(activity.end().toString()),
                    SqlValue.Text(clock.toString()),
                    SqlValue.Text("LAST_24_MONTHS"),
                    SqlValue.Text(activity.start().toString()),
                    SqlValue.Integer(1000),
                    SqlValue.Integer(601000),
                    SqlValue.Integer(61),
                    SqlValue.Integer(2),
                    SqlValue.Text("a".repeat(64)),
                    SqlValue.Text("b".repeat(64)),
                ),
            )
            val receipts = SqliteRecordingDetailReceipts(db)
            val receipt = assertNotNull(receipts.removal("session-actual"))
            assertEquals(61L, receipt.points())
            assertEquals(2L, receipt.events())
            assertEquals(clock, receipt.removedAt())
            val historical = assertNotNull(activities.activityDetail(activity.id()))
            assertEquals(activity.distanceMetres(), historical.distanceMetres())
            assertEquals(activity.movingTime(), historical.movingTime())
            assertTrue(historical.samples().isEmpty())
            assertEquals(originalResults, results.results())
            assertNull(receipts.removal("not-known"))
            activities.deleteActivity(activity.id())
            assertNull(receipts.removal("session-actual"))
        }

    @Test
    fun absentAndEmptyBaselinesStayUnknownAndNeverBecomeRecordedRuns(): Unit =
        storage { db, activities, results ->
            val repository = SqliteBaselines(db) { clock }
            assertEquals(
                org.racedaydreams.domain.baseline.ManualBaselineInput.empty(),
                repository.savedManualBaseline(),
            )
            assertNull(repository.manualBaselineRecordedAt())
            assertTrue(db.query("SELECT * FROM baseline_state").isEmpty())
            repository.saveManualBaseline(
                org.racedaydreams.domain.baseline.ManualBaselineInput.empty()
            )
            assertEquals(clock, repository.manualBaselineRecordedAt())
            assertEquals(
                SqlValue.Integer(1),
                db.query("SELECT reviewed FROM baseline_state").single().single(),
            )
            assertTrue(activities.activities().isEmpty())
            assertTrue(results.results().isEmpty())
        }

    @Test
    fun baselineAnswersAndOriginalObservationTimeSurviveReadWithoutClock(): Unit =
        storage { db, _, _ ->
            val input =
                org.racedaydreams.domain.baseline.ManualBaselineInput(
                    0.0,
                    3.0,
                    12.0,
                    Durations.ofMinutes(25),
                    null,
                    null,
                    360.0,
                    0,
                )
            SqliteBaselines(db) { clock }.saveManualBaseline(input)
            val reader =
                SqliteBaselines(db) { error("Reads must not invent a new observation time") }
            assertEquals(input, reader.savedManualBaseline())
            assertEquals(clock, reader.manualBaselineRecordedAt())
            reader.putBaseline(input, null)
            assertEquals(input, reader.savedManualBaseline())
            assertNull(reader.manualBaselineRecordedAt())
        }

    @Test
    fun completedAssessmentAndActualActivityAreRetainedTogether(): Unit =
        storage { db, activities, results ->
            val actual = activity(provider = "assessment")
            val assessment =
                org.racedaydreams.domain.baseline.AssessmentResult(
                    "assessment",
                    org.racedaydreams.domain.baseline.BaselineAssessment(
                        3,
                        org.racedaydreams.domain.baseline.AssessmentMode.CONTROLLED,
                    ),
                    Durations.ofSeconds(500, 123456789),
                    null,
                    actual.end(),
                    actual.id(),
                )
            val repository =
                SqliteBaselines(db) { error("Actual completion time is already recorded") }
            repository.saveAssessment(actual, assessment)
            assertEquals(actual, activities.activityDetail(actual.id()))
            assertEquals(listOf(assessment), repository.assessments())
            assertTrue(results.results().isNotEmpty())
            assertNull(repository.manualBaselineRecordedAt())
        }

    @Test
    fun rejectedAssessmentCannotLeaveAnActivityOrNewPersonalBest(): Unit =
        storage { db, activities, results ->
            val original = activity()
            activities.upsertManualActivity(original)
            val before = results.results()
            val actual = activity("new", "assessment")
            val assessment =
                org.racedaydreams.domain.baseline.AssessmentResult(
                    "blocked",
                    org.racedaydreams.domain.baseline.BaselineAssessment(
                        3,
                        org.racedaydreams.domain.baseline.AssessmentMode.BEST_EFFORT,
                    ),
                    Durations.ofSeconds(500),
                    7.5,
                    actual.end(),
                    actual.id(),
                )
            val repository = SqliteBaselines(db) { clock }
            db.execute(
                "CREATE TRIGGER reject_assessment BEFORE INSERT ON assessment_results BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails { repository.saveAssessment(actual, assessment) }
            assertEquals(listOf(original), activities.activities())
            assertEquals(before, results.results())
            assertTrue(repository.assessments().isEmpty())
            assertFailsWith<IllegalArgumentException> {
                repository.saveAssessment(
                    actual.copy(source = SourceIdentity("health-connect", "imported", null)),
                    assessment,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                repository.saveAssessment(actual, assessment.copy(activityId = "different"))
            }
        }

    @Test
    fun baselineCodecsKeepNullZeroWholeCountsAndAssessmentEffortDistinct(): Unit =
        storage { _, _, _ ->
            val input =
                org.racedaydreams.domain.baseline.ManualBaselineInput(
                    0.0,
                    null,
                    null,
                    null,
                    Durations.ofMinutes(50),
                    null,
                    null,
                    0,
                )
            val encoded = BaselineJson.encode(input)
            assertEquals(input, BaselineJson.decode(encoded))
            assertEquals(
                input,
                BaselineJson.decode(
                    encoded.replace("\"consistentWeeks\":0", "\"consistentWeeks\":0.0")
                ),
            )
            assertFailsWith<IllegalArgumentException> {
                BaselineJson.decode(
                    encoded.replace("\"consistentWeeks\":0", "\"consistentWeeks\":0.5")
                )
            }
            assertFailsWith<IllegalArgumentException> {
                BaselineJson.decode(encoded.replace("PT50M", "PT-1S"))
            }
            val assessment =
                org.racedaydreams.domain.baseline.AssessmentResult(
                    "assessment",
                    org.racedaydreams.domain.baseline.BaselineAssessment(
                        5,
                        org.racedaydreams.domain.baseline.AssessmentMode.BEST_EFFORT,
                    ),
                    Durations.ofSeconds(1500, 123456789),
                    7.5,
                    clock,
                    null,
                )
            assertEquals(
                assessment,
                BaselineJson.decodeAssessment(BaselineJson.encodeAssessment(assessment)),
            )
        }
}
