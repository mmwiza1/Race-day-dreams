package org.racedaydreams.domain

import java.time.LocalDate
import java.util.Random
import kotlin.test.*
import org.racedaydreams.domain.time.IsoCalendar
import org.racedaydreams.domain.time.checkedAdd
import org.racedaydreams.domain.time.checkedScale

class AuthoredTextAndCalendarCompatibilityTest {
    @Test
    fun retainedTextBlankBoundaryMatchesEveryOriginalUtf16Unit() {
        for (code in 0..65535) {
            val value = code.toChar().toString()
            assertEquals(Character.isWhitespace(code), isJavaBlank(value), "U+${code.toString(16)}")
        }
        assertTrue(isJavaBlank(""))
    }

    @Test
    fun nativeCalendarArithmeticAndWeekdaysMatchTheOriginalJdkAcrossRetainedRange() {
        val random = Random(218405L)
        val dates =
            listOf(
                LocalDate.MIN,
                LocalDate.MAX,
                LocalDate.of(0, 2, 29),
                LocalDate.of(1900, 2, 28),
                LocalDate.of(2000, 2, 29),
                LocalDate.of(1970, 1, 1),
            ) +
                List(2000) {
                    LocalDate.of(
                        random.nextInt(1999999999) - 999999999,
                        1 + random.nextInt(12),
                        1 + random.nextInt(28),
                    )
                }
        val adjustments = listOf(Long.MIN_VALUE, -366L, -7, -1, 0, 1, 7, 366, Long.MAX_VALUE)
        for (date in dates) {
            assertEquals(date.dayOfWeek.value - 1, (date.toEpochDay() + 3).mod(7).toInt())
            for (days in adjustments) {
                val expected = runCatching { date.plusDays(days) }
                val actual = runCatching {
                    val p = IsoCalendar.parts(checkedAdd(date.toEpochDay(), days))
                    IsoCalendar.validate(p.year, p.month, p.day)
                    IsoCalendar.text(p.year, p.month, p.day)
                }
                assertEquals(expected.isSuccess, actual.isSuccess, "$date/$days")
                if (expected.isSuccess)
                    assertEquals(expected.getOrThrow().toString(), actual.getOrThrow())
                val expectedWeeks = runCatching { date.plusWeeks(days) }
                val actualWeeks = runCatching {
                    val p = IsoCalendar.parts(checkedAdd(date.toEpochDay(), checkedScale(days, 7)))
                    IsoCalendar.validate(p.year, p.month, p.day)
                    IsoCalendar.text(p.year, p.month, p.day)
                }
                assertEquals(expectedWeeks.isSuccess, actualWeeks.isSuccess)
                if (expectedWeeks.isSuccess)
                    assertEquals(expectedWeeks.getOrThrow().toString(), actualWeeks.getOrThrow())
            }
        }
    }
}
