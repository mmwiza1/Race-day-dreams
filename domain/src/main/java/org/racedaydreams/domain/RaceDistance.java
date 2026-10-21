package org.racedaydreams.domain;

/** Official road-race distances; general aerobic fitness has no finish distance. */
public enum RaceDistance {
    FIVE_K(5.0, "5K"),
    TEN_K(10.0, "10K"),
    HALF_MARATHON(21.0975, "Half Marathon"),
    MARATHON(42.195, "Marathon"),
    GENERAL_FITNESS(0.0, "General Fitness / Zone 2");

    private final double kilometres;
    private final String label;

    RaceDistance(double kilometres, String label) {
        this.kilometres = kilometres;
        this.label = label;
    }

    public double kilometres() { return kilometres; }
    public String label() { return label; }
}
