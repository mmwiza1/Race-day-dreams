package org.racedaydreams.domain;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Dependency-free unit suite, also invoked by Gradle's test lifecycle. */
public final class DomainTestSuite {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
    private static int checks;

    private DomainTestSuite() { }

    public static void main(String[] args) {
        paceAndTime();
        goalValidation();
        dateBoundaries();
        availability();
        strategies();
        plannedAndActual();
        optionalMetrics();
        transparentFutureModels();
        System.out.println("DomainTestSuite: " + checks + " checks passed.");
    }

    private static void paceAndTime() {
        equal(Duration.ofSeconds(7199), PaceCalculator.parseTime("1:59:59"), "HM target time");
        equal(Duration.ofMinutes(27), PaceCalculator.parseTime("27:00"), "Two-part time");
        equal(Duration.ofHours(2), PaceCalculator.parseTime("120:00"), "Minutes over 59");
        equal(Duration.ofSeconds(1), PaceCalculator.parseTime("00:01"), "Smallest whole-second time");
        equal(Duration.ofHours(48), PaceCalculator.parseTime("48:00:00"), "Upper target boundary");
        equal(Duration.ofMinutes(27), PaceCalculator.parseTime(" 27:00 "), "Whitespace trimming");
        equal("1:59:59", PaceCalculator.formatTime(Duration.ofSeconds(7199)), "Time formatting");
        equal("0:27:00", PaceCalculator.formatTime(Duration.ofMinutes(27)), "Time formatting below hour");
        equal("0:00:00", PaceCalculator.formatTime(Duration.ZERO), "Zero display duration");
        equal("5:41/km", PaceCalculator.targetPace(Duration.ofSeconds(7199), RaceDistance.HALF_MARATHON, UnitSystem.METRIC), "HM metric pace");
        equal("9:09/mi", PaceCalculator.targetPace(Duration.ofSeconds(7199), RaceDistance.HALF_MARATHON, UnitSystem.IMPERIAL), "HM imperial pace");
        equal("5:24/km", PaceCalculator.targetPace(Duration.ofMinutes(27), RaceDistance.FIVE_K, UnitSystem.METRIC), "5K pace");
        equal("5:30/km", PaceCalculator.targetPace(Duration.ofMinutes(55), RaceDistance.TEN_K, UnitSystem.METRIC), "10K pace");
        equal("4:16/km", PaceCalculator.targetPace(Duration.ofHours(3), RaceDistance.MARATHON, UnitSystem.METRIC), "Marathon pace");
        equal("6:00/km", PaceCalculator.targetPace(Duration.ofSeconds(1799), RaceDistance.FIVE_K, UnitSystem.METRIC), "Pace rounding carries a minute");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"));
            equal("5:41/km", PaceCalculator.targetPace(Duration.ofSeconds(7199), RaceDistance.HALF_MARATHON, UnitSystem.METRIC), "Stable persisted/display conversion regardless of locale");
        } finally { Locale.setDefault(previous); }
        for (String invalid : new String[] {"", "1", "1:2", "1:60", "1:60:00", "1:00:60", "-1:00", "+1:00", "1.5:00", "1:00:00:00", "00:00", "0:00:00", "48:00:01", "2881:00", "99999999999999:00", "١:٠٠"}) {
            rejects(() -> PaceCalculator.parseTime(invalid), "Reject malformed/out-of-range target: " + invalid);
        }
        rejects(() -> PaceCalculator.parseTime(null), "Reject missing target");
        rejects(() -> PaceCalculator.targetPace(Duration.ZERO, RaceDistance.TEN_K, UnitSystem.METRIC), "Reject zero target pace");
        rejects(() -> PaceCalculator.targetPace(Duration.ofMinutes(30), RaceDistance.GENERAL_FITNESS, UnitSystem.METRIC), "No invented general-fitness race pace");
        rejects(() -> PaceCalculator.formatTime(Duration.ofSeconds(-1)), "Reject negative duration display");
    }

    private static void goalValidation() {
        for (RaceDistance distance : RaceDistance.values()) {
            RaceGoal finish = new RaceGoal(distance, null, null, GoalMode.FINISH, null);
            equal("", finish.raceName(), "Optional race name normalized: " + distance);
            equal(null, finish.raceDate(), "No event allowed: " + distance);
            RaceGoal maximum = new RaceGoal(distance, "", null, GoalMode.MAXIMUM_PERFORMANCE, null);
            equal(GoalMode.MAXIMUM_PERFORMANCE, maximum.mode(), "Maximum performance needs no target: " + distance);
            if (distance != RaceDistance.GENERAL_FITNESS) {
                RaceGoal target = new RaceGoal(distance, "", TODAY.plusMonths(6), GoalMode.TARGET_TIME, Duration.ofHours(1));
                equal(distance, target.distance(), "Target time supported for race distance");
            }
        }
        equal("Park Run", new RaceGoal(RaceDistance.FIVE_K, " Park Run ", TODAY, GoalMode.FINISH, null).raceName(), "Race name trimmed");
        equal("", new RaceGoal(RaceDistance.FIVE_K, null, TODAY, GoalMode.FINISH, null).raceName(), "Unnamed event supported");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "Race", null, GoalMode.FINISH, null), "Named event needs date");
        rejects(() -> new RaceGoal(RaceDistance.GENERAL_FITNESS, "", TODAY, GoalMode.FINISH, null), "General fitness has no event anchor");
        rejects(() -> new RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.TARGET_TIME, Duration.ofMinutes(30)), "General fitness has no finishing time");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.TARGET_TIME, null), "Time mode requires target");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.TARGET_TIME, Duration.ZERO), "Target must be positive");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.TARGET_TIME, Duration.ofSeconds(-1)), "Negative target invalid");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.TARGET_TIME, Duration.ofHours(49)), "Huge target invalid");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.TARGET_TIME, Duration.ofSeconds(20).plusNanos(1)), "Fractional seconds cannot silently lose precision in persistence");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.FINISH, Duration.ofMinutes(30)), "Finish mode must not retain stale time");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.MAXIMUM_PERFORMANCE, Duration.ofMinutes(30)), "Maximum mode must not retain stale time");
        rejects(() -> new RaceGoal(RaceDistance.FIVE_K, "x".repeat(201), TODAY, GoalMode.FINISH, null), "Race name length bounded");
        equal(21.0975, RaceDistance.HALF_MARATHON.kilometres(), "Official HM distance");
        equal(42.195, RaceDistance.MARATHON.kilometres(), "Official marathon distance");
    }

    private static void dateBoundaries() {
        for (long days : new long[] {0, 1, 6, 7, 9 * 7, 365, 366}) {
            LocalDate date = TODAY.plusDays(days);
            UserProfile profile = profile(RaceDistance.MARATHON, date, TrainingStrategy.BALANCED, false);
            OnboardingValidator.validate(profile, CLOCK);
            equal(days, RaceDates.daysUntil(date, CLOCK), "Remaining calendar days: " + days);
            equal(days / 7, RaceDates.weeksUntil(date, CLOCK), "Whole weeks: " + days);
        }
        UserProfile past = profile(RaceDistance.FIVE_K, TODAY.minusDays(1), TrainingStrategy.DISTANCE_FIRST, false);
        rejects(() -> OnboardingValidator.validate(past, CLOCK), "Onboarding rejects past date");
        equal(0L, RaceDates.daysUntil(TODAY.minusDays(10), CLOCK), "Old stored race remains readable");
        OnboardingValidator.validate(profile(RaceDistance.GENERAL_FITNESS, null, TrainingStrategy.ZONE_2, false), CLOCK);
        check(true, "General fitness needs no race date");
        Clock localClock = Clock.fixed(Instant.parse("2026-10-21T00:30:00Z"), ZoneId.of("America/Los_Angeles"));
        equal(1L, RaceDates.daysUntil(TODAY, localClock), "Race date follows local calendar, not UTC elapsed seconds");
        Clock dstClock = Clock.fixed(Instant.parse("2026-03-28T12:00:00Z"), ZoneId.of("Europe/London"));
        equal(2L, RaceDates.daysUntil(LocalDate.of(2026, 3, 30), dstClock), "DST calendar day calculation");
    }

    private static void availability() {
        Set<DayOfWeek> running = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY);
        Set<DayOfWeek> gym = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY);
        TrainingPreferences hybrid = new TrainingPreferences(TrainingStrategy.ZONE_2, running, 3, DayOfWeek.SUNDAY, gym, ScheduleType.FLEXIBLE, UnitSystem.METRIC);
        check(hybrid.runningDays().contains(DayOfWeek.MONDAY) && hybrid.gymDays().contains(DayOfWeek.MONDAY), "Run and gym can share the day");
        equal(5, hybrid.gymDays().size(), "Five gym days does not remove running availability");
        running.clear(); gym.clear();
        equal(3, hybrid.runningDays().size(), "Running days defensive copy");
        equal(5, hybrid.gymDays().size(), "Gym days defensive copy");
        expect(UnsupportedOperationException.class, () -> hybrid.runningDays().clear(), "Running set immutable");
        expect(UnsupportedOperationException.class, () -> hybrid.gymDays().clear(), "Gym set immutable");
        for (int runs : new int[] {1, 2, 6, 7}) {
            TrainingPreferences preferences = new TrainingPreferences(TrainingStrategy.BALANCED, EnumSet.allOf(DayOfWeek.class), runs, null, Set.of(), ScheduleType.FIXED, UnitSystem.IMPERIAL);
            equal(runs, preferences.runsPerWeek(), "Availability boundary: " + runs);
            equal(null, preferences.longRunDay(), "No preferred long-run day allowed");
            equal(0, preferences.gymDays().size(), "No gym is supported");
        }
        rejects(() -> preferences(Set.of(), 1, null), "Empty running availability");
        rejects(() -> preferences(Set.of(DayOfWeek.MONDAY), 2, null), "Runs cannot exceed selected days");
        rejects(() -> preferences(EnumSet.allOf(DayOfWeek.class), 0, null), "Zero runs rejected");
        rejects(() -> preferences(EnumSet.allOf(DayOfWeek.class), 8, null), "Eight runs rejected in one-run-per-day foundation");
        rejects(() -> preferences(Set.of(DayOfWeek.MONDAY), 1, DayOfWeek.SUNDAY), "Long run must be on selected day");
    }

    private static void strategies() {
        for (TrainingStrategy strategy : TrainingStrategy.values()) {
            UserProfile profile = profile(RaceDistance.TEN_K, null, strategy, strategy == TrainingStrategy.PERFORMANCE);
            equal(strategy, profile.preferences().strategy(), "Strategy retained: " + strategy);
        }
        UserProfile preserved = profile(RaceDistance.TEN_K, null, TrainingStrategy.PERFORMANCE, false);
        equal(TrainingStrategy.PERFORMANCE, preserved.preferences().strategy(), "Saved Performance preference survives insufficient current evidence");
        equal(false, preserved.performanceEligible(), "Legacy eligibility snapshot remains false without silently changing the preference");
        UserProfile maximum = new UserProfile(new RaceGoal(RaceDistance.TEN_K, "", null, GoalMode.MAXIMUM_PERFORMANCE, null), preferences(Set.of(DayOfWeek.MONDAY), 1, null), false);
        equal(GoalMode.MAXIMUM_PERFORMANCE, maximum.raceGoal().mode(), "Maximum-performance goal independent of Performance strategy eligibility");
    }

    private static void plannedAndActual() {
        List<WorkoutSegment> segments = new ArrayList<>();
        segments.add(new WorkoutSegment(WorkoutSegment.Kind.RUN, 8.0, null, null, null, null, 3.0, "Conversational effort"));
        PlannedWorkout planned = new PlannedWorkout("p1", TODAY, WorkoutCategory.EASY_RUN, 8.0, null, segments, "Original prescription");
        CompletedWorkout actual = new CompletedWorkout("a1", planned.id(), CLOCK.instant(), 11.4, Duration.ofMinutes(70), null, 7.0, "Week changed");
        equal(8.0, planned.distanceKm(), "Original distance preserved after longer actual run");
        equal(11.4, actual.distanceKm(), "Actual distance stored independently");
        equal("p1", actual.plannedWorkoutId(), "Actual references original prescription");
        equal(null, actual.averageHeartRate(), "Actual completion works without HR");
        segments.clear();
        equal(1, planned.segments().size(), "Prescribed segments defensive copy");
        expect(UnsupportedOperationException.class, () -> planned.segments().clear(), "Segments immutable");
        CompletedWorkout custom = new CompletedWorkout("a2", null, CLOCK.instant(), 5.0, Duration.ofMinutes(30), null, null, null);
        equal(null, custom.plannedWorkoutId(), "Unplanned run allowed without punishment/fake plan");
        GymSession gym = new GymSession("g1", CLOCK.instant(), GymSession.Type.LOWER_BODY, Duration.ofMinutes(58), GymSession.Effort.HARD, null, GymSession.Source.MANUAL, null);
        equal(gym.startedAt(), actual.completedAt(), "Gym and run observations can overlap");
        rejects(() -> new CompletedWorkout("bad", null, CLOCK.instant(), -1.0, null, null, null, null), "Negative observed distance invalid");
        rejects(() -> new CompletedWorkout("bad", null, CLOCK.instant(), Double.NaN, null, null, null, null), "NaN observed distance invalid");
        rejects(() -> new CompletedWorkout("bad", null, CLOCK.instant(), null, null, null, 11.0, null), "RPE above range invalid");
        rejects(() -> new WorkoutSegment(WorkoutSegment.Kind.RUN, null, null, 400.0, 350.0, null, null, null), "Pace range must be ordered");
    }

    private static void optionalMetrics() {
        RecoveryMetrics absent = new RecoveryMetrics(null, null, null, null, null);
        equal(null, absent.sleep(), "Missing sleep is not fabricated as zero");
        equal(null, absent.restingHeartRate(), "Missing resting HR is supported");
        TrainingMetrics metrics = new TrainingMetrics(null, null, null, null, null, null, null, null);
        equal(null, metrics.currentWeekKm(), "Unknown mileage remains unavailable");
        new RecoveryMetrics(Duration.ZERO, 60.0, 0.0, 10.0, "User supplied");
        check(true, "Subjective metric boundaries accepted");
        rejects(() -> new RecoveryMetrics(Duration.ofMinutes(-1), null, null, null, null), "Negative sleep invalid");
        rejects(() -> new RecoveryMetrics(null, Double.POSITIVE_INFINITY, null, null, null), "Non-finite HR invalid");
        rejects(() -> new TrainingMetrics(0.0, 0.0, 0.0, 0.0, 8, 0, 0, 0), "Invalid running day count rejected");
    }

    private static void transparentFutureModels() {
        AdjustmentOption keep = new AdjustmentOption("keep", "Keep original workout", AdjustmentOption.Action.KEEP_ORIGINAL, List.of(), "Your decision remains available.");
        AdjustmentOption easier = new AdjustmentOption("easy", "Easy run instead", AdjustmentOption.Action.EASY_RUN, List.of(), "Reduce intensity if you choose.");
        List<AdjustmentOption> choices = new ArrayList<>(List.of(keep, easier));
        PlanAdjustment pending = new PlanAdjustment("adjustment", "plan", CLOCK.instant(), "Consider an easier run.", "Recent workload is above your usual range.", choices, null);
        equal(null, pending.selectedOptionId(), "Recommendations cannot silently accept themselves");
        choices.clear();
        equal(2, pending.options().size(), "User choices preserved defensively");
        PlanAdjustment overridden = new PlanAdjustment("adjustment", "plan", CLOCK.instant(), pending.recommendation(), pending.reason(), pending.options(), "keep");
        equal("keep", overridden.selectedOptionId(), "Runner can retain the original workout");
        rejects(() -> new PlanAdjustment("a", "p", CLOCK.instant(), "Change", "Reason", List.of(easier), null), "Forced adjustment rejected");
        rejects(() -> new PlanAdjustment("a", "p", CLOCK.instant(), "Change", "Reason", List.of(keep, easier), "unknown"), "Only offered choices may be selected");
        rejects(() -> new PlanAdjustment("a", "p", CLOCK.instant(), "Change", "Reason", List.of(keep, keep), null), "Choices must have unique identifiers");
        RacePrediction range = new RacePrediction(RaceDistance.HALF_MARATHON, Duration.ofMinutes(121), Duration.ofMinutes(125), CLOCK.instant(), List.of("Recent runs"), "Endurance evidence remains limited.");
        equal(Duration.ofMinutes(125), range.upperFinishTime(), "Prediction range retained as evidence model only");
        rejects(() -> new RacePrediction(RaceDistance.TEN_K, Duration.ofMinutes(60), Duration.ofMinutes(50), CLOCK.instant(), List.of(), "Limitation"), "Prediction range cannot be reversed");
        RaceReadiness readiness = new RaceReadiness(82, CLOCK.instant(), List.of(new RaceReadiness.Indicator("Endurance", 80, "Explained internal score, not a probability.")));
        equal(82, readiness.score(), "Transparent readiness score model");
        rejects(() -> new RaceReadiness(101, CLOCK.instant(), List.of()), "Readiness has bounded score");
        TrainingPhase foundation = new TrainingPhase(TrainingPhase.Kind.FOUNDATION, TODAY, TODAY.plusDays(7), "Allocation pending engine implementation.");
        TrainingPlan plan = new TrainingPlan("plan", TODAY, TODAY.plusDays(14), List.of(foundation), List.of());
        equal(foundation, plan.phases().get(0), "Future phase model supported without generating a plan");
        rejects(() -> new TrainingPlan("plan", TODAY, TODAY.plusDays(1), List.of(foundation), List.of()), "Phases must fit plan dates");
        rejects(() -> new TrainingPhase(TrainingPhase.Kind.TAPER, TODAY, TODAY.minusDays(1), "No"), "Phase dates ordered");
    }

    private static TrainingPreferences preferences(Set<DayOfWeek> days, int runs, DayOfWeek longDay) {
        return new TrainingPreferences(TrainingStrategy.BALANCED, days, runs, longDay, Set.of(), ScheduleType.FIXED, UnitSystem.METRIC);
    }

    private static UserProfile profile(RaceDistance distance, LocalDate raceDate, TrainingStrategy strategy, boolean eligible) {
        return new UserProfile(new RaceGoal(distance, "", raceDate, GoalMode.FINISH, null),
                new TrainingPreferences(strategy, Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY), 3, DayOfWeek.SUNDAY, Set.of(), ScheduleType.FLEXIBLE, UnitSystem.METRIC), eligible);
    }

    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name);
    }

    private static void equal(Object expected, Object actual, String name) {
        check(Objects.equals(expected, actual), name + ": expected " + expected + ", got " + actual);
    }

    private static void rejects(Runnable operation, String name) {
        expect(IllegalArgumentException.class, operation, name);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation, String name) {
        checks++;
        try { operation.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError(name + ": expected " + type.getSimpleName() + ", got " + error, error);
        }
        throw new AssertionError(name + ": expected " + type.getSimpleName());
    }
}
