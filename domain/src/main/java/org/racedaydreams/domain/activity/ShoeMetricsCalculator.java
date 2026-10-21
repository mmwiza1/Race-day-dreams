package org.racedaydreams.domain.activity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/** Recomputed from current assignments, so corrections, reassignment and deletions remain accurate. */
public final class ShoeMetricsCalculator {
    private ShoeMetricsCalculator() {}
    public static ShoeMetrics calculate(Shoe shoe, List<ActivityRecord> activities, Clock clock) {
        Objects.requireNonNull(shoe, "Shoe");
        Objects.requireNonNull(activities, "Activities");
        Objects.requireNonNull(clock, "Clock");
        double recorded = 0;
        int count = 0;
        int known = 0;
        Double longest = null;
        Instant first = null;
        for (ActivityRecord activity : activities) {
            if (!shoe.id().equals(activity.shoeId())) continue;
            count++;
            if (first == null || activity.start().isBefore(first)) first = activity.start();
            if (activity.distanceMetres() != null) {
                recorded += activity.distanceMetres();
                longest = longest == null ? activity.distanceMetres() : Math.max(longest, activity.distanceMetres());
                known++;
            }
        }
        Double total = shoe.startingDistanceMetres() == null ? null : recorded + shoe.startingDistanceMetres();
        Long age = shoe.purchaseDate() == null || shoe.purchaseDate().isAfter(LocalDate.now(clock)) ? null
                : ChronoUnit.DAYS.between(shoe.purchaseDate(), LocalDate.now(clock));
        double knownMinimum = total == null ? recorded : total;
        String guidance;
        if (knownMinimum >= 800_000) guidance = "Recorded use is substantial. Check cushioning, outsole wear and how the shoes feel regularly; consider replacement if their condition no longer suits you. Longevity varies by shoe, runner and surface.";
        else if (knownMinimum >= 600_000) guidance = "Pay closer attention to cushioning and outsole condition as mileage accumulates. There is no universal replacement distance; shoe longevity varies by model, runner and surface.";
        else if (knownMinimum >= 400_000) guidance = "Consider checking how the cushioning and outsole feel. Mileage is a prompt to inspect condition, not a hard replacement rule.";
        else guidance = "Check comfort, cushioning and outsole condition over time. Mileage alone does not determine when a shoe needs replacing.";
        if (known < count) guidance += " Some assigned runs have no recorded distance, so known mileage is incomplete.";
        if (total == null) guidance += " Earlier mileage was not supplied; only recorded use is known.";
        return new ShoeMetrics(shoe.id(), recorded, total, count, known, age, longest,
                known == 0 ? null : recorded / known, first, guidance);
    }
}
