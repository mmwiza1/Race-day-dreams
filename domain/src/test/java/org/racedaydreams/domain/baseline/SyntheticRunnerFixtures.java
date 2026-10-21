package org.racedaydreams.domain.baseline;

import org.racedaydreams.domain.*;
import java.time.*;
import java.util.*;

/** Test-only seed contexts. They are never inserted into a runner's live data. */
public final class SyntheticRunnerFixtures {
    public record Fixture(String id, UserProfile profile, RunnerBaseline baseline, BaselineAssessment assessment) {}

    public static List<Fixture> profiles(LocalDate start) {
        Instant now = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        RunnerBaseline empty = RunnerBaseline.empty(now.minus(Duration.ofDays(84)), now);
        List<Fixture> values = new ArrayList<>();
        values.add(new Fixture("A-beginner-5k", profile(RaceDistance.FIVE_K, GoalMode.FINISH, null, start.plusWeeks(12), 3,
                TrainingStrategy.DISTANCE_FIRST, Collections.emptySet()), empty, null));
        values.add(new Fixture("B-sub2-half", profile(RaceDistance.HALF_MARATHON, GoalMode.TARGET_TIME, Duration.ofSeconds(7199), start.plusWeeks(20), 4,
                TrainingStrategy.BALANCED, Collections.emptySet()), manual(empty, now, 20, 4, 10, 8), null));
        values.add(new Fixture("C-marathon", profile(RaceDistance.MARATHON, GoalMode.MAXIMUM_PERFORMANCE, null, start.plusWeeks(24), 5,
                TrainingStrategy.BALANCED, EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)), manual(empty, now, 45, 5, 22, 12), null));
        values.add(new Fixture("D-manual-aerobic", profile(RaceDistance.GENERAL_FITNESS, GoalMode.FINISH, null, null, 3,
                TrainingStrategy.ZONE_2, Collections.emptySet()), manual(empty, now, 15, 3, 6, 8), null));
        values.add(new Fixture("E-returning", profile(RaceDistance.FIVE_K, GoalMode.FINISH, null, start.plusWeeks(16), 3,
                TrainingStrategy.DISTANCE_FIRST, Collections.emptySet()), manual(empty, now, 3, 1, 2, 1), new BaselineAssessment(3, AssessmentMode.CONTROLLED)));
        return Collections.unmodifiableList(values);
    }

    private static RunnerBaseline manual(RunnerBaseline empty, Instant now, double km, double runs, double longest, int weeks) {
        return BaselineEngine.applyManualOverlay(empty, new ManualBaselineInput(km, runs, longest, null, null, null, null, weeks), now);
    }

    private static UserProfile profile(RaceDistance distance, GoalMode mode, Duration target, LocalDate date, int count,
                                       TrainingStrategy strategy, Set<DayOfWeek> gym) {
        EnumSet<DayOfWeek> running = EnumSet.noneOf(DayOfWeek.class);
        for (int index = 1; index <= count; index++) running.add(DayOfWeek.of(index));
        return new UserProfile(new RaceGoal(distance, null, date, mode, target),
                new TrainingPreferences(strategy, running, count, DayOfWeek.of(count), gym,
                        ScheduleType.FLEXIBLE, UnitSystem.METRIC), false);
    }
}
