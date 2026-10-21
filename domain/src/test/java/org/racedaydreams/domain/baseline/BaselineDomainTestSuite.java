package org.racedaydreams.domain.baseline;

import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;

import java.time.*;
import java.util.*;

/** Dependency-free assertions for evidence quality, manual fallback and optional assessments. */
public final class BaselineDomainTestSuite {
    private static final Instant FROM = Instant.parse("2025-01-06T00:00:00Z");
    private static final Instant TO = FROM.plus(Duration.ofDays(84));
    private static int checks;

    public static void main(String[] args) {
        emptyAndPartialHistory();
        recordedHistoryAndQuality();
        manualFallbackAndFreshness();
        performanceUsesEvidence();
        mixedEvidenceDeadlines();
        assessmentChoicesAndAnalysis();
        resultAndTimeZoneBoundaries();
        immutabilityAndInputBoundaries();
        regressionFixtureBoundary();
        System.out.println("BaselineDomainTestSuite: " + checks + " checks passed.");
    }

    private static void emptyAndPartialHistory() {
        RunnerBaseline empty = BaselineEngine.derive(Collections.emptyList(), FROM, TO);
        eq(null, empty.weeklyDistanceKm()); eq(null, empty.runsPerWeek());
        eq(null, empty.longestRunKm()); eq(null, empty.typicalRunMinutes());
        eq(null, empty.easyPaceSecondsPerKm()); eq(null, empty.recent5k());
        eq(null, empty.lastActivityAt()); eq(0, empty.activityCount());
        eq(BaselineConfidence.LOW, empty.confidence());
        check(!PerformanceEligibilityEvaluator.evaluate(empty, TO).eligible());
        check(empty.explanations().stream().anyMatch(reason -> reason.contains("No running history")));
        RunnerBaseline one = BaselineEngine.derive(Collections.singletonList(activity("one", TO.minus(Duration.ofDays(2)), 1000.0, 400, ZoneOffset.UTC)), FROM, TO);
        eq(1, one.activityCount()); near(1, one.longestRunKm());
        eq(null, one.weeklyDistanceKm()); eq(null, one.runsPerWeek()); eq(null, one.recent5k());
        eq(BaselineConfidence.LOW, one.confidence());
        RunnerBaseline noneAvailable = BaselineEngine.applyManualOverlay(empty, ManualBaselineInput.empty());
        eq(empty, noneAvailable); check(!ManualBaselineInput.empty().hasAnyValue());
        // Denied/unavailable providers supply no activities, not all-zero health measurements.
        eq(null, noneAvailable.weeklyDistanceKm());
    }

    private static void recordedHistoryAndQuality() {
        List<ActivityRecord> runs = regularRuns(4, 8, 3, 10.0);
        RunnerBaseline baseline = BaselineEngine.derive(runs, FROM, TO);
        eq(24, baseline.activityCount()); eq(8, baseline.weeksWithRuns());
        near(30, baseline.weeklyDistanceKm()); near(3, baseline.runsPerWeek());
        near(10, baseline.longestRunKm()); near(60, baseline.typicalRunMinutes()); eq(8, baseline.consistentWeeks());
        eq(BaselineConfidence.STRONG, baseline.confidence());
        eq(BaselineProvenance.HEALTH_CONNECT, baseline.provenance().get(RunnerBaseline.WEEKLY_DISTANCE));
        eq(null, baseline.easyPaceSecondsPerKm()); eq(null, baseline.recent5k());
        check(baseline.explanations().stream().anyMatch(reason -> reason.contains("not counted as zero")));
        ArrayList<ActivityRecord> duplicateIds = new ArrayList<>(runs); duplicateIds.add(runs.get(0));
        eq(24, BaselineEngine.derive(duplicateIds, FROM, TO).activityCount());
        ActivityRecord first = runs.get(0);
        ActivityRecord mirror = new ActivityRecord("mirror", new SourceIdentity("health-connect", "mirror-id", "other.sync"),
                first.start(), first.end(), first.startOffset(), first.endOffset(), first.distanceMetres(), null,
                null, null, null, null, null, null, Collections.emptyList(), Collections.emptyList(), null, null);
        ArrayList<ActivityRecord> mirrored = new ArrayList<>(runs); mirrored.add(mirror);
        RunnerBaseline mirrorBaseline = BaselineEngine.derive(mirrored, FROM, TO);
        eq(24, mirrorBaseline.activityCount()); eq(25, mirrored.size());
        check(mirrorBaseline.explanations().stream().anyMatch(reason -> reason.contains("possible mirrored")));
        ArrayList<ActivityRecord> missingDistances = new ArrayList<>();
        for (ActivityRecord run : runs) missingDistances.add(activity(run.id(), run.start(), null, 1800, ZoneOffset.UTC));
        RunnerBaseline missing = BaselineEngine.derive(missingDistances, FROM, TO);
        eq(null, missing.weeklyDistanceKm()); eq(null, missing.longestRunKm());
        near(3, missing.runsPerWeek()); near(30, missing.typicalRunMinutes());
        eq(BaselineConfidence.LOW, missing.confidence());
        check(missing.weeklyHistory().stream().allMatch(week -> week.distanceKm() == null));
        List<ActivityRecord> gaps = Arrays.asList(
                activity("gap-a", FROM.plus(Duration.ofDays(2)), 5000.0, 1800, ZoneOffset.UTC),
                activity("gap-b", FROM.plus(Duration.ofDays(23)), 7000.0, 2400, ZoneOffset.UTC));
        RunnerBaseline gapBaseline = BaselineEngine.derive(gaps, FROM, TO);
        near(6, gapBaseline.weeklyDistanceKm()); eq(2, gapBaseline.weeklyHistory().size());
        eq(1, gapBaseline.gaps().size()); eq(21L, gapBaseline.gaps().get(0).daysWithoutRecordedRun());
        eq(1, gapBaseline.consistentWeeks());
        RunnerBaseline old = BaselineEngine.derive(runs, FROM, TO.plus(Duration.ofDays(30)));
        eq(BaselineConfidence.LOW, old.confidence());
        check(!PerformanceEligibilityEvaluator.evaluate(old, TO.plus(Duration.ofDays(30))).eligible());
    }

    private static void manualFallbackAndFreshness() {
        RunnerBaseline empty = RunnerBaseline.empty(FROM, TO);
        ManualBaselineInput input = new ManualBaselineInput(30.0, 3.0, 10.0,
                Duration.ofMinutes(27), null, null, 390.0, 8);
        RunnerBaseline manual = BaselineEngine.applyManualOverlay(empty, input, TO.minus(Duration.ofDays(2)));
        near(30, manual.weeklyDistanceKm()); near(3, manual.runsPerWeek()); near(10, manual.longestRunKm());
        near(390, manual.easyPaceSecondsPerKm()); eq(Duration.ofMinutes(27), manual.recent5k());
        eq(null, manual.recent10k()); eq(0, manual.activityCount()); eq(null, manual.lastActivityAt());
        eq(BaselineConfidence.LOW, manual.confidence());
        eq(BaselineProvenance.MANUAL, manual.provenance().get(RunnerBaseline.WEEKLY_DISTANCE));
        eq(TO.minus(Duration.ofDays(2)), manual.manualRecordedAt());
        check(PerformanceEligibilityEvaluator.evaluate(manual, TO).eligible());
        check(!PerformanceEligibilityEvaluator.evaluate(manual, TO.plus(Duration.ofDays(20))).eligible());
        // Merely recomputing the history window cannot refresh old manual testimony.
        RunnerBaseline reloaded = BaselineEngine.applyManualOverlay(RunnerBaseline.empty(FROM, TO.plus(Duration.ofDays(60))), input, manual.manualRecordedAt());
        check(!PerformanceEligibilityEvaluator.evaluate(reloaded, TO.plus(Duration.ofDays(60))).eligible());
        RunnerBaseline timestampUnknown = BaselineEngine.applyManualOverlay(empty, input);
        check(!PerformanceEligibilityEvaluator.evaluate(timestampUnknown, TO).eligible());
        RunnerBaseline corrected = BaselineEngine.applyManualOverlay(BaselineEngine.derive(regularRuns(4, 8, 3, 10), FROM, TO),
                new ManualBaselineInput(15.0, null, null, null, null, null, null, null), TO);
        near(15, corrected.weeklyDistanceKm()); near(3, corrected.runsPerWeek()); near(10, corrected.longestRunKm());
        eq(BaselineConfidence.MODERATE, corrected.confidence());
        RunnerBaseline zero = BaselineEngine.applyManualOverlay(empty,
                new ManualBaselineInput(0.0, 0.0, 0.0, null, null, null, null, 0), TO);
        near(0, zero.weeklyDistanceKm()); eq(BaselineProvenance.MANUAL, zero.provenance().get(RunnerBaseline.WEEKLY_DISTANCE));
        eq(null, zero.typicalRunMinutes());
    }

    private static void performanceUsesEvidence() {
        RunnerBaseline strong = BaselineEngine.derive(regularRuns(4, 8, 3, 10), FROM, TO);
        EligibilityDecision decision = PerformanceEligibilityEvaluator.evaluate(strong, TO);
        check(decision.eligible()); check(!decision.validUntil().isBefore(TO));
        check(decision.reasons().stream().anyMatch(reason -> reason.contains("heuristics")));
        RunnerBaseline lowVolume = BaselineEngine.derive(regularRuns(4, 8, 3, 5), FROM, TO);
        check(!PerformanceEligibilityEvaluator.evaluate(lowVolume, TO).eligible());
        RunnerBaseline twoRuns = BaselineEngine.derive(regularRuns(4, 8, 2, 15), FROM, TO);
        check(!PerformanceEligibilityEvaluator.evaluate(twoRuns, TO).eligible());
        RunnerBaseline returning = BaselineEngine.derive(Arrays.asList(
                activity("old1", FROM.plus(Duration.ofDays(1)), 10000.0, 3600, ZoneOffset.UTC),
                activity("new1", TO.minus(Duration.ofDays(2)), 2000.0, 900, ZoneOffset.UTC)), FROM, TO);
        eq(1, returning.consistentWeeks()); check(!PerformanceEligibilityEvaluator.evaluate(returning, TO).eligible());
        // A Phase 1 compatibility flag is not an input to the evaluator.
        UserProfile staleFlag = SyntheticRunnerFixtures.profiles(LocalDate.of(2025, 1, 6)).get(0).profile();
        UserProfile flagged = new UserProfile(staleFlag.raceGoal(), staleFlag.preferences(), true);
        check(flagged.performanceEligible()); check(!PerformanceEligibilityEvaluator.evaluate(RunnerBaseline.empty(FROM, TO), TO).eligible());
        RunnerBaseline partialManual = BaselineEngine.applyManualOverlay(BaselineEngine.derive(regularRuns(0, 6, 3, 10), FROM, TO),
                new ManualBaselineInput(30.0, null, null, null, null, null, null, null), TO);
        check(!PerformanceEligibilityEvaluator.evaluate(partialManual, TO).eligible());
    }

    private static void mixedEvidenceDeadlines() {
        ArrayList<ActivityRecord> runs = new ArrayList<>(regularRuns(2, 8, 3, 10));
        runs.add(activity("latest-supporting-record", TO.minus(Duration.ofDays(13)), 10000.0, 3600, ZoneOffset.UTC));
        RunnerBaseline recorded = BaselineEngine.derive(runs, FROM, TO);
        RunnerBaseline mixed = BaselineEngine.applyManualOverlay(recorded,
                new ManualBaselineInput(30.0, 3.0, null, null, null, null, null, null), TO);
        EligibilityDecision current = PerformanceEligibilityEvaluator.evaluate(mixed, TO);
        check(current.eligible());
        eq(TO.plus(Duration.ofDays(1)), current.validUntil());
        check(PerformanceEligibilityEvaluator.evaluate(mixed, current.validUntil()).eligible());
        check(!PerformanceEligibilityEvaluator.evaluate(mixed, current.validUntil().plusSeconds(1)).eligible());
        eq(TO.plus(Duration.ofDays(1)), PerformanceEligibilityEvaluator.evaluate(recorded, TO).validUntil());

        RunnerBaseline fresherRecorded = BaselineEngine.derive(regularRuns(4, 8, 3, 10), FROM, TO);
        RunnerBaseline olderManual = BaselineEngine.applyManualOverlay(fresherRecorded,
                new ManualBaselineInput(30.0, null, null, null, null, null, null, null), TO.minus(Duration.ofDays(13)));
        check(PerformanceEligibilityEvaluator.evaluate(olderManual, TO).eligible());
        eq(TO.plus(Duration.ofDays(1)), PerformanceEligibilityEvaluator.evaluate(olderManual, TO).validUntil());

        RunnerBaseline entirelyManual = BaselineEngine.applyManualOverlay(recorded,
                new ManualBaselineInput(30.0, 3.0, 10.0, null, null, null, null, 8), TO);
        check(PerformanceEligibilityEvaluator.evaluate(entirelyManual, TO).eligible());
        eq(TO.plus(Duration.ofDays(14)), PerformanceEligibilityEvaluator.evaluate(entirelyManual, TO).validUntil());
        RunnerBaseline unsupported = BaselineEngine.applyManualOverlay(RunnerBaseline.empty(FROM, TO),
                new ManualBaselineInput(30.0, null, null, null, null, null, null, null), TO);
        check(!PerformanceEligibilityEvaluator.evaluate(unsupported, TO).eligible());
        eq(TO, PerformanceEligibilityEvaluator.evaluate(unsupported, TO).validUntil());
    }

    private static void assessmentChoicesAndAnalysis() {
        RunnerBaseline empty = RunnerBaseline.empty(FROM, TO);
        eq(1, AssessmentAnalyzer.recommend(empty).distanceKm());
        RunnerBaseline occasional = BaselineEngine.applyManualOverlay(empty,
                new ManualBaselineInput(10.0, 2.0, 4.0, null, null, null, null, 4), TO);
        eq(3, AssessmentAnalyzer.recommend(occasional).distanceKm());
        RunnerBaseline limited = BaselineEngine.applyManualOverlay(empty,
                new ManualBaselineInput(4.0, 1.0, 2.0, null, null, null, null, 3), TO);
        eq(2, AssessmentAnalyzer.recommend(limited).distanceKm());
        RunnerBaseline strong = BaselineEngine.derive(regularRuns(4, 8, 3, 10), FROM, TO);
        check(AssessmentAnalyzer.recommend(strong).unnecessary());
        eq(5, AssessmentAnalyzer.recommend(strong).distanceKm());
        BaselineAssessment chosen = new BaselineAssessment(4, AssessmentMode.CONTROLLED);
        eq(4, chosen.distanceKm()); // Recommendations do not constrain the chosen assessment.
        AssessmentResult oneK = new AssessmentResult("a1", new BaselineAssessment(1, AssessmentMode.CONTROLLED),
                Duration.ofMinutes(7), null, TO, null);
        AssessmentSummary oneSummary = AssessmentAnalyzer.analyze(oneK, null, empty);
        near(420, oneSummary.averagePaceSecondsPerKm()); eq(null, oneSummary.finalKilometrePace());
        eq(null, oneSummary.averageHeartRate()); eq(null, oneSummary.cadence());
        eq(BaselineConfidence.LOW, oneSummary.confidence()); near(1, oneSummary.evidenceWeight());
        check(oneSummary.findings().stream().anyMatch(reason -> reason.contains("not a claim of maximal")));
        check(AssessmentAnalyzer.analyze(oneK, null, strong).evidenceWeight() < oneSummary.evidenceWeight());
        ActivityRecord assessment = assessmentActivity();
        AssessmentResult result = new AssessmentResult("a3", new BaselineAssessment(3, AssessmentMode.BEST_EFFORT),
                Duration.ofSeconds(978), 8.0, assessment.end(), assessment.id());
        AssessmentSummary summary = AssessmentAnalyzer.analyze(result, assessment, empty);
        near(326, summary.averagePaceSecondsPerKm()); near(330, summary.firstKilometrePace());
        near(314, summary.finalKilometrePace()); near(151, summary.averageHeartRate());
        check(summary.paceFadePercent() < 0);
        check(summary.findings().stream().anyMatch(reason -> reason.contains("relatively stable")));
        check(summary.findings().stream().anyMatch(reason -> reason.contains("faster than the first")));
        eq(BaselineConfidence.LOW, summary.confidence());
        invalid(() -> AssessmentAnalyzer.analyze(result, activity("wrong-id", FROM, 3000.0, 978, ZoneOffset.UTC), empty));
        ActivityRecord sparse = activity("sparse", FROM, 3000.0, 978, ZoneOffset.UTC);
        AssessmentResult sparseResult = new AssessmentResult("sparse-result", new BaselineAssessment(3, AssessmentMode.BEST_EFFORT), Duration.ofSeconds(978), null, sparse.end(), sparse.id());
        eq(null, AssessmentAnalyzer.analyze(sparseResult, sparse, empty).finalKilometrePace());
    }

    private static void resultAndTimeZoneBoundaries() {
        List<ActivityRecord> runs = Collections.singletonList(activity("exact5k", TO.minus(Duration.ofDays(2)), 5000.0, 1620, ZoneOffset.UTC));
        eq(null, BaselineEngine.derive(runs, FROM, TO).recent5k());
        RecordedResult race = new RecordedResult("race", PerformanceDistance.FIVE_K, Duration.ofMinutes(27),
                LocalDate.ofInstant(TO.minus(Duration.ofDays(2)), ZoneOffset.UTC), "Local race", ResultSource.OFFICIAL_CHIP, null, null);
        RunnerBaseline withResult = BaselineEngine.derive(runs, Collections.singletonList(race), FROM, TO);
        eq(Duration.ofMinutes(27), withResult.recent5k()); eq(BaselineProvenance.MANUAL, withResult.provenance().get(RunnerBaseline.RECENT_5K));
        RecordedResult old = new RecordedResult("old-race", PerformanceDistance.FIVE_K, Duration.ofMinutes(20),
                LocalDate.of(2024, 1, 1), "Old", ResultSource.MANUAL, null, null);
        eq(null, BaselineEngine.derive(runs, Collections.singletonList(old), FROM, TO).recent5k());
        RecordedResult fractional = new RecordedResult("fractional", PerformanceDistance.FIVE_K, Duration.ofMillis(1620123),
                race.date(), null, ResultSource.ROLLING, "exact5k", null);
        eq(Duration.ofMillis(1620123), BaselineEngine.derive(runs, Collections.singletonList(fractional), FROM, TO).recent5k());
        RecordedResult authoritative = new RecordedResult("official-linked", PerformanceDistance.FIVE_K, Duration.ofMinutes(28),
                race.date(), "Chip result", ResultSource.OFFICIAL_CHIP, "exact5k", null);
        eq(Duration.ofMinutes(28), BaselineEngine.derive(runs, Arrays.asList(fractional, authoritative), FROM, TO).recent5k());
        eq(Duration.ofMinutes(27), BaselineEngine.derive(runs, Arrays.asList(fractional, authoritative, race), FROM, TO).recent5k());
        ActivityRecord offsetRun = activity("offset", Instant.parse("2025-01-13T01:00:00Z"), 5000.0, 1800, ZoneOffset.ofHours(-4));
        RunnerBaseline local = BaselineEngine.derive(Collections.singletonList(offsetRun), FROM, TO);
        eq(LocalDate.of(2025, 1, 6), local.weeklyHistory().get(0).weekStarting());
        ActivityRecord outside = activity("outside", FROM.minusSeconds(1), 5000.0, 1800, ZoneOffset.UTC);
        eq(0, BaselineEngine.derive(Collections.singletonList(outside), FROM, TO).activityCount());
        eq(0, BaselineEngine.derive(Collections.singletonList(activity("end", TO, 1000.0, 400, null)), FROM, TO).activityCount());
    }

    private static void immutabilityAndInputBoundaries() {
        invalid(() -> new ManualBaselineInput(-1.0, null, null, null, null, null, null, null));
        invalid(() -> new ManualBaselineInput(Double.NaN, null, null, null, null, null, null, null));
        invalid(() -> new ManualBaselineInput(null, 22.0, null, null, null, null, null, null));
        invalid(() -> new ManualBaselineInput(null, null, null, Duration.ZERO, null, null, null, null));
        invalid(() -> new ManualBaselineInput(null, null, null, null, null, null, 0.0, null));
        invalid(() -> new ManualBaselineInput(null, null, null, null, null, null, null, -1));
        invalid(() -> new BaselineAssessment(0, AssessmentMode.CONTROLLED));
        invalid(() -> new BaselineAssessment(6, AssessmentMode.BEST_EFFORT));
        invalid(() -> new AssessmentResult("bad", new BaselineAssessment(3, AssessmentMode.CONTROLLED), Duration.ZERO, null, TO, null));
        invalid(() -> new AssessmentResult("bad", new BaselineAssessment(3, AssessmentMode.CONTROLLED), Duration.ofMinutes(20), 11.0, TO, null));
        invalid(() -> RunnerBaseline.empty(TO, FROM));
        RunnerBaseline baseline = BaselineEngine.derive(regularRuns(4, 8, 3, 10), FROM, TO);
        unsupported(() -> baseline.provenance().clear()); unsupported(() -> baseline.explanations().clear());
        unsupported(() -> baseline.weeklyHistory().clear()); unsupported(() -> baseline.gaps().add(new RecordedTrainingGap(FROM, TO, 84)));
        check(!PerformanceEligibilityEvaluator.evaluate(BaselineEngine.applyManualOverlay(RunnerBaseline.empty(FROM, TO),
                new ManualBaselineInput(30.0, 3.0, 10.0, null, null, null, null, 8), TO.plusSeconds(1)), TO).eligible());
    }

    private static void regressionFixtureBoundary() {
        List<SyntheticRunnerFixtures.Fixture> fixtures = SyntheticRunnerFixtures.profiles(LocalDate.of(2025, 1, 6));
        eq(5, fixtures.size());
        eq(fixtures, SyntheticRunnerFixtures.profiles(LocalDate.of(2025, 1, 6)));
        eq(RaceDistance.FIVE_K, fixtures.get(0).profile().raceGoal().distance());
        eq(3, fixtures.get(0).profile().preferences().runsPerWeek());
        eq(LocalDate.of(2025, 3, 31), fixtures.get(0).profile().raceGoal().raceDate());
        near(20, fixtures.get(1).baseline().weeklyDistanceKm());
        eq(4, fixtures.get(1).profile().preferences().runsPerWeek());
        near(45, fixtures.get(2).baseline().weeklyDistanceKm());
        check(fixtures.get(2).profile().preferences().gymDays().contains(DayOfWeek.TUESDAY));
        check(fixtures.get(2).profile().preferences().gymDays().contains(DayOfWeek.FRIDAY));
        eq(RaceDistance.GENERAL_FITNESS, fixtures.get(3).profile().raceGoal().distance());
        eq(BaselineProvenance.MANUAL, fixtures.get(3).baseline().provenance().get(RunnerBaseline.WEEKLY_DISTANCE));
        eq(3, fixtures.get(4).assessment().distanceKm());
        eq(AssessmentMode.CONTROLLED, fixtures.get(4).assessment().mode());
        // Whole-plan snapshots await a real generator. This test creates no pretend production plan.
        check(TrainingPlanGenerator.class.isInterface());
    }

    private static List<ActivityRecord> regularRuns(int startWeek, int weeks, int runsPerWeek, double kilometres) {
        ArrayList<ActivityRecord> result = new ArrayList<>();
        int[] days = {1, 3, 5, 6, 2, 4};
        for (int week = startWeek; week < startWeek + weeks; week++) for (int run = 0; run < runsPerWeek; run++) {
            Instant start = FROM.plus(Duration.ofDays(week * 7L + days[run]));
            result.add(activity("r-" + week + "-" + run, start, kilometres * 1000, Math.round(kilometres * 360), ZoneOffset.UTC));
        }
        return result;
    }

    private static ActivityRecord activity(String id, Instant start, Double distance, long seconds, ZoneOffset offset) {
        return new ActivityRecord(id, new SourceIdentity("health-connect", id, "test.sync"), start, start.plusSeconds(seconds), offset, offset,
                distance, null, null, null, null, null, null, null, Collections.emptyList(), Collections.emptyList(), null, null);
    }

    private static ActivityRecord assessmentActivity() {
        ArrayList<DistanceSample> samples = new ArrayList<>();
        double elapsed = 0;
        samples.add(new DistanceSample(Duration.ZERO, 0, 151.0, 170.0, null, false));
        int[] kilometres = {330, 334, 314};
        for (int kilometre = 0; kilometre < 3; kilometre++) for (int point = 1; point <= 20; point++) {
            elapsed += kilometres[kilometre] / 20.0;
            samples.add(new DistanceSample(Duration.ofMillis(Math.round(elapsed * 1000)), kilometre * 1000 + point * 50,
                    151.0, 170.0, null, false));
        }
        Instant start = TO.minus(Duration.ofDays(1));
        return new ActivityRecord("assessment-3k", new SourceIdentity("assessment", "assessment-3k", null), start, start.plusSeconds(978),
                ZoneOffset.UTC, ZoneOffset.UTC, 3000.0, null, 151.0, 165.0, null, 170.0, 8.0, null,
                samples, Collections.emptyList(), null, null);
    }

    private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("Check " + checks + " failed"); }
    private static void eq(Object expected, Object actual) { checks++; if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + " but was " + actual + " at check " + checks); }
    private static void near(double expected, Double actual) { checks++; if (actual == null || Math.abs(expected - actual) > 0.0001) throw new AssertionError("Expected " + expected + " but was " + actual + " at check " + checks); }
    private static void invalid(Runnable action) { checks++; try { action.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError("Expected invalid input at check " + checks); }
    private static void unsupported(Runnable action) { checks++; try { action.run(); } catch (UnsupportedOperationException expected) { return; } throw new AssertionError("Expected immutable collection at check " + checks); }
}
