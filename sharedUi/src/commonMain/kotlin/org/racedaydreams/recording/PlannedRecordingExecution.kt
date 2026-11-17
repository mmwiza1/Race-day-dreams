package org.racedaydreams.recording

import kotlinx.serialization.json.*
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.coaching.PlanVersion
import org.racedaydreams.domain.execution.RecordingCueCoordinator
import org.racedaydreams.domain.execution.WorkoutExecutionCoordinator
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.race.RaceStrategyEngine
import org.racedaydreams.storage.PlannedWorkoutJson

/** Existing support payload and step engine for an explicitly selected retained prescription. */
class PlannedRecordingExecution(version: PlanVersion, workoutId: String) {
    private val workout =
        version.plan().workouts().firstOrNull { it.id() == workoutId }
            ?: throw IllegalArgumentException("This workout is no longer in the selected plan.")
    private val goal = version.goalSnapshot().raceGoal()
    private val race = workout.category() == WorkoutCategory.RACE
    private val steps =
        if (race) {
            RaceStrategyEngine.create(
                    goal.distance().kilometres() * 1_000,
                    goal.targetTime()?.getSeconds(),
                    false,
                )
                .map { stage ->
                    WorkoutExecutionEngine.Step(
                        stage.guidance(),
                        WorkoutSegment.Kind.RUN,
                        stage.to() - stage.from(),
                        null,
                        stage.fastPace(),
                        stage.slowPace(),
                        null,
                    )
                }
        } else WorkoutExecutionEngine.from(workout)

    val raceTarget =
        if (race)
            RecordingCueCoordinator.RaceTarget(
                goal.distance().kilometres() * 1_000,
                goal.targetTime()?.getSeconds(),
            )
        else null

    val coordinator = WorkoutExecutionCoordinator(steps)

    private val prescription = buildJsonObject {
        put("version", 1)
        put("mode", if (race) "race" else "workout")
        put("planVersion", version.id())
        put("workoutId", workout.id())
        put("workout", Json.parseToJsonElement(PlannedWorkoutJson.encode(workout)))
        put("goalMetres", goal.distance().kilometres() * 1_000)
        put("raceName", goal.raceName())
        goal.targetTime()?.let { put("targetSeconds", it.getSeconds()) }
        put("hilly", false)
        put(
            "steps",
            buildJsonArray {
                steps.forEach { step ->
                    add(
                        buildJsonObject {
                            put("label", step.label())
                            put("kind", step.kind().name)
                            step.metres()?.let { put("metres", it) }
                            step.millis()?.let { put("millis", it) }
                            step.fastPace()?.let { put("fast", it) }
                            step.slowPace()?.let { put("slow", it) }
                            step.rpe()?.let { put("rpe", it) }
                        }
                    )
                }
            },
        )
    }

    fun snapshot(): String =
        buildJsonObject {
                prescription.forEach { (key, value) -> put(key, value) }
                put(
                    "events",
                    buildJsonArray {
                        coordinator.events().forEach { event ->
                            add(
                                buildJsonObject {
                                    put("type", event.type())
                                    put("step", event.step())
                                    put("active", event.activeMillis())
                                    put("metres", event.metres())
                                }
                            )
                        }
                    },
                )
            }
            .toString()
}
