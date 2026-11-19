package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.WorkoutDecision
import org.racedaydreams.domain.presentation.DashboardAnalytics
import org.racedaydreams.domain.presentation.DashboardAnalytics.MuscleGroup
import org.racedaydreams.domain.presentation.DashboardAnalytics.Period
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class DashboardAnalyticsContractTest {
    private val utc = Offsets.ofTotalSeconds(0)
    // Wednesday 18 November 2026; ISO week starts Monday 16 November.
    private val today = CalendarDates.of(2026, 11, 18)
    private val asOf = Instants.ofEpochSecond(today.toEpochDay() * 86_400 + 20 * 3600)

    private fun at(date: LocalDate, hour: Int = 7) =
        Instants.ofEpochSecond(date.toEpochDay() * 86_400 + hour * 3600L)

    private fun run(
        id: String,
        date: LocalDate,
        metres: Double?,
        movingSeconds: Long? = null,
        planned: String? = null,
        provider: String = "rdd",
    ) =
        ActivityRecord(
            id,
            SourceIdentity(provider, id, null),
            at(date),
            Instants.ofEpochSecond(
                at(date).getEpochSecond() + maxOf(3600L, (movingSeconds ?: 0L) + 60)
            ),
            utc,
            utc,
            metres,
            movingSeconds?.let { Durations.ofSeconds(it) },
            null,
            null,
            null,
            null,
            null,
            "",
            emptyList(),
            emptyList(),
            planned,
            null,
        )

    private fun utcDate(at: Instant) = CalendarDates.atOffset(at, utc)

    private fun plan(id: String, date: LocalDate) =
        PlannedWorkout(id, date, WorkoutCategory.EASY_RUN, 5.0, null, emptyList(), "")

    private fun decision(
        workout: String,
        date: LocalDate,
        kind: WorkoutDecision.Kind,
        to: LocalDate? = null,
    ) = WorkoutDecision("d-$workout", workout, "v1", date, to, kind, asOf)

    @Test
    fun periodsAreWholeWeeksEndingTodayAndComparedWithTheSameDaysEarlier() {
        val four = DashboardAnalytics.window(Period.FOUR_WEEKS, today)
        assertEquals(CalendarDates.of(2026, 10, 26), four.start)
        assertEquals(today, four.end)
        val earlier = DashboardAnalytics.previousWindow(Period.FOUR_WEEKS, today)
        assertEquals(CalendarDates.of(2026, 9, 28), earlier.start)
        // Ends on the same weekday, so the part-finished current week is matched day for day.
        assertEquals(CalendarDates.of(2026, 10, 21), earlier.end)
        assertEquals(four.days(), earlier.days())
        assertEquals(52 * 7 - 4, DashboardAnalytics.window(Period.ONE_YEAR, today).days())
        assertEquals(26, Period.SIX_MONTHS.weeks)
    }

    @Test
    fun runningTotalsLeaveMissingMeasurementsUnknownAndUseMovingPaceOnly() {
        val activities =
            listOf(
                run("this-week", today.minusDays(1), 10_000.0, 3_600),
                run("last-week-same-days", today.minusDays(8), 8_000.0, 2_880),
                run("last-week-later", today.minusDays(5), 6_000.0, null),
                run("no-distance", today.minusDays(9), null, 1_800),
                run("old-long", today.minusDays(40), 30_000.0, 10_800),
                run("future", today.plusDays(1), 99_000.0, 1),
            )
        val overview = DashboardAnalytics.running(activities, today, Period.FOUR_WEEKS, ::utcDate)

        assertEquals(Period.FOUR_WEEKS.weeks, overview.weeklyTrend.size)
        assertEquals(3, overview.weekDays)
        assertEquals(10_000.0, overview.weekToDate.distanceMetres)
        // Last week to the same weekday (Mon–Wed) only; the Friday run is excluded and the
        // Monday run without distance is counted as a run but not added to distance.
        assertEquals(8_000.0, overview.sameDaysLastWeek.distanceMetres)
        assertEquals(2, overview.sameDaysLastWeek.runs)
        assertEquals(1, overview.sameDaysLastWeek.missingDistance)

        val period = overview.current
        assertEquals(4, period.runs)
        assertEquals(24_000.0, period.distanceMetres)
        assertEquals(1, period.missingDistance)
        assertEquals(10_000.0, period.longestMetres)
        assertEquals("this-week", period.longestActivityId)
        // (3600 + 2880) s over 18 km = 360 s/km; runs without distance or moving time excluded.
        assertEquals(360.0, requireNotNull(period.movingSecondsPerKm), 1e-9)
        assertEquals(2, period.pacedRuns)

        // History starts after the earlier 4-week window began, so no comparison is claimed…
        assertFalse(overview.comparable)
        // …while the 12-week period reaches the 30 km run and its own longest-run figure.
        val longer = DashboardAnalytics.running(activities, today, Period.TWELVE_WEEKS, ::utcDate)
        assertEquals(30_000.0, longer.current.longestMetres)
        assertEquals(
            listOf("this-week", "last-week-later", "last-week-same-days"),
            overview.recentRuns.map { it.activityId },
        )
        assertNull(overview.recentRuns[1].movingSecondsPerKm)
    }

    @Test
    fun comparisonNeedsHistoryCoveringTheEarlierPeriod() {
        val activities =
            listOf(
                run("now", today.minusDays(2), 5_000.0, 1_500),
                run("before", today.minusDays(30), 4_000.0, 1_300),
                run("first", today.minusDays(60), 3_000.0, 1_000),
            )
        val overview = DashboardAnalytics.running(activities, today, Period.FOUR_WEEKS, ::utcDate)
        assertTrue(overview.comparable)
        assertEquals(4_000.0, overview.previous.distanceMetres)
    }

    @Test
    fun emptyHistoryHasNoInventedFigures() {
        val overview = DashboardAnalytics.running(emptyList(), today, Period.ONE_YEAR, ::utcDate)
        assertFalse(overview.hasAnyRun())
        assertFalse(overview.comparable)
        assertEquals(0, overview.current.runs)
        assertNull(overview.current.distanceMetres)
        assertNull(overview.current.longestMetres)
        assertNull(overview.current.movingSecondsPerKm)
        assertEquals(52, overview.weeklyTrend.size)
    }

    @Test
    fun amendedAndDeletedRecordsAreReflectedImmediately() {
        val original = listOf(run("a", today, 5_000.0, 1_500), run("b", today, 3_000.0, 900))
        val before = DashboardAnalytics.running(original, today, Period.FOUR_WEEKS, ::utcDate)
        assertEquals(8_000.0, before.weekToDate.distanceMetres)
        val amended = listOf(run("a", today, 7_000.0, 2_100))
        val after = DashboardAnalytics.running(amended, today, Period.FOUR_WEEKS, ::utcDate)
        assertEquals(7_000.0, after.weekToDate.distanceMetres)
        assertEquals(1, after.weekToDate.runs)
    }

    @Test
    fun planCompletionUsesTheCalendarLinkAndDecisionRules() {
        val planned =
            listOf(
                plan("done", today.minusDays(2)),
                plan("done-another-day", today.minusDays(4)),
                plan("missed", today.minusDays(3)),
                plan("skipped", today.minusDays(5)),
                plan("moved-later", today.minusDays(6)),
                plan("today", today),
                plan("too-old", today.minusDays(40)),
            )
        val activities =
            listOf(
                run("linked", today.minusDays(2), 5_000.0, 1_800, planned = "done"),
                run("late", today.minusDays(1), 5_000.0, 1_800, planned = "done-another-day"),
                run("unlinked-same-day", today.minusDays(3), 5_000.0, 1_800),
            )
        val decisions =
            listOf(
                decision("skipped", today.minusDays(5), WorkoutDecision.Kind.SKIPPED),
                decision(
                    "moved-later",
                    today.minusDays(6),
                    WorkoutDecision.Kind.MOVED,
                    today.plusDays(1),
                ),
            )
        val completion =
            DashboardAnalytics.planCompletion(
                planned,
                activities,
                decisions,
                DashboardAnalytics.window(Period.FOUR_WEEKS, today),
                today,
            )
        assertEquals(DashboardAnalytics.PlanCompletion(3, 2, 1, 1), completion)
    }

    @Test
    fun programmeCompletionCountsLinkedScheduledSessionsOnly() {
        val window = DashboardAnalytics.window(Period.FOUR_WEEKS, today)
        val completion =
            DashboardAnalytics.programmeCompletion(
                listOf(
                    "a" to today.minusDays(6),
                    "b" to today.minusDays(3),
                    "c" to today,
                    "d" to today.plusDays(2),
                    "old" to today.minusDays(60),
                ),
                setOf("a"),
                window,
                today,
            )
        assertEquals(DashboardAnalytics.PlanCompletion(2, 1, 0, 1), completion)
    }

    private val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()
    private val squat = requireNotNull(ExerciseCatalog.definition("back-squat")).exercise()
    private val press = requireNotNull(ExerciseCatalog.definition("overhead-press")).exercise()
    private val plank = requireNotNull(ExerciseCatalog.definition("plank")).exercise()
    private val custom =
        StrengthExercise(
            "custom-machine",
            "My machine",
            StrengthExercise.Region.UPPER,
            StrengthExercise.Mode.WEIGHTED,
            true,
            StrengthExercise.MeasurementProfile.LOAD_REPS,
        )

    private fun gym(
        id: String,
        date: LocalDate,
        vararg blocks: Pair<StrengthExercise, List<StrengthSession.SetEntry>>,
    ) =
        StrengthSession(
            id,
            at(date, 18),
            utc,
            Durations.ofMinutes(45),
            null,
            null,
            blocks.map { StrengthSession.ExerciseSets(it.first, it.second) },
        )

    private fun set(id: String, reps: Int, kg: Double, warmUp: Boolean = false) =
        StrengthSession.SetEntry(id, reps, kg, null, warmUp)

    private val sessions =
        listOf(
            gym("old", today.minusDays(51), bench to listOf(set("o1", 5, 70.0))),
            gym(
                "last",
                today.minusDays(8),
                bench to listOf(set("l1", 5, 75.0), set("lw", 10, 40.0, warmUp = true)),
            ),
            gym("now", today.minusDays(1), bench to listOf(set("n1", 5, 80.0), set("n2", 3, 82.5))),
            gym(
                "mixed",
                today,
                squat to listOf(set("s1", 5, 100.0), set("s2", 5, 100.0), set("s3", 5, 100.0)),
                press to listOf(set("p1", 8, 40.0)),
                custom to listOf(set("c1", 10, 30.0)),
            ),
        )

    @Test
    fun strengthPeriodReusesVolumeRecordsAndTheExistingEstimate() {
        val overview = DashboardAnalytics.strength(sessions, today, asOf, Period.FOUR_WEEKS)

        assertEquals(4, overview.weeks.size)
        assertEquals(3, overview.workouts)
        assertEquals(8, overview.workingSets)
        // The first session predates the earlier 4-week window, so a comparison is available.
        assertEquals(1, overview.previousWorkingSets)
        // Bench heaviest load 70 → 75 → 80 → 82.5: two workouts improved it inside the period (80
        // and 82.5 in one workout count once); first-ever lifts and estimates are not new records.
        assertEquals(2, overview.recordImprovements)

        // Eligible exercises have sets the existing estimate applies to; squat has the most.
        assertEquals("back-squat", overview.trendExercise?.id())
        assertTrue(overview.eligibleExercises.none { it.id() == "plank" })

        val selected =
            DashboardAnalytics.strength(sessions, today, asOf, Period.FOUR_WEEKS, "bench-press")
        val point = selected.oneRepMaxTrend.last()
        assertEquals(CalendarDates.of(2026, 11, 16), point.weekStart)
        val bestN1 =
            requireNotNull(StrengthRecordPolicy.estimatedOneRepMax(bench, set("n1", 5, 80.0)))
        val bestN2 =
            requireNotNull(StrengthRecordPolicy.estimatedOneRepMax(bench, set("n2", 3, 82.5)))
        assertEquals(maxOf(bestN1, bestN2), point.kilograms)
        assertEquals("now", point.sessionId)
        assertEquals(today.minusDays(1), point.date)
        // The heaviest actual set is reported separately from the estimate.
        assertEquals(
            DashboardAnalytics.RecordedSet("now", today.minusDays(1), 82.5, 3),
            selected.heaviestSet,
        )
        assertNull(selected.oneRepMaxTrend.first().kilograms)

        assertEquals(listOf("mixed", "now", "last"), overview.recentWorkouts.map { it.sessionId })
        assertEquals(375.0, overview.recentWorkouts[2].loadVolumeKg)
    }

    @Test
    fun anIneligibleExerciseSelectionFallsBackToAnEligibleOne() {
        assertNull(StrengthRecordPolicy.estimatedOneRepMax(plank, set("k1", 1, 0.0)))
        val overview =
            DashboardAnalytics.strength(sessions, today, asOf, Period.FOUR_WEEKS, "plank")
        assertEquals("back-squat", overview.trendExercise?.id())
    }

    @Test
    fun muscleWorkloadSharesPrimarySetsAndKeepsSecondaryAndUnclassifiedSeparate() {
        val pressing =
            listOf(
                gym(
                    "press",
                    today,
                    press to listOf(set("p1", 8, 40.0), set("p2", 8, 40.0)),
                    custom to listOf(set("c1", 10, 30.0)),
                )
            )
        val overview = DashboardAnalytics.strength(pressing, today, asOf, Period.FOUR_WEEKS)
        val byGroup = overview.muscleGroups.associateBy { it.group }
        // Custom exercise has no catalogue metadata: visibly unclassified, never guessed.
        assertEquals(1, overview.unclassifiedSets)
        // Primary shares add up to exactly the classified working sets (no double counting).
        assertEquals(
            (overview.workingSets - overview.unclassifiedSets).toDouble(),
            overview.classifiedSets(),
            1e-9,
        )
        assertEquals(2.0, requireNotNull(byGroup[MuscleGroup.SHOULDERS]).primarySets, 1e-9)
        // Overhead press involves arms and core secondarily: once per set, not in the total.
        assertEquals(0.0, requireNotNull(byGroup[MuscleGroup.ARMS]).primarySets, 1e-9)
        assertEquals(2, requireNotNull(byGroup[MuscleGroup.ARMS]).secondarySets)
        assertEquals(2, requireNotNull(byGroup[MuscleGroup.CORE]).secondarySets)
        assertEquals(MuscleGroup.SHOULDERS, overview.muscleGroups.first().group)
    }

    @Test
    fun strengthWithoutWorkoutsReportsNothingRatherThanZeroEstimates() {
        val overview = DashboardAnalytics.strength(emptyList(), today, asOf, Period.TWELVE_WEEKS)
        assertFalse(overview.hasAnyWorkout())
        assertNull(overview.trendExercise)
        assertNull(overview.heaviestSet)
        assertNull(overview.previousWorkingSets)
        assertTrue(overview.oneRepMaxTrend.all { it.kilograms == null })
        assertTrue(overview.muscleGroups.isEmpty())
        assertEquals(0, overview.recordImprovements)
    }

    @Test
    fun everyExistingBodyAreaHasADisplayGroup() {
        for (area in ExerciseMetadata.BodyArea.values()) assertNotEquals(
            MuscleGroup.OTHER,
            DashboardAnalytics.muscleGroup(area),
            "Unmapped body area $area",
        )
    }
}
