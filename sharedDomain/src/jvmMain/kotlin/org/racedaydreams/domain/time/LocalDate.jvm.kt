package org.racedaydreams.domain.time

actual typealias LocalDate = java.time.LocalDate

actual object CalendarDates {
    actual fun ofEpochDay(epochDay: Long): LocalDate = java.time.LocalDate.ofEpochDay(epochDay)

    actual fun of(year: Int, month: Int, day: Int): LocalDate =
        java.time.LocalDate.of(year, month, day)
}
