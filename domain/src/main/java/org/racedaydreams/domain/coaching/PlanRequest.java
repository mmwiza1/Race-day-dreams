package org.racedaydreams.domain.coaching;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.baseline.RunnerBaseline;
import org.racedaydreams.domain.strength.StrengthSession;

public record PlanRequest(UserProfile profile, RunnerBaseline baseline, LocalDate startDate,
        Set<LocalDate> unavailableDates, List<StrengthSession> strengthSessions) {
    public PlanRequest {
        Objects.requireNonNull(profile); Objects.requireNonNull(baseline); Objects.requireNonNull(startDate);
        unavailableDates = org.racedaydreams.domain.coaching.Immutable.copySet(unavailableDates); strengthSessions = org.racedaydreams.domain.coaching.Immutable.copyList(strengthSessions);
    }
}
