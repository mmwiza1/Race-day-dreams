package org.racedaydreams.domain.time

actual enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    actual fun getValue(): Int = ordinal + 1
}
