package org.racedaydreams.domain.achievements;

import org.racedaydreams.domain.achievements.TrainingMilestones.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

public final class TrainingMilestonesTestSuite {
    static int checks;
    static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    public static void main(String[] args) {
        eq(0, TrainingMilestones.eligible(List.of(), List.of(), NOW).size());
        eq(0, TrainingMilestones.eligible(List.of(gym("warm", NOW, true)), List.of(), NOW).size());
        var strength = new ArrayList<StrengthSession>();
        var runs = new ArrayList<ActivityRecord>();
        for (int i = 0; i < 78; i++) {
            strength.add(gym("gym-" + i, NOW.minus(Duration.ofDays(i * 2L)), false));
            runs.add(
                    run(
                            "run-" + i,
                            NOW.minus(Duration.ofDays(i * 2L + 1)),
                            i == 20 ? 5000.0 : null,
                            ZoneOffset.UTC));
        }
        var awarded = TrainingMilestones.eligible(strength, runs, NOW);
        eq(7, awarded.size());
        var reversed = new ArrayList<>(strength);
        Collections.reverse(reversed);
        var reversedRuns = new ArrayList<>(runs);
        Collections.reverse(reversedRuns);
        eq(awarded, TrainingMilestones.eligible(reversed, reversedRuns, NOW));
        strength.add(strength.get(0));
        runs.add(runs.get(0));
        eq(awarded, TrainingMilestones.eligible(strength, runs, NOW));
        for (var a : awarded) {
            eq(a.kind().count(), a.evidence().size());
            check(!a.earnedAt().isAfter(NOW));
            eq(a.earnedAt(), a.evidence().get(a.evidence().size() - 1).at());
        }
        eq(
                0,
                TrainingMilestones.eligible(
                                List.of(gym("future", NOW.plusSeconds(1), false)),
                                List.of(run("ongoing", NOW, 6000.0, ZoneOffset.UTC)),
                                NOW)
                        .size());
        var unknownOffset = new ArrayList<ActivityRecord>();
        for (int i = 1; i <= 26; i++)
            unknownOffset.add(run("unknown-" + i, NOW.minus(Duration.ofDays(i * 7L)), null, null));
        check(
                TrainingMilestones.eligible(List.of(), unknownOffset, NOW).stream()
                        .noneMatch(a -> a.kind() == Kind.TWELVE_TRAINING_WEEKS));
        check(
                TrainingMilestones.eligible(List.of(), unknownOffset, NOW).stream()
                        .anyMatch(a -> a.kind() == Kind.TEN_RUNS));
        rejects(
                () ->
                        new Award(
                                Kind.FIVE_GYM,
                                NOW,
                                List.of(new Proof(Source.GYM, "one", NOW, ZoneOffset.UTC, null))));
        rejects(
                () ->
                        new Award(
                                Kind.FIRST_FIVE_KM_RUN,
                                NOW,
                                List.of(new Proof(Source.RUN, "one", NOW, ZoneOffset.UTC, null))));
        rejects(
                () ->
                        new Award(
                                Kind.FIRST_GYM,
                                NOW,
                                List.of(
                                        new Proof(
                                                Source.RUN, "one", NOW, ZoneOffset.UTC, 6000.0))));
        rejects(
                () ->
                        new Award(
                                Kind.FIRST_RUN,
                                NOW.plusSeconds(1),
                                List.of(new Proof(Source.RUN, "one", NOW, ZoneOffset.UTC, null))));
        System.out.println("TrainingMilestonesTestSuite: " + checks + " checks passed.");
    }

    static StrengthSession gym(String id, Instant at, boolean warm) {
        return new StrengthSession(
                id,
                at,
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "",
                List.of(
                        new StrengthSession.ExerciseSets(
                                ExerciseCatalog.definition("push-up").exercise(),
                                List.of(new StrengthSession.SetEntry("set", 6, 0, null, warm)))));
    }

    static ActivityRecord run(String id, Instant at, Double distance, ZoneOffset offset) {
        return new ActivityRecord(
                id,
                new SourceIdentity("native", id, null),
                at,
                at.plusSeconds(1800),
                offset,
                offset,
                distance,
                null,
                null,
                null,
                null,
                null,
                null,
                "",
                List.of(),
                List.of(),
                null,
                null);
    }

    static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("Incorrect milestone evidence");
    }

    static void eq(Object a, Object b) {
        checks++;
        if (!Objects.equals(a, b)) throw new AssertionError(a + " != " + b);
    }

    static void rejects(Runnable r) {
        checks++;
        try {
            r.run();
            throw new AssertionError("Invalid recognition accepted");
        } catch (IllegalArgumentException expected) {
        }
    }
}
