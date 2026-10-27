package org.racedaydreams.domain.coaching;

import org.racedaydreams.domain.PlannedWorkout;

import java.util.List;

/** Revisioned reminder mirror; actual recorded activity is stored independently. */
public interface PlannedWorkoutRepository {
    List<PlannedWorkout> plannedWorkouts();

    long plannedWorkoutRevision(String id);

    void savePlannedWorkout(PlannedWorkout workout);

    void deletePlannedWorkout(String id);
}
