package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.StrengthExercise

/** Presentation only: no progression calculations, inferred actuals or RPE-to-RIR conversions. */
object StrengthPrescriptionText {
    @JvmStatic
    fun instructions(slot: GymWorkoutDefinition.Slot): List<String> {
        val target =
            slot.target()
                ?: return Immutable.list(
                    "Choose your own task for this exercise. No set or effort target is prescribed."
                )
        val lines = mutableListOf<String>()
        val task =
            if (target.minimumReps() != null)
                range(target.minimumReps()!!.toDouble(), target.maximumReps()!!.toDouble()) +
                    (if (target.maximumReps() == 1) " repetition" else " repetitions")
            else if (target.seconds() != null) duration(target.seconds()!!)
            else number(target.metres()!!) + " metres"
        lines.add(
            "Complete ${target.sets()}${if(target.sets()==1) " set of " else " sets of "}$task."
        )
        target.restSeconds()?.let {
            lines.add(
                if (it == 0) "No rest is prescribed between sets."
                else "Rest for ${duration(it.toLong())} between sets."
            )
        }
        target.effort()?.let { effort ->
            if (effort.scale() == GymWorkoutDefinition.EffortScale.RIR)
                lines.add(
                    "Finish each set with ${range(effort.minimum(),effort.maximum())} repetitions left before failure."
                )
            else {
                val low = effortWord(effort.minimum())
                val high = effortWord(effort.maximum())
                lines.add(
                    "Aim for ${if(low==high) low else "$low to $high"} perceived effort. Use how hard the set feels to guide your effort."
                )
            }
        }
        if (slot.exercise().mode() == StrengthExercise.Mode.BODYWEIGHT)
            lines.add("Use your body weight; no added weight is prescribed.")
        return Immutable.copyList(lines)
    }

    @JvmStatic
    fun effortDetails(effort: GymWorkoutDefinition.EffortRange?): String {
        if (effort == null) return "No effort target is specified."
        return if (effort.scale() == GymWorkoutDefinition.EffortScale.RPE)
            "RPE (rating of perceived effort): ${range(effort.minimum(),effort.maximum())} out of 10. This describes how hard the set feels."
        else
            "RIR (repetitions in reserve): ${range(effort.minimum(),effort.maximum())} repetitions left before failure. This counts repetitions, not perceived effort."
    }

    @JvmStatic
    fun weight(
        slot: GymWorkoutDefinition.Slot,
        proposed: GymWorkoutDefinition.Target?,
    ): List<String> {
        if (
            !slot.exercise().measurement().usesLoad() ||
                slot.exercise().mode() == StrengthExercise.Mode.BODYWEIGHT
        )
            return Immutable.list("No external weight is prescribed for this exercise.")
        val lines = mutableListOf<String>()
        val current = slot.target()?.referenceLoadKg()
        val next = proposed?.referenceLoadKg()
        val label =
            if (slot.exercise().mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT)
                "added-weight reference"
            else "weight reference"
        lines.add(
            if (current == null) "Your current target has no $label."
            else "Your current $label is ${number(current)} kg."
        )
        if (next != null && current != null && number(next) == number(current))
            lines.add("Your $label is unchanged in this suggestion.")
        else if (next != null)
            lines.add(
                "The suggested new $label is ${number(next)} kg." +
                    if (current != null)
                        " That is ${prescriptionDifference(next,current)} kg ${if(next>=current) "more" else "less"} than your current reference."
                    else ""
            )
        lines.add(
            "A reference is optional, not a weight you must lift or a recorded result. Choose a manageable weight and record what you actually do."
        )
        return Immutable.copyList(lines)
    }

    /** Display comparison only, never a recommendation or an inference from logged weights. */
    data class TargetComparison(
        private val key: String?,
        private val label: String?,
        private val currentValue: String?,
        private val proposedValue: String?,
        private val changed: Boolean,
    ) {
        fun key(): String? = key

        fun label(): String? = label

        fun currentValue(): String? = currentValue

        fun proposedValue(): String? = proposedValue

        fun changed(): Boolean = changed

        override fun toString(): String =
            "TargetComparison[key=$key, label=$label, currentValue=$currentValue, proposedValue=$proposedValue, changed=$changed]"
    }

    @JvmStatic
    fun comparisons(
        slot: GymWorkoutDefinition.Slot,
        proposed: GymWorkoutDefinition.Target?,
    ): List<TargetComparison> {
        val current = slot.target()
        val next = proposed ?: current
        if (current == null && next == null) return Immutable.list()
        val rows = mutableListOf<TargetComparison>()
        row(
            rows,
            "sets",
            "Sets",
            current?.sets()?.let { number(it.toDouble()) },
            next?.sets()?.let { number(it.toDouble()) },
        )
        if (current?.minimumReps() != null || next?.minimumReps() != null)
            row(rows, "repetitions", "Repetitions", reps(current), reps(next))
        if (current?.seconds() != null || next?.seconds() != null)
            row(
                rows,
                "duration",
                "Time per set",
                current?.seconds()?.let { duration(it) },
                next?.seconds()?.let { duration(it) },
            )
        if (current?.metres() != null || next?.metres() != null)
            row(
                rows,
                "distance",
                "Distance per set",
                current?.metres()?.let { "${number(it)} metres" },
                next?.metres()?.let { "${number(it)} metres" },
            )
        row(
            rows,
            "effort",
            "Effort",
            compactEffort(current?.effort()),
            compactEffort(next?.effort()),
        )
        row(
            rows,
            "rest",
            "Rest",
            current?.restSeconds()?.let { duration(it.toLong()) },
            next?.restSeconds()?.let { duration(it.toLong()) },
        )
        if (current?.referenceLoadKg() != null || next?.referenceLoadKg() != null)
            row(
                rows,
                "weight-reference",
                if (slot.exercise().mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT)
                    "Added weight reference"
                else "Weight reference",
                current?.referenceLoadKg()?.let { "${number(it)} kg" },
                next?.referenceLoadKg()?.let { "${number(it)} kg" },
            )
        return Immutable.copyList(rows)
    }

    private fun row(
        rows: MutableList<TargetComparison>,
        key: String,
        label: String,
        current: String?,
        next: String?,
    ) {
        val before = current ?: "Not set"
        val after = next ?: "Not set"
        rows.add(TargetComparison(key, label, before, after, before != after))
    }

    private fun reps(target: GymWorkoutDefinition.Target?): String? =
        target?.minimumReps()?.let { range(it.toDouble(), target.maximumReps()!!.toDouble()) }

    @JvmStatic
    fun compactEffort(effort: GymWorkoutDefinition.EffortRange?): String? =
        effort?.let {
            if (it.scale() == GymWorkoutDefinition.EffortScale.RPE)
                "RPE ${range(it.minimum(),it.maximum())}/10"
            else "RIR ${range(it.minimum(),it.maximum())}"
        }

    @JvmStatic
    fun duration(seconds: Long): String {
        val minutes = seconds / 60
        val remainder = seconds % 60
        val m = "$minutes${if(minutes==1L) " minute" else " minutes"}"
        val s = "$remainder${if(remainder==1L) " second" else " seconds"}"
        return if (minutes == 0L) s else if (remainder == 0L) m else "$m and $s"
    }

    @JvmStatic
    fun effortDescription(value: Double): String =
        if (value.isFinite() && value >= 1 && value <= 10) effortWord(value)
        else "no effort target available"

    private fun effortWord(value: Double): String =
        when {
            value <= 2 -> "very light"
            value <= 4 -> "light"
            value <= 6 -> "moderate"
            value <= 7 -> "challenging"
            value <= 8 -> "hard"
            value < 10 -> "very hard"
            else -> "maximum"
        }

    private fun range(minimum: Double, maximum: Double): String =
        number(minimum) + if (minimum == maximum) "" else "–${number(maximum)}"

    @JvmStatic fun number(value: Double): String = prescriptionNumber(value)
}

/** Platform decimal text only; never used to derive a target, working load or actual result. */
internal expect fun prescriptionNumber(value: Double): String

internal expect fun prescriptionDifference(next: Double, current: Double): String
