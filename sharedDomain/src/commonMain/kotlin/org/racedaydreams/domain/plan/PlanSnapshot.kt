package org.racedaydreams.domain.plan

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.TrainingPlan

/** Original deterministic engineering snapshot; never changes the plan or actuals. */
object PlanSnapshot {
    @JvmStatic
    fun render(plan: TrainingPlan): String {
        val out = StringBuilder("plan-snapshot-version|1\n")
        row(out, "plan", plan.id(), plan.startDate(), plan.endDate())
        val phases =
            plan
                .phases()
                .sortedWith(
                    compareBy<org.racedaydreams.domain.TrainingPhase> {
                            it.startDate().toEpochDay()
                        }
                        .thenBy { it.endDate().toEpochDay() }
                        .thenBy { it.kind() }
                )
        for (phase in phases) row(
            out,
            "phase",
            phase.kind(),
            phase.startDate(),
            phase.endDate(),
            phase.explanation(),
        )
        val workouts =
            plan
                .workouts()
                .sortedWith(
                    compareBy<org.racedaydreams.domain.PlannedWorkout> { it.date().toEpochDay() }
                        .thenBy { it.id() }
                )
        for (workout in workouts) {
            row(
                out,
                "workout",
                workout.id(),
                workout.date(),
                workout.category(),
                workout.distanceKm(),
                workout.duration(),
                workout.note(),
            )
            for ((index, segment) in workout.segments().withIndex()) row(
                out,
                "segment",
                workout.id(),
                index,
                segment.kind(),
                segment.distanceKm(),
                segment.duration(),
                segment.minimumSecondsPerKm(),
                segment.maximumSecondsPerKm(),
                segment.heartRateZone(),
                segment.rpe(),
                segment.guidance(),
            )
        }
        return out.toString()
    }

    @JvmStatic
    fun difference(expected: String, actual: String): String {
        if (expected == actual) return ""
        val before = expected.split('\n')
        val after = actual.split('\n')
        val out = StringBuilder()
        var changes = 0
        for (index in 0 until maxOf(before.size, after.size)) {
            val left = before.getOrNull(index)
            val right = after.getOrNull(index)
            if (left == right) continue
            if (++changes > 60) {
                out.append("... additional changed lines omitted\n")
                break
            }
            if (left != null)
                out.append('-').append(index + 1).append(' ').append(left).append('\n')
            if (right != null)
                out.append('+').append(index + 1).append(' ').append(right).append('\n')
        }
        return out.toString()
    }

    private fun row(out: StringBuilder, vararg values: Any?) {
        for ((index, value) in values.withIndex()) {
            if (index > 0) out.append('|')
            out.append(escape(value))
        }
        out.append('\n')
    }

    private fun escape(value: Any?): String =
        value
            ?.toString()
            ?.replace("\\", "\\\\")
            ?.replace("|", "\\|")
            ?.replace("~", "\\~")
            ?.replace("\r", "\\r")
            ?.replace("\n", "\\n") ?: "~"
}
