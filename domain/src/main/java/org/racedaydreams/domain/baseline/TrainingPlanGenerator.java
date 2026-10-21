package org.racedaydreams.domain.baseline;

import java.time.LocalDate;
import org.racedaydreams.domain.TrainingPlan;
import org.racedaydreams.domain.UserProfile;

/**
 * Future deterministic engine boundary. No production implementation or schedule catalogue is
 * provided yet. A future implementation must consume evidence without mutating runner-owned goals.
 */
public interface TrainingPlanGenerator {
    TrainingPlan generate(UserProfile profile, RunnerBaseline baseline, LocalDate startDate);
}
