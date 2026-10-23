package org.racedaydreams.domain.coaching;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.baseline.RunnerBaseline;
import org.racedaydreams.domain.strength.StrengthSession;

public record PlanRequest(UserProfile profile, RunnerBaseline baseline, LocalDate startDate,
        Set<LocalDate> unavailableDates, List<StrengthSession> strengthSessions, Integer maximumMinutes,String distribution) {
    public PlanRequest(UserProfile profile,RunnerBaseline baseline,LocalDate startDate,Set<LocalDate> unavailableDates,List<StrengthSession> strengthSessions){this(profile,baseline,startDate,unavailableDates,strengthSessions,null,"BALANCED");}
    public PlanRequest {
        if(maximumMinutes!=null&&(maximumMinutes<15||maximumMinutes>360)||!java.util.Arrays.asList("FEWER_LONGER","BALANCED","MORE_SHORTER").contains(distribution))throw new IllegalArgumentException("Check session time and distribution preference");
        Objects.requireNonNull(profile); Objects.requireNonNull(baseline); Objects.requireNonNull(startDate);
        unavailableDates = org.racedaydreams.domain.coaching.Immutable.copySet(unavailableDates); strengthSessions = org.racedaydreams.domain.coaching.Immutable.copyList(strengthSessions);
    }
}
