package org.racedaydreams.domain.coaching;

/** Runner response is separate from the measured activity and the original prescription. */
public record RunExecution(String activityId, String plannedWorkoutId, String planVersionId,
        Integer completedReps, Boolean recoveriesCompleted, String responseNotes) {
    public RunExecution {
        if (activityId == null || activityId.trim().isEmpty()) throw new IllegalArgumentException("Activity ID is required.");
        if ((plannedWorkoutId == null) != (planVersionId == null)) throw new IllegalArgumentException("A planned link needs its preserved plan version.");
        if (completedReps != null && (completedReps < 0 || completedReps > 1000)) throw new IllegalArgumentException("Check completed reps.");
        responseNotes = responseNotes == null ? "" : responseNotes;
        if (responseNotes.length() > 10000) throw new IllegalArgumentException("Response notes are too long.");
    }
}
