package org.racedaydreams.domain.coaching;

import java.time.*;
import java.util.*;
import org.racedaydreams.domain.TrainingPreferences;

/** Exhaustively scores at most 128 day subsets. Hard constraints are filtered before soft scoring. */
public final class ConstraintScheduler {
    private ConstraintScheduler() { }
    public record Schedule(List<LocalDate> days, LocalDate longRun, LocalDate quality, int cost, String explanation) { }
    public static Schedule select(LocalDate from, LocalDate to, int maximum, TrainingPreferences preferences,
            Set<LocalDate> unavailable, Set<LocalDate> hardLowerBody, boolean wantQuality, LocalDate raceDate) {
        List<LocalDate> candidates = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1))
            if (preferences.runningDays().contains(date.getDayOfWeek()) && !unavailable.contains(date)
                    && (raceDate == null || date.isBefore(raceDate.minusDays(1)))) candidates.add(date);
        Schedule winner = new Schedule(List.of(), null, null, 0, "No available training days in this part of the week.");
        int count = Math.min(maximum, candidates.size()), bestCost = Integer.MAX_VALUE;
        for (int mask = 1; mask < (1 << candidates.size()); mask++) {
            if (Integer.bitCount(mask) != count) continue;
            List<LocalDate> days = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) if ((mask & (1 << i)) != 0) days.add(candidates.get(i));
            for (LocalDate longDay : days) {
                // Quality requires two days between it and the long run, including across week boundaries
                // (the generator reserves the first two days when the preceding week ends with a long run).
                List<LocalDate> qualities = new ArrayList<>(); qualities.add(null);
                if (wantQuality) for (LocalDate d : days)
                    if (Math.abs(java.time.temporal.ChronoUnit.DAYS.between(d, longDay)) >= 2) qualities.add(d);
                for (LocalDate quality : qualities) {
                    int cost = 0;
                    if (preferences.longRunDay() != null && longDay.getDayOfWeek() != preferences.longRunDay()) cost += 20;
                    if (wantQuality && quality == null) cost += 80;
                    for (int i = 1; i < days.size(); i++) if (days.get(i - 1).plusDays(1).equals(days.get(i))) cost += 2;
                    for (LocalDate d : days) {
                        boolean demanding = d.equals(longDay) || d.equals(quality);
                        if (demanding && hardLowerBody.contains(d.minusDays(1))) cost += 120;
                        if (demanding && hardLowerBody.contains(d)) cost += 90;
                        if (demanding && preferences.gymDays().contains(d.minusDays(1).getDayOfWeek())) cost += 4;
                    }
                    if (cost < bestCost) {
                        bestCost = cost;
                        winner = new Schedule(List.copyOf(days), longDay, quality, cost,
                            "Selected from feasible available days; score " + cost + " (lower is preferred). "
                            + "Long-run preference, recovery spacing and recorded lower-body work affect placement. "
                            + "Gym availability alone is a small preference, not evidence of a hard session.");
                    }
                }
            }
        }
        return winner;
    }
}
