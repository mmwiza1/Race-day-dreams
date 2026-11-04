package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.*;
import java.util.*;

/** Optional task-specific progression. No actual, reference or programme is ever mutated. */
public final class StrengthTargetProgression {
    private StrengthTargetProgression() {}

    public record Advice(
            StrengthProgressionAdvisor.Action action,
            String reason,
            GymWorkoutDefinition.Target proposedTarget,
            List<String> supportingSessionIds) {
        public Advice {
            Objects.requireNonNull(action);
            Objects.requireNonNull(reason);
            supportingSessionIds = Immutable.copyList(supportingSessionIds);
            if ((action == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE)
                    != (proposedTarget != null))
                throw new IllegalArgumentException(
                        "A progression target is an optional increase only.");
        }
    }

    private record Evidence(StrengthSession session, List<StrengthSession.SetEntry> sets) {}

    public static Advice review(
            GymWorkoutDefinition.Slot slot, List<StrengthSession> history, Instant asOf) {
        Objects.requireNonNull(slot);
        Objects.requireNonNull(history);
        Objects.requireNonNull(asOf);
        var target = slot.target();
        var exercise = slot.exercise();
        var canonical = ExerciseCatalog.definition(exercise.id());
        if (target == null
                || canonical == null
                || canonical.exercise().measurement() != exercise.measurement()
                || canonical.exercise().mode() != exercise.mode()
                || canonical.exercise().region() != exercise.region())
            return advice(
                    StrengthProgressionAdvisor.Action.GATHER_EVIDENCE,
                    "An explicit supported task and comparable working evidence are needed. No"
                            + " custom-exercise progression is inferred.",
                    null,
                    Collections.emptyList());
        var recent = new ArrayList<>(history);
        recent.sort(
                Comparator.comparing(StrengthSession::startedAt)
                        .reversed()
                        .thenComparing(StrengthSession::id));
        var evidence = new ArrayList<Evidence>();
        var seen = new HashSet<String>();
        for (var session : recent) {
            if (session.startedAt().isBefore(asOf.minus(Duration.ofDays(28)))
                    || session.startedAt().isAfter(asOf)
                    || !seen.add(session.id())) continue;
            var sets = new ArrayList<StrengthSession.SetEntry>();
            for (var block : session.exercises())
                if (block.exercise().id().equals(exercise.id())
                        && block.exercise().measurement() == exercise.measurement()
                        && block.exercise().mode() == exercise.mode())
                    for (var set : block.sets()) if (!set.warmUp()) sets.add(set);
            if (!sets.isEmpty()) evidence.add(new Evidence(session, sets));
            if (evidence.size() == 2) break;
        }
        var ids = new ArrayList<String>();
        for (var e : evidence) ids.add(e.session().id());
        if (evidence.isEmpty())
            return advice(
                    StrengthProgressionAdvisor.Action.GATHER_EVIDENCE,
                    "No recent exact-exercise working sets support a change. Older, future or"
                            + " warm-up evidence does not count.",
                    null,
                    ids);
        var latest = evidence.get(0).sets();
        if (target.effort() == null)
            return advice(
                    StrengthProgressionAdvisor.Action.HOLD,
                    "No intended effort was specified. Review effort before considering"
                            + " progression.",
                    null,
                    ids);
        double maximumRpe =
                target.effort().scale() == GymWorkoutDefinition.EffortScale.RIR
                        ? 10 - target.effort().minimum()
                        : target.effort().maximum();
        if (latest.size() < target.sets())
            return advice(
                    StrengthProgressionAdvisor.Action.HOLD,
                    "The latest session has fewer working sets than prescribed; repeat and review.",
                    null,
                    ids);
        if (latest.stream()
                .anyMatch(
                        s ->
                                s.coachingRpe() != null && s.coachingRpe() > maximumRpe + .5
                                        || !reachedMinimum(s, target)))
            return advice(
                    StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY,
                    "Recent sets were harder or shorter than intended. Consider less load or a"
                            + " simpler task; nothing changes automatically.",
                    null,
                    ids);
        if (evidence.size() < 2)
            return advice(
                    StrengthProgressionAdvisor.Action.HOLD,
                    "One recent session is useful evidence. Repeat before considering progression.",
                    null,
                    ids);
        double load = latest.get(0).kilograms();
        for (var e : evidence) {
            if (e.sets().size() < target.sets()
                    || e.sets().stream()
                            .anyMatch(
                                    s ->
                                            s.coachingRpe() == null
                                                    || s.coachingRpe() > maximumRpe
                                                    || !reachedTop(s, target)
                                                    || s.kilograms() != load))
                return advice(
                        StrengthProgressionAdvisor.Action.HOLD,
                        "Two recent comparable sessions at the same loading and intended recorded"
                                + " effort are required. Missing effort is unknown.",
                        null,
                        ids);
        }
        // Preserve the established weighted double-progression result rather than another load
        // formula.
        if (exercise.measurement() == StrengthExercise.MeasurementProfile.LOAD_REPS && load > 0) {
            if (target.sets() > 6 || target.maximumReps() > 100)
                return advice(
                        StrengthProgressionAdvisor.Action.HOLD,
                        "This task is outside the established load-advice bounds; keep your chosen"
                                + " load.",
                        null,
                        ids);
            int lowRpe =
                    (int)
                            Math.max(
                                    1,
                                    Math.ceil(
                                            target.effort().scale()
                                                            == GymWorkoutDefinition.EffortScale.RIR
                                                    ? 10 - target.effort().maximum()
                                                    : target.effort().minimum()));
            int highRpe = (int) Math.floor(maximumRpe);
            if (highRpe < lowRpe)
                return advice(
                        StrengthProgressionAdvisor.Action.HOLD,
                        "Use a comparable effort range before seeking a numeric load reference.",
                        null,
                        ids);
            var prescription =
                    new GymProgramme.Prescription(
                            exercise.id(),
                            target.sets(),
                            target.minimumReps(),
                            target.maximumReps(),
                            null,
                            null,
                            lowRpe,
                            highRpe,
                            90,
                            "Exact preserved exercise target");
            var observations = new ArrayList<StrengthSession>();
            for (var e : evidence)
                observations.add(
                        new StrengthSession(
                                e.session().id(),
                                e.session().startedAt(),
                                e.session().offset(),
                                e.session().duration(),
                                e.session().rpe(),
                                e.session().notes(),
                                Immutable.list(
                                        new StrengthSession.ExerciseSets(exercise, e.sets()))));
            var established = StrengthProgressionAdvisor.review(prescription, observations, asOf);
            if (established.action() != StrengthProgressionAdvisor.Action.CONSIDER_INCREASE)
                return advice(established.action(), established.reason(), null, ids);
            if (target.referenceLoadKg() != null
                    && established.suggestedKilograms() <= target.referenceLoadKg())
                return advice(
                        StrengthProgressionAdvisor.Action.HOLD,
                        "A newer optional load reference already exists. Review actual loading"
                                + " before replacing it.",
                        null,
                        ids);
            return advice(
                    established.action(),
                    established.reason(),
                    new GymWorkoutDefinition.Target(
                            target.sets(),
                            target.minimumReps(),
                            target.maximumReps(),
                            null,
                            null,
                            target.effort(),
                            target.restSeconds(),
                            established.suggestedKilograms()),
                    ids);
        }
        Integer low = target.minimumReps(), high = target.maximumReps();
        Long seconds = target.seconds();
        Double metres = target.metres();
        if (high != null && high < 500) {
            low++;
            high++;
        } else if (seconds != null && seconds <= 43195) seconds += seconds >= 30 ? 5 : 1;
        else if (metres != null && metres <= 99995) metres += metres >= 20 ? 5 : 1;
        else
            return advice(
                    StrengthProgressionAdvisor.Action.HOLD,
                    "Keep this task within the supported measurement bounds.",
                    null,
                    ids);
        Double reference =
                exercise.measurement().usesLoad()
                                && exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
                        ? load
                        : null;
        return advice(
                StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "Two recent exact-exercise sessions reached the intended task at recorded effort. A"
                    + " small rep/time/distance increase at the same proven load is optional; do"
                    + " not assume added load is equivalent.",
                new GymWorkoutDefinition.Target(
                        target.sets(),
                        low,
                        high,
                        seconds,
                        metres,
                        target.effort(),
                        target.restSeconds(),
                        reference),
                ids);
    }

    private static boolean reachedMinimum(
            StrengthSession.SetEntry s, GymWorkoutDefinition.Target t) {
        return t.minimumReps() != null
                ? s.reps() >= t.minimumReps()
                : t.seconds() != null
                        ? s.durationSeconds() != null && s.durationSeconds() >= t.seconds()
                        : s.distanceMetres() != null && s.distanceMetres() >= t.metres();
    }

    private static boolean reachedTop(StrengthSession.SetEntry s, GymWorkoutDefinition.Target t) {
        return t.maximumReps() != null ? s.reps() >= t.maximumReps() : reachedMinimum(s, t);
    }

    private static Advice advice(
            StrengthProgressionAdvisor.Action action,
            String reason,
            GymWorkoutDefinition.Target target,
            List<String> ids) {
        return new Advice(action, reason, target, ids);
    }
}
