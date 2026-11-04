package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Authoring owns prescriptions/choices, never actual sets or a second execution engine. */
public interface StrengthProgrammingRepository {
    enum Owner {
        LEGACY,
        AUTHORED
    }

    record Reference(Owner owner, String id, int revision) {
        public Reference {
            Objects.requireNonNull(owner);
            GymWorkoutDefinition.requireId(id, "programme");
            if (revision < 1) throw new IllegalArgumentException("Invalid programme revision.");
        }

        public String key() {
            return id + ":v" + revision;
        }
    }

    record Decision(
            String id,
            Reference before,
            Reference after,
            Instant at,
            String reason,
            List<String> protectedWorkoutIds,
            List<StrengthProgrammeEditor.Change> substitutions) {
        public Decision {
            GymWorkoutDefinition.requireId(id, "programme decision");
            Objects.requireNonNull(at);
            StrengthProgramme.text(reason, 2000, false, "decision reason");
            substitutions = Immutable.copyList(substitutions);
            protectedWorkoutIds = Immutable.copyList(protectedWorkoutIds);
            if (before == null && after == null
                    || after != null && after.owner() != Owner.AUTHORED
                    || substitutions.size() > 312 * 12
                    || protectedWorkoutIds.size() > 312
                    || new java.util.HashSet<>(protectedWorkoutIds).size()
                            != protectedWorkoutIds.size())
                throw new IllegalArgumentException("Invalid programme decision.");
            if (!substitutions.isEmpty()
                    && (before == null
                            || after == null
                            || before.owner() != Owner.AUTHORED
                            || !before.id().equals(after.id())
                            || after.revision() != before.revision() + 1))
                throw new IllegalArgumentException(
                        "Substitutions need an explicit forward revision.");
        }
    }

    /** One read-transaction snapshot; acceptance must still recheck fresh completions. */
    record RevisionReview(StrengthProgramme programme, Set<String> completedWorkoutIds) {
        public RevisionReview {
            Objects.requireNonNull(programme);
            completedWorkoutIds = Immutable.copySet(completedWorkoutIds);
            var known = new java.util.HashSet<String>();
            for (var workout : programme.workouts()) known.add(workout.id());
            if (!known.containsAll(completedWorkoutIds))
                throw new IllegalArgumentException(
                        "Completed evidence references an unknown workout.");
        }

        public Reference reference() {
            return new Reference(Owner.AUTHORED, programme.id(), programme.revision());
        }
    }

    record Summary(String id, int revision, String name, LocalDate start, LocalDate end) {}

    record DraftSummary(String id, int revision, String name, LocalDate start) {}

    Reference currentReference();

    StrengthProgramme programme(String id, int revision);

    /** Read an authored revision and its retained completed evidence in one SQLite snapshot. */
    RevisionReview reviewRevision(Reference reference);

    List<Summary> versions(int limit, int offset);

    StrengthProgrammeDraft draft(String id);

    List<DraftSummary> drafts(int limit, int offset);

    void saveDraft(StrengthProgrammeDraft draft, int expectedRevision);

    void deleteDraft(String id, int expectedRevision);

    CustomStrengthExercise customExercise(String id);

    List<CustomStrengthExercise> customExercises();

    void saveCustom(CustomStrengthExercise exercise, CustomStrengthExercise expected);

    /** Compare-and-activate, with fresh completed evidence checked inside the write transaction. */
    Decision activate(
            StrengthProgramme proposed,
            Reference expected,
            String decisionId,
            List<StrengthProgrammeEditor.Change> substitutions);

    /** Confirm the exact reviewed draft and programme pointer in the SAME write transaction. */
    Decision activateDraft(
            StrengthProgramme proposed,
            Reference expected,
            String decisionId,
            StrengthProgrammeDraft reviewed);

    Decision stop(Reference expected, String decisionId);

    List<Decision> decisions();
}
