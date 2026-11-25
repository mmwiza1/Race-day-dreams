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
        // Piece 4: graded readiness for intensity from current evidence, replacing the single
        // 20 km/week switch. It is reassessed whenever a plan is generated or rebuilt from the
        // latest actual evidence; planned future runs are never treated as proof of fitness.
        val aerobicOnly =
            prefs.strategy() == TrainingStrategy.ZONE_2 ||
                goal.distance() == RaceDistance.GENERAL_FITNESS
        val readiness =
            when {
                !established || aerobicOnly || consistent == null || consistent < 3 ->
                    QualityWorkouts.Readiness.NONE
                weeklyEvidence!! >= 15 && consistent >= 6 && prefs.runsPerWeek() >= 3 ->
                    if (prefs.strategy() == TrainingStrategy.PERFORMANCE && performance)
                        QualityWorkouts.Readiness.PERFORMANCE
                    else QualityWorkouts.Readiness.CONTROLLED
                else -> QualityWorkouts.Readiness.STRIDES
            }
        // Returning from a break of two weeks or more in the last six: easy running first.
        val returning =
            baseline.gaps().any {
                it.daysWithoutRecordedRun() >= 14 &&
                    !it.before()
                        .isBefore(Instants.ofEpochSecond(start.minusDays(42).toEpochDay() * 86400))
            }
        val paces = QualityWorkouts.paces(baseline)
        var lastTemplate: QualityWorkouts.Template? = null
        var qualitySessions = 0
        var frequency = if (established) prefs.runsPerWeek() else min(3, prefs.runsPerWeek())
        if (request.distribution() == "FEWER_LONGER" && frequency > 2) frequency--
        var base = if (established) weeklyEvidence!! else 0.0
        val previousFrequency = baseline.runsPerWeek()
        if (established && previousFrequency != null && previousFrequency > frequency)
            base *= frequency / previousFrequency
        // Piece 3: the long run's share of the week is contextual guidance that depends on how
        // many days the runner runs (fewer runs concentrate more of the week in the long run),
        // not one universal 48% rule.
        val longShare =
            when {
                frequency <= 2 -> .6
                frequency == 3 -> .5
                frequency == 4 -> .45
                else -> .4
            } + (if (goal.distance() == RaceDistance.MARATHON) .05 else .0)
        // The share is guidance for how far the long run grows, never a reason to cut a credible,
        // recent long run the runner already does (Piece 3 correction). A recovery safeguard
        // still keeps at least 30% of each week for the other runs.
        val longBase = if (established) min(longestEvidence!!, base * .7) else 0.0
        val distance = goal.distance().kilometres()
        var taperDays =
            if (race == null) 0
            else
                when (goal.distance()) {
                    RaceDistance.MARATHON -> if (base >= 45) 21 else 14
                    RaceDistance.HALF_MARATHON -> 14
                    RaceDistance.TEN_K -> 10
                    else -> 7
                }
        taperDays = min(taperDays, max(0, days(start, end).toInt()))
        val taperStart = end.minusDays(taperDays.toLong())
        val phases = phases(start, end, race, taperStart, established, weeks)
        val workouts = mutableListOf<PlannedWorkout>()
        val heavy = StrengthAnalytics.hardLowerBodyDates(request.strengthSessions())
        // Piece 3: an individual target for the busiest week. It starts from a reference for the
        // distance, scaled by running days, and is limited by the runner's own starting volume
        // (at most about 80% more, or 10 km for small volumes). It is never below what the runner
        // already does, so established runners are not cut back. Growth towards it is bounded
        // week by week below; once reached, the week is held rather than pushed further.
        val referencePeak =
            when (goal.distance()) {
                RaceDistance.MARATHON -> 55.0
                RaceDistance.HALF_MARATHON -> 40.0
                RaceDistance.TEN_K -> 35.0
                RaceDistance.FIVE_K -> 28.0
                else -> 30.0
            } *
                when {
                    frequency <= 2 -> .7
                    frequency == 3 -> .85
                    frequency == 4 -> 1.0
                    else -> 1.15
                }
        // General fitness maintains the runner's current volume by default; it builds gently only
        // when they have chosen the Distance First approach (Piece 3 correction).
        val generalFitness = goal.distance() == RaceDistance.GENERAL_FITNESS
        val gentleProgression = prefs.strategy() == TrainingStrategy.DISTANCE_FIRST
        val volumeCap =
            if (generalFitness)
                (if (gentleProgression) max(base, min(30.0, base * 1.3 + 2)) else base)
            // Experienced runners already near or above the reference may build about 30% more.
            else max(base, min(max(referencePeak, base * 1.3), max(base * 1.8, base + 10)))
        // The long-run ceiling is the usual peak for the distance, but never below the longest run
        // the runner already does: established 5K and 10K runners keep their tolerated long runs.
        val longCap =
            max(
                when (goal.distance()) {
                    RaceDistance.MARATHON -> 32.0
                    RaceDistance.HALF_MARATHON -> if (base >= 40) 21.0 else 19.0
                    RaceDistance.TEN_K -> 14.0
                    RaceDistance.FIVE_K -> 10.0
                    else -> 14.0
                },
                if (established) longestEvidence!! else 0.0,
            )
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
        // Piece 3 progression state, carried from week to week.
        var level = base
        var lastFullLong = longBase
        val plannedLongs = mutableListOf<Pair<LocalDate, Double>>()
        val initialMonday = monday(start)
        var week = initialMonday
        while (!after(week, end)) {
            val from = if (before(week, start)) start else week
            val to = if (after(week.plusDays(6), end)) end else week.plusDays(6)
            val index = (days(initialMonday, week) / 7).toInt()
            val phase = phaseAt(phases, from)
            // Taper is decided from the week's own distance to race day, by race distance
            // (Piece 3), not by whether any day of the week touches the taper window.
            val taper = race != null && days(from, race) < taperDays
            val raceWeekNow = race != null && days(from, race) < 7
            // In the taper (not race week), only a shorter version of a familiar session.
            val sharpen = taper && !raceWeekNow && qualitySessions > 0
            val returningWeek = returning && index < 3
            val wantsQuality =
                readiness >= QualityWorkouts.Readiness.CONTROLLED &&
                    !returningWeek &&
                    (!taper || sharpen) &&
                    (race == null || days(from, race) <= 140) &&
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
            val distantFoundation = race != null && days(from, race) > 196
            // Every fourth week is lighter, relative to the current level, and never on top of a
            // taper (the two reductions no longer stack).
            val cutback = index % 4 == 3 && !taper
            if (index > 0 && !cutback && !taper) {
                // Bounded growth from the current level (about 7% a week, 0.5–3 km), slower while
                // a race is still more than 28 weeks away, and held once the target is reached.
                val step = (level * .07).coerceIn(.5, 3.0) * (if (distantFoundation) .5 else 1.0)
                level = min(volumeCap, level + step)
                // While the race is more than 28 weeks away, stay near the starting volume
                // (existing foundation safeguard): the real build starts closer to the race.
                if (distantFoundation) level = min(level, base + 6)
            }
            val build = if (cutback) level * .8 else level
            val raceWeek = race != null && days(from, race) < 7
            val taperFactor =
                if (!taper) 1.0
                else if (raceWeek) (if (distance > 10) .5 else .6)
                else if (goal.distance() == RaceDistance.MARATHON && days(from, race!!) >= 14) .85
                else .75
            val weekly = build * taperFactor * selectedDays.size / frequency
            // The long run follows its own track: no more than about 10% (and 1.5 km) beyond the
            // longest run of the last 30 days, a risk signal from research rather than a target;
            // within the week's share for the runner's running days; and within the distance's
            // usual ceiling, which is never below the runner's own longest run.
            val windowStart = from.minusDays(30)
            val recentLong =
                max(
                    if (established && days(start, from) < 30) longestEvidence!! else 0.0,
                    plannedLongs.filter { after(it.first, windowStart) }.maxOfOrNull { it.second }
                        ?: 0.0,
                )
            val progressionLimit =
                if (recentLong > 0) min(recentLong * 1.10, recentLong + 1.5) else longBase
            // Guided growth (the share for the runner's days), or holding the long run they already
            // do (scaled by the taper), whichever is larger; never beyond the 30-day guide, the
            // distance ceiling or 70% of the week.
            val heldLong = longBase * taperFactor
            var longKm =
                min(
                    longCap,
                    min(progressionLimit, min(max(weekly * longShare, heldLong), weekly * .7)),
                )
            // General-fitness maintenance also holds the long run at the runner's own level.
            if (generalFitness && !gentleProgression) longKm = min(longKm, longBase)
            if (distantFoundation) longKm = min(longKm, longBase + 3)
            if (cutback) longKm = min(longKm, lastFullLong * .8)
            // A long run kept in the taper is clearly shorter than the last full one.
            if (taper) longKm = min(longKm, lastFullLong * .7)
            if (request.distribution() == "MORE_SHORTER") longKm *= .85
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
            // In a half or marathon taper, a shorter long run is kept until 10 days out.
            fun longAllowed(day: LocalDate) =
                !taper || distance > 10 && race != null && days(day, race) >= 10
            run {
                var last = demandingBefore
                for (day in ordered) {
                    val wantsLong = n >= 2 && day == schedule.longRun() && longAllowed(day)
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
                effectiveLong =
                    relocatedLongDay(
                        ordered.filter { longAllowed(it) },
                        effectiveQuality,
                        demandingBefore,
                        heavy,
                    )
                longMoved = effectiveLong != null
            }
            val hasLong = effectiveLong != null
            // Everyday runs are never longer than the week's long run (or, with no long run, the
            // distance's long-run ceiling, itself never below the runner's own longest run).
            val sessionCap = if (hasLong) max(rounded(longKm), .1) else longCap
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
                        else
                            "everyday runs are kept no longer than ${if (hasLong) "this week's long run" else "the usual long run for this distance"} (${rounded(sessionCap)} km)") +
                        "; the difference is not added elsewhere. "
                else ""
            val relocationNote =
                if (longMoved)
                    "The long run moved within this week so it does not follow another demanding run too closely. "
                else if (longDemoted)
                    "No day this week leaves enough recovery after the previous demanding run, so there is no long run and the week's distance is shared across the other runs. "
                else ""
            // Strides: once a week on an easy run when there is no quality session, and in race
            // week at least two days before the race; never on the long run.
            val stridesDay =
                if (
                    readiness >= QualityWorkouts.Readiness.STRIDES &&
                        effectiveQuality == null &&
                        !(returning && index < 1)
                )
                    ordered.firstOrNull {
                        it != effectiveLong &&
                            (plannedKm[it] ?: 0.0) >= 3 &&
                            (race == null || days(it, race) >= 2)
                    }
                else null
            for (day in selectedDays) {
                var isLong = day == effectiveLong
                var isQuality = day == effectiveQuality
                val km: Double? = if (established) plannedKm[day] else null
                // Piece 4: a distance- and phase-specific session, smaller after hard legs or in
                // the taper; if it does not fit the day's distance, the day stays easy.
                val afterHardLegs = day in heavy || day.minusDays(1) in heavy
                val planned =
                    if (afterHardLegs) QualityWorkouts.Template.THRESHOLD
                    else if (sharpen && lastTemplate != null) lastTemplate!!
                    else QualityWorkouts.template(goal.distance(), phase, index)
                val reduced = sharpen || afterHardLegs
                // If the race-specific session does not fit the day, a threshold session is used
                // before the day is left easy.
                val session =
                    if (isQuality && km != null)
                        QualityWorkouts.build(planned, km, readiness, paces, reduced)
                            ?: QualityWorkouts.build(
                                QualityWorkouts.Template.THRESHOLD,
                                km,
                                readiness,
                                paces,
                                reduced,
                            )
                    else null
                if (session == null) isQuality = false
                // A quality day too short for a session keeps a few strides instead.
                val strides =
                    !isQuality &&
                        (day == stridesDay ||
                            day == effectiveQuality &&
                                km != null &&
                                km >= 3 &&
                                (race == null || days(day, race) >= 2))
                var category =
                    if (isLong) WorkoutCategory.LONG_RUN
                    else if (session != null) session.category
                    else if (
                        prefs.strategy() == TrainingStrategy.ZONE_2 ||
                            goal.distance() == RaceDistance.GENERAL_FITNESS
                    )
                        WorkoutCategory.ZONE_2_RUN
                    else WorkoutCategory.EASY_RUN
                val maximumMinutes = request.maximumMinutes()
                // Run/walk without distance evidence: everyday sessions stay short (up to 30
                // minutes or the usual budget). Piece 3: the weekly longer session grows gently
                // (about 3 minutes a fortnight) towards a ceiling suited to the race distance, so
                // beginners can progress beyond 30 minutes; it is shortened in the taper.
                val walkLongCap =
                    when (goal.distance()) {
                        RaceDistance.MARATHON -> 90
                        RaceDistance.HALF_MARATHON -> 70
                        RaceDistance.TEN_K -> 50
                        RaceDistance.FIVE_K -> 35
                        // General fitness: the longer session grows only with Distance First.
                        else -> if (gentleProgression) 40 else 30
                    }
                val duration =
                    if (established) null
                    else if (isLong && !(generalFitness && !gentleProgression))
                    // The runner's usual time budget, when set, still applies (athlete choice).
                    Durations.ofMinutes(
                            max(
                                    15,
                                    (min(
                                            maximumMinutes ?: walkLongCap,
                                            min(walkLongCap, 15 + index / 2 * 3),
                                        ) * taperFactor)
                                        .toInt(),
                                )
                                .let { if (maximumMinutes != null) min(it, maximumMinutes) else it }
                                .toLong()
                        )
                    else
                        Durations.ofMinutes(
                            min(
                                    if (maximumMinutes == null) 30 else min(30, maximumMinutes),
                                    15 + index / 2 * 2,
                                )
                                .toLong()
                        )
                val segments = mutableListOf<WorkoutSegment>()
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
                } else if (session != null) {
                    segments += session.segments
                } else if (strides && km != null) {
                    segments += QualityWorkouts.strides(km)
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
                        "Develop endurance for ${goal.distance().label()}. Long runs build gradually from your recent longest run (${rounded(longBase)} km): at most about 10% beyond the longest of the last 30 days, and about ${(longShare * 100).toInt()}% of the week for $frequency running days. "
                    else if (session != null)
                        session.explanation +
                            " Your recent running supports one structured session a week; every other run stays easy. "
                    else if (strides)
                        "Build aerobic endurance with easy running, finishing with a few relaxed strides for leg speed without making the run hard. " +
                            (if (readiness == QualityWorkouts.Readiness.STRIDES)
                                "A weekly structured session is added when your recent running reaches about 15 km a week over 6 consistent weeks on 3 or more days; rebuild your plan then. "
                            else "")
                    else
                        "Build aerobic endurance while keeping effort controlled to leave room for recovery. ") +
                        (if (established)
                            "Starting availability-adjusted weekly evidence: ${rounded(base)} km. This week's budget: ${rounded(weekly)} km. Busiest week this plan builds towards: about ${rounded(volumeCap)} km, reached gradually and then held. "
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
                if (session != null) {
                    qualitySessions++
                    if (!sharpen && !afterHardLegs) lastTemplate = session.template
                }
                if (isLong && km != null) {
                    plannedLongs += day to km
                    if (!cutback && !taper) lastFullLong = km
                }
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
        /**
         * Where a demoted long run can move within its week (Piece 1): only the week's scheduled,
         * available run days; at least two days from the previous demanding run and from the week's
         * quality session; and, using the scheduler's own soft recovery costs, away from hard
         * lower-body strength days and the day after them. Among equally suitable days the latest
         * is chosen. Returns null when no day leaves enough recovery.
         */
        internal fun relocatedLongDay(
            days: List<LocalDate>,
            quality: LocalDate?,
            previousDemanding: LocalDate?,
            hardLowerBody: Set<LocalDate>,
        ): LocalDate? {
            fun spaced(a: LocalDate?, b: LocalDate) = a == null || kotlin.math.abs(days(a, b)) >= 2
            return days
                .filter { it != quality && spaced(previousDemanding, it) && spaced(quality, it) }
                .minWithOrNull(
                    compareBy<LocalDate> {
                            (if (it in hardLowerBody) 90 else 0) +
                                (if (it.minusDays(1) in hardLowerBody) 120 else 0)
                        }
                        .thenByDescending { it.toEpochDay() }
                )
        }

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
