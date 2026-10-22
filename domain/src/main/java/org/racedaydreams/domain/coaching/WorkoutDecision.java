package org.racedaydreams.domain.coaching;
import java.time.*;
import java.util.Objects;
/** Explicit choices are separate from an unrecorded session and from actual completion. */
public record WorkoutDecision(String id,String workoutId,String versionId,LocalDate originalDate,LocalDate movedTo,Kind kind,Instant at) {
    public enum Kind { MOVED, SKIPPED }
    public WorkoutDecision {Objects.requireNonNull(originalDate);Objects.requireNonNull(kind);Objects.requireNonNull(at);if(id==null||workoutId==null||versionId==null||(kind==Kind.MOVED)!=(movedTo!=null))throw new IllegalArgumentException("Invalid workout decision.");}
}
