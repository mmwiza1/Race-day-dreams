package org.racedaydreams.recording

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.polish.CustomWorkout
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.storage.SupportArchiveJson

class RecordingExecutionArchiveTest {
    @Test
    fun distanceTargetArchivesStandalonePrescriptionAndCrossing() {
        val execution = QuickRecordingExecution(1_000.0, null)
        execution.coordinator.update(status(0.0, 0, 0))
        execution.coordinator.update(status(900.0, 270_000, 270_000))
        execution.coordinator.update(status(1_100.0, 330_000, 330_000))

        val snapshot = archived(execution)
        val evidence = assertIs<SupportArchiveJson.Evidence.Execution>(snapshot.evidence)
        assertEquals("quick", evidence.mode)
        assertEquals(1_000.0, evidence.steps.single().metres())
        assertNull(evidence.steps.single().millis())
        assertFalse("planVersion" in snapshot.payload)
        assertFalse("workoutId" in snapshot.payload)
        val completion = completedStep(snapshot.payload)
        assertEquals(1_000.0, completion.getValue("metres").jsonPrimitive.double)
        assertEquals(300_000L, completion.getValue("active").jsonPrimitive.long)
    }

    @Test
    fun durationTargetUsesMovingTimeAndArchivesTheCompletion() {
        val execution = QuickRecordingExecution(null, 60_000)
        execution.coordinator.update(status(0.0, 0, 0))
        execution.coordinator.update(status(100.0, 120_000, 30_000, paused = true))
        assertFalse(execution.coordinator.events().any { it.type() == "STEP_COMPLETED" })
        execution.coordinator.update(status(200.0, 150_000, 60_000))

        val snapshot = archived(execution)
        val evidence = assertIs<SupportArchiveJson.Evidence.Execution>(snapshot.evidence)
        assertNull(evidence.steps.single().metres())
        assertEquals(60_000L, evidence.steps.single().millis())
        assertEquals(60_000L, completedStep(snapshot.payload).getValue("active").jsonPrimitive.long)
    }

    @Test
    fun replayRetainsThePrescriptionAndStartsWithNoActualsOrPlanLink() {
        val step =
            WorkoutExecutionEngine.Step(
                "Fast kilometre",
                WorkoutSegment.Kind.RUN,
                1_000.0,
                null,
                280.0,
                300.0,
                7.0,
            )
        val original =
            CustomWorkout("prior", "Intervals", listOf(CustomWorkout.Group(2, listOf(step))))
        val snapshot = archived(CustomRecordingExecution(original))
        val evidence = assertIs<SupportArchiveJson.Evidence.Execution>(snapshot.evidence)
        assertEquals("repeat", evidence.mode)
        assertEquals(listOf(step, step), evidence.steps)
        assertEquals("prior", snapshot.payload.getValue("customId").jsonPrimitive.content)
        assertTrue(snapshot.payload.getValue("events").jsonArray.isEmpty())
        assertFalse("activityId" in snapshot.payload)
        assertFalse("planVersion" in snapshot.payload)
    }

    @Test
    fun authoredManualStepAdvancesOnlyAfterExplicitAction() {
        val open =
            WorkoutExecutionEngine.Step(
                "Warm up",
                WorkoutSegment.Kind.WARM_UP,
                null,
                null,
                null,
                null,
                null,
            )
        val timed =
            WorkoutExecutionEngine.Step(
                "Run",
                WorkoutSegment.Kind.RUN,
                null,
                60_000,
                null,
                null,
                null,
            )
        val workout =
            CustomWorkout(
                "custom-run",
                "Warm up and run",
                listOf(CustomWorkout.Group(1, listOf(open, timed))),
            )
        val execution = CustomRecordingExecution(workout, "custom")
        execution.coordinator.update(status(0.0, 0, 0))
        execution.coordinator.update(status(300.0, 180_000, 180_000))
        assertFalse(execution.coordinator.events().any { it.type() == "STEP_COMPLETED" })

        execution.coordinator.completeOpen()
        execution.coordinator.update(status(500.0, 240_000, 240_000))
        val snapshot = archived(execution)
        val evidence = assertIs<SupportArchiveJson.Evidence.Execution>(snapshot.evidence)
        assertEquals("custom", evidence.mode)
        assertEquals(listOf(open, timed), evidence.steps)
        val completions =
            snapshot.payload
                .getValue("events")
                .jsonArray
                .map { it.jsonObject }
                .filter { it.getValue("type").jsonPrimitive.content == "STEP_COMPLETED" }
        assertEquals(
            listOf(180_000L, 240_000L),
            completions.map { it.getValue("active").jsonPrimitive.long },
        )
    }

    private fun status(metres: Double, elapsed: Long, moving: Long, paused: Boolean = false) =
        RunMetricsEngine.Status(metres, elapsed, moving, null, null, paused)

    private fun completedStep(payload: JsonObject) =
        payload
            .getValue("events")
            .jsonArray
            .map { it.jsonObject }
            .single { it.getValue("type").jsonPrimitive.content == "STEP_COMPLETED" }

    private fun archived(execution: RecordingExecution) =
        SupportArchiveJson.decode(
                buildJsonObject {
                    put("version", 1)
                    put(
                        "rows",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("kind", "execution")
                                    put("id", "quick-run")
                                    put("payload", Json.parseToJsonElement(execution.snapshot()))
                                }
                            )
                        },
                    )
                }
            )
            .records
            .single()
}
