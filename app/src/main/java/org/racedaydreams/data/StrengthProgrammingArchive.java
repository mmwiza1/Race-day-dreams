package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import org.json.*;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*;

import java.util.*;

/** Entire authoring graph is checked before restore; SQL reconstruction is a separate operation. */
final class StrengthProgrammingArchive {
    private StrengthProgrammingArchive() {}

    record Snapshot(
            Map<String, StrengthProgramme> programmes,
            List<StrengthProgrammeDraft> drafts,
            List<CustomStrengthExercise> custom,
            List<Decision> decisions,
            Reference current) {}

    static Snapshot validate(JSONObject json, JSONObject legacy) {
        fields(json, "version", "programmes", "drafts", "customExercises", "decisions", "current");
        version(json, 1);
        var programmes = new LinkedHashMap<String, StrengthProgramme>();
        var legacyIds = new HashSet<String>();
        var legacyKeys = new HashSet<String>();
        if (legacy != null) {
            GymProgrammeStore.validate(legacy);
            var rows = array(legacy, "versions", 1000);
            for (int i = 0; i < rows.length(); i++) {
                var p = GymProgrammeStore.parse(row(rows, i));
                legacyIds.add(p.id());
                legacyKeys.add(GymProgrammeStore.key(p));
            }
        }
        var rows = array(json, "programmes", 1000);
        for (int i = 0; i < rows.length(); i++) {
            var p = StrengthProgrammingJson.decode(row(rows, i));
            SqliteStrengthProgrammingRepository.validateRows(p);
            if (legacyIds.contains(p.id()) || programmes.put(key(p), p) != null)
                throw invalid("duplicate or ambiguous programme identity");
        }
        var custom = new ArrayList<CustomStrengthExercise>();
        var customIds = new HashSet<String>();
        rows = array(json, "customExercises", 10000);
        for (int i = 0; i < rows.length(); i++) {
            var e = StrengthProgrammingJson.decodeCustom(row(rows, i));
            SqliteStrengthProgrammingRepository.bounded(
                    StrengthProgrammingJson.encodeCustom(e), 65536);
            if (!customIds.add(e.exercise().id())) throw invalid("duplicate custom exercise");
            custom.add(e);
        }
        for (var programme : programmes.values())
            for (var workout : programme.workouts())
                for (var slot : workout.definition().slots())
                    if (ExerciseCatalog.definition(slot.exercise().id()) == null
                            && !customIds.contains(slot.exercise().id()))
                        throw invalid("programme custom metadata reference");
        var drafts = new ArrayList<StrengthProgrammeDraft>();
        var draftIds = new HashSet<String>();
        rows = array(json, "drafts", 1000);
        for (int i = 0; i < rows.length(); i++) {
            var d = StrengthProgrammingJson.decodeDraft(row(rows, i));
            SqliteStrengthProgrammingRepository.validateRows(d);
            if (!draftIds.add(d.id())
                    || legacyIds.contains(d.programmeId())
                    || d.baseRevision() > 0
                            && !programmes.containsKey(d.programmeId() + ":v" + d.baseRevision()))
                throw invalid("draft identity or base revision");
            for (var b : d.blocks())
                for (var day : b.days())
                    for (var slot : day.slots())
                        if (ExerciseCatalog.definition(slot.exercise().id()) == null
                                && !customIds.contains(slot.exercise().id()))
                            throw invalid("draft custom metadata reference");
            drafts.add(d);
        }
        var decisions = new ArrayList<Decision>();
        var decisionIds = new HashSet<String>();
        var activated = new HashSet<String>();
        Reference previous = null;
        rows = array(json, "decisions", 100000);
        for (int i = 0; i < rows.length(); i++) {
            var d = decision(row(rows, i));
            SqliteStrengthProgrammingRepository.bounded(decision(d), 524288);
            if (!decisionIds.add(d.id())) throw invalid("duplicate programme decision");
            checkRef(d.before(), programmes, legacyKeys);
            checkRef(d.after(), programmes, legacyKeys);
            if (i > 0 && previous != null && !previous.equals(d.before()))
                throw invalid("programme decision chain");
            if (i == 0 && d.before() != null && d.before().owner() == Owner.AUTHORED)
                throw invalid("missing first activation");
            if (previous == null && d.before() != null && d.before().owner() == Owner.AUTHORED)
                throw invalid("authored programme cannot silently become current");
            if (d.after() != null) {
                var p = programmes.get(d.after().key());
                if (p.createdAt().isAfter(d.at()) || !activated.add(key(p)))
                    throw invalid("programme activation order");
                if (p.revision() > 1) {
                    var base = programmes.get(p.id() + ":v" + p.parentRevision());
                    if (base == null
                            || !new Reference(Owner.AUTHORED, base.id(), base.revision())
                                    .equals(d.before()))
                        throw invalid("programme revision lineage");
                    new StrengthProgrammeEditor.Proposal(
                            base, p, new HashSet<>(d.protectedWorkoutIds()), d.substitutions());
                } else if (!d.protectedWorkoutIds().isEmpty() || !d.substitutions().isEmpty())
                    throw invalid("initial programme cannot claim earlier substitution evidence");
            } else if (!d.protectedWorkoutIds().isEmpty() || !d.substitutions().isEmpty())
                throw invalid("stop cannot rewrite a prescription");
            previous = d.after();
            decisions.add(d);
        }
        Reference current = isNull(json, "current") ? null : reference(child(json, "current"));
        if (current != null && current.owner() != Owner.AUTHORED
                || !Objects.equals(current, previous)
                || !activated.equals(programmes.keySet()))
            throw invalid("current programme or orphan revision");
        if (current != null && legacy != null && !isNull(legacy, "current"))
            throw invalid("two active strength programmes");
        return new Snapshot(
                Collections.unmodifiableMap(programmes),
                Collections.unmodifiableList(drafts),
                Collections.unmodifiableList(custom),
                Collections.unmodifiableList(decisions),
                current);
    }

    static String key(StrengthProgramme p) {
        return p.id() + ":v" + p.revision();
    }

    static void validateCoaching(Snapshot snapshot, JSONObject coaching) {
        var definitions = new HashMap<String, StrengthExercise>();
        if (coaching != null) {
            var rows = array(coaching, "strength_exercises", 100000);
            for (int i = 0; i < rows.length(); i++) {
                var row = row(rows, i);
                try {
                    var exercise =
                            CoachingJson.exercise(
                                    FeatureJson.object(text(row, "payload", 1024 * 1024)));
                    definitions.put(exercise.id(), exercise);
                } catch (JSONException e) {
                    throw FeatureJson.invalid(e);
                }
            }
        }
        for (var custom : snapshot.custom())
            if (!custom.exercise().equals(definitions.get(custom.exercise().id())))
                throw invalid("custom authoring and logging definitions disagree");
    }

    private static void checkRef(
            Reference ref, Map<String, StrengthProgramme> modern, Set<String> legacy) {
        if (ref != null
                && !(ref.owner() == Owner.AUTHORED
                        ? modern.containsKey(ref.key())
                        : legacy.contains(ref.key())))
            throw invalid("missing programme decision reference");
    }

    static JSONObject reference(Reference ref) {
        return object("owner", ref.owner().name(), "id", ref.id(), "revision", ref.revision());
    }

    static Reference reference(JSONObject json) {
        fields(json, "owner", "id", "revision");
        return new Reference(
                choice(json, "owner", Owner.class),
                text(json, "id", 256),
                integer(json, "revision"));
    }

    static JSONObject decision(Decision d) {
        var changes = new JSONArray();
        for (var c : d.substitutions())
            changes.put(
                    object(
                            "workoutId",
                            c.workoutId(),
                            "slotId",
                            c.slotId(),
                            "from",
                            c.fromExerciseId(),
                            "to",
                            c.toExerciseId(),
                            "reason",
                            c.reason()));
        var protectedIds = new JSONArray();
        for (var id : d.protectedWorkoutIds()) protectedIds.put(id);
        return object(
                "version",
                1,
                "id",
                d.id(),
                "before",
                d.before() == null ? null : reference(d.before()),
                "after",
                d.after() == null ? null : reference(d.after()),
                "at",
                d.at().toString(),
                "reason",
                d.reason(),
                "protectedWorkoutIds",
                protectedIds,
                "substitutions",
                changes);
    }

    static Decision decision(JSONObject json) {
        fields(
                json,
                "version",
                "id",
                "before",
                "after",
                "at",
                "reason",
                "protectedWorkoutIds",
                "substitutions");
        version(json, 1);
        var changes = new ArrayList<StrengthProgrammeEditor.Change>();
        var rows = array(json, "substitutions", 312 * 12);
        for (int i = 0; i < rows.length(); i++) {
            var c = row(rows, i);
            fields(c, "workoutId", "slotId", "from", "to", "reason");
            changes.add(
                    new StrengthProgrammeEditor.Change(
                            text(c, "workoutId", 256),
                            text(c, "slotId", 256),
                            text(c, "from", 256),
                            text(c, "to", 256),
                            text(c, "reason", 2000)));
        }
        var protectedIds = new ArrayList<String>();
        rows = array(json, "protectedWorkoutIds", 312);
        for (int i = 0; i < rows.length(); i++) {
            Object v = rows.opt(i);
            if (!(v instanceof String) || ((String) v).isBlank() || ((String) v).length() > 256)
                throw invalid("protected session identity");
            protectedIds.add((String) v);
        }
        return new Decision(
                text(json, "id", 256),
                isNull(json, "before") ? null : reference(child(json, "before")),
                isNull(json, "after") ? null : reference(child(json, "after")),
                instant(json, "at"),
                text(json, "reason", 2000),
                protectedIds,
                changes);
    }
}
