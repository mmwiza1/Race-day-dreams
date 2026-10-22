package org.racedaydreams.domain.coaching;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.baseline.*;
import org.racedaydreams.domain.strength.StrengthAnalytics;

/** Original, deterministic coaching heuristics. No commercial schedules or prescription tables. */
public final class OriginalTrainingPlanGenerator implements TrainingPlanGenerator {
    @Override public TrainingPlan generate(UserProfile p, RunnerBaseline b, LocalDate start) {
        return generate(new PlanRequest(p, b, start, Set.of(), List.of()));
    }
    public TrainingPlan generate(PlanRequest request) {
        UserProfile profile = request.profile(); RunnerBaseline baseline = request.baseline();
        RaceGoal goal = profile.raceGoal(); TrainingPreferences prefs = profile.preferences();
        LocalDate start = request.startDate(), race = goal.raceDate();
        LocalDate end = race == null ? start.plusWeeks(12).minusDays(1) : race;
        if (end.isBefore(start)) throw new IllegalArgumentException("The race is in the past. Review its date before generating a plan.");
        if (ChronoUnit.DAYS.between(start, end) > 730) throw new IllegalArgumentException("Choose a race within two years; a nearer rolling plan can be used meanwhile.");
        if (race != null && (!prefs.runningDays().contains(race.getDayOfWeek()) || request.unavailableDates().contains(race)))
            throw new IllegalArgumentException("Race day is outside your running availability. Review availability or the race date; no plan was changed.");
        String id = UUID.nameUUIDFromBytes((start + ":" + goal + ":" + prefs).getBytes(StandardCharsets.UTF_8)).toString();
        int weeks = (int) (ChronoUnit.DAYS.between(start, end) / 7) + 1;
        boolean established = baseline.weeklyDistanceKm() != null && baseline.weeklyDistanceKm() >= 10
                && baseline.longestRunKm() != null && baseline.longestRunKm() >= 3;
        boolean recent = baseline.lastActivityAt() != null && !baseline.lastActivityAt().isBefore(start.minusDays(21).atStartOfDay().toInstant(ZoneOffset.UTC))
                || baseline.manualRecordedAt() != null && !baseline.manualRecordedAt().isBefore(start.minusDays(21).atStartOfDay().toInstant(ZoneOffset.UTC));
        // Stale history is useful context but not authority to start at its old workload.
        if (!recent) established = false;
        boolean performance = PerformanceEligibilityEvaluator.evaluate(baseline, start.atTime(23, 59).toInstant(ZoneOffset.UTC)).eligible();
        boolean qualitySupported = established && baseline.consistentWeeks() != null && baseline.consistentWeeks() >= 6
                && baseline.weeklyDistanceKm() >= 20 && prefs.runsPerWeek() >= 3;
        int frequency = established ? prefs.runsPerWeek() : Math.min(3, prefs.runsPerWeek());
        double base = established ? baseline.weeklyDistanceKm() : 0;
        double longBase = established ? Math.min(baseline.longestRunKm(), base * .48) : 0;
        double distance = goal.distance().kilometres();
        int taperDays = race == null ? 0 : goal.distance() == RaceDistance.MARATHON ? (base >= 45 ? 21 : 14)
                : goal.distance() == RaceDistance.HALF_MARATHON ? 10 : 6;
        taperDays = Math.min(taperDays, Math.max(0, (int) ChronoUnit.DAYS.between(start, end)));
        LocalDate taperStart = end.minusDays(taperDays);
        List<TrainingPhase> phases = phases(start, end, race, taperStart, established, weeks);
        List<PlannedWorkout> workouts = new ArrayList<>();
        Set<LocalDate> heavy = StrengthAnalytics.hardLowerBodyDates(request.strengthSessions());
        double volumeCap = goal.distance() == RaceDistance.MARATHON ? Math.max(base, 60)
                : goal.distance() == RaceDistance.HALF_MARATHON ? Math.max(base, 42)
                : goal.distance() == RaceDistance.TEN_K ? Math.max(base, 32) : Math.max(base, 26);
        if (goal.distance() == RaceDistance.GENERAL_FITNESS) volumeCap = Math.max(base, 30);
        double longCap = goal.distance() == RaceDistance.MARATHON ? 30 : goal.distance() == RaceDistance.HALF_MARATHON ? 18
                : goal.distance() == RaceDistance.TEN_K ? 11 : goal.distance() == RaceDistance.FIVE_K ? 7 : 12;
        String caution = race != null && distance > 10 && (!established || longBase < distance * .45) && weeks < 12
                ? " Your current history gives less preparation time than normally recommended for this distance. This plan does not accelerate mileage to catch up. Consider changing the date/distance or keeping a conservative completion focus."
                : "";
        LocalDate previousDemanding = null;
        for (LocalDate week = start.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); !week.isAfter(end); week = week.plusWeeks(1)) {
            LocalDate from = week.isBefore(start) ? start : week;
            LocalDate to = week.plusDays(6).isAfter(end) ? end : week.plusDays(6);
            int index = (int) ChronoUnit.WEEKS.between(start.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), week);
            TrainingPhase.Kind phase = phaseAt(phases, from);
            boolean taper = race != null && !to.isBefore(taperStart);
            boolean wantsQuality = qualitySupported && !taper && prefs.strategy() != TrainingStrategy.ZONE_2
                    && goal.distance() != RaceDistance.GENERAL_FITNESS
                    && (prefs.strategy() != TrainingStrategy.PERFORMANCE || performance)
                    && (prefs.strategy() != TrainingStrategy.DISTANCE_FIRST || phase == TrainingPhase.Kind.RACE_SPECIFIC || phase == TrainingPhase.Kind.PEAK);
            Set<LocalDate> blocked = new HashSet<>(request.unavailableDates());
            // Easy days remain available; quality is omitted if this week's winning placement
            // would sit directly after the previous week's demanding workout.
            var schedule = ConstraintScheduler.select(from, to, frequency, prefs, blocked, heavy, wantsQuality, race);
            LocalDate quality = schedule.quality();
            if (quality != null && previousDemanding != null && ChronoUnit.DAYS.between(previousDemanding, quality) < 2) quality = null;
            double step = Math.min(2.0, Math.max(.5, base / 30));
            double build = Math.min(volumeCap, base + Math.max(0, index - index / 4) * step);
            if (index % 4 == 3) build *= .8;
            double taperFactor = taper ? (ChronoUnit.DAYS.between(from, end) <= 7 ? .5 : .7) : 1;
            double weekly = build * taperFactor;
            double longKm = Math.min(longCap, Math.min(longBase + Math.max(0, index - index / 4) * .5, weekly * .48));
            if (index % 4 == 3) longKm = Math.min(longKm, longBase * .8 + index * .15);
            longKm *= taperFactor;
            int n = schedule.days().size();
            for (LocalDate day : schedule.days()) {
                boolean isLong = n >= 2 && day.equals(schedule.longRun()) && !taper;
                boolean isQuality = day.equals(quality);
                if (previousDemanding != null && ChronoUnit.DAYS.between(previousDemanding, day) < 2) { isLong = false; isQuality = false; }
                WorkoutCategory category = isLong ? WorkoutCategory.LONG_RUN : isQuality
                        ? phase == TrainingPhase.Kind.RACE_SPECIFIC || phase == TrainingPhase.Kind.PEAK ? WorkoutCategory.RACE_PACE_RUN : WorkoutCategory.TEMPO_THRESHOLD_RUN
                        : prefs.strategy() == TrainingStrategy.ZONE_2 || goal.distance() == RaceDistance.GENERAL_FITNESS ? WorkoutCategory.ZONE_2_RUN : WorkoutCategory.EASY_RUN;
                Double km = established ? rounded(n == 1 ? weekly : isLong ? longKm : (weekly - (n >= 2 && !taper ? longKm : 0)) / (n - (n >= 2 && !taper ? 1 : 0))) : null;
                Duration duration = established ? null : Duration.ofMinutes(Math.min(30, 15 + index / 2 * 2));
                List<WorkoutSegment> segments = new ArrayList<>();
                double rpe = isQuality ? 6 : 3;
                if (!established) {
                    segments.add(segment(WorkoutSegment.Kind.WARM_UP, Duration.ofMinutes(5), 2, "Comfortable walk; shorten or pause if needed."));
                    int minutes = (int) duration.toMinutes() - 10;
                    segments.add(segment(WorkoutSegment.Kind.RUN, Duration.ofMinutes(minutes), 3,
                        "Alternate comfortable running and walking as needed; conversational effort. Continuous running is optional."));
                    segments.add(segment(WorkoutSegment.Kind.COOL_DOWN, Duration.ofMinutes(5), 2, "Comfortable walk."));
                } else if (isQuality && km != null && km >= 4) {
                    segments.add(new WorkoutSegment(WorkoutSegment.Kind.WARM_UP, 1.0, null, null, null, null, 3.0, "Easy conversational warm-up."));
                    Double pace = category == WorkoutCategory.RACE_PACE_RUN && goal.targetTime() != null
                            ? goal.targetTime().toMillis() / 1000.0 / distance : null;
                    segments.add(new WorkoutSegment(WorkoutSegment.Kind.RUN, km - 2, null, pace == null ? null : pace * .98,
                        pace == null ? null : pace * 1.04, null, rpe,
                        "Controlled sustained work; effort takes precedence over target pace. Slow down or shorten if this becomes a maximal effort."));
                    segments.add(new WorkoutSegment(WorkoutSegment.Kind.COOL_DOWN, 1.0, null, null, null, null, 2.0, "Easy cool-down."));
                } else segments.add(new WorkoutSegment(WorkoutSegment.Kind.RUN, km, null, null, null, null, 3.0,
                    "Easy conversational effort; generally able to speak in full sentences. Walk breaks are welcome."));
                String reason = (isLong ? "Develop endurance for " + goal.distance().label() + ". Long-run starting evidence: " + rounded(longBase) + " km; progression is limited by the weekly budget. "
                    : isQuality ? "Develop controlled race-specific endurance alongside easy training; recent volume and consistency support one quality session this week. "
                    : "Build aerobic endurance while keeping effort controlled to leave room for recovery. ")
                    + (established ? "Starting weekly evidence: " + rounded(base) + " km. This week's budget: " + rounded(weekly) + " km. "
                        : "Current workload evidence is missing, low or stale, so this is a short run/walk session rather than a mileage prescription. ")
                    + "Phase: " + phase + ". " + (taper ? "Volume is reduced before race day; arriving recovered is the priority. " : "")
                    + (prefs.strategy() == TrainingStrategy.PERFORMANCE && !performance ? "Current evidence does not support extra Performance intensity; your saved preference is unchanged. " : "")
                    + schedule.explanation() + caution;
                workouts.add(new PlannedWorkout(id + ":" + day, day, category, km, duration, segments, reason));
                if (isLong || isQuality) previousDemanding = day;
            }
        }
        if (race != null) workouts.add(new PlannedWorkout(id + ":race", race, WorkoutCategory.RACE, distance, null,
            List.of(new WorkoutSegment(WorkoutSegment.Kind.RUN, distance, null, null, null, null, null,
                "Runner-owned race goal. Adjust effort to conditions and how you feel; completion is not guaranteed by a training plan.")),
            "Your chosen race anchors this programme. The target has not been changed." + caution));
        workouts.sort(Comparator.comparing(PlannedWorkout::date));
        // Race counts toward the maximum frequency in its calendar week. Remove the latest
        // optional training, never the runner-owned race, when the final week would exceed it.
        if (race != null) {
            LocalDate monday = race.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            while (workouts.stream().filter(w -> !w.date().isBefore(monday)).count() > prefs.runsPerWeek()) {
                for (int i = workouts.size() - 1; i >= 0; i--) if (workouts.get(i).category() != WorkoutCategory.RACE && !workouts.get(i).date().isBefore(monday)) { workouts.remove(i); break; }
            }
        }
        return new TrainingPlan(id, start, end, phases, workouts);
    }
    private static WorkoutSegment segment(WorkoutSegment.Kind kind, Duration duration, double rpe, String guidance) {
        return new WorkoutSegment(kind, null, duration, null, null, null, rpe, guidance);
    }
    private static double rounded(double value) { return Math.round(value * 10) / 10.0; }
    public static TrainingPhase.Kind phaseAt(List<TrainingPhase> phases, LocalDate date) {
        return phases.stream().filter(p -> !date.isBefore(p.startDate()) && !date.isAfter(p.endDate()))
            .map(TrainingPhase::kind).findFirst().orElse(TrainingPhase.Kind.FOUNDATION);
    }
    private static List<TrainingPhase> phases(LocalDate start, LocalDate end, LocalDate race, LocalDate taper, boolean established, int weeks) {
        List<TrainingPhase> result = new ArrayList<>(); TrainingPhase.Kind previous = null; LocalDate phaseStart = start;
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        for (LocalDate day = start; !day.isAfter(end.plusDays(1)); day = day.plusDays(1)) {
            long index = ChronoUnit.DAYS.between(start, day);
            TrainingPhase.Kind kind = day.isAfter(end) ? null : race != null && day.equals(race) ? TrainingPhase.Kind.RACE
                : race != null && !day.isBefore(taper) ? TrainingPhase.Kind.TAPER
                : index < (established ? weeks >= 20 ? days * .2 : 0 : days * .3) ? TrainingPhase.Kind.FOUNDATION
                : race == null || index < days * .6 ? TrainingPhase.Kind.ENDURANCE_DEVELOPMENT
                : index < days * .85 ? TrainingPhase.Kind.RACE_SPECIFIC : TrainingPhase.Kind.PEAK;
            if (kind != previous) {
                if (previous != null) result.add(new TrainingPhase(previous, phaseStart, day.minusDays(1),
                    "Allocated from the actual " + days + "-day horizon and " + (established ? "established" : "limited")
                    + " running evidence. Phase boundaries are projections, not fixed-length schedules."));
                phaseStart = day; previous = kind;
            }
        }
        return List.copyOf(result);
    }
}
