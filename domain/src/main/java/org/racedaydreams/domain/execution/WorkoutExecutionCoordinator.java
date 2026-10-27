package org.racedaydreams.domain.execution;

import org.racedaydreams.domain.recording.RunMetricsEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Session-local orchestration over the one workout executor. The recorder supplies accepted
 * metrics; this class never owns a clock, sensors, persistence or a plan.
 */
public final class WorkoutExecutionCoordinator {
    public record Update(
            WorkoutExecutionEngine.State state, List<WorkoutExecutionEngine.Event> newEvents) {
        public Update {
            newEvents = Collections.unmodifiableList(new ArrayList<>(newEvents));
        }
    }

    private final WorkoutExecutionEngine execution;
    private int publishedEvents;

    public WorkoutExecutionCoordinator(List<WorkoutExecutionEngine.Step> steps) {
        execution = new WorkoutExecutionEngine(steps);
    }

    /** Returns each execution event once, while retaining the complete snapshot. */
    public Update update(RunMetricsEngine.Status metrics) {
        var input =
                new WorkoutExecutionEngine.Input(
                        metrics.metres(),
                        metrics.movingMillis(),
                        metrics.elapsedMillis(),
                        metrics.secondsPerKm(),
                        metrics.hasReliablePace(),
                        metrics.paused());
        var state = execution.update(input);
        var events = execution.events();
        var fresh = new ArrayList<>(events.subList(publishedEvents, events.size()));
        publishedEvents = events.size();
        return new Update(state, fresh);
    }

    public void completeOpen() {
        execution.completeOpen();
    }

    public void skip() {
        execution.skip();
    }

    public void finishEarly() {
        execution.finishEarly();
    }

    public List<WorkoutExecutionEngine.Event> events() {
        return execution.events();
    }

    public List<WorkoutExecutionEngine.Step> steps() {
        return execution.steps();
    }
}
