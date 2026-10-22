package org.racedaydreams.domain.coaching;

import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;

/** Product heuristics for execution, not fitness, health, effort or a scientifically validated score. */
public final class RunScoreEngine {
    public static final int RULE_VERSION = 1;
    private RunScoreEngine() { }
    public static RunFeedback analyse(PlannedWorkout planned, ActivityRecord actual, RunExecution response) {
        if (planned == null) return new RunFeedback(null, "Run Analysis", org.racedaydreams.domain.coaching.Immutable.list(), org.racedaydreams.domain.coaching.Immutable.list(), org.racedaydreams.domain.coaching.Immutable.list(),
            "There was no planned workout to compare this run against. Recorded distance, duration, RPE and splits remain useful evidence.",
            "One completed run does not establish a fitness change.");
        if (response != null && !Objects.equals(response.plannedWorkoutId(), planned.id()))
            throw new IllegalArgumentException("The response must refer to this prescription.");
        List<RunFeedback.Component> components = new ArrayList<>(); List<String> good = new ArrayList<>(), differed = new ArrayList<>();
        Double ratio = planned.distanceKm() != null && planned.distanceKm() > 0 && actual.distanceMetres() != null
            ? actual.distanceMetres() / (planned.distanceKm() * 1000)
            : planned.duration() != null && !planned.duration().isZero()
                ? actual.elapsedTime().toMillis() / (double) planned.duration().toMillis() : null;
        Double completion = ratio == null ? null : Math.max(0, Math.min(1, ratio));
        components.add(new RunFeedback.Component("Completion", completion,
            ratio == null ? "Comparable distance or duration is unavailable." : "Completed " + Math.round(ratio * 100) + "% of the prescription; extra distance earns no extra credit."));
        if (ratio != null) { if (ratio >= .95 && ratio <= 1.15) good.add("You completed approximately the prescribed amount.");
            else differed.add(ratio < .95 ? "You completed less than prescribed. That is useful evidence, not a failure." : "This run exceeded its planned amount; more is not automatically better."); }
        boolean controlled = switch (planned.category()) {
            case RECOVERY_RUN, EASY_RUN, ZONE_2_RUN, LONG_RUN -> true;
            default -> false;
        };
        double upper = planned.category() == WorkoutCategory.RECOVERY_RUN ? 3 : controlled ? 4 : 8;
        double lower = controlled ? 0 : 5;
        Double effort = actual.rpe() == null ? null : Math.max(0, 1 - Math.max(0, actual.rpe() - upper) * .2 - Math.max(0, lower - actual.rpe()) * .12);
        components.add(new RunFeedback.Component("Effort match", effort, actual.rpe() == null ? "RPE was not recorded. Heart rate is not required."
            : "Recorded RPE " + actual.rpe() + "; intended " + (controlled ? "controlled conversational effort" : "controlled quality work") + ". RPE and Run Score measure different things."));
        if (effort != null) { if (effort >= .9) good.add("Your reported effort matched the session purpose.");
            else differed.add("Your reported effort differed from the intended intensity."); }
        Double paceMatch = null;
        // Only whole-run prescriptions can be compared with whole-run average pace. An interval
        // average includes recoveries, so it cannot establish rep pace execution.
        if (planned.segments().size() == 1 && actual.distanceMetres() != null && actual.distanceMetres() > 0) {
            var segment = planned.segments().get(0);
            if (segment.minimumSecondsPerKm() != null && segment.maximumSecondsPerKm() != null) {
                double pace = actual.elapsedTime().toMillis() / actual.distanceMetres();
                double outside = Math.max(0, Math.max(segment.minimumSecondsPerKm() - pace, pace - segment.maximumSecondsPerKm()));
                paceMatch = Math.max(0, 1 - outside / Math.max(1, segment.maximumSecondsPerKm()) * 4);
            }
        }
        components.add(new RunFeedback.Component("Target pace", paceMatch, paceMatch == null
            ? "No reliable whole-run pace comparison is available; warm-ups and recoveries are not treated as quality reps."
            : "Compared with the prescribed range, without rewarding extra speed."));
        Double stability = null;
        var splits = SplitCalculator.calculate(actual, 1000);
        if (splits.splits().size() >= 3 && planned.category() != WorkoutCategory.INTERVALS) {
            var full = splits.splits().stream().filter(s -> !s.partial()).collect(java.util.stream.Collectors.toList());
            if (full.size() >= 3) {
                double first = full.get(0).duration().toMillis() / full.get(0).distanceMetres();
                double last = full.get(full.size()-1).duration().toMillis() / full.get(full.size()-1).distanceMetres();
                double fade = (last - first) / first;
                stability = planned.category() == WorkoutCategory.PROGRESSION_RUN
                    ? last < first * .97 ? 1.0 : .4 : Math.max(0, 1 - Math.max(0, fade - .08) * 3);
            }
        }
        components.add(new RunFeedback.Component("Pacing / objective", stability, stability == null
            ? "Reliable continuous splits are unavailable; pacing stability is not inferred."
            : planned.category() == WorkoutCategory.PROGRESSION_RUN ? "Compared early and late complete splits for a genuine progression."
            : "Compared early and late complete splits. Terrain, conditions and context can affect this heuristic."));
        Double reps = null;
        if (planned.category() == WorkoutCategory.INTERVALS && response != null && response.completedReps() != null) {
            long prescribed = planned.segments().stream().filter(s -> s.kind() == WorkoutSegment.Kind.RUN).count();
            if (prescribed > 0) reps = Math.min(1, response.completedReps() / (double) prescribed);
        }
        if (planned.category() == WorkoutCategory.INTERVALS) {
            components.add(new RunFeedback.Component("Reps completed", reps, reps == null ? "Rep completion was not recorded; total distance does not prove reps were completed."
                : "Uses runner-reported completed reps against the prescribed main sets."));
            if (response != null && response.recoveriesCompleted() != null) components.add(new RunFeedback.Component("Recoveries", response.recoveriesCompleted() ? 1.0 : .4,
                "Uses runner-reported recovery completion."));
        }
        double sum = 0, weight = 0; int known = 0;
        for (var c : components) if (c.fraction() != null) {
            double w = c.name().equals("Completion") ? 3 : c.name().equals("Effort match") ? 4 : 2;
            sum += c.fraction() * w; weight += w; known++;
        }
        // Completion alone cannot demonstrate correct execution. Do not manufacture a score.
        Integer score = known < 2 ? null : Math.max(1, Math.min(10, (int) Math.round(sum / weight * 10)));
        return new RunFeedback(score, score == null ? "Execution evidence incomplete" : score >= 8 ? "Good execution" : "Against today's objective",
            components, good, differed,
            "This records how the run met its purpose. Completion alone does not prove fitness improved. Missing components are excluded, not scored as zero.",
            differed.isEmpty() ? "No change is needed from this run alone. Keep the next session aligned with its purpose."
                : "Review the raw evidence and the rest of your week. Any meaningful plan adjustment remains your choice.");
    }
}
