package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.race.RacePredictionEngine
import org.racedaydreams.domain.time.*

/**
 * Audit B-01: runs without a source offset (every HealthKit import, some Health Connect records)
 * are dated in the device's time zone by every screen and engine, not in UTC by some of them.
 */
class ActivityLocalDatingTest {
    private fun hours(h: Int) = Offsets.ofTotalSeconds(h * 3600)

    /** The instant of a local wall-clock time at a fixed offset. */
    private fun local(y: Int, m: Int, d: Int, h: Int, min: Int, offsetHours: Int): Instant =
        Instants.ofEpochSecond(
            CalendarDates.of(y, m, d).toEpochDay() * 86400 + h * 3600 + min * 60 -
                offsetHours * 3600
        )

    private fun utcSecond(y: Int, m: Int, d: Int, h: Int) =
        CalendarDates.of(y, m, d).toEpochDay() * 86400 + h * 3600

    // America/New_York in 2026: EDT (-4) from 8 March 07:00 UTC to 1 November 06:00 UTC.
    private val newYork = LocalZone { at ->
        hours(
            if (at.getEpochSecond() in utcSecond(2026, 3, 8, 7) until utcSecond(2026, 11, 1, 6)) -4
            else -5
        )
    }

    // Australia/Sydney in 2026: AEST (+10) from 4 April 16:00 UTC to 3 October 16:00 UTC.
    private val sydney = LocalZone { at ->
        hours(
            if (at.getEpochSecond() in utcSecond(2026, 4, 4, 16) until utcSecond(2026, 10, 3, 16))
                10
            else 11
        )
    }

    private fun run(
        id: String,
        start: Instant,
        km: Double = 10.0,
        offset: ZoneOffset? = null,
        planned: String? = null,
        provider: String = "apple-health",
    ) =
        ActivityRecord(
            id,
            SourceIdentity(provider, id, null),
            start,
            Instants.ofEpochSecond(start.getEpochSecond() + 3000),
            offset,
            offset,
            km * 1000,
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

    @Test
    fun offsetLessRunsAroundMidnightUseTheDeviceDayEastAndWestOfUtc() {
        val day = CalendarDates.of(2026, 11, 16)
        for (offset in listOf(10, -8)) ActivityDates.withZone(LocalZone.fixed(hours(offset))) {
            assertEquals(
                day,
                ActivityDates.localDate(run("late", local(2026, 11, 16, 23, 30, offset))),
            )
            assertEquals(
                day,
                ActivityDates.localDate(run("early", local(2026, 11, 16, 0, 30, offset))),
            )
        }
    }

    @Test
    fun aRecordedOffsetStillWinsOverTheDeviceZone() {
        ActivityDates.withZone(LocalZone.fixed(hours(-8))) {
            // 23:30 on 16 November in UTC+10 is 05:30 on 16 November in UTC-8, but the source
            // recorded where the run happened.
            val travelled = run("away", local(2026, 11, 16, 23, 30, 10), offset = hours(10))
            assertEquals(CalendarDates.of(2026, 11, 16), ActivityDates.localDate(travelled))
            val other = run("other", local(2026, 11, 17, 0, 30, 10), offset = hours(10))
            assertEquals(CalendarDates.of(2026, 11, 17), ActivityDates.localDate(other))
        }
    }

    @Test
    fun nativeRecordingsPlaceholderUtcOffsetIsTreatedAsUnknown() {
        ActivityDates.withZone(LocalZone.fixed(hours(10))) {
            // This app's recordings and GPX imports were stored with "Z" rather than the zone.
            val start = local(2026, 11, 16, 6, 0, 10) // Sunday 20:00 UTC
            for (provider in listOf("rdd", "gpx")) assertEquals(
                CalendarDates.of(2026, 11, 16),
                ActivityDates.localDate(
                    run(provider, start, offset = hours(0), provider = provider)
                ),
            )
            // A health platform's explicit UTC offset is respected.
            assertEquals(
                CalendarDates.of(2026, 11, 15),
                ActivityDates.localDate(
                    run("hc", start, offset = hours(0), provider = "health-connect")
                ),
            )
            assertNull(ActivityDates.recordedOffset("rdd", hours(0)))
            assertEquals(hours(2), ActivityDates.recordedOffset("rdd", hours(2)))
        }
    }

    @Test
    fun daylightSavingChangeDaysResolveTheOffsetPerInstant() {
        ActivityDates.withZone(newYork) {
            // Spring forward, 8 March 2026: 00:30 is EST (-5), 23:30 is EDT (-4).
            val march8 = CalendarDates.of(2026, 3, 8)
            assertEquals(march8, ActivityDates.localDate(run("a", local(2026, 3, 8, 0, 30, -5))))
            assertEquals(march8, ActivityDates.localDate(run("b", local(2026, 3, 8, 23, 30, -4))))
            // Fall back, 1 November 2026: 00:30 is EDT (-4), 23:30 is EST (-5).
            val november1 = CalendarDates.of(2026, 11, 1)
            assertEquals(
                november1,
                ActivityDates.localDate(run("c", local(2026, 11, 1, 0, 30, -4))),
            )
            assertEquals(
                november1,
                ActivityDates.localDate(run("d", local(2026, 11, 1, 23, 30, -5))),
            )
            // Local midnight follows the offset in force on each side of the change.
            assertEquals(
                utcSecond(2026, 3, 8, 5),
                ActivityDates.startOfDeviceDay(march8).getEpochSecond(),
            )
            assertEquals(
                utcSecond(2026, 3, 9, 4),
                ActivityDates.startOfDeviceDay(march8.plusDays(1)).getEpochSecond(),
            )
            assertEquals(
                utcSecond(2026, 11, 2, 5),
                ActivityDates.startOfDeviceDay(november1.plusDays(1)).getEpochSecond(),
            )
        }
        ActivityDates.withZone(sydney) {
            // 5 April 2026 00:30 AEDT (+11) is 4 April 13:30 UTC.
            val start = local(2026, 4, 5, 0, 30, 11)
            assertEquals(CalendarDates.of(2026, 4, 5), ActivityDates.localDate(run("e", start)))
            // Back at +10 the same evening.
            val evening = local(2026, 4, 5, 23, 30, 10)
            assertEquals(CalendarDates.of(2026, 4, 5), ActivityDates.localDate(run("f", evening)))
        }
    }

    private fun version(workouts: List<PlannedWorkout> = emptyList()): PlanVersion {
        val today = CalendarDates.of(2026, 11, 15)
        return PlanVersion(
            "v",
            "p",
            1,
            Instants.ofEpochSecond(today.toEpochDay() * 86400),
            "",
            UserProfile(
                RaceGoal(RaceDistance.HALF_MARATHON, "", null, GoalMode.FINISH, null),
                TrainingPreferences(
                    TrainingStrategy.BALANCED,
                    DayOfWeek.values().toSet(),
                    4,
                    DayOfWeek.SUNDAY,
                    emptySet(),
                    ScheduleType.FIXED,
                    UnitSystem.METRIC,
                ),
                false,
            ),
            "",
            TrainingPlan("p", today.minusDays(28), today.plusDays(28), emptyList(), workouts),
        )
    }

    private fun weeklyKm(runs: List<ActivityRecord>): Double {
        val v = version()
        // Reviewed week: Monday 2 November to Sunday 8 November 2026.
        return WeeklyCoaching.review(
                v,
                listOf(v),
                runs,
                emptyList(),
                emptyList(),
                emptyList(),
                CalendarDates.of(2026, 11, 15),
                null,
            )
            .kilometres()
    }

    @Test
    fun weeklyReviewWeekBoundaryUsesTheDeviceZone() {
        // Sunday 8 November 23:30 local in UTC-8 is Monday 07:30 UTC: it belongs to the
        // reviewed week, not the next one.
        ActivityDates.withZone(LocalZone.fixed(hours(-8))) {
            assertEquals(10.0, weeklyKm(listOf(run("west", local(2026, 11, 8, 23, 30, -8)))))
            assertEquals(0.0, weeklyKm(listOf(run("next", local(2026, 11, 9, 0, 30, -8)))))
        }
        // Monday 9 November 00:30 local in UTC+10 is Sunday 14:30 UTC: it is next week's run.
        ActivityDates.withZone(LocalZone.fixed(hours(10))) {
            assertEquals(0.0, weeklyKm(listOf(run("east", local(2026, 11, 9, 0, 30, 10)))))
            assertEquals(10.0, weeklyKm(listOf(run("sunday", local(2026, 11, 8, 23, 30, 10)))))
        }
    }

    @Test
    fun calendarHistoryAndRacePredictionAgreeOnTheDeviceDay() {
        ActivityDates.withZone(LocalZone.fixed(hours(-5))) {
            // Monday 16 November 20:00 in New York is Tuesday 01:00 UTC.
            val monday = CalendarDates.of(2026, 11, 16)
            val r = run("hk", local(2026, 11, 16, 20, 0, -5), planned = "w")
            val workout =
                PlannedWorkout("w", monday, WorkoutCategory.EASY_RUN, 10.0, null, emptyList(), "")
            val entries =
                CalendarData.day(
                    monday,
                    monday,
                    null,
                    listOf(workout),
                    listOf(r),
                    emptyList(),
                    emptyMap(),
                    emptyList(),
                )
            assertEquals(CalendarData.State.COMPLETED, entries.single().state())
            assertTrue(
                CalendarData.day(
                        monday.plusDays(1),
                        monday,
                        null,
                        listOf(workout),
                        listOf(r),
                        emptyList(),
                        emptyMap(),
                        emptyList(),
                    )
                    .none { it.activityId() == "hk" }
            )
            val history =
                WorkoutHistoryQuery.page(
                    listOf(r),
                    emptyList(),
                    emptyList(),
                    WorkoutHistoryQuery.Kind.RUNNING,
                    null,
                    null,
                    null,
                    null,
                    0,
                    10,
                )
            assertEquals(monday, history.entries()!!.single().date())
            // On Monday evening the run is today's, not tomorrow's, so race evidence includes it.
            val prediction = RacePredictionEngine.predict(5000.0, emptyList(), listOf(r), monday)
            assertTrue(
                prediction.reasons()!!.any { it.contains("longest 10") },
                prediction.reasons().toString(),
            )
        }
    }
}
