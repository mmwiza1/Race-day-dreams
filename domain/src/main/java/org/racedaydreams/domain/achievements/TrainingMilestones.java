package org.racedaydreams.domain.achievements;

import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.StrengthSession;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Restrained milestones from accepted actuals, including eligible retained history. No streaks. */
public final class TrainingMilestones {
    private TrainingMilestones() {}

    public enum Source {
        RUN,
        GYM
    }

    public enum Kind {
        FIRST_GYM(1, "First recorded strength workout"),
        FIVE_GYM(5, "Five recorded strength workouts"),
        FIFTY_GYM(50, "Fifty recorded strength workouts"),
        FIRST_RUN(1, "First recorded run"),
        TEN_RUNS(10, "Ten recorded runs"),
        FIRST_FIVE_KM_RUN(1, "A recorded run of at least 5 km"),
        TWELVE_TRAINING_WEEKS(12, "Training recorded across twelve weeks");
        private final int count;
        private final String title;

        Kind(int count, String title) {
            this.count = count;
            this.title = title;
        }

        public int count() {
            return count;
        }

        public String title() {
            return title;
        }

        public String explanation() {
            return switch (this) {
                case FIRST_GYM, FIVE_GYM, FIFTY_GYM ->
                        "Based on completed recorded strength workouts with at least one working"
                                + " set. Warm-up-only sessions and manual PB entries do not count.";
                case FIRST_RUN, TEN_RUNS ->
                        "Based on retained completed running records. These are recorded"
                                + " activities, not a claim of independently verified physical"
                                + " performance.";
                case FIRST_FIVE_KM_RUN ->
                        "A completed running record has at least 5,000 metres of recorded distance."
                                + " This does not assert an official race result or universal GPS"
                                + " accuracy.";
                case TWELVE_TRAINING_WEEKS ->
                        "Training appears in twelve distinct Monday-based weeks. The weeks need not"
                                + " be consecutive; rest, breaks and missed days carry no penalty.";
            };
        }
    }

    public record Proof(
            Source source, String recordId, Instant at, ZoneOffset offset, Double distanceMetres) {
        public Proof {
            Objects.requireNonNull(source);
            Objects.requireNonNull(at);
            if (recordId == null || recordId.trim().isEmpty() || recordId.length() > 256)
                throw new IllegalArgumentException("Milestone source identity required.");
            if (distanceMetres != null && (!Double.isFinite(distanceMetres) || distanceMetres < 0))
                throw new IllegalArgumentException("Invalid retained distance.");
            if (source == Source.GYM && distanceMetres != null)
                throw new IllegalArgumentException(
                        "Gym workouts are not running distance evidence.");
        }

        public LocalDate week() {
            return offset == null
                    ? null
                    : at.atOffset(offset)
                            .toLocalDate()
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
    }

    public record Award(Kind kind, Instant earnedAt, List<Proof> evidence) {
        public Award {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(earnedAt);
            evidence = Immutable.copyList(evidence);
            if (evidence.size() != kind.count())
                throw new IllegalArgumentException("Milestone evidence count differs.");
            var sources = new HashSet<String>();
            var weeks = new HashSet<LocalDate>();
            Instant last = null;
            for (var proof : evidence) {
                if (!sources.add(proof.source().name() + ":" + proof.recordId()))
                    throw new IllegalArgumentException("Duplicate milestone source.");
                if (last != null && proof.at().isBefore(last))
                    throw new IllegalArgumentException("Milestone evidence must be chronological.");
                last = proof.at();
                weeks.add(proof.week());
                if ((kind == Kind.FIRST_GYM || kind == Kind.FIVE_GYM || kind == Kind.FIFTY_GYM)
                        && proof.source() != Source.GYM)
                    throw new IllegalArgumentException(
                            "Strength milestone requires strength evidence.");
                if ((kind == Kind.FIRST_RUN
                                || kind == Kind.TEN_RUNS
                                || kind == Kind.FIRST_FIVE_KM_RUN)
                        && proof.source() != Source.RUN)
                    throw new IllegalArgumentException(
                            "Running milestone requires running evidence.");
                if (kind == Kind.FIRST_FIVE_KM_RUN
                        && (proof.distanceMetres() == null || proof.distanceMetres() < 5000))
                    throw new IllegalArgumentException(
                            "Distance milestone requires retained distance.");
            }
            if (!earnedAt.equals(last)
                    || kind == Kind.TWELVE_TRAINING_WEEKS
                            && (weeks.size() != 12 || weeks.contains(null)))
                throw new IllegalArgumentException("Milestone chronology differs.");
        }

        public String id() {
            return kind.name();
        }
    }

    public static List<Award> eligible(
            List<StrengthSession> strength, List<ActivityRecord> runs, Instant now) {
        Objects.requireNonNull(now);
        var gym = new ArrayList<Proof>();
        var running = new ArrayList<Proof>();
        var seenGym = new HashMap<String, StrengthSession>();
        var seenRuns = new HashMap<String, ActivityRecord>();
        for (var session : strength) {
            var old = seenGym.putIfAbsent(session.id(), session);
            if (old != null) {
                if (!old.equals(session))
                    throw new IllegalArgumentException("Conflicting actuals.");
                continue;
            }
            if (!session.startedAt().isAfter(now)
                    && session.exercises().stream()
                            .anyMatch(e -> e.sets().stream().anyMatch(s -> !s.warmUp())))
                gym.add(
                        new Proof(
                                Source.GYM,
                                session.id(),
                                session.startedAt(),
                                session.offset(),
                                null));
        }
        for (var run : runs) {
            var old = seenRuns.putIfAbsent(run.id(), run);
            if (old != null) {
                if (!old.equals(run))
                    throw new IllegalArgumentException("Conflicting running evidence.");
                continue;
            }
            if (!run.end().isAfter(now))
                running.add(
                        new Proof(
                                Source.RUN,
                                run.id(),
                                run.start(),
                                run.startOffset(),
                                run.distanceMetres()));
        }
        var order =
                Comparator.comparing(Proof::at)
                        .thenComparing(p -> p.source().name())
                        .thenComparing(Proof::recordId);
        gym.sort(order);
        running.sort(order);
        var result = new ArrayList<Award>();
        for (var kind : Kind.values()) {
            List<Proof> source;
            if (kind == Kind.TWELVE_TRAINING_WEEKS) {
                var all = new ArrayList<Proof>(gym);
                all.addAll(running);
                all.sort(order);
                var byWeek = new LinkedHashMap<LocalDate, Proof>();
                for (var proof : all)
                    if (proof.week() != null) byWeek.putIfAbsent(proof.week(), proof);
                source = new ArrayList<>(byWeek.values());
            } else if (kind == Kind.FIRST_FIVE_KM_RUN) {
                source = new ArrayList<>();
                for (var proof : running)
                    if (proof.distanceMetres() != null && proof.distanceMetres() >= 5000)
                        source.add(proof);
            } else source = kind.name().contains("GYM") ? gym : running;
            if (source.size() >= kind.count()) {
                var proof = Immutable.copyList(source.subList(0, kind.count()));
                result.add(new Award(kind, proof.get(proof.size() - 1).at(), proof));
            }
        }
        return Immutable.copyList(result);
    }
}
