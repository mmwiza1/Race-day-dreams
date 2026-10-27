package org.racedaydreams.domain.presentation;

import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.StrengthAnalytics;
import org.racedaydreams.domain.strength.StrengthProgressionAdvisor;
import org.racedaydreams.domain.strength.StrengthSession;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Strength-first Home evidence. Prescriptions and actual workouts remain separate. */
public record StrengthTodaySummary(
        GymProgramme.Session today,
        boolean todayCompleted,
        LocalDate weekStart,
        int plannedThisWeek,
        int completedThisWeek,
        int actualWorkoutsThisWeek,
        int recentPbExercises,
        Recommendation recommendation) {
    /** An explicit actual/prescription association, including its preserved version identity. */
    public record Association(
            String actualSessionId, String versionKey, String prescribedSessionId) {
        public Association {
            Objects.requireNonNull(actualSessionId);
            Objects.requireNonNull(versionKey);
            Objects.requireNonNull(prescribedSessionId);
        }
    }

    public record Recommendation(
            String exerciseId, StrengthProgressionAdvisor.Action action, String reason) {}

    public static StrengthTodaySummary from(
            GymProgramme programme,
            List<StrengthSession> history,
            List<Association> associations,
            LocalDate today,
            Instant asOf) {
        Objects.requireNonNull(today);
        Objects.requireNonNull(asOf);
        List<StrengthSession> actual =
                history.stream()
                        .filter(session -> !session.startedAt().isAfter(asOf))
                        .collect(Collectors.toList());
        Set<String> actualIds = new HashSet<>();
        actual.forEach(session -> actualIds.add(session.id()));
        Set<String> completed = new HashSet<>();
        if (programme != null) {
            for (Association association : associations) {
                if (actualIds.contains(association.actualSessionId())
                        && belongsToProgramme(association.versionKey(), programme)) {
                    completed.add(association.prescribedSessionId());
                }
            }
        }
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<GymProgramme.Session> week =
                programme == null
                        ? Collections.emptyList()
                        : programme.sessions().stream()
                                .filter(session -> inWeek(session.date(), weekStart))
                                .collect(Collectors.toList());
        GymProgramme.Session prescribedToday =
                programme == null
                        ? null
                        : programme.sessions().stream()
                                .filter(session -> session.date().equals(today))
                                .findFirst()
                                .orElse(null);
        int actualThisWeek =
                (int)
                        actual.stream()
                                .filter(
                                        session ->
                                                inWeek(
                                                        session.startedAt()
                                                                .atOffset(session.offset())
                                                                .toLocalDate(),
                                                        weekStart))
                                .count();
        int recentPbExercises =
                (int)
                        StrengthAnalytics.progression(actual).stream()
                                .filter(
                                        record ->
                                                !record.category().equals("Estimated 1RM")
                                                        && Duration.between(record.at(), asOf)
                                                                        .compareTo(
                                                                                Duration.ofDays(28))
                                                                <= 0)
                                .map(StrengthAnalytics.Record::exerciseId)
                                .distinct()
                                .count();
        return new StrengthTodaySummary(
                prescribedToday,
                prescribedToday != null && completed.contains(prescribedToday.id()),
                weekStart,
                week.size(),
                (int) week.stream().filter(session -> completed.contains(session.id())).count(),
                actualThisWeek,
                recentPbExercises,
                recommendation(programme, actual, today, asOf));
    }

    private static boolean inWeek(LocalDate date, LocalDate weekStart) {
        return !date.isBefore(weekStart) && date.isBefore(weekStart.plusWeeks(1));
    }

    private static boolean belongsToProgramme(String versionKey, GymProgramme programme) {
        String prefix = programme.id() + ":v";
        if (!versionKey.startsWith(prefix)) return false;
        try {
            int version = Integer.parseInt(versionKey.substring(prefix.length()));
            return version >= 1 && version <= programme.version();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static Recommendation recommendation(
            GymProgramme programme, List<StrengthSession> actual, LocalDate today, Instant asOf) {
        if (programme == null) return null;
        // Both observations must be recent. Unknown effort is retained and interpreted by the
        // advisor.
        List<StrengthSession> recent =
                actual.stream()
                        .filter(
                                session ->
                                        Duration.between(session.startedAt(), asOf)
                                                        .compareTo(Duration.ofDays(28))
                                                <= 0)
                        .collect(Collectors.toList());
        var considered = new HashSet<String>();
        var recommendations = new ArrayList<Recommendation>();
        programme.sessions().stream()
                .filter(session -> !session.date().isBefore(today))
                .sorted(Comparator.comparing(GymProgramme.Session::date))
                .forEach(
                        session -> {
                            for (GymProgramme.Prescription prescription : session.exercises()) {
                                if (!considered.add(prescription.exerciseId())) continue;
                                var advice =
                                        StrengthProgressionAdvisor.review(prescription, recent);
                                if (advice.action()
                                                == StrengthProgressionAdvisor.Action
                                                        .CONSIDER_INCREASE
                                        || advice.action()
                                                == StrengthProgressionAdvisor.Action
                                                        .REDUCE_OR_SIMPLIFY) {
                                    recommendations.add(
                                            new Recommendation(
                                                    prescription.exerciseId(),
                                                    advice.action(),
                                                    advice.reason()));
                                }
                            }
                        });
        return recommendations.stream()
                .filter(
                        advice ->
                                advice.action()
                                        == StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY)
                .findFirst()
                .orElse(recommendations.isEmpty() ? null : recommendations.get(0));
    }
}
