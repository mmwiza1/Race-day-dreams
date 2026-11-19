package org.racedaydreams.domain.time

actual class LocalDate
internal constructor(private val year: Int, private val month: Int, private val day: Int) {
    init {
        IsoCalendar.validate(year, month, day)
    }

    actual fun getYear(): Int = year

    actual fun getMonthValue(): Int = month

    actual fun getDayOfMonth(): Int = day

    actual fun toEpochDay(): Long = IsoCalendar.epochDay(year, month, day)

    actual fun plusDays(daysToAdd: Long): LocalDate =
        CalendarDates.ofEpochDay(checkedAdd(toEpochDay(), daysToAdd))

    actual fun plusWeeks(weeksToAdd: Long): LocalDate = plusDays(checkedScale(weeksToAdd, 7))

    actual fun minusDays(daysToSubtract: Long): LocalDate =
        if (daysToSubtract == Long.MIN_VALUE) plusDays(Long.MAX_VALUE).plusDays(1)
        else plusDays(-daysToSubtract)

    actual fun getDayOfWeek(): DayOfWeek = DayOfWeek.values()[(toEpochDay() + 3).mod(7).toInt()]

    override fun equals(other: Any?): Boolean =
        other is LocalDate && year == other.year && month == other.month && day == other.day

    override fun hashCode(): Int = (year * 31 + month) * 31 + day

    actual override fun toString(): String = IsoCalendar.text(year, month, day)
}

actual object CalendarDates {
    actual fun atOffset(instant: Instant, offset: ZoneOffset): LocalDate =
        ofEpochDay(
            checkedAdd(instant.getEpochSecond(), offset.getTotalSeconds().toLong()).floorDiv(86400)
        )

    actual fun ofEpochDay(epochDay: Long): LocalDate {
        val parts = IsoCalendar.parts(epochDay)
        return of(parts.year, parts.month, parts.day)
    }

    actual fun of(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
}
