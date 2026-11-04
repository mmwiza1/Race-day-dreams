package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** A saved, unaccepted concrete preview. It preserves dated variation and all explanations. */
public record ConcreteStrengthProgrammeDraft(
        String id,
        int revision,
        StrengthProgrammingRepository.Reference before,
        StrengthProgramme programme,
        List<StrengthProgrammeEditor.Change> substitutions) {
    public ConcreteStrengthProgrammeDraft {
        GymWorkoutDefinition.requireId(id, "concrete programme draft");
        if (revision < 1) throw new IllegalArgumentException("Invalid concrete draft revision.");
        Objects.requireNonNull(programme);
        substitutions = Immutable.copyList(substitutions);
        boolean forward =
                before != null
                        && before.owner() == StrengthProgrammingRepository.Owner.AUTHORED
                        && before.id().equals(programme.id())
                        && before.revision() == programme.parentRevision();
        if (programme.parentRevision() > 0 && !forward)
            throw new IllegalArgumentException(
                    "A revision draft must identify its preserved base.");
        if (programme.parentRevision() == 0 && before != null && before.id().equals(programme.id()))
            throw new IllegalArgumentException(
                    "A replacement programme needs a distinct identity.");
        if (!substitutions.isEmpty() && !forward)
            throw new IllegalArgumentException("Permanent substitutions need a forward revision.");
        var changed = new HashSet<List<String>>();
        if (substitutions.size() > 312 * 12)
            throw new IllegalArgumentException("Too many substitution decisions.");
        for (var change : substitutions)
            if (!changed.add(List.of(change.workoutId(), change.slotId())))
                throw new IllegalArgumentException("Review the final substitution for each slot.");
        for (var workout : programme.workouts())
            for (var slot : workout.definition().slots())
                if (slot.target() == null)
                    throw new IllegalArgumentException(
                            "Review a target for every programme exercise.");
    }

    /**
     * Structural validation only: unchanged retained rows may predate changed availability. This
     * never infers completion. Acceptance must independently query fresh actual evidence.
     */
    public void validateAgainst(StrengthProgramme preserved) {
        Objects.requireNonNull(preserved);
        if (before == null
                || before.owner() != StrengthProgrammingRepository.Owner.AUTHORED
                || !before.id().equals(preserved.id())
                || before.revision() != preserved.revision()
                || programme.parentRevision() == 0)
            throw new IllegalArgumentException("Review the preserved base of this revision.");
        var proposed = new java.util.HashMap<String, StrengthProgramme.Workout>();
        for (var workout : programme.workouts()) proposed.put(workout.id(), workout);
        var unchanged = new HashSet<String>();
        for (var workout : preserved.workouts())
            if (workout.equals(proposed.get(workout.id()))) unchanged.add(workout.id());
        new StrengthProgrammeEditor.Proposal(preserved, programme, unchanged, substitutions);
    }

    /** Retain programme lineage while saving a new athlete-edited draft revision. */
    public ConcreteStrengthProgrammeDraft withProgramme(
            StrengthProgramme replacement, List<StrengthProgrammeEditor.Change> changes) {
        if (!programme.id().equals(replacement.id())
                || programme.revision() != replacement.revision()
                || programme.parentRevision() != replacement.parentRevision()
                || replacement.createdAt().isBefore(programme.createdAt()))
            throw new IllegalArgumentException(
                    "Draft edits must retain programme identity and lineage.");
        return new ConcreteStrengthProgrammeDraft(
                id, Math.addExact(revision, 1), before, replacement, changes);
    }
}
