package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.roundedNanos
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.strength.StrengthAnalytics
import org.racedaydreams.domain.strength.StrengthProgrammeEditor
import org.racedaydreams.domain.time.*

/** Original deterministic training heuristics, not commercial schedules or new prescriptions. */
class OriginalTrainingPlanGenerator : TrainingPlanGenerator {
    override fun generate(
        profile: UserProfile,
        baseline: RunnerBaseline,
        startDate: LocalDate,
    ): TrainingPlan =
        generate(PlanRequest(profile, baseline, startDate, Immutable.set(), Immutable.list()))

    fun generate(request: PlanRequest): TrainingPlan {
        val profile = request.profile()
        val baseline = request.baseline()
        val goal = profile.raceGoal()
        val prefs = profile.preferences()
        val start = request.startDate()
        val race = goal.raceDate()
        val end = race ?: start.plusWeeks(12).minusDays(1)
        require(!before(end, start)) {
            "The race is in the past. Review its date before generating a plan."
        }
        require(days(start, end) <= 36525) {
            "Check the race year; dates beyond 100 years are not supported."
        }
        require(race == null || race !in request.unavailableDates()) {
            "Race day is explicitly marked unavailable. Review that one-off conflict; no plan was changed."
        }
        val id = StrengthProgrammeEditor.nameUuid("$start:$goal:$prefs")
        val weeks = (days(start, end) / 7).toInt() + 1
        val weeklyEvidence = baseline.weeklyDistanceKm()
        val longestEvidence = baseline.longestRunKm()
        var established =
            weeklyEvidence != null &&
                weeklyEvidence >= 10 &&
                longestEvidence != null &&
                longestEvidence >= 3
        val recentBoundary = Instants.ofEpochSecond(start.minusDays(21).toEpochDay() * 86400)
        val last = baseline.lastActivityAt()
        val reviewed = baseline.manualRecordedAt()
        val recent =
            last != null && !last.isBefore(recentBoundary) ||
                reviewed != null && !reviewed.isBefore(recentBoundary)
        if (!recent) established = false
        val performance =
            PerformanceEligibilityEvaluator.evaluate(
                    baseline,
                    Instants.ofEpochSecond(start.toEpochDay() * 86400 + 23 * 3600 + 59 * 60),
                )
                .eligible()
        val consistent = baseline.consistentWeeks()
        val qualitySupported =
            established &&
                consistent != null &&
                consistent >= 6 &&
                weeklyEvidence!! >= 20 &&
                prefs.runsPerWeek() >= 3
        var frequency = if (established) prefs.runsPerWeek() else min(3, prefs.runsPerWeek())
        if (request.distribution() == "FEWER_LONGER" && frequency > 2) frequency--
        var base = if (established) weeklyEvidence!! else 0.0
        val previousFrequency = baseline.runsPerWeek()
        if (established && previousFrequency != null && previousFrequency > frequency)
            base *= frequency / previousFrequency
        val longBase = if (established) min(longestEvidence!!, base * .48) else 0.0
        val distance = goal.distance().kilometres()
        var taperDays =
            if (race == null) 0
            else if (goal.distance() == RaceDistance.MARATHON) {
                if (base >= 45) 21 else 14
            } else if (goal.distance() == RaceDistance.HALF_MARATHON) 10 else 6
        taperDays = min(taperDays, max(0, days(start, end).toInt()))
        val taperStart = end.minusDays(taperDays.toLong())
        val phases = phases(start, end, race, taperStart, established, weeks)
        val workouts = mutableListOf<PlannedWorkout>()
        val heavy = StrengthAnalytics.hardLowerBodyDates(request.strengthSessions())
        var volumeCap =
            when (goal.distance()) {
                RaceDistance.MARATHON -> max(base, 60.0)
                RaceDistance.HALF_MARATHON -> max(base, 42.0)
                RaceDistance.TEN_K -> max(base, 32.0)
                else -> max(base, 26.0)
            }
        if (goal.distance() == RaceDistance.GENERAL_FITNESS) volumeCap = max(base, 30.0)
        val longCap =
            when (goal.distance()) {
                RaceDistance.MARATHON -> 30.0
                RaceDistance.HALF_MARATHON -> 18.0
                RaceDistance.TEN_K -> 11.0
                RaceDistance.FIVE_K -> 7.0
                else -> 12.0
            }
        val caution =
            if (
                race != null &&
                    distance > 10 &&
                    (!established || longBase < distance * .45) &&
                    weeks < 12
            )
                " Your current history gives less preparation time than normally recommended for this distance. This plan does not accelerate mileage to catch up. Consider changing the date/distance or keeping a conservative completion focus."
            else ""
        var previousDemanding: LocalDate? = null
        val initialMonday = monday(start)
        var week = initialMonday
        while (!after(week, end)) {
            val from = if (before(week, start)) start else week
            val to = if (after(week.plusDays(6), end)) end else week.plusDays(6)
            val index = (days(initialMonday, week) / 7).toInt()
            val phase = phaseAt(phases, from)
            val taper = race != null && !before(to, taperStart)
            val wantsQuality =
                qualitySupported &&
                    !taper &&
                    (race == null || days(from, race) <= 140) &&
                    prefs.strategy() != TrainingStrategy.ZONE_2 &&
                    goal.distance() != RaceDistance.GENERAL_FITNESS &&
                    (prefs.strategy() != TrainingStrategy.PERFORMANCE || performance) &&
                    (prefs.strategy() != TrainingStrategy.DISTANCE_FIRST ||
                        phase == TrainingPhase.Kind.RACE_SPECIFIC ||
                        phase == TrainingPhase.Kind.PEAK)
            val blocked = HashSet(request.unavailableDates())
            val schedule =
                ConstraintScheduler.select(
                    from,
                    to,
                    frequency,
                    prefs,
                    blocked,
                    heavy,
                    wantsQuality,
                    race,
                )
            val selectedDays = requireNotNull(schedule.days())
            var quality = schedule.quality()
            if (
                quality != null && previousDemanding != null && days(previousDemanding, quality) < 2
            )
                quality = null
            val step = min(2.0, max(.5, base / 30))
            val foundationWeeks =
                if (race == null) 0 else max(0, days(start, race.minusDays(196)).toInt()) / 7
            val distantFoundation = race != null && days(from, race) > 196
            val buildIndex = if (foundationWeeks > 0) max(0, index - foundationWeeks) else index
            val foundationVolume =
                if (foundationWeeks > 0)
                    min(6.0, max(0, foundationWeeks - foundationWeeks / 4) * step)
                else 0.0
            var build =
                if (distantFoundation)
                    min(volumeCap, min(base + 6, base + max(0, index - index / 4) * step))
                else
                    min(
                        volumeCap,
                        base + foundationVolume + max(0, buildIndex - buildIndex / 4) * step,
                    )
            if (index % 4 == 3) build *= .8
            val taperFactor =
                if (taper) {
                    if (days(from, end) <= 7) .5 else .7
                } else 1.0
            val weekly = build * taperFactor * selectedDays.size / frequency
            val longProgress =
                if (distantFoundation) min(3.0, max(0, index - index / 4) * .5)
                else
                    (if (foundationWeeks > 0)
                        min(3.0, max(0, foundationWeeks - foundationWeeks / 4) * .5)
                    else 0.0) + max(0, buildIndex - buildIndex / 4) * .5
            var longKm = min(longCap, min(longBase + longProgress, weekly * .48))
            if (index % 4 == 3) longKm = min(longKm, longBase * .8 + index * .15)
            if (request.distribution() == "MORE_SHORTER") longKm *= .85
            longKm *= taperFactor
            val n = selectedDays.size
            // Each day's role is settled before any distance is allocated (Piece 1). A long run
            // that would follow a demanding day too closely, including one at the end of the
            // previous week, moves to another available day when spacing allows; otherwise the
            // week has no long run and its whole budget is shared by the remaining runs, so the
            // planned kilometres no longer silently disappear.
            val ordered = selectedDays.sortedBy { it.toEpochDay() }
            val demandingBefore = previousDemanding
            var effectiveQuality: LocalDate? = null
            var effectiveLong: LocalDate? = null
            var longDemoted = false
            run {
                var last = demandingBefore
                for (day in ordered) {
                    val wantsLong = n >= 2 && day == schedule.longRun() && !taper
                    val wantsQuality = day == quality
                    if (last != null && days(last, day) < 2) {
                        if (wantsLong) longDemoted = true
                        continue
                    }
                    if (wantsLong) effectiveLong = day
                    if (wantsQuality) effectiveQuality = day
                    if (wantsLong || wantsQuality) last = day
                }
            }
            var longMoved = false
            if (longDemoted) {
                fun spaced(a: LocalDate?, b: LocalDate) =
                    a == null || kotlin.math.abs(days(a, b)) >= 2
                effectiveLong =
                    ordered
                        .filter {
                            it != effectiveQuality &&
                                spaced(demandingBefore, it) &&
                                spaced(effectiveQuality, it)
                        }
                        .maxByOrNull { it.toEpochDay() }
                longMoved = effectiveLong != null
            }
            val hasLong = effectiveLong != null
            val sessionCap = min(longCap, longBase + max(0, index - index / 4) * .5)
            val maximumMinutesBudget = request.maximumMinutes()
            val easyPace = baseline.easyPaceSecondsPerKm()
            // With a pace estimate, the usual session budget becomes a real limit for everyday
            // runs. The long run is exempt by design and its estimated duration is explained.
            val budgetKm =
                if (established && maximumMinutesBudget != null && easyPace != null && easyPace > 0)
                    maximumMinutesBudget * 60.0 / easyPace
                else null
            val plannedKm = mutableMapOf<LocalDate, Double>()
            if (established)
                for (day in ordered) {
                    val raw =
                        if (n == 1) weekly
                        else if (day == effectiveLong) longKm
                        else if (hasLong) (weekly - longKm) / (n - 1) else weekly / n
                    var km = rounded(min(rounded(raw), sessionCap))
                    if (budgetKm != null && day != effectiveLong)
                        km = min(km, kotlin.math.floor(budgetKm * 10) / 10)
                    plannedKm[day] = km
                }
            val plannedTotal = plannedKm.values.sum()
            val shortfall = if (established) weekly - plannedTotal else 0.0
            val capacityNote =
                if (shortfall > 0.15 * n + 0.05)
                    "This week's runs add up to ${rounded(plannedTotal)} km, below the ${rounded(weekly)} km budget, because " +
                        (if (
                            budgetKm != null &&
                                plannedKm.any { (d, km) ->
                                    d != effectiveLong &&
                                        km >= kotlin.math.floor(budgetKm * 10) / 10
                                }
                        )
                            "everyday runs are kept within your usual ${maximumMinutesBudget}-minute session budget"
                        else "a single run is limited to ${rounded(sessionCap)} km at this stage") +
                        "; the difference is not added elsewhere. "
                else ""
            val relocationNote =
                if (longMoved)
                    "The long run moved within this week so it does not follow another demanding run too closely. "
                else if (longDemoted)
                    "No day this week leaves enough recovery after the previous demanding run, so there is no long run and the week's distance is shared across the other runs. "
                else ""
            for (day in selectedDays) {
                var isLong = day == effectiveLong
                var isQuality = day == effectiveQuality
                var category =
                    if (isLong) WorkoutCategory.LONG_RUN
                    else if (isQuality) {
                        if (
                            phase == TrainingPhase.Kind.RACE_SPECIFIC ||
                                phase == TrainingPhase.Kind.PEAK
                        )
                            WorkoutCategory.RACE_PACE_RUN
                        else WorkoutCategory.TEMPO_THRESHOLD_RUN
                    } else if (
                        prefs.strategy() == TrainingStrategy.ZONE_2 ||
                            goal.distance() == RaceDistance.GENERAL_FITNESS
                    )
                        WorkoutCategory.ZONE_2_RUN
                    else WorkoutCategory.EASY_RUN
                val km: Double? = if (established) plannedKm[day] else null
                if (km != null) {
                    if (isQuality && km < 4) {
                        isQuality = false
                        category = WorkoutCategory.EASY_RUN
                    }
                }
                val maximumMinutes = request.maximumMinutes()
                val duration =
                    if (established) null
                    else
                        Durations.ofMinutes(
                            min(
                                    if (maximumMinutes == null) 30 else min(30, maximumMinutes),
                                    15 + index / 2 * 2,
                                )
                                .toLong()
                        )
                val segments = mutableListOf<WorkoutSegment>()
                val rpe =
                    if (isQuality) {
                        if (prefs.strategy() == TrainingStrategy.PERFORMANCE && performance) 7.0
                        else 6.0
                    } else 3.0
                if (!established) {
                    segments.add(
                        segment(
                            WorkoutSegment.Kind.WARM_UP,
                            Durations.ofMinutes(5),
                            2.0,
                            "Comfortable walk; shorten or pause if needed.",
                        )
                    )
                    val minutes = (duration!!.getSeconds() / 60).toInt() - 10
                    segments.add(
                        segment(
                            WorkoutSegment.Kind.RUN,
                            Durations.ofMinutes(minutes.toLong()),
                            3.0,
                            "Alternate comfortable running and walking as needed; conversational effort. Continuous running is optional.",
                        )
                    )
                    segments.add(
                        segment(
                            WorkoutSegment.Kind.COOL_DOWN,
                            Durations.ofMinutes(5),
                            2.0,
                            "Comfortable walk.",
                        )
                    )
                } else if (isQuality && km != null && km >= 4) {
                    segments.add(
                        WorkoutSegment(
                            WorkoutSegment.Kind.WARM_UP,
                            1.0,
                            null,
                            null,
                            null,
                            null,
                            3.0,
                            "Easy conversational warm-up.",
                        )
                    )
                    val target = goal.targetTime()
                    val pace =
                        if (category == WorkoutCategory.RACE_PACE_RUN && target != null)
                            target.toMillis() / 1000.0 / distance
                        else null
                    segments.add(
                        WorkoutSegment(
                            WorkoutSegment.Kind.RUN,
                            km - 2,
                            null,
                            pace?.times(.98),
                            pace?.times(1.04),
                            null,
                            rpe,
                            "Controlled sustained work; effort takes precedence over target pace. Slow down or shorten if this becomes a maximal effort.",
                        )
                    )
                    segments.add(
                        WorkoutSegment(
                            WorkoutSegment.Kind.COOL_DOWN,
                            1.0,
                            null,
                            null,
                            null,
                            null,
                            2.0,
                            "Easy cool-down.",
                        )
                    )
                } else
                    segments.add(
                        WorkoutSegment(
                            WorkoutSegment.Kind.RUN,
                            km,
                            null,
                            null,
                            null,
                            null,
                            3.0,
                            "Easy conversational effort; generally able to speak in full sentences. Walk breaks are welcome.",
                        )
                    )
                var reason =
                    (if (isLong)
                        "Develop endurance for ${goal.distance().label()}. Long-run starting evidence: ${rounded(longBase)} km; progression is limited by the weekly budget. "
                    else if (isQuality)
                        "Develop controlled race-specific endurance alongside easy training; recent volume and consistency support one quality session this week. "
                    else
                        "Build aerobic endurance while keeping effort controlled to leave room for recovery. ") +
                        (if (established)
                            "Starting availability-adjusted weekly evidence: ${rounded(base)} km. This week's budget: ${rounded(weekly)} km. "
                        else
                            "Current workload evidence is missing, low or stale, so this is a short run/walk session rather than a mileage prescription. ") +
                        "Phase: $phase. " +
                        (if (taper)
                            "Volume is reduced before race day; arriving recovered is the priority. "
                        else "") +
                        (if (prefs.strategy() == TrainingStrategy.PERFORMANCE && !performance)
                            "Current evidence does not support extra Performance intensity; your saved preference is unchanged. "
                        else "") +
                        schedule.explanation() +
                        caution
                if (maximumMinutes != null) {
                    reason += " Your usual time preference is $maximumMinutes minutes. "
                    if (km != null && easyPace != null && easyPace > 0) {
                        val estimate = kotlin.math.round(km * easyPace / 60).toLong()
                        reason +=
                            if (isLong && estimate > maximumMinutes)
                                "This long run is estimated at about $estimate minutes at your usual easy pace, longer than your usual budget by design; move it to a day with more time or shorten by effort. "
                            else "Estimated about $estimate minutes at your usual easy pace. "
                    } else if (km != null)
                        reason +=
                            "This distance prescription has no reliable pace-based duration estimate; shorten by effort to your time budget rather than adding catch-up distance. "
                }
                reason += (if (isLong || !hasLong) relocationNote else "") + capacityNote
                reason +=
                    " Session distribution: ${request.distribution().lowercase().replace('_', ' ')}; requested running frequency is a maximum, not an obligation."
                workouts.add(
                    PlannedWorkout("$id:$day", day, category, km, duration, segments, reason)
                )
                if (isLong || isQuality) previousDemanding = day
            }
            week = week.plusWeeks(1)
        }
        if (race != null)
            workouts.add(
                PlannedWorkout(
                    "$id:race",
                    race,
                    WorkoutCategory.RACE,
                    distance,
                    null,
                    Immutable.list(
                        WorkoutSegment(
                            WorkoutSegment.Kind.RUN,
                            distance,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Runner-owned race goal. Adjust effort to conditions and how you feel; completion is not guaranteed by a training plan.",
                        )
                    ),
                    "Your chosen race anchors this programme. The target has not been changed." +
                        caution,
                )
            )
        workouts.sortBy { it.date().toEpochDay() }
        if (race != null) {
            val monday = monday(race)
            while (workouts.count { !before(it.date(), monday) } > prefs.runsPerWeek()) {
                for (i in workouts.size - 1 downTo 0) if (
                    workouts[i].category() != WorkoutCategory.RACE &&
                        !before(workouts[i].date(), monday)
                ) {
                    workouts.removeAt(i)
                    break
                }
            }
        }
        return TrainingPlan(id, start, end, phases, workouts)
    }

    companion object {
        private fun days(from: LocalDate, to: LocalDate): Long = to.toEpochDay() - from.toEpochDay()

        private fun before(a: LocalDate, b: LocalDate): Boolean = a.toEpochDay() < b.toEpochDay()

        private fun after(a: LocalDate, b: LocalDate): Boolean = a.toEpochDay() > b.toEpochDay()

        private fun monday(day: LocalDate): LocalDate =
            day.minusDays((day.getDayOfWeek().getValue() - 1).toLong())

        private fun rounded(value: Double): Double = roundedNanos(value * 10) / 10.0

        private fun segment(
            kind: WorkoutSegment.Kind,
            duration: Duration,
            rpe: Double,
            guidance: String,
        ): WorkoutSegment = WorkoutSegment(kind, null, duration, null, null, null, rpe, guidance)

        @JvmStatic
        fun phaseAt(phases: List<TrainingPhase>, date: LocalDate): TrainingPhase.Kind =
            phases
                .firstOrNull { !before(date, it.startDate()) && !after(date, it.endDate()) }
                ?.kind() ?: TrainingPhase.Kind.FOUNDATION

        private fun phases(
            start: LocalDate,
            end: LocalDate,
            race: LocalDate?,
            taper: LocalDate,
            established: Boolean,
            weeks: Int,
        ): List<TrainingPhase> {
            val result = mutableListOf<TrainingPhase>()
            var previous: TrainingPhase.Kind? = null
            var phaseStart = start
            val horizon = days(start, end) + 1
            var day = start
            val beyond = end.plusDays(1)
            while (!after(day, beyond)) {
                val index = days(start, day)
                val kind: TrainingPhase.Kind? =
                    if (after(day, end)) null
                    else if (race != null && day == race) TrainingPhase.Kind.RACE
                    else if (race != null && !before(day, taper)) TrainingPhase.Kind.TAPER
                    else if (race != null && horizon > 280) {
                        if (days(day, race) > 196) TrainingPhase.Kind.FOUNDATION
                        else if (days(day, race) > 140) TrainingPhase.Kind.ENDURANCE_DEVELOPMENT
                        else if (days(day, race) > 28) TrainingPhase.Kind.RACE_SPECIFIC
                        else TrainingPhase.Kind.PEAK
                    } else if (
                        index <
                            (if (established) {
                                if (weeks >= 20) horizon * .2 else 0.0
                            } else horizon * .3)
                    )
                        TrainingPhase.Kind.FOUNDATION
                    else if (race == null || index < horizon * .6)
                        TrainingPhase.Kind.ENDURANCE_DEVELOPMENT
                    else if (index < horizon * .85) TrainingPhase.Kind.RACE_SPECIFIC
                    else TrainingPhase.Kind.PEAK
                if (kind != previous) {
                    if (previous != null)
                        result.add(
                            TrainingPhase(
                                previous,
                                phaseStart,
                                day.minusDays(1),
                                "Allocated from the actual $horizon-day horizon and ${if (established) "established" else "limited"} running evidence. Phase boundaries are projections, not fixed-length schedules.",
                            )
                        )
                    phaseStart = day
                    previous = kind
                }
                day = day.plusDays(1)
            }
            return Immutable.copyList(result)
        }
    }
}
