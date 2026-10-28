package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * A persistable execution snapshot. Unfinished actuals are not completed StrengthSession evidence.
 */
public record GymExecution(
        String id,
        Origin origin,
        GymWorkoutDefinition prescription,
        SourceReference source,
        Instant startedAt,
        ZoneOffset offset,
        Status status,
        Tick lastTick,
        long recordedElapsedMillis,
        long activeMillis,
        String selectedSlotId,
        List<GymWorkoutDefinition.Slot> slots,
        List<GymExecutionSet> actualSets,
        List<Event> events,
        StrengthSession completedSession,
        boolean partial) {
    public static final int FORMAT_VERSION = 1;

    public enum Origin {
        PLANNED,
        QUICK,
        CUSTOM,
        REPEATED,
        FAVOURITE,
        GENERATED,
        FREE
    }

    public enum Status {
        ACTIVE,
        PAUSED,
        RECOVERY_REQUIRED,
        FINISHED,
        DISCARDED
    }

    public enum SourceKind {
        PROGRAMME,
        SAVED_WORKOUT,
        ACTUAL_SESSION
    }

    public enum EventType {
        WORKOUT_STARTED,
        EXERCISE_SELECTED,
        EXERCISES_ADDED,
        EXERCISE_REPLACED,
        SET_SAVED,
        PAUSED,
        RESUMED,
        RECOVERED_WITH_GAP,
        WORKOUT_COMPLETED,
        WORKOUT_FINISHED_EARLY,
        WORKOUT_DISCARDED
    }

    /** Preserved provenance, never an instruction to auto-link a spontaneous workout. */
    public record SourceReference(SourceKind kind, String id, int revision, String sessionId) {
        public SourceReference {
            Objects.requireNonNull(kind, "source kind");
            GymWorkoutDefinition.requireId(id, "source");
            if (kind == SourceKind.PROGRAMME) {
                if (revision < 1)
                    throw new IllegalArgumentException("Preserve the programme revision.");
                GymWorkoutDefinition.requireId(sessionId, "prescribed session");
            } else if (sessionId != null
                    || kind == SourceKind.SAVED_WORKOUT && revision < 1
                    || kind == SourceKind.ACTUAL_SESSION && revision != 0) {
                throw new IllegalArgumentException("Invalid frozen workout source.");
            }
        }
    }

    /** Continuity ID identifies the supplied monotonic clock epoch, never a user/account ID. */
    public record Tick(String continuityId, long monotonicMillis, Instant wallTime) {
        public Tick {
            GymWorkoutDefinition.requireId(continuityId, "clock continuity");
            if (monotonicMillis < 0)
                throw new IllegalArgumentException("Monotonic time must be non-negative.");
            Objects.requireNonNull(wallTime, "wall time");
        }
    }

    public record Event(
            int sequence,
            EventType type,
            String slotId,
            String actualSetId,
            long recordedElapsedMillis,
            long activeMillis,
            Instant observedAt) {
        public Event {
            if (sequence < 0
                    || recordedElapsedMillis < 0
                    || activeMillis < 0
                    || activeMillis > recordedElapsedMillis)
                throw new IllegalArgumentException("Invalid gym event timing.");
            Objects.requireNonNull(type, "event type");
            Objects.requireNonNull(observedAt, "event observation time");
            if (slotId != null) GymWorkoutDefinition.requireId(slotId, "exercise slot");
            if (actualSetId != null) GymWorkoutDefinition.requireId(actualSetId, "actual set");
            if ((type == EventType.SET_SAVED) != (actualSetId != null)
                    || type == EventType.SET_SAVED && slotId == null) {
                throw new IllegalArgumentException(
                        "Set events require exact actual/slot identities.");
            }
            if ((type == EventType.EXERCISE_SELECTED
                            || type == EventType.EXERCISE_REPLACED
                            || type == EventType.EXERCISES_ADDED)
                    && slotId == null) {
                throw new IllegalArgumentException(
                        "Exercise events require the affected slot identity.");
            }
        }
    }

    public GymExecution {
        GymWorkoutDefinition.requireId(id, "execution");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(startedAt, "started at");
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(lastTick, "last tick");
        if (prescription == null && origin != Origin.FREE)
            throw new IllegalArgumentException("Preserve the workout prescription.");
        if (origin == Origin.PLANNED && (source == null || source.kind() != SourceKind.PROGRAMME)) {
            throw new IllegalArgumentException(
                    "Planned execution requires the exact preserved programme session.");
        }
        if (origin == Origin.FAVOURITE
                        && (source == null || source.kind() != SourceKind.SAVED_WORKOUT)
                || origin == Origin.REPEATED && source == null) {
            throw new IllegalArgumentException(
                    "Preserve the source of a favourite or repeated workout.");
        }
        if (source != null
                && source.kind() == SourceKind.SAVED_WORKOUT
                && (prescription == null
                        || !prescription.id().equals(source.id())
                        || prescription.revision() != source.revision())) {
            throw new IllegalArgumentException(
                    "Saved workout source must match the preserved revision.");
        }
        if (source != null
                && source.kind() == SourceKind.PROGRAMME
                && (prescription == null
                        || prescription.revision() != source.revision()
                        || !prescription
                                .id()
                                .equals(GymWorkoutAdapter.programmeWorkoutId(source)))) {
            throw new IllegalArgumentException(
                    "Programme source must match the preserved workout identity and revision.");
        }
        if (recordedElapsedMillis < 0
                || recordedElapsedMillis > 7L * 24 * 60 * 60 * 1000
                || activeMillis < 0
                || activeMillis > recordedElapsedMillis
                || activeMillis > 12L * 60 * 60 * 1000) {
            throw new IllegalArgumentException("Invalid recorded gym timing.");
        }
        slots = Immutable.copyList(slots);
        GymWorkoutDefinition.validateSlots(slots, origin == Origin.FREE);
        var ids = new HashSet<String>();
        for (var slot : slots) ids.add(slot.id());
        if (selectedSlotId != null && !ids.contains(selectedSlotId)
                || selectedSlotId == null && !slots.isEmpty())
            throw new IllegalArgumentException("Invalid selected exercise slot.");
        actualSets = Immutable.copyList(actualSets);
        if (actualSets.size() > 10000)
            throw new IllegalArgumentException("Too many actual gym sets.");
        var setIds = new HashSet<String>();
        var actualById = new LinkedHashMap<String, GymExecutionSet>();
        for (GymExecutionSet set : actualSets) {
            if (!ids.contains(set.slotId()) || !setIds.add(set.actual().id()))
                throw new IllegalArgumentException("Invalid or duplicate actual set identity.");
            actualById.put(set.actual().id(), set);
        }
        actualBlocks(actualSets);
        events = Immutable.copyList(events);
        if (events.isEmpty() || events.size() > 50000)
            throw new IllegalArgumentException("Invalid gym event history.");
        long elapsed = 0, active = 0;
        var savedEvents = new HashSet<String>();
        Status replayed = Status.ACTIVE;
        int actualIndex = 0;
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            if (event.sequence() != i
                    || event.recordedElapsedMillis() < elapsed
                    || event.activeMillis() < active
                    || event.recordedElapsedMillis() > recordedElapsedMillis
                    || event.activeMillis() > activeMillis
                    || event.slotId() != null && !ids.contains(event.slotId())
                    || event.actualSetId() != null && !setIds.contains(event.actualSetId())) {
                throw new IllegalArgumentException("Corrupt gym execution events.");
            }
            elapsed = event.recordedElapsedMillis();
            active = event.activeMillis();
            if (i == 0) {
                if (event.type() != EventType.WORKOUT_STARTED || elapsed != 0 || active != 0) {
                    throw new IllegalArgumentException(
                            "Workout must start once at zero recorded duration.");
                }
            } else {
                replayed = afterEvent(replayed, event.type());
            }
            if (event.type() == EventType.SET_SAVED && !savedEvents.add(event.actualSetId())) {
                throw new IllegalArgumentException("An actual set must have one save event.");
            }
            if (event.type() == EventType.SET_SAVED) {
                var recorded = actualById.get(event.actualSetId());
                if (recorded == null
                        || !recorded.slotId().equals(event.slotId())
                        || !actualSets
                                .get(actualIndex++)
                                .actual()
                                .id()
                                .equals(event.actualSetId())) {
                    throw new IllegalArgumentException(
                            "Actual set event and recorded exercise slot disagree.");
                }
            }
        }
        if (!savedEvents.equals(setIds))
            throw new IllegalArgumentException("Actual set events are incomplete.");
        if (events.get(0).type() != EventType.WORKOUT_STARTED)
            throw new IllegalArgumentException("Missing workout start event.");
        if (status != replayed)
            throw new IllegalArgumentException("Workout status disagrees with event transitions.");
        if ((status == Status.FINISHED) != (completedSession != null))
            throw new IllegalArgumentException("Completed gym evidence is missing or premature.");
        if (completedSession != null
                && (!completedSession.id().equals(id)
                        || !completedSession.startedAt().equals(startedAt)
                        || !completedSession.offset().equals(offset)
                        || !completedSession.duration().equals(Duration.ofMillis(activeMillis)))) {
            throw new IllegalArgumentException("Completed evidence does not match this execution.");
        }
        EventType lastType = events.get(events.size() - 1).type();
        if (status == Status.FINISHED
                        && lastType
                                != (partial
                                        ? EventType.WORKOUT_FINISHED_EARLY
                                        : EventType.WORKOUT_COMPLETED)
                || status == Status.DISCARDED && lastType != EventType.WORKOUT_DISCARDED
                || status != Status.FINISHED && partial) {
            throw new IllegalArgumentException("Completion state and events disagree.");
        }
        if (status == Status.PAUSED
                        && lastType != EventType.PAUSED
                        && lastType != EventType.RECOVERED_WITH_GAP
                || status == Status.RECOVERY_REQUIRED && lastType != EventType.RECOVERED_WITH_GAP
                || status == Status.ACTIVE
                        && (lastType == EventType.PAUSED
                                || lastType == EventType.RECOVERED_WITH_GAP
                                || lastType == EventType.WORKOUT_COMPLETED
                                || lastType == EventType.WORKOUT_FINISHED_EARLY
                                || lastType == EventType.WORKOUT_DISCARDED)) {
            throw new IllegalArgumentException("Workout status disagrees with its event history.");
        }
        if (completedSession != null) {
            int count = 0;
            for (var block : completedSession.exercises()) {
                for (var set : block.sets()) {
                    var recorded = actualById.get(set.id());
                    if (recorded == null
                            || !recorded.actual().equals(set)
                            || !recorded.exercise().equals(block.exercise())) {
                        throw new IllegalArgumentException(
                                "Completed sets differ from staged actual evidence.");
                    }
                    count++;
                }
            }
            if (count != actualSets.size())
                throw new IllegalArgumentException("Completed evidence omitted actual sets.");
            if (!partial && hasUnfinishedTargets(prescription, slots, actualSets)) {
                throw new IllegalArgumentException(
                        "Unfinished prescribed work cannot be labelled fully complete.");
            }
        }
    }

    /**
     * Only an explicitly started planned execution is eligible for its original plan association.
     */
    public SourceReference plannedAssociation() {
        return origin == Origin.PLANNED && status == Status.FINISHED ? source : null;
    }

    /** Whether prescribed working sets remain, excluding warm-up sets from completion. */
    public boolean hasUnfinishedTargets() {
        return hasUnfinishedTargets(prescription, slots, actualSets);
    }

    private record BlockKey(String slotId, StrengthExercise exercise) {}

    static boolean hasUnfinishedTargets(
            GymWorkoutDefinition prescription,
            List<GymWorkoutDefinition.Slot> slots,
            List<GymExecutionSet> sets) {
        for (var slot : slots) {
            int expected =
                    slot.target() != null ? slot.target().sets() : prescription == null ? 0 : 1;
            int performed = 0;
            for (var actual : sets) {
                if (actual.slotId().equals(slot.id())
                        && actual.exercise().equals(slot.exercise())
                        && !actual.actual().warmUp()) performed++;
            }
            if (performed < expected) return true;
        }
        return false;
    }

    private static Status afterEvent(Status before, EventType type) {
        if (before == Status.FINISHED
                || before == Status.DISCARDED
                || type == EventType.WORKOUT_STARTED) {
            throw new IllegalArgumentException(
                    "Invalid event after a workout start or terminal event.");
        }
        switch (type) {
            case PAUSED:
                if (before != Status.ACTIVE)
                    throw new IllegalArgumentException("Pause requires an active workout.");
                return Status.PAUSED;
            case RESUMED:
                if (before != Status.PAUSED && before != Status.RECOVERY_REQUIRED)
                    throw new IllegalArgumentException(
                            "Resume requires a paused or recovered workout.");
                return Status.ACTIVE;
            case RECOVERED_WITH_GAP:
                return before == Status.PAUSED ? Status.PAUSED : Status.RECOVERY_REQUIRED;
            case WORKOUT_COMPLETED:
            case WORKOUT_FINISHED_EARLY:
                return Status.FINISHED;
            case WORKOUT_DISCARDED:
                return Status.DISCARDED;
            default:
                if (before != Status.ACTIVE)
                    throw new IllegalArgumentException(
                            "Exercise/set events require an active workout.");
                return before;
        }
    }

    static List<StrengthSession.ExerciseSets> actualBlocks(List<GymExecutionSet> sets) {
        var groups = new LinkedHashMap<BlockKey, List<StrengthSession.SetEntry>>();
        for (var row : sets) {
            var key = new BlockKey(row.slotId(), row.exercise());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row.actual());
        }
        if (groups.size() > 100)
            throw new IllegalArgumentException("Too many recorded exercise blocks.");
        var blocks = new ArrayList<StrengthSession.ExerciseSets>();
        for (var entry : groups.entrySet()) {
            blocks.add(
                    new StrengthSession.ExerciseSets(entry.getKey().exercise(), entry.getValue()));
        }
        return Immutable.copyList(blocks);
    }
}
