package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class LegacyGymProgrammeArchiveTest {
    private fun root(decisions: Boolean = true) = buildJsonObject {
        put("version", 1)
        put(
            "versions",
            buildJsonArray {
                add(LegacyGymProgrammePayloadJson.element(LegacyGymArchiveFixtures.programme()))
                add(LegacyGymProgrammePayloadJson.element(LegacyGymArchiveFixtures.programme(2)))
            },
        )
        put("current", "programme:v2")
        if (decisions)
            put(
                "decisions",
                buildJsonArray {
                    add(
                        LegacyGymProgrammePayloadJson.decisionElement(
                            LegacyGymProgrammePayloadJson.Decision(
                                "decision",
                                "programme:v1",
                                "programme:v2",
                                "session",
                                "bench-press",
                                "INCREASE",
                                "Two retained comparable sessions",
                                LegacyGymArchiveFixtures.at,
                            )
                        )
                    )
                },
            )
        put("legacyExtra", "retained")
    }

    private fun change(p: JsonObject, k: String, v: JsonElement) = JsonObject(p + (k to v))

    private fun links(
        version: String? = "programme:v1",
        session: String? = "session",
        date: String = LegacyGymArchiveFixtures.start.toString(),
    ) =
        DevelopmentArchiveJson.decode(
            buildJsonObject {
                put("version", 1)
                put(
                    "records",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("kind", "STRENGTH_LINK")
                                put("id", "actual")
                                put(
                                    "payload",
                                    buildJsonObject {
                                        put("id", "actual")
                                        put("plannedDate", date)
                                        put(
                                            "gymVersion",
                                            version?.let { JsonPrimitive(it) } ?: JsonNull,
                                        )
                                        put(
                                            "gymSession",
                                            session?.let { JsonPrimitive(it) } ?: JsonNull,
                                        )
                                    },
                                )
                            }
                        )
                    },
                )
            }
        )

    @Test
    fun originalRevisionsDecisionsAndUnknownFieldsRoundTripWithoutActivation() {
        val original = root()
        val snapshot = LegacyGymProgrammeArchiveJson.decode(original)
        assertEquals(setOf("programme:v1", "programme:v2"), snapshot.programmes.keys)
        assertEquals("programme:v2", snapshot.current)
        assertEquals("INCREASE", snapshot.decisions.single().choice)
        assertEquals(
            82.25,
            snapshot.programmes
                .getValue("programme:v1")
                .sessions()
                .single()
                .exercises()
                .single()
                .referenceLoadKg(),
        )
        assertEquals(original, LegacyGymProgrammeArchiveJson.element(snapshot))
        assertEquals(
            LegacyGymArchiveFixtures.programme(),
            LegacyGymProgrammePayloadJson.decode(
                LegacyGymProgrammePayloadJson.element(LegacyGymArchiveFixtures.programme())
            ),
        )
    }

    @Test
    fun olderAbsentDecisionsAndDisabledPointersDoNotEraseHistory() {
        val original = change(root(false), "current", JsonNull)
        val snapshot = LegacyGymProgrammeArchiveJson.decode(original)
        assertNull(snapshot.current)
        assertTrue(snapshot.decisions.isEmpty())
        assertEquals(2, snapshot.programmes.size)
        assertEquals(original, LegacyGymProgrammeArchiveJson.element(snapshot))
        assertTrue(LegacyGymProgrammeArchiveJson.decode(null).programmes.isEmpty())
        assertFails { LegacyGymProgrammeArchiveJson.decode(change(root(), "decisions", JsonNull)) }
    }

    @Test
    fun duplicatesFutureVersionsMissingPointersAndExcessiveRowsReject() {
        val p = root().getValue("versions").jsonArray.first()
        for (bad in
            listOf(
                change(root(), "version", JsonPrimitive(2)),
                change(root(), "version", JsonPrimitive("1")),
                change(root(), "versions", JsonArray(listOf(p, p))),
                change(root(), "current", JsonPrimitive("missing:v1")),
                change(root(), "versions", JsonArray(List(1001) { p })),
            )) assertFails { LegacyGymProgrammeArchiveJson.decode(bad) }
    }

    @Test
    fun decisionsNeedUniqueIdsAndRetainedBaseAndResultVersions() {
        val d = root().getValue("decisions").jsonArray.single().jsonObject
        for (ds in
            listOf(
                JsonArray(listOf(d, d)),
                JsonArray(List(100001) { d }),
                JsonArray(listOf(change(d, "base", JsonPrimitive("missing:v1")))),
                JsonArray(listOf(change(d, "result", JsonPrimitive("missing:v2")))),
                JsonArray(listOf(change(d, "choice", JsonPrimitive("APPLY")))),
            )) assertFails { LegacyGymProgrammeArchiveJson.decode(change(root(), "decisions", ds)) }
    }

    @Test
    fun retainedUnusualDecisionDetailsAreNotReinterpretedByArchiveReader() {
        val d = root().getValue("decisions").jsonArray.single().jsonObject
        // Original archive validation checks references, not whether old details form a new
        // proposal.
        val unusual =
            change(
                change(d, "choice", JsonPrimitive("KEEP")),
                "result",
                JsonPrimitive("programme:v2"),
            )
        val snapshot =
            LegacyGymProgrammeArchiveJson.decode(
                change(root(), "decisions", JsonArray(listOf(unusual)))
            )
        assertEquals("programme:v2", snapshot.decisions.single().resultingVersion)
        assertEquals("KEEP", snapshot.decisions.single().choice)
    }

    @Test
    fun linkedActualNeedsExactRetainedSessionDateAndActualIdentity() {
        val snapshot = LegacyGymProgrammeArchiveJson.decode(root())
        LegacyGymProgrammeArchiveJson.validateLinks(snapshot, links(), setOf("actual"))
        for (bad in
            listOf(
                links("missing:v1"),
                links(session = "missing"),
                links(date = "2026-11-18"),
            )) assertFails {
            LegacyGymProgrammeArchiveJson.validateLinks(snapshot, bad, setOf("actual"))
        }
        assertFails { LegacyGymProgrammeArchiveJson.validateLinks(snapshot, links(), emptySet()) }
        LegacyGymProgrammeArchiveJson.validateLinks(snapshot, links(null, null), emptySet())
        LegacyGymProgrammeArchiveJson.validateLinks(snapshot, null, emptySet())
    }

    @Test
    fun authoredLinkUsesExactAuthoredWorkoutAndNeverFallsBackToUnrelatedLegacyTarget() {
        val p = LegacyGymArchiveFixtures.programme()
        val at = p.createdAt()
        val day = p.start()
        val slots =
            listOf(
                GymWorkoutDefinition.Slot(
                    "slot",
                    ExerciseCatalog.definition("bench-press")!!.exercise(),
                    GymWorkoutDefinition.Target(2, 4, 6, null, null, null, 180, null),
                    null,
                )
            )
        val workout =
            StrengthProgramme.Workout(
                "session",
                day,
                StrengthWorkoutBudget.workoutMinutes(slots),
                GymWorkoutDefinition("session", 1, at, "Upper", slots),
                null,
            )
        val authored =
            StrengthProgramme(
                "authored",
                1,
                0,
                at,
                "Authored",
                StrengthProgramme.Origin.CUSTOM,
                null,
                StrengthProgramme.Settings(
                    GymProgramme.Goal.STRENGTH,
                    GymProgramme.Priority.BALANCED,
                    GymProgramme.Experience.INTERMEDIATE,
                    setOf(DayOfWeek.TUESDAY),
                    1,
                    60,
                    ExerciseMetadata.Equipment.entries.toSet(),
                ),
                listOf(
                    StrengthProgramme.Block(
                        "block",
                        StrengthProgramme.Phase.DEVELOPMENT,
                        "Development",
                        day,
                        1,
                        listOf(workout),
                        "Original",
                    )
                ),
                day,
                "Athlete authored",
                emptyList(),
            )
        val snapshot = LegacyGymProgrammeArchiveJson.decode(null)
        LegacyGymProgrammeArchiveJson.validateLinks(
            snapshot,
            links("authored:v1"),
            setOf("actual"),
            mapOf("authored:v1" to authored),
        )
        assertFails {
            LegacyGymProgrammeArchiveJson.validateLinks(
                snapshot,
                links("authored:v1", date = "2026-11-18"),
                setOf("actual"),
                mapOf("authored:v1" to authored),
            )
        }
        assertFails {
            LegacyGymProgrammeArchiveJson.validateLinks(
                snapshot,
                links("authored:v1"),
                setOf("other"),
                mapOf("authored:v1" to authored),
            )
        }
    }

    @Test
    fun snapshotsKeepPrivatePayloadCopiesAndImmutableCollections() {
        val map = root().toMutableMap()
        val snapshot = LegacyGymProgrammeArchiveJson.decode(JsonObject(map))
        map["current"] = JsonNull
        assertEquals("programme:v2", snapshot.current)
        assertEquals(
            "programme:v2",
            LegacyGymProgrammeArchiveJson.element(snapshot)
                .getValue("current")
                .jsonPrimitive
                .content,
        )
        assertFails { (snapshot.programmes as MutableMap).clear() }
        assertFails { (snapshot.decisions as MutableList).clear() }
    }
}
