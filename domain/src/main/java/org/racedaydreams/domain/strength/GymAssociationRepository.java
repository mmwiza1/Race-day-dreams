package org.racedaydreams.domain.strength;

import java.time.Instant;
import java.util.List;

/** Explicit choices over completed evidence; callers must keep UI success after acknowledgement. */
public interface GymAssociationRepository {
    List<GymWorkoutAssociation.Proposal> proposals(String actualId, Instant reviewedAt);

    GymWorkoutAssociation.Decision current(String actualId);

    GymWorkoutAssociation.Proposal reviewCurrent(String actualId, Instant reviewedAt);

    GymWorkoutAssociation.Decision decide(GymWorkoutAssociation.Decision decision);
}
