package org.racedaydreams.domain;

public enum GoalMode {
    FINISH("Finish"), TARGET_TIME("Target Time"), MAXIMUM_PERFORMANCE("Maximum Performance");

    private final String label;
    GoalMode(String label) { this.label = label; }
    public String label() { return label; }
}
