package org.racedaydreams.domain;

/** Stored workout distances always remain kilometres; units affect display only. */
public enum UnitSystem {
    METRIC("Metric"), IMPERIAL("Imperial");
    private final String label;
    UnitSystem(String label) { this.label = label; }
    public String label() { return label; }
}
