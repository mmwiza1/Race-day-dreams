package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.support.DreamRoutes
import org.racedaydreams.domain.support.RunningRoutines

class SupportArchiveJsonTest {
    private val route = DreamRoutes.routes().first().id()

    private fun step() =
        Json.parseToJsonElement(
            """{"label":"Run easily","kind":"RUN","metres":"1000.5","millis":"300000","rpe":null}"""
        )

    private fun execution(mode: String = "quick") = buildJsonObject {
        put("version", 1)
        put("mode", mode)
        put("steps", JsonArray(listOf(step())))
        if (mode in listOf("workout", "race")) {
            put("planVersion", "v1")
            put("workoutId", "run")
        }
        put("interrupted", true)
        put("retainedExtra", "original, not completion")
    }

    private fun row(kind: String, p: JsonObject, id: String = "same") = buildJsonObject {
        put("kind", kind)
        put("id", id)
        put("payload", p)
    }

    private fun root(vararg rows: JsonObject) = buildJsonObject {
        put("version", 1)
        put("rows", JsonArray(rows.toList()))
    }

    private fun change(p: JsonObject, key: String, value: JsonElement) =
        JsonObject(p + (key to value))

    private fun event(step: Int, active: Long, metres: Double, type: String) = buildJsonObject {
        put("step", step)
        put("active", active)
        put("metres", metres)
        put("type", type)
    }

    private fun decode(p: JsonObject) = SupportArchiveJson.decode(root(row("execution", p)))

    @Test
    fun retainedPlannedWorkoutSnapshotIsValidatedWithoutReplacingExecutionTargets() {
        val workout = CoachingArchiveFixtures.workout()
        val p = change(execution("workout"), "workout", PlannedWorkoutJson.element(workout))
        val snapshot = decode(p)
        assertEquals(
            workout,
            PlannedWorkoutJson.read(
                snapshot.records.single().payload.getValue("workout").jsonObject
            ),
        )
        assertEquals(
            1000.5,
            (snapshot.records.single().evidence as SupportArchiveJson.Evidence.Execution)
                .steps
                .single()
                .metres(),
        )
    }

    @Test
    fun assignmentsRoutinesAndAllExecutionModesRetainOriginalEvidence() {
        val dream = buildJsonObject {
            put("version", 1)
            put("route", route)
            put("start", 0)
            put("distance", 5000)
            put("assignedAt", "historic")
        }
        val routine = buildJsonObject {
            put("version", 1)
            put("routine", RunningRoutines.all().first().id())
            put("at", "2026-11-16T10:00:00.123456789Z")
            put("related", JsonNull)
        }
        val original =
            root(
                row("dream", dream),
                row("routine", routine),
                *listOf("quick", "workout", "race", "custom", "repeat")
                    .map { row("execution", execution(it), it) }
                    .toTypedArray(),
            )
        val snapshot = SupportArchiveJson.decode(original)
        assertEquals(original, SupportArchiveJson.element(snapshot))
        assertEquals(7, snapshot.records.size)
        assertEquals(
            5000.0,
            (snapshot.records.first().evidence as SupportArchiveJson.Evidence.Dream)
                .assignment
                .coveredMetres(),
        )
        val saved = snapshot.records.last().evidence as SupportArchiveJson.Evidence.Execution
        assertEquals(1000.5, saved.steps.single().metres())
        assertEquals(300000L, saved.steps.single().millis())
        assertNull(saved.steps.single().rpe())
        assertFalse("events" in snapshot.records.last().payload)
    }

    @Test
    fun terminalStepAndSkippedEventsAreRetainedWithoutAssumingCompletion() {
        val events =
            JsonArray(
                listOf(
                    event(0, 0, 0.0, "WORKOUT_STARTED"),
                    event(0, 3000, 10.0, "STEP_SKIPPED"),
                    event(1, 3000, 10.0, "WORKOUT_FINISHED_EARLY"),
                )
            )
        val p = change(execution(), "events", events)
        assertEquals(p, decode(p).records.single().payload)
        for (bad in
            listOf(
                event(2, 3000, 10.0, "STEP_COMPLETED"),
                event(0, -1, 0.0, "STEP_STARTED"),
                event(0, 3000, -1.0, "STEP_STARTED"),
                event(0, 3000, 10.0, "MADE_UP"),
            )) assertFails { decode(change(execution(), "events", JsonArray(listOf(bad)))) }
        assertFails {
            decode(
                change(
                    execution(),
                    "events",
                    JsonArray(
                        listOf(
                            event(0, 5000, 20.0, "STEP_STARTED"),
                            event(0, 4000, 30.0, "STEP_COMPLETED"),
                        )
                    ),
                )
            )
        }
        assertFails {
            decode(
                change(
                    execution(),
                    "events",
                    JsonArray(
                        listOf(
                            event(0, 5000, 20.0, "STEP_STARTED"),
                            event(0, 6000, 10.0, "STEP_COMPLETED"),
                        )
                    ),
                )
            )
        }
        assertFails {
            decode(
                change(
                    execution(),
                    "events",
                    JsonArray(List(2001) { event(0, 0, 0.0, "STEP_STARTED") }),
                )
            )
        }
    }

    @Test
    fun versionsIdentitiesAndPayloadKindsFailClosed() {
        val r = row("execution", execution())
        assertFails { SupportArchiveJson.decode(root(r, r)) }
        assertFails { SupportArchiveJson.decode(root(change(r, "kind", JsonPrimitive("unknown")))) }
        assertFails { SupportArchiveJson.decode(root(change(r, "id", JsonPrimitive("")))) }
        for (v in listOf(JsonPrimitive(2), JsonPrimitive("1"), JsonPrimitive(1.0))) {
            assertFails { SupportArchiveJson.decode(change(root(r), "version", v)) }
            assertFails { decode(change(execution(), "version", v)) }
        }
        assertFails {
            SupportArchiveJson.decode(change(root(), "rows", JsonArray(List(100001) { r })))
        }
        assertEquals(emptyList(), SupportArchiveJson.decode(root()).records)
    }

    @Test
    fun invalidStepsAndMissingPlannedIdentitiesDoNotBecomeExecutionEvidence() {
        assertFails { decode(execution("free")) }
        assertFails { decode(JsonObject(execution("workout") - "planVersion")) }
        for (steps in
            listOf(
                JsonArray(emptyList()),
                JsonArray(List(501) { step() }),
                JsonArray(listOf(change(step().jsonObject, "metres", JsonPrimitive(-1)))),
                JsonArray(listOf(change(step().jsonObject, "rpe", JsonPrimitive("NaN")))),
            )) assertFails { decode(change(execution(), "steps", steps)) }
        assertFails { decode(change(execution(), "workout", buildJsonObject {})) }
        assertFails {
            SupportArchiveJson.decode(
                root(
                    row(
                        "dream",
                        buildJsonObject {
                            put("version", 1)
                            put("route", route)
                            put("start", 42000)
                            put("distance", 1000)
                        },
                    )
                )
            )
        }
        assertFails {
            SupportArchiveJson.decode(
                root(
                    row(
                        "routine",
                        buildJsonObject {
                            put("version", 1)
                            put("routine", "missing")
                            put("at", "2026-11-16T10:00:00Z")
                        },
                    )
                )
            )
        }
    }

    @Test
    fun originalNumericCoercionsAndUnknownTargetsArePreserved() {
        val p =
            Json.parseToJsonElement(
                    """{"n":4294967297,"s":"4294967297","d":1.9,"ns":"NaN","yes":"TRUE","no":false,"exact":9007199254740993}"""
                )
                .jsonObject
        assertEquals(1, SupportPayloadJson.integer(p, "n"))
        assertEquals(Int.MAX_VALUE, SupportPayloadJson.integer(p, "s"))
        assertEquals(1L, SupportPayloadJson.long(p, "d"))
        assertEquals(9007199254740993L, SupportPayloadJson.long(p, "exact"))
        assertTrue(SupportPayloadJson.bool(p, "yes"))
        assertFalse(SupportPayloadJson.bool(p, "no"))
        assertTrue(SupportPayloadJson.number(p, "ns").isNaN())
        val noTargets =
            change(
                execution(),
                "steps",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("label", "Retained step")
                            put("kind", "RUN")
                        }
                    )
                ),
            )
        val step =
            (decode(noTargets).records.single().evidence as SupportArchiveJson.Evidence.Execution)
                .steps
                .single()
        assertNull(step.metres())
        assertNull(step.millis())
        assertNull(step.rpe())
    }

    @Test
    fun callerMutationCannotChangeRetainedPayloadOrSteps() {
        val map = execution().toMutableMap()
        val snapshot = decode(JsonObject(map))
        map["mode"] = JsonPrimitive("race")
        assertEquals("quick", snapshot.records.single().payload["mode"]!!.jsonPrimitive.content)
        assertFails { (snapshot.records as MutableList).clear() }
        val steps =
            (snapshot.records.single().evidence as SupportArchiveJson.Evidence.Execution).steps
        assertFails { (steps as MutableList).clear() }
    }
}
