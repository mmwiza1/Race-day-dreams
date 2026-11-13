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

    override fun equals(other: Any?): Boolean =
        other is LocalDate && year == other.year && month == other.month && day == other.day

    override fun hashCode(): Int = (year * 31 + month) * 31 + day

    actual override fun toString(): String = IsoCalendar.text(year, month, day)
}

actual object CalendarDates {
    actual fun of(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
}
