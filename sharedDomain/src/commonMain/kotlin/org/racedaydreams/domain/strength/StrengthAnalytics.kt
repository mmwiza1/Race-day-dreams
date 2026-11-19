package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Original projections recomputed from retained sets, not programme prescriptions. */
object StrengthAnalytics {
    enum class Involvement {
        LOW,
        MODERATE,
        HIGH,
    }

    data class Context(
        private val region: StrengthExercise.Region?,
        private val effort: String?,
        private val lowerBody: Involvement?,
        private val reasons: List<String>?,
    ) {
        fun region(): StrengthExercise.Region? = region

        fun effort(): String? = effort

        fun lowerBody(): Involvement? = lowerBody

        fun reasons(): List<String>? = reasons

        override fun toString(): String =
            "Context[region=$region, effort=$effort, lowerBody=$lowerBody, reasons=$reasons]"
    }

    data class Record(
        private val exerciseId: String?,
        private val category: String?,
        private val value: Double,
        private val kilograms: Double,
        private val reps: Int,
        private val at: Instant?,
        private val sessionId: String?,
        private val setId: String?,
        private val notes: String?,
    ) {
        fun exerciseId(): String? = exerciseId

        fun category(): String? = category

        fun value(): Double = value

        fun kilograms(): Double = kilograms

        fun reps(): Int = reps

        fun at(): Instant? = at

        fun sessionId(): String? = sessionId

        fun setId(): String? = setId

        fun notes(): String? = notes

        override fun toString(): String =
            "Record[exerciseId=$exerciseId, category=$category, value=$value, kilograms=$kilograms, reps=$reps, at=$at, sessionId=$sessionId, setId=$setId, notes=$notes]"
    }

    @JvmStatic
    fun context(session: StrengthSession): Context {
        var lower = 0
        var upper = 0
        val sessionEffort = session.rpe()
        var hard = sessionEffort != null && sessionEffort >= 7
        var hardLower = hard
        for (block in session.exercises()) for (set in block.sets()) if (!set.warmUp()) {
            val involvesLower =
                block.exercise().region() != StrengthExercise.Region.UPPER &&
                    ExerciseCatalog.definition(block.exercise().id())?.category() != "Core"
            if (involvesLower) lower++
            if (block.exercise().region() != StrengthExercise.Region.LOWER) upper++
            val setEffort = set.coachingRpe()
            if (setEffort != null && setEffort >= 8) {
                hard = true
                if (involvesLower) hardLower = true
            }
        }
        val region =
            if (lower == 0) StrengthExercise.Region.UPPER
            else if (upper == 0) StrengthExercise.Region.LOWER else StrengthExercise.Region.FULL
        val involvement =
            if (lower >= 3 && hardLower) Involvement.HIGH
            else if (lower > 0) Involvement.MODERATE else Involvement.LOW
        return Context(
            region,
            if (hard) "Hard"
            else if (sessionEffort == null) "Effort not recorded"
            else if (sessionEffort <= 3) "Light" else "Moderate",
            involvement,
            Immutable.list(
                "$lower working sets involved the lower body.",
                "Effort uses recorded session/set RPE or an approximate 10−RIR conversion; load alone does not establish fatigue. This is coaching context, not a physiological measurement.",
            ),
        )
    }

    @JvmStatic
    fun sessionLoad(session: StrengthSession): Double? {
        val effort = session.rpe() ?: return null
        // Valid recorded sessions are positive and <=12h. Retain original toMillis
        // truncation/order.
        val duration = session.duration()
        val millis = duration.getSeconds() * 1000 + duration.getNano() / 1000000
        return millis / 60000.0 * effort
    }

    @JvmStatic
    fun estimatedOneRepMax(exercise: StrengthExercise?, set: StrengthSession.SetEntry?): Double? =
        StrengthRecordPolicy.estimatedOneRepMax(exercise, set)

    @JvmStatic
    fun progression(sessions: List<StrengthSession>): List<Record> {
        val sorted =
            sessions.sortedWith(compareBy<StrengthSession> { it.startedAt() }.thenBy { it.id() })
        val best = mutableMapOf<String, Double>()
        val history = mutableListOf<Record>()
        for (session in sorted) for (block in session.exercises()) for (set in block.sets()) if (
            !set.warmUp()
        ) {
            val exercise = block.exercise()
            if (!exercise.measurement().usesReps()) {
                val duration = set.durationSeconds()
                val value = duration?.toDouble() ?: set.distanceMetres()!!
                var unit = if (duration != null) "Duration seconds" else "Distance metres"
                if (exercise.measurement().usesLoad()) unit += " at ${set.kilograms()} kg"
                award(history, best, exercise, unit, value, set, session)
            } else if (exercise.mode() == StrengthExercise.Mode.BODYWEIGHT)
                award(
                    history,
                    best,
                    exercise,
                    "Bodyweight reps",
                    set.reps().toDouble(),
                    set,
                    session,
                )
            else {
                val prefix =
                    if (exercise.mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT) "Added load"
                    else "Load"
                award(history, best, exercise, prefix, set.kilograms(), set, session)
                if (set.reps() in listOf(1, 3, 5, 6, 8, 10))
                    award(
                        history,
                        best,
                        exercise,
                        "$prefix × ${set.reps()}",
                        set.kilograms(),
                        set,
                        session,
                    )
                val estimate = estimatedOneRepMax(exercise, set)
                if (estimate != null)
                    award(history, best, exercise, "Estimated 1RM", estimate, set, session)
            }
        }
        return Immutable.copyList(history)
    }

    private fun award(
        history: MutableList<Record>,
        best: MutableMap<String, Double>,
        exercise: StrengthExercise,
        category: String,
        value: Double,
        set: StrengthSession.SetEntry,
        session: StrengthSession,
    ) {
        val key = "${exercise.id()}:$category"
        if (value <= 0 || value <= (best[key] ?: -1.0) + 0.0000001) return
        best[key] = value
        history.add(
            Record(
                exercise.id(),
                category,
                value,
                set.kilograms(),
                set.reps(),
                session.startedAt(),
                session.id(),
                set.id(),
                session.notes(),
            )
        )
    }

    @JvmStatic
    fun hardLowerBodyDates(sessions: List<StrengthSession>): Set<LocalDate> =
        Immutable.copySet(
            sessions
                .filter { context(it).lowerBody() == Involvement.HIGH }
                .map { CalendarDates.atOffset(it.startedAt(), it.offset()) }
        )
}
