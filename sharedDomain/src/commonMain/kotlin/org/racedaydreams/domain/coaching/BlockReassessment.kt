package org.racedaydreams.domain.coaching

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.time.*

/**
 * Adaptive block reassessment (training Piece 5A), run as part of the weekly review.
 *
 * It compares the last four weeks of actual recorded running with what the plan asked for and with
 * the next two planned weeks, and may offer one runner-approved change:
 * - **rebuild**: recent running is well below the coming weeks, so the next four weeks build
 *   gradually from what the runner is actually doing (never making up missed sessions);
 * - **progress**: every planned run was completed and the runner consistently ran more than
 *   planned, so the next two weeks' easy and long runs may grow by at most 10% (quality sessions
 *   and intensity are never increased).
 *
 * Only recorded runs before the review week ends count; planned future workouts are never treated
 * as evidence. Weeks with no recorded run are treated as unknown (possibly missing data), not as
 * rest, so they never justify progression and are left out of the rebuild average. Nothing changes
 * until the runner accepts a choice, and holding, reducing and the return bridge (existing
 * weekly-review choices) take priority over both changes.
 */
internal object BlockReassessment {
    class Result(
        val observations: List<String>,
        val recommendation: String?,
        val choice: WeeklyCoaching.Choice?,
    )

    /** Recent four-week evidence; kilometres are per recorded week. */
    class Evidence(
        val weeklyKm: List<Double>,
        val recordedWeeks: Int,
        val planned: Int,
        val completed: Int,
        val plannedKm: Double,
        val longestKm: Double,
    ) {
        /** Weeks with recorded distance; weeks without are unknown, not zero. */
        fun distanceWeeks(): Int = weeklyKm.count { it > 0 }

        /** Average over weeks with recorded distance only. */
        fun recordedAverage(): Double =
            if (distanceWeeks() == 0) 0.0 else weeklyKm.filter { it > 0 }.sum() / distanceWeeks()
    }

    fun evidence(
        reviewed: List<PlannedWorkout>,
        actual: Map<String, ActivityRecord>,
        counted: List<ActivityRecord>,
        end: LocalDate,
    ): Evidence {
        val window = end.minusDays(28)
        val weeklyKm = MutableList(4) { 0.0 }
        val weeklyRuns = MutableList(4) { 0 }
        var longest = 0.0
        for (run in counted) {
            val date = coachingDate(run)
            if (coachingBefore(date, window) || !coachingBefore(date, end)) continue
            val week = ((date.toEpochDay() - window.toEpochDay()) / 7).toInt()
            weeklyRuns[week]++
            val km = (run.distanceMetres() ?: 0.0) / 1000
            weeklyKm[week] += km
            longest = max(longest, km)
        }
        var planned = 0
        var completed = 0
        var plannedKm = 0.0
        for (w in reviewed) {
            if (w.category() == WorkoutCategory.RACE) continue
            if (coachingBefore(w.date(), window) || !coachingBefore(w.date(), end)) continue
            planned++
            plannedKm += w.distanceKm() ?: 0.0
            val a = actual[w.id()]
            if (a != null && coachingBefore(coachingDate(a), end)) completed++
        }
        return Evidence(
            weeklyKm,
            weeklyRuns.count { it > 0 },
            planned,
            completed,
            plannedKm,
            longest,
        )
    }

    fun assess(
        version: PlanVersion,
        evidence: Evidence,
        end: LocalDate,
        concerns: Boolean,
        recurringMisses: Boolean,
    ): Result {
        val observations = mutableListOf<String>()
        val goal = version.goalSnapshot().raceGoal()
        val prefs = version.goalSnapshot().preferences()
        val race = goal.raceDate()
        val daysToRace = race?.let { it.toEpochDay() - end.toEpochDay() }
        val upcoming = week(version, end) + week(version, end.plusWeeks(1))
        val nextAverage = upcoming.sumOf { it.distanceKm() ?: 0.0 } / 2
        val recorded = evidence.recordedAverage()

        if (evidence.recordedWeeks == 0) {
            if (evidence.planned >= 2)
                observations +=
                    "No runs were recorded in the last four weeks. If you have been running, check that your watch or health app is syncing; if you have not, record a break so your plan can offer a return bridge. Your plan is not changed on missing data."
            return Result(observations, null, null)
        }
        if (evidence.recordedWeeks < 4)
            observations +=
                "${4 - evidence.recordedWeeks} of the last four weeks have no recorded runs. They are treated as unknown, not as rest, so no increase is suggested from them."
        // Realism: much of the planned training was missed close to the race.
        if (
            daysToRace != null &&
                daysToRace in 0..56 &&
                evidence.planned >= 4 &&
                evidence.completed * 2 < evidence.planned
        )
            observations +=
                "Only ${evidence.completed} of ${evidence.planned} planned runs were recorded in the last four weeks, with ${daysToRace / 7} weeks to race day. Your current training may no longer fully support this race${if (goal.mode() == GoalMode.TARGET_TIME) " or your target time" else ""}. You could keep it as a completion or run/walk goal, or choose a later race. Missed runs are not made up."
        if (concerns || nextAverage <= 0) return Result(observations, null, null)

        // Rebuild: recent running is well below the coming weeks.
        if (
            evidence.distanceWeeks() >= 2 &&
                recorded < nextAverage * .75 &&
                (daysToRace == null || daysToRace >= 14)
        ) {
            // Misses on a recurring day mean reduced availability, not lower fitness: the fix is
            // the runner's days, which regenerates the plan around the days they actually run.
            if (recurringMisses) {
                observations +=
                    "Your recent running (about ${rounded(recorded)} km a week) is below the coming weeks because the same day keeps being missed. Editing your regular days rebuilds the plan around the days you actually run; no catch-up mileage is added."
                return Result(observations, null, null)
            }
            val replacements = mutableListOf<PlannedWorkout>()
            for (i in 0 until 4) {
                val workouts = week(version, end.plusWeeks(i.toLong()))
                val total = workouts.sumOf { it.distanceKm() ?: 0.0 }
                if (total <= 0) continue
                val factor = min(1.0, recorded * (1 + .05 * i) / total)
                if (factor < .98)
                    for (w in workouts) replacements +=
                        scale(
                            w,
                            factor,
                            "Rebuilt from your recent running (about ${rounded(recorded)} km a week); missed sessions are not made up.",
                        )
            }
            if (replacements.isNotEmpty()) {
                val recommendation =
                    "Your recorded running over the last four weeks (about ${rounded(recorded)} km a week) is well below the next two planned weeks (about ${rounded(nextAverage)} km). Consider rebuilding gradually from what you are running now, rising about 5% a week for four weeks; your plan is then reviewed again."
                realism(version, replacements)?.let { observations += it }
                return Result(
                    observations,
                    recommendation,
                    WeeklyCoaching.Choice("rebuild", "Rebuild from my recent running", replacements),
                )
            }
        }

        // Progress: everything planned was done and the runner consistently ran more.
        val maintains =
            goal.distance() == RaceDistance.GENERAL_FITNESS &&
                prefs.strategy() != TrainingStrategy.DISTANCE_FIRST
        if (
            !maintains &&
                evidence.recordedWeeks == 4 &&
                evidence.planned >= 4 &&
                evidence.completed == evidence.planned &&
                evidence.plannedKm > 0 &&
                evidence.weeklyKm.all { it >= evidence.plannedKm / 4 * 1.15 } &&
                (daysToRace == null || daysToRace >= 35)
        ) {
            val replacements = mutableListOf<PlannedWorkout>()
            for (i in 0 until 2) {
                val workouts = week(version, end.plusWeeks(i.toLong()))
                val total = workouts.sumOf { it.distanceKm() ?: 0.0 }
                if (total <= 0 || total >= recorded) continue
                val growable =
                    workouts.filter {
                        it.category() == WorkoutCategory.EASY_RUN ||
                            it.category() == WorkoutCategory.LONG_RUN
                    }
                val growableKm = growable.sumOf { it.distanceKm() ?: 0.0 }
                if (growableKm <= 0) continue
                val target = min(recorded, total * 1.10)
                val factor = min(1.10, 1 + (target - total) / growableKm)
                for (w in growable) {
                    var f = factor
                    val km = w.distanceKm() ?: continue
                    // A long run grows at most 10% beyond the longest recent recorded run.
                    if (w.category() == WorkoutCategory.LONG_RUN)
                        f = min(f, max(1.0, evidence.longestKm * 1.10 / km))
                    if (f > 1.02)
                        replacements +=
                            scale(
                                w,
                                f,
                                "Cautious progression after four weeks of completing every planned run and running more than planned; intensity is unchanged.",
                            )
                }
            }
            if (replacements.isNotEmpty())
                return Result(
                    observations,
                    "You completed every planned run and consistently ran more than planned for four weeks (about ${rounded(recorded)} km a week). If that feels comfortable, you could let the next two weeks' easy and long runs grow by up to 10%. Quality sessions are unchanged, and keeping the plan is equally fine.",
                    WeeklyCoaching.Choice(
                        "progress",
                        "Progress cautiously for two weeks",
                        replacements,
                    ),
                )
        }
        return Result(observations, null, null)
    }

    /** Planned training (not the race) in the week starting [from]. */
    private fun week(version: PlanVersion, from: LocalDate): List<PlannedWorkout> =
        version.plan().workouts().filter {
            !coachingBefore(it.date(), from) &&
                coachingBefore(it.date(), from.plusWeeks(1)) &&
                it.category() != WorkoutCategory.RACE
        }

    /** Whether the rebuilt plan lowers the race-readiness rating; judged as a finish goal. */
    private fun realism(version: PlanVersion, replacements: List<PlannedWorkout>): String? {
        val goal = version.goalSnapshot().raceGoal()
        if (goal.raceDate() == null) return null
        val finish =
            RaceGoal(goal.distance(), goal.raceName(), goal.raceDate(), GoalMode.FINISH, null)
        val prefs = version.goalSnapshot().preferences()
        val changes = replacements.associateBy { it.id() }
        val plan = version.plan()
        val rebuilt =
            TrainingPlan(
                plan.id(),
                plan.startDate(),
                plan.endDate(),
                plan.phases(),
                plan.workouts().map { changes[it.id()] ?: it },
            )
        val before = PlanFeasibility.assess(plan, finish, prefs) ?: return null
        val after = PlanFeasibility.assess(rebuilt, finish, prefs) ?: return null
        val target =
            if (goal.mode() == GoalMode.TARGET_TIME)
                " Your target time was set for the original build-up and may no longer be realistic."
            else ""
        return if (after.level().ordinal > before.level().ordinal)
            "With the rebuilt weeks: ${after.headline()} ${after.reasons().firstOrNull() ?: ""}$target"
                .trim()
        else if (target.isNotEmpty()) target.trim() else null
    }

    /**
     * Resizes [w] by [factor] while keeping a structured session's repetitions and recoveries: only
     * the easy parts (warm-up, cool-down, easy running) change. A session that would no longer fit
     * becomes an easy run.
     */
    fun scale(w: PlannedWorkout, factor: Double, note: String): PlannedWorkout {
        val km = w.distanceKm()
        val duration =
            w.duration()?.let { Durations.ofSeconds(max(60L, (it.getSeconds() * factor).toLong())) }
        val newKm = km?.let { max(.1, round(it * factor * 10) / 10.0) }
        fun fixed(s: WorkoutSegment) =
            s.kind() == WorkoutSegment.Kind.RECOVERY ||
                s.kind() == WorkoutSegment.Kind.RUN && (s.rpe() ?: 0.0) > 3.0
        val fixedKm = w.segments().filter(::fixed).sumOf { it.distanceKm() ?: 0.0 }
        val flexible = w.segments().filter { !fixed(it) && it.distanceKm() != null }
        val flexibleKm = flexible.sumOf { it.distanceKm()!! }
        val structured = w.segments().any(::fixed)
        val untimed = km != null && newKm != null
        val remaining = if (untimed) newKm!! - (km!! - flexibleKm) else 0.0
        val minimum = if (AdaptiveCoaching.isQuality(w.category())) 2.0 else 1.0
        if (structured && untimed && (flexibleKm <= 0 || remaining < minimum))
            return PlannedWorkout(
                w.id(),
                w.date(),
                WorkoutCategory.EASY_RUN,
                newKm,
                null,
                listOf(
                    WorkoutSegment(
                        WorkoutSegment.Kind.RUN,
                        newKm,
                        null,
                        null,
                        null,
                        null,
                        3.0,
                        "Easy conversational running.",
                    )
                ),
                "$note This day is now an easy run. Original prescription is retained in plan history.",
            )
        val flexFactor = if (structured && untimed) remaining / flexibleKm else factor
        val segments =
            w.segments().map { s ->
                if (structured && fixed(s)) s
                else
                    WorkoutSegment(
                        s.kind(),
                        s.distanceKm()?.let { max(.1, round(it * flexFactor * 10) / 10.0) },
                        s.duration()?.let {
                            Durations.ofSeconds(max(1L, (it.getSeconds() * factor).toLong()))
                        },
                        s.minimumSecondsPerKm(),
                        s.maximumSecondsPerKm(),
                        s.heartRateZone(),
                        s.rpe(),
                        s.guidance(),
                    )
            }
        return PlannedWorkout(
            w.id(),
            w.date(),
            w.category(),
            newKm,
            duration,
            segments,
            "$note Original prescription is retained in plan history.",
        )
    }

    private fun rounded(value: Double): String {
        val tenths = round(value * 10).toLong()
        return "${tenths / 10}.${tenths % 10}"
    }
}
