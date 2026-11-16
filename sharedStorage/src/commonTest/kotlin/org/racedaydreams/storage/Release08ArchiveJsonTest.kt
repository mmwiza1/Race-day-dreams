package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.support.DreamRoutes

class Release08ArchiveJsonTest {
    private fun p(text: String) = Json.parseToJsonElement(text).jsonObject

    private fun row(kind: String, id: String, payload: JsonObject) = buildJsonObject {
        put("kind", kind)
        put("id", id)
        put("payload", payload)
    }

    private fun root(vararg rows: JsonObject) = buildJsonObject {
        put("version", 1)
        put("rows", JsonArray(rows.toList()))
    }

    private fun change(p: JsonObject, key: String, value: JsonElement) =
        JsonObject(p + (key to value))

    private fun custom() =
        p(
            """{"version":1,"id":"custom","title":"My run","groups":[{"repeats":2,"steps":[{"label":"Run","kind":"RUN","metres":500},{"label":"Recover","kind":"RECOVERY","millis":60000}]}],"extra":"retained"}"""
        )

    private fun decode(kind: String, id: String, p: JsonObject) =
        Release08ArchiveJson.decode(root(row(kind, id, p)))

    @Test
    fun allRecordKindsAndConfigFamiliesRoundTripWithoutChangingProgrammeOrHistory() {
        val route = DreamRoutes.routes().first().id()
        val original =
            root(
                row("custom", "custom", custom()),
                row(
                    "origin",
                    "run",
                    p(
                        """{"version":1,"mode":"free","at":"2026-11-16T10:00:00Z","dreamRoute":"$route"}"""
                    ),
                ),
                row(
                    "association",
                    "actual",
                    p(
                        """{"version":1,"activity":"actual","planVersion":"historic","workout":"run","at":"2026-11-16T10:00:00Z"}"""
                    ),
                ),
                row("checklist", "item", p("""{"version":1,"label":"Shoes","checked":true}""")),
                row(
                    "config",
                    "dream",
                    p("""{"version":1,"type":"dream","enabled":false,"cities":["$route"]}"""),
                ),
                row(
                    "config",
                    "shoe-feedback",
                    p("""{"version":1,"type":"shoe_feedback","activities":["actual"]}"""),
                ),
                row(
                    "config",
                    "schedule",
                    p(
                        """{"version":1,"type":"schedule","maximumMinutes":45,"distribution":"BALANCED","unavailable":["2026-11-17"]}"""
                    ),
                ),
                row("config", "start", p("""{"version":1,"type":"start","path":"hybrid"}""")),
            )
        val snapshot = Release08ArchiveJson.decode(original)
        assertEquals(8, snapshot.records.size)
        assertEquals(original, Release08ArchiveJson.element(snapshot))
        assertEquals(4, snapshot.records.first().custom!!.flattened().size)
        assertEquals(500.0, snapshot.records.first().custom!!.flattened().first().metres())
        assertNull(snapshot.records.last().custom)
    }

    @Test
    fun customGroupsKeepRepeatsAndStepBoundsRatherThanInventingActuals() {
        fun withGroup(repeats: Int, steps: JsonArray) =
            change(
                custom(),
                "groups",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("repeats", repeats)
                            put("steps", steps)
                        }
                    )
                },
            )
        val step = p("""{"label":"Run","kind":"RUN","metres":300000,"millis":21600000}""")
        assertEquals(
            1,
            decode("custom", "custom", withGroup(1, JsonArray(listOf(step))))
                .records
                .single()
                .custom!!
                .flattened()
                .size,
        )
        for (bad in
            listOf(
                withGroup(0, JsonArray(listOf(step))),
                withGroup(101, JsonArray(listOf(step))),
                withGroup(100, JsonArray(List(6) { step })),
                withGroup(1, JsonArray(emptyList())),
                withGroup(1, JsonArray(listOf(change(step, "metres", JsonPrimitive(300001))))),
                withGroup(1, JsonArray(listOf(change(step, "millis", JsonPrimitive(21600001))))),
                change(custom(), "title", JsonPrimitive("")),
                change(custom(), "groups", JsonArray(emptyList())),
            )) assertFails { decode("custom", "custom", bad) }
    }

    @Test
    fun rowIdentityAndConfigTypeMustMatchAndVersionsMustBeOriginal() {
        val r = row("custom", "custom", custom())
        assertFails { Release08ArchiveJson.decode(root(r, r)) }
        assertFails { decode("custom", "other", custom()) }
        assertFails { decode("unknown", "custom", custom()) }
        assertFails {
            decode(
                "association",
                "another",
                p(
                    """{"version":1,"activity":"actual","planVersion":"v","workout":"run","at":"2026-11-16T10:00:00Z"}"""
                ),
            )
        }
        assertFails {
            decode("config", "dream", p("""{"version":1,"type":"start","path":"record"}"""))
        }
        for (v in listOf(JsonPrimitive(2), JsonPrimitive("1"), JsonPrimitive(1.0))) {
            assertFails { Release08ArchiveJson.decode(change(root(r), "version", v)) }
            assertFails { decode("custom", "custom", change(custom(), "version", v)) }
        }
        assertFails {
            Release08ArchiveJson.decode(change(root(), "rows", JsonArray(List(100001) { r })))
        }
    }

    @Test
    fun originModesAndAbsentBlankOrNullDreamChoicesRemainReadable() {
        for (mode in listOf("free", "quick", "custom", "repeat", "workout", "race")) {
            val base = p("""{"version":1,"mode":"$mode","at":"2026-11-16T10:00:00.123456789Z"}""")
            for (value in listOf(null, JsonNull, JsonPrimitive("null"), JsonPrimitive(" "))) {
                val payload = if (value == null) base else change(base, "dreamRoute", value)
                assertEquals(payload, decode("origin", "run", payload).records.single().payload)
            }
            assertFails {
                decode("origin", "run", change(base, "dreamRoute", JsonPrimitive("not-a-city")))
            }
            assertFails { decode("origin", "run", change(base, "at", JsonPrimitive("unknown"))) }
        }
    }

    @Test
    fun preferencesRespectOriginalMembershipUniquenessAndDateBounds() {
        val city = DreamRoutes.routes().first().id()
        for (cities in
            listOf(
                "[\"missing\"]",
                "[\"$city\",\"$city\"]",
                "[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"]",
            )) assertFails {
            decode(
                "config",
                "dream",
                p("""{"version":1,"type":"dream","enabled":true,"cities":$cities}"""),
            )
        }
        for (activities in listOf("[\"a\",\"a\"]", "[\"\"]")) assertFails {
            decode(
                "config",
                "shoe-feedback",
                p("""{"version":1,"type":"shoe_feedback","activities":$activities}"""),
            )
        }
        for (minutes in listOf(14, 361)) assertFails {
            decode(
                "config",
                "schedule",
                p(
                    """{"version":1,"type":"schedule","maximumMinutes":$minutes,"distribution":"BALANCED","unavailable":[]}"""
                ),
            )
        }
        assertFails {
            decode(
                "config",
                "schedule",
                p(
                    """{"version":1,"type":"schedule","maximumMinutes":45,"distribution":"BALANCED","unavailable":["2026-02-30"]}"""
                ),
            )
        }
        assertFails {
            decode("checklist", "item", p("""{"version":1,"label":"Shoes","checked":"unknown"}"""))
        }
        // Original getInt/getBoolean coercions remain valid; repeated unavailable dates were legal.
        decode(
            "config",
            "schedule",
            p(
                """{"version":1,"type":"schedule","maximumMinutes":"45.9","distribution":"BALANCED","unavailable":["2026-11-17","2026-11-17"]}"""
            ),
        )
        decode("checklist", "item", p("""{"version":1,"label":"Shoes","checked":"TRUE"}"""))
    }

    @Test
    fun customSnapshotDoesNotExposeCallerMutablePayloadOrLists() {
        val map = custom().toMutableMap()
        val snapshot = decode("custom", "custom", JsonObject(map))
        map["title"] = JsonPrimitive("changed")
        assertEquals("My run", snapshot.records.single().custom!!.title())
        assertEquals("My run", snapshot.records.single().payload["title"]!!.jsonPrimitive.content)
        assertFails { (snapshot.records as MutableList).clear() }
    }
}
