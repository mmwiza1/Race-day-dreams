package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.achievements.TrainingMilestones.Award
import org.racedaydreams.domain.achievements.TrainingMilestones.Kind
import org.racedaydreams.domain.achievements.TrainingMilestones.Proof
import org.racedaydreams.domain.achievements.TrainingMilestones.Source
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.recording.RunMetricsEngine.Fix
import org.racedaydreams.domain.recording.RunMetricsEngine.Point
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.storage.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

abstract class ApplicationBackupRestoreRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = GymEngineArchiveFixtures.at
    private val day = CalendarDates.of(2026, 11, 16)
    private var sequence = 0

    private fun replacement(db: SqlDatabase) =
        SqliteApplicationBackupRestore(db) { "restored-dataset-${++sequence}" }

    private fun storage(block: (SqlDatabase, SqlDatabase) -> Unit) {
        val source = database()
        val target = database()
        try {
            for (db in listOf(source, target)) ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "application-restore-test")
            }
            block(source, target)
        } finally {
            source.close()
            target.close()
        }
    }

    private fun bytes(db: SqlDatabase) = SqliteApplicationBackup(db).encode(at)

    private fun snapshot(db: SqlDatabase) =
        UserBackupDataJson.element(SqliteApplicationBackup(db).snapshot())

    private fun epoch(db: SqlDatabase) =
        db.query("SELECT epoch FROM detail_retention_state").single().single().sqlText()

    private fun activity(
        id: String = "manual",
        provider: String = "manual",
        samples: List<DistanceSample> = emptyList(),
    ) =
        ActivityRecord(
            id,
            SourceIdentity(provider, if (provider == "rdd") "run" else id, null),
            at,
            Instants.ofEpochSecond(at.getEpochSecond() + 60, at.getNano().toLong()),
            null,
            null,
            100.0,
            Durations.ofSeconds(60),
            null,
            null,
            null,
            null,
            null,
            "Original annotation",
            samples,
            emptyList(),
            null,
            null,
        )

    private fun gym(db: SqlDatabase, unfinished: Boolean = false) {
        val e =
            GymExecutionEngine.start(
                "gym",
                GymExecution.Origin.FREE,
                null,
                null,
                Offsets.ofTotalSeconds(0),
                GymEngineArchiveFixtures.tick(0),
            )
        val repo =
            SqliteGymExecutions(
                db,
                { GymEngineArchiveFixtures.tick(10000).wallTime() },
                PortableLegacyStrengthFingerprint,
            )
        val saved = repo.start(e.snapshot())
        e.addExercises(
            listOf(GymWorkoutDefinition.Slot("bench", GymEngineArchiveFixtures.bench, null, null)),
            GymEngineArchiveFixtures.tick(10),
        )
        e.saveSet(
            "bench",
            StrengthSession.SetEntry("working", 6, 50.0, 7.5, false),
            GymEngineArchiveFixtures.tick(1000),
        )
        SqliteGymTools(db).change("gym") {
            GymRestTimer.start(
                "gym",
                "retained-trigger",
                90,
                GymRestTimer.Clock("original-boot", 1000),
            )
        }
        if (unfinished) repo.save(e.snapshot(), saved.revision())
        else
            repo.finish(
                e.finish(GymEngineArchiveFixtures.tick(2000), null, "Performed actual", true),
                saved.revision(),
            )
    }

    private fun full(db: SqlDatabase) {
        SqliteAthleteProfile(db).save(CoachingArchiveFixtures.profile())
        SqliteRunningCoaching(db, { at }, { "unused" })
            .saveCurrent(CoachingArchiveFixtures.version())
        SqliteSettings(db).putSetting("primary.focus", "hybrid")
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.BEGINNER,
                setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                2,
                45,
                ExerciseMetadata.Equipment.entries.toSet(),
            )
        val p =
            StrengthProgrammeGenerator()
                .generate(
                    StrengthProgrammeGenerator.Request(
                        "authored",
                        at,
                        "Retained programme",
                        settings,
                        day.plusDays(1),
                        2,
                        "full-body-2",
                        PrimaryFocus.STRENGTH,
                        emptySet(),
                        null,
                        emptySet(),
                        emptySet(),
                        false,
                    )
                )
        SqliteStrengthProgramming(db, { at }, { day })
            .activate(p, null, "original-acceptance", emptyList())
        SqliteStrengthDrafts(db)
            .saveDraft(
                StrengthProgrammeDraft(
                    "future",
                    3,
                    p.id(),
                    1,
                    "",
                    settings,
                    p.start(),
                    emptyList(),
                    StrengthProgramme.Origin.CUSTOM,
                    null,
                ),
                0,
            )
        gym(db)
        SqliteShoes(db).saveShoe(Shoe("shoe", "Brand", "Model", null, 42.0, false, null, true))
        SqliteActivities(db) { at }.upsertManualActivity(activity())
        SqliteStrengthChoices(db).save(SqliteStrengthChoices.Choice("bench-press", true, false))
        SqliteExerciseExclusions(db)
            .exclude(
                org.racedaydreams.domain.strength.ExerciseExclusionRepository.Exclusion(
                    "bench-press",
                    org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason.PREFERENCE,
                    at,
                )
            )
        SqliteBodyHeights(db)
            .saveHeight(BodyHeight("height", 1.83, at, null, "manual", "height", null, null))
        SqliteTrainingMilestones(db)
            .recognize(
                listOf(Award(Kind.FIRST_GYM, at, listOf(Proof(Source.GYM, "gym", at, null, null)))),
                Instants.ofEpochSecond(at.getEpochSecond() + 60),
            )
        SqliteTrainingMilestones(db).celebrations(false)
    }

    private fun raw(db: SqlDatabase): RemovedRecordingDetail {
        val a = activity("recorded-run", "rdd")
        SqliteActivities(db) { at }.upsertManualActivity(a)
        db.execute(
            "INSERT INTO recording_sessions VALUES('run','FINISHED',?,0,60000,'recorded-run')",
            listOf(SqlValue.Text(at.toString())),
        )
        val points =
            listOf(
                Point(Fix(at, 0, 51.5, -.12, 4.0, null, null, null), 0.0, 0, 0, false, null, null),
                Point(
                    Fix(a.end(), 60000, 51.5008, -.12, 4.0, null, null, null),
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
        val pointHash = SqliteRecordingEvidence(db).evidence("recording_points", "run").sha256
        val eventHash = SqliteRecordingEvidence(db).evidence("recording_events", "run").sha256
        return RemovedRecordingDetail(
            "run",
            a.id(),
            "rdd",
            "run",
            a.end(),
            Instants.ofEpochSecond(at.getEpochSecond() + 120),
            DetailRetention.Policy.LAST_24_MONTHS,
            at,
            0,
            60000,
            2,
            0,
            pointHash,
            eventHash,
        )
    }

    private fun envelope(data: JsonObject, version: Int = 13) =
        buildJsonObject {
                put("format", BackupEnvelopeJson.FORMAT)
                put("version", version)
                put("createdAt", at.toString())
                put("data", data)
            }
            .toString()
            .encodeToByteArray()

    @Test
    fun allApplicationSectionsReplaceTogetherAndCurrentBackupRoundTripsWithoutNewDecisions() =
        storage { source, target ->
            full(source)
            SqliteSettings(target).putSetting("old", "replace only after full validation")
            SqliteActivities(target) { at }.upsertManualActivity(activity("old-manual"))
            val expected = snapshot(source)
            val oldEpoch = epoch(target)
            replacement(target).replace(bytes(source))
            assertEquals(expected, snapshot(target))
            assertNotEquals(oldEpoch, epoch(target))
            assertEquals(
                "original-acceptance",
                SqliteStrengthProgrammingRecords(target).decisions().single().id(),
            )
            assertEquals(3, SqliteStrengthProgrammingRecords(target).draft("future")!!.revision())
            assertEquals(
                1,
                SqliteGymExecutionRecords(target).load("gym")!!.execution().actualSets().size,
            )
            assertEquals(1, SqliteExerciseExclusions(target).exclusions().size)
            assertTrue(target.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun reviewedReplacementCannotCrossOwnersOrOverlapAnExport() = storage { source, target ->
        val owner = LocalBackupOperations(target, { at }, { "restored-dataset-${++sequence}" })
        val other = LocalBackupOperations(target, { at }, { error("Unexpected replacement") })
        val reviewed = owner.reviewApp(bytes(source))
        SqliteSettings(target).putSetting("old", "retain until confirmation")
        assertEquals(13, reviewed.version)
        assertEquals("retain until confirmation", SqliteSettings(target).getSetting("old"))
        assertFailsWith<IllegalArgumentException> { other.restoreReviewed(reviewed) }
        owner.writeApp {
            assertFailsWith<IllegalStateException> { owner.restoreReviewed(reviewed) }
        }
        assertEquals("retain until confirmation", SqliteSettings(target).getSetting("old"))
        owner.restoreReviewed(reviewed)
        assertEquals(snapshot(source), snapshot(target))
    }

    @Test
    fun allSupportedOldRootsRestoreTheirGenuineMissingSectionsAndUnknownConsentSafely() {
        for (version in 1..13) storage { source, target ->
            val current = snapshot(source)
            val minima =
                mapOf(
                    "coaching" to 2,
                    "development" to 3,
                    "strengthState" to 4,
                    "gymProgramming" to 4,
                    "support" to 6,
                    "release08" to 7,
                    "strengthEngine" to 8,
                    "strengthProgramming" to 9,
                    "gymTools" to 10,
                    "achievements" to 11,
                    "bodyMeasurements" to 12,
                    "detailRetention" to 13,
                )
            val old =
                JsonObject(
                    current.filterKeys { (minima[it] ?: 1) <= version } +
                        ("settings" to buildJsonObject { put("provider.preference", "GOOGLE_FIT") })
                )
            replacement(target).replace(envelope(old, version))
            assertEquals("HEALTH_CONNECT", SqliteSettings(target).getSetting("provider.preference"))
            assertNull(SqliteAthleteProfile(target).load())
            assertTrue(SqliteActivities(target) { at }.activities().isEmpty())
            assertTrue(SqliteTrainingMilestones(target).awards().isEmpty())
            assertFalse(SqliteBodyHeights(target).preferences().proteinEnabled())
            if (version < 13)
                assertEquals(
                    DetailRetention.Preferences.existing(),
                    SqliteRetentionPreferences(target).load(),
                )
            else
                assertEquals(
                    SqliteRetentionPreferences(source).load(),
                    SqliteRetentionPreferences(target).load(),
                )
        }
    }

    @Test
    fun corruptFutureAndIncompleteCurrentFilesRejectBeforeAnyDatabaseAccess() =
        storage { source, target ->
            val data = snapshot(source)
            val bad =
                listOf(
                    "{".encodeToByteArray(),
                    envelope(data, 14),
                    envelope(JsonObject(data - "gymTools")),
                    envelope(
                        JsonObject(
                            data + ("strengthEngine" to buildJsonObject { put("version", 99) })
                        )
                    ),
                )
            var touches = 0
            val watched =
                object : SqlDatabase by target {
                    override fun <T> transaction(block: () -> T): T {
                        touches++
                        return target.transaction(block)
                    }

                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        touches++
                        return target.query(sql, arguments)
                    }

                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        touches++
                        target.execute(sql, arguments)
                    }
                }
            for (file in bad) {
                assertFails { replacement(watched).replace(file) }
                assertEquals(0, touches)
            }
        }

    @Test
    fun activePausedAndRecoverableRunOrGymStatesCannotBeReplaced() = storage { source, target ->
        val file = bytes(source)
        target.execute(
            "INSERT INTO recording_sessions VALUES('run','ACTIVE',?,0,1000,NULL)",
            listOf(SqlValue.Text(at.toString())),
        )
        for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) {
            target.execute("UPDATE recording_sessions SET state=?", listOf(SqlValue.Text(state)))
            val before = target.query("SELECT total_changes()")
            assertFails { replacement(target).replace(file) }
            assertEquals(before, target.query("SELECT total_changes()"))
        }
        target.execute("DELETE FROM recording_sessions")
        gym(target, true)
        for (state in listOf("ACTIVE", "PAUSED", "RECOVERY_REQUIRED")) {
            target.execute("UPDATE strength_executions SET state=?", listOf(SqlValue.Text(state)))
            val before = target.query("SELECT total_changes()")
            assertFails { replacement(target).replace(file) }
            assertEquals(before, target.query("SELECT total_changes()"))
        }
    }

    @Test
    fun ownedRunOmissionIdentityAndMeasuredConflictsRejectBeforeClearingAnything() =
        storage { source, target ->
            raw(target)
            val before = snapshot(target)
            val changes = target.query("SELECT total_changes()")
            assertFails { replacement(target).replace(bytes(source)) }
            val a = activity("recorded-run", "rdd")
            for (conflict in
                listOf(
                    a.copy(distanceMetres = 101.0),
                    a.copy(distanceMetres = 100.00000000000001),
                    a.copy(averageHeartRate = 140.0),
                    a.copy(source = SourceIdentity("gpx", "another", null)),
                )) {
                val core = snapshot(source)
                val changed =
                    JsonObject(
                        core +
                            ("activities" to
                                JsonArray(
                                    listOf(Json.parseToJsonElement(ActivityJson.encode(conflict)))
                                ))
                    )
                assertFails { replacement(target).replace(envelope(changed)) }
            }
            assertEquals(changes, target.query("SELECT total_changes()"))
            assertEquals(before, snapshot(target))
            assertEquals(2, target.query("SELECT * FROM recording_points").size)
        }

    @Test
    fun retainedRawCoordinatesCountsAndTotalsSurviveReplacementWhileAnnotationsRemainEditable() =
        storage { source, target ->
            raw(target)
            val retainedPb =
                RecordedResult(
                    "retained-100m",
                    PerformanceDistance("test-100m", 100.0, "100 metres"),
                    Durations.ofSeconds(60),
                    day,
                    "Recorded interval",
                    ResultSource.ROLLING,
                    "recorded-run",
                    null,
                )
            target.execute(
                "INSERT INTO preserved_best_efforts VALUES('retained-100m','recorded-run',?,'rdd','run',NULL,?)",
                listOf(
                    SqlValue.Text(retainedPb.distance().code()),
                    SqlValue.Text(ResultJson.encode(retainedPb)),
                ),
            )
            val file = bytes(target)
            val raw = target.query("SELECT * FROM recording_points ORDER BY sequence")
            val sessions = target.query("SELECT * FROM recording_sessions")
            SqliteActivities(target) { at }.saveActivityNotes("recorded-run", "Later note", 8.5)
            replacement(target).replace(file)
            assertEquals(raw, target.query("SELECT * FROM recording_points ORDER BY sequence"))
            assertEquals(sessions, target.query("SELECT * FROM recording_sessions"))
            val restored = SqliteActivities(target) { at }.activityDetail("recorded-run")!!
            assertEquals(100.0, restored.distanceMetres())
            assertEquals(Durations.ofSeconds(60), restored.movingTime())
            assertEquals(2, restored.samples().size)
            assertEquals("Original annotation", restored.notes())
            assertTrue(target.query("SELECT * FROM best_efforts").isEmpty())
            assertEquals(listOf(retainedPb), SqliteResults(target).results())
        }

    @Test
    fun originalReaderZeroSpellingDoesNotConflictWithEquivalentStoredSummaryMeasurement() =
        storage { _, target ->
            raw(target)
            val recorded =
                ActivityJson.decode(
                        target
                            .query("SELECT payload FROM activities WHERE id='recorded-run'")
                            .single()
                            .single()
                            .sqlText()
                    )
                    .copy(elevationGainMetres = -0.0)
            target.execute(
                "UPDATE activities SET payload=? WHERE id='recorded-run'",
                listOf(SqlValue.Text(ActivityJson.encode(recorded))),
            )
            val root = snapshot(target)
            val rows =
                root.getValue("activities").jsonArray.map {
                    JsonObject(it.jsonObject + ("elevationGainMetres" to JsonPrimitive(0)))
                }
            val before = target.query("SELECT * FROM recording_points ORDER BY sequence")
            replacement(target)
                .replace(envelope(JsonObject(root + ("activities" to JsonArray(rows)))))
            assertEquals(
                0.0,
                SqliteActivities(target) { at }
                    .activityDetail("recorded-run")!!
                    .elevationGainMetres(),
            )
            assertEquals(before, target.query("SELECT * FROM recording_points ORDER BY sequence"))
        }

    @Test
    fun denseLegacyInlineEvidenceIsPreservedInPlaceAndGenuineOldInlineBackupsRestoreIntoEmptyData() =
        storage { source, target ->
            val samples =
                listOf(
                    DistanceSample(Durations.ofSeconds(0), 0.0, null, null, null, false),
                    DistanceSample(Durations.ofSeconds(60), 100.0, null, null, null, false),
                )
            val a = activity("recorded-run", "rdd", samples)
            SqliteActivities(source) { at }.upsertManualActivity(a)
            source.execute(
                "UPDATE activities SET payload=?",
                listOf(SqlValue.Text(ActivityJson.encode(a))),
            )
            val compact = bytes(source)
            replacement(source).replace(compact)
            assertEquals(
                samples,
                SqliteActivities(source) { at }.activityDetail(a.id())!!.samples(),
            )
            val core = SqliteCoreBackup(source).snapshot().data
            val old =
                JsonObject(
                    core +
                        ("activities" to
                            JsonArray(listOf(Json.parseToJsonElement(ActivityJson.encode(a)))))
                )
            replacement(target).replace(envelope(old, 1))
            assertEquals(
                samples,
                SqliteActivities(target) { at }.activityDetail(a.id())!!.samples(),
            )
        }

    @Test
    fun olderBackupCannotEraseKnownRemovalProvenanceAndConflictingReceiptCannotHideAvailableDetail() =
        storage { source, target ->
            val proof = raw(target)
            val older = bytes(target)
            target.execute("DELETE FROM recording_points")
            SqliteRetentionRestore(target) { "removed-fixture" }
                .replace(
                    DetailRetentionArchiveJson.Snapshot(
                        SqliteRetentionPreferences(target).load(),
                        listOf(proof),
                    )
                )
            replacement(target).replace(older)
            assertEquals(proof, SqliteRecordingDetailReceipts(target).removal("run"))
            assertTrue(target.query("SELECT * FROM recording_points").isEmpty())
            val conflicting =
                DetailRetentionArchiveJson.Snapshot(
                    SqliteRetentionPreferences(target).load(),
                    listOf(proof.copy(pointsHash = "ab".repeat(32))),
                )
            val bad =
                JsonObject(
                    snapshot(target) +
                        ("detailRetention" to DetailRetentionArchiveJson.element(conflicting))
                )
            val before = target.query("SELECT total_changes()")
            assertFails { replacement(target).replace(envelope(bad)) }
            assertEquals(before, target.query("SELECT total_changes()"))
            raw(source)
            val sourceBefore = source.query("SELECT total_changes()")
            val availableBad =
                JsonObject(
                    snapshot(source) +
                        ("detailRetention" to DetailRetentionArchiveJson.element(conflicting))
                )
            assertFails { replacement(source).replace(envelope(availableBad)) }
            assertEquals(sourceBefore, source.query("SELECT total_changes()"))
            val good =
                DetailRetentionArchiveJson.Snapshot(
                    SqliteRetentionPreferences(source).load(),
                    listOf(proof),
                )
            replacement(source)
                .replace(
                    envelope(
                        JsonObject(
                            snapshot(source) +
                                ("detailRetention" to DetailRetentionArchiveJson.element(good))
                        )
                    )
                )
            assertNull(SqliteRecordingDetailReceipts(source).removal("run"))
            assertEquals(2, source.query("SELECT * FROM recording_points").size)
        }

    @Test
    fun finalGymWriteFailureRollsBackEverySectionRawEvidenceAndApprovalEpoch() =
        storage { source, target ->
            full(target)
            raw(target)
            val file = bytes(target)
            SqliteSettings(target).putSetting("later", "Retain this if restoration fails")
            val before = snapshot(target)
            val raw = target.query("SELECT * FROM recording_points")
            val oldEpoch = epoch(target)
            val failed =
                object : SqlDatabase by target {
                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        if (sql.startsWith("INSERT OR REPLACE INTO gym_rest_preferences"))
                            error("Simulated final restore failure")
                        target.execute(sql, arguments)
                    }
                }
            assertFails { replacement(failed).replace(file) }
            assertEquals(before, snapshot(target))
            assertEquals(raw, target.query("SELECT * FROM recording_points"))
            assertEquals(oldEpoch, epoch(target))
            replacement(target).replace(file)
            assertNull(SqliteSettings(target).getSetting("later"))
            assertNotEquals(oldEpoch, epoch(target))
        }

    private val recordedContext =
        """{ "version":1, "mode":"quick", "steps":[{"label":"Easy running","kind":"RUN","millis":60000}], "note":"original source" }"""

    private fun context(db: SqlDatabase, payload: String = recordedContext, id: String = "run") {
        db.execute(
            "INSERT INTO coach_support VALUES('execution',?,?)",
            listOf(SqlValue.Text(id), SqlValue.Text(payload)),
        )
    }

    @Test
    fun olderBackupWithoutSupportRetainsRecordedContextButReplacesUnrelatedSupport() =
        storage { source, target ->
            raw(target)
            context(target)
            context(target, id = "unassociated-setup")
            val data = snapshot(target)
            val raw = target.query("SELECT * FROM recording_points")
            replacement(target).replace(envelope(JsonObject(data - "support"), 5))
            assertEquals(
                listOf(
                    listOf(
                        SqlValue.Text("execution"),
                        SqlValue.Text("run"),
                        SqlValue.Text(recordedContext),
                    )
                ),
                target.query("SELECT * FROM coach_support"),
            )
            assertEquals(raw, target.query("SELECT * FROM recording_points"))
        }

    @Test
    fun matchingRecordedContextKeepsOriginalBytesDespiteObjectOrderAndWhitespace() =
        storage { source, target ->
            raw(target)
            context(target)
            val data = snapshot(target)
            val support = data.getValue("support").jsonObject
            val rows =
                support.getValue("rows").jsonArray.map { row ->
                    val r = row.jsonObject
                    JsonObject(
                        r +
                            ("payload" to
                                JsonObject(
                                    r.getValue("payload").jsonObject.entries.reversed().associate {
                                        it.toPair()
                                    }
                                ))
                    )
                }
            replacement(target)
                .replace(
                    envelope(
                        JsonObject(
                            data + ("support" to JsonObject(support + ("rows" to JsonArray(rows))))
                        )
                    )
                )
            assertEquals(
                recordedContext,
                target
                    .query("SELECT payload FROM coach_support WHERE id='run'")
                    .single()
                    .single()
                    .sqlText(),
            )
        }

    @Test
    fun changedStepOrUnknownContextEvidenceRejectsBeforeAnyReplacementWrite() =
        storage { source, target ->
            raw(target)
            context(target)
            val data = snapshot(target)
            val before = snapshot(target)
            val oldEpoch = epoch(target)
            var writes = 0
            val observed =
                object : SqlDatabase by target {
                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        writes++
                        target.execute(sql, arguments)
                    }
                }
            for (different in
                listOf(
                    recordedContext.replace("60000", "30000"),
                    recordedContext.replace("original source", "different evidence"),
                )) {
                val support = buildJsonObject {
                    put("version", 1)
                    put(
                        "rows",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("kind", "execution")
                                    put("id", "run")
                                    put("payload", Json.parseToJsonElement(different))
                                }
                            )
                        },
                    )
                }
                assertFails {
                    replacement(observed)
                        .replace(envelope(JsonObject(data + ("support" to support))))
                }
                assertEquals(0, writes)
                assertEquals(before, snapshot(target))
                assertEquals(oldEpoch, epoch(target))
            }
        }

    @Test
    fun excessiveRecordedContextBudgetRejectsBeforePayloadReadOrClearing() =
        storage { source, target ->
            raw(target)
            context(target)
            val file = bytes(target)
            var payloadReads = 0
            var writes = 0
            val observed =
                object : SqlDatabase by target {
                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        if (
                            sql.startsWith("SELECT COUNT(*)") &&
                                sql.contains("JOIN recording_sessions")
                        )
                            return listOf(
                                listOf(
                                    SqlValue.Integer(1),
                                    SqlValue.Integer(3),
                                    SqlValue.Integer(BackupEnvelopeJson.MAX_BYTES.toLong() + 1),
                                )
                            )
                        if (sql.startsWith("SELECT c.id,c.payload")) payloadReads++
                        return target.query(sql, arguments)
                    }

                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        writes++
                        target.execute(sql, arguments)
                    }
                }
            assertFails { replacement(observed).replace(file) }
            assertEquals(0, payloadReads)
            assertEquals(0, writes)
        }

    @Test
    fun callerRollbackRestoresOriginalRecordedContextAndAllOtherEvidence() =
        storage { source, target ->
            raw(target)
            context(target)
            val file = envelope(JsonObject(snapshot(target) - "support"), 5)
            val before = snapshot(target)
            val raw = target.query("SELECT * FROM recording_points")
            val oldEpoch = epoch(target)
            assertFails {
                target.transaction {
                    replacement(target).replace(file)
                    assertEquals(
                        recordedContext,
                        target
                            .query("SELECT payload FROM coach_support")
                            .single()
                            .single()
                            .sqlText(),
                    )
                    error("Caller rejects replacement")
                }
            }
            assertEquals(before, snapshot(target))
            assertEquals(raw, target.query("SELECT * FROM recording_points"))
            assertEquals(oldEpoch, epoch(target))
        }

    @Test
    fun callerRollbackKeepsOriginalDataAndIncomingUnfinishedGymDoesNotBypassPreReplacementGuard() =
        storage { source, target ->
            gym(source, true)
            val file = bytes(source)
            val oldEpoch = epoch(target)
            assertFails {
                target.transaction {
                    replacement(target).replace(file)
                    assertEquals(1, SqliteGymExecutionRecords(target).unfinished().size)
                    error("Caller rejects replacement")
                }
            }
            assertEquals(oldEpoch, epoch(target))
            assertTrue(SqliteGymExecutionRecords(target).unfinished().isEmpty())
            replacement(target).replace(file)
            assertEquals("gym", SqliteGymExecutionRecords(target).unfinished().single().id())
            assertTrue(SqliteCoachingArchive(target).snapshot().sessions.isEmpty())
            assertNotNull(SqliteGymTools(target).timer("gym"))
            assertNotEquals(oldEpoch, epoch(target))
            val before = target.query("SELECT total_changes()")
            assertFails { replacement(target).replace(file) }
            assertEquals(before, target.query("SELECT total_changes()"))
        }
}
