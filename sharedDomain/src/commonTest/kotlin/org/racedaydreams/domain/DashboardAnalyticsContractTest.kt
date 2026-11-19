package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.presentation.DashboardAnalytics
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
    ) =
        ActivityRecord(
            id,
            SourceIdentity("native", id, null),
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

    @Test
    fun runningOverviewUsesRecordedWeeksAndLeavesMissingMeasurementsUnknown() {
        val activities =
            listOf(
                run("this-week", today.minusDays(1), 10_000.0, 3_600),
                run("last-week", today.minusDays(8), 8_000.0, 2_880),
                run("no-distance", today.minusDays(9), null, 1_800),
                run("old-long", today.minusDays(40), 30_000.0, 10_800),
                run("future", today.plusDays(1), 99_000.0, 1),
            )
        val overview = DashboardAnalytics.running(activities, emptyList(), today, ::utcDate)

        assertEquals(DashboardAnalytics.TREND_WEEKS, overview.weeklyTrend.size)
        assertEquals(CalendarDates.of(2026, 11, 16), overview.thisWeek.weekStart())
        assertEquals(10_000.0, overview.thisWeek.distanceMetres())
        assertEquals(8_000.0, overview.lastWeek.distanceMetres())
        assertEquals(1, overview.lastWeek.missingDistanceCount())
        // Longest within 30 days only; the 30 km run is older and the future run is ignored.
        assertEquals(10_000.0, overview.longestRunMetres)
        assertEquals(today.minusDays(1), overview.longestRunDate)
        // (3600 + 2880) s over 18 km = 360 s/km; the run without distance is excluded.
        assertEquals(360.0, requireNotNull(overview.averageSecondsPerKm), 1e-9)
        assertEquals(2, overview.pacedRuns)
        assertEquals(3, overview.runsInWindow)
        assertEquals(
            listOf("this-week", "last-week", "no-distance"),
            overview.recentRuns.map { it.activityId },
        )
        assertNull(overview.recentRuns[2].distanceMetres)
        assertNull(overview.recentRuns[2].secondsPerKm)
    }

    @Test
    fun emptyHistoryHasNoInventedFigures() {
        val overview = DashboardAnalytics.running(emptyList(), emptyList(), today, ::utcDate)
        assertFalse(overview.hasAnyRun())
        assertNull(overview.longestRunMetres)
        assertNull(overview.averageSecondsPerKm)
        assertEquals(0, overview.plannedDue)
        assertTrue(overview.recentRuns.isEmpty())
    }

    @Test
    fun planCompletionUsesTheCalendarLinkRuleForPastPlannedRunsOnly() {
        val planned =
            listOf(
                plan("done", today.minusDays(2)),
                plan("missed", today.minusDays(3)),
                plan("today", today),
                plan("too-old", today.minusDays(40)),
            )
        val activities =
            listOf(
                run("linked", today.minusDays(2), 5_000.0, 1_800, planned = "done"),
                run("unlinked-same-day", today.minusDays(3), 5_000.0, 1_800),
            )
        val overview = DashboardAnalytics.running(activities, planned, today, ::utcDate)
        assertEquals(2, overview.plannedDue)
        assertEquals(1, overview.plannedCompleted)
    }

    private val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()
    private val squat = requireNotNull(ExerciseCatalog.definition("back-squat")).exercise()

    private fun gym(
        id: String,
        date: LocalDate,
        exercise: StrengthExercise,
        vararg sets: StrengthSession.SetEntry,
    ) =
        StrengthSession(
            id,
            at(date, 18),
            utc,
            Durations.ofMinutes(45),
            null,
            null,
            listOf(StrengthSession.ExerciseSets(exercise, sets.toList())),
        )

    private fun set(id: String, reps: Int, kg: Double, warmUp: Boolean = false) =
        StrengthSession.SetEntry(id, reps, kg, null, warmUp)

    @Test
    fun strengthOverviewReusesVolumeRecordsAndTheExistingEstimate() {
        val sessions =
            listOf(
                gym("old", today.minusDays(35), bench, set("o1", 5, 70.0)),
                gym(
                    "last",
                    today.minusDays(8),
                    bench,
                    set("l1", 5, 75.0),
                    set("lw", 10, 40.0, warmUp = true),
                ),
                gym("now", today.minusDays(1), bench, set("n1", 5, 80.0), set("n2", 5, 80.0)),
                gym("squat", today, squat, set("s1", 5, 100.0)),
            )
        val planned = listOf(today.minusDays(1), today.plusDays(2), today.minusDays(8))
        val overview = DashboardAnalytics.strength(sessions, planned, today, asOf)

        assertEquals(DashboardAnalytics.STRENGTH_WEEKS, overview.weeks.size)
        assertEquals(3, overview.thisWeek().workingSets())
        assertEquals(2, overview.thisWeek().workouts())
        assertEquals(1, overview.lastWeek().workingSets())
        assertEquals(2, overview.plannedThisWeek)
        // Bench heaviest load improved 70 → 75 → 80 within 30 days (first-ever bench and squat are
        // not improvements; estimates and per-repetition variants are not counted as PBs).
        assertEquals(2, overview.recordImprovements)

        assertEquals("bench-press", overview.trendExercise?.id())
        // The weekly trend is the best existing Epley estimate per week, from the same policy.
        val thisWeek = overview.oneRepMaxTrend.last()
        assertEquals(CalendarDates.of(2026, 11, 16), thisWeek.weekStart)
        assertEquals(
            StrengthRecordPolicy.estimatedOneRepMax(bench, set("n1", 5, 80.0)),
            thisWeek.kilograms,
        )
        assertEquals(
            StrengthRecordPolicy.estimatedOneRepMax(bench, set("l1", 5, 75.0)),
            overview.oneRepMaxTrend[overview.oneRepMaxTrend.size - 2].kilograms,
        )
        assertNull(overview.oneRepMaxTrend.first().kilograms)

        assertTrue(overview.muscleGroupSets.isNotEmpty())
        assertEquals(DashboardAnalytics.MuscleGroup.CHEST, overview.muscleGroupSets.first().first)

        assertEquals(listOf("squat", "now", "last"), overview.recentWorkouts.map { it.sessionId })
        val last = overview.recentWorkouts[2]
        assertEquals(1, last.workingSets)
        assertEquals(375.0, last.loadVolumeKg)
    }

    @Test
    fun strengthWithoutWorkoutsReportsNothingRatherThanZeroEstimates() {
        val overview = DashboardAnalytics.strength(emptyList(), emptyList(), today, asOf)
        assertFalse(overview.hasAnyWorkout())
        assertNull(overview.trendExercise)
        assertTrue(overview.oneRepMaxTrend.all { it.kilograms == null })
        assertTrue(overview.muscleGroupSets.isEmpty())
        assertEquals(0, overview.recordImprovements)
    }

    @Test
    fun everyExistingBodyAreaHasADisplayGroup() {
        for (area in ExerciseMetadata.BodyArea.values()) assertNotEquals(
            DashboardAnalytics.MuscleGroup.OTHER,
            DashboardAnalytics.muscleGroup(area),
            "Unmapped body area $area",
        )
    }
}
