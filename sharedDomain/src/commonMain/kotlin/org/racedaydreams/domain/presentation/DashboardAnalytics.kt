package org.racedaydreams.domain.presentation

import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.RecordedWeek
import org.racedaydreams.domain.activity.RecordedWeekAggregation
import org.racedaydreams.domain.coaching.WorkoutDecision
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.ExerciseMetadata
import org.racedaydreams.domain.strength.ExerciseMetadataCatalog
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthRecordPolicy
import org.racedaydreams.domain.strength.StrengthRecords
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.strength.StrengthVolume
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.Instants
import org.racedaydreams.domain.time.LocalDate

/**
 * Read-only overview projections for the Progress and Strength dashboards.
 *
 * Every figure is derived from recorded evidence through the existing shared calculations
 * ([RecordedWeekAggregation], [StrengthVolume], [StrengthRecords], [StrengthRecordPolicy]) and the
 * calendar's existing plan-link rules; nothing here estimates missing measurements, changes plans
 * or writes data. A `null` value means there is not enough recorded evidence and must be shown as
 * such, never as zero.
 *
 * Work is bounded: each call filters the already-loaded summaries once by date and never reads
 * route points or other retained detail.
 */
object DashboardAnalytics {
    /** Reporting periods. Each is a whole number of calendar weeks ending with this week. */
    enum class Period(val label: String, val weeks: Int) {
        FOUR_WEEKS("4 weeks", 4),
        TWELVE_WEEKS("12 weeks", 12),
        SIX_MONTHS("6 months", 26),
        ONE_YEAR("1 year", 52),
    }

    /** Inclusive calendar-date range. */
    data class Window(val start: LocalDate, val end: LocalDate) {
        fun contains(date: LocalDate): Boolean =
            date.toEpochDay() >= start.toEpochDay() && date.toEpochDay() <= end.toEpochDay()

        fun days(): Int = (end.toEpochDay() - start.toEpochDay() + 1).toInt()
    }

    private fun before(a: LocalDate, b: LocalDate): Boolean = a.toEpochDay() < b.toEpochDay()

    fun monday(date: LocalDate): LocalDate =
        date.minusDays((date.getDayOfWeek().getValue() - 1).toLong())

    /**
     * The selected period: Monday [Period.weeks] − 1 weeks ago through today (week in progress).
     */
    fun window(period: Period, today: LocalDate): Window =
        Window(monday(today).plusWeeks(-(period.weeks - 1).toLong()), today)

    /**
     * The equivalent earlier period: the same number of days, ending on the same weekday, so a
     * part-finished current week is only ever compared with the same part of an earlier week.
     */
    fun previousWindow(period: Period, today: LocalDate): Window {
        val shift = period.weeks * 7L
        val current = window(period, today)
        return Window(current.start.minusDays(shift), current.end.minusDays(shift))
    }

    /** Planned-session outcomes using the calendar's existing link and decision rules. */
    data class PlanCompletion(
        /** Sessions whose date has passed, plus any already completed today. */
        val due: Int,
        val completed: Int,
        val skipped: Int,
        /** Due, not completed and not skipped by choice. */
        val missed: Int,
    )

    /**
     * Running plan completion in [window]: a planned run is complete when an activity is linked to
     * it (the calendar rule, even if recorded on another day); a latest "skipped" decision counts
     * as skipped; a "moved" decision dates it on its new day. Today's open sessions are not yet
     * due.
     */
    fun planCompletion(
        workouts: List<PlannedWorkout>,
        activities: List<ActivityRecord>,
        decisions: List<WorkoutDecision>,
        window: Window,
        today: LocalDate,
    ): PlanCompletion {
        val linked = activities.mapNotNull { it.plannedWorkoutId() }.toSet()
        val latest = HashMap<String, WorkoutDecision>()
        for (d in decisions) {
            val old = latest[d.workoutId()]
            if (old == null || old.at().isBefore(d.at())) latest[d.workoutId()] = d
        }
        var due = 0
        var completed = 0
        var skipped = 0
        for (w in workouts.distinctBy { it.id() }) {
            val decision = latest[w.id()]
            val date =
                if (decision?.kind() == WorkoutDecision.Kind.MOVED) decision.movedTo() ?: w.date()
                else w.date()
            if (!window.contains(date)) continue
            when {
                w.id() in linked -> {
                    due++
                    completed++
                }
                decision?.kind() == WorkoutDecision.Kind.SKIPPED -> skipped++
                before(date, today) -> due++
            }
        }
        return PlanCompletion(due, completed, skipped, due - completed)
    }

    /**
     * Gym programme completion in [window]: a scheduled session is complete when the existing gym
     * link associates it with a retained actual workout (the calendar rule).
     */
    fun programmeCompletion(
        scheduled: List<Pair<String, LocalDate>>,
        completedIds: Set<String>,
        window: Window,
        today: LocalDate,
    ): PlanCompletion {
        var due = 0
        var completed = 0
        for ((id, date) in scheduled.distinctBy { it.first }) {
            if (!window.contains(date)) continue
            if (id in completedIds) {
                due++
                completed++
            } else if (before(date, today)) due++
        }
        return PlanCompletion(due, completed, 0, due - completed)
    }

    data class RecentRun(
        val activityId: String,
        val date: LocalDate,
        val distanceMetres: Double?,
        /** Moving pace from recorded moving time; null when either measurement is missing. */
        val movingSecondsPerKm: Double?,
        val planned: Boolean,
    )

    /** Totals over recorded runs in one window. */
    data class RunTotals(
        val runs: Int,
        val runningDays: Int,
        /** Sum of known distances; null when no run in the window has a distance. */
        val distanceMetres: Double?,
        val missingDistance: Int,
        /** Runs with both a positive distance and recorded moving time. */
        val pacedRuns: Int,
        /** Total moving time ÷ total distance of [pacedRuns]; never elapsed time. */
        val movingSecondsPerKm: Double?,
        val longestMetres: Double?,
        val longestActivityId: String?,
        val longestDate: LocalDate?,
    )

    data class Running(
        val period: Period,
        val window: Window,
        val previousWindow: Window,
        /** Monday to today; [weekDays] says how much of the week has elapsed. */
        val weekToDate: RunTotals,
        /** Last week's Monday to the same weekday, for a like-for-like partial-week comparison. */
        val sameDaysLastWeek: RunTotals,
        val weekDays: Int,
        val current: RunTotals,
        val previous: RunTotals,
        /**
         * Whether recorded history covers the whole earlier period, so a comparison is meaningful.
         */
        val comparable: Boolean,
        /** Oldest first; one entry per week of the period, the last still in progress. */
        val weeklyTrend: List<RecordedWeek>,
        val recentRuns: List<RecentRun>,
    ) {
        fun hasAnyRun(): Boolean = current.runs > 0 || recentRuns.isNotEmpty()
    }

    private fun totals(dated: List<Pair<ActivityRecord, LocalDate>>, window: Window): RunTotals {
        val inside = dated.filter { window.contains(it.second) }
        var known = 0.0
        var measured = 0
        var paceSeconds = 0.0
        var paceMetres = 0.0
        var paced = 0
        var longest: Pair<ActivityRecord, LocalDate>? = null
        for (pair in inside) {
            val activity = pair.first
            val metres = activity.distanceMetres() ?: continue
            known += metres
            measured++
            if (longest == null || metres > requireNotNull(longest.first.distanceMetres()))
                longest = pair
            val moving = activity.movingTime() ?: continue
            if (metres <= 0.0 || moving.toMillis() <= 0) continue
            paceSeconds += moving.toMillis() / 1000.0
            paceMetres += metres
            paced++
        }
        return RunTotals(
            runs = inside.size,
            runningDays = inside.map { it.second }.distinct().size,
            distanceMetres = if (measured > 0) known else null,
            missingDistance = inside.size - measured,
            pacedRuns = paced,
            movingSecondsPerKm = if (paced > 0) paceSeconds / (paceMetres / 1000.0) else null,
            longestMetres = longest?.first?.distanceMetres(),
            longestActivityId = longest?.first?.id(),
            longestDate = longest?.second,
        )
    }

    fun running(
        activities: List<ActivityRecord>,
        today: LocalDate,
        period: Period,
        displayDate: (Instant) -> LocalDate,
    ): Running {
        val window = window(period, today)
        val previous = previousWindow(period, today)
        val dated =
            activities
                .distinctBy { it.id() }
                .map { it to displayDate(it.start()) }
                .filter { !before(today, it.second) }
        val weekStart = monday(today)
        val weekDays = (today.toEpochDay() - weekStart.toEpochDay() + 1).toInt()
        val earliest = dated.minOfOrNull { it.second.toEpochDay() }
        val recent =
            dated
                .sortedWith(
                    compareByDescending<Pair<ActivityRecord, LocalDate>> { it.first.start() }
                        .thenBy { it.first.id() }
                )
                .take(3)
                .map { (activity, date) ->
                    val metres = activity.distanceMetres()
                    val moving = activity.movingTime()
                    RecentRun(
                        activity.id(),
                        date,
                        metres,
                        if (metres != null && metres > 0 && moving != null && moving.toMillis() > 0)
                            moving.toMillis() / metres
                        else null,
                        activity.plannedWorkoutId() != null,
                    )
                }
        return Running(
            period = period,
            window = window,
            previousWindow = previous,
            weekToDate = totals(dated, Window(weekStart, today)),
            sameDaysLastWeek = totals(dated, Window(weekStart.minusDays(7), today.minusDays(7))),
            weekDays = weekDays,
            current = totals(dated, window),
            previous = totals(dated, previous),
            comparable = earliest != null && earliest <= previous.start.toEpochDay(),
            weeklyTrend =
                RecordedWeekAggregation.weekly(activities, today, period.weeks, displayDate),
            recentRuns = recent,
        )
    }

    /** Broad display groups over the existing exercise metadata areas. */
    enum class MuscleGroup(val label: String) {
        LEGS("Legs"),
        CHEST("Chest"),
        BACK("Back"),
        SHOULDERS("Shoulders"),
        ARMS("Arms"),
        CORE("Core"),
        OTHER("Other"),
    }

    fun muscleGroup(area: ExerciseMetadata.BodyArea): MuscleGroup =
        when (area.name) {
            "QUADS",
            "HAMSTRINGS",
            "GLUTES",
            "CALVES",
            "LOWER_LEG",
            "HIP_ADDUCTORS",
            "HIP_FLEXORS" -> MuscleGroup.LEGS
            "CHEST" -> MuscleGroup.CHEST
            "UPPER_BACK",
            "LATS" -> MuscleGroup.BACK
            "SHOULDERS" -> MuscleGroup.SHOULDERS
            "BICEPS",
            "TRICEPS",
            "FOREARMS" -> MuscleGroup.ARMS
            "TRUNK" -> MuscleGroup.CORE
            else -> MuscleGroup.OTHER
        }

    /**
     * Recorded workload for one group. [primarySets] are equal shares of working sets between an
     * exercise's primary areas (so they add up to the classified total). [secondarySets] counts
     * working sets that involve the group only secondarily; they are never added to the total.
     */
    data class GroupWorkload(
        val group: MuscleGroup,
        val primarySets: Double,
        val secondarySets: Int,
    )

    /** The best existing estimate in one week, with the recorded set it came from. */
    data class OneRepMaxPoint(
        val weekStart: LocalDate,
        val kilograms: Double?,
        val sessionId: String? = null,
        val date: LocalDate? = null,
        val setKilograms: Double? = null,
        val reps: Int? = null,
    )

    /** An actual recorded working set (not an estimate). */
    data class RecordedSet(
        val sessionId: String,
        val date: LocalDate,
        val kilograms: Double,
        val reps: Int,
    )

    data class RecentWorkout(
        val sessionId: String,
        val date: LocalDate,
        val exerciseNames: List<String>,
        val workingSets: Int,
        /** Sum of load × repetitions for weighted load/rep working sets; null when none apply. */
        val loadVolumeKg: Double?,
    )

    data class Strength(
        val period: Period,
        val window: Window,
        val previousWindow: Window,
        /** Oldest first; one entry per week of the period, the last still in progress. */
        val weeks: List<StrengthVolume.Week>,
        val workouts: Int,
        val workingSets: Int,
        /** Working sets in the equivalent earlier period; null when history does not cover it. */
        val previousWorkingSets: Int?,
        /**
         * Workout-level new recorded bests (not estimates) in the period that improve an earlier
         * best.
         */
        val recordImprovements: Int,
        /** Exercises with at least one working set the existing estimate applies to. */
        val eligibleExercises: List<StrengthExercise>,
        val trendExercise: StrengthExercise?,
        val oneRepMaxTrend: List<OneRepMaxPoint>,
        /** Heaviest actual working set of [trendExercise] in the period. */
        val heaviestSet: RecordedSet?,
        /** Largest primary share first; groups with only secondary involvement follow. */
        val muscleGroups: List<GroupWorkload>,
        val unclassifiedSets: Int,
        val recentWorkouts: List<RecentWorkout>,
    ) {
        fun hasAnyWorkout(): Boolean = workouts > 0 || recentWorkouts.isNotEmpty()

        fun classifiedSets(): Double = muscleGroups.sumOf { it.primarySets }
    }

    private fun dateOf(session: StrengthSession) =
        CalendarDates.atOffset(session.startedAt(), session.offset())

    /** Metadata only for unchanged catalogue exercises, exactly as [StrengthVolume] classifies. */
    private fun metadata(exercise: StrengthExercise): ExerciseMetadata? {
        val canonical = ExerciseCatalog.definition(exercise.id())
        return if (canonical != null && canonical.exercise() == exercise)
            ExerciseMetadataCatalog.find(exercise.id())
        else null
    }

    fun strength(
        sessions: List<StrengthSession>,
        today: LocalDate,
        asOf: Instant,
        period: Period,
        trendExerciseId: String? = null,
    ): Strength {
        val window = window(period, today)
        val previous = previousWindow(period, today)
        val counted = sessions.filter { !it.startedAt().isAfter(asOf) }.distinctBy { it.id() }
        val weeks = StrengthVolume.weekly(counted, window.start, period.weeks, asOf)
        val shiftedAsOf = Instants.ofEpochSecond(asOf.getEpochSecond() - period.weeks * 7L * 86_400)
        val previousWeeks =
            StrengthVolume.weekly(counted, previous.start, period.weeks, shiftedAsOf)
        val earliest = counted.minOfOrNull { dateOf(it).toEpochDay() }
        val inPeriod = counted.filter { window.contains(dateOf(it)) }

        // One improvement per record type per workout: successive better sets in the same workout
        // are a single new record, not several.
        val improved = mutableSetOf<Pair<StrengthRecords.Key?, String?>>()
        val seenKeys = mutableSetOf<StrengthRecords.Key?>()
        for (point in StrengthRecords.progression(counted, asOf)) {
            val firstForKey = seenKeys.add(point.key())
            if (firstForKey || point.estimated()) continue
            val kind = point.key()?.kind()
            if (
                kind == StrengthRecords.Kind.EXACT_REPETITIONS_LOAD ||
                    kind == StrengthRecords.Kind.REPETITIONS_AT_LOAD
            )
                continue
            val date = point.date() ?: continue
            if (window.contains(date)) improved.add(point.key() to point.workoutId())
        }

        val eligibleSets = mutableMapOf<String, Int>()
        val exercises = mutableMapOf<String, StrengthExercise>()
        for (session in inPeriod) for (block in session.exercises()) for (set in block.sets()) {
            if (set.warmUp()) continue
            if (StrengthRecordPolicy.estimatedOneRepMax(block.exercise(), set) == null) continue
            val id = block.exercise().id()
            eligibleSets[id] = (eligibleSets[id] ?: 0) + 1
            exercises[id] = block.exercise()
        }
        val eligible =
            eligibleSets.entries
                .sortedWith(
                    compareByDescending<Map.Entry<String, Int>> { it.value }
                        .thenBy { requireNotNull(exercises[it.key]).name() }
                )
                .map { requireNotNull(exercises[it.key]) }
        val trendId = trendExerciseId?.takeIf { it in exercises } ?: eligible.firstOrNull()?.id()

        val bestByWeek = arrayOfNulls<OneRepMaxPoint>(period.weeks)
        var heaviest: RecordedSet? = null
        if (trendId != null)
            for (session in inPeriod.sortedBy { it.startedAt() }) {
                val date = dateOf(session)
                val index = ((date.toEpochDay() - window.start.toEpochDay()) / 7).toInt()
                for (block in session.exercises()) {
                    if (block.exercise().id() != trendId) continue
                    for (set in block.sets()) {
                        if (set.warmUp()) continue
                        if (
                            block.exercise().mode() == StrengthExercise.Mode.WEIGHTED &&
                                block.exercise().measurement() ==
                                    StrengthExercise.MeasurementProfile.LOAD_REPS &&
                                (heaviest == null || set.kilograms() > heaviest.kilograms)
                        )
                            heaviest = RecordedSet(session.id(), date, set.kilograms(), set.reps())
                        val estimate =
                            StrengthRecordPolicy.estimatedOneRepMax(block.exercise(), set)
                                ?: continue
                        val best = bestByWeek[index]
                        if (best?.kilograms == null || estimate > best.kilograms)
                            bestByWeek[index] =
                                OneRepMaxPoint(
                                    requireNotNull(weeks[index].monday()),
                                    estimate,
                                    session.id(),
                                    date,
                                    set.kilograms(),
                                    set.reps(),
                                )
                    }
                }
            }
        val trend =
            weeks.mapIndexed { i, week ->
                bestByWeek[i] ?: OneRepMaxPoint(requireNotNull(week.monday()), null)
            }

        val primary = mutableMapOf<MuscleGroup, Double>()
        for (week in weeks) for ((area, share) in week.primarySetShares()) {
            val group = muscleGroup(area)
            primary[group] = (primary[group] ?: 0.0) + (share ?: 0.0)
        }
        // Count each working set at most once per group, and only where that group is not already
        // a primary area of the exercise, so secondary involvement never duplicates the shares.
        val secondary = mutableMapOf<MuscleGroup, Int>()
        for (session in inPeriod) for (block in session.exercises()) {
            val meta = metadata(block.exercise()) ?: continue
            if (meta.primaryAreas().isEmpty()) continue
            val primaryGroups = meta.primaryAreas().map(::muscleGroup).toSet()
            val groups = meta.secondaryAreas().map(::muscleGroup).toSet() - primaryGroups
            if (groups.isEmpty()) continue
            val working = block.sets().count { !it.warmUp() }
            for (group in groups) secondary[group] = (secondary[group] ?: 0) + working
        }
        val workload =
            MuscleGroup.values()
                .map { GroupWorkload(it, primary[it] ?: 0.0, secondary[it] ?: 0) }
                .filter { it.primarySets > 0.0 || it.secondarySets > 0 }
                .sortedWith(
                    compareByDescending<GroupWorkload> { it.primarySets }
                        .thenByDescending { it.secondarySets }
                        .thenBy { it.group.ordinal }
                )

        val recent =
            counted
                .sortedWith(
                    compareByDescending<StrengthSession> { it.startedAt() }.thenBy { it.id() }
                )
                .take(3)
                .map { session ->
                    var working = 0
                    var volume = 0.0
                    var hasVolume = false
                    for (block in session.exercises()) {
                        val exercise = block.exercise()
                        val loadReps =
                            exercise.mode() == StrengthExercise.Mode.WEIGHTED &&
                                exercise.measurement() ==
                                    StrengthExercise.MeasurementProfile.LOAD_REPS
                        for (set in block.sets()) {
                            if (set.warmUp()) continue
                            working++
                            if (loadReps) {
                                hasVolume = true
                                volume += set.kilograms() * set.reps()
                            }
                        }
                    }
                    RecentWorkout(
                        session.id(),
                        dateOf(session),
                        session.exercises().map { it.exercise().name() }.distinct(),
                        working,
                        if (hasVolume) volume else null,
                    )
                }

        return Strength(
            period = period,
            window = window,
            previousWindow = previous,
            weeks = weeks,
            workouts = weeks.sumOf { it.workouts() },
            workingSets = weeks.sumOf { it.workingSets() },
            previousWorkingSets =
                if (earliest != null && earliest <= previous.start.toEpochDay())
                    previousWeeks.sumOf { it.workingSets() }
                else null,
            recordImprovements = improved.size,
            eligibleExercises = eligible,
            trendExercise = trendId?.let { exercises[it] },
            oneRepMaxTrend = trend,
            heaviestSet = heaviest,
            muscleGroups = workload,
            unclassifiedSets = weeks.sumOf { it.unclassifiedSets() },
            recentWorkouts = recent,
        )
    }
}
