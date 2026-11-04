package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.strength.GymExecution.SourceKind;
import org.racedaydreams.domain.strength.GymExecution.SourceReference;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Converts preserved sources into the one date-free gym execution model, without copying actuals.
 */
public final class GymWorkoutAdapter {
    private GymWorkoutAdapter() {}

    public record PreparedWorkout(GymWorkoutDefinition definition, SourceReference source) {
        public PreparedWorkout {
            Objects.requireNonNull(definition, "workout definition");
            Objects.requireNonNull(source, "preserved source");
            if (source.kind() == SourceKind.PROGRAMME
                    && (!definition.id().equals(programmeWorkoutId(source))
                            || definition.revision() != source.revision())) {
                throw new IllegalArgumentException(
                        "Programme workout and source revisions disagree.");
            }
        }
    }

    public static PreparedWorkout fromProgramme(GymProgramme programme, String sessionId) {
        return fromProgramme(programme, sessionId, Collections.emptyMap());
    }

    /** New authoring sources still enter the same authoritative execution model and source kind. */
    public static PreparedWorkout fromAuthoredProgramme(
            StrengthProgramme programme, String sessionId) {
        Objects.requireNonNull(programme, "preserved authored programme");
        var workout =
                programme.workouts().stream()
                        .filter(w -> w.id().equals(sessionId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "The preserved authored session is missing."));
        var source =
                new SourceReference(
                        SourceKind.PROGRAMME, programme.id(), programme.revision(), workout.id());
        var definition =
                new GymWorkoutDefinition(
                        programmeWorkoutId(source),
                        programme.revision(),
                        programme.createdAt(),
                        workout.definition().title(),
                        workout.definition().slots());
        return new PreparedWorkout(definition, source);
    }

    /**
     * Explicit custom definitions are frozen by the caller at start, never resolved again at
     * finish.
     */
    public static PreparedWorkout fromProgramme(
            GymProgramme programme,
            String sessionId,
            Map<String, StrengthExercise> customDefinitions) {
        Objects.requireNonNull(programme, "programme");
        Objects.requireNonNull(customDefinitions, "custom definitions");
        GymProgramme.Session session = null;
        for (var candidate : programme.sessions())
            if (candidate.id().equals(sessionId)) session = candidate;
        if (session == null)
            throw new IllegalArgumentException("The preserved programme session is missing.");
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        for (var prescription : session.exercises()) {
            var canonical = ExerciseCatalog.definition(prescription.exerciseId());
            var exercise =
                    canonical == null
                            ? customDefinitions.get(prescription.exerciseId())
                            : canonical.exercise();
            if (exercise == null || !exercise.id().equals(prescription.exerciseId())) {
                throw new IllegalArgumentException(
                        "The preserved exercise definition is unavailable.");
            }
            boolean rir =
                    programme.settings().effort() == GymProgramme.Effort.RIR
                            && exercise.measurement().usesReps();
            var effort =
                    new GymWorkoutDefinition.EffortRange(
                            rir
                                    ? GymWorkoutDefinition.EffortScale.RIR
                                    : GymWorkoutDefinition.EffortScale.RPE,
                            rir ? 10 - prescription.maximumRpe() : prescription.minimumRpe(),
                            rir ? 10 - prescription.minimumRpe() : prescription.maximumRpe());
            Double reference =
                    exercise.measurement().usesLoad()
                                    && exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
                            ? prescription.referenceLoadKg()
                            : null;
            var target =
                    new GymWorkoutDefinition.Target(
                            prescription.sets(),
                            prescription.minimumReps() > 0 ? prescription.minimumReps() : null,
                            prescription.minimumReps() > 0 ? prescription.maximumReps() : null,
                            prescription.seconds() == null
                                    ? null
                                    : prescription.seconds().longValue(),
                            prescription.metres(),
                            effort,
                            prescription.restSeconds(),
                            reference);
            slots.add(
                    new GymWorkoutDefinition.Slot("slot-" + slots.size(), exercise, target, null));
        }
        var source =
                new SourceReference(
                        SourceKind.PROGRAMME, programme.id(), programme.version(), session.id());
        var definition =
                new GymWorkoutDefinition(
                        programmeWorkoutId(source),
                        programme.version(),
                        programme.createdAt(),
                        session.title(),
                        slots);
        return new PreparedWorkout(definition, source);
    }

    /**
     * A manual workout has no preserved targets. Repeat its working exercise list as open slots.
     */
    public static PreparedWorkout repeatActual(
            String workoutId,
            int revision,
            Instant createdAt,
            String title,
            StrengthSession previous) {
        Objects.requireNonNull(previous, "previous actual workout");
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        for (var block : previous.exercises()) {
            boolean working = false;
            for (var actual : block.sets()) if (!actual.warmUp()) working = true;
            if (working)
                slots.add(
                        new GymWorkoutDefinition.Slot(
                                "slot-" + slots.size(), block.exercise(), null, null));
        }
        if (slots.isEmpty())
            throw new IllegalArgumentException(
                    "This workout has no working exercise structure to repeat.");
        return new PreparedWorkout(
                new GymWorkoutDefinition(workoutId, revision, createdAt, title, slots),
                new SourceReference(SourceKind.ACTUAL_SESSION, previous.id(), 0, null));
    }

    /**
     * Reuses the preserved execution structure, including unperformed targets in a partial workout.
     * Repeating intentions does not claim those targets were completed; new actuals remain empty.
     */
    public static PreparedWorkout repeatExecution(
            String workoutId,
            int revision,
            Instant createdAt,
            String title,
            GymExecution previous) {
        Objects.requireNonNull(previous, "previous execution");
        if (previous.status() != GymExecution.Status.FINISHED)
            throw new IllegalArgumentException("Finish or recover the previous workout first.");
        boolean working = false;
        for (var actual : previous.actualSets()) if (!actual.actual().warmUp()) working = true;
        if (!working)
            throw new IllegalArgumentException("This execution has no working evidence to repeat.");
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        for (var slot : previous.slots())
            slots.add(
                    new GymWorkoutDefinition.Slot(
                            slot.id(),
                            slot.exercise(),
                            slot.target() == null ? null : slot.target().withoutLoadReference(),
                            slot.supersetGroup()));
        return new PreparedWorkout(
                new GymWorkoutDefinition(workoutId, revision, createdAt, title, slots),
                new SourceReference(
                        SourceKind.ACTUAL_SESSION, previous.completedSession().id(), 0, null));
    }

    static String programmeWorkoutId(SourceReference source) {
        if (source.kind() != SourceKind.PROGRAMME)
            throw new IllegalArgumentException("A programme source is required.");
        String value =
                source.id().length()
                        + ":"
                        + source.id()
                        + ":"
                        + source.revision()
                        + ":"
                        + source.sessionId();
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder("programme-session-");
            for (byte part : digest)
                hex.append(Character.forDigit((part >>> 4) & 15, 16))
                        .append(Character.forDigit(part & 15, 16));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "SHA-256 is required by the Java/Android platform.", impossible);
        }
    }
}
