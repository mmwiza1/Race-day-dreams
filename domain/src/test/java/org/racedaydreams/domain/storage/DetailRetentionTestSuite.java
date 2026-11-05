package org.racedaydreams.domain.storage;

import org.racedaydreams.domain.storage.DetailRetention.*;

import java.time.*;
import java.util.*;

public final class DetailRetentionTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    public static void main(String[] args) {
        eq(Instant.parse("2024-11-05T12:00:00Z"), Policy.LAST_24_MONTHS.cutoff(NOW));
        eq(Instant.parse("2021-11-05T12:00:00Z"), Policy.LAST_5_YEARS.cutoff(NOW));
        eq(null, Policy.KEEP_FOREVER.cutoff(NOW));
        eq(
                Instant.parse("2023-02-28T12:00:00Z"),
                Policy.LAST_5_YEARS.cutoff(Instant.parse("2028-02-29T12:00:00Z")));
        var existing = Preferences.existing();
        eq(Choice.EXISTING_UNREVIEWED, existing.choice());
        eq(Policy.KEEP_FOREVER, existing.policy());
        var defaults = Preferences.fresh(NOW, "install");
        eq(Policy.LAST_24_MONTHS, defaults.policy());
        var old = record("old", NOW.minus(Duration.ofDays(1000)), 1000, 200, 1000L);
        var edge = record("edge", Policy.LAST_24_MONTHS.cutoff(NOW), 5, 2, 5L);
        var unknownEnd = record("unknown", null, 1000, 200, null);
        var preview =
                DetailRetention.preview(
                        Policy.LAST_24_MONTHS, List.of(old, old, edge, unknownEnd), NOW, "dataset");
        eq(List.of(old), preview.recordings());
        eq(1000L, preview.points());
        eq(200L, preview.events());
        eq(1000L, preview.knownDenseSamples());
        rejects(() -> DetailRetention.approve(preview, existing, NOW, "dataset", false, false));
        rejects(
                () ->
                        new Preview(
                                preview.policy(),
                                preview.inspectedAt(),
                                "dataset",
                                preview.recordings(),
                                999,
                                preview.events(),
                                preview.knownDenseSamples(),
                                0));
        rejects(() -> new Approval(preview, "choice", NOW.minusSeconds(1)));
        rejects(
                () ->
                        new Preferences(
                                Policy.LAST_24_MONTHS, Choice.EXISTING_UNREVIEWED, null, null));
        var choice = Preferences.acknowledged(Policy.LAST_24_MONTHS, NOW, "choice");
        var approval = DetailRetention.approve(preview, choice, NOW, "dataset", false, false);
        DetailRetention.checkBatch(approval, choice, NOW, "dataset", List.of(old), false, false);
        checks++;
        DetailRetention.checkBatch(approval, choice, NOW, "dataset", List.of(), false, false);
        checks++;
        rejects(() -> DetailRetention.approve(preview, choice, NOW, "dataset", true, false));
        rejects(() -> DetailRetention.approve(preview, choice, NOW, "dataset", false, true));
        rejects(() -> DetailRetention.approve(preview, choice, NOW, "replaced", false, false));
        rejects(
                () ->
                        DetailRetention.approve(
                                preview, choice, NOW.minusSeconds(1), "dataset", false, false));
        rejects(
                () ->
                        DetailRetention.approve(
                                preview,
                                choice,
                                NOW.plus(Duration.ofDays(2)),
                                "dataset",
                                false,
                                false));
        rejects(
                () ->
                        DetailRetention.checkBatch(
                                approval,
                                choice,
                                NOW,
                                "dataset",
                                List.of(record("old", old.endedAt(), 999, 200, 1000L)),
                                false,
                                false));
        rejects(
                () ->
                        DetailRetention.checkBatch(
                                approval, choice, NOW, "dataset", List.of(old, old), false, false));
        rejects(
                () ->
                        DetailRetention.checkBatch(
                                approval,
                                Preferences.acknowledged(Policy.LAST_24_MONTHS, NOW, "new-choice"),
                                NOW,
                                "dataset",
                                List.of(old),
                                false,
                                false));
        rejects(
                () ->
                        DetailRetention.checkBatch(
                                approval,
                                Preferences.acknowledged(Policy.KEEP_FOREVER, NOW, "forever"),
                                NOW,
                                "dataset",
                                List.of(old),
                                false,
                                false));
        var unknown =
                DetailRetention.preview(
                        Policy.LAST_24_MONTHS,
                        List.of(record("unknown-samples", old.endedAt(), 1000, 0, null)),
                        NOW,
                        "dataset");
        eq(1, unknown.unknownSampleCounts());
        eq(0L, unknown.knownDenseSamples());
        for (String provider : List.of("health-connect", "manual", "external", "rdd", "gpx"))
            for (boolean finished : List.of(false, true)) {
                var row =
                        new Recording(
                                "s", "a", provider, "s", old.endedAt(), finished, 10, 10, 10L);
                int expected =
                        finished && (provider.equals("rdd") || provider.equals("gpx")) ? 1 : 0;
                eq(
                        expected,
                        DetailRetention.preview(Policy.LAST_24_MONTHS, List.of(row), NOW, "dataset")
                                .recordings()
                                .size());
            }
        eq(
                0,
                DetailRetention.preview(
                                Policy.LAST_24_MONTHS,
                                List.of(
                                        new Recording(
                                                "s",
                                                "a",
                                                "rdd",
                                                "different-source",
                                                old.endedAt(),
                                                true,
                                                10,
                                                10,
                                                10L)),
                                NOW,
                                "dataset")
                        .recordings()
                        .size());
        eq(
                0,
                DetailRetention.preview(
                                Policy.LAST_24_MONTHS,
                                List.of(record("already-empty", old.endedAt(), 0, 0, 0L)),
                                NOW,
                                "dataset")
                        .recordings()
                        .size());
        eq(
                0,
                DetailRetention.preview(Policy.KEEP_FOREVER, List.of(old), NOW, "dataset")
                        .recordings()
                        .size());
        eq(
                0,
                DetailRetention.preview(Policy.LAST_5_YEARS, List.of(old), NOW, "dataset")
                        .recordings()
                        .size());
        rejects(
                () ->
                        DetailRetention.preview(
                                Policy.LAST_24_MONTHS,
                                List.of(old, record("old", old.endedAt(), 2, 2, 2L)),
                                NOW,
                                "dataset"));
        rejects(
                () ->
                        DetailRetention.preview(
                                Policy.LAST_24_MONTHS,
                                List.of(
                                        old,
                                        new Recording(
                                                "alias",
                                                old.activityId(),
                                                "rdd",
                                                "alias",
                                                old.endedAt(),
                                                true,
                                                10,
                                                10,
                                                10L)),
                                NOW,
                                "dataset"));
        var many = new ArrayList<Recording>();
        for (int i = 0; i < 5000; i++)
            many.add(record("run-" + i, NOW.minus(Duration.ofDays(i)), 3600, 120, 3600L));
        for (var policy : Policy.values()) {
            var value = DetailRetention.preview(policy, many, NOW, "dataset");
            for (var row : value.recordings())
                check(
                        row.endedAt().isBefore(policy.cutoff(NOW)),
                        "Only genuinely old completed observations selected");
            eq(value.recordings().size() * 3600L, value.points());
            eq(value.recordings().size() * 120L, value.events());
        }
        System.out.println("DetailRetentionTestSuite: " + checks + " checks passed.");
    }

    private static Recording record(
            String id, Instant end, long points, long events, Long samples) {
        return new Recording(id, "activity:" + id, "rdd", id, end, true, points, events, samples);
    }

    private static void eq(Object a, Object b) {
        check(Objects.equals(a, b), a + " != " + b);
    }

    private static void check(boolean v, String why) {
        checks++;
        if (!v) throw new AssertionError(why);
    }

    private static void rejects(Runnable action) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException | ArithmeticException expected) {
            return;
        }
        throw new AssertionError("Expected safe rejection");
    }
}
