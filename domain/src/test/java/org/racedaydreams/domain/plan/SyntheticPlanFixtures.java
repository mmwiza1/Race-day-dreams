package org.racedaydreams.domain.plan;

import org.racedaydreams.domain.*;
import org.racedaydreams.domain.baseline.SyntheticRunnerFixtures;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * TEST DATA ONLY. This constructs varied whole-plan examples to exercise regression tooling.
 * It is deliberately not in main sources, implements no production generator and is not an NHS
 * programme, training methodology, or a schedule intended for a real runner to follow.
 */
final class SyntheticPlanFixtures {
    static final LocalDate ANCHOR = LocalDate.of(2030, 1, 7);
    static final String NOTE = "SYNTHETIC TEST FIXTURE — not training advice";
    static final PlanRegressionPolicy BUDGET = new PlanRegressionPolicy(4, 1.30, 3, .75, 2, 2, 1, .85);

    record Fixture(String id, UserProfile profile, TrainingPlan plan, Set<LocalDate> lowerBodyDates) { }

    static List<Fixture> all() {
        List<Fixture> values = new ArrayList<>();
        for (SyntheticRunnerFixtures.Fixture context : SyntheticRunnerFixtures.profiles(ANCHOR)) {
            double weekly = context.baseline().weeklyDistanceKm() == null ? 6 : context.baseline().weeklyDistanceKm();
            values.add(build(context.id(), context.profile(), ANCHOR, weekly));
        }
        return values;
    }

    static Fixture build(String id, UserProfile profile, LocalDate start, double weeklyKm) {
        LocalDate race = profile.raceGoal().raceDate();
        LocalDate end = race == null ? start.plusWeeks(8).minusDays(1) : race;
        if (end.isBefore(start)) throw new IllegalArgumentException("Synthetic fixture race precedes start");
        long days = ChronoUnit.DAYS.between(start, end);
        List<TrainingPhase> phases = new ArrayList<>();
        LocalDate taper = race == null || days < 28 ? null : race.minusWeeks(2);
        if (race == null) phases.add(new TrainingPhase(TrainingPhase.Kind.FOUNDATION, start, end, NOTE));
        else if (days == 0) phases.add(new TrainingPhase(TrainingPhase.Kind.RACE, race, race, NOTE));
        else if (taper == null) {
            phases.add(new TrainingPhase(TrainingPhase.Kind.FOUNDATION, start, race.minusDays(1), NOTE));
            phases.add(new TrainingPhase(TrainingPhase.Kind.RACE, race, race, NOTE));
        } else {
            LocalDate foundationEnd = start.plusDays(Math.max(7, days / 4) - 1);
            LocalDate peakStart = taper.minusWeeks(1);
            phases.add(new TrainingPhase(TrainingPhase.Kind.FOUNDATION, start, foundationEnd, NOTE));
            if (foundationEnd.plusDays(1).isBefore(peakStart)) phases.add(new TrainingPhase(TrainingPhase.Kind.ENDURANCE_DEVELOPMENT,
                    foundationEnd.plusDays(1), peakStart.minusDays(1), NOTE));
            phases.add(new TrainingPhase(TrainingPhase.Kind.PEAK, peakStart, taper.minusDays(1), NOTE));
            phases.add(new TrainingPhase(TrainingPhase.Kind.TAPER, taper, race.minusDays(1), NOTE));
            phases.add(new TrainingPhase(TrainingPhase.Kind.RACE, race, race, NOTE));
        }
        EnumSet<DayOfWeek> selected = selectDays(profile.preferences());
        DayOfWeek longDay = profile.preferences().longRunDay();
        if (longDay == null || !selected.contains(longDay)) longDay = selected.toArray(new DayOfWeek[0])[selected.size() - 1];
        List<PlannedWorkout> workouts = new ArrayList<>();
        Set<LocalDate> lowerBody = new java.util.HashSet<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            // Fixture C explicitly labels Tuesday/Friday as hard lower-body dates; a gym-day
            // preference alone is never treated as proof that actual training was hard.
            if (id.startsWith("C-") && profile.preferences().gymDays().contains(date.getDayOfWeek())) lowerBody.add(date);
            if (race != null && date.equals(race)) {
                workouts.add(new PlannedWorkout(id + ":" + date, date, WorkoutCategory.RACE,
                        profile.raceGoal().distance().kilometres(), null, Collections.emptyList(), NOTE));
                continue;
            }
            if (!selected.contains(date.getDayOfWeek())) continue;
            long week = ChronoUnit.DAYS.between(start, date) / 7;
            double budget = weeklyKm * (1 + .02 * Math.min(week, 8));
            if (taper != null && !date.isBefore(taper)) budget = weeklyKm * (date.isBefore(race.minusWeeks(1)) ? .70 : .50);
            double longKm = budget * (selected.size() == 2 ? .55 : .45);
            double otherKm = selected.size() == 1 ? budget : (budget - longKm) / (selected.size() - 1);
            boolean isLong = selected.size() > 1 && date.getDayOfWeek() == longDay;
            WorkoutCategory category = profile.preferences().strategy() == TrainingStrategy.ZONE_2
                    ? WorkoutCategory.ZONE_2_RUN : isLong ? WorkoutCategory.LONG_RUN : WorkoutCategory.EASY_RUN;
            double distance = isLong ? longKm : otherKm;
            workouts.add(new PlannedWorkout(id + ":" + date, date, category, rounded(distance), null, Collections.emptyList(), NOTE));
        }
        return new Fixture(id, profile, new TrainingPlan("test:" + id, start, end, phases, workouts), Collections.unmodifiableSet(lowerBody));
    }

    static UserProfile availability(UserProfile original, int count, LocalDate race) {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (int index = 1; index <= count; index++) days.add(DayOfWeek.of(index));
        RaceGoal prior = original.raceGoal();
        return new UserProfile(new RaceGoal(prior.distance(), null, race, prior.mode(), prior.targetTime()),
                new TrainingPreferences(original.preferences().strategy(), days, count, DayOfWeek.of(count),
                        original.preferences().gymDays(), ScheduleType.FLEXIBLE, original.preferences().units()),
                original.performanceEligible());
    }

    private static EnumSet<DayOfWeek> selectDays(TrainingPreferences preferences) {
        EnumSet<DayOfWeek> selected = EnumSet.noneOf(DayOfWeek.class);
        if (preferences.longRunDay() != null) selected.add(preferences.longRunDay());
        for (DayOfWeek day : DayOfWeek.values()) if (selected.size() < preferences.runsPerWeek() && preferences.runningDays().contains(day)) selected.add(day);
        return selected;
    }
    private static double rounded(double value) { return Math.round(value * 1000) / 1000.0; }
}
