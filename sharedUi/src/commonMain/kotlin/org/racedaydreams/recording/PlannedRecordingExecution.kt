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

interface RecordingExecution {
    val mode: String
    val raceTarget: RecordingCueCoordinator.RaceTarget?
    val coordinator: WorkoutExecutionCoordinator

    fun snapshot(): String
}

/** Existing support payload and step engine for an explicitly selected retained prescription. */
class PlannedRecordingExecution(version: PlanVersion, workoutId: String, hilly: Boolean = false) :
    RecordingExecution {
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
                    hilly,
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

    override val mode = if (race) "race" else "workout"

    override val raceTarget =
        if (race)
            RecordingCueCoordinator.RaceTarget(
                goal.distance().kilometres() * 1_000,
                goal.targetTime()?.getSeconds(),
            )
        else null

    override val coordinator = WorkoutExecutionCoordinator(steps)

    private val prescription = buildJsonObject {
        put("version", 1)
        put("mode", mode)
        put("planVersion", version.id())
        put("workoutId", workout.id())
        put("workout", Json.parseToJsonElement(PlannedWorkoutJson.encode(workout)))
        put("goalMetres", goal.distance().kilometres() * 1_000)
        put("raceName", goal.raceName())
        goal.targetTime()?.let { put("targetSeconds", it.getSeconds()) }
        put("hilly", race && hilly)
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

    override fun snapshot(): String = recordingSnapshot(prescription, coordinator)
}

class QuickRecordingExecution(metres: Double?, millis: Long?) : RecordingExecution {
    init {
        require((metres == null) != (millis == null)) { "Choose one distance or duration." }
        require(metres == null || metres in 100.0..100000.0) { "Enter 0.1–100 km." }
        require(millis == null || millis in 60000L..21600000L) { "Enter 1–360 minutes." }
    }

    override val mode = "quick"
    override val raceTarget = null
    private val label = if (metres != null) "Distance target" else "Duration target"
    override val coordinator =
        WorkoutExecutionCoordinator(
            listOf(
                WorkoutExecutionEngine.Step(
                    label,
                    WorkoutSegment.Kind.RUN,
                    metres,
                    millis,
                    null,
                    null,
                    null,
                )
            )
        )
    private val prescription = buildJsonObject {
        put("version", 1)
        put("mode", mode)
        put(
            "steps",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("label", label)
                        put("kind", WorkoutSegment.Kind.RUN.name)
                        metres?.let { put("metres", it) }
                        millis?.let { put("millis", it) }
                    }
                )
            },
        )
    }

    override fun snapshot(): String = recordingSnapshot(prescription, coordinator)
}

private fun recordingSnapshot(
    prescription: JsonObject,
    coordinator: WorkoutExecutionCoordinator,
): String =
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
