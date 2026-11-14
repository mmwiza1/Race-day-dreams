package org.racedaydreams.domain

enum class TrainingStrategy(private val displayLabel: String, private val detail: String) {
    DISTANCE_FIRST(
        "Distance First",
        "Build an endurance foundation before gradually adding race-specific pace work.",
    ),
    BALANCED("Balanced", "Develop endurance and speed together with easy running and recovery."),
    PERFORMANCE(
        "Performance",
        "More threshold, interval and race-pace work when your training history supports it.",
    ),
    ZONE_2(
        "Zone 2 / Aerobic",
        "Lower-intensity training option designed to build aerobic endurance while limiting unnecessary high-intensity workload.",
    );

    fun label(): String = displayLabel

    fun description(): String = detail
}
