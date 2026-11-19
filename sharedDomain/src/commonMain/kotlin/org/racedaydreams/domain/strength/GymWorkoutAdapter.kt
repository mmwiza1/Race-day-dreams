package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.javaUtf8
import org.racedaydreams.domain.strength.GymExecution.SourceKind
import org.racedaydreams.domain.strength.GymExecution.SourceReference
import org.racedaydreams.domain.time.Instant

/** Preserved sources enter the same date-free execution model without copying actual sets. */
object GymWorkoutAdapter {
    data class PreparedWorkout(
        private val definition: GymWorkoutDefinition?,
        private val source: SourceReference?,
    ) {
        init {
            if (definition == null) throw NullPointerException("workout definition")
            if (source == null) throw NullPointerException("preserved source")
            require(
                source.kind() != SourceKind.PROGRAMME ||
                    definition.id() == programmeWorkoutId(source) &&
                        definition.revision() == source.revision()
            ) {
                "Programme workout and source revisions disagree."
            }
        }

        fun definition(): GymWorkoutDefinition = definition!!

        fun source(): SourceReference = source!!

        override fun toString(): String = "PreparedWorkout[definition=$definition, source=$source]"
    }

    @JvmStatic
    fun fromProgramme(programme: GymProgramme?, sessionId: String?): PreparedWorkout =
        fromProgramme(programme, sessionId, emptyMap())

    @JvmStatic
    fun fromAuthoredProgramme(programme: StrengthProgramme?, sessionId: String?): PreparedWorkout {
        if (programme == null) throw NullPointerException("preserved authored programme")
        val workout =
            programme.workouts().firstOrNull { it.id() == sessionId }
                ?: throw IllegalArgumentException("The preserved authored session is missing.")
        val source =
            SourceReference(
                SourceKind.PROGRAMME,
                programme.id(),
                programme.revision(),
                workout.id(),
            )
        return PreparedWorkout(
            GymWorkoutDefinition(
                programmeWorkoutId(source),
                programme.revision(),
                programme.createdAt(),
                workout.definition().title(),
                workout.definition().slots(),
            ),
            source,
        )
    }

    /** Explicit custom definitions are frozen at start, never resolved again at finish. */
    @JvmStatic
    fun fromProgramme(
        programme: GymProgramme?,
        sessionId: String?,
        customDefinitions: Map<String, StrengthExercise>?,
    ): PreparedWorkout {
        if (programme == null) throw NullPointerException("programme")
        if (customDefinitions == null) throw NullPointerException("custom definitions")
        // The original loop selects the final match; preserve this even for raw DTO inputs.
        val session =
            programme.sessions().lastOrNull { it.id() == sessionId }
                ?: throw IllegalArgumentException("The preserved programme session is missing.")
        val slots = mutableListOf<GymWorkoutDefinition.Slot>()
        for (prescription in session.exercises()) {
            val canonical = ExerciseCatalog.definition(prescription.exerciseId())
            val exercise =
                if (canonical == null) customDefinitions[prescription.exerciseId()]
                else canonical.exercise()
            require(exercise != null && exercise.id() == prescription.exerciseId()) {
                "The preserved exercise definition is unavailable."
            }
            val rir =
                programme.settings().effort() == GymProgramme.Effort.RIR &&
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
            val target =
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
            slots.add(GymWorkoutDefinition.Slot("slot-${slots.size}", exercise, target, null))
        }
        val source =
            SourceReference(SourceKind.PROGRAMME, programme.id(), programme.version(), session.id())
        return PreparedWorkout(
            GymWorkoutDefinition(
                programmeWorkoutId(source),
                programme.version(),
                programme.createdAt(),
                session.title(),
                slots,
            ),
            source,
        )
    }

    /** A manual workout repeats only its working exercise structure, never its actual sets. */
    @JvmStatic
    fun repeatActual(
        workoutId: String?,
        revision: Int,
        createdAt: Instant?,
        title: String?,
        previous: StrengthSession?,
    ): PreparedWorkout {
        if (previous == null) throw NullPointerException("previous actual workout")
        val slots = mutableListOf<GymWorkoutDefinition.Slot>()
        for (block in previous.exercises()) if (block.sets().any { !it.warmUp() })
            slots.add(GymWorkoutDefinition.Slot("slot-${slots.size}", block.exercise(), null, null))
        require(slots.isNotEmpty()) { "This workout has no working exercise structure to repeat." }
        return PreparedWorkout(
            GymWorkoutDefinition(workoutId, revision, createdAt, title, slots),
            SourceReference(SourceKind.ACTUAL_SESSION, previous.id(), 0, null),
        )
    }

    /** Partial targets survive repeating, but no past load reference becomes a new actual. */
    @JvmStatic
    fun repeatExecution(
        workoutId: String?,
        revision: Int,
        createdAt: Instant?,
        title: String?,
        previous: GymExecution?,
    ): PreparedWorkout {
        if (previous == null) throw NullPointerException("previous execution")
        require(previous.status() == GymExecution.Status.FINISHED) {
            "Finish or recover the previous workout first."
        }
        require(previous.actualSets().any { !it.actual().warmUp() }) {
            "This execution has no working evidence to repeat."
        }
        val slots =
            previous.slots().map {
                GymWorkoutDefinition.Slot(
                    it.id(),
                    it.exercise(),
                    it.target()?.withoutLoadReference(),
                    it.supersetGroup(),
                )
            }
        return PreparedWorkout(
            GymWorkoutDefinition(workoutId, revision, createdAt, title, slots),
            SourceReference(SourceKind.ACTUAL_SESSION, previous.completedSession()!!.id(), 0, null),
        )
    }

    @JvmStatic
    fun programmeWorkoutId(source: SourceReference): String {
        require(source.kind() == SourceKind.PROGRAMME) { "A programme source is required." }
        val value =
            "${source.id().length}:${source.id()}:${source.revision()}:${source.sessionId()}"
        val digest = workoutSha256(javaUtf8(value))
        val digits = "0123456789abcdef"
        return buildString {
            append("programme-session-")
            for (part in digest) {
                val byte = part.toInt()
                append(digits[(byte ushr 4) and 15])
                append(digits[byte and 15])
            }
        }
    }
}

/** System cryptography only; no new dependency or provider-specific storage identity. */
internal expect fun workoutSha256(bytes: ByteArray): ByteArray
