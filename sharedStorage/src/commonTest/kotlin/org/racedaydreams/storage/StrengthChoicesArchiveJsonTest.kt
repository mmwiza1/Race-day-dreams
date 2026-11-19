package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason

class StrengthChoicesArchiveJsonTest {
    private fun root(version: Int = 2) = buildJsonObject {
        put("version", version)
        put(
            "choices",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("id", " retained-exercise ")
                        put("favourite", true)
                        put("archived", false)
                    }
                )
            },
        )
        put(
            "acknowledged",
            buildJsonArray {
                add("record-🏋️")
                add(" ")
            },
        )
        if (version == 2)
            put(
                "exclusions",
                buildJsonObject {
                    put("version", 1)
                    put(
                        "entries",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("exerciseId", "retained-exercise")
                                    put("reason", "UNCOMFORTABLE")
                                    put("excludedAt", "2026-11-16T10:00:00.123456789Z")
                                }
                            )
                        },
                    )
                },
            )
    }

    private fun change(p: JsonObject, key: String, value: JsonElement) =
        JsonObject(p + (key to value))

    @Test
    fun olderChoicesAndCurrentReversibleExclusionsRoundTripWithoutTouchingEvidence() {
        for (version in 1..2) {
            val original = root(version)
            val snapshot = StrengthChoicesArchiveJson.decode(original)
            assertEquals(version, snapshot.version)
            assertEquals(original, StrengthChoicesArchiveJson.element(snapshot))
            assertEquals(" retained-exercise ", snapshot.choices.single().exerciseId)
            assertTrue(snapshot.choices.single().favourite)
            assertFalse(snapshot.choices.single().archived)
            assertEquals(listOf("record-🏋️", " "), snapshot.acknowledged)
            if (version == 1) assertTrue(snapshot.exclusions.isEmpty())
            else assertEquals(Reason.UNCOMFORTABLE, snapshot.exclusions.single().reason())
        }
    }

    @Test
    fun unknownCurrentFieldsFutureVersionsAndUnexpectedOlderExclusionsReject() {
        assertFails {
            StrengthChoicesArchiveJson.decode(change(root(), "version", JsonPrimitive(3)))
        }
        assertFails {
            StrengthChoicesArchiveJson.decode(change(root(), "version", JsonPrimitive("2")))
        }
        assertFails {
            StrengthChoicesArchiveJson.decode(change(root(), "extra", JsonPrimitive(true)))
        }
        assertFails {
            StrengthChoicesArchiveJson.decode(change(root(), "version", JsonPrimitive(1)))
        }
        val legacy = change(root(1), "legacyExtra", buildJsonObject { put("retained", true) })
        assertEquals(
            legacy,
            StrengthChoicesArchiveJson.element(StrengthChoicesArchiveJson.decode(legacy)),
        )
    }

    @Test
    fun choicesRequireRealBooleansAndUniqueNonBlankIdentities() {
        val row = root().getValue("choices").jsonArray.single().jsonObject
        for (bad in
            listOf(
                change(row, "id", JsonPrimitive(" ")),
                change(row, "id", JsonPrimitive("x".repeat(201))),
                change(row, "favourite", JsonPrimitive("true")),
                change(row, "archived", JsonNull),
            )) assertFails {
            StrengthChoicesArchiveJson.decode(change(root(), "choices", JsonArray(listOf(bad))))
        }
        assertFails {
            StrengthChoicesArchiveJson.decode(
                change(root(), "choices", JsonArray(listOf(row, row)))
            )
        }
        assertFails {
            StrengthChoicesArchiveJson.decode(
                change(root(), "choices", JsonArray(List(10001) { row }))
            )
        }
    }

    @Test
    fun acknowledgementsRemainBoundedUniqueTextWithoutInventingAchievementEvidence() {
        for (bad in
            listOf(
                JsonArray(listOf(JsonPrimitive(1))),
                JsonArray(listOf(JsonPrimitive(""))),
                JsonArray(listOf(JsonPrimitive("x".repeat(501)))),
                JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("a"))),
                JsonArray(List(100001) { JsonPrimitive("a") }),
            )) assertFails {
            StrengthChoicesArchiveJson.decode(change(root(), "acknowledged", bad))
        }
    }

    @Test
    fun exclusionsPreserveReasonsAndTimestampsAndRejectDuplicatesOrMalformedRows() {
        val p = root().getValue("exclusions").jsonObject
        val e = p.getValue("entries").jsonArray.single().jsonObject
        for (bad in
            listOf(
                change(e, "exerciseId", JsonPrimitive("")),
                change(e, "reason", JsonPrimitive("INJURED")),
                change(e, "excludedAt", JsonPrimitive("unknown")),
                change(e, "extra", JsonPrimitive(true)),
            )) assertFails {
            StrengthChoicesArchiveJson.decode(
                change(root(), "exclusions", change(p, "entries", JsonArray(listOf(bad))))
            )
        }
        assertFails {
            StrengthChoicesArchiveJson.decode(
                change(root(), "exclusions", change(p, "entries", JsonArray(listOf(e, e))))
            )
        }
        assertFails {
            StrengthChoicesArchiveJson.decode(
                change(root(), "exclusions", change(p, "version", JsonPrimitive(2)))
            )
        }
        for (reason in Reason.entries) {
            val out =
                StrengthChoicesArchiveJson.decode(
                    change(
                        root(),
                        "exclusions",
                        change(
                            p,
                            "entries",
                            JsonArray(listOf(change(e, "reason", JsonPrimitive(reason.name)))),
                        ),
                    )
                )
            assertEquals(reason, out.exclusions.single().reason())
        }
    }

    @Test
    fun snapshotDoesNotExposeCallerMutablePayloadOrLists() {
        val map = root().toMutableMap()
        val snapshot = StrengthChoicesArchiveJson.decode(JsonObject(map))
        map["version"] = JsonPrimitive(3)
        assertEquals(
            2,
            StrengthChoicesArchiveJson.element(snapshot).getValue("version").jsonPrimitive.int,
        )
        assertFails { (snapshot.choices as MutableList).clear() }
        assertFails { (snapshot.acknowledged as MutableList).clear() }
        assertFails { (snapshot.exclusions as MutableList).clear() }
    }
}
