package org.racedaydreams.domain.plan

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.*
import org.racedaydreams.domain.plan.PlanRegressionReport.Issue
import org.racedaydreams.domain.plan.PlanRegressionReport.Severity
import org.racedaydreams.domain.plan.PlanRegressionReport.Week
import org.racedaydreams.domain.time.LocalDate

/** Original whole-plan engineering budgets. Never generates or changes an athlete's plan. */
object PlanRegression {
    private const val EPSILON = 0.000001

    @JvmStatic
    fun inspect(
        plan: TrainingPlan,
        profile: UserProfile,
        knownHardLowerBodyDates: Set<LocalDate>,
        policy: PlanRegressionPolicy,
    ): PlanRegressionReport {
        val issues = mutableListOf<Issue>()
        inspectPhases(plan, issues)
        val ordered =
            plan
                .workouts()
                .sortedWith(compareBy<PlannedWorkout> { it.date().toEpochDay() }.thenBy { it.id() })
        val ids = mutableSetOf<String>()
        val runDates = mutableSetOf<LocalDate>()
        val weeks = linkedMapOf<LocalDate, MutableWeek>()
        var date = monday(plan.startDate())
        while (date.toEpochDay() <= plan.endDate().toEpochDay()) {
            weeks[date] =
                MutableWeek(
                    date.toEpochDay() >= plan.startDate().toEpochDay() &&
                        date.plusDays(6).toEpochDay() <= plan.endDate().toEpochDay()
                )
            date = date.plusWeeks(1)
        }
        var previousHard: PlannedWorkout? = null
        var previousLong: PlannedWorkout? = null
        for (workout in ordered) {
            if (!ids.add(workout.id()))
                add(
                    issues,
                    "DUPLICATE_WORKOUT_ID",
                    workout.date(),
                    "Workout IDs must be stable and unique: " + workout.id(),
                )
            if (!runDates.add(workout.date()))
                add(
                    issues,
                    "MULTIPLE_RUNS_ON_DAY",
                    workout.date(),
                    "This availability model supports one prescribed run per day.",
                )
            val race = workout.category() == WorkoutCategory.RACE
            val intendedRace = race && workout.date() == profile.raceGoal().raceDate()
            if (
                !intendedRace &&
                    workout.date().getDayOfWeek() !in profile.preferences().runningDays()
            )
                add(
                    issues,
                    "UNAVAILABLE_DAY",
                    workout.date(),
                    "Run falls outside the runner's selected running days.",
                )
            if (race && !intendedRace)
                add(
                    issues,
                    "UNSELECTED_RACE",
                    workout.date(),
                    "A race prescription does not match the runner-owned event date.",
                )
            if (
                (workout.distanceKm() == null || workout.distanceKm()!! <= 0) &&
                    (workout.duration() == null || workout.duration()!!.isZero())
            )
                add(
                    issues,
                    "EMPTY_PRESCRIPTION",
                    workout.date(),
                    "A workout needs a positive distance or time prescription.",
                )
            val week = weeks[monday(workout.date())]
            if (week == null) {
                add(
                    issues,
                    "OUTSIDE_PLAN",
                    workout.date(),
                    "Workout falls outside the plan horizon.",
                )
                continue
            }
            week.totalRuns++
            if (!race) {
                week.trainingRuns++
                val distance = workout.distanceKm()
                if (distance == null) week.unknownDistance++
                else {
                    week.knownDistance += distance
                    week.largestRun = maxOf(week.largestRun, distance)
                }
            }
            if (hard(workout.category())) {
                if (!race) week.hard++
                if (
                    previousHard != null &&
                        workout.date().toEpochDay() - previousHard.date().toEpochDay() <
                            policy.minimumDaysBetweenHardSessions()
                )
                    add(
                        issues,
                        "HARD_DAY_SPACING",
                        workout.date(),
                        "Hard sessions are closer than the fixture's configured recovery spacing.",
                    )
                previousHard = workout
            }
            if (hard(workout.category()) || workout.category() == WorkoutCategory.LONG_RUN) {
                for (days in 1..policy.daysAfterHardLowerBody()) if (
                    workout.date().minusDays(days.toLong()) in knownHardLowerBodyDates
                ) {
                    add(
                        issues,
                        "HARD_LOWER_BODY_PROXIMITY",
                        workout.date(),
                        "A long/hard run follows a known hard lower-body session within the configured review window.",
                    )
                    break
                }
            }
            if (workout.category() == WorkoutCategory.LONG_RUN && workout.distanceKm() != null) {
                week.longest =
                    week.longest?.let { maxOf(it, workout.distanceKm()!!) } ?: workout.distanceKm()
                if (
                    previousLong != null &&
                        previousLong.distanceKm() != null &&
                        workout.distanceKm()!! - previousLong.distanceKm()!! >
                            policy.maximumLongRunIncreaseKm() + EPSILON
                )
                    add(
                        issues,
                        "LONG_RUN_SPIKE",
                        workout.date(),
                        "Long-run distance increases beyond the context's explicit acceptance budget.",
                    )
                previousLong = workout
            }
        }
        val summaries = mutableListOf<Week>()
        var prior: MutableWeek? = null
        for ((start, week) in weeks) {
            if (week.totalRuns > profile.preferences().runsPerWeek())
                add(
                    issues,
                    "RUN_FREQUENCY",
                    start,
                    "Prescribed runs, including the race, exceed the runner's chosen weekly count.",
                )
            if (week.hard > policy.maximumHardSessionsPerWeek())
                add(
                    issues,
                    "HARD_FREQUENCY",
                    start,
                    "Hard-session count exceeds this fixture's explicit acceptance budget.",
                )
            if (week.unknownDistance > 0)
                issues.add(
                    Issue(
                        Severity.UNVERIFIABLE,
                        "UNKNOWN_WEEKLY_DISTANCE",
                        start,
                        "Time-only prescriptions prevent a complete distance comparison; missing distance is not zero.",
                    )
                )
            else if (
                week.complete &&
                    week.knownDistance > 0 &&
                    week.largestRun / week.knownDistance >
                        policy.maximumSingleRunFraction() + EPSILON
            )
                add(
                    issues,
                    "SINGLE_RUN_DOMINATES",
                    start,
                    "One run occupies more than the fixture's permitted share of prescribed weekly distance.",
                )
            if (
                prior != null &&
                    week.complete &&
                    prior.complete &&
                    week.unknownDistance == 0 &&
                    prior.unknownDistance == 0 &&
                    prior.knownDistance > 0
            ) {
                val previous = prior.knownDistance
                if (
                    week.knownDistance - previous > policy.maximumWeeklyIncreaseKm() + EPSILON ||
                        week.knownDistance / previous > policy.maximumWeeklyMultiplier() + EPSILON
                )
                    add(
                        issues,
                        "WEEKLY_DISTANCE_SPIKE",
                        start,
                        "Prescribed weekly distance exceeds this context's relative or absolute change budget.",
                    )
            }
            summaries.add(
                Week(
                    start,
                    week.complete,
                    week.trainingRuns,
                    week.hard,
                    if (week.unknownDistance > 0) null else week.knownDistance,
                    week.knownDistance,
                    week.unknownDistance,
                    week.longest,
                )
            )
            prior = week
        }
        inspectTaper(plan, profile, summaries, policy, issues)
        return PlanRegressionReport(issues, summaries)
    }

    @JvmStatic
    fun hard(category: WorkoutCategory?): Boolean =
        category == WorkoutCategory.INTERVALS ||
            category == WorkoutCategory.TEMPO_THRESHOLD_RUN ||
            category == WorkoutCategory.RACE_PACE_RUN ||
            category == WorkoutCategory.TIME_TRIAL ||
            category == WorkoutCategory.PROGRESSION_RUN ||
            category == WorkoutCategory.RACE

    private fun inspectPhases(plan: TrainingPlan, issues: MutableList<Issue>) {
        val phases =
            plan
                .phases()
                .sortedWith(
                    compareBy<TrainingPhase> { it.startDate().toEpochDay() }
                        .thenBy { it.endDate().toEpochDay() }
                )
        var next = plan.startDate()
        for (phase in phases) {
            if (phase.startDate() != next)
                add(
                    issues,
                    if (phase.startDate().toEpochDay() < next.toEpochDay()) "PHASE_OVERLAP"
                    else "PHASE_GAP",
                    phase.startDate(),
                    "Phase dates must cover the horizon once, without overlaps or gaps.",
                )
            if (phase.endDate().toEpochDay() >= next.toEpochDay())
                next = phase.endDate().plusDays(1)
        }
        if (next != plan.endDate().plusDays(1))
            add(issues, "PHASE_GAP", next, "Phases do not cover the complete plan horizon.")
    }

    private fun inspectTaper(
        plan: TrainingPlan,
        profile: UserProfile,
        weeks: List<Week>,
        policy: PlanRegressionPolicy,
        issues: MutableList<Issue>,
    ) {
        if (
            profile.raceGoal().raceDate() == null ||
                plan.endDate().toEpochDay() - plan.startDate().toEpochDay() < 28
        )
            return
        var taper: LocalDate? = null
        for (phase in plan.phases()) if (phase.kind() == TrainingPhase.Kind.TAPER)
            taper =
                if (taper == null || taper.toEpochDay() >= phase.startDate().toEpochDay())
                    phase.startDate()
                else taper
        if (taper == null) {
            add(
                issues,
                "TAPER_MISSING",
                plan.endDate(),
                "This dated regression fixture has no taper phase.",
            )
            return
        }
        var priorPeak = 0.0
        for (week in weeks) if (
            week.start()!!.toEpochDay() < taper.toEpochDay() && week.trainingDistanceKm() != null
        )
            priorPeak = maxOf(priorPeak, week.trainingDistanceKm()!!)
        if (priorPeak == 0.0) return
        for (week in weeks) if (
            week.complete() &&
                week.start()!!.toEpochDay() >= taper.toEpochDay() &&
                week.trainingRuns() > 0 &&
                week.trainingDistanceKm() != null &&
                week.trainingDistanceKm()!! >
                    priorPeak * policy.maximumTaperWeekFraction() + EPSILON
        )
            add(
                issues,
                "TAPER_VOLUME",
                week.start()!!,
                "Taper-week training volume is not reduced within this fixture's explicit acceptance budget.",
            )
    }

    private fun monday(date: LocalDate): LocalDate =
        date.minusDays((date.getDayOfWeek().getValue() - 1).toLong())

    private fun add(issues: MutableList<Issue>, code: String, date: LocalDate, reason: String) {
        issues.add(Issue(Severity.ERROR, code, date, reason))
    }

    private class MutableWeek(val complete: Boolean) {
        var totalRuns = 0
        var trainingRuns = 0
        var hard = 0
        var unknownDistance = 0
        var knownDistance = 0.0
        var largestRun = 0.0
        var longest: Double? = null
    }
}
