package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.presentation.DashboardAnalytics
import org.racedaydreams.domain.time.*

/** Weekly totals count physical runs once, whichever sources and syncs reported them. */
class PhysicalRunSessionsTest {
    // Monday 16 November 2026, 00:00 UTC.
    private val monday = 1_794_787_200L
    private val today = CalendarDates.of(2026, 11, 22)

    private fun run(
        id: String,
        provider: String,
        origin: String?,
        startMinutes: Long,
        minutes: Long,
        metres: Double?,
        planned: String? = null,
    ) =
        ActivityRecord(
            id,
            SourceIdentity(provider, id, origin),
            Instants.ofEpochSecond(monday + startMinutes * 60),
            Instants.ofEpochSecond(monday + (startMinutes + minutes) * 60),
            null,
            null,
            metres,
            null,
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

    private fun utc(at: Instant) = CalendarDates.atOffset(at, Offsets.ofTotalSeconds(0))

    private fun week(rows: List<ActivityRecord>) =
        RecordedWeekAggregation.weekly(rows, today, 1, ::utc).single()

    private val samsung = "com.sec.android.app.shealth"
    private val strava = "com.strava"

    // Two real runs: Monday evening 8 km and Thursday morning 3 km.
    private val mondaySamsung = run("hc:a", "health-connect", samsung, 18 * 60, 50, 8_000.0)
    private val mondayStrava = run("hc:b", "health-connect", strava, 18 * 60 + 1, 49, 8_050.0)
    private val thursdaySamsung =
        run("hc:c", "health-connect", samsung, 3 * 1440 + 7 * 60, 20, 3_000.0)

    @Test
    fun runnerCanCountTheFitbitDistanceAndKeepTheNativePrescribedLink() {
        val rdd = run("native", "rdd", null, 450, 33, 1500.0, planned = "long-run")
        val fitbit = run("watch", "health-connect", "com.fitbit.FitbitMobile", 450, 33, 5100.0)
        val undecided = listOf(rdd, fitbit)
        assertEquals(1500.0, week(undecided).distanceMetres())
        val choice =
            listOf(
                rdd.withDuplicateChoices(mapOf("watch" to DuplicateRunChoice.COUNT_OTHER)),
                fitbit.withDuplicateChoices(mapOf("native" to DuplicateRunChoice.COUNT_THIS)),
            )
        val group = PhysicalRunSessions.sessions(choice).single()
        assertEquals("watch", group.counted.id())
        assertEquals("long-run", group.counted.plannedWorkoutId())
        assertNull(choice.last().plannedWorkoutId())
        assertEquals(listOf("native"), group.duplicates.map { it.id() })
        assertEquals(5100.0, week(choice).distanceMetres())
        assertEquals(1, week(choice).activityCount())
        val separate =
            listOf(
                rdd.withDuplicateChoices(mapOf("watch" to DuplicateRunChoice.SEPARATE)),
                fitbit.withDuplicateChoices(mapOf("native" to DuplicateRunChoice.SEPARATE)),
            )
        assertEquals(6600.0, week(separate).distanceMetres())
        assertEquals(2, week(separate).activityCount())
    }

    @Test
    fun closeSameWriterDuplicateCanBeReviewedWithoutChangingAutomaticGrouping() {
        val first = run("manual-a", "manual", "local", 700, 20, 3300.0)
        val second = run("manual-b", "manual", "local", 700, 20, 3400.0)
        assertTrue(PhysicalRunSessions.possibleDuplicate(first, second))
        assertEquals(2, week(listOf(first, second)).activityCount())
        val reviewed =
            first.withDuplicateChoices(mapOf(second.id() to DuplicateRunChoice.COUNT_OTHER))
        assertEquals(3400.0, week(listOf(reviewed, second)).distanceMetres())
    }

    @Test
    fun theSameRunFromTwoAppsCountsOnceWithOneDistance() {
        val week = week(listOf(mondaySamsung, mondayStrava, thursdaySamsung))
        assertEquals(2, week.activityCount())
        assertEquals(2, week.runningDays())
        assertEquals(11_000.0, week.distanceMetres())
    }

    @Test
    fun aNativeRecordingAndAManualEntryOfTheSameRunCountOnce() {
        val native = run("rdd:1", "rdd", null, 18 * 60, 50, 8_010.0)
        val manual = run("manual:1", "manual", null, 18 * 60 + 5, 40, 8_000.0)
        val sessions = PhysicalRunSessions.sessions(listOf(manual, mondaySamsung, native))
        assertEquals(1, sessions.size)
        // This app's own recording supplies the totals; the others are retained as copies.
        assertEquals("rdd:1", sessions.single().counted.id())
        assertEquals(
            setOf("manual:1", "hc:a"),
            sessions.single().duplicates.map { it.id() }.toSet(),
        )
        assertEquals(8_010.0, week(listOf(manual, mondaySamsung, native)).distanceMetres())
    }

    @Test
    fun repeatedSyncsOfTheSameRecordNeverAddRuns() {
        val again = mondaySamsung.copy()
        assertEquals(1, week(listOf(mondaySamsung, again, mondaySamsung)).activityCount())
    }

    @Test
    fun separateRunsOnTheSameDayStaySeparate() {
        val morning = run("hc:m", "health-connect", samsung, 7 * 60, 30, 5_000.0)
        val evening = run("hc:e", "health-connect", samsung, 19 * 60, 30, 5_000.0)
        // A different app's evening copy merges only with the evening run it overlaps.
        val eveningCopy = run("hc:e2", "health-connect", strava, 19 * 60, 30, 5_000.0)
        val week = week(listOf(morning, evening, eveningCopy))
        assertEquals(2, week.activityCount())
        assertEquals(1, week.runningDays())
        assertEquals(10_000.0, week.distanceMetres())
    }

    @Test
    fun overlappingRecordsFromOneSourceAreNeverCombined() {
        // The same writer reporting two overlapping sessions is not evidence of a duplicate.
        val first = run("hc:1", "health-connect", samsung, 18 * 60, 50, 8_000.0)
        val second = run("hc:2", "health-connect", samsung, 18 * 60 + 10, 50, 2_000.0)
        assertEquals(2, week(listOf(first, second)).activityCount())
    }

    @Test
    fun briefOverlapBetweenDifferentSourcesIsNotEnough() {
        // A cool-down jog logged by another app starts as the main run ends: 5 of 30 minutes.
        val main = run("rdd:main", "rdd", null, 18 * 60, 60, 10_000.0)
        val other = run("hc:cool", "health-connect", samsung, 18 * 60 + 55, 30, 3_000.0)
        assertEquals(2, week(listOf(main, other)).activityCount())
    }

    @Test
    fun theRecordLinkedToAPlannedWorkoutSuppliesTheTotals() {
        val linked = run("manual:p", "manual", null, 18 * 60, 50, 8_000.0, planned = "w1")
        val sessions = PhysicalRunSessions.sessions(listOf(mondaySamsung, linked))
        assertEquals("manual:p", sessions.single().counted.id())
    }

    @Test
    fun dashboardTotalsAndRecentRunsUseTheSameSessions() {
        val rows = listOf(mondaySamsung, mondayStrava, thursdaySamsung)
        val running =
            DashboardAnalytics.running(rows, today, DashboardAnalytics.Period.FOUR_WEEKS, ::utc)
        assertEquals(2, running.weekToDate.runs)
        assertEquals(11_000.0, running.weekToDate.distanceMetres)
        assertEquals(2, running.recentRuns.size)
        assertEquals(running.weekToDate.runs, running.weeklyTrend.last().activityCount())
    }

    @Test
    fun theCalendarShowsOneRecordedRunPerPhysicalRun() {
        val entries =
            CalendarData.day(
                CalendarDates.of(2026, 11, 16),
                today,
                null,
                emptyList(),
                listOf(mondaySamsung, mondayStrava),
                emptyList(),
                emptyMap(),
                emptyList(),
            )
        assertEquals(1, entries.count { it.activityId() != null })
    }

    @Test
    fun originalRecordsAreReturnedUnchanged() {
        val rows = listOf(mondayStrava, mondaySamsung)
        val all = PhysicalRunSessions.sessions(rows).flatMap { it.records() }
        assertEquals(rows.toSet(), all.toSet())
        assertTrue(all.all { original -> rows.any { it === original } })
    }
}
