package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.strength.GymExecution.SourceKind
import org.racedaydreams.domain.strength.GymExecution.SourceReference

/** Frozen intentions are validated against their retained source, never today's catalogue. */
object GymWorkoutSourceValidator {
    @JvmStatic
    fun authoredProgramme(
        workout: GymWorkoutDefinition?,
        source: SourceReference?,
        retained: StrengthProgramme?,
    ): StrengthProgramme.Workout {
        require(workout != null && retained != null) {
            "The retained authored programme is required."
        }
        val src = sourceKind(source, SourceKind.PROGRAMME)
        require(retained.id() == src.id() && retained.revision() == src.revision()) {
            "The exact authored programme revision is required."
        }
        val expected = GymWorkoutAdapter.fromAuthoredProgramme(retained, src.sessionId())
        require(src == expected.source() && workout == expected.definition()) {
            "The preserved workout differs from its authored programme source."
        }
        return retained.workouts().firstOrNull { it.id() == src.sessionId() }
            ?: throw IllegalArgumentException("The authored programme session is missing.")
    }

    @JvmStatic
    fun programme(
        workout: GymWorkoutDefinition?,
        source: SourceReference?,
        retained: GymProgramme?,
    ): GymProgramme.Session {
        require(workout != null && retained != null) { "The retained programme is required." }
        val src = sourceKind(source, SourceKind.PROGRAMME)
        require(retained.id() == src.id() && retained.version() == src.revision()) {
            "The exact programme revision is required."
        }
        val session =
            retained.sessions().lastOrNull { it.id() == src.sessionId() }
                ?: throw IllegalArgumentException("The retained programme session is missing.")
        require(
            workout.id() == GymWorkoutAdapter.programmeWorkoutId(src) &&
                workout.revision() == src.revision() &&
                workout.createdAt() == retained.createdAt() &&
                workout.title() == session.title() &&
                workout.slots().size == session.exercises().size
        ) {
            "The preserved workout differs from its programme source."
        }
        for ((i, prescription) in session.exercises().withIndex()) {
            val slot = workout.slots()[i]
            require(
                slot.id() == "slot-$i" &&
                    slot.exercise().id() == prescription.exerciseId() &&
                    slot.supersetGroup() == null
            ) {
                "The prescribed exercise order or identity differs."
            }
            val exercise = slot.exercise()
            val rir =
                retained.settings().effort() == GymProgramme.Effort.RIR &&
                    exercise.measurement().usesReps()
            val effort =
                GymWorkoutDefinition.EffortRange(
                    if (rir) GymWorkoutDefinition.EffortScale.RIR
                    else GymWorkoutDefinition.EffortScale.RPE,
                    (if (rir) 10 - prescription.maximumRpe() else prescription.minimumRpe())
                        .toDouble(),
                    (if (rir) 10 - prescription.minimumRpe() else prescription.maximumRpe())
                        .toDouble(),
                )
            val reference =
                if (
                    exercise.measurement().usesLoad() &&
                        exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
                )
                    prescription.referenceLoadKg()
                else null
            val expected =
                GymWorkoutDefinition.Target(
                    prescription.sets(),
                    prescription.minimumReps().takeIf { it > 0 },
                    prescription.maximumReps().takeIf { prescription.minimumReps() > 0 },
                    prescription.seconds()?.toLong(),
                    prescription.metres(),
                    effort,
                    prescription.restSeconds(),
                    reference,
                )
            require(expected == slot.target()) {
                "The preserved target differs from its programme prescription."
            }
        }
        return session
    }

    @JvmStatic
    fun savedWorkout(
        workout: GymWorkoutDefinition?,
        source: SourceReference?,
        retained: GymWorkoutDefinition?,
    ) {
        require(workout != null && retained != null) {
            "The retained workout revision is required."
        }
        val src = sourceKind(source, SourceKind.SAVED_WORKOUT)
        require(
            retained.id() == src.id() &&
                retained.revision() == src.revision() &&
                retained == workout
        ) {
            "The preserved workout differs from its saved revision."
        }
    }

    @JvmStatic
    fun repeatedActual(
        workout: GymWorkoutDefinition?,
        source: SourceReference?,
        retained: StrengthSession?,
    ) {
        require(workout != null && retained != null) { "The retained actual workout is required." }
        val src = sourceKind(source, SourceKind.ACTUAL_SESSION)
        val expected =
            GymWorkoutAdapter.repeatActual(
                workout.id(),
                workout.revision(),
                workout.createdAt(),
                workout.title(),
                retained,
            )
        require(src == expected.source() && workout == expected.definition()) {
            "The repeated workout differs from its retained actual exercise structure."
        }
    }

    @JvmStatic
    fun repeatedExecution(
        workout: GymWorkoutDefinition?,
        source: SourceReference?,
        retained: GymExecution?,
    ) {
        require(workout != null && retained != null) { "The retained execution is required." }
        val src = sourceKind(source, SourceKind.ACTUAL_SESSION)
        val expected =
            GymWorkoutAdapter.repeatExecution(
                workout.id(),
                workout.revision(),
                workout.createdAt(),
                workout.title(),
                retained,
            )
        require(src == expected.source() && workout == expected.definition()) {
            "The repeated workout differs from its retained execution intentions."
        }
    }

    private fun sourceKind(source: SourceReference?, kind: SourceKind): SourceReference {
        require(source != null && source.kind() == kind) { "The exact workout source is required." }
        return source
    }
}
