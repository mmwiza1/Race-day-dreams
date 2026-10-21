package org.racedaydreams.domain.baseline;

import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.RecordedResult;
import org.racedaydreams.domain.activity.ResultSource;
import org.racedaydreams.domain.activity.PersonalBestCalculator;
import org.racedaydreams.domain.activity.PersonalBestEntry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Deterministic summaries of available evidence, without inventing workouts or missing metrics. */
public final class BaselineEngine {
    private BaselineEngine() { }

    public static RunnerBaseline derive(List<ActivityRecord> activities, Instant from, Instant to) {
        return derive(activities, Collections.emptyList(), from, to);
    }

    public static RunnerBaseline derive(List<ActivityRecord> activities, List<RecordedResult> results,
                                        Instant from, Instant to) {
        RunnerBaseline empty = RunnerBaseline.empty(from, to);
        Objects.requireNonNull(activities, "activities"); Objects.requireNonNull(results, "results");
        ArrayList<ActivityRecord> runs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ActivityRecord run : activities) {
            Objects.requireNonNull(run, "activity");
            // Stored source identity is the deduplication authority; equal visible totals are not.
            String identity = run.source().provider() + "\u0000" + run.source().recordId();
            if (!run.start().isBefore(from) && run.start().isBefore(to) && seen.add(identity)) runs.add(run);
        }
        runs.sort(Comparator.comparing(ActivityRecord::start).thenComparing(ActivityRecord::id));
        int possibleMirrors = collapseExactMirrors(runs);
        Map<String, BaselineProvenance> provenance = new LinkedHashMap<>();
        ArrayList<String> explanations = new ArrayList<>();
        ArrayList<WeeklyRunningSummary> history = new ArrayList<>();
        ArrayList<RecordedTrainingGap> gaps = new ArrayList<>();
        TreeMap<LocalDate, Week> weeks = new TreeMap<>();
        ArrayList<Double> durations = new ArrayList<>();
        Double longest = null;
        int measuredDistances = 0;
        BaselineProvenance origin = combinedOrigin(runs);
        for (ActivityRecord run : runs) {
            ZoneOffset offset = run.startOffset() == null ? ZoneOffset.UTC : run.startOffset();
            LocalDate monday = run.start().atOffset(offset).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            Week week = weeks.get(monday);
            if (week == null) {
                Instant weekStart = monday.atStartOfDay().toInstant(offset);
                week = new Week(!weekStart.isBefore(from) && !weekStart.plus(7, ChronoUnit.DAYS).isAfter(to));
                weeks.put(monday, week);
            }
            week.count++;
            if (run.distanceMetres() == null) week.hasMissingDistance = true;
            else {
                measuredDistances++;
                week.distanceKm += run.distanceMetres() / 1000.0;
                longest = longest == null ? run.distanceMetres() / 1000.0 : Math.max(longest, run.distanceMetres() / 1000.0);
            }
            Duration elapsed = Duration.between(run.start(), run.end());
            durations.add((elapsed.getSeconds() + elapsed.getNano() / 1_000_000_000.0) / 60.0);
        }
        Double weeklyDistance = null;
        Double frequency = null;
        double distanceSum = 0;
        int fullDistanceWeeks = 0;
        int fullRunWeeks = 0;
        int runCount = 0;
        for (Map.Entry<LocalDate, Week> entry : weeks.entrySet()) {
            Week week = entry.getValue();
            Double distance = week.hasMissingDistance ? null : week.distanceKm;
            history.add(new WeeklyRunningSummary(entry.getKey(), week.count, distance, week.complete));
            if (week.complete) {
                fullRunWeeks++; runCount += week.count;
                if (distance != null) { fullDistanceWeeks++; distanceSum += distance; }
            }
        }
        // One partial week is deliberately insufficient to prescribe a habitual weekly load.
        if (fullDistanceWeeks >= 2) weeklyDistance = distanceSum / fullDistanceWeeks;
        if (fullRunWeeks >= 2) frequency = runCount / (double) fullRunWeeks;
        Integer consistent = runs.isEmpty() ? null : consecutiveRecordedWeeks(weeks);
        for (int index = 1; index < runs.size(); index++) {
            long days = Duration.between(runs.get(index - 1).start(), runs.get(index).start()).toDays();
            if (days >= 10) gaps.add(new RecordedTrainingGap(runs.get(index - 1).start(), runs.get(index).start(), days));
        }
        Double typical = durations.isEmpty() ? null : median(durations);
        if (weeklyDistance != null) provenance.put(RunnerBaseline.WEEKLY_DISTANCE, origin);
        if (frequency != null) provenance.put(RunnerBaseline.RUNS_PER_WEEK, origin);
        if (longest != null) provenance.put(RunnerBaseline.LONGEST_RUN, origin);
        if (typical != null) provenance.put(RunnerBaseline.TYPICAL_DURATION, origin);
        if (consistent != null) provenance.put(RunnerBaseline.CONSISTENT_WEEKS, origin);

        Duration recent5k = resultTime(results, 5000, from, to, provenance, RunnerBaseline.RECENT_5K);
        Duration recent10k = resultTime(results, 10000, from, to, provenance, RunnerBaseline.RECENT_10K);
        Duration recentHalf = resultTime(results, 21097.5, from, to, provenance, RunnerBaseline.RECENT_HALF);
        Instant latest = runs.isEmpty() ? null : runs.get(runs.size() - 1).start();
        boolean recent = latest != null && Duration.between(latest, to).compareTo(Duration.ofDays(14)) <= 0;
        double distanceCoverage = runs.isEmpty() ? 0 : measuredDistances / (double) runs.size();
        BaselineConfidence confidence = BaselineConfidence.LOW;
        if (runs.size() >= 6 && fullRunWeeks >= 3 && distanceCoverage >= 0.75 && recent) confidence = BaselineConfidence.MODERATE;
        if (runs.size() >= 16 && fullRunWeeks >= 6 && distanceCoverage >= 0.90 && recent) confidence = BaselineConfidence.STRONG;
        if (runs.isEmpty()) explanations.addAll(empty.explanations());
        else {
            explanations.add("Found " + runs.size() + " recorded runs in the selected history window. These are records, not proof of all training completed.");
            if (possibleMirrors > 0) explanations.add(possibleMirrors + " possible mirrored recordings shared exact start/end times and matching distance. They were counted once for this baseline; original activities remain stored for review.");
            if (weeklyDistance != null) explanations.add("Weekly distance averages observed complete weeks with recorded distance. Weeks without records are not counted as zero.");
            else explanations.add("There are not yet two complete observed weeks with distance, so habitual weekly mileage remains unknown.");
            if (measuredDistances < runs.size()) explanations.add("Some runs have no distance. Missing distances have not been replaced with zero.");
            explanations.add("Typical duration uses elapsed session time; moving time is kept separate.");
            explanations.add("Weekly grouping uses each run's recorded start offset, or explicitly UTC when its offset is unavailable.");
            if (!gaps.isEmpty()) explanations.add("There are gaps in recorded runs. They may reflect training changes or incomplete synchronisation.");
            if (!recent) explanations.add("The latest recorded run is more than two weeks old; current baseline confidence is reduced.");
            if (fullRunWeeks >= 4) explanations.add(trend(history));
        }
        explanations.add("Easy pace is not inferred from session averages without evidence of easy effort. PBs require explicit results or qualified rolling efforts.");
        explanations.add(confidence == BaselineConfidence.STRONG
                ? "Several recent weeks support a stronger baseline; this is evidence quality, not certainty about race performance."
                : confidence == BaselineConfidence.MODERATE
                ? "Several recorded runs support a moderate baseline; more consistent history will improve it."
                : "The baseline is provisional. A short assessment alone does not establish endurance.");
        return new RunnerBaseline(weeklyDistance, frequency, longest, typical, null, consistent,
                recent5k, recent10k, recentHalf, runs.size(), weeks.size(), from, to, latest,
                confidence, provenance, explanations, history, gaps);
    }

    /** Null manual fields leave imported values intact. A provided value is an explicit correction. */
    public static RunnerBaseline applyManualOverlay(RunnerBaseline derived, ManualBaselineInput manual) {
        return applyManualOverlay(derived, manual, null);
    }

    /** recordedAt is the explicit edit/review instant, never fabricated from app reload time. */
    public static RunnerBaseline applyManualOverlay(RunnerBaseline derived, ManualBaselineInput manual, Instant recordedAt) {
        Objects.requireNonNull(derived, "derived"); Objects.requireNonNull(manual, "manual");
        Map<String, BaselineProvenance> provenance = new LinkedHashMap<>(derived.provenance());
        mark(provenance, RunnerBaseline.WEEKLY_DISTANCE, manual.weeklyDistanceKm());
        mark(provenance, RunnerBaseline.RUNS_PER_WEEK, manual.runsPerWeek());
        mark(provenance, RunnerBaseline.LONGEST_RUN, manual.longestRunKm());
        mark(provenance, RunnerBaseline.EASY_PACE, manual.easyPaceSecondsPerKm());
        mark(provenance, RunnerBaseline.CONSISTENT_WEEKS, manual.consistentWeeks());
        mark(provenance, RunnerBaseline.RECENT_5K, manual.recent5k());
        mark(provenance, RunnerBaseline.RECENT_10K, manual.recent10k());
        mark(provenance, RunnerBaseline.RECENT_HALF, manual.recentHalf());
        ArrayList<String> reasons = new ArrayList<>(derived.explanations());
        BaselineConfidence confidence = derived.confidence();
        if (manual.hasAnyValue()) reasons.add("Your corrections and optional answers are marked as manually entered. They do not fabricate workout history or increase measured-data confidence.");
        if (confidence == BaselineConfidence.STRONG && materiallyDifferent(manual.weeklyDistanceKm(), derived.weeklyDistanceKm())) {
            confidence = BaselineConfidence.MODERATE;
            reasons.add("Your weekly-distance correction differs substantially from recorded history. The combined baseline remains provisional while the difference is reviewed.");
        }
        return new RunnerBaseline(or(manual.weeklyDistanceKm(), derived.weeklyDistanceKm()),
                or(manual.runsPerWeek(), derived.runsPerWeek()), or(manual.longestRunKm(), derived.longestRunKm()),
                derived.typicalRunMinutes(), or(manual.easyPaceSecondsPerKm(), derived.easyPaceSecondsPerKm()),
                or(manual.consistentWeeks(), derived.consistentWeeks()), or(manual.recent5k(), derived.recent5k()),
                or(manual.recent10k(), derived.recent10k()), or(manual.recentHalf(), derived.recentHalf()),
                derived.activityCount(), derived.weeksWithRuns(), derived.windowStart(), derived.windowEnd(),
                derived.lastActivityAt(), confidence, provenance, reasons, derived.weeklyHistory(), derived.gaps(),
                manual.hasAnyValue() ? recordedAt : derived.manualRecordedAt());
    }

    private static <T> T or(T manual, T recorded) { return manual != null ? manual : recorded; }
    private static void mark(Map<String, BaselineProvenance> values, String key, Object value) {
        if (value != null) values.put(key, BaselineProvenance.MANUAL);
    }

    private static boolean materiallyDifferent(Double manual, Double recorded) {
        return manual != null && recorded != null && Math.abs(manual - recorded) > Math.max(5, recorded * 0.25);
    }

    private static int collapseExactMirrors(List<ActivityRecord> runs) {
        int mirrors = 0;
        ArrayList<ActivityRecord> unique = new ArrayList<>();
        for (ActivityRecord candidate : runs) {
            boolean duplicate = false;
            for (int index = unique.size() - 1; index >= 0; index--) {
                ActivityRecord prior = unique.get(index);
                if (!prior.start().equals(candidate.start())) break;
                boolean otherOrigin = !Objects.equals(prior.source().originPackage(), candidate.source().originPackage())
                        || !prior.source().provider().equals(candidate.source().provider());
                if (otherOrigin && prior.end().equals(candidate.end()) && prior.distanceMetres() != null
                        && candidate.distanceMetres() != null && Math.abs(prior.distanceMetres() - candidate.distanceMetres()) <= 1.0) {
                    duplicate = true; break;
                }
            }
            if (duplicate) mirrors++; else unique.add(candidate);
        }
        runs.clear(); runs.addAll(unique);
        return mirrors;
    }

    private static Duration resultTime(List<RecordedResult> results, double metres, Instant from,
            Instant to, Map<String, BaselineProvenance> provenance, String field) {
        LocalDate first = from.atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDate last = to.minusNanos(1).atOffset(ZoneOffset.UTC).toLocalDate();
        ArrayList<RecordedResult> relevant = new ArrayList<>();
        for (RecordedResult result : results) {
            if (Math.abs(result.distance().metres() - metres) > 0.001 || result.date().isBefore(first) || result.date().isAfter(last)) continue;
            relevant.add(result);
        }
        List<PersonalBestEntry> current = PersonalBestCalculator.current(relevant);
        if (!current.isEmpty()) {
            RecordedResult result = current.get(0).result();
            provenance.put(field, result.source() == ResultSource.ROLLING ? BaselineProvenance.INFERRED : BaselineProvenance.MANUAL);
            return result.duration();
        }
        return null;
    }

    private static BaselineProvenance combinedOrigin(List<ActivityRecord> runs) {
        BaselineProvenance result = null;
        for (ActivityRecord run : runs) {
            String provider = run.source().provider();
            BaselineProvenance current = "health-connect".equals(provider) ? BaselineProvenance.HEALTH_CONNECT
                    : "assessment".equals(provider) ? BaselineProvenance.ASSESSMENT
                    : "manual".equals(provider) ? BaselineProvenance.MANUAL : BaselineProvenance.INFERRED;
            if (result == null) result = current;
            else if (result != current) return BaselineProvenance.INFERRED;
        }
        return result == null ? BaselineProvenance.INFERRED : result;
    }

    private static int consecutiveRecordedWeeks(TreeMap<LocalDate, Week> weeks) {
        int current = 0;
        LocalDate previous = null;
        for (LocalDate week : weeks.keySet()) {
            current = previous != null && week.equals(previous.plusWeeks(1)) ? current + 1 : 1;
            previous = week;
        }
        return current;
    }

    private static double median(List<Double> values) {
        Collections.sort(values);
        int middle = values.size() / 2;
        return values.size() % 2 == 1 ? values.get(middle) : (values.get(middle - 1) + values.get(middle)) / 2;
    }

    private static String trend(List<WeeklyRunningSummary> history) {
        ArrayList<Double> usable = new ArrayList<>();
        for (WeeklyRunningSummary week : history) if (week.completeWindowWeek() && week.distanceKm() != null) usable.add(week.distanceKm());
        if (usable.size() < 4) return "There are not yet enough complete distance weeks to describe a trend.";
        int middle = usable.size() / 2;
        double early = 0, late = 0;
        for (int index = 0; index < usable.size(); index++) {
            if (index < middle) early += usable.get(index); else late += usable.get(index);
        }
        early /= middle; late /= usable.size() - middle;
        if (early == 0) return "Recorded weekly distance is now present after earlier observed zero-distance runs; a percentage trend would be misleading.";
        double ratio = late / early;
        return ratio > 1.15 ? "Recorded distance in later observed weeks is higher than earlier observed weeks."
                : ratio < 0.85 ? "Recorded distance in later observed weeks is lower than earlier observed weeks."
                : "Recorded distance is broadly similar across the observed complete weeks.";
    }

    private static final class Week {
        int count;
        double distanceKm;
        boolean hasMissingDistance;
        final boolean complete;
        Week(boolean complete) { this.complete = complete; }
    }
}
