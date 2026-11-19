package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.coachingMonday
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Strength-first Home evidence. Prescriptions and actual workouts remain separate. */
data class StrengthTodaySummary
@JvmOverloads
constructor(
    private val today: GymProgramme.Session?,
    private val todayCompleted: Boolean,
    private val weekStart: LocalDate?,
    private val plannedThisWeek: Int,
    private val completedThisWeek: Int,
    private val actualWorkoutsThisWeek: Int,
    private val recentPbExercises: Int,
    private val recommendation: Recommendation?,
    private val authoredToday: StrengthProgramme.Workout? = null,
) {
    fun today(): GymProgramme.Session? = today

    fun todayCompleted(): Boolean = todayCompleted

    fun weekStart(): LocalDate? = weekStart

    fun plannedThisWeek(): Int = plannedThisWeek

    fun completedThisWeek(): Int = completedThisWeek

    fun actualWorkoutsThisWeek(): Int = actualWorkoutsThisWeek

    fun recentPbExercises(): Int = recentPbExercises

    fun recommendation(): Recommendation? = recommendation

    fun authoredToday(): StrengthProgramme.Workout? = authoredToday

    override fun toString(): String =
        "StrengthTodaySummary[today=$today, todayCompleted=$todayCompleted, weekStart=$weekStart, plannedThisWeek=$plannedThisWeek, completedThisWeek=$completedThisWeek, actualWorkoutsThisWeek=$actualWorkoutsThisWeek, recentPbExercises=$recentPbExercises, recommendation=$recommendation, authoredToday=$authoredToday]"

    /** Explicit actual/prescription association with preserved revision identity. */
    data class Association(
        private val actualSessionId: String,
        private val versionKey: String,
        private val prescribedSessionId: String,
    ) {
        fun actualSessionId(): String = actualSessionId

        fun versionKey(): String = versionKey

        fun prescribedSessionId(): String = prescribedSessionId

        override fun toString(): String =
            "Association[actualSessionId=$actualSessionId, versionKey=$versionKey, prescribedSessionId=$prescribedSessionId]"
    }

    data class Recommendation(
        private val exerciseId: String?,
        private val action: StrengthProgressionAdvisor.Action?,
        private val reason: String?,
    ) {
        fun exerciseId(): String? = exerciseId

        fun action(): StrengthProgressionAdvisor.Action? = action

        fun reason(): String? = reason

        override fun toString(): String =
            "Recommendation[exerciseId=$exerciseId, action=$action, reason=$reason]"
    }

    companion object {
        @JvmStatic
        fun from(
            programme: GymProgramme?,
            history: List<StrengthSession>,
            associations: List<Association>,
            today: LocalDate,
            asOf: Instant,
        ): StrengthTodaySummary {
            val actual = history.filter { !it.startedAt().isAfter(asOf) }
            val actualIds = actual.map { it.id() }.toSet()
            val completed =
                if (programme == null) emptySet()
                else
                    associations
                        .filter {
                            it.actualSessionId() in actualIds &&
                                belongs(it.versionKey(), programme.id(), programme.version())
                        }
                        .map { it.prescribedSessionId() }
                        .toSet()
            val weekStart = coachingMonday(today)
            val week = programme?.sessions()?.filter { inWeek(it.date(), weekStart) } ?: emptyList()
            val prescribed = programme?.sessions()?.firstOrNull { it.date() == today }
            val actualThisWeek =
                actual.count {
                    inWeek(CalendarDates.atOffset(it.startedAt(), it.offset()), weekStart)
                }
            val recentPbs =
                StrengthAnalytics.progression(actual)
                    .filter {
                        it.category() != "Estimated 1RM" &&
                            Durations.between(it.at()!!, asOf) <= Durations.ofSeconds(28 * 86400L)
                    }
                    .map { it.exerciseId() }
                    .distinct()
                    .size
            return StrengthTodaySummary(
                prescribed,
                prescribed != null && prescribed.id() in completed,
                weekStart,
                week.size,
                week.count { it.id() in completed },
                actualThisWeek,
                recentPbs,
                recommendation(programme, actual, today, asOf),
            )
        }

        @JvmStatic
        fun fromAuthored(
            programme: StrengthProgramme,
            history: List<StrengthSession>,
            associations: List<Association>,
            today: LocalDate,
            asOf: Instant,
        ): StrengthTodaySummary {
            val actualSummary = from(null, history, emptyList(), today, asOf)
            val actualIds = history.filter { !it.startedAt().isAfter(asOf) }.map { it.id() }.toSet()
            val completed =
                associations
                    .filter {
                        it.actualSessionId() in actualIds &&
                            belongs(it.versionKey(), programme.id(), programme.revision())
                    }
                    .map { it.prescribedSessionId() }
                    .toSet()
            val week =
                programme.workouts().filter { inWeek(it.date(), actualSummary.weekStart()!!) }
            val prescribed = programme.workouts().firstOrNull { it.date() == today }
            val considered = mutableSetOf<String>()
            val advice = mutableListOf<Recommendation>()
            for (w in
                programme
                    .workouts()
                    .filter { it.date().toEpochDay() >= today.toEpochDay() }
                    .sortedBy { it.date().toEpochDay() }) for (slot in w.definition().slots()) {
                if (!considered.add(slot.exercise().id())) continue
                val result = StrengthTargetProgression.review(slot, history, asOf)
                if (actionable(result.action()))
                    advice.add(
                        Recommendation(slot.exercise().id(), result.action(), result.reason())
                    )
            }
            return StrengthTodaySummary(
                null,
                prescribed != null && prescribed.id() in completed,
                actualSummary.weekStart(),
                week.size,
                week.count { it.id() in completed },
                actualSummary.actualWorkoutsThisWeek(),
                actualSummary.recentPbExercises(),
                prioritise(advice),
                prescribed,
            )
        }

        private fun inWeek(date: LocalDate, start: LocalDate): Boolean =
            date.toEpochDay() >= start.toEpochDay() &&
                date.toEpochDay() < start.plusWeeks(1).toEpochDay()

        private fun belongs(key: String, id: String, revision: Int): Boolean {
            val prefix = "$id:v"
            if (!key.startsWith(prefix)) return false
            val version = key.substring(prefix.length).toIntOrNull() ?: return false
            return version in 1..revision
        }

        private fun actionable(action: StrengthProgressionAdvisor.Action?): Boolean =
            action == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE ||
                action == StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY

        private fun prioritise(advice: List<Recommendation>): Recommendation? =
            advice.firstOrNull {
                it.action() == StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY
            } ?: advice.firstOrNull()

        private fun recommendation(
            programme: GymProgramme?,
            actual: List<StrengthSession>,
            today: LocalDate,
            asOf: Instant,
        ): Recommendation? {
            if (programme == null) return null
            val recent =
                actual.filter {
                    Durations.between(it.startedAt(), asOf) <= Durations.ofSeconds(28 * 86400L)
                }
            val considered = mutableSetOf<String>()
            val advice = mutableListOf<Recommendation>()
            for (session in
                programme
                    .sessions()
                    .filter { it.date().toEpochDay() >= today.toEpochDay() }
                    .sortedBy { it.date().toEpochDay() }) for (p in session.exercises()) {
                if (!considered.add(p.exerciseId())) continue
                val result = StrengthProgressionAdvisor.review(p, recent)
                if (actionable(result.action()))
                    advice.add(Recommendation(p.exerciseId(), result.action(), result.reason()))
            }
            return prioritise(advice)
        }
    }
}
