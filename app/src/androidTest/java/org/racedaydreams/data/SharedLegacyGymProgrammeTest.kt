package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedLegacyGymProgrammeTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val at = Instant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = LocalDate.of(2026, 11, 17)
    private lateinit var original: GymProgrammeStore
    private lateinit var shared: SqliteLegacyGymProgrammes
    private lateinit var bridge: AndroidSqlTestConnection

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        bridge = AndroidSqlTestConnection(ProfileDatabase.get(context).writableDatabase)
        original = GymProgrammeStore(context)
        shared = SqliteLegacyGymProgrammes(bridge, { at }, { "shared-decision" })
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun programme(version: Int = 1, id: String = "programme") =
        GymProgramme(
            id,
            version,
            at,
            "Athlete accepted",
            GymProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.TUESDAY),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RIR,
            ),
            start,
            start.plusDays(41),
            listOf(
                GymProgramme.Session(
                    "session",
                    start,
                    "Upper",
                    listOf(
                        GymProgramme.Prescription(
                            "bench-press",
                            2,
                            4,
                            6,
                            null,
                            null,
                            6,
                            8,
                            180,
                            "Retained intention",
                            82.25,
                        )
                    ),
                    "Original explanation",
                )
            ),
            listOf("Retained trade-off"),
        )

    @Test
    fun archiveMatchesOriginalExportAndRejectsBrokenRevisionGraphsWithoutChangingHistory() {
        original.save(programme())
        original.decide(programme(), programme(2), "session", "bench-press", "Reviewed evidence")
        val db = ProfileDatabase.get(context).writableDatabase
        val before = GymProgrammeStore.exportData(db)
        GymProgrammeStore.validate(before)
        val snapshot =
            LegacyGymProgrammeArchiveJson.decode(
                Json.parseToJsonElement(before.toString()) as JsonObject
            )
        assertEquals(original.versions().toSet(), snapshot.programmes.values.toSet())
        assertEquals("programme:v2", snapshot.current)
        assertEquals(
            Json.parseToJsonElement(before.toString()),
            LegacyGymProgrammeArchiveJson.element(snapshot),
        )
        GymProgrammeStore.validate(
            JSONObject(LegacyGymProgrammeArchiveJson.element(snapshot).toString())
        )
        assertEquals(
            LegacyGymProgrammeArchiveJson.element(snapshot),
            LegacyGymProgrammeArchiveJson.element(shared.archiveSnapshot()),
        )
        val old =
            JSONObject(before.toString()).apply {
                remove("decisions")
                put("current", JSONObject.NULL)
            }
        GymProgrammeStore.validate(old)
        assertEquals(
            Json.parseToJsonElement(old.toString()),
            LegacyGymProgrammeArchiveJson.element(
                LegacyGymProgrammeArchiveJson.decode(
                    Json.parseToJsonElement(old.toString()) as JsonObject
                )
            ),
        )
        for (bad in
            listOf(
                JSONObject(before.toString()).put("version", 2),
                JSONObject(before.toString()).put("current", "missing:v1"),
                JSONObject(before.toString()).apply {
                    getJSONArray("versions").put(getJSONArray("versions").getJSONObject(0))
                },
                JSONObject(before.toString()).apply {
                    getJSONArray("decisions").getJSONObject(0).put("base", "missing:v1")
                },
            )) {
            assertTrue(runCatching { GymProgrammeStore.validate(bad) }.isFailure)
            assertTrue(
                runCatching {
                        LegacyGymProgrammeArchiveJson.decode(
                            Json.parseToJsonElement(bad.toString()) as JsonObject
                        )
                    }
                    .isFailure
            )
        }
        assertEquals(before.toString(), GymProgrammeStore.exportData(db).toString())
        assertTrue(bridge.query("SELECT id FROM strength_sessions").isEmpty())
    }

    @Test
    fun archiveLinkGuardsMatchOriginalActualSessionAndDateReferencesWithoutInventingRecordedLoad() {
        original.save(programme())
        val actual =
            StrengthSession(
                "actual",
                at,
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "Recorded separately",
                listOf(
                    StrengthSession.ExerciseSets(
                        ExerciseCatalog.definition("bench-press")!!.exercise(),
                        listOf(StrengthSession.SetEntry("working", 5, 50.0, 8.0, false)),
                    )
                ),
            )
        CoachingStore(context).saveStrength(actual)
        val db = ProfileDatabase.get(context).writableDatabase
        val gym = GymProgrammeStore.exportData(db)
        val coaching = CoachingStore.exportData(db)
        val payload =
            JSONObject()
                .put("id", "actual")
                .put("plannedDate", start.toString())
                .put("gymVersion", "programme:v1")
                .put("gymSession", "session")
        fun evidence(p: JSONObject) =
            JSONObject()
                .put("version", 1)
                .put(
                    "records",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("kind", "STRENGTH_LINK")
                                .put("id", p.getString("id"))
                                .put("payload", p)
                        ),
                )
        val development = evidence(payload)
        DevelopmentStore.validate(development)
        GymProgrammeStore.validateLinks(development, coaching, gym)
        val snapshot =
            LegacyGymProgrammeArchiveJson.decode(
                Json.parseToJsonElement(gym.toString()) as JsonObject
            )
        fun sharedCheck(j: JSONObject) =
            LegacyGymProgrammeArchiveJson.validateLinks(
                snapshot,
                DevelopmentArchiveJson.decode(Json.parseToJsonElement(j.toString()) as JsonObject),
                setOf("actual"),
            )
        sharedCheck(development)
        for (p in
            listOf(
                JSONObject(payload.toString()).put("id", "missing-actual"),
                JSONObject(payload.toString()).put("gymSession", "missing-session"),
                JSONObject(payload.toString()).put("gymVersion", "missing:v1"),
                JSONObject(payload.toString()).put("plannedDate", start.plusDays(1).toString()),
            )) {
            val bad = evidence(p)
            DevelopmentStore.validate(bad)
            assertTrue(
                runCatching { GymProgrammeStore.validateLinks(bad, coaching, gym) }.isFailure
            )
            assertTrue(runCatching { sharedCheck(bad) }.isFailure)
        }
        assertEquals(coaching.toString(), CoachingStore.exportData(db).toString())
        assertEquals(
            50.0,
            CoachingStore(context)
                .strength()
                .single()
                .exercises()
                .single()
                .sets()
                .single()
                .kilograms(),
            0.0,
        )
    }

    @Test
    fun originalAndSharedCodecsAndWritersRetainImmutableVersionsAndDisableOnlyThePointer() {
        val first = programme()
        assertEquals(
            first,
            LegacyGymProgrammePayloadJson.decode(GymProgrammeStore.json(first).toString()),
        )
        assertEquals(
            first,
            GymProgrammeStore.parse(JSONObject(LegacyGymProgrammePayloadJson.encode(first))),
        )
        original.save(first)
        shared.save(programme(2))
        original.save(programme(3))
        assertEquals(original.current(), shared.current())
        assertEquals(original.versions(), shared.versions())
        assertEquals(first, original.version("programme", 1))
        assertEquals(first, shared.version("programme", 1))
        shared.disable()
        assertNull(original.current())
        assertEquals(3, original.versions().size)
        assertEquals(original.versions(), shared.versions())
        assertTrue(bridge.query("SELECT id FROM strength_sessions").isEmpty())
    }

    @Test
    fun explicitProgressionDecisionsAreReadableInBothDirectionsWithoutInventingActuals() {
        original.save(programme())
        original.decide(programme(), null, "session", "bench-press", "Original evidence")
        shared.decide(programme(), programme(2), "session", "bench-press", "Reviewed evidence")
        assertEquals(original.current(), shared.current())
        val expected =
            original.decisions().map {
                LegacyGymProgrammePayloadJson.Decision(
                    it.id(),
                    it.baseVersion(),
                    it.resultingVersion(),
                    it.prescribedSession(),
                    it.exerciseId(),
                    it.choice(),
                    it.evidence(),
                    it.at(),
                )
            }
        assertEquals(expected, shared.decisions())
        assertEquals(listOf("KEEP", "INCREASE"), shared.decisions().map { it.choice })
        assertTrue(bridge.query("SELECT id FROM strength_sessions").isEmpty())
        assertTrue(bridge.query("SELECT id FROM manual_results").isEmpty())
    }

    @Test
    fun originalAndSharedWritesRespectOneOuterTransactionAndOriginalIdentityRules() {
        val db = ProfileDatabase.get(context).writableDatabase
        db.beginTransaction()
        try {
            original.save(programme())
            shared.decide(programme(), programme(2), "session", "bench-press", "Evidence")
        } finally {
            db.endTransaction()
        }
        assertNull(original.current())
        assertNull(shared.current())
        assertTrue(shared.versions().isEmpty())
        assertTrue(original.decisions().isEmpty())
        for (id in listOf("\u00a0", "kept\u0000🏋️")) {
            val plan = programme(id = id)
            original.save(plan)
            assertEquals(plan, shared.version(id, 1))
            shared.disable()
            assertEquals(plan, original.version(id, 1))
        }
        assertThrows(IllegalArgumentException::class.java) { original.version("\u2003", 1) }
        assertThrows(IllegalArgumentException::class.java) { shared.version("\u2003", 1) }
    }
}
