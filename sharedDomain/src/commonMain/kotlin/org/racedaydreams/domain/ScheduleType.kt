package org.racedaydreams.domain

enum class ScheduleType(private val displayLabel: String) {
    FIXED("Fixed"),
    FLEXIBLE("Flexible");

    fun label(): String = displayLabel
}
