package org.racedaydreams.domain.coaching;

import java.util.*;
import org.racedaydreams.domain.*;

/** A same-day rebuild must not relabel an already completed prescription with new targets. */
public final class PlanPreservation {
    private PlanPreservation() { }
    public static TrainingPlan retainCompleted(TrainingPlan candidate, Map<String, PlannedWorkout> completed) {
        List<PlannedWorkout> workouts = new ArrayList<>(); Set<java.time.LocalDate> dates = new HashSet<>();
        for (var proposed : candidate.workouts()) {
            var preserved = completed.getOrDefault(proposed.id(), proposed);
            if (!preserved.id().equals(proposed.id()) || !dates.add(preserved.date()))
                throw new IllegalArgumentException("Completed prescriptions conflict with the rebuilt schedule. Keep the existing programme and review availability.");
            workouts.add(preserved);
        }
        workouts.sort(Comparator.comparing(PlannedWorkout::date));
        return new TrainingPlan(candidate.id(), candidate.startDate(), candidate.endDate(), candidate.phases(), workouts);
    }
}
