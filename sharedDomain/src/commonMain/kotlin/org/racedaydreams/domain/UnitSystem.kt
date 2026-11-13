package org.racedaydreams.domain

enum class UnitSystem(private val displayLabel: String) {
    METRIC("Metric"),
    IMPERIAL("Imperial");

    fun label(): String = displayLabel
}
