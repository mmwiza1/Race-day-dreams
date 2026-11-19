package org.racedaydreams.domain.presentation

import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.RecordedWeek
import org.racedaydreams.domain.activity.RecordedWeekAggregation
import org.racedaydreams.domain.strength.ExerciseMetadata
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthRecordPolicy
import org.racedaydreams.domain.strength.StrengthRecords
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.strength.StrengthVolume
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/**
 * Read-only overview projections for the Progress and Strength dashboards.
 *
 * Every figure is derived from recorded evidence through the existing shared calculations
 * ([RecordedWeekAggregation], [StrengthVolume], [StrengthRecords], [StrengthRecordPolicy]); nothing
 * here estimates missing measurements, changes plans or writes data. A `null` value means there is
 * not enough recorded evidence and must be shown as such, never as zero.
 */
object DashboardAnalytics {
    const val TREND_WEEKS = 6
    const val STRENGTH_WEEKS = 12
    const val RECENT_DAYS = 30
    const val WINDOW_DAYS = 28

    private fun before(a: LocalDate, b: LocalDate): Boolean = a.toEpochDay() < b.toEpochDay()

    private fun after(a: LocalDate, b: LocalDate): Boolean = a.toEpochDay() > b.toEpochDay()

    data class RecentRun(
        val activityId: String,
        val date: LocalDate,
        val distanceMetres: Double?,
        /** Moving pace from recorded moving time; null when either measurement is missing. */
        val secondsPerKm: Double?,
    )

    data class Running(
        /** This calendar week so far (in progress). */
        val thisWeek: RecordedWeek,
        /** The previous complete calendar week, for context. */
        val lastWeek: RecordedWeek,
        /** Oldest first; the final entry is the current, in-progress week. */
        val weeklyTrend: List<RecordedWeek>,
        val longestRunMetres: Double?,
        val longestRunDate: LocalDate?,
        /** Total recorded moving time ÷ distance over runs with both, last [WINDOW_DAYS] days. */
        val averageSecondsPerKm: Double?,
        val pacedRuns: Int,
        val runsInWindow: Int,
        /** Planned runs dated in the last [WINDOW_DAYS] days, before today. */
        val plannedDue: Int,
        /** Of [plannedDue], those linked to a recorded activity (same rule as the calendar). */
        val plannedCompleted: Int,
        val recentRuns: List<RecentRun>,
    ) {
        fun hasAnyRun(): Boolean =
            weeklyTrend.any { it.activityCount() > 0 } || recentRuns.isNotEmpty()
    }

    fun running(
        activities: List<ActivityRecord>,
        plannedWorkouts: List<PlannedWorkout>,
        today: LocalDate,
        displayDate: (Instant) -> LocalDate,
    ): Running {
        val weeks = RecordedWeekAggregation.weekly(activities, today, TREND_WEEKS, displayDate)
        val recentFrom = today.minusDays((RECENT_DAYS - 1).toLong())
        val windowFrom = today.minusDays((WINDOW_DAYS - 1).toLong())
        val dated =
            activities.map { it to displayDate(it.start()) }.filter { !after(it.second, today) }

        val longest =
            dated
                .filter { !before(it.second, recentFrom) && it.first.distanceMetres() != null }
                .maxByOrNull { requireNotNull(it.first.distanceMetres()) }

        var paceSeconds = 0.0
        var paceMetres = 0.0
        var paced = 0
        val inWindow = dated.filter { !before(it.second, windowFrom) }
        for ((activity, _) in inWindow) {
            val metres = activity.distanceMetres() ?: continue
            val moving = activity.movingTime() ?: continue
            if (metres <= 0.0 || moving.toMillis() <= 0) continue
            paceSeconds += moving.toMillis() / 1000.0
            paceMetres += metres
            paced++
        }

        val linked = activities.mapNotNull { it.plannedWorkoutId() }.toSet()
        val due =
            plannedWorkouts.filter { !before(it.date(), windowFrom) && before(it.date(), today) }

        val recent =
            dated
                .sortedWith(
                    compareByDescending<Pair<ActivityRecord, LocalDate>> { it.first.start() }
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
                    )
                }

        return Running(
            thisWeek = weeks.last(),
            lastWeek = weeks[weeks.size - 2],
            weeklyTrend = weeks,
            longestRunMetres = longest?.first?.distanceMetres(),
            longestRunDate = longest?.second,
            averageSecondsPerKm = if (paced > 0) paceSeconds / (paceMetres / 1000.0) else null,
            pacedRuns = paced,
            runsInWindow = inWindow.size,
            plannedDue = due.size,
            plannedCompleted = due.count { it.id() in linked },
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

    data class OneRepMaxPoint(val weekStart: LocalDate, val kilograms: Double?)

    data class RecentWorkout(
        val sessionId: String,
        val date: LocalDate,
        val exerciseNames: List<String>,
        val workingSets: Int,
        /** Sum of load × repetitions for weighted load/rep working sets; null when none apply. */
        val loadVolumeKg: Double?,
    )

    data class Strength(
        /** Oldest first; the final entry is the current, in-progress week. */
        val weeks: List<StrengthVolume.Week>,
        val plannedThisWeek: Int,
        /**
         * New recorded bests (not estimates) in the last [RECENT_DAYS] days, improving a prior
         * best.
         */
        val recordImprovements: Int,
        val trendExercise: StrengthExercise?,
        val oneRepMaxTrend: List<OneRepMaxPoint>,
        /** Primary working-set shares over the last four weeks, largest first. */
        val muscleGroupSets: List<Pair<MuscleGroup, Double>>,
        val unclassifiedSets: Int,
        val recentWorkouts: List<RecentWorkout>,
    ) {
        fun thisWeek(): StrengthVolume.Week = weeks.last()

        fun lastWeek(): StrengthVolume.Week = weeks[weeks.size - 2]

        fun hasAnyWorkout(): Boolean =
            weeks.any { it.workouts() > 0 } || recentWorkouts.isNotEmpty()
    }

    fun strength(
        sessions: List<StrengthSession>,
        plannedDates: Collection<LocalDate>,
        today: LocalDate,
        asOf: Instant,
        trendExerciseId: String? = null,
    ): Strength {
        val monday = today.minusDays((today.getDayOfWeek().getValue() - 1).toLong())
        val first = monday.plusWeeks(-(STRENGTH_WEEKS - 1).toLong())
        val weeks = StrengthVolume.weekly(sessions, first, STRENGTH_WEEKS, asOf)
        val counted = sessions.filter { !it.startedAt().isAfter(asOf) }.distinctBy { it.id() }
        fun dateOf(session: StrengthSession) =
            CalendarDates.atOffset(session.startedAt(), session.offset())

        val recentFrom = today.minusDays((RECENT_DAYS - 1).toLong())
        val seenKeys = mutableSetOf<StrengthRecords.Key?>()
        var improvements = 0
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
            if (!before(date, recentFrom) && !after(date, today)) improvements++
        }

        val eligible = mutableMapOf<String, Int>()
        val exercises = mutableMapOf<String, StrengthExercise>()
        for (session in counted) {
            if (before(dateOf(session), first)) continue
            for (block in session.exercises()) for (set in block.sets()) {
                if (set.warmUp()) continue
                if (StrengthRecordPolicy.estimatedOneRepMax(block.exercise(), set) == null) continue
                eligible[block.exercise().id()] = (eligible[block.exercise().id()] ?: 0) + 1
                exercises[block.exercise().id()] = block.exercise()
            }
        }
        val trendId =
            trendExerciseId?.takeIf { it in exercises }
                ?: eligible.entries
                    .sortedWith(
                        compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
                    )
                    .firstOrNull()
                    ?.key
        val trend =
            weeks.map { week ->
                val start = requireNotNull(week.monday())
                var best: Double? = null
                if (trendId != null)
                    for (session in counted) {
                        val date = dateOf(session)
                        if (before(date, start) || !before(date, start.plusWeeks(1))) continue
                        for (block in session.exercises()) {
                            if (block.exercise().id() != trendId) continue
                            for (set in block.sets()) {
                                if (set.warmUp()) continue
                                val estimate =
                                    StrengthRecordPolicy.estimatedOneRepMax(block.exercise(), set)
                                        ?: continue
                                best = maxOf(best ?: estimate, estimate)
                            }
                        }
                    }
                OneRepMaxPoint(start, best)
            }

        val recentWeeks = weeks.takeLast(4)
        val shares = mutableMapOf<MuscleGroup, Double>()
        for (week in recentWeeks) for ((area, share) in week.primarySetShares()) shares[
            muscleGroup(area)] = (shares[muscleGroup(area)] ?: 0.0) + (share ?: 0.0)

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

        val thisWeekEnd = monday.plusWeeks(1)
        return Strength(
            weeks = weeks,
            plannedThisWeek = plannedDates.count { !before(it, monday) && before(it, thisWeekEnd) },
            recordImprovements = improvements,
            trendExercise = trendId?.let { exercises[it] },
            oneRepMaxTrend = trend,
            muscleGroupSets =
                shares.entries
                    .filter { it.value > 0.0 }
                    .sortedWith(
                        compareByDescending<Map.Entry<MuscleGroup, Double>> { it.value }
                            .thenBy { it.key.ordinal }
                    )
                    .map { it.key to it.value },
            unclassifiedSets = recentWeeks.sumOf { it.unclassifiedSets() },
            recentWorkouts = recent,
        )
    }
}
