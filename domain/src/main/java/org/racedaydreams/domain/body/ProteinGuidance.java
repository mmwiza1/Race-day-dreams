package org.racedaydreams.domain.body;

import org.racedaydreams.domain.settings.PrimaryFocus;

import java.time.*;
import java.util.*;

/** General adult sports-nutrition range, never a diet prescription or calorie/food tracker. */
public final class ProteinGuidance {
    private ProteinGuidance() {}

    public record Basis(String id, Instant at, double kilograms) {
        public Basis {
            if (id == null
                    || id.trim().isEmpty()
                    || id.length() > 256
                    || at == null
                    || !Double.isFinite(kilograms)
                    || kilograms <= 0
                    || kilograms > 1000)
                throw new IllegalArgumentException("Select a recorded weight.");
        }

        public static Basis from(BodyWeight w) {
            return new Basis(w.id(), w.at(), w.kilograms());
        }

        public boolean matches(BodyWeight w) {
            return id.equals(w.id())
                    && at.equals(w.at())
                    && Double.compare(kilograms, w.kilograms()) == 0;
        }
    }

    public record Advice(
            Integer minimumGrams,
            Integer maximumGrams,
            Double minimumPerKg,
            Double maximumPerKg,
            BodyWeight basis,
            String instruction,
            String explanation,
            String limitation) {
        public boolean available() {
            return minimumGrams != null && maximumGrams != null && basis != null;
        }
    }

    public static Advice evaluate(
            boolean enabled,
            Basis selection,
            List<BodyWeight> weights,
            PrimaryFocus focus,
            Instant now) {
        Objects.requireNonNull(weights);
        Objects.requireNonNull(now);
        if (!enabled)
            return unavailable(
                    "Protein guidance is off. It is optional and does not affect your training.");
        if (selection == null)
            return unavailable(
                    "Choose a recorded body weight if you would like a general protein range.");
        BodyWeight match = null;
        for (var row : weights)
            if (row.id().equals(selection.id())) {
                if (match != null && !match.equals(row))
                    throw new IllegalArgumentException("Conflicting weight observations.");
                match = row;
            }
        if (match == null || !selection.matches(match))
            return unavailable(
                    "Your selected weight is no longer available or has changed. Review and choose"
                            + " a measurement again; no replacement was assumed.");
        if (match.at().isAfter(now))
            return unavailable(
                    "Your selected measurement is dated in the future. Review its date before using"
                            + " it.");
        if (match.at().isBefore(now.minus(Duration.ofDays(90))))
            return unavailable(
                    "Your selected weight is over 90 days old. Review a recent measurement before"
                            + " using a daily range.");
        if (match.kilograms() < 30 || match.kilograms() > 250)
            return unavailable(
                    "This general adult training range may not be useful for your selected"
                            + " measurement. Individual nutrition advice may be more appropriate.");
        double low = focus == null || focus == PrimaryFocus.RUNNING ? 1.2 : 1.4,
                high = focus == PrimaryFocus.RUNNING ? 1.6 : 2.0;
        int minimum = (int) (Math.round(match.kilograms() * low / 5.0) * 5);
        int maximum = (int) (Math.round(match.kilograms() * high / 5.0) * 5);
        return new Advice(
                minimum,
                maximum,
                low,
                high,
                match,
                "A general daily range is about " + minimum + "–" + maximum + " grams of protein.",
                "Based on your chosen body weight and "
                        + (focus == null
                                ? "general training"
                                : focus == PrimaryFocus.RUNNING
                                        ? "running"
                                        : focus == PrimaryFocus.STRENGTH
                                                ? "strength"
                                                : "running and strength")
                        + " focus. This is general guidance, not a requirement or a weight-loss"
                        + " programme.",
                limitation());
    }

    private static Advice unavailable(String why) {
        return new Advice(
                null,
                null,
                null,
                null,
                null,
                why,
                "No numerical range is inferred from height, missing or stale weight, or an"
                        + " unreviewed provider measurement.",
                limitation());
    }

    public static String limitation() {
        return "General guidance for adults training recreationally. Medical needs, pregnancy, age"
                + " and dietary circumstances can change protein requirements; seek individual"
                + " advice when appropriate. No supplement is required. The range is"
                + " approximate, not a medical prescription.";
    }

    public static String sources() {
        return "ACSM/Academy of Nutrition and Dietetics/Dietitians of Canada (2016): athletes"
                + " commonly need 1.2–2.0 g/kg/day. ISSN position stand (2017): 1.4–2.0 g/kg/day"
                + " for most exercising adults. Focus-specific subranges, nearest-5-gram"
                + " rounding and a 90-day freshness boundary are conservative original product"
                + " choices, not individually measured requirements.";
    }
}
