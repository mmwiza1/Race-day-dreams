package org.racedaydreams.domain.plan;

import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.TrainingPhase;
import org.racedaydreams.domain.TrainingPlan;
import org.racedaydreams.domain.UserProfile;
import org.racedaydreams.domain.WorkoutCategory;
import org.racedaydreams.domain.plan.PlanRegressionReport.Issue;
import org.racedaydreams.domain.plan.PlanRegressionReport.Severity;
import org.racedaydreams.domain.plan.PlanRegressionReport.Week;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Whole-plan structural and context regression checks for present/future generators.
 * The checks never generate a workout or change a runner's goal. Configured bounds are explicit
 * engineering acceptance criteria, not a rule such as a universal ten-percent progression limit.
 */
public final class PlanRegression {
    private static final double EPSILON = 0.000001;
    private PlanRegression() { }

    public static PlanRegressionReport inspect(TrainingPlan plan, UserProfile profile,
            Set<LocalDate> knownHardLowerBodyDates, PlanRegressionPolicy policy) {
        Objects.requireNonNull(plan); Objects.requireNonNull(profile);
        Objects.requireNonNull(knownHardLowerBodyDates); Objects.requireNonNull(policy);
        List<Issue> issues = new ArrayList<>();
        inspectPhases(plan, issues);
        List<PlannedWorkout> ordered = new ArrayList<>(plan.workouts());
        ordered.sort(Comparator.comparing(PlannedWorkout::date).thenComparing(PlannedWorkout::id));
        Set<String> ids = new HashSet<>();
        Set<LocalDate> runDates = new HashSet<>();
        TreeMap<LocalDate, MutableWeek> weeks = new TreeMap<>();
        LocalDate firstMonday = monday(plan.startDate());
        for (LocalDate date = firstMonday; !date.isAfter(plan.endDate()); date = date.plusWeeks(1)) {
            weeks.put(date, new MutableWeek(!date.isBefore(plan.startDate()) && !date.plusDays(6).isAfter(plan.endDate())));
        }
        PlannedWorkout previousHard = null;
        PlannedWorkout previousLong = null;
        for (PlannedWorkout workout : ordered) {
            if (!ids.add(workout.id())) add(issues, "DUPLICATE_WORKOUT_ID", workout.date(), "Workout IDs must be stable and unique: " + workout.id());
            if (!runDates.add(workout.date())) add(issues, "MULTIPLE_RUNS_ON_DAY", workout.date(), "This availability model supports one prescribed run per day.");
            boolean race = workout.category() == WorkoutCategory.RACE;
            boolean intendedRace = race && workout.date().equals(profile.raceGoal().raceDate());
            if (!intendedRace && !profile.preferences().runningDays().contains(workout.date().getDayOfWeek())) {
                add(issues, "UNAVAILABLE_DAY", workout.date(), "Run falls outside the runner's selected running days.");
            }
            if (race && !intendedRace) add(issues, "UNSELECTED_RACE", workout.date(), "A race prescription does not match the runner-owned event date.");
            if ((workout.distanceKm() == null || workout.distanceKm() <= 0)
                    && (workout.duration() == null || workout.duration().isZero())) {
                add(issues, "EMPTY_PRESCRIPTION", workout.date(), "A workout needs a positive distance or time prescription.");
            }
            MutableWeek week = weeks.get(monday(workout.date()));
            if (week == null) {
                add(issues, "OUTSIDE_PLAN", workout.date(), "Workout falls outside the plan horizon.");
                continue;
            }
            week.totalRuns++;
            if (!race) {
                week.trainingRuns++;
                if (workout.distanceKm() == null) week.unknownDistance++;
                else { week.knownDistance += workout.distanceKm(); week.largestRun = Math.max(week.largestRun, workout.distanceKm()); }
            }
            if (hard(workout.category())) {
                if (!race) week.hard++;
                if (previousHard != null && ChronoUnit.DAYS.between(previousHard.date(), workout.date())
                        < policy.minimumDaysBetweenHardSessions()) {
                    add(issues, "HARD_DAY_SPACING", workout.date(), "Hard sessions are closer than the fixture's configured recovery spacing.");
                }
                previousHard = workout;
            }
            if (hard(workout.category()) || workout.category() == WorkoutCategory.LONG_RUN) {
                for (int days = 1; days <= policy.daysAfterHardLowerBody(); days++) {
                    if (knownHardLowerBodyDates.contains(workout.date().minusDays(days))) {
                        add(issues, "HARD_LOWER_BODY_PROXIMITY", workout.date(), "A long/hard run follows a known hard lower-body session within the configured review window.");
                        break;
                    }
                }
            }
            if (workout.category() == WorkoutCategory.LONG_RUN && workout.distanceKm() != null) {
                week.longest = week.longest == null ? workout.distanceKm() : Math.max(week.longest, workout.distanceKm());
                if (previousLong != null && previousLong.distanceKm() != null
                        && workout.distanceKm() - previousLong.distanceKm() > policy.maximumLongRunIncreaseKm() + EPSILON) {
                    add(issues, "LONG_RUN_SPIKE", workout.date(), "Long-run distance increases beyond the context's explicit acceptance budget.");
                }
                previousLong = workout;
            }
        }
        List<Week> summaries = new ArrayList<>();
        Map.Entry<LocalDate, MutableWeek> prior = null;
        for (Map.Entry<LocalDate, MutableWeek> entry : weeks.entrySet()) {
            MutableWeek week = entry.getValue();
            if (week.totalRuns > profile.preferences().runsPerWeek()) add(issues, "RUN_FREQUENCY", entry.getKey(), "Prescribed runs, including the race, exceed the runner's chosen weekly count.");
            if (week.hard > policy.maximumHardSessionsPerWeek()) add(issues, "HARD_FREQUENCY", entry.getKey(), "Hard-session count exceeds this fixture's explicit acceptance budget.");
            if (week.unknownDistance > 0) {
                issues.add(new Issue(Severity.UNVERIFIABLE, "UNKNOWN_WEEKLY_DISTANCE", entry.getKey(), "Time-only prescriptions prevent a complete distance comparison; missing distance is not zero."));
            } else if (week.complete && week.knownDistance > 0 && week.largestRun / week.knownDistance > policy.maximumSingleRunFraction() + EPSILON) {
                add(issues, "SINGLE_RUN_DOMINATES", entry.getKey(), "One run occupies more than the fixture's permitted share of prescribed weekly distance.");
            }
            if (prior != null && week.complete && prior.getValue().complete && week.unknownDistance == 0 && prior.getValue().unknownDistance == 0
                    && prior.getValue().knownDistance > 0) {
                double previous = prior.getValue().knownDistance;
                if (week.knownDistance - previous > policy.maximumWeeklyIncreaseKm() + EPSILON
                        || week.knownDistance / previous > policy.maximumWeeklyMultiplier() + EPSILON) {
                    add(issues, "WEEKLY_DISTANCE_SPIKE", entry.getKey(), "Prescribed weekly distance exceeds this context's relative or absolute change budget.");
                }
            }
            summaries.add(new Week(entry.getKey(), week.complete, week.trainingRuns, week.hard,
                    week.unknownDistance > 0 ? null : week.knownDistance, week.knownDistance,
                    week.unknownDistance, week.longest));
            prior = entry;
        }
        inspectTaper(plan, profile, summaries, policy, issues);
        return new PlanRegressionReport(issues, summaries);
    }

    public static boolean hard(WorkoutCategory category) {
        return category == WorkoutCategory.INTERVALS || category == WorkoutCategory.TEMPO_THRESHOLD_RUN
                || category == WorkoutCategory.RACE_PACE_RUN || category == WorkoutCategory.TIME_TRIAL
                || category == WorkoutCategory.PROGRESSION_RUN || category == WorkoutCategory.RACE;
    }

    private static void inspectPhases(TrainingPlan plan, List<Issue> issues) {
        List<TrainingPhase> phases = new ArrayList<>(plan.phases());
        phases.sort(Comparator.comparing(TrainingPhase::startDate).thenComparing(TrainingPhase::endDate));
        LocalDate next = plan.startDate();
        for (TrainingPhase phase : phases) {
            if (!phase.startDate().equals(next)) add(issues, phase.startDate().isBefore(next) ? "PHASE_OVERLAP" : "PHASE_GAP", phase.startDate(), "Phase dates must cover the horizon once, without overlaps or gaps.");
            if (!phase.endDate().isBefore(next)) next = phase.endDate().plusDays(1);
        }
        if (!next.equals(plan.endDate().plusDays(1))) add(issues, "PHASE_GAP", next, "Phases do not cover the complete plan horizon.");
    }

    private static void inspectTaper(TrainingPlan plan, UserProfile profile, List<Week> weeks,
            PlanRegressionPolicy policy, List<Issue> issues) {
        if (profile.raceGoal().raceDate() == null || ChronoUnit.DAYS.between(plan.startDate(), plan.endDate()) < 28) return;
        LocalDate taper = null;
        for (TrainingPhase phase : plan.phases()) if (phase.kind() == TrainingPhase.Kind.TAPER) taper = taper == null ? phase.startDate() : taper.isBefore(phase.startDate()) ? taper : phase.startDate();
        if (taper == null) { add(issues, "TAPER_MISSING", plan.endDate(), "This dated regression fixture has no taper phase."); return; }
        double priorPeak = 0;
        for (Week week : weeks) if (week.start().isBefore(taper) && week.trainingDistanceKm() != null) priorPeak = Math.max(priorPeak, week.trainingDistanceKm());
        if (priorPeak == 0) return;
        for (Week week : weeks) {
            if (week.complete() && !week.start().isBefore(taper) && week.trainingRuns() > 0 && week.trainingDistanceKm() != null
                    && week.trainingDistanceKm() > priorPeak * policy.maximumTaperWeekFraction() + EPSILON) {
                add(issues, "TAPER_VOLUME", week.start(), "Taper-week training volume is not reduced within this fixture's explicit acceptance budget.");
            }
        }
    }

    private static LocalDate monday(LocalDate date) { return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }
    private static void add(List<Issue> issues, String code, LocalDate date, String reason) { issues.add(new Issue(Severity.ERROR, code, date, reason)); }
    private static final class MutableWeek {
        final boolean complete;
        int totalRuns; int trainingRuns; int hard; int unknownDistance; double knownDistance; double largestRun; Double longest;
        MutableWeek(boolean complete) { this.complete = complete; }
    }
}
