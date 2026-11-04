package org.racedaydreams.domain.strength;

import java.util.*;

/** Edits an unaccepted concrete preview; every other dated prescription is retained exactly. */
public final class ConcreteStrengthProgrammeEditor {
    private ConcreteStrengthProgrammeEditor() {}

    public static ConcreteStrengthProgrammeDraft replaceWorkout(
            ConcreteStrengthProgrammeDraft draft, StrengthProgramme.Workout replacement) {
        var old = draft.programme();
        var blocks = new ArrayList<StrengthProgramme.Block>();
        boolean found = false;
        for (var block : old.blocks()) {
            var rows = new ArrayList<StrengthProgramme.Workout>();
            for (var workout : block.workouts()) {
                if (workout.id().equals(replacement.id())) {
                    if (!workout.date().equals(replacement.date()))
                        throw new IllegalArgumentException(
                                "Review schedule changes through regeneration.");
                    rows.add(replacement);
                    found = true;
                } else rows.add(workout);
            }
            blocks.add(
                    new StrengthProgramme.Block(
                            block.id(),
                            block.phase(),
                            block.name(),
                            block.start(),
                            block.weeks(),
                            rows,
                            block.explanation()));
        }
        if (!found) throw new IllegalArgumentException("This workout is no longer in the preview.");
        var edited =
                new StrengthProgramme(
                        old.id(),
                        old.revision(),
                        old.parentRevision(),
                        old.createdAt(),
                        old.name(),
                        old.origin(),
                        old.templateId(),
                        old.settings(),
                        blocks,
                        old.effectiveFrom(),
                        old.reason(),
                        old.tradeoffs());
        return draft.withProgramme(edited, draft.substitutions());
    }
}
