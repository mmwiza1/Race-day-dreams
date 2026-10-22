package org.racedaydreams.domain.coaching;

import java.util.List;
import java.util.Set;
import org.racedaydreams.domain.strength.*;

/** Provider/storage-neutral boundary. Implementations must preserve immutable versions and atomic decisions. */
public interface CoachingRepository {
    PlanVersion current();
    List<PlanVersion> versions();
    List<StrengthExercise> exercises();
    List<StrengthSession> strength();
    List<RunExecution> responses();
    List<AdjustmentProposal> proposals();
    boolean proposalKnown(String id);
    void saveCurrent(PlanVersion version);
    void saveExercise(StrengthExercise exercise);
    void saveStrength(StrengthSession session);
    void deleteStrength(String id);
    void saveResponse(RunExecution response);
    void saveFeedback(String activityId, String versionId, RunFeedback feedback);
    void saveProposal(AdjustmentProposal proposal);
    void decide(AdjustmentProposal proposal, String optionId, Set<String> completedWorkoutIds);
    void transaction(Runnable action);
}
