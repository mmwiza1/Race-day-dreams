package org.racedaydreams.domain.reminders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.racedaydreams.domain.WorkoutCategory;

/** Deterministic local scheduling rules. No diagnosis, rigid fluid prescription or plan generation. */
public final class EveningReminderPolicy {
    private EveningReminderPolicy() { }

    public static List<EveningReminder> schedule(List<ReminderPlanEntry> entries,
            ReminderPreferences preferences, Clock clock, boolean notificationPermission) {
        Objects.requireNonNull(entries, "planned workouts");
        Objects.requireNonNull(clock, "clock");
        if (!notificationPermission || preferences.mode() == ReminderPreferences.Mode.DISABLED) {
            return Collections.emptyList();
        }
        List<EveningReminder> reminders = new ArrayList<>();
        for (ReminderPlanEntry entry : entries) {
            Optional<EveningReminder> reminder = candidate(entry, preferences);
            if (reminder.isPresent() && reminder.get().triggerAt().isAfter(clock.instant())) {
                reminders.add(reminder.get());
            }
        }
        return Collections.unmodifiableList(reminders);
    }

    /**
     * A delayed alarm is delivered only on the same evening, outside quiet hours and within two
     * hours of its proposal. Revisions, replacements, completion and preference changes invalidate it.
     */
    public static boolean canDeliver(EveningReminder scheduled, ReminderPlanEntry current,
            ReminderPreferences preferences, Clock clock, boolean notificationPermission) {
        if (!notificationPermission || current == null) return false;
        Optional<EveningReminder> expected = candidate(current, preferences);
        if (!expected.isPresent() || !expected.get().equals(scheduled)) return false;
        Instant now = clock.instant();
        if (now.isBefore(scheduled.triggerAt())
                || !now.isBefore(scheduled.triggerAt().plus(Duration.ofHours(2)))) return false;
        ZonedDateTime local = now.atZone(preferences.zone());
        return local.toLocalDate().equals(scheduled.workoutDate().minusDays(1))
                && !quiet(preferences, local.toLocalTime());
    }

    public static Optional<EveningReminder> candidate(ReminderPlanEntry entry, ReminderPreferences preferences) {
        Objects.requireNonNull(entry, "planned workout");
        Objects.requireNonNull(preferences, "preferences");
        if (entry.status() != ReminderPlanEntry.Status.PLANNED
                || preferences.mode() == ReminderPreferences.Mode.DISABLED
                || (preferences.mode() == ReminderPreferences.Mode.LONG_HARD_ONLY
                    && !longOrHard(entry.workout().category()))) return Optional.empty();
        // java.time shifts a missing spring-forward time to the next valid instant, and chooses
        // one occurrence of an overlapping autumn time. The same calculation is used at delivery.
        ZonedDateTime local = entry.workout().date().minusDays(1)
                .atTime(preferences.eveningTime()).atZone(preferences.zone());
        if (quiet(preferences, local.toLocalTime())) return Optional.empty();
        WorkoutCategory category = entry.workout().category();
        String label = switch (category) {
            case RECOVERY_RUN -> "Recovery run";
            case EASY_RUN -> "Easy run";
            case ZONE_2_RUN -> "Zone 2 run";
            case STEADY_RUN -> "Steady run";
            case LONG_RUN -> "Long run";
            case PROGRESSION_RUN -> "Progression run";
            case TEMPO_THRESHOLD_RUN -> "Tempo run";
            case INTERVALS -> "Intervals";
            case RACE_PACE_RUN -> "Race-pace run";
            case TIME_TRIAL -> "Time trial";
            case RACE -> "Race";
            case CUSTOM_RUN -> "Custom run";
        };
        String prescription = entry.workout().distanceKm() == null ? "Your workout"
                : String.format(Locale.ROOT, "%.1f km", entry.workout().distanceKm());
        String message = prescription + " is planned tomorrow. Hydrate this evening, eat normally, "
                + "prepare your kit and aim for adequate sleep.";
        return Optional.of(new EveningReminder(entry.workout().id(), entry.revision(),
                entry.workout().date(), local.toInstant(), label + " tomorrow", message));
    }

    private static boolean quiet(ReminderPreferences preferences, LocalTime time) {
        return preferences.quietHours() != null && preferences.quietHours().contains(time);
    }

    private static boolean longOrHard(WorkoutCategory category) {
        return category == WorkoutCategory.LONG_RUN || category == WorkoutCategory.INTERVALS
                || category == WorkoutCategory.TEMPO_THRESHOLD_RUN
                || category == WorkoutCategory.RACE_PACE_RUN || category == WorkoutCategory.TIME_TRIAL
                || category == WorkoutCategory.RACE || category == WorkoutCategory.PROGRESSION_RUN;
    }
}
