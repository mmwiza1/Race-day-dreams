package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.time.*

abstract class CoreBackupRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")

    private fun <T> storage(block: (SqlDatabase) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "core-backup-test")
            }
            return block(db)
        } finally {
            db.close()
        }
    }

    private fun activity(
        id: String,
        provider: String = "manual",
        samples: List<DistanceSample> = emptyList(),
    ) =
        ActivityRecord(
            id,
            SourceIdentity(provider, "record-$id", "local"),
            at,
            Instants.ofEpochSecond(at.getEpochSecond() + 7200, at.getNano().toLong()),
            null,
            null,
            6000.0,
            Durations.ofSeconds(6000),
            null,
            null,
            null,
            null,
            5.0,
            "Source note",
            samples,
            emptyList(),
            null,
            null,
        )

    @Test
    fun scheduleCanBeSavedAndInvalidReplacementLeavesExistingChoiceIntact() = storage { db ->
        val support = SqliteRunningSupportArchives(db)
        val schedule =
            SqliteRunningSupportArchives.SchedulePreferences(
                75,
                "FEWER_LONGER",
                setOf(CalendarDates.of(2026, 12, 5)),
            )
        support.saveSchedule(schedule)
        assertEquals(schedule, support.schedule())
        assertFailsWith<IllegalArgumentException> {
            support.saveSchedule(schedule.copy(maximumMinutes = 10))
        }
        assertEquals(schedule, support.schedule())

        val snapshot = SqliteApplicationBackup(db).encode(at)
        db.execute("DELETE FROM release08 WHERE kind='config' AND id='schedule'")
        SqliteApplicationBackupRestore(db) { "schedule-restored" }.replace(snapshot)
        assertEquals(schedule, support.schedule())
    }

    @Test
    fun dreamRoutePreferencesDefaultOffAndFreezeSelectedGeometryInNewRunOrigin() = storage { db ->
        val support = SqliteRunningSupportArchives(db)
        assertFalse(support.dreamPreferences().enabled)
        assertNull(StoredJson.objectValue(support.recordingOrigin("free", at))["dreamRoute"])
        val choices = SqliteRunningSupportArchives.DreamPreferences(true, setOf("paris-v2"))
        support.saveDreamPreferences(choices)
        assertEquals(choices, support.dreamPreferences())
        val origin = StoredJson.objectValue(support.recordingOrigin("race", at))
        assertEquals("race", origin.getValue("mode").jsonPrimitive.content)
        assertEquals("paris-2026-geo-v1", origin.getValue("dreamRoute").jsonPrimitive.content)
        support.saveDreamPreferences(choices.copy(enabled = false))
        assertNull(StoredJson.objectValue(support.recordingOrigin("free", at))["dreamRoute"])
        assertEquals("paris-2026-geo-v1", origin.getValue("dreamRoute").jsonPrimitive.content)
    }

    @Test
    fun savedDreamAssignmentReadIsScopedAndPreservesVersionedGeometry() = storage { db ->
        val support = SqliteRunningSupportArchives(db)
        assertNull(support.dreamAssignment("retained-run"))
        db.execute(
            "INSERT INTO coach_support(kind,id,payload) VALUES('dream','retained-run',?)",
            listOf(
                SqlValue.Text(
                    """{"version":1,"route":"paris-2026-geo-v1","start":1200,"distance":5000}"""
                )
            ),
        )
        val before = db.query("SELECT total_changes()")
        val saved = assertNotNull(support.dreamAssignment("retained-run"))
        assertEquals("paris-2026-geo-v1", saved.routeId())
        assertEquals(1200.0, saved.startMetres())
        assertEquals(5000.0, saved.coveredMetres())
        assertNull(support.dreamAssignment("unrelated-run"))
        assertEquals(before, db.query("SELECT total_changes()"))
    }

    @Test
    fun completedRaceWarmupSurvivesAppBackupAndRejectsUnknownRoutines() = storage { db ->
        val support = SqliteRunningSupportArchives(db)
        support.completeRoutine("completed", "warm-race-short", at)
        assertFailsWith<IllegalArgumentException> {
            support.completeRoutine("invalid", "unknown-routine", at)
        }
        val snapshot = SqliteApplicationBackup(db).encode(at)
        db.execute("DELETE FROM coach_support WHERE kind='routine'")
        SqliteApplicationBackupRestore(db) { "routine-restored" }.replace(snapshot)

        val restored = support.snapshot().support.records.single()
        assertEquals("completed", restored.id)
        assertEquals(SupportArchiveJson.Evidence.Routine("warm-race-short", at), restored.evidence)
    }

    @Test
    fun raceChecklistReadsLegacyRowsAndSurvivesAppBackupReplacement() = storage { db ->
        val id = "2026-11-20:Park 5K:Shoes & socks"
        val other = "2026-11-21:Park 5K:Shoes & socks"
        db.execute(
            "INSERT INTO release08(kind,id,payload) VALUES('checklist',?,?)",
            listOf(
                SqlValue.Text(id),
                SqlValue.Text("""{"version":1,"label":"Shoes & socks","checked":true}"""),
            ),
        )
        val checklist = SqliteRaceChecklist(db)
        assertTrue(checklist.checked(id))
        assertFalse(checklist.checked(other))
        checklist.save(other, "Shoes & socks", true)
        val snapshot = SqliteApplicationBackup(db).encode(at)
        checklist.save(id, "Shoes & socks", false)
        assertFalse(checklist.checked(id))

        SqliteApplicationBackupRestore(db) { "race-checklist-restored" }.replace(snapshot)
        assertTrue(checklist.checked(id))
        assertTrue(checklist.checked(other))
    }

    @Test
    fun strengthOnlySnapshotUsesOneReadOnlyTransactionAndDoesNotInventRaceProfileOrConsent() =
        storage { db ->
            SqlitePrimaryFocus(SqliteSettings(db)).save(PrimaryFocus.STRENGTH)
            val preferences = SqliteRetentionPreferences(db).load()
            val before = db.query("SELECT total_changes()")
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

                    override fun execute(sql: String, arguments: List<SqlValue>) =
                        error("Capture must not write")

                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        assertTrue(depth > 0)
                        assertTrue(arguments.size <= 512)
                        return db.query(sql, arguments)
                    }
                }
            val result = SqliteCoreBackup(readonly).snapshot()
            assertNull(result.core.profile)
            assertNull(result.core.draft)
            assertNull(result.core.baseline)
            assertNull(result.core.baselineAt)
            assertEquals("strength", result.core.settings["primary.focus"])
            assertTrue(result.core.activities.isEmpty())
            assertTrue(result.core.results.isEmpty())
            assertFalse("coaching" in result.data)
            assertFalse("detailRetention" in result.data)
            assertEquals(before, db.query("SELECT total_changes()"))
            assertEquals(preferences, SqliteRetentionPreferences(db).load())
        }

    @Test
    fun importedCachesBeyondOwnedRowAndByteBudgetsRemainUntouchedAndAreNeverDecoded() =
        storage { db ->
            db.execute(
                "WITH RECURSIVE n(x) AS (SELECT 0 UNION ALL SELECT x+1 FROM n WHERE x<100000) INSERT INTO activities(id,provider,record_id,start_millis,end_millis,payload) SELECT printf('cache-%06d',x),'health-connect',printf('cache-%06d',x),0,1,? FROM n",
                listOf(SqlValue.Text("Cached provider evidence " + "x".repeat(600))),
            )
            val source = activity("owned")
            SqliteActivities(db) { at }.upsertManualActivity(source)
            val before = db.query("SELECT total_changes()")
            val result = SqliteCoreBackup(db).snapshot()
            assertEquals(listOf(source), result.core.activities)
            assertEquals(
                100002L,
                db.query("SELECT COUNT(*) FROM activities").single().single().sqlLong(),
            )
            assertTrue(
                db.query(
                        "SELECT TOTAL(LENGTH(CAST(payload AS BLOB))) FROM activities WHERE provider='health-connect'"
                    )
                    .single()
                    .single()
                    .nullableSqlNumber()!! > BackupEnvelopeJson.MAX_BYTES
            )
            assertEquals(before, db.query("SELECT total_changes()"))
        }

    @Test
    fun legacyInlineRoutePayloadsMayExceedAggregateFileBudgetWhileCompactSummariesRemainValid() =
        storage { db ->
            val samples =
                (0..5999).map {
                    DistanceSample(
                        Durations.ofSeconds(it.toLong()),
                        it.toDouble(),
                        null,
                        null,
                        null,
                        false,
                    )
                }
            val first = activity("run-000", "rdd", samples)
            val approximate = ActivityJson.encode(first).encodeToByteArray().size
            val count = BackupEnvelopeJson.MAX_BYTES / approximate + 2
            assertTrue(count in 2..100)
            db.transaction {
                for (i in 0 until count) {
                    val source = activity("run-${i.toString().padStart(3,'0')}", "rdd", samples)
                    db.execute(
                        "INSERT INTO activities(id,provider,record_id,origin_package,start_millis,end_millis,payload) VALUES(?,?,?,?,?,?,?)",
                        listOf(
                            SqlValue.Text(source.id()),
                            SqlValue.Text("rdd"),
                            SqlValue.Text(source.source().recordId()),
                            SqlValue.Text("local"),
                            SqlValue.Integer(storedEpochMillis(at)),
                            SqlValue.Integer(storedEpochMillis(source.end())),
                            SqlValue.Text(ActivityJson.encode(source)),
                        ),
                    )
                }
            }
            val before = db.query("SELECT total_changes()")
            assertTrue(
                db.query("SELECT TOTAL(LENGTH(CAST(payload AS BLOB))) FROM activities")
                    .single()
                    .single()
                    .nullableSqlNumber()!! > BackupEnvelopeJson.MAX_BYTES
            )
            val result = SqliteCoreBackup(db).snapshot()
            assertEquals(count, result.core.activities.size)
            assertTrue(
                result.core.activities.all {
                    it.samples().isEmpty() &&
                        it.distanceMetres() == 6000.0 &&
                        it.movingTime() == Durations.ofSeconds(6000)
                }
            )
            assertTrue(result.data.toString().encodeToByteArray().size < 200000)
            assertEquals(before, db.query("SELECT total_changes()"))
            assertEquals(
                ActivityJson.encode(first),
                db.query("SELECT payload FROM activities WHERE id='run-000'")
                    .single()
                    .single()
                    .sqlText(),
            )
        }

    @Test
    fun actualAnnotationsDefaultShoeAuthorityAndRetainedBestPrecedenceMatchExistingExports() =
        storage { db ->
            val shoes = SqliteShoes(db)
            shoes.saveShoe(Shoe("shoe-a", "Brand", "First", null, 0.0, false, null, false))
            shoes.saveShoe(Shoe("shoe-b", "Brand", "Default", null, 0.0, false, null, true))
            val source = activity("actual")
            SqliteActivities(db) { at }.upsertManualActivity(source)
            db.execute("UPDATE activity_annotations SET notes=NULL,rpe=2.5,shoe_id='shoe-a'")
            val official =
                RecordedResult(
                    "race",
                    PerformanceDistance.FIVE_K,
                    Durations.ofMinutes(25),
                    CalendarDates.of(2026, 11, 16),
                    "Local race",
                    ResultSource.OFFICIAL_CHIP,
                    null,
                    "Recorded",
                )
            SqliteResults(db).saveResult(official)
            val best =
                official.copy(id = "best", source = ResultSource.ROLLING, activityId = "actual")
            db.execute(
                "INSERT INTO best_efforts VALUES('best','actual',?,?)",
                listOf(
                    SqlValue.Text(PerformanceDistance.FIVE_K.code()),
                    SqlValue.Text(ResultJson.encode(best)),
                ),
            )
            db.execute(
                "INSERT INTO preserved_best_efforts VALUES('best','actual',?,'ignored-provider','old',NULL,'{}')",
                listOf(SqlValue.Text(PerformanceDistance.FIVE_K.code())),
            )
            val before = db.query("SELECT total_changes()")
            val result = SqliteCoreBackup(db).snapshot()
            assertEquals(listOf("shoe-b", "shoe-a"), result.core.shoes.map { it.id() })
            assertEquals("Source note", result.core.activities.single().notes())
            assertEquals(2.5, result.core.activities.single().rpe())
            assertEquals("shoe-a", result.core.activities.single().shoeId())
            assertEquals(listOf(official), result.core.results)
            assertEquals(best, result.core.bestEfforts.single().result)
            assertEquals(source.source(), result.core.bestEfforts.single().source)
            assertEquals(before, db.query("SELECT total_changes()"))
        }

    @Test
    fun sqliteUnicodeOrderAndEmbeddedNullSourcePairsDoNotCollapseOrReorderHistory() =
        storage { db ->
            for (id in listOf("id-\uD83D\uDE00", "id-\uE000")) SqliteActivities(db) { at }
                .upsertManualActivity(activity(id))
            for ((provider, record) in listOf("a\u0000b" to "c", "a" to "b\u0000c")) db.execute(
                "INSERT INTO activity_annotations VALUES(?,?,NULL,NULL,NULL)",
                listOf(SqlValue.Text(provider), SqlValue.Text(record)),
            )
            val result = SqliteCoreBackup(db).snapshot()
            assertEquals(
                db.query("SELECT id FROM activities ORDER BY start_millis DESC,id").map {
                    it.single().sqlText()
                },
                result.core.activities.map { it.id() },
            )
            val expected =
                db.query(
                        "SELECT provider,record_id FROM activity_annotations ORDER BY provider,record_id"
                    )
                    .map { it[0].sqlText() to it[1].sqlText() }
            assertEquals(expected, result.core.annotations.map { it.provider to it.recordId })
            assertTrue(
                result.core.annotations.any { it.provider == "a\u0000b" && it.recordId == "c" }
            )
            assertTrue(
                result.core.annotations.any { it.provider == "a" && it.recordId == "b\u0000c" }
            )
            assertEquals(4, result.core.annotations.size)
        }

    @Test
    fun corruptOrOversizedOwnedSourcesRejectWithoutChangingDataAndSubsequentValidCaptureWorks() =
        storage { db ->
            SqliteActivities(db) { at }.upsertManualActivity(activity("retained"))
            db.execute("INSERT INTO onboarding_draft VALUES(1,'{')")
            val before = db.query("SELECT total_changes()")
            assertFails { SqliteCoreBackup(db).snapshot() }
            assertEquals(before, db.query("SELECT total_changes()"))
            assertEquals(
                "{",
                db.query("SELECT payload FROM onboarding_draft").single().single().sqlText(),
            )
            db.execute("DELETE FROM onboarding_draft")
            db.execute(
                "INSERT INTO shoes VALUES('bad',zeroblob(?),0)",
                listOf(SqlValue.Integer(BackupEnvelopeJson.MAX_BYTES.toLong() + 1)),
            )
            val corrupt = db.query("SELECT total_changes()")
            assertFails { SqliteCoreBackup(db).snapshot() }
            assertEquals(corrupt, db.query("SELECT total_changes()"))
            db.execute("DELETE FROM shoes WHERE id='bad'")
            assertEquals("retained", SqliteCoreBackup(db).snapshot().core.activities.single().id())
        }
}
