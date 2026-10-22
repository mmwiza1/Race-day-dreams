package org.racedaydreams.domain.coaching;

import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.strength.*;

/** Raw evidence drives proposals. Run Score is deliberately not an input. */
public final class AdaptiveCoachingEngine {
    private AdaptiveCoachingEngine() { }
    public static Optional<AdjustmentProposal> review(PlanVersion version, List<ActivityRecord> actual,
            List<RunExecution> responses, List<StrengthSession> strength, LocalDate today, Instant now) {
        Map<String, PlannedWorkout> prescriptions = new HashMap<>();
        version.plan().workouts().forEach(w -> prescriptions.put(w.id(), w));
        Set<String> completed = new HashSet<>(); int highEffort = 0; boolean lowAndHard = false;
        List<String> evidence = new ArrayList<>(); List<String> evidenceIds = new ArrayList<>();
        for (ActivityRecord run : actual) {
            RunExecution response = responses.stream().filter(r -> r.activityId().equals(run.id())).findFirst().orElse(null);
            String linked = response == null ? run.plannedWorkoutId() : response.plannedWorkoutId();
            if (linked != null) completed.add(linked);
            if (run.end().isBefore(now.minus(Duration.ofDays(10))) || run.end().isAfter(now)) continue;
            PlannedWorkout planned = prescriptions.get(linked);
            if (planned == null || run.rpe() == null) continue;
            double expected = isQuality(planned.category()) ? planned.segments().stream().filter(s -> s.rpe() != null).mapToDouble(s -> s.rpe()).max().orElse(6) + 1 : 4;
            if (run.rpe() > expected) { highEffort++; evidenceIds.add(run.id() + ":" + run.rpe()); }
            if (planned.distanceKm() != null && planned.distanceKm() > 0 && run.distanceMetres() != null
                    && run.distanceMetres() < planned.distanceKm() * 750 && run.rpe() >= 8) {
                lowAndHard = true; evidenceIds.add(run.id() + ":" + run.distanceMetres());
            }
        }
        final boolean repeated = highEffort >= 2;
        final boolean lowHard = lowAndHard;
        var hardDates = StrengthAnalytics.hardLowerBodyDates(strength);
        var next = version.plan().workouts().stream().filter(w -> !w.date().isBefore(today) && w.date().isBefore(today.plusDays(7))
            && !completed.contains(w.id()) && w.category() != WorkoutCategory.RACE && isDemanding(w.category()) && (repeated || lowHard || hardDates.contains(w.date().minusDays(1)))).findFirst().orElse(null);
        if (next == null) return Optional.empty();
        boolean gym = hardDates.contains(next.date().minusDays(1));
        if (!gym && !repeated && !lowAndHard) return Optional.empty();
        if (repeated) evidence.add("At least two recent linked sessions had reported RPE above their intended range.");
        if (lowAndHard) evidence.add("A recent linked run combined less than 75% of planned distance with RPE 8 or higher.");
        if (gym) {
            evidence.add("Recorded hard lower-body work falls the day before this demanding session.");
            strength.stream().filter(s -> s.startedAt().atOffset(s.offset()).toLocalDate().equals(next.date().minusDays(1)))
                .forEach(s -> evidenceIds.add(s.id() + ":" + s));
        }
        List<AdjustmentProposal.Option> options = new ArrayList<>();
        var easy = new PlannedWorkout(next.id(), next.date(), WorkoutCategory.EASY_RUN, next.distanceKm(), next.duration(),
            org.racedaydreams.domain.coaching.Immutable.list(new WorkoutSegment(WorkoutSegment.Kind.RUN, next.distanceKm(), next.duration(), null, null, null, 3.0,
                "Conversational effort; shorten or take walk breaks if needed.")), "Runner-selected easier alternative. Original prescription remains in plan history.");
        options.add(new AdjustmentProposal.Option("easy", "Run easy instead (recommended)", AdjustmentProposal.Action.REPLACE, easy));
        if (gym) {
            for (LocalDate day = next.date().plusDays(1); day.isBefore(next.date().plusDays(4)); day = day.plusDays(1)) {
                final LocalDate candidate = day;
                if (!version.goalSnapshot().preferences().runningDays().contains(day.getDayOfWeek()) || day.isAfter(version.plan().endDate())) continue;
                if (version.plan().workouts().stream().anyMatch(w -> w.date().equals(candidate)
                    || isDemanding(w.category()) && Math.abs(java.time.temporal.ChronoUnit.DAYS.between(w.date(), candidate)) < 2)) continue;
                if (hardDates.contains(day.minusDays(1))) continue;
                // Moving across a calendar week must not exceed the runner's maximum frequency.
                var monday = day.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                long count = version.plan().workouts().stream().filter(w -> !w.id().equals(next.id()) && !w.date().isBefore(monday) && w.date().isBefore(monday.plusWeeks(1))).count();
                if (count >= version.goalSnapshot().preferences().runsPerWeek()) continue;
                options.add(0, new AdjustmentProposal.Option("move", "Move demanding session to " + day.format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu",java.util.Locale.UK)) + " (recommended)", AdjustmentProposal.Action.REPLACE,
                    new PlannedWorkout(next.id(), day, next.category(), next.distanceKm(), next.duration(), next.segments(),
                        "Runner accepted a move after recorded lower-body work. " + next.note())));
                break;
            }
        }
        options.add(new AdjustmentProposal.Option("keep", "Keep original workout", AdjustmentProposal.Action.KEEP, null));
        options.add(new AdjustmentProposal.Option("rest", "Rest instead", AdjustmentProposal.Action.REST, null));
        String id = UUID.nameUUIDFromBytes((version.id() + next.id() + evidenceIds).getBytes(StandardCharsets.UTF_8)).toString();
        return Optional.of(new AdjustmentProposal(id, version.id(), next.id(), now,
            String.join(" ", evidence) + " Consider reducing or moving one demanding session; no other workouts need changing from this evidence.",
            evidence, "Only this workout is affected. No mileage is added elsewhere. Your race goal is unchanged.", "Moderate — coaching heuristics, not a medical assessment", options));
    }
    public static boolean isQuality(WorkoutCategory category) {
        return switch (category) { case INTERVALS, TEMPO_THRESHOLD_RUN, RACE_PACE_RUN, TIME_TRIAL, PROGRESSION_RUN -> true; default -> false; };
    }
    public static boolean isDemanding(WorkoutCategory category) { return isQuality(category) || category == WorkoutCategory.LONG_RUN || category == WorkoutCategory.RACE; }
    public static TrainingPlan apply(TrainingPlan plan, AdjustmentProposal proposal, AdjustmentProposal.Option option, Set<String> completed) {
        if (!proposal.options().contains(option)) throw new IllegalArgumentException("Choose an option from this proposal.");
        if (completed.contains(proposal.workoutId())) throw new IllegalArgumentException("Completed prescriptions cannot be changed.");
        List<PlannedWorkout> workouts = new ArrayList<>(); boolean found = false;
        for (var w : plan.workouts()) {
            if (!w.id().equals(proposal.workoutId())) workouts.add(w);
            else { found = true; if (option.action() == AdjustmentProposal.Action.KEEP) workouts.add(w);
                else if (option.action() == AdjustmentProposal.Action.REPLACE) workouts.add(option.replacement()); }
        }
        if (!found) throw new IllegalArgumentException("This proposal no longer matches the current plan.");
        workouts.sort(Comparator.comparing(PlannedWorkout::date));
        return new TrainingPlan(plan.id(), plan.startDate(), plan.endDate(), plan.phases(), workouts);
    }
}
