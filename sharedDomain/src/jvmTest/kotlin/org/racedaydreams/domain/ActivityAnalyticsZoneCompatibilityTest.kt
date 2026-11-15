package org.racedaydreams.domain

import java.time.*
import kotlin.test.*
import org.racedaydreams.domain.activity.*

class ActivityAnalyticsZoneCompatibilityTest {
    private fun record(id: String, at: Instant) =
        ActivityRecord(
            id,
            SourceIdentity("native", id, null),
            at,
            at.plusSeconds(600),
            ZoneOffset.ofHours(2),
            null,
            1000.0,
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

    @Test
    fun retainedJvmApiProjectsUsingRealDstAndUnusualOffsets() {
        for (zone in
            listOf(
                "UTC",
                "Europe/London",
                "America/New_York",
                "Pacific/Auckland",
                "Asia/Kathmandu",
                "+18:00",
                "-18:00",
            )) {
            val display = ZoneId.of(zone)
            val today = LocalDate.of(2026, 11, 1)
            val instants =
                listOf(
                    Instant.parse("2026-10-25T00:30:00Z"),
                    Instant.parse("2026-10-25T01:30:00Z"),
                    Instant.parse("2026-11-01T00:30:00Z"),
                    Instant.parse("2026-11-02T00:30:00Z"),
                )
            val rows = instants.mapIndexed { i, at -> record("r$i", at) }
            val week = ActivityAnalytics.weekly(rows, today, display, 1).single()
            val start = LocalDate.of(2026, 10, 26)
            val dates =
                instants
                    .map { it.atZone(display).toLocalDate() }
                    .filter { !it.isBefore(start) && !it.isAfter(today) }
            assertEquals(dates.size, week.activityCount())
            assertEquals(dates.distinct().size, week.runningDays())
            assertEquals(dates.size * 1000.0, week.distanceMetres())
        }
    }

    @Test
    fun unusedNullDisplayZoneRetainsOriginalEmptyHistoryBehavior() {
        assertEquals(
            4,
            ActivityAnalytics.weekly(emptyList(), LocalDate.of(2026, 11, 15), null, 4).size,
        )
    }
}
