package org.racedaydreams.domain.body;
/** Canonical storage is kilograms; display preference never rewrites evidence. */
public enum MassUnit {
    KG(1), LB(0.45359237);
    private final double kilograms;
    MassUnit(double kilograms) { this.kilograms = kilograms; }
    public double toKilograms(double value) { if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Enter a non-negative finite mass."); return value * kilograms; }
    public double fromKilograms(double value) { return value / kilograms; }
}
