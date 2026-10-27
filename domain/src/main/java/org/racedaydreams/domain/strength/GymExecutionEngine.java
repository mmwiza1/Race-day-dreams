package org.racedaydreams.domain.strength;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.GymExecution.Event;
import org.racedaydreams.domain.strength.GymExecution.EventType;
import org.racedaydreams.domain.strength.GymExecution.Status;
import org.racedaydreams.domain.strength.GymExecution.Tick;

/**
 * One deterministic gym execution path for all workout origins. The caller persists returned
 * snapshots; this engine performs no IO, starts no clock and never pre-fills an actual set.
 */
public final class GymExecutionEngine {
    private GymExecution state;

    private GymExecutionEngine(GymExecution state) { this.state = state; }

    public static GymExecutionEngine start(String id, GymExecution.Origin origin,
            GymWorkoutDefinition prescription, GymExecution.SourceReference source,
            ZoneOffset offset, Tick at) {
        Objects.requireNonNull(at, "start tick");
        var slots = prescription == null ? Immutable.<GymWorkoutDefinition.Slot>list() : prescription.slots();
        var first = slots.isEmpty() ? null : slots.get(0).id();
        var event = new Event(0, EventType.WORKOUT_STARTED, first, null, 0, 0, at.wallTime());
        return new GymExecutionEngine(new GymExecution(id, origin, prescription, source, at.wallTime(),
                offset, Status.ACTIVE, at, 0, 0, first, slots, Immutable.list(), Immutable.list(event), null, false));
    }

    /** A process interruption is an explicit gap. No old/new clock difference is added as exercise. */
    public static GymExecutionEngine recover(GymExecution saved, Tick at) {
        Objects.requireNonNull(saved, "saved execution");
        Objects.requireNonNull(at, "recovery tick");
        if (saved.status() == Status.FINISHED || saved.status() == Status.DISCARDED) {
            throw new IllegalArgumentException("Only unfinished gym execution can be recovered.");
        }
        var engine = new GymExecutionEngine(saved);
        Status recovered = saved.status() == Status.PAUSED ? Status.PAUSED : Status.RECOVERY_REQUIRED;
        engine.state = engine.withEvent(recovered, new Timing(at, saved.recordedElapsedMillis(), saved.activeMillis()),
                saved.selectedSlotId(), saved.slots(), saved.actualSets(), EventType.RECOVERED_WITH_GAP,
                saved.selectedSlotId(), null, null, false);
        return engine;
    }

    public GymExecution snapshot() { return state; }

    public GymExecution observe(Tick at) {
        requireUnfinished();
        Timing timing = timing(at);
        state = snapshot(state.status(), timing, state.selectedSlotId(), state.slots(), state.actualSets(),
                state.events(), null, false);
        return state;
    }

    public GymExecution pause(Tick at) {
        requireActive();
        state = withEvent(Status.PAUSED, timing(at), state.selectedSlotId(), state.slots(), state.actualSets(),
                EventType.PAUSED, state.selectedSlotId(), null, null, false);
        return state;
    }

    public GymExecution resume(Tick at) {
        if (state.status() != Status.PAUSED && state.status() != Status.RECOVERY_REQUIRED) {
            throw new IllegalArgumentException("Only a paused or explicitly recovered workout can resume.");
        }
        state = withEvent(Status.ACTIVE, timing(at), state.selectedSlotId(), state.slots(), state.actualSets(),
                EventType.RESUMED, state.selectedSlotId(), null, null, false);
        return state;
    }

    public GymExecution selectExercise(String slotId, Tick at) {
        requireActive();
        slot(slotId);
        if (slotId.equals(state.selectedSlotId())) return state;
        state = withEvent(state.status(), timing(at), slotId, state.slots(), state.actualSets(),
                EventType.EXERCISE_SELECTED, slotId, null, null, false);
        return state;
    }

    public GymExecution addExercises(List<GymWorkoutDefinition.Slot> added, Tick at) {
        requireActive();
        if (added == null || added.isEmpty()) throw new IllegalArgumentException("Choose an exercise to add.");
        var slots = new ArrayList<>(state.slots());
        slots.addAll(added);
        GymWorkoutDefinition.validateSlots(slots, false);
        String selected = state.selectedSlotId() == null ? slots.get(0).id() : state.selectedSlotId();
        state = withEvent(state.status(), timing(at), selected, slots, state.actualSets(),
                EventType.EXERCISES_ADDED, added.get(0).id(), null, null, false);
        return state;
    }

    /** Today-only replacement. The original prescription and already entered actual sets survive. */
    public GymExecution replaceExercise(String slotId, StrengthExercise replacement, Tick at) {
        requireActive();
        var original = slot(slotId);
        Objects.requireNonNull(replacement, "replacement exercise");
        var target = original.target();
        if (target != null) {
            target = original.exercise().measurement() == replacement.measurement()
                    ? target.withoutLoadReference() : null;
            if (target != null && replacement.mode() == StrengthExercise.Mode.BODYWEIGHT) {
                target = target.withoutLoadReference();
            }
        }
        var changed = new GymWorkoutDefinition.Slot(slotId, replacement, target, original.supersetGroup());
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        for (var existing : state.slots()) slots.add(existing.id().equals(slotId) ? changed : existing);
        state = withEvent(state.status(), timing(at), state.selectedSlotId(), slots, state.actualSets(),
                EventType.EXERCISE_REPLACED, slotId, null, null, false);
        return state;
    }

    public GymExecution saveSet(String slotId, StrengthSession.SetEntry actual, Tick at) {
        return saveSet(new GymExecutionSet(slotId, slot(slotId).exercise(), actual), at);
    }

    /** Same stable ID and identical values is a retry; conflicting reuse never overwrites evidence. */
    public GymExecution saveSet(GymExecutionSet row, Tick at) {
        Objects.requireNonNull(row, "actual set");
        for (var saved : state.actualSets()) {
            if (saved.actual().id().equals(row.actual().id())) {
                if (saved.equals(row)) return state;
                throw new IllegalArgumentException("An actual set ID cannot be reused with different evidence.");
            }
        }
        requireActive();
        if (!slot(row.slotId()).exercise().equals(row.exercise())) {
            throw new IllegalArgumentException("Actual set must retain the exercise currently being performed.");
        }
        var sets = new ArrayList<>(state.actualSets());
        sets.add(row);
        GymExecution.actualBlocks(sets);
        state = withEvent(state.status(), timing(at), state.selectedSlotId(), state.slots(), sets,
                EventType.SET_SAVED, row.slotId(), row.actual().id(), null, false);
        return state;
    }

    public int workingSets(String slotId) {
        var selected = slot(slotId);
        int count = 0;
        for (var set : state.actualSets()) {
            if (set.slotId().equals(slotId) && set.exercise().equals(selected.exercise()) && !set.actual().warmUp()) count++;
        }
        return count;
    }

    /** Finalise only entered evidence; targets, load references and past sets are never actuals. */
    public GymExecution finish(Tick at, Double sessionRpe, String notes, boolean finishEarly) {
        if (state.status() == Status.FINISHED) {
            if (Objects.equals(state.completedSession().rpe(), sessionRpe)
                    && Objects.equals(state.completedSession().notes(), notes == null ? "" : notes)) return state;
            throw new IllegalArgumentException("Finished workout evidence cannot be overwritten by a retry.");
        }
        requireUnfinished();
        if (state.actualSets().isEmpty()) throw new IllegalArgumentException("Record an actual set or discard this unfinished workout.");
        Timing timing = timing(at);
        boolean partial = finishEarly || GymExecution.hasUnfinishedTargets(state.prescription(), state.slots(), state.actualSets());
        var session = new StrengthSession(state.id(), state.startedAt(), state.offset(),
                Duration.ofMillis(timing.activeMillis()), sessionRpe, notes, GymExecution.actualBlocks(state.actualSets()));
        state = withEvent(Status.FINISHED, timing, state.selectedSlotId(), state.slots(), state.actualSets(),
                partial ? EventType.WORKOUT_FINISHED_EARLY : EventType.WORKOUT_COMPLETED,
                state.selectedSlotId(), null, session, partial);
        return state;
    }

    public GymExecution discard(Tick at) {
        requireUnfinished();
        state = withEvent(Status.DISCARDED, timing(at), state.selectedSlotId(), state.slots(), state.actualSets(),
                EventType.WORKOUT_DISCARDED, state.selectedSlotId(), null, null, false);
        return state;
    }

    private GymWorkoutDefinition.Slot slot(String slotId) {
        for (var slot : state.slots()) if (slot.id().equals(slotId)) return slot;
        throw new IllegalArgumentException("Unknown exercise slot.");
    }

    private void requireActive() {
        if (state.status() != Status.ACTIVE) throw new IllegalArgumentException("Resume the workout before entering or changing exercise evidence.");
    }

    private void requireUnfinished() {
        if (state.status() == Status.FINISHED || state.status() == Status.DISCARDED) {
            throw new IllegalArgumentException("This workout has already ended.");
        }
    }

    private record Timing(Tick at, long elapsedMillis, long activeMillis) {}

    private Timing timing(Tick at) {
        Objects.requireNonNull(at, "clock observation");
        if (!at.continuityId().equals(state.lastTick().continuityId())
                || at.monotonicMillis() < state.lastTick().monotonicMillis()) {
            throw new IllegalArgumentException("Clock continuity changed. Recover the unfinished workout explicitly.");
        }
        long delta = state.status() == Status.RECOVERY_REQUIRED ? 0
                : at.monotonicMillis() - state.lastTick().monotonicMillis();
        long elapsed = Math.addExact(state.recordedElapsedMillis(), delta);
        long active = Math.addExact(state.activeMillis(), state.status() == Status.ACTIVE ? delta : 0);
        return new Timing(at, elapsed, active);
    }

    private GymExecution withEvent(Status status, Timing timing, String selected,
            List<GymWorkoutDefinition.Slot> slots, List<GymExecutionSet> sets,
            EventType type, String eventSlot, String actualId, StrengthSession completed, boolean partial) {
        var events = new ArrayList<>(state.events());
        events.add(new Event(events.size(), type, eventSlot, actualId,
                timing.elapsedMillis(), timing.activeMillis(), timing.at().wallTime()));
        return snapshot(status, timing, selected, slots, sets, events, completed, partial);
    }

    private GymExecution snapshot(Status status, Timing timing, String selected,
            List<GymWorkoutDefinition.Slot> slots, List<GymExecutionSet> sets,
            List<Event> events, StrengthSession completed, boolean partial) {
        return new GymExecution(state.id(), state.origin(), state.prescription(), state.source(),
                state.startedAt(), state.offset(), status, timing.at(), timing.elapsedMillis(),
                timing.activeMillis(), selected, slots, sets, events, completed, partial);
    }
}
