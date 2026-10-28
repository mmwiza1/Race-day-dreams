package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.strength.GymExecution.SourceKind;
import org.racedaydreams.domain.strength.GymExecution.SourceReference;

/**
 * Checks preserved intentions against their retained source before storage or restore. This class
 * never resolves exercises through the current catalogue. Repositories must additionally retain and
 * compare the exact immutable workout snapshot accepted when execution started.
 */
public final class GymWorkoutSourceValidator {
    private GymWorkoutSourceValidator() {}

    /**
     * Returns the exact retained session, including its original date for an explicit association.
     * Legacy programmes retained exercise IDs and targets, not exercise names/definitions. This
     * check can prove that structure and those targets agree; it cannot authenticate an old name.
     */
    public static GymProgramme.Session programme(
            GymWorkoutDefinition workout, SourceReference source, GymProgramme retained) {
        require(workout != null && retained != null, "The retained programme is required.");
        sourceKind(source, SourceKind.PROGRAMME);
        require(
                retained.id().equals(source.id()) && retained.version() == source.revision(),
                "The exact programme revision is required.");
        GymProgramme.Session session = null;
        for (var candidate : retained.sessions()) {
            if (candidate.id().equals(source.sessionId())) session = candidate;
        }
        require(session != null, "The retained programme session is missing.");
        require(
                workout.id().equals(GymWorkoutAdapter.programmeWorkoutId(source))
                        && workout.revision() == source.revision()
                        && workout.createdAt().equals(retained.createdAt())
                        && workout.title().equals(session.title())
                        && workout.slots().size() == session.exercises().size(),
                "The preserved workout differs from its programme source.");
        for (int i = 0; i < session.exercises().size(); i++) {
            var prescription = session.exercises().get(i);
            var slot = workout.slots().get(i);
            require(
                    slot.id().equals("slot-" + i)
                            && slot.exercise().id().equals(prescription.exerciseId())
                            && slot.supersetGroup() == null,
                    "The prescribed exercise order or identity differs.");
            var exercise = slot.exercise();
            boolean rir =
                    retained.settings().effort() == GymProgramme.Effort.RIR
                            && exercise.measurement().usesReps();
            var expectedEffort =
                    new GymWorkoutDefinition.EffortRange(
                            rir
                                    ? GymWorkoutDefinition.EffortScale.RIR
                                    : GymWorkoutDefinition.EffortScale.RPE,
                            rir ? 10 - prescription.maximumRpe() : prescription.minimumRpe(),
                            rir ? 10 - prescription.minimumRpe() : prescription.maximumRpe());
            Double expectedReference =
                    exercise.measurement().usesLoad()
                                    && exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
                            ? prescription.referenceLoadKg()
                            : null;
            var expectedTarget =
                    new GymWorkoutDefinition.Target(
                            prescription.sets(),
                            prescription.minimumReps() > 0 ? prescription.minimumReps() : null,
                            prescription.minimumReps() > 0 ? prescription.maximumReps() : null,
                            prescription.seconds() == null
                                    ? null
                                    : prescription.seconds().longValue(),
                            prescription.metres(),
                            expectedEffort,
                            prescription.restSeconds(),
                            expectedReference);
            require(
                    expectedTarget.equals(slot.target()),
                    "The preserved target differs from its programme prescription.");
        }
        return session;
    }

    /** Saved revisions include frozen exercise definitions and must match in full. */
    public static void savedWorkout(
            GymWorkoutDefinition workout, SourceReference source, GymWorkoutDefinition retained) {
        require(workout != null && retained != null, "The retained workout revision is required.");
        sourceKind(source, SourceKind.SAVED_WORKOUT);
        require(
                retained.id().equals(source.id())
                        && retained.revision() == source.revision()
                        && retained.equals(workout),
                "The preserved workout differs from its saved revision.");
    }

    /** Manual actuals supply only the retained working exercise list, without invented targets. */
    public static void repeatedActual(
            GymWorkoutDefinition workout, SourceReference source, StrengthSession retained) {
        require(workout != null && retained != null, "The retained actual workout is required.");
        sourceKind(source, SourceKind.ACTUAL_SESSION);
        var expected =
                GymWorkoutAdapter.repeatActual(
                        workout.id(),
                        workout.revision(),
                        workout.createdAt(),
                        workout.title(),
                        retained);
        require(
                source.equals(expected.source()) && workout.equals(expected.definition()),
                "The repeated workout differs from its retained actual exercise structure.");
    }

    /**
     * An explicitly retained execution can supply unperformed intentions from a partial workout.
     * Its targets remain intentions; repeating never copies old actuals or load references.
     */
    public static void repeatedExecution(
            GymWorkoutDefinition workout, SourceReference source, GymExecution retained) {
        require(workout != null && retained != null, "The retained execution is required.");
        sourceKind(source, SourceKind.ACTUAL_SESSION);
        var expected =
                GymWorkoutAdapter.repeatExecution(
                        workout.id(),
                        workout.revision(),
                        workout.createdAt(),
                        workout.title(),
                        retained);
        require(
                source.equals(expected.source()) && workout.equals(expected.definition()),
                "The repeated workout differs from its retained execution intentions.");
    }

    private static void sourceKind(SourceReference source, SourceKind kind) {
        require(source != null && source.kind() == kind, "The exact workout source is required.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
