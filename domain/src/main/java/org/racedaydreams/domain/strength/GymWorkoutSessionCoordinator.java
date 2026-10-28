package org.racedaydreams.domain.strength;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * Serialized application operations around the one gym execution engine and local durability. Call
 * its methods on an IO executor, never from UI recomposition: the monitor deliberately covers the
 * complete repository operation. UI renders the published immutable View. Reuse this coordinator
 * across UI recreation; creating a new one does not resume an old clock or recover a workout
 * without the athlete's action.
 */
public final class GymWorkoutSessionCoordinator {
    @FunctionalInterface
    public interface TickSource {
        GymExecution.Tick now();
    }

    public enum Operation {
        START,
        SAVE,
        FINISH
    }

    /** Exact immutable retry input, including the clock observation taken before attempted IO. */
    public record Pending(Operation operation, GymExecution snapshot, long expectedRevision) {
        public Pending {
            Objects.requireNonNull(operation, "pending operation");
            Objects.requireNonNull(snapshot, "pending snapshot");
            if (operation == Operation.START
                    ? expectedRevision != 0
                    : expectedRevision < 1 || expectedRevision == Long.MAX_VALUE)
                throw new IllegalArgumentException("Invalid pending gym revision.");
        }
    }

    /**
     * Accepted is the last acknowledged durable state. Observed may advance only its display clock,
     * never actual sets. Pending evidence is explicitly unacknowledged, even after a lost ack; it
     * must not be presented as a completed workout. While pending, observed stays at accepted.
     */
    public record View(
            GymExecutionRepository.Stored accepted, GymExecution observed, Pending pending) {}

    private final GymExecutionRepository repository;
    private final TickSource ticks;
    private GymExecutionEngine engine;
    private GymExecutionRepository.Stored accepted;
    private Pending pending;

    public GymWorkoutSessionCoordinator(GymExecutionRepository repository, TickSource ticks) {
        this.repository = Objects.requireNonNull(repository, "gym repository");
        this.ticks = Objects.requireNonNull(ticks, "gym clock");
    }

    public synchronized View start(
            String id,
            GymExecution.Origin origin,
            GymWorkoutDefinition prescription,
            GymExecution.SourceReference source,
            ZoneOffset offset) {
        requireNoPending();
        if (engine != null && unfinished(engine.snapshot()))
            throw new IllegalStateException("Finish or discard the current gym workout first.");
        engine = GymExecutionEngine.start(id, origin, prescription, source, offset, ticks.now());
        // A different session must not display the previous workout as its accepted evidence.
        accepted = null;
        return persist(Operation.START, engine.snapshot(), 0);
    }

    /** Read the staged evidence only after an explicit recovery choice; never bridge the gap. */
    public synchronized View recoverExplicitly(String id) {
        requireNoPending();
        if (engine != null && unfinished(engine.snapshot()) && !engine.snapshot().id().equals(id))
            throw new IllegalStateException("Finish or discard the current gym workout first.");
        var stored = repository.load(id);
        if (stored == null)
            throw new IllegalArgumentException("The unfinished gym workout is missing.");
        var recovered = GymExecutionEngine.recover(stored.execution(), ticks.now());
        accepted = stored;
        engine = recovered;
        return persist(Operation.SAVE, engine.snapshot(), stored.revision());
    }

    public synchronized View saveSet(String slotId, StrengthSession.SetEntry actual) {
        requireReady();
        return persist(
                Operation.SAVE, engine.saveSet(slotId, actual, ticks.now()), accepted.revision());
    }

    public synchronized View selectExercise(String slotId) {
        requireReady();
        return persist(
                Operation.SAVE, engine.selectExercise(slotId, ticks.now()), accepted.revision());
    }

    public synchronized View addExercises(List<GymWorkoutDefinition.Slot> slots) {
        requireReady();
        return persist(
                Operation.SAVE, engine.addExercises(slots, ticks.now()), accepted.revision());
    }

    public synchronized View replaceExercise(String slotId, StrengthExercise replacement) {
        requireReady();
        return persist(
                Operation.SAVE,
                engine.replaceExercise(slotId, replacement, ticks.now()),
                accepted.revision());
    }

    public synchronized View pause() {
        requireReady();
        return persist(Operation.SAVE, engine.pause(ticks.now()), accepted.revision());
    }

    public synchronized View resume() {
        requireReady();
        return persist(Operation.SAVE, engine.resume(ticks.now()), accepted.revision());
    }

    public synchronized View finish(Double sessionRpe, String notes, boolean finishEarly) {
        requireReady();
        return persist(
                Operation.FINISH,
                engine.finish(ticks.now(), sessionRpe, notes, finishEarly),
                accepted.revision());
    }

    /**
     * Discard is a persisted explicit decision. A failed pending write must first be retried: it
     * might already have committed, so dropping it locally would risk losing actual evidence.
     */
    public synchronized View discard() {
        requireReady();
        return persist(Operation.SAVE, engine.discard(ticks.now()), accepted.revision());
    }

    /** Optional user/lifecycle checkpoint, not a disk write for every display frame. */
    public synchronized View checkpoint() {
        requireReady();
        return persist(Operation.SAVE, engine.observe(ticks.now()), accepted.revision());
    }

    /** No repository IO. A new process must use explicit recovery before it has a live engine. */
    public synchronized View observe() {
        if (pending == null && engine != null && unfinished(engine.snapshot()))
            engine.observe(ticks.now());
        return view();
    }

    /** No IO or clock observation; safe to inspect an error without changing its retry input. */
    public synchronized View view() {
        GymExecution observed =
                pending != null
                        ? accepted == null ? null : accepted.execution()
                        : engine == null ? null : engine.snapshot();
        return new View(accepted, observed, pending);
    }

    /**
     * Retry the exact operation; do not reread the clock, mutate actuals or append another event.
     */
    public synchronized View retryPending() {
        if (pending == null)
            throw new IllegalStateException("There is no pending gym write to retry.");
        var attempt = pending;
        GymExecutionRepository.Stored stored;
        switch (attempt.operation()) {
            case START:
                stored = repository.start(attempt.snapshot());
                break;
            case SAVE:
                stored = repository.save(attempt.snapshot(), attempt.expectedRevision());
                break;
            case FINISH:
                stored = repository.finish(attempt.snapshot(), attempt.expectedRevision());
                break;
            default:
                throw new IllegalStateException("Unknown gym operation.");
        }
        boolean validRevision =
                stored != null
                        && (attempt.operation() == Operation.START
                                ? stored.revision() == 1
                                : stored.revision() == attempt.expectedRevision()
                                        || stored.revision() == attempt.expectedRevision() + 1);
        if (!validRevision || !stored.execution().equals(attempt.snapshot()))
            throw new IllegalStateException(
                    "The gym write acknowledgement does not match its evidence.");
        accepted = stored;
        pending = null;
        return view();
    }

    private View persist(Operation operation, GymExecution snapshot, long expectedRevision) {
        pending = new Pending(operation, snapshot, expectedRevision);
        return retryPending();
    }

    private void requireNoPending() {
        if (pending != null)
            throw new IllegalStateException("Retry the pending gym write before another action.");
    }

    private void requireReady() {
        requireNoPending();
        if (engine == null || accepted == null)
            throw new IllegalStateException("Start or explicitly recover a gym workout first.");
        if (accepted.revision() == Long.MAX_VALUE)
            throw new IllegalStateException("The saved gym revision cannot advance further.");
    }

    private static boolean unfinished(GymExecution execution) {
        return execution.status() != GymExecution.Status.FINISHED
                && execution.status() != GymExecution.Status.DISCARDED;
    }
}
