package org.racedaydreams.domain.activity;

import java.time.LocalDate;

/** Starting mileage is optional/manual; first recorded run is derived from assignments. */
public record Shoe(String id, String brand, String model, LocalDate purchaseDate,
                   Double startingDistanceMetres, boolean retired, String notes, boolean defaultShoe) {
    public Shoe {
        id = ActivityChecks.required(id, "Shoe ID");
        brand = brand == null ? "" : brand.trim();
        model = ActivityChecks.required(model, "Shoe model").trim();
        if (brand.length() > 200 || model.length() > 200) throw new IllegalArgumentException("Shoe brand/model must be at most 200 characters.");
        ActivityChecks.nonNegative(startingDistanceMetres, "Starting shoe mileage");
        if (retired && defaultShoe) throw new IllegalArgumentException("A retired shoe cannot be the default shoe.");
    }
}
