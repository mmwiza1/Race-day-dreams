package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.*
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.racedaydreams.domain.achievements.TrainingMilestones
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.body.BodyHeight
import org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.body.HeightUnit
import org.racedaydreams.domain.body.ProteinGuidance
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.support.DreamRoutes
import org.racedaydreams.domain.support.RunningRoutines
import org.racedaydreams.portability.BackupManager
import org.racedaydreams.storage.AchievementsArchiveJson
import org.racedaydreams.storage.BackupEnvelopeJson
import org.racedaydreams.storage.BodyMeasurementsArchiveJson
import org.racedaydreams.storage.CoreBackupDataJson
import org.racedaydreams.storage.DevelopmentArchiveJson
import org.racedaydreams.storage.Release08ArchiveJson
import org.racedaydreams.storage.SqliteRunningSupportArchives
import org.racedaydreams.storage.StrengthChoicesArchiveJson
import org.racedaydreams.storage.SupportArchiveJson

@RunWith(AndroidJUnit4::class)
class SharedBackupEnvelopeTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val at = Instant.parse("2026-11-16T10:00:00.123456789Z")
    private lateinit var repository: SqliteBackupRepository
    private lateinit var original: BackupManager

    @Test
    fun completePackageManifestMatchesRealOriginalPayloadHashesWithoutReplacingAppData() {
        val before = repository.exportUserData().toString()
        val output = ByteArrayOutputStream()
        CompleteBackup(context).exportTo(output)
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                assertFalse(entries.containsKey(entry.name))
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        assertEquals(setOf("manifest.json", "app.json", "recordings.zip"), entries.keys)
        val manifest =
            org.racedaydreams.storage.CompleteBackupManifest.decode(
                entries.getValue("manifest.json")
            )
        val receipts =
            entries
                .filterKeys { it != "manifest.json" }
                .map { (name, bytes) ->
                    org.racedaydreams.storage.CompleteBackupManifest.Receipt(
                        name,
                        org.racedaydreams.storage.CompleteBackupManifest.Entry(
                            bytes.size.toLong(),
                            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                                "%02x".format(it)
                            },
                        ),
                    )
                }
        manifest.verify(receipts)
        val parent =
            org.racedaydreams.storage.UserBackupDataJson.inspect(entries.getValue("app.json"))
        assertEquals(13, parent.version)
        assertTrue(parent.snapshot.engine!!.executions.isEmpty())
        assertTrue(
            runCatching { manifest.verify(listOf(receipts.first(), receipts.first())) }.isFailure
        )
        assertEquals(before, repository.exportUserData().toString())
    }

    @Test
    fun sharedBodyArchiveMatchesOriginalSourceIdentityAndOptionalGuidancePreferences() {
        val store = SqliteBodyMeasurementsRepository(context)
        val height =
            BodyHeight("manual-height", 1.83, at, null, "manual", "height-source", null, null)
        val imported =
            BodyHeight(
                "imported-height",
                1.84,
                at,
                ZoneOffset.ofHoursMinutesSeconds(5, 30, 15),
                "health-connect",
                "provider-id",
                "provider.package",
                at,
            )
        store.saveHeight(height)
        store.saveHeight(imported)
        store.preferences(
            Preferences(
                true,
                HeightUnit.CENTIMETRES,
                ProteinGuidance.Basis("retained-weight-reference", at, 87.25),
            )
        )
        val db = ProfileDatabase.get(context).writableDatabase
        val before = SqliteBodyMeasurementsRepository.exportData(db)
        val original = SqliteBodyMeasurementsRepository.validate(before)
        val shared =
            BodyMeasurementsArchiveJson.decode(
                Json.parseToJsonElement(before.toString()) as JsonObject
            )
        assertEquals(original.heights(), shared.heights)
        assertEquals(original.preferences(), shared.preferences)
        val rechecked =
            SqliteBodyMeasurementsRepository.validate(
                JSONObject(BodyMeasurementsArchiveJson.element(shared).toString())
            )
        assertEquals(original.heights(), rechecked.heights())
        assertEquals(original.preferences(), rechecked.preferences())
        val duplicate =
            JSONObject(before.toString()).apply {
                getJSONArray("heights").put(getJSONArray("heights").getJSONObject(0))
            }
        for (bad in
            listOf(
                JSONObject(before.toString()).put("version", 2),
                duplicate,
                JSONObject(before.toString()).apply {
                    getJSONArray("heights").getJSONObject(0).put("metres", 0)
                },
            )) {
            assertTrue(runCatching { SqliteBodyMeasurementsRepository.validate(bad) }.isFailure)
            assertTrue(
                runCatching {
                        BodyMeasurementsArchiveJson.decode(
                            Json.parseToJsonElement(bad.toString()) as JsonObject
                        )
                    }
                    .isFailure
            )
        }
        assertEquals(before.toString(), SqliteBodyMeasurementsRepository.exportData(db).toString())
        assertEquals(
            SqliteBodyMeasurementsRepository.validate(null).preferences(),
            BodyMeasurementsArchiveJson.decode(null).preferences,
        )
    }

    @Test
    fun sharedAchievementsArchiveRetainsOriginalProofWithoutReawardingOrChangingCelebrations() {
        val store = SqliteAchievementsRepository(context)
        val proof =
            TrainingMilestones.Proof(
                TrainingMilestones.Source.RUN,
                "retained-run",
                at,
                null,
                5000.5,
            )
        val award =
            TrainingMilestones.Award(TrainingMilestones.Kind.FIRST_FIVE_KM_RUN, at, listOf(proof))
        store.recognize(listOf(award), at)
        store.celebrations(false)
        val db = ProfileDatabase.get(context).writableDatabase
        val before = SqliteAchievementsRepository.exportData(db)
        val original = SqliteAchievementsRepository.validate(before)
        val shared =
            AchievementsArchiveJson.decode(Json.parseToJsonElement(before.toString()) as JsonObject)
        assertFalse(shared.celebrations)
        assertEquals(original.awards().single().award(), shared.awards.single().award)
        assertEquals(original.awards().single().recognizedAt(), shared.awards.single().recognizedAt)
        val rechecked =
            SqliteAchievementsRepository.validate(
                JSONObject(AchievementsArchiveJson.element(shared).toString())
            )
        assertEquals(original.awards(), rechecked.awards())
        assertFalse(rechecked.celebrations())
        for (bad in
            listOf(
                JSONObject(before.toString()).put("version", 2),
                JSONObject(before.toString()).apply {
                    getJSONArray("awards").put(getJSONArray("awards").getJSONObject(0))
                },
                JSONObject(before.toString()).apply {
                    getJSONArray("awards")
                        .getJSONObject(0)
                        .put("recognizedAt", "2020-01-01T00:00:00Z")
                },
            )) {
            assertTrue(runCatching { SqliteAchievementsRepository.validate(bad) }.isFailure)
            assertTrue(
                runCatching {
                        AchievementsArchiveJson.decode(
                            Json.parseToJsonElement(bad.toString()) as JsonObject
                        )
                    }
                    .isFailure
            )
        }
        assertEquals(before.toString(), SqliteAchievementsRepository.exportData(db).toString())
        assertEquals(0, store.recognize(listOf(award), at))
        assertFalse(AchievementsArchiveJson.decode(null).celebrations)
    }

    @Test
    fun sharedStrengthChoicesArchivePreservesOriginalFavouritesExclusionsAndAcknowledgements() {
        val store = StrengthUserStore(context)
        store.save(StrengthUserStore.Choice("retained-exercise", true, false))
        store.acknowledge("record-1")
        store.exclude(
            ExerciseExclusionRepository.Exclusion(
                "retained-exercise",
                ExerciseExclusionRepository.Reason.UNCOMFORTABLE,
                at,
            )
        )
        val db = ProfileDatabase.get(context).writableDatabase
        val before = StrengthUserStore.exportData(db)
        StrengthUserStore.validate(before)
        val snapshot =
            StrengthChoicesArchiveJson.decode(
                Json.parseToJsonElement(before.toString()) as JsonObject
            )
        assertEquals(
            StrengthChoicesArchiveJson.element(snapshot),
            StrengthChoicesArchiveJson.element(
                org.racedaydreams.storage
                    .SqliteStrengthChoices(AndroidSqlTestConnection(db))
                    .archiveSnapshot()
            ),
        )
        assertEquals("retained-exercise", snapshot.choices.single().exerciseId)
        assertEquals(store.exclusions(), snapshot.exclusions)
        assertEquals(store.acknowledged(), snapshot.acknowledged.toSet())
        assertEquals(
            Json.parseToJsonElement(before.toString()),
            StrengthChoicesArchiveJson.element(snapshot),
        )
        StrengthUserStore.validate(
            JSONObject(StrengthChoicesArchiveJson.element(snapshot).toString())
        )
        val legacy =
            JSONObject(before.toString()).put("version", 1).apply {
                remove("exclusions")
                put("legacyExtra", "preserved")
            }
        StrengthUserStore.validate(legacy)
        assertEquals(
            Json.parseToJsonElement(legacy.toString()),
            StrengthChoicesArchiveJson.element(
                StrengthChoicesArchiveJson.decode(
                    Json.parseToJsonElement(legacy.toString()) as JsonObject
                )
            ),
        )
        val badExclusions = JSONObject(before.toString())
        badExclusions
            .getJSONObject("exclusions")
            .getJSONArray("entries")
            .getJSONObject(0)
            .put("reason", "INJURED")
        for (bad in
            listOf(
                JSONObject(before.toString()).put("version", 3),
                JSONObject(before.toString()).put("extra", true),
                badExclusions,
                JSONObject(before.toString()).apply {
                    getJSONArray("choices").getJSONObject(0).put("favourite", "true")
                },
            )) {
            assertTrue(runCatching { StrengthUserStore.validate(bad) }.isFailure)
            assertTrue(
                runCatching {
                        StrengthChoicesArchiveJson.decode(
                            Json.parseToJsonElement(bad.toString()) as JsonObject
                        )
                    }
                    .isFailure
            )
        }
        assertEquals(before.toString(), StrengthUserStore.exportData(db).toString())
        store.restoreExercise("retained-exercise")
        assertTrue(store.exclusions().isEmpty())
        assertEquals("retained-exercise", store.choices().single().exerciseId())
        assertEquals(setOf("record-1"), store.acknowledged())
    }

    @Test
    fun sharedSupportArchiveMatchesOriginalSavedRunningEvidenceWithoutSideEffects() {
        val db = ProfileDatabase.get(context).writableDatabase
        val route = DreamRoutes.routes().first().id()
        SupportStore.put(
            db,
            "dream",
            "actual",
            JSONObject()
                .put("version", 1)
                .put("route", route)
                .put("start", 0)
                .put("distance", 5000)
                .put("assignedAt", at.toString()),
        )
        SupportStore.put(
            db,
            "routine",
            "routine",
            JSONObject()
                .put("version", 1)
                .put("routine", RunningRoutines.all().first().id())
                .put("at", at.toString())
                .put("related", "actual"),
        )
        for (mode in listOf("workout", "race", "quick", "custom", "repeat")) {
            val payload =
                JSONObject()
                    .put("version", 1)
                    .put("mode", mode)
                    .put("planVersion", "historic-v1")
                    .put("workoutId", "run")
                    .put(
                        "steps",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("label", "Run")
                                    .put("kind", "RUN")
                                    .put("metres", "1000.5")
                                    .put("millis", "300000.9")
                            ),
                    )
                    .put(
                        "events",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("step", 1)
                                    .put("active", "1000.9")
                                    .put("metres", "10.5")
                                    .put("type", "WORKOUT_FINISHED_EARLY")
                            ),
                    )
                    .put("interrupted", true)
                    .put("unrecognisedLegacyField", "retained")
            SupportStore.put(db, "execution", mode, payload)
        }
        val planned =
            org.racedaydreams.domain.PlannedWorkout(
                "run",
                LocalDate.of(2026, 11, 16),
                org.racedaydreams.domain.WorkoutCategory.EASY_RUN,
                5.0,
                Duration.ofMinutes(30),
                emptyList(),
                "Retained prescription",
            )
        val retained =
            SupportStore(context)
                .get("execution", "workout")!!
                .put("workout", FeatureJson.planned(planned))
        SupportStore.put(db, "execution", "workout", retained)
        val before = SupportStore.exportData(db)
        SupportStore.validate(before)
        val snapshot =
            SupportArchiveJson.decode(Json.parseToJsonElement(before.toString()) as JsonObject)
        assertEquals(7, snapshot.records.size)
        assertEquals(
            Json.parseToJsonElement(before.toString()),
            SupportArchiveJson.element(snapshot),
        )
        SupportStore.validate(JSONObject(SupportArchiveJson.element(snapshot).toString()))
        assertEquals(
            SupportArchiveJson.element(snapshot),
            SupportArchiveJson.element(
                SqliteRunningSupportArchives(AndroidSqlTestConnection(db)).snapshot().support
            ),
        )
        for (record in snapshot.records.filter { it.kind == SupportArchiveJson.Kind.EXECUTION }) {
            val originalSteps = SupportStore.steps(JSONObject(record.payload.toString()))
            assertEquals(
                originalSteps,
                (record.evidence as SupportArchiveJson.Evidence.Execution).steps,
            )
        }
        val bad = JSONObject(before.toString())
        bad.getJSONArray("rows")
            .put(
                JSONObject()
                    .put("kind", "execution")
                    .put("id", "corrupt")
                    .put(
                        "payload",
                        JSONObject()
                            .put("version", 1)
                            .put("mode", "quick")
                            .put(
                                "steps",
                                JSONArray().put(JSONObject().put("label", "Run").put("kind", "RUN")),
                            )
                            .put(
                                "events",
                                JSONArray()
                                    .put(
                                        JSONObject()
                                            .put("step", 0)
                                            .put("active", -1)
                                            .put("metres", 0)
                                            .put("type", "STEP_STARTED")
                                    ),
                            ),
                    )
            )
        assertTrue(runCatching { SupportStore.validate(bad) }.isFailure)
        assertTrue(
            runCatching {
                    SupportArchiveJson.decode(Json.parseToJsonElement(bad.toString()) as JsonObject)
                }
                .isFailure
        )
        assertEquals(before.toString(), SupportStore.exportData(db).toString())
        assertTrue(CoachingStore(context).versions().isEmpty())
    }

    @Test
    fun sharedRelease08ArchiveMatchesOriginalCustomDefinitionsAndPreferences() {
        val db = ProfileDatabase.get(context).writableDatabase
        val city = DreamRoutes.routes().first().id()
        val rows =
            listOf(
                Triple(
                    "custom",
                    "custom",
                    """{"version":1,"id":"custom","title":"My run","groups":[{"repeats":"2.9","steps":[{"label":"Run","kind":"RUN","metres":"500.5"}]}],"extra":"preserved"}""",
                ),
                Triple(
                    "origin",
                    "run",
                    """{"version":1,"mode":"free","at":"$at","dreamRoute":null}""",
                ),
                Triple(
                    "association",
                    "actual",
                    """{"version":1,"activity":"actual","planVersion":"v1","workout":"run","at":"$at"}""",
                ),
                Triple("checklist", "item", """{"version":1,"label":"Shoes","checked":"TRUE"}"""),
                Triple(
                    "config",
                    "dream",
                    """{"version":1,"type":"dream","enabled":"FALSE","cities":["$city"]}""",
                ),
                Triple(
                    "config",
                    "schedule",
                    """{"version":1,"type":"schedule","maximumMinutes":"45.9","distribution":"BALANCED","unavailable":["2026-11-17","2026-11-17"]}""",
                ),
                Triple(
                    "config",
                    "shoe-feedback",
                    """{"version":1,"type":"shoe_feedback","activities":["actual"]}""",
                ),
                Triple("config", "start", """{"version":1,"type":"start","path":"hybrid"}"""),
            )
        for ((kind, id, payload) in rows) Release08Store.put(db, kind, id, JSONObject(payload))
        val before = Release08Store.exportData(db)
        Release08Store.validate(before)
        val snapshot =
            Release08ArchiveJson.decode(Json.parseToJsonElement(before.toString()) as JsonObject)
        assertEquals(8, snapshot.records.size)
        assertEquals(
            Json.parseToJsonElement(before.toString()),
            Release08ArchiveJson.element(snapshot),
        )
        Release08Store.validate(JSONObject(Release08ArchiveJson.element(snapshot).toString()))
        assertEquals(
            Release08ArchiveJson.element(snapshot),
            Release08ArchiveJson.element(
                SqliteRunningSupportArchives(AndroidSqlTestConnection(db)).snapshot().release08
            ),
        )
        val custom = snapshot.records.single { it.kind == Release08ArchiveJson.Kind.CUSTOM }
        assertEquals(Release08Store.decode(JSONObject(custom.payload.toString())), custom.custom)
        for (bad in
            listOf(
                JSONObject(before.toString()).put("version", 2),
                JSONObject()
                    .put("version", 1)
                    .put(
                        "rows",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("kind", "custom")
                                    .put("id", "different")
                                    .put("payload", JSONObject(rows.first().third))
                            ),
                    ),
                JSONObject()
                    .put("version", 1)
                    .put(
                        "rows",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("kind", "config")
                                    .put("id", "dream")
                                    .put("payload", JSONObject(rows.last().third))
                            ),
                    ),
            )) {
            assertTrue(runCatching { Release08Store.validate(bad) }.isFailure)
            assertTrue(
                runCatching {
                        Release08ArchiveJson.decode(
                            Json.parseToJsonElement(bad.toString()) as JsonObject
                        )
                    }
                    .isFailure
            )
        }
        assertEquals(before.toString(), Release08Store.exportData(db).toString())
        assertEquals(2, Release08Store(context).workouts().single().flattened().size)
    }

    @Test
    fun developmentArchiveMatchesOriginalKindsAndManualWeightExportFilter() {
        val store = DevelopmentStore(context)
        store.saveWeight(
            BodyWeight(
                "manual",
                87.25,
                at,
                ZoneOffset.ofHoursMinutesSeconds(5, 30, 15),
                "manual",
                "source",
                null,
            )
        )
        store.saveWeight(
            BodyWeight(
                "imported",
                88.0,
                at,
                ZoneOffset.UTC,
                "health-connect",
                "imported-source",
                "provider",
            )
        )
        val exercise =
            StrengthExercise(
                "history",
                "Retained lift",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                true,
            )
        store.saveHistorical(
            HistoricalStrengthRecord(
                "history",
                exercise,
                82.25,
                6,
                null,
                HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                "Entered history",
            )
        )
        val db = ProfileDatabase.get(context).writableDatabase
        val examples =
            mapOf(
                "INTERRUPTION" to
                    """{"id":"INTERRUPTION","from":"2026-10-01","returned":"2026-10-20","reason":"Time away"}""",
                "SESSION_DECISION" to
                    """{"id":"SESSION_DECISION","workoutId":"run","versionId":"v1","originalDate":"2026-11-16","movedTo":null,"kind":"SKIPPED","at":"$at"}""",
                "STRENGTH_LINK" to
                    """{"id":"STRENGTH_LINK","plannedDate":"2026-11-16","gymVersion":null,"gymSession":null}""",
                "REVIEW_DECISION" to
                    """{"id":"REVIEW_DECISION","base":"v1","choice":"keep","result":"v1"}""",
                "WEEKLY_REVIEW" to
                    """{"id":"WEEKLY_REVIEW","base":"v1","week":"2026-11-16","planned":3,"completed":4,"km":12.5,"observations":["retained"],"channels":["legacy"],"choices":[{"replacements":[]}],"extra":"preserved"}""",
            )
        for ((kind, payload) in examples) db.execSQL(
            "INSERT INTO coach_evidence(kind,id,payload) VALUES(?,?,?)",
            arrayOf(kind, kind, payload),
        )
        val before = DevelopmentStore.exportData(db)
        DevelopmentStore.validate(before)
        val snapshot =
            DevelopmentArchiveJson.decode(Json.parseToJsonElement(before.toString()) as JsonObject)
        assertEquals(7, snapshot.records.size)
        val captured =
            org.racedaydreams.storage
                .SqliteDevelopmentArchive(AndroidSqlTestConnection(db))
                .snapshot()
        assertEquals(
            Json.parseToJsonElement(before.toString()),
            DevelopmentArchiveJson.element(captured),
        )
        assertEquals(
            Json.parseToJsonElement(before.toString()),
            DevelopmentArchiveJson.element(snapshot),
        )
        val incoming = JSONObject(before.toString())
        db.rawQuery(
                "SELECT payload FROM coach_evidence WHERE kind='WEIGHT' AND id='imported'",
                null,
            )
            .use {
                assertTrue(it.moveToFirst())
                incoming
                    .getJSONArray("records")
                    .put(
                        JSONObject()
                            .put("kind", "WEIGHT")
                            .put("id", "imported")
                            .put("payload", JSONObject(it.getString(0)))
                    )
            }
        DevelopmentStore.validate(incoming)
        val oldSnapshot =
            DevelopmentArchiveJson.decode(
                Json.parseToJsonElement(incoming.toString()) as JsonObject
            )
        assertEquals(8, oldSnapshot.records.size)
        assertEquals(
            Json.parseToJsonElement(before.toString()),
            DevelopmentArchiveJson.userOwnedElement(oldSnapshot),
        )
        DevelopmentStore.validate(
            JSONObject(DevelopmentArchiveJson.element(oldSnapshot).toString())
        )
        assertEquals(2, store.weights().size)
        assertEquals(before.toString(), DevelopmentStore.exportData(db).toString())
        for (i in 0 until 513) {
            val id = "cache-page-$i"
            store.saveWeight(
                BodyWeight(id, 88.0, at, ZoneOffset.UTC, "health-connect", id, "provider")
            )
        }
        val paged =
            org.racedaydreams.storage
                .SqliteDevelopmentArchive(AndroidSqlTestConnection(db))
                .snapshot()
        assertEquals(
            Json.parseToJsonElement(before.toString()),
            DevelopmentArchiveJson.element(paged),
        )
        assertEquals(515, store.weights().size)
        assertEquals(before.toString(), DevelopmentStore.exportData(db).toString())
    }

    @Test
    fun malformedDevelopmentEvidenceFailsBothValidatorsWithoutChangingStoredHistory() {
        val store = DevelopmentStore(context)
        store.saveWeight(BodyWeight("manual", 87.25, at, ZoneOffset.UTC, "manual", "source", null))
        val db = ProfileDatabase.get(context).writableDatabase
        val before = DevelopmentStore.exportData(db)
        val record = before.getJSONArray("records").getJSONObject(0)
        val malformed =
            listOf(
                JSONObject(before.toString()).put("version", 2),
                JSONObject(before.toString()).put("records", JSONArray().put(record).put(record)),
                JSONObject(before.toString())
                    .put(
                        "records",
                        JSONArray().put(JSONObject(record.toString()).put("id", "another")),
                    ),
                JSONObject(before.toString())
                    .put(
                        "records",
                        JSONArray().put(JSONObject(record.toString()).put("kind", "UNKNOWN")),
                    ),
                JSONObject(before.toString())
                    .put(
                        "records",
                        JSONArray()
                            .put(
                                JSONObject(record.toString())
                                    .put(
                                        "payload",
                                        JSONObject(record.getJSONObject("payload").toString())
                                            .put("kg", 0),
                                    )
                            ),
                    ),
            )
        for (bad in malformed) {
            assertTrue(runCatching { DevelopmentStore.validate(bad) }.isFailure)
            assertTrue(
                runCatching {
                        DevelopmentArchiveJson.decode(
                            Json.parseToJsonElement(bad.toString()) as JsonObject
                        )
                    }
                    .isFailure
            )
        }
        assertEquals(before.toString(), DevelopmentStore.exportData(db).toString())
    }

    @Test
    fun sharedCoreGuardsMatchOriginalWholeDataActivityResultAndAnnotationRules() {
        val actual =
            ActivityRecord(
                "actual",
                SourceIdentity("rdd", "run", null),
                at,
                at.plusSeconds(600),
                ZoneOffset.UTC,
                null,
                3000.0,
                Duration.ofSeconds(600),
                null,
                null,
                null,
                null,
                6.0,
                "Retained actual",
                emptyList(),
                emptyList(),
                null,
                null,
            )
        SqliteActivityRepository(context).upsertManualActivity(actual)
        val result =
            RecordedResult(
                "race",
                PerformanceDistance.FIVE_K,
                Duration.ofMinutes(25),
                LocalDate.of(2026, 11, 16),
                "Race",
                ResultSource.OFFICIAL_CHIP,
                null,
                "Recorded",
            )
        SqliteResultRepository(context).saveResult(result)
        val before = repository.exportUserData()
        val snapshot =
            CoreBackupDataJson.decode(Json.parseToJsonElement(before.toString()) as JsonObject)
        assertEquals(listOf(actual), snapshot.activities)
        assertEquals(listOf(result), snapshot.results)
        repository.validateSnapshot(before)
        val duplicated =
            JSONObject(before.toString())
                .put(
                    "activities",
                    JSONArray()
                        .put(before.getJSONArray("activities").getJSONObject(0))
                        .put(before.getJSONArray("activities").getJSONObject(0)),
                )
        val external = JSONObject(before.toString())
        external
            .getJSONArray("activities")
            .getJSONObject(0)
            .getJSONObject("source")
            .put("provider", "health-connect")
        val missingNotes =
            JSONObject(before.toString())
                .put(
                    "annotations",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("provider", "rdd")
                                .put("recordId", "run")
                                .put("rpe", 7.5)
                                .put("shoeId", JSONObject.NULL)
                        ),
                )
        for (bad in
            listOf(
                duplicated,
                external,
                missingNotes,
                JSONObject(before.toString()).put("settings", JSONObject().put("invalid", 42)),
            )) {
            assertTrue(runCatching { repository.validateSnapshot(bad) }.isFailure)
            assertTrue(
                runCatching {
                        CoreBackupDataJson.decode(
                            Json.parseToJsonElement(bad.toString()) as JsonObject
                        )
                    }
                    .isFailure
            )
        }
        assertEquals(before.toString(), repository.exportUserData().toString())
    }

    private val introduced =
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

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        repository = SqliteBackupRepository(context)
        original = BackupManager(context, repository, Clock.fixed(at, ZoneOffset.UTC))
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun checked(bytes: ByteArray) =
        BackupEnvelopeJson.inspect(bytes) {
            repository.validateSnapshot(JSONObject(it.toString()))
            org.racedaydreams.storage.UserBackupDataJson.decode(it)
            it
        }

    private fun envelope(data: JSONObject, version: Int) =
        JSONObject()
            .put("format", BackupManager.FORMAT)
            .put("version", version)
            .put("createdAt", at.toString())
            .put("data", data)
            .toString()
            .toByteArray(Charsets.UTF_8)

    @Test
    fun originalOldCurrentAndSharedExportsPassTheSameCompleteSnapshotValidator() {
        val source = repository.exportUserData()
        for (version in 1..13) {
            val data = JSONObject(source.toString())
            for ((section, minimum) in introduced) if (version < minimum) data.remove(section)
            val bytes = envelope(data, version)
            val old = original.inspect(ByteArrayInputStream(bytes))
            val shared = checked(bytes)
            assertEquals(old.getInt("version"), shared.version)
            assertEquals(at, shared.createdAt)
            assertEquals(
                Json.parseToJsonElement(old.getJSONObject("data").toString()),
                shared.snapshot,
            )
        }
        val output = ByteArrayOutputStream()
        original.exportTo(output)
        val shared = checked(output.toByteArray())
        val encoded =
            BackupEnvelopeJson.encode(shared.snapshot, at) {
                repository.validateSnapshot(JSONObject(it.toString()))
            }
        val old = original.inspect(ByteArrayInputStream(encoded))
        assertEquals(shared.snapshot, Json.parseToJsonElement(old.getJSONObject("data").toString()))
        assertEquals(
            shared,
            checked(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + encoded),
        )
        original.inspect(
            ByteArrayInputStream(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + encoded)
        )
    }

    @Test
    fun fullRowValidatorAndEnvelopeFailuresNeverReplaceExistingData() {
        val before = repository.exportUserData().toString()
        val badRows = JSONObject(before).put("settings", JSONObject().put("invalid", 42))
        val bad =
            listOf(
                envelope(JSONObject(before), 14),
                envelope(badRows, 13),
                envelope(JSONObject(before).apply { remove("detailRetention") }, 13),
            )
        for (bytes in bad) {
            assertTrue(runCatching { original.inspect(ByteArrayInputStream(bytes)) }.isFailure)
            assertTrue(runCatching { checked(bytes) }.isFailure)
            assertEquals(before, repository.exportUserData().toString())
        }
        var accepted = false
        val rows = Json.parseToJsonElement(badRows.toString()) as JsonObject
        assertTrue(
            runCatching {
                    BackupEnvelopeJson.encode(rows, at) {
                        repository.validateSnapshot(JSONObject(it.toString()))
                        accepted = true
                    }
                }
                .isFailure
        )
        assertFalse(accepted)
        assertEquals(before, repository.exportUserData().toString())
    }
}
