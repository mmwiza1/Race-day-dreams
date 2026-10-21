package org.racedaydreams.domain.plan;

/**
 * Explicit acceptance budgets for a regression fixture, not universal training prescriptions.
 * A future training engine should choose and justify appropriate budgets for each context.
 */
public record PlanRegressionPolicy(double maximumWeeklyIncreaseKm,
        double maximumWeeklyMultiplier, double maximumLongRunIncreaseKm,
        double maximumSingleRunFraction, int maximumHardSessionsPerWeek,
        int minimumDaysBetweenHardSessions, int daysAfterHardLowerBody,
        double maximumTaperWeekFraction) {
    public PlanRegressionPolicy {
        positive(maximumWeeklyIncreaseKm, "weekly increase");
        positive(maximumWeeklyMultiplier, "weekly multiplier");
        positive(maximumLongRunIncreaseKm, "long-run increase");
        positive(maximumSingleRunFraction, "single-run fraction");
        positive(maximumTaperWeekFraction, "taper fraction");
        if (maximumWeeklyMultiplier < 1 || maximumSingleRunFraction > 1
                || maximumTaperWeekFraction > 1 || maximumHardSessionsPerWeek < 0
                || minimumDaysBetweenHardSessions < 1 || daysAfterHardLowerBody < 0) {
            throw new IllegalArgumentException("Invalid regression acceptance budget");
        }
    }

    private static void positive(double value, String field) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Invalid " + field);
    }
}
