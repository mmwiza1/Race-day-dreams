package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.time.*

abstract class LegacyGymProgrammeRepositoryContract {
    protected abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = CalendarDates.of(2026, 11, 17)

    private fun programme(version: Int = 1, id: String = "programme"): GymProgramme =
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

    private fun <T> storage(block: (SqlDatabase, SqliteLegacyGymProgrammes) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            var id = 0
            return block(db, SqliteLegacyGymProgrammes(db, { at }, { "decision-${++id}" }))
        } finally {
            db.close()
        }
    }

    @Test
    fun versionsStayImmutableAndDisablingNeverDeletesEvidence(): Unit = storage { db, plans ->
        assertNull(plans.current())
        assertTrue(plans.versions().isEmpty())
        val first = programme()
        val second = programme(2).copy(reason = "Explicit future change")
        plans.save(first)
        plans.save(second)
        assertEquals(listOf(second, first), plans.versions())
        assertEquals(first, plans.version("programme", 1))
        assertEquals(second, plans.current())
        plans.disable()
        assertNull(plans.current())
        assertEquals(listOf(second, first), plans.versions())
        assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        assertTrue(db.query("SELECT id FROM manual_results").isEmpty())
    }

    @Test
    fun staleSkippedAndDuplicateVersionsCannotChangeTheCurrentPointer(): Unit =
        storage { _, plans ->
            plans.save(programme())
            for (invalid in
                listOf(programme(), programme(3), programme(2, "other"))) assertFailsWith<
                IllegalArgumentException
            > {
                plans.save(invalid)
            }
            assertEquals(listOf(programme()), plans.versions())
            assertEquals(programme(), plans.current())
            plans.disable()
            assertFails { plans.save(programme()) }
            assertNull(plans.current())
            assertEquals(listOf(programme()), plans.versions())
        }

    @Test
    fun explicitKeepAndIncreaseUseRealDecisionHistoryWithoutFabricatingActuals(): Unit =
        storage { db, plans ->
            plans.save(programme())
            plans.decide(programme(), null, "session", "bench-press", "Exact recorded evidence")
            assertEquals(1, plans.versions().size)
            assertEquals("KEEP", plans.decisions().single().choice)
            assertEquals(at, plans.decisions().single().at)
            plans.decide(programme(), programme(2), "session", "bench-press", "Reviewed increase")
            assertEquals(programme(2), plans.current())
            assertEquals(listOf("KEEP", "INCREASE"), plans.decisions().map { it.choice })
            assertEquals("programme:v2", plans.decisions().last().resultingVersion)
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        }

    @Test
    fun staleOrUnrepresentableChoicesRollBackEveryChange(): Unit = storage { _, plans ->
        plans.save(programme())
        assertFailsWith<IllegalArgumentException> {
            plans.decide(programme(2), programme(3), "session", "bench-press", "stale")
        }
        assertFailsWith<IllegalArgumentException> {
            plans.decide(programme(), programme(2), "session", "bench-press", "")
        }
        assertEquals(programme(), plans.current())
        assertEquals(listOf(programme()), plans.versions())
        assertTrue(plans.decisions().isEmpty())
        assertFailsWith<IllegalArgumentException> {
            plans.save(programme(2).copy(reason = "x".repeat(257)))
        }
        assertEquals(listOf(programme()), plans.versions())
    }

    @Test
    fun decisionInsertFailureRollsBackItsNewVersionAndPointer(): Unit = storage { db, plans ->
        plans.save(programme())
        db.execute(
            "CREATE TRIGGER reject_legacy_decision BEFORE INSERT ON gym_progression_decisions BEGIN SELECT RAISE(ABORT,'test failure'); END"
        )
        assertFails {
            plans.decide(programme(), programme(2), "session", "bench-press", "Evidence")
        }
        assertEquals(programme(), plans.current())
        assertEquals(listOf(programme()), plans.versions())
        assertTrue(plans.decisions().isEmpty())
    }

    @Test
    fun commandsShareTheExistingOuterTransaction(): Unit = storage { db, plans ->
        plans.save(programme())
        assertFails {
            db.transaction {
                plans.decide(programme(), programme(2), "session", "bench-press", "Evidence")
                plans.disable()
                error("Outer failure")
            }
        }
        assertEquals(programme(), plans.current())
        assertEquals(listOf(programme()), plans.versions())
        assertTrue(plans.decisions().isEmpty())
    }

    @Test
    fun exactVersionLookupUsesOriginalWhitespaceAndValidatesIndexedIdentity(): Unit =
        storage { db, plans ->
            for (id in listOf(null, "", "\u2003")) assertFailsWith<IllegalArgumentException> {
                plans.version(id, 1)
            }
            assertFailsWith<IllegalArgumentException> { plans.version("programme", 0) }
            assertNull(plans.version("missing", 1))
            val unusual = programme(id = "\u00a0")
            plans.save(unusual)
            assertEquals(unusual, plans.version("\u00a0", 1))
            plans.disable()
            val first = programme(id = "kept\u0000🏋️")
            plans.save(first)
            assertEquals(first, plans.version(first.id(), 1))
            db.execute(
                "UPDATE gym_plan_versions SET payload=? WHERE version_id=?",
                listOf(
                    SqlValue.Text(
                        LegacyGymProgrammePayloadJson.encode(first.copy(id = "different"))
                    ),
                    SqlValue.Text("${first.id()}:v1"),
                ),
            )
            assertFailsWith<IllegalArgumentException> { plans.version(first.id(), 1) }
            assertFailsWith<IllegalArgumentException> { plans.current() }
            assertEquals(2, db.query("SELECT version_id FROM gym_plan_versions").size)
        }

    @Test
    fun legacyOptionalReferencesAndWholeNumericValuesRemainSupported(): Unit = storage { _, _ ->
        val first = programme()
        val encoded = LegacyGymProgrammePayloadJson.encode(first)
        assertEquals(first, LegacyGymProgrammePayloadJson.decode(encoded))
        assertEquals(
            first,
            LegacyGymProgrammePayloadJson.decode(
                encoded.replace("\"version\":1", "\"version\":1e0")
            ),
        )
        val root = Json.parseToJsonElement(encoded).jsonObject
        val session = root.getValue("sessions").jsonArray.single().jsonObject
        val p = session.getValue("exercises").jsonArray.single().jsonObject
        val old =
            JsonObject(
                    root +
                        ("sessions" to
                            JsonArray(
                                listOf(
                                    JsonObject(
                                        session +
                                            ("exercises" to
                                                JsonArray(listOf(JsonObject(p - "referenceKg"))))
                                    )
                                )
                            ))
                )
                .toString()
        assertNull(
            LegacyGymProgrammePayloadJson.decode(old)
                .sessions()
                .single()
                .exercises()
                .single()
                .referenceLoadKg()
        )
        val decision =
            LegacyGymProgrammePayloadJson.Decision(
                "d",
                "programme:v1",
                "programme:v1",
                "session",
                "bench-press",
                "KEEP",
                "Evidence",
                at,
            )
        assertEquals(
            decision,
            LegacyGymProgrammePayloadJson.decodeDecision(
                LegacyGymProgrammePayloadJson.encodeDecision(decision)
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            LegacyGymProgrammePayloadJson.decodeDecision(
                LegacyGymProgrammePayloadJson.encodeDecision(decision.copy(choice = "AUTO"))
            )
        }
    }

    @Test
    fun duplicateDaysAndOversizeCollectionsRejectWithoutLosingRetainedVersions(): Unit =
        storage { _, plans ->
            plans.save(programme())
            val root =
                Json.parseToJsonElement(LegacyGymProgrammePayloadJson.encode(programme(2)))
                    .jsonObject
            val invalid =
                listOf(
                    JsonObject(
                        root +
                            ("days" to
                                JsonArray(
                                    listOf(JsonPrimitive("TUESDAY"), JsonPrimitive("TUESDAY"))
                                ))
                    ),
                    JsonObject(root + ("sessions" to JsonArray(List(57) { JsonNull }))),
                    JsonObject(
                        root + ("tradeoffs" to JsonArray(List(301) { JsonPrimitive("note") }))
                    ),
                )
            for (payload in invalid) assertFailsWith<IllegalArgumentException> {
                LegacyGymProgrammePayloadJson.decode(payload.toString())
            }
            assertEquals(programme(), plans.current())
            assertEquals(listOf(programme()), plans.versions())
        }

    @Test
    fun activeAuthoredProgrammePreventsLegacyActivationWithoutDeletingEitherHistory(): Unit =
        storage { db, plans ->
            plans.save(programme())
            plans.disable()
            db.execute(
                "INSERT INTO strength_programme_revisions(programme_id,revision,parent_revision,name,start_date,end_date,header) VALUES('modern',1,0,'Modern','2026-11-17','2026-11-23','{}')"
            )
            db.execute(
                "INSERT INTO strength_programme_current(id,programme_id,revision) VALUES(1,'modern',1)"
            )
            assertFailsWith<IllegalArgumentException> { plans.save(programme(2)) }
            assertNull(plans.current())
            assertEquals(listOf(programme()), plans.versions())
            assertEquals(
                listOf(SqlValue.Text("modern")),
                db.query("SELECT programme_id FROM strength_programme_current").single(),
            )
        }
}
