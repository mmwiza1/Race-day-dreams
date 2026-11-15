package org.racedaydreams.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Random
import kotlin.test.*
import org.racedaydreams.domain.storage.DetailRetention

class RetentionCalendarCompatibilityTest {
    @Test
    fun cutoffsMatchOriginalOffsetDateTimeAcrossHistoricLeapAndWideDateRanges() {
        val random = Random(24)
        for (year in
            listOf(
                -999999990,
                -400,
                -1,
                0,
                1,
                1900,
                1969,
                2000,
                2024,
                2100,
                999999990,
            )) for (month in 1..12) {
            val date =
                LocalDate.of(year, month, 1)
                    .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
            val at = date.atTime(23, 59, 59, random.nextInt(1000000000)).toInstant(ZoneOffset.UTC)
            assertEquals(
                at.atOffset(ZoneOffset.UTC).minusMonths(24).toInstant(),
                DetailRetention.Policy.LAST_24_MONTHS.cutoff(at),
            )
            assertEquals(
                at.atOffset(ZoneOffset.UTC).minusMonths(60).toInstant(),
                DetailRetention.Policy.LAST_5_YEARS.cutoff(at),
            )
        }
        assertNull(DetailRetention.Policy.KEEP_FOREVER.cutoff(Instant.MAX))
    }

    @Test
    fun previewSnapshotsRejectJavaMutationAndRetainUnknownSampleCounts() {
        val now = Instant.parse("2026-11-15T12:00:00Z")
        val row =
            DetailRetention.Recording(
                "r",
                "a",
                "rdd",
                "r",
                Instant.parse("2020-01-01T00:00:00Z"),
                true,
                10,
                1,
                null,
            )
        val input = mutableListOf(row)
        val preview =
            DetailRetention.preview(DetailRetention.Policy.LAST_24_MONTHS, input, now, "epoch")
        input.clear()
        assertEquals(listOf(row), preview.recordings())
        assertEquals(1, preview.unknownSampleCounts())
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
        val javaRows = preview.recordings() as java.util.List<*>
        assertFailsWith<UnsupportedOperationException> { javaRows.clear() }
    }
}
