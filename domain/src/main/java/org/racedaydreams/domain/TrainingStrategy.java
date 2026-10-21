package org.racedaydreams.domain;

public enum TrainingStrategy {
    DISTANCE_FIRST("Distance First", "Build an endurance foundation before gradually adding race-specific pace work."),
    BALANCED("Balanced", "Develop endurance and speed together with easy running and recovery."),
    PERFORMANCE("Performance", "More threshold, interval and race-pace work when your training history supports it."),
    ZONE_2("Zone 2 / Aerobic", "Lower-intensity training option designed to build aerobic endurance while limiting unnecessary high-intensity workload.");

    private final String label;
    private final String description;
    TrainingStrategy(String label, String description) {
        this.label = label;
        this.description = description;
    }
    public String label() { return label; }
    public String description() { return description; }
}
