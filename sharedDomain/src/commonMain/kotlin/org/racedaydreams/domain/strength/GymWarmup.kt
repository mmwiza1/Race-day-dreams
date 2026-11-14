package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import kotlin.math.floor
import kotlin.math.min
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Original conservative preparation heuristic. Suggestions are never actual performed evidence. */
object GymWarmup {
    data class Ramp(private val kilograms: Double, private val repetitions: Int) {
        fun kilograms(): Double = kilograms

        fun repetitions(): Int = repetitions

        override fun toString(): String = "Ramp[kilograms=$kilograms, repetitions=$repetitions]"
    }

    data class Suggestion(
        private val instruction: String?,
        private val why: String?,
        private var ramps: List<Ramp>?,
    ) {
        init {
            ramps = Immutable.copyList(ramps ?: throw NullPointerException())
        }

        fun instruction(): String? = instruction

        fun why(): String? = why

        fun ramps(): List<Ramp> = ramps!!

        override fun toString(): String =
            "Suggestion[instruction=$instruction, why=$why, ramps=$ramps]"
    }

    @JvmStatic
    fun suggest(
        slot: GymWorkoutDefinition.Slot?,
        history: List<StrengthSession>,
        today: List<GymExecutionSet>,
        experience: GymProgramme.Experience?,
        now: Instant?,
    ): Suggestion {
        if (slot == null) throw NullPointerException()
        if (now == null) throw NullPointerException()
        if (experience == null) throw NullPointerException()
        if (today.any { it.exercise() == slot.exercise() && !it.actual().warmUp() })
            return Suggestion(
                "You have already recorded working sets for this exercise. Prepare again only if useful.",
                "No additional ramp is suggested after working sets.",
                Immutable.list(),
            )
        val metadata = ExerciseMetadataCatalog.find(slot.exercise().id())
        if (
            metadata != null &&
                today
                    .filter { !it.actual().warmUp() }
                    .mapNotNull { ExerciseMetadataCatalog.find(it.exercise().id()) }
                    .any { it.primaryRole() == metadata.primaryRole() }
        )
            return Suggestion(
                "You have practised a related movement. Start with light, controlled preparation if needed.",
                "Related recorded work reduces the need for another numerical ramp.",
                Immutable.list(),
            )
        val fallback =
            Suggestion(
                "Practise the movement gently before working sets. Choose a light load or an easier version; stop if uncomfortable.",
                "No trustworthy recent exact-exercise working-load evidence is available. No kilograms are invented.",
                Immutable.list(),
            )
        if (
            metadata == null ||
                ExerciseMetadata.Role.PRIMARY_STRENGTH !in metadata.roles() ||
                !slot.exercise().measurement().usesReps() ||
                slot.exercise().mode() != StrengthExercise.Mode.WEIGHTED ||
                ExerciseMetadata.Role.POWER in metadata.roles()
        )
            return fallback
        val recent =
            history
                .filter {
                    !it.startedAt().isAfter(now) &&
                        !it.startedAt()
                            .isBefore(
                                Instants.ofEpochSecond(
                                    now.getEpochSecond() - 28L * 24 * 60 * 60,
                                    now.getNano().toLong(),
                                )
                            )
                }
                .sortedByDescending { it.startedAt() }
        for (session in recent) {
            val eligible = mutableListOf<StrengthSession.SetEntry>()
            for (exercise in session.exercises()) if (exercise.exercise() == slot.exercise())
                for (set in exercise.sets()) {
                    val effort = set.coachingRpe()
                    if (
                        !set.warmUp() &&
                            set.kilograms() > 0 &&
                            set.reps() in 3..12 &&
                            effort != null &&
                            effort >= 4 &&
                            effort <= 8
                    )
                        eligible.add(set)
                }
            if (eligible.size < 2) continue
            var working = eligible.minOf { it.kilograms() }
            slot.target()?.referenceLoadKg()?.let { working = min(working, it) }
            if (working < 10) return fallback
            val ramps =
                Immutable.list(
                    Ramp(floor(working * .4 * 2) / 2, 6),
                    Ramp(
                        floor(working * .65 * 2) / 2,
                        if (experience == GymProgramme.Experience.BEGINNER) 4 else 3,
                    ),
                )
            return Suggestion(
                "Optional light practice sets before your working sets. Adjust for the equipment and how the movement feels.",
                "Two recent exact-exercise working sets with recorded effort support these lower-load examples. They are not a working-weight recommendation or actual results.",
                ramps,
            )
        }
        return fallback
    }
}
