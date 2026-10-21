package org.racedaydreams.domain.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reproducible progression; every source result remains stored even when not itself a PB. */
public final class PersonalBestCalculator {
    private PersonalBestCalculator() {}

    public static List<PersonalBestEntry> history(List<RecordedResult> results) {
        Objects.requireNonNull(results, "Results");
        Map<String, RecordedResult> ids = new LinkedHashMap<>();
        Map<String, Double> distances = new HashMap<>();
        Map<String, Integer> authoritative = new HashMap<>();
        for (RecordedResult result : results) {
            Objects.requireNonNull(result, "Result");
            RecordedResult existing = ids.put(result.id(), result);
            if (existing != null && !existing.equals(result)) throw new IllegalArgumentException("Conflicting duplicate result ID: " + result.id());
            Double metres = distances.put(result.distance().code(), result.distance().metres());
            if (metres != null && Math.abs(metres - result.distance().metres()) > .000001) throw new IllegalArgumentException("One distance code cannot refer to different distances.");
            if (result.activityId() != null) {
                String key = linkedKey(result);
                authoritative.put(key, Math.max(authoritative.containsKey(key) ? authoritative.get(key) : 0, priority(result.source())));
            }
        }
        List<RecordedResult> ordered = new ArrayList<>();
        for (RecordedResult result : ids.values()) {
            if (result.activityId() != null && priority(result.source()) < authoritative.get(linkedKey(result))) continue;
            ordered.add(result);
        }
        // A date-only manual result cannot establish ordering within that day. Show its day's
        // best evidence first rather than inventing a series of same-day improvements.
        ordered.sort(Comparator.comparing(RecordedResult::date)
                .thenComparing(result -> result.distance().code())
                .thenComparing(RecordedResult::duration)
                .thenComparing(result -> -priority(result.source()))
                .thenComparing(RecordedResult::id));
        Map<String, RecordedResult> current = new HashMap<>();
        List<PersonalBestEntry> history = new ArrayList<>();
        for (RecordedResult result : ordered) {
            String key = result.distance().code();
            RecordedResult previous = current.get(key);
            if (previous == null || result.duration().compareTo(previous.duration()) < 0) {
                history.add(new PersonalBestEntry(result, previous,
                        previous == null ? null : previous.duration().minus(result.duration())));
                current.put(key, result);
            }
        }
        return Collections.unmodifiableList(history);
    }

    public static List<PersonalBestEntry> current(List<RecordedResult> results) {
        Map<String, PersonalBestEntry> current = new HashMap<>();
        for (PersonalBestEntry entry : history(results)) current.put(entry.result().distance().code(), entry);
        List<PersonalBestEntry> ordered = new ArrayList<>(current.values());
        ordered.sort(Comparator.comparingDouble(entry -> entry.result().distance().metres()));
        return Collections.unmodifiableList(ordered);
    }

    private static int priority(ResultSource source) {
        return source == ResultSource.OFFICIAL_CHIP ? 3 : source == ResultSource.MANUAL ? 2 : 1;
    }
    private static String linkedKey(RecordedResult result) { return result.activityId() + "\u0000" + result.distance().code(); }
}
