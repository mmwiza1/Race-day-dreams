package org.racedaydreams.domain.coaching;

/** Runner response is separate from the measured activity and the original prescription. */
public record RunExecution(String activityId, String plannedWorkoutId, String planVersionId,
        Integer completedReps, Boolean recoveriesCompleted, String responseNotes, java.util.List<StepEvidence> steps) {
    public record StepEvidence(int index, org.racedaydreams.domain.WorkoutSegment.Kind kind, double metres,
            long activeMillis, Double fastPace, Double slowPace, boolean completed, boolean reliablePace) {
        public StepEvidence {if(index<0||index>=500||kind==null||!Double.isFinite(metres)||metres<0||activeMillis<0||fastPace!=null&&(!Double.isFinite(fastPace)||fastPace<=0)||slowPace!=null&&(!Double.isFinite(slowPace)||slowPace<=0)||fastPace!=null&&slowPace!=null&&fastPace>slowPace)throw new IllegalArgumentException("Invalid step execution evidence");}
    }
    public RunExecution(String activityId,String plannedWorkoutId,String planVersionId,Integer completedReps,Boolean recoveriesCompleted,String responseNotes){this(activityId,plannedWorkoutId,planVersionId,completedReps,recoveriesCompleted,responseNotes,java.util.Collections.emptyList());}
    public RunExecution {
        steps=org.racedaydreams.domain.coaching.Immutable.copyList(steps);java.util.Set<Integer> indices=new java.util.HashSet<>();for(var step:steps)if(!indices.add(step.index()))throw new IllegalArgumentException("Duplicate step evidence");if(!steps.isEmpty()&&plannedWorkoutId==null)throw new IllegalArgumentException("Structured evidence needs a preserved prescription");if(steps.size()>500)throw new IllegalArgumentException("Too many execution steps");
        if (activityId == null || activityId.trim().isEmpty()) throw new IllegalArgumentException("Activity ID is required.");
        if ((plannedWorkoutId == null) != (planVersionId == null)) throw new IllegalArgumentException("A planned link needs its preserved plan version.");
        if (completedReps != null && (completedReps < 0 || completedReps > 1000)) throw new IllegalArgumentException("Check completed reps.");
        responseNotes = responseNotes == null ? "" : responseNotes;
        if (responseNotes.length() > 10000) throw new IllegalArgumentException("Response notes are too long.");
    }
}
