package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
