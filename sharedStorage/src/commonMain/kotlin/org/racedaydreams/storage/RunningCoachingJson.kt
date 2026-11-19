package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.bool
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.integer
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.prose
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.requiredNumber
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.text

/** Existing running coaching payloads; history and recommendations stay separate from actuals. */
object RunningCoachingJson {
    fun encodeVersion(version: PlanVersion): String =
        buildJsonObject {
                val plan = version.plan()
                put("id", version.id())
                put("planId", version.planId())
                put("version", version.version())
                put("createdAt", version.createdAt().toString())
                put("reason", version.reason())
                put("goal", ProfileJson.element(version.goalSnapshot()))
                put("evidence", version.evidenceSnapshot())
                put("start", plan.startDate().toString())
                put("end", plan.endDate().toString())
                put(
                    "phases",
                    buildJsonArray {
                        for (phase in plan.phases()) add(
                            buildJsonObject {
                                put("kind", phase.kind().name)
                                put("start", phase.startDate().toString())
                                put("end", phase.endDate().toString())
                                put("explanation", StoredJson.nullable(phase.explanation()))
                            }
                        )
                    },
                )
                put(
                    "workouts",
                    buildJsonArray {
                        plan.workouts().forEach { add(PlannedWorkoutJson.element(it)) }
                    },
                )
            }
            .toString()

    fun decodeVersion(text: String): PlanVersion {
        val row = StoredJson.objectValue(text)
        val phases =
            row.array("phases", 1000).map {
                val p = it.row()
                TrainingPhase(
                    TrainingPhase.Kind.valueOf(p.required("kind")),
                    StoredDate.parse(p.required("start")),
                    StoredDate.parse(p.required("end")),
                    p.prose("explanation"),
                )
            }
        val workouts = row.array("workouts", 6000).map { PlannedWorkoutJson.read(it.row()) }
        require(
            workouts.map { it.id() }.distinct().size == workouts.size &&
                workouts.map { it.date() }.distinct().size == workouts.size
        ) {
            "Duplicated workout placement."
        }
        val planId = row.required("planId")
        return PlanVersion(
            row.required("id"),
            planId,
            row.requiredInteger("version"),
            StoredInstant.parse(row.required("createdAt")),
            row.prose("reason"),
            ProfileJson.read(row.getValue("goal").row()),
            row.prose("evidence"),
            TrainingPlan(
                planId,
                StoredDate.parse(row.required("start")),
                StoredDate.parse(row.required("end")),
                phases,
                workouts,
            ),
        )
    }

    fun encodeResponse(response: RunExecution): String =
        buildJsonObject {
                put("activityId", response.activityId())
                put("plannedWorkoutId", StoredJson.nullable(response.plannedWorkoutId()))
                put("planVersionId", StoredJson.nullable(response.planVersionId()))
                put(
                    "completedReps",
                    response.completedReps()?.let { JsonPrimitive(it) } ?: JsonNull,
                )
                put(
                    "recoveriesCompleted",
                    response.recoveriesCompleted()?.let { JsonPrimitive(it) } ?: JsonNull,
                )
                put("notes", response.responseNotes())
                put(
                    "steps",
                    buildJsonArray {
                        for (step in response.steps()) add(
                            buildJsonObject {
                                put("index", step.index())
                                put("kind", step.kind().name)
                                put("metres", step.metres())
                                put("activeMillis", step.activeMillis())
                                put("fast", StoredJson.nullable(step.fastPace()))
                                put("slow", StoredJson.nullable(step.slowPace()))
                                put("completed", step.completed())
                                put("reliable", step.reliablePace())
                            }
                        )
                    },
                )
            }
            .toString()

    fun decodeResponse(text: String): RunExecution {
        val row = StoredJson.objectValue(text)
        val steps =
            if (row.containsKey("steps"))
                row.array("steps", 500).map {
                    val step = it.row()
                    RunExecution.StepEvidence(
                        step.requiredInteger("index"),
                        WorkoutSegment.Kind.valueOf(step.required("kind")),
                        step.requiredNumber("metres"),
                        step.exactLong("activeMillis"),
                        step.number("fast"),
                        step.number("slow"),
                        step.bool("completed"),
                        step.bool("reliable"),
                    )
                }
            else emptyList()
        return RunExecution(
            row.required("activityId"),
            row.text("plannedWorkoutId"),
            row.text("planVersionId"),
            row.integer("completedReps"),
            if (row["recoveriesCompleted"] == null || row["recoveriesCompleted"] == JsonNull) null
            else row.bool("recoveriesCompleted"),
            row.prose("notes"),
            steps,
        )
    }

    fun encodeProposal(proposal: AdjustmentProposal): String =
        buildJsonObject {
                put("id", proposal.id())
                put("baseVersionId", proposal.baseVersionId())
                put("workoutId", proposal.workoutId())
                put("createdAt", proposal.createdAt().toString())
                put("reason", proposal.reason())
                put(
                    "evidence",
                    buildJsonArray { proposal.evidence().forEach { add(JsonPrimitive(it)) } },
                )
                put("impact", proposal.impact())
                put("confidence", proposal.confidence())
                put(
                    "options",
                    buildJsonArray {
                        for (option in proposal.options()) add(
                            buildJsonObject {
                                put("id", option.id())
                                put("label", option.label())
                                put("action", option.action().name)
                                put(
                                    "replacement",
                                    option.replacement()?.let { PlannedWorkoutJson.element(it) }
                                        ?: JsonNull,
                                )
                            }
                        )
                    },
                )
            }
            .toString()

    fun decodeProposal(text: String): AdjustmentProposal {
        val row = StoredJson.objectValue(text)
        val evidence =
            row.array("evidence", 100).map {
                require(it is JsonPrimitive && it.isString)
                it.content
            }
        val options =
            row.array("options", 20).map {
                val option = it.row()
                AdjustmentProposal.Option(
                    option.required("id"),
                    option.required("label"),
                    AdjustmentProposal.Action.valueOf(option.required("action")),
                    if (option["replacement"] == null || option["replacement"] == JsonNull) null
                    else PlannedWorkoutJson.read(option.getValue("replacement").row()),
                )
            }
        return AdjustmentProposal(
            row.required("id"),
            row.required("baseVersionId"),
            row.required("workoutId"),
            StoredInstant.parse(row.required("createdAt")),
            row.prose("reason"),
            evidence,
            row.prose("impact"),
            row.prose("confidence"),
            options,
        )
    }

    fun encodeFeedback(feedback: RunFeedback): String =
        buildJsonObject {
                put("ruleVersion", RunScoreEngine.RULE_VERSION)
                put("score", feedback.score()?.let { JsonPrimitive(it) } ?: JsonNull)
                put("label", StoredJson.nullable(feedback.label()))
                put(
                    "components",
                    buildJsonArray {
                        for (component in feedback.components()) add(
                            buildJsonObject {
                                put("name", StoredJson.nullable(component.name()))
                                put("fraction", StoredJson.nullable(component.fraction()))
                                put("explanation", StoredJson.nullable(component.explanation()))
                            }
                        )
                    },
                )
                put(
                    "wentWell",
                    buildJsonArray { feedback.wentWell().forEach { add(JsonPrimitive(it)) } },
                )
                put(
                    "differed",
                    buildJsonArray { feedback.differed().forEach { add(JsonPrimitive(it)) } },
                )
                put("learned", StoredJson.nullable(feedback.learned()))
                put("nextStep", StoredJson.nullable(feedback.nextStep()))
            }
            .toString()
}
