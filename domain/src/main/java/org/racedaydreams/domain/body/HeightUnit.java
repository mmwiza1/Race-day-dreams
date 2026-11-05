package org.racedaydreams.domain.body;

public enum HeightUnit {
    CENTIMETRES(0.01, "cm"),
    INCHES(0.0254, "in");
    private final double metres;
    private final String label;

    HeightUnit(double metres, String label) {
        this.metres = metres;
        this.label = label;
    }

    public String label() {
        return label;
    }

    public double toMetres(double value) {
        if (!Double.isFinite(value) || value <= 0)
            throw new IllegalArgumentException("Enter a positive finite height.");
        return value * metres;
    }

    public double fromMetres(double value) {
        if (!Double.isFinite(value) || value <= 0)
            throw new IllegalArgumentException("Height is unavailable.");
        return value / metres;
    }
}
