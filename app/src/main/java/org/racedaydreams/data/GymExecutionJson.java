package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymExecutionSet;
import org.racedaydreams.domain.strength.StrengthSession;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * V1 execution staging codec. It preserves original, today-only and actual exercise definitions
 * separately. Domain constructors replay event/state invariants before returning a snapshot.
 *
 * <p>Source IDs bind identity, not source content. A repository/finaliser or backup validator must
 * additionally check the frozen prescription against its exact retained source revision. Never
 * rebuild historical source content from the current exercise catalogue while decoding.
 */
public final class GymExecutionJson {
    private GymExecutionJson() {}

    public static String write(GymExecution execution) {
        return GymJsonSupport.write(encode(execution));
    }

    public static GymExecution read(String payload) {
        return decode(GymJsonSupport.read(payload));
    }

    public static JSONObject encode(GymExecution execution) {
        if (execution == null) throw invalid("execution");
        JSONArray actual = new JSONArray();
        for (var row : execution.actualSets())
            actual.put(
                    object(
                            "slotId",
                            row.slotId(),
                            "exercise",
                            GymWorkoutJson.exercise(row.exercise()),
                            "actual",
                            set(row.actual())));
        JSONArray events = new JSONArray();
        for (var event : execution.events())
            events.put(
                    object(
                            "sequence",
                            event.sequence(),
                            "type",
                            event.type().name(),
                            "slotId",
                            event.slotId(),
                            "actualSetId",
                            event.actualSetId(),
                            "recordedElapsedMillis",
                            event.recordedElapsedMillis(),
                            "activeMillis",
                            event.activeMillis(),
                            "observedAt",
                            event.observedAt().toString()));
        return object(
                "version",
                GymExecution.FORMAT_VERSION,
                "id",
                execution.id(),
                "origin",
                execution.origin().name(),
                "prescription",
                execution.prescription() == null
                        ? null
                        : GymWorkoutJson.encode(execution.prescription()),
                "source",
                execution.source() == null ? null : source(execution.source()),
                "startedAt",
                execution.startedAt().toString(),
                "offset",
                execution.offset().toString(),
                "status",
                execution.status().name(),
                "lastTick",
                tick(execution.lastTick()),
                "recordedElapsedMillis",
                execution.recordedElapsedMillis(),
                "activeMillis",
                execution.activeMillis(),
                "selectedSlotId",
                execution.selectedSlotId(),
                "slots",
                GymWorkoutJson.slots(execution.slots()),
                "actualSets",
                actual,
                "events",
                events,
                "completedSession",
                execution.completedSession() == null ? null : session(execution.completedSession()),
                "partial",
                execution.partial());
    }

    public static GymExecution decode(JSONObject json) {
        fields(
                json,
                "version",
                "id",
                "origin",
                "prescription",
                "source",
                "startedAt",
                "offset",
                "status",
                "lastTick",
                "recordedElapsedMillis",
                "activeMillis",
                "selectedSlotId",
                "slots",
                "actualSets",
                "events",
                "completedSession",
                "partial");
        version(json, GymExecution.FORMAT_VERSION);
        List<GymExecutionSet> actual = new ArrayList<>();
        JSONArray rows = array(json, "actualSets", 10000);
        for (int i = 0; i < rows.length(); i++) {
            var row = row(rows, i);
            fields(row, "slotId", "exercise", "actual");
            actual.add(
                    new GymExecutionSet(
                            text(row, "slotId", 256),
                            GymWorkoutJson.exercise(child(row, "exercise")),
                            set(child(row, "actual"))));
        }
        List<GymExecution.Event> events = new ArrayList<>();
        rows = array(json, "events", 50000);
        for (int i = 0; i < rows.length(); i++) {
            var row = row(rows, i);
            fields(
                    row,
                    "sequence",
                    "type",
                    "slotId",
                    "actualSetId",
                    "recordedElapsedMillis",
                    "activeMillis",
                    "observedAt");
            events.add(
                    new GymExecution.Event(
                            integer(row, "sequence"),
                            choice(row, "type", GymExecution.EventType.class),
                            nullableText(row, "slotId", 256),
                            nullableText(row, "actualSetId", 256),
                            whole(row, "recordedElapsedMillis"),
                            whole(row, "activeMillis"),
                            instant(row, "observedAt")));
        }
        return new GymExecution(
                text(json, "id", 256),
                choice(json, "origin", GymExecution.Origin.class),
                isNull(json, "prescription")
                        ? null
                        : GymWorkoutJson.decode(child(json, "prescription")),
                isNull(json, "source") ? null : source(child(json, "source")),
                instant(json, "startedAt"),
                offset(json, "offset"),
                choice(json, "status", GymExecution.Status.class),
                tick(child(json, "lastTick")),
                whole(json, "recordedElapsedMillis"),
                whole(json, "activeMillis"),
                nullableText(json, "selectedSlotId", 256),
                GymWorkoutJson.slots(array(json, "slots", 100)),
                actual,
                events,
                isNull(json, "completedSession") ? null : session(child(json, "completedSession")),
                bool(json, "partial"));
    }

    private static JSONObject source(GymExecution.SourceReference source) {
        return object(
                "kind",
                source.kind().name(),
                "id",
                source.id(),
                "revision",
                source.revision(),
                "sessionId",
                source.sessionId());
    }

    private static GymExecution.SourceReference source(JSONObject json) {
        fields(json, "kind", "id", "revision", "sessionId");
        return new GymExecution.SourceReference(
                choice(json, "kind", GymExecution.SourceKind.class),
                text(json, "id", 256),
                integer(json, "revision"),
                nullableText(json, "sessionId", 256));
    }

    private static JSONObject tick(GymExecution.Tick tick) {
        return object(
                "continuityId",
                tick.continuityId(),
                "monotonicMillis",
                tick.monotonicMillis(),
                "wallTime",
                tick.wallTime().toString());
    }

    private static GymExecution.Tick tick(JSONObject json) {
        fields(json, "continuityId", "monotonicMillis", "wallTime");
        return new GymExecution.Tick(
                text(json, "continuityId", 256),
                whole(json, "monotonicMillis"),
                instant(json, "wallTime"));
    }

    private static JSONObject set(StrengthSession.SetEntry set) {
        return object(
                "id",
                set.id(),
                "reps",
                set.reps(),
                "kilograms",
                set.kilograms(),
                "rpe",
                set.rpe(),
                "warmUp",
                set.warmUp(),
                "durationSeconds",
                set.durationSeconds(),
                "distanceMetres",
                set.distanceMetres(),
                "repetitionsInReserve",
                set.repetitionsInReserve());
    }

    private static StrengthSession.SetEntry set(JSONObject json) {
        fields(
                json,
                "id",
                "reps",
                "kilograms",
                "rpe",
                "warmUp",
                "durationSeconds",
                "distanceMetres",
                "repetitionsInReserve");
        return new StrengthSession.SetEntry(
                text(json, "id", 256),
                integer(json, "reps"),
                number(json, "kilograms"),
                nullableNumber(json, "rpe"),
                bool(json, "warmUp"),
                nullableLong(json, "durationSeconds"),
                nullableNumber(json, "distanceMetres"),
                nullableNumber(json, "repetitionsInReserve"));
    }

    private static JSONObject session(StrengthSession session) {
        JSONArray blocks = new JSONArray();
        for (var block : session.exercises()) {
            JSONArray sets = new JSONArray();
            for (var actual : block.sets()) sets.put(set(actual));
            blocks.put(object("exercise", GymWorkoutJson.exercise(block.exercise()), "sets", sets));
        }
        return object(
                "id",
                session.id(),
                "startedAt",
                session.startedAt().toString(),
                "offset",
                session.offset().toString(),
                "durationMillis",
                session.duration().toMillis(),
                "rpe",
                session.rpe(),
                "notes",
                session.notes(),
                "exercises",
                blocks);
    }

    private static StrengthSession session(JSONObject json) {
        fields(json, "id", "startedAt", "offset", "durationMillis", "rpe", "notes", "exercises");
        List<StrengthSession.ExerciseSets> blocks = new ArrayList<>();
        JSONArray rows = array(json, "exercises", 100);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = row(rows, i);
            fields(row, "exercise", "sets");
            List<StrengthSession.SetEntry> sets = new ArrayList<>();
            JSONArray values = array(row, "sets", 100);
            for (int k = 0; k < values.length(); k++) sets.add(set(row(values, k)));
            blocks.add(
                    new StrengthSession.ExerciseSets(
                            GymWorkoutJson.exercise(child(row, "exercise")), sets));
        }
        long millis = whole(json, "durationMillis");
        if (millis <= 0 || millis > 12L * 60 * 60 * 1000) throw invalid("completed duration");
        return new StrengthSession(
                text(json, "id", 256),
                instant(json, "startedAt"),
                offset(json, "offset"),
                Duration.ofMillis(millis),
                nullableNumber(json, "rpe"),
                text(json, "notes", 10000),
                blocks);
    }
}
