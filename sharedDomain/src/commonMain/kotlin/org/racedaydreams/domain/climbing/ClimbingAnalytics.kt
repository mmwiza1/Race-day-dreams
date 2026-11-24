package org.racedaydreams.domain.climbing

import org.racedaydreams.domain.presentation.DashboardAnalytics
import org.racedaydreams.domain.presentation.DashboardAnalytics.Period
import org.racedaydreams.domain.time.LocalDate

/**
 * Bouldering statistics from saved sessions only. They are kept separate from weightlifting: no
 * working sets, estimated 1RM, lifting records or muscle-group workload are derived from climbing.
 *
 * Grade statistics use each problem's original grade. Distribution and progression cover the chosen
 * [GradeSystem]; problems graded in the other system or left ungraded are counted separately rather
 * than converted, so no difficulty is inferred.
 */
object ClimbingAnalytics {
    data class GradeCount(val grade: ClimbingGrade, val attempted: Int, val sent: Int)

    /**
     * One week of the progression chart. [highestSend] is the hardest send in the chosen grading
     * system only; [sends] counts every send in the week, graded in either system or ungraded. No
     * screen shows [sends] beside the grade line today (audit B-R7); label it as all sends if it is
     * ever shown there.
     */
    data class WeekPoint(val weekStart: LocalDate, val highestSend: ClimbingGrade?, val sends: Int)

    data class Summary(
        val period: Period,
        val window: DashboardAnalytics.Window,
        val system: GradeSystem,
        val sessions: Int,
        val climbingSeconds: Long,
        val problems: Int,
        val attempts: Int,
        val sends: Int,
        val flashes: Int,
        /** Hardest actual send per grading system, in that system's own grades. */
        val highestSend: Map<GradeSystem, ClimbingGrade>,
        /** Grades of the chosen system that were tried in the period, easiest first. */
        val distribution: List<GradeCount>,
        val otherSystemProblems: Int,
        val ungradedProblems: Int,
        /** One point per week of the period; the last week is still in progress. */
        val progression: List<WeekPoint>,
        /** The three most recent sessions overall, newest first. */
        val recent: List<ClimbingSession>,
    ) {
        fun hasAnySession(): Boolean = sessions > 0 || recent.isNotEmpty()
    }

    fun summary(
        sessions: List<ClimbingSession>,
        today: LocalDate,
        period: Period,
        system: GradeSystem,
    ): Summary {
        val window = DashboardAnalytics.window(period, today)
        val unique = sessions.distinctBy { it.id }
        val inPeriod = unique.filter { window.contains(it.date()) }
        val problems = inPeriod.flatMap { it.problems }

        val highest = mutableMapOf<GradeSystem, ClimbingGrade>()
        for (problem in problems) {
            val grade = problem.grade ?: continue
            if (!problem.sent()) continue
            val best = highest[grade.system]
            if (best == null || grade.rank > best.rank) highest[grade.system] = grade
        }

        val counts = mutableMapOf<String, Pair<Int, Int>>()
        var other = 0
        var ungraded = 0
        for (problem in problems) {
            val grade = problem.grade
            when {
                grade == null -> ungraded++
                grade.system != system -> other++
                else -> {
                    val (tried, sent) = counts[grade.label] ?: (0 to 0)
                    counts[grade.label] = (tried + 1) to (sent + if (problem.sent()) 1 else 0)
                }
            }
        }
        val distribution =
            ClimbingGrade.labels(system).mapNotNull { label ->
                counts[label]?.let { (tried, sent) ->
                    GradeCount(ClimbingGrade(system, label), tried, sent)
                }
            }

        val progression =
            (0 until period.weeks).map { index ->
                val start = window.start.plusWeeks(index.toLong())
                val end = start.plusDays(6)
                val week =
                    inPeriod.filter {
                        val day = it.date().toEpochDay()
                        day >= start.toEpochDay() && day <= end.toEpochDay()
                    }
                val sent =
                    week.flatMap { it.problems }.filter { it.sent() && it.grade?.system == system }
                WeekPoint(
                    start,
                    sent.mapNotNull { it.grade }.maxByOrNull { it.rank },
                    week.sumOf { it.sends() },
                )
            }

        return Summary(
            period = period,
            window = window,
            system = system,
            sessions = inPeriod.size,
            climbingSeconds = inPeriod.sumOf { it.durationSeconds },
            problems = problems.size,
            attempts = problems.sumOf { it.attempts },
            sends = problems.count { it.sent() },
            flashes = problems.count { it.outcome == ClimbOutcome.FLASH },
            highestSend = highest,
            distribution = distribution,
            otherSystemProblems = other,
            ungradedProblems = ungraded,
            progression = progression,
            recent =
                unique
                    .sortedWith(
                        compareByDescending<ClimbingSession> { it.startedAt }.thenBy { it.id }
                    )
                    .take(3),
        )
    }
}
