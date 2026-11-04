package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*;

import java.time.*;
import java.util.*;

/** Unaccepted previews retain concrete dates, targets and explanations, without actuals. */
public final class ConcreteStrengthProgrammeDraftTestSuite {
    private static int checks;

    public static void main(String[] args) {
        var settings =
                StrengthProgrammeTestSuite.settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Experience.EXPERIENCED,
                        GymProgramme.Priority.BALANCED,
                        4,
                        60,
                        EnumSet.allOf(ExerciseMetadata.Equipment.class));
        var p =
                new StrengthProgrammeGenerator()
                        .generate(
                                StrengthProgrammeTestSuite.request(
                                        settings,
                                        16,
                                        "upper-lower-4",
                                        Set.of(),
                                        PrimaryFocus.HYBRID,
                                        Set.of(StrengthProgrammeTestSuite.START.plusDays(2)),
                                        null,
                                        true));
        var changes = new ArrayList<StrengthProgrammeEditor.Change>();
        var draft = new ConcreteStrengthProgrammeDraft("preview", 1, null, p, changes);
        changes.add(new StrengthProgrammeEditor.Change("w", "s", "old", "new", "Equipment busy"));
        check(draft.substitutions().isEmpty(), "Defensively copied decisions");
        check(draft.programme().equals(p), "Exact programme, not a repeated first week");
        check(draft.programme().workouts().size() == 64, "All 16 dated weeks retained");
        check(draft.programme().blocks().size() > 2, "Meaningful phases retained");
        check(!draft.programme().tradeoffs().isEmpty(), "Generation explanations retained");
        check(draft.withProgramme(p, List.of()).revision() == 2, "Optimistic edit revision");
        rejects(() -> draft.substitutions().clear());
        rejects(() -> new ConcreteStrengthProgrammeDraft("preview", 0, null, p, List.of()));
        rejects(
                () ->
                        new ConcreteStrengthProgrammeDraft(
                                "preview",
                                1,
                                new Reference(Owner.AUTHORED, p.id(), 1),
                                p,
                                List.of()));
        rejects(() -> new ConcreteStrengthProgrammeDraft("preview", 1, null, p, changes));
        var next =
                new StrengthProgramme(
                        p.id(),
                        2,
                        1,
                        p.createdAt(),
                        p.name(),
                        p.origin(),
                        p.templateId(),
                        p.settings(),
                        p.blocks(),
                        p.effectiveFrom(),
                        "Review future work",
                        p.tradeoffs());
        rejects(() -> draft.withProgramme(next, List.of()));
        rejects(() -> new ConcreteStrengthProgrammeDraft("preview", 1, null, next, List.of()));
        var base = new Reference(Owner.AUTHORED, p.id(), 1);
        check(
                new ConcreteStrengthProgrammeDraft("future", 1, base, next, List.of())
                        .before()
                        .equals(base),
                "Forward revision identifies immutable base");
        var c = new StrengthProgrammeEditor.Change("w:a", "b", "old", "new", "Choice");
        var other = new StrengthProgrammeEditor.Change("w", "a:b", "old", "new", "Choice");
        check(
                new ConcreteStrengthProgrammeDraft("future", 1, base, next, List.of(c, other))
                                .substitutions()
                                .size()
                        == 2,
                "IDs containing delimiters are not conflated");
        rejects(() -> new ConcreteStrengthProgrammeDraft("future", 1, base, next, List.of(c, c)));
        var workout = p.workouts().get(0);
        var editedWorkout =
                new StrengthProgramme.Workout(
                        workout.id(),
                        workout.date(),
                        workout.expectedMinutes(),
                        workout.definition(),
                        "Athlete's own reviewed guidance");
        var editedDraft = ConcreteStrengthProgrammeEditor.replaceWorkout(draft, editedWorkout);
        check(editedDraft.revision() == 2, "Dated edit advances only draft revision");
        check(
                editedDraft.programme().workouts().stream()
                        .filter(w -> w.id().equals(workout.id()))
                        .findFirst()
                        .orElseThrow()
                        .equals(editedWorkout),
                "Edited workout retained");
        check(
                editedDraft.programme().workouts().stream()
                        .filter(w -> !w.id().equals(workout.id()))
                        .allMatch(p.workouts()::contains),
                "Every other dated prescription exact");
        check(
                editedDraft.programme().tradeoffs().equals(p.tradeoffs()),
                "Generation explanation not dropped by editing");
        check(draft.programme().equals(p), "Original candidate remains immutable");
        rejects(
                () ->
                        ConcreteStrengthProgrammeEditor.replaceWorkout(
                                draft,
                                new StrengthProgramme.Workout(
                                        workout.id(),
                                        workout.date().plusDays(1),
                                        workout.expectedMinutes(),
                                        workout.definition(),
                                        workout.notes())));
        System.out.println(
                "ConcreteStrengthProgrammeDraftTestSuite: " + checks + " checks passed.");
    }

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    private static void rejects(Runnable run) {
        checks++;
        try {
            run.run();
        } catch (IllegalArgumentException | UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected rejection");
    }
}
