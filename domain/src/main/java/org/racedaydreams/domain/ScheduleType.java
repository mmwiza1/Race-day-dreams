package org.racedaydreams.domain;

public enum ScheduleType {
    FIXED("Fixed"), FLEXIBLE("Flexible");
    private final String label;
    ScheduleType(String label) { this.label = label; }
    public String label() { return label; }
}
