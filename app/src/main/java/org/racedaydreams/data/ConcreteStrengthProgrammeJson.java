package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import org.json.*;
import org.racedaydreams.domain.strength.*;

import java.util.*;

/** Independently versioned concrete drafts use the existing frozen programme codec. */
final class ConcreteStrengthProgrammeJson {
    private ConcreteStrengthProgrammeJson() {}

    static JSONObject encode(ConcreteStrengthProgrammeDraft draft) {
        var changes = new JSONArray();
        for (var c : draft.substitutions())
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
        return object(
                "version",
                1,
                "kind",
                "concreteDraft",
                "id",
                draft.id(),
                "revision",
                draft.revision(),
                "before",
                draft.before() == null
                        ? null
                        : StrengthProgrammingArchive.reference(draft.before()),
                "programme",
                StrengthProgrammingJson.encode(draft.programme()),
                "substitutions",
                changes);
    }

    static ConcreteStrengthProgrammeDraft decode(JSONObject json) {
        fields(json, "version", "kind", "id", "revision", "before", "programme", "substitutions");
        version(json, 1);
        if (!"concreteDraft".equals(text(json, "kind", 40))) throw invalid("concrete draft kind");
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
        return new ConcreteStrengthProgrammeDraft(
                text(json, "id", 256),
                integer(json, "revision"),
                isNull(json, "before")
                        ? null
                        : StrengthProgrammingArchive.reference(child(json, "before")),
                StrengthProgrammingJson.decode(child(json, "programme")),
                changes);
    }
}
