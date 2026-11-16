package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.strength.*
import org.racedaydreams.portability.BackupManager
import org.racedaydreams.storage.BackupEnvelopeJson
import org.racedaydreams.storage.CoreBackupDataJson
import org.racedaydreams.storage.DevelopmentArchiveJson

@RunWith(AndroidJUnit4::class)
class SharedBackupEnvelopeTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val at = Instant.parse("2026-11-16T10:00:00.123456789Z")
    private lateinit var repository: SqliteBackupRepository
    private lateinit var original: BackupManager

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
