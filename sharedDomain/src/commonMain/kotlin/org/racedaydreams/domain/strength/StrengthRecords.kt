package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import kotlin.math.round
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Measured and estimated records retain exact source actuals. No prescription is an input. */
object StrengthRecords {
    enum class Kind {
        HEAVIEST_LOAD,
        EXACT_REPETITIONS_LOAD,
        REPETITIONS_AT_LOAD,
        BODYWEIGHT_REPETITIONS,
        DURATION,
        DISTANCE,
        ESTIMATED_ONE_REP_MAX,
    }

    data class Key(
        private val exerciseId: String?,
        private val mode: StrengthExercise.Mode?,
        private val measurement: StrengthExercise.MeasurementProfile?,
        private val kind: Kind?,
        private val repetitions: Int?,
        private val load: Double?,
    ) {
        fun exerciseId(): String? = exerciseId

        fun mode(): StrengthExercise.Mode? = mode

        fun measurement(): StrengthExercise.MeasurementProfile? = measurement

        fun kind(): Kind? = kind

        fun repetitions(): Int? = repetitions

        fun load(): Double? = load

        override fun toString(): String =
            "Key[exerciseId=$exerciseId, mode=$mode, measurement=$measurement, kind=$kind, repetitions=$repetitions, load=$load]"
    }

    data class Point(
        private val key: Key?,
        private val value: Double,
        private val exercise: StrengthExercise?,
        private val actual: StrengthSession.SetEntry?,
        private val workoutId: String?,
        private val at: Instant?,
        private val date: LocalDate?,
    ) {
        fun key(): Key? = key

        fun value(): Double = value

        fun exercise(): StrengthExercise? = exercise

        fun actual(): StrengthSession.SetEntry? = actual

        fun workoutId(): String? = workoutId

        fun at(): Instant? = at

        fun date(): LocalDate? = date

        fun estimated(): Boolean = key!!.kind() == Kind.ESTIMATED_ONE_REP_MAX

        override fun toString(): String =
            "Point[key=$key, value=$value, exercise=$exercise, actual=$actual, workoutId=$workoutId, at=$at, date=$date]"
    }

    @JvmStatic
    fun progression(sessions: List<StrengthSession>, asOf: Instant?): List<Point> {
        val sorted =
            sessions.sortedWith(compareBy<StrengthSession> { it.startedAt() }.thenBy { it.id() })
        val seen = mutableMapOf<String, StrengthSession>()
        val best = linkedMapOf<Key, Double>()
        val result = mutableListOf<Point>()
        for (session in sorted) {
            val old = seen[session.id()]
            if (old != null) {
                require(old == session) { "Conflicting actual workout evidence." }
                continue
            }
            seen[session.id()] = session
            if (session.startedAt().isAfter(asOf ?: throw NullPointerException())) continue
            for (block in session.exercises()) for (set in block.sets()) {
                if (set.warmUp()) continue
                val exercise = block.exercise()
                val load =
                    if (exercise.measurement().usesLoad())
                        round(set.kilograms() * 1000000) / 1000000
                    else null
                if (exercise.measurement().usesReps()) {
                    if (exercise.mode() == StrengthExercise.Mode.BODYWEIGHT)
                        award(
                            result,
                            best,
                            exercise,
                            set,
                            session,
                            Kind.BODYWEIGHT_REPETITIONS,
                            null,
                            null,
                            set.reps().toDouble(),
                        )
                    else {
                        award(
                            result,
                            best,
                            exercise,
                            set,
                            session,
                            Kind.HEAVIEST_LOAD,
                            null,
                            null,
                            set.kilograms(),
                        )
                        award(
                            result,
                            best,
                            exercise,
                            set,
                            session,
                            Kind.EXACT_REPETITIONS_LOAD,
                            set.reps(),
                            null,
                            set.kilograms(),
                        )
                        award(
                            result,
                            best,
                            exercise,
                            set,
                            session,
                            Kind.REPETITIONS_AT_LOAD,
                            null,
                            load,
                            set.reps().toDouble(),
                        )
                        val estimate =
                            if (set.reps() >= 2)
                                StrengthRecordPolicy.estimatedOneRepMax(exercise, set)
                            else null
                        if (estimate != null)
                            award(
                                result,
                                best,
                                exercise,
                                set,
                                session,
                                Kind.ESTIMATED_ONE_REP_MAX,
                                null,
                                null,
                                estimate,
                            )
                    }
                } else if (exercise.measurement().usesDuration())
                    award(
                        result,
                        best,
                        exercise,
                        set,
                        session,
                        Kind.DURATION,
                        null,
                        load,
                        set.durationSeconds()!!.toDouble(),
                    )
                else if (exercise.measurement().usesDistance())
                    award(
                        result,
                        best,
                        exercise,
                        set,
                        session,
                        Kind.DISTANCE,
                        null,
                        load,
                        set.distanceMetres()!!,
                    )
            }
        }
        return Immutable.copyList(result)
    }

    private fun award(
        result: MutableList<Point>,
        best: MutableMap<Key, Double>,
        exercise: StrengthExercise,
        actual: StrengthSession.SetEntry,
        session: StrengthSession,
        kind: Kind,
        reps: Int?,
        load: Double?,
        value: Double,
    ) {
        val key = Key(exercise.id(), exercise.mode(), exercise.measurement(), kind, reps, load)
        if (value <= 0 || value <= (best[key] ?: -1.0) + 0.0000001) return
        best[key] = value
        result.add(
            Point(
                key,
                value,
                exercise,
                actual,
                session.id(),
                session.startedAt(),
                CalendarDates.atOffset(session.startedAt(), session.offset()),
            )
        )
    }

    @JvmStatic
    fun best(history: List<Point>): List<Point> {
        val best = linkedMapOf<Key?, Point>()
        for (point in history) {
            val previous = best[point.key()]
            if (previous == null || point.value() > previous.value()) best[point.key()] = point
        }
        return Immutable.copyList(best.values)
    }

    @JvmStatic
    fun estimateExplanation(): String =
        "Epley estimate: recorded load × (1 + repetitions ÷ 30), for eligible external-load working sets of 2–10 repetitions only. It is not a measured lift or a promise. Technique, effort and individual differences affect the estimate; no maximal test is required."
}
