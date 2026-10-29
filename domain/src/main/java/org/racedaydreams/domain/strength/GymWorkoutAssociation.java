package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Explicit post-workout choices. Association is not a claim that every target was performed. */
public final class GymWorkoutAssociation {
    public static final int FORMAT_VERSION = 1;

    private GymWorkoutAssociation() {}

    public enum Choice {
        KEEP_EXTRA,
        COUNT_TOWARD_SESSION
    }

    /** The reviewed actual and prescription are immutable evidence, never reconstructed by date. */
    public record Proposal(
            StrengthSession actual,
            GymExecution.Origin origin,
            String programmeId,
            int programmeRevision,
            GymProgramme.Session prescription,
            Instant reviewedAt,
            StrengthProgramme.Workout authoredPrescription) {
        /** Retained legacy constructor: old prescriptions are never converted to new targets. */
        public Proposal(
                StrengthSession actual,
                GymExecution.Origin origin,
                String programmeId,
                int programmeRevision,
                GymProgramme.Session prescription,
                Instant reviewedAt) {
            this(actual, origin, programmeId, programmeRevision, prescription, reviewedAt, null);
        }

        public static Proposal authored(
                StrengthSession actual,
                GymExecution.Origin origin,
                String programmeId,
                int revision,
                StrengthProgramme.Workout prescription,
                Instant at) {
            return new Proposal(actual, origin, programmeId, revision, null, at, prescription);
        }

        public boolean authored() {
            return authoredPrescription != null;
        }

        public String sessionId() {
            return authored() ? authoredPrescription.id() : prescription.id();
        }

        public LocalDate date() {
            return authored() ? authoredPrescription.date() : prescription.date();
        }

        public String title() {
            return authored() ? authoredPrescription.definition().title() : prescription.title();
        }

        public int prescribedExerciseCount() {
            return authored()
                    ? authoredPrescription.definition().slots().size()
                    : prescription.exercises().size();
        }

        public Proposal reviewed(
                StrengthSession latestActual, GymExecution.Origin retainedOrigin, Instant at) {
            return new Proposal(
                    latestActual,
                    retainedOrigin,
                    programmeId,
                    programmeRevision,
                    prescription,
                    at,
                    authoredPrescription);
        }

        public Proposal {
            Objects.requireNonNull(actual, "reviewed actual");
            Objects.requireNonNull(origin, "original workout origin");
            if ((prescription == null) == (authoredPrescription == null))
                throw new IllegalArgumentException(
                        "Review exactly one preserved prescription family.");
            Objects.requireNonNull(reviewedAt, "review time");
            GymWorkoutDefinition.requireId(programmeId, "programme");
            if (programmeRevision < 1 || origin == GymExecution.Origin.PLANNED)
                throw new IllegalArgumentException("Choose an independent completed workout.");
            if (actual.startedAt().plus(actual.duration()).isAfter(reviewedAt))
                throw new IllegalArgumentException(
                        "Only completed actual evidence can be associated.");
        }
    }

    public record Decision(String id, Proposal proposal, Choice choice, Instant decidedAt) {
        public Decision {
            GymWorkoutDefinition.requireId(id, "association decision");
            Objects.requireNonNull(proposal, "reviewed proposal");
            Objects.requireNonNull(choice, "athlete choice");
            Objects.requireNonNull(decidedAt, "decision time");
            if (decidedAt.isBefore(proposal.reviewedAt()))
                throw new IllegalArgumentException("A decision cannot precede its review.");
        }
    }

    /**
     * Conservative discovery only: within three local days and an exact working exercise in common.
     * No similarity lookup, calendar mutation or automatic association occurs here.
     */
    public static List<Proposal> proposals(
            StrengthSession actual,
            GymExecution.Origin origin,
            GymProgramme programme,
            Set<String> occupiedSessionIds,
            Instant now) {
        Objects.requireNonNull(actual);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(occupiedSessionIds);
        Objects.requireNonNull(now);
        if (programme == null
                || origin == GymExecution.Origin.PLANNED
                || actual.startedAt().plus(actual.duration()).isAfter(now))
            return Collections.emptyList();
        var working = working(actual);
        var day = actual.startedAt().atOffset(actual.offset()).toLocalDate();
        var out = new java.util.ArrayList<Proposal>();
        for (var session : programme.sessions()) {
            if (occupiedSessionIds.contains(session.id())
                    || Math.abs(ChronoUnit.DAYS.between(day, session.date())) > 3
                    || session.exercises().stream()
                            .noneMatch(
                                    p ->
                                            working.contains(
                                                    new Task(
                                                            p.exerciseId(),
                                                            p.minimumReps() > 0,
                                                            p.seconds() != null,
                                                            p.metres() != null)))) continue;
            out.add(
                    new Proposal(
                            actual, origin, programme.id(), programme.version(), session, now));
        }
        return Immutable.copyList(out);
    }

    /** Authored targets retain nullable/RIR/fractional guidance without legacy coercion. */
    public static List<Proposal> authoredProposals(
            StrengthSession actual,
            GymExecution.Origin origin,
            StrengthProgramme programme,
            Set<String> occupiedSessionIds,
            Instant now) {
        Objects.requireNonNull(actual);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(occupiedSessionIds);
        Objects.requireNonNull(now);
        if (programme == null
                || origin == GymExecution.Origin.PLANNED
                || actual.startedAt().plus(actual.duration()).isAfter(now))
            return Collections.emptyList();
        var working = working(actual);
        var day = actual.startedAt().atOffset(actual.offset()).toLocalDate();
        var out = new java.util.ArrayList<Proposal>();
        for (var session : programme.workouts()) {
            if (occupiedSessionIds.contains(session.id())
                    || Math.abs(ChronoUnit.DAYS.between(day, session.date())) > 3
                    || session.definition().slots().stream()
                            .noneMatch(
                                    slot -> {
                                        if (slot.target() == null) return false;
                                        var task = slot.exercise().measurement();
                                        return working.contains(
                                                new Task(
                                                        slot.exercise().id(),
                                                        task.usesReps(),
                                                        task.usesDuration(),
                                                        task.usesDistance()));
                                    })) continue;
            out.add(
                    Proposal.authored(
                            actual, origin, programme.id(), programme.revision(), session, now));
        }
        return Immutable.copyList(out);
    }

    private static Set<Task> working(StrengthSession actual) {
        Set<Task> working = new HashSet<>();
        for (var block : actual.exercises())
            if (block.sets().stream().anyMatch(set -> !set.warmUp())) {
                var measurement = block.exercise().measurement();
                working.add(
                        new Task(
                                block.exercise().id(),
                                measurement.usesReps(),
                                measurement.usesDuration(),
                                measurement.usesDistance()));
            }
        return working;
    }

    public static void validateAuthoredForAcceptance(
            Proposal proposal,
            StrengthSession currentActual,
            GymExecution.Origin origin,
            StrengthProgramme programme,
            Set<String> occupied,
            boolean linked) {
        validateAuthoredRetainedEvidence(proposal, currentActual, origin, programme);
        Objects.requireNonNull(occupied);
        if (linked || occupied.contains(proposal.sessionId()))
            throw new IllegalArgumentException(
                    "This workout or target already has an association. Review it again.");
        if (!authoredProposals(currentActual, origin, programme, occupied, proposal.reviewedAt())
                .contains(proposal))
            throw new IllegalArgumentException("No comparable nearby prescription was reviewed.");
    }

    public static void validateAuthoredRetainedEvidence(
            Proposal proposal,
            StrengthSession currentActual,
            GymExecution.Origin origin,
            StrengthProgramme programme) {
        Objects.requireNonNull(proposal);
        Objects.requireNonNull(origin);
        if (!proposal.authored()
                || proposal.origin() != origin
                || !proposal.actual().equals(currentActual)
                || programme == null
                || !proposal.programmeId().equals(programme.id())
                || proposal.programmeRevision() != programme.revision()
                || !programme.workouts().contains(proposal.authoredPrescription()))
            throw new IllegalArgumentException(
                    "The reviewed actual or preserved prescription changed.");
    }

    private record Task(String exerciseId, boolean reps, boolean duration, boolean distance) {}

    /** Revalidate inside the persistence transaction before accepting the athlete's choice. */
    public static void validateForAcceptance(
            Proposal proposal,
            StrengthSession currentActual,
            GymExecution.Origin recordedOrigin,
            GymProgramme currentProgramme,
            Set<String> occupiedSessionIds,
            boolean actualLinked) {
        validateRetainedEvidence(proposal, currentActual, recordedOrigin, currentProgramme);
        Objects.requireNonNull(occupiedSessionIds);
        if (actualLinked || occupiedSessionIds.contains(proposal.prescription().id()))
            throw new IllegalArgumentException(
                    "This workout or target already has an association. Review it again.");
        if (proposals(
                        currentActual,
                        proposal.origin(),
                        currentProgramme,
                        occupiedSessionIds,
                        proposal.reviewedAt())
                .stream()
                .noneMatch(proposal::equals))
            throw new IllegalArgumentException("No comparable nearby prescription was reviewed.");
    }

    /** Keeping/unlinking an extra workout does not claim that its edited task matches a target. */
    public static void validateRetainedEvidence(
            Proposal proposal,
            StrengthSession currentActual,
            GymExecution.Origin recordedOrigin,
            GymProgramme retainedProgramme) {
        Objects.requireNonNull(proposal);
        Objects.requireNonNull(recordedOrigin, "retained workout provenance");
        if (proposal.authored()
                || proposal.origin() != recordedOrigin
                || !proposal.actual().equals(currentActual)
                || retainedProgramme == null
                || !proposal.programmeId().equals(retainedProgramme.id())
                || proposal.programmeRevision() != retainedProgramme.version()
                || retainedProgramme.sessions().stream().noneMatch(proposal.prescription()::equals))
            throw new IllegalArgumentException(
                    "The reviewed actual or preserved prescription changed.");
    }
}
