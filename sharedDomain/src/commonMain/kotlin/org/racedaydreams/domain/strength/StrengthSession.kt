package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Actual retained workout evidence. Prescriptions and recommendations do not create these sets. */
data class StrengthSession(
    private val id: String?,
    private val startedAt: Instant?,
    private val offset: ZoneOffset?,
    private val duration: Duration?,
    private val rpe: Double?,
    private var notes: String?,
    private var exercises: List<ExerciseSets>?,
) {
    data class SetEntry(
        private val id: String?,
        private val reps: Int,
        private val kilograms: Double,
        private val rpe: Double?,
        private val warmUp: Boolean,
        private val durationSeconds: Long?,
        private val distanceMetres: Double?,
        private val repetitionsInReserve: Double?,
    ) {
        constructor(
            id: String?,
            reps: Int,
            kilograms: Double,
            rpe: Double?,
            warmUp: Boolean,
            durationSeconds: Long?,
            distanceMetres: Double?,
        ) : this(id, reps, kilograms, rpe, warmUp, durationSeconds, distanceMetres, null)

        constructor(
            id: String?,
            reps: Int,
            kilograms: Double,
            rpe: Double?,
            warmUp: Boolean,
        ) : this(id, reps, kilograms, rpe, warmUp, null, null, null)

        init {
            require(
                id != null &&
                    id.trim { it <= ' ' }.isNotEmpty() &&
                    reps in 0..500 &&
                    kilograms.isFinite() &&
                    kilograms >= 0 &&
                    kilograms <= 1500
            ) {
                "Check set ID, reps and load."
            }
            require(durationSeconds == null || durationSeconds > 0 && durationSeconds <= 43200) {
                "Check set duration."
            }
            require(
                distanceMetres == null ||
                    distanceMetres.isFinite() && distanceMetres > 0 && distanceMetres <= 100000
            ) {
                "Check set distance."
            }
            require(
                (if (reps > 0) 1 else 0) +
                    (if (durationSeconds != null) 1 else 0) +
                    (if (distanceMetres != null) 1 else 0) == 1
            ) {
                "Record exactly one of repetitions, duration or distance."
            }
            require(
                repetitionsInReserve == null ||
                    repetitionsInReserve.isFinite() &&
                        repetitionsInReserve >= 0 &&
                        repetitionsInReserve <= 10 &&
                        reps != 0 &&
                        rpe == null
            ) {
                "Record RPE or rep-based RIR, not both."
            }
            checkRpe(rpe)
        }

        fun coachingRpe(): Double? = rpe ?: repetitionsInReserve?.let { 10 - it }

        fun id(): String = id!!

        fun reps(): Int = reps

        fun kilograms(): Double = kilograms

        fun rpe(): Double? = rpe

        fun warmUp(): Boolean = warmUp

        fun durationSeconds(): Long? = durationSeconds

        fun distanceMetres(): Double? = distanceMetres

        fun repetitionsInReserve(): Double? = repetitionsInReserve

        override fun toString(): String =
            "SetEntry[id=$id, reps=$reps, kilograms=$kilograms, rpe=$rpe, warmUp=$warmUp, durationSeconds=$durationSeconds, distanceMetres=$distanceMetres, repetitionsInReserve=$repetitionsInReserve]"
    }

    data class ExerciseSets(
        private val exercise: StrengthExercise?,
        private var sets: List<SetEntry>?,
    ) {
        init {
            if (exercise == null) throw NullPointerException()
            sets = Immutable.copyList(sets ?: throw NullPointerException())
            require(sets!!.size in 1..100) { "Record 1–100 sets per exercise." }
            for (set in sets!!) {
                val profile = exercise.measurement()
                require(
                    profile.usesReps() == (set.reps() > 0) &&
                        profile.usesDuration() == (set.durationSeconds() != null) &&
                        profile.usesDistance() == (set.distanceMetres() != null)
                ) {
                    "Set does not match the exercise measurement profile."
                }
                require(profile.usesLoad() || set.kilograms() == 0.0) {
                    "This exercise does not use external load."
                }
                require(
                    exercise.mode() != StrengthExercise.Mode.BODYWEIGHT || set.kilograms() == 0.0
                ) {
                    "Use a weighted-bodyweight exercise for added load."
                }
            }
        }

        fun exercise(): StrengthExercise = exercise!!

        fun sets(): List<SetEntry> = sets!!

        override fun toString(): String = "ExerciseSets[exercise=$exercise, sets=$sets]"
    }

    init {
        require(id != null && id.trim { it <= ' ' }.isNotEmpty()) { "Session ID is required." }
        if (startedAt == null) throw NullPointerException()
        if (offset == null) throw NullPointerException()
        if (duration == null) throw NullPointerException()
        require(!duration.isNegative() && !duration.isZero() && duration <= Durations.ofHours(12)) {
            "Session duration must be positive and at most 12 hours."
        }
        checkRpe(rpe)
        notes = notes ?: ""
        require(notes!!.length <= 10000) { "Notes are too long." }
        exercises = Immutable.copyList(exercises ?: throw NullPointerException())
        require(exercises!!.size in 1..100) { "Record 1–100 exercises." }
        val ids = mutableSetOf<String>()
        for (exercise in exercises!!) for (set in exercise.sets()) require(ids.add(set.id())) {
            "Set IDs must be unique within a workout."
        }
    }

    fun id(): String = id!!

    fun startedAt(): Instant = startedAt!!

    fun offset(): ZoneOffset = offset!!

    fun duration(): Duration = duration!!

    fun rpe(): Double? = rpe

    fun notes(): String = notes!!

    fun exercises(): List<ExerciseSets> = exercises!!

    override fun toString(): String =
        "StrengthSession[id=$id, startedAt=$startedAt, offset=$offset, duration=$duration, rpe=$rpe, notes=$notes, exercises=$exercises]"

    companion object {
        @JvmStatic
        fun checkRpe(rpe: Double?) {
            require(rpe == null || rpe.isFinite() && rpe >= 0 && rpe <= 10) {
                "RPE must be between 0 and 10."
            }
        }
    }
}
