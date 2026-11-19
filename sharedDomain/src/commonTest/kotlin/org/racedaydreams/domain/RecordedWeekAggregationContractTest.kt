package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.time.*

class RecordedWeekAggregationContractTest {
    private fun record(id: String, epoch: Long, metres: Double?, sourceOffset: ZoneOffset? = null) =
        ActivityRecord(
            id,
            SourceIdentity("native", id, null),
            Instants.ofEpochSecond(epoch),
            Instants.ofEpochSecond(epoch + 600),
            sourceOffset,
            sourceOffset,
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
            null,
            null,
        )

    private fun utcDate(at: Instant) = CalendarDates.atOffset(at, Offsets.ofTotalSeconds(0))

    @Test
    fun everyStartWeekdayUsesTheSameIsoMondayAndOldestFirstOrdering() {
        val monday = CalendarDates.of(2026, 11, 9)
        for (day in 0L..6L) {
            val weeks =
                RecordedWeekAggregation.weekly(emptyList(), monday.plusDays(day), 4) {
                    error("No records to project")
                }
            assertEquals(
                listOf(monday.minusDays(21), monday.minusDays(14), monday.minusDays(7), monday),
                weeks.map { it.weekStart() },
            )
            assertTrue(
                weeks.all {
                    it.distanceMetres() == 0.0 &&
                        it.activityCount() == 0 &&
                        it.runningDays() == 0 &&
                        it.longestRunMetres() == null
                }
            )
            assertFails { (weeks as MutableList).clear() }
        }
    }

    @Test
    fun entirelyUnknownRecordedDistanceRemainsNullRatherThanZero() {
        val rows = listOf(record("a", 0, null), record("b", 3600, null))
        val week =
            RecordedWeekAggregation.weekly(rows, CalendarDates.of(1970, 1, 1), 1, ::utcDate)
                .single()
        assertNull(week.distanceMetres())
        assertEquals(2, week.activityCount())
        assertEquals(1, week.runningDays())
        assertEquals(2, week.missingDistanceCount())
        assertNull(week.longestRunMetres())
    }

    @Test
    fun mixedEvidenceSumsOnlyRecordedDistancesAndCountsKnownZeroAsKnown() {
        val rows =
            listOf(
                record("a", 0, null),
                record("b", 3600, 0.0),
                record("c", 7200, 2000.0),
                record("d", 86400, 3000.0),
            )
        val week =
            RecordedWeekAggregation.weekly(rows, CalendarDates.of(1970, 1, 2), 1, ::utcDate)
                .single()
        assertEquals(5000.0, week.distanceMetres())
        assertEquals(4, week.activityCount())
        assertEquals(2, week.runningDays())
        assertEquals(1, week.missingDistanceCount())
        assertEquals(3000.0, week.longestRunMetres())
        assertNull(rows.first().distanceMetres())
    }

    @Test
    fun actualDisplayDateProjectionWinsOverTheRecordedSourceOffset() {
        val rows = listOf(record("late", 85500, 5000.0, Offsets.ofTotalSeconds(7200)))
        val today = CalendarDates.of(1970, 1, 1)
        val utc = RecordedWeekAggregation.weekly(rows, today, 1, ::utcDate).single()
        val east =
            RecordedWeekAggregation.weekly(rows, today, 1) {
                    CalendarDates.atOffset(it, Offsets.ofTotalSeconds(7200))
                }
                .single()
        assertEquals(1, utc.activityCount())
        assertEquals(0, east.activityCount())
        assertEquals(7200, rows.single().startOffset()!!.getTotalSeconds())
    }

    @Test
    fun displayDayDeterminesMembershipAndFutureDaysAreExcluded() {
        val sunday = record("sunday", 3 * 86400L + 82800, 1000.0)
        val monday = record("monday", 4 * 86400L, 2000.0)
        val tuesday = record("tuesday", 5 * 86400L, 3000.0)
        val weeks =
            RecordedWeekAggregation.weekly(
                listOf(sunday, monday, tuesday),
                CalendarDates.of(1970, 1, 5),
                2,
                ::utcDate,
            )
        assertEquals(listOf(1000.0, 2000.0), weeks.map { it.distanceMetres() })
        assertEquals(listOf(1, 1), weeks.map { it.activityCount() })
    }

    @Test
    fun exactWeekRangeIsBoundedAndEmptyHistoryNeverInvokesAPlatformProvider() {
        var calls = 0
        val weeks =
            RecordedWeekAggregation.weekly(emptyList(), CalendarDates.of(2026, 11, 15), 104) {
                calls++
                error("No observations")
            }
        assertEquals(104, weeks.size)
        assertEquals(0, calls)
        for (count in listOf(0, 105)) assertFailsWith<IllegalArgumentException> {
            RecordedWeekAggregation.weekly(
                emptyList(),
                CalendarDates.of(2026, 11, 15),
                count,
                ::utcDate,
            )
        }
    }
}
