package org.racedaydreams.domain.plan;

import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.TrainingPhase;
import org.racedaydreams.domain.TrainingPlan;
import org.racedaydreams.domain.WorkoutSegment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Human-inspectable, deterministic whole-plan snapshots. No timestamps or random hashes added. */
public final class PlanSnapshot {
    private PlanSnapshot() { }

    public static String render(TrainingPlan plan) {
        Objects.requireNonNull(plan);
        StringBuilder out = new StringBuilder("plan-snapshot-version|1\n");
        row(out, "plan", plan.id(), plan.startDate(), plan.endDate());
        List<TrainingPhase> phases = new ArrayList<>(plan.phases());
        phases.sort(Comparator.comparing(TrainingPhase::startDate).thenComparing(TrainingPhase::endDate).thenComparing(TrainingPhase::kind));
        for (TrainingPhase phase : phases) row(out, "phase", phase.kind(), phase.startDate(), phase.endDate(), phase.explanation());
        List<PlannedWorkout> workouts = new ArrayList<>(plan.workouts());
        workouts.sort(Comparator.comparing(PlannedWorkout::date).thenComparing(PlannedWorkout::id));
        for (PlannedWorkout workout : workouts) {
            row(out, "workout", workout.id(), workout.date(), workout.category(), workout.distanceKm(), workout.duration(), workout.note());
            int index = 0;
            for (WorkoutSegment segment : workout.segments()) row(out, "segment", workout.id(), index++, segment.kind(), segment.distanceKm(), segment.duration(), segment.minimumSecondsPerKm(), segment.maximumSecondsPerKm(), segment.heartRateZone(), segment.rpe(), segment.guidance());
        }
        return out.toString();
    }

    /** Small line-numbered diff suitable for test failures and pull-request review. */
    public static String difference(String expected, String actual) {
        if (expected.equals(actual)) return "";
        String[] before = expected.split("\n", -1);
        String[] after = actual.split("\n", -1);
        StringBuilder out = new StringBuilder();
        int changes = 0;
        for (int index = 0; index < Math.max(before.length, after.length); index++) {
            String left = index < before.length ? before[index] : null;
            String right = index < after.length ? after[index] : null;
            if (Objects.equals(left, right)) continue;
            if (++changes > 60) { out.append("... additional changed lines omitted\n"); break; }
            if (left != null) out.append('-').append(index + 1).append(' ').append(left).append('\n');
            if (right != null) out.append('+').append(index + 1).append(' ').append(right).append('\n');
        }
        return out.toString();
    }

    private static void row(StringBuilder out, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) out.append('|');
            out.append(escape(values[index]));
        }
        out.append('\n');
    }
    private static String escape(Object value) {
        return value == null ? "~" : value.toString().replace("\\", "\\\\").replace("|", "\\|").replace("~", "\\~").replace("\r", "\\r").replace("\n", "\\n");
    }
}
