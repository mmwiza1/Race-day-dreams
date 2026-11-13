package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.Instant

/** Date-free preserved intentions. No actual set is created from a workout definition. */
data class GymWorkoutDefinition(
    private val id: String?,
    private val revision: Int,
    private val createdAt: Instant?,
    private val title: String?,
    private var slots: List<Slot>?,
) {
    enum class EffortScale {
        RPE,
        RIR,
    }

    data class EffortRange(
        private val scale: EffortScale?,
        private val minimum: Double,
        private val maximum: Double,
    ) {
        init {
            if (scale == null) throw NullPointerException("effort scale")
            require(
                minimum.isFinite() &&
                    maximum.isFinite() &&
                    minimum >= 0 &&
                    maximum <= 10 &&
                    maximum >= minimum
            ) {
                "Effort guidance needs an ordered 0–10 range."
            }
        }

        fun scale(): EffortScale = scale!!

        fun minimum(): Double = minimum

        fun maximum(): Double = maximum

        override fun toString(): String =
            "EffortRange[scale=$scale, minimum=$minimum, maximum=$maximum]"
    }

    /** Exactly one measurement is prescribed. A slot with no target is explicitly open. */
    data class Target(
        private val sets: Int,
        private val minimumReps: Int?,
        private val maximumReps: Int?,
        private val seconds: Long?,
        private val metres: Double?,
        private val effort: EffortRange?,
        private val restSeconds: Int?,
        private val referenceLoadKg: Double?,
    ) {
        init {
            require(sets in 1..100) { "Choose 1–100 target sets." }
            require(
                (minimumReps == null) == (maximumReps == null) &&
                    (minimumReps == null ||
                        minimumReps >= 1 && maximumReps!! >= minimumReps && maximumReps <= 500)
            ) {
                "Choose an ordered 1–500 repetition range."
            }
            require(
                (if (minimumReps != null) 1 else 0) +
                    (if (seconds != null) 1 else 0) +
                    (if (metres != null) 1 else 0) == 1 &&
                    (seconds == null || seconds in 1..43200) &&
                    (metres == null || metres.isFinite() && metres > 0 && metres <= 100000)
            ) {
                "Choose one valid repetition, duration or distance target."
            }
            require(restSeconds == null || restSeconds in 0..1800) {
                "Optional rest must be 0–1800 seconds."
            }
            require(
                referenceLoadKg == null ||
                    referenceLoadKg.isFinite() && referenceLoadKg >= 0 && referenceLoadKg <= 1500
            ) {
                "Optional load reference must be 0–1500 kg."
            }
        }

        fun sets(): Int = sets

        fun minimumReps(): Int? = minimumReps

        fun maximumReps(): Int? = maximumReps

        fun seconds(): Long? = seconds

        fun metres(): Double? = metres

        fun effort(): EffortRange? = effort

        fun restSeconds(): Int? = restSeconds

        fun referenceLoadKg(): Double? = referenceLoadKg

        fun withoutLoadReference(): Target =
            Target(sets, minimumReps, maximumReps, seconds, metres, effort, restSeconds, null)

        override fun toString(): String =
            "Target[sets=$sets, minimumReps=$minimumReps, maximumReps=$maximumReps, seconds=$seconds, metres=$metres, effort=$effort, restSeconds=$restSeconds, referenceLoadKg=$referenceLoadKg]"
    }

    /** Frozen exercise definition, independently of the current catalogue or user rename. */
    data class Slot(
        private val id: String?,
        private val exercise: StrengthExercise?,
        private val target: Target?,
        private val supersetGroup: String?,
    ) {
        init {
            requireId(id, "exercise slot")
            if (exercise == null) throw NullPointerException("exercise")
            if (supersetGroup != null) requireId(supersetGroup, "superset group")
            if (target != null) {
                val measurement = exercise.measurement()
                require(
                    measurement.usesReps() == (target.minimumReps() != null) &&
                        measurement.usesDuration() == (target.seconds() != null) &&
                        measurement.usesDistance() == (target.metres() != null)
                ) {
                    "Target does not match the frozen exercise measurement."
                }
                require(
                    target.referenceLoadKg() == null ||
                        measurement.usesLoad() &&
                            exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
                ) {
                    "This exercise does not support an external load reference."
                }
                require(target.effort()?.scale() != EffortScale.RIR || measurement.usesReps()) {
                    "RIR guidance requires repetitions."
                }
            }
        }

        fun id(): String = id!!

        fun exercise(): StrengthExercise = exercise!!

        fun target(): Target? = target

        fun supersetGroup(): String? = supersetGroup

        override fun toString(): String =
            "Slot[id=$id, exercise=$exercise, target=$target, supersetGroup=$supersetGroup]"
    }

    init {
        requireId(id, "workout")
        require(revision >= 1) { "Workout revision must be positive." }
        if (createdAt == null) throw NullPointerException("created at")
        require(title != null && title.trim { it <= ' ' }.isNotEmpty() && title.length <= 120) {
            "Workout needs a title of up to 120 characters."
        }
        slots = Immutable.copyList(slots ?: throw NullPointerException())
        validateSlots(slots!!, false)
    }

    fun id(): String = id!!

    fun revision(): Int = revision

    fun createdAt(): Instant = createdAt!!

    fun title(): String = title!!

    fun slots(): List<Slot> = slots!!

    override fun toString(): String =
        "GymWorkoutDefinition[id=$id, revision=$revision, createdAt=$createdAt, title=$title, slots=$slots]"

    companion object {
        const val FORMAT_VERSION: Int = 1

        @JvmStatic
        fun validateSlots(slots: List<Slot>, allowEmpty: Boolean) {
            require((allowEmpty || slots.isNotEmpty()) && slots.size <= 100) {
                "A workout requires 1–100 exercise slots."
            }
            val ids = mutableSetOf<String>()
            val groupCounts = mutableMapOf<String, Int>()
            val finishedGroups = mutableSetOf<String>()
            var previous: String? = null
            for (slot in slots) {
                if (slot == null) throw NullPointerException("exercise slot")
                require(ids.add(slot.id())) { "Exercise slot IDs must be unique." }
                val group = slot.supersetGroup()
                if (group != previous) {
                    previous?.let { finishedGroups.add(it) }
                    require(group == null || group !in finishedGroups) {
                        "Superset exercises must stay together."
                    }
                }
                if (group != null) groupCounts[group] = (groupCounts[group] ?: 0) + 1
                previous = group
            }
            for (count in groupCounts.values) require(count >= 2) {
                "A superset needs at least two exercises."
            }
        }

        @JvmStatic
        fun requireId(value: String?, label: String) {
            require(value != null && value.trim { it <= ' ' }.isNotEmpty() && value.length <= 256) {
                "A stable $label ID is required."
            }
        }
    }
}
