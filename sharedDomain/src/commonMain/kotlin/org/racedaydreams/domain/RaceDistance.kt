package org.racedaydreams.domain

enum class RaceDistance(private val km: Double, private val displayLabel: String) {
    FIVE_K(5.0, "5K"),
    TEN_K(10.0, "10K"),
    HALF_MARATHON(21.0975, "Half Marathon"),
    MARATHON(42.195, "Marathon"),
    GENERAL_FITNESS(0.0, "General Fitness / Zone 2");

    fun label(): String = displayLabel

    fun kilometres(): Double = km
}
