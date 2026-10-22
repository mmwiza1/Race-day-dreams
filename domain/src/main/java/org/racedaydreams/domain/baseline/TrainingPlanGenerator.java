package org.racedaydreams.domain.baseline;

import java.time.LocalDate;
import org.racedaydreams.domain.TrainingPlan;
import org.racedaydreams.domain.UserProfile;

/** Deterministic plan boundary; production implementation lives in domain.coaching. */
public interface TrainingPlanGenerator {
    TrainingPlan generate(UserProfile profile, RunnerBaseline baseline, LocalDate startDate);
}
