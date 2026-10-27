package org.racedaydreams.domain.coaching;

import java.util.List;
import org.racedaydreams.domain.PlannedWorkout;

/** Revisioned reminder mirror; actual recorded activity is stored independently. */
public interface PlannedWorkoutRepository {
    List<PlannedWorkout> plannedWorkouts();
    long plannedWorkoutRevision(String id);
    void savePlannedWorkout(PlannedWorkout workout);
    void deletePlannedWorkout(String id);
}
