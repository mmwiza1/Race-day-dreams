package org.racedaydreams.domain

import java.time.Duration
import java.time.LocalDate
import java.util.Random
import kotlin.test.*
import org.racedaydreams.domain.time.DurationParts
import org.racedaydreams.domain.time.IsoCalendar
import org.racedaydreams.domain.time.checkedScale

/** Native primitive representation compared to retained JDK calendar/exact-duration behavior. */
class TemporalCompatibilityTest {
    @Test
    fun prolepticGregorianDaysAndTextMatchAcrossOriginalRangeAndCenturyBoundaries() {
        val random = Random(218401L)
        val years =
            listOf(
                -999999999,
                -1000000,
                -10000,
                -400,
                -100,
                -4,
                -1,
                0,
                1,
                4,
                100,
                400,
                1582,
                1900,
                1970,
                2000,
                9999,
                10000,
                1000000,
                999999999,
            ) + (-400..400).toList() + List(5000) { random.nextInt(1999999999) - 999999999 }
        for (year in years) for (month in 1..12) {
            val date = LocalDate.of(year, month, 1)
            for (day in listOf(1, date.lengthOfMonth())) {
                val original = date.withDayOfMonth(day)
                assertEquals(
                    original.toEpochDay(),
                    IsoCalendar.epochDay(year, month, day),
                    original.toString(),
                )
                assertEquals(original.toString(), IsoCalendar.text(year, month, day))
            }
            assertFails { IsoCalendar.validate(year, month, date.lengthOfMonth() + 1) }
        }
    }

    @Test
    fun fractionalDurationsAndOverflowMatchOriginalWithoutFloatingPointConversion() {
        val random = Random(218402L)
        val seconds =
            listOf(
                Long.MIN_VALUE,
                Long.MIN_VALUE + 1,
                -3601,
                -3600,
                -60,
                -1,
                0,
                1,
                59,
                60,
                3599,
                3600,
                Long.MAX_VALUE - 1,
                Long.MAX_VALUE,
            ) + List(2000) { random.nextLong() }
        val adjustments =
            listOf(
                Long.MIN_VALUE,
                -1000000001,
                -1000000000,
                -999999999,
                -1,
                0,
                1,
                999999999,
                1000000000,
                1000000001,
                Long.MAX_VALUE,
            )
        for (second in seconds) for (adjustment in adjustments) {
            val expected = runCatching { Duration.ofSeconds(second, adjustment) }
            val actual = runCatching { DurationParts.ofSeconds(second, adjustment) }
            assertEquals(expected.isSuccess, actual.isSuccess, "$second/$adjustment")
            if (expected.isSuccess) {
                val old = expected.getOrThrow()
                val now = actual.getOrThrow()
                assertEquals(old.seconds, now.seconds)
                assertEquals(old.nano, now.nanos)
                assertEquals(old.toString(), now.text())
            } else assertIs<ArithmeticException>(actual.exceptionOrNull())
        }
        for (value in seconds) for (scale in listOf(60L, 3600L)) {
            val expected = runCatching { Math.multiplyExact(value, scale) }
            val actual = runCatching { checkedScale(value, scale) }
            assertEquals(expected.isSuccess, actual.isSuccess)
            if (expected.isSuccess) assertEquals(expected.getOrThrow(), actual.getOrThrow())
            else assertIs<ArithmeticException>(actual.exceptionOrNull())
        }
    }
}
