package org.racedaydreams.domain.time

/** ISO calendar identity. The JVM actual is the original java.time.LocalDate, not a new DTO. */
expect class LocalDate {
    fun getYear(): Int

    fun getMonthValue(): Int

    fun getDayOfMonth(): Int

    fun toEpochDay(): Long

    fun plusDays(daysToAdd: Long): LocalDate

    fun plusWeeks(weeksToAdd: Long): LocalDate

    fun minusDays(daysToSubtract: Long): LocalDate

    fun getDayOfWeek(): DayOfWeek

    override fun toString(): String
}

expect object CalendarDates {
    fun of(year: Int, month: Int, day: Int): LocalDate

    fun ofEpochDay(epochDay: Long): LocalDate
}

/**
 * Full original ISO date range. kotlinx-datetime 0.6.2 Native dates stop at year 999999; retaining
 * the existing Java range avoids narrowing previously supported stored identities.
 */
internal object IsoCalendar {
    private val monthStarts = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    fun leap(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    fun validate(year: Int, month: Int, day: Int) {
        require(year in -999999999..999999999 && month in 1..12) { "Invalid ISO calendar date." }
        val length =
            when (month) {
                2 -> if (leap(year)) 29 else 28
                4,
                6,
                9,
                11 -> 30
                else -> 31
            }
        require(day in 1..length) { "Invalid ISO calendar date." }
    }

    fun epochDay(year: Int, month: Int, day: Int): Long {
        validate(year, month, day)
        return yearStart(year.toLong()) + monthStarts[month - 1] + day - 1 +
            (if (month > 2 && leap(year)) 1 else 0)
    }

    private fun yearStart(year: Long): Long =
        365 * year + (year + 3).floorDiv(4) - (year + 99).floorDiv(100) +
            (year + 399).floorDiv(400) - 719528

    /** UTC instant formatting includes the extra endpoint year permitted by java.time.Instant. */
    fun parts(epochDay: Long): IsoDateParts {
        require(epochDay >= yearStart(-1000000000L) && epochDay < yearStart(1000000001L)) {
            "ISO day is outside the supported instant range."
        }
        var lower = -1000000000L
        var upper = 1000000001L
        while (lower + 1 < upper) {
            val middle = (lower + upper).floorDiv(2)
            if (yearStart(middle) <= epochDay) lower = middle else upper = middle
        }
        val year = lower.toInt()
        val dayOfYear = (epochDay - yearStart(lower)).toInt()
        var month = 1
        while (
            month < 12 && dayOfYear >= monthStarts[month] + if (month >= 2 && leap(year)) 1 else 0
        ) month++
        val first = monthStarts[month - 1] + if (month > 2 && leap(year)) 1 else 0
        return IsoDateParts(year, month, dayOfYear - first + 1)
    }

    fun text(year: Int, month: Int, day: Int): String {
        val yearText =
            when {
                year < 0 -> "-" + (-year).toString().padStart(4, '0')
                year > 9999 -> "+$year"
                else -> year.toString().padStart(4, '0')
            }
        return "$yearText-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }
}

internal data class IsoDateParts(val year: Int, val month: Int, val day: Int)
