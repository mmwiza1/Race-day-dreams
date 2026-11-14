package org.racedaydreams.domain.time

/** ISO weekday used by existing availability; JVM remains java.time.DayOfWeek. */
expect enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    fun getValue(): Int
}
