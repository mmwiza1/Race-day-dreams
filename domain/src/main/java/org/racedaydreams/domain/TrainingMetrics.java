package org.racedaydreams.domain;

/** Nullable values mean unavailable observations rather than a measured zero. */
public record TrainingMetrics(Double currentWeekKm, Double previousWeekKm,
        Double rollingFourWeekKm, Double longestRecentRunKm, Integer runningDays,
        Integer gymDays, Integer restDays, Integer hardSessions) {
    public TrainingMetrics {
        DomainChecks.nonNegative(currentWeekKm, "current weekly distance");
        DomainChecks.nonNegative(previousWeekKm, "previous weekly distance");
        DomainChecks.nonNegative(rollingFourWeekKm, "four week distance");
        DomainChecks.nonNegative(longestRecentRunKm, "longest run");
        checkDays(runningDays); checkDays(gymDays); checkDays(restDays);
        if (hardSessions != null && hardSessions < 0) {
            throw new IllegalArgumentException("Hard session count cannot be negative");
        }
    }

    private static void checkDays(Integer days) {
        if (days != null && (days < 0 || days > 7)) throw new IllegalArgumentException("Days must be 0 to 7");
    }
}
