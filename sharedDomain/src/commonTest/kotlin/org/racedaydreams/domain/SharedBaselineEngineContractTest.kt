package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.time.*

class SharedBaselineEngineContractTest {
    private val from = Instants.ofEpochSecond(4 * 86400L)
    private val to = Instants.ofEpochSecond(60 * 86400L)

    private fun at(day: Long, nano: Long = 0) = Instants.ofEpochSecond((4 + day) * 86400L, nano)

    private fun record(
        id: String,
        day: Long,
        metres: Double? = 10000.0,
        provider: String = "health-connect",
        sourceId: String = id,
        offset: ZoneOffset? = null,
    ) =
        ActivityRecord(
            id,
            SourceIdentity(provider, sourceId, if (provider == "health-connect") "watch" else null),
            at(day),
            Instants.ofEpochSecond(at(day).getEpochSecond() + 3600),
            offset,
            offset,
            metres,
            null,
            null,
            null,
            null,
            null,
            null,
            "",
            emptyList(),
            emptyList(),
            null,
            null,
        )

    private fun history(): List<ActivityRecord> =
        (0 until 8).flatMap { week ->
            listOf(1L, 3L, 5L).map { day -> record("$week-$day", week * 7 + day) }
        }

    private fun baseline() = BaselineEngine.derive(history(), from, to)

    private fun manual(
        distance: Double? = null,
        frequency: Double? = null,
        longest: Double? = null,
        weeks: Int? = null,
    ) = ManualBaselineInput(distance, frequency, longest, null, null, null, null, weeks)

    @Test
    fun absentHistoryStaysUnknownAndCannotEstablishPerformanceOrMaximalFitness() {
        val b = BaselineEngine.derive(emptyList(), from, to)
        assertNull(b.weeklyDistanceKm())
        assertNull(b.runsPerWeek())
        assertNull(b.longestRunKm())
        assertNull(b.easyPaceSecondsPerKm())
        assertNull(b.consistentWeeks())
        assertEquals(BaselineConfidence.LOW, b.confidence())
        assertFalse(PerformanceEligibilityEvaluator.evaluate(b, to).eligible())
        val assessment = AssessmentAnalyzer.recommend(b)
        assertEquals(1, assessment.distanceKm())
        assertFalse(assessment.unnecessary())
    }

    @Test
    fun sufficientRecentMeasuredWeeksRetainOriginalStrongBaseline() {
        val b = baseline()
        assertEquals(24, b.activityCount())
        assertEquals(8, b.weeksWithRuns())
        assertEquals(30.0, b.weeklyDistanceKm())
        assertEquals(3.0, b.runsPerWeek())
        assertEquals(10.0, b.longestRunKm())
        assertEquals(60.0, b.typicalRunMinutes())
        assertEquals(8, b.consistentWeeks())
        assertEquals(BaselineConfidence.STRONG, b.confidence())
        assertEquals(
            BaselineProvenance.HEALTH_CONNECT,
            b.provenance()[RunnerBaseline.WEEKLY_DISTANCE],
        )
        assertTrue(PerformanceEligibilityEvaluator.evaluate(b, to).eligible())
        assertTrue(AssessmentAnalyzer.recommend(b).unnecessary())
        assertNull(b.easyPaceSecondsPerKm())
    }

    @Test
    fun sourceDeduplicationAndExactCrossProviderMirrorsDoNotDeleteHistoricalRecords() {
        val rows = history().toMutableList()
        rows.add(record("copy", 1, sourceId = "0-1"))
        rows.add(record("mirror", 1, provider = "native"))
        val b = BaselineEngine.derive(rows, from, to)
        assertEquals(24, b.activityCount())
        assertEquals(26, rows.size)
        assertTrue(b.explanations().any { it.contains("1 possible mirrored") })
        rows.add(record("independent", 1, sourceId = "different"))
        assertEquals(25, BaselineEngine.derive(rows, from, to).activityCount())
    }

    @Test
    fun missingDistancesExcludeWholeWeekDistanceButNeverInventZeroOrLoseFrequency() {
        val rows = listOf(record("a", 1, null), record("b", 8), record("c", 15))
        val b = BaselineEngine.derive(rows, from, to)
        assertNull(b.weeklyHistory().first().distanceKm())
        assertEquals(10.0, b.weeklyDistanceKm())
        assertEquals(1.0, b.runsPerWeek())
        assertEquals(3, b.activityCount())
        assertNull(BaselineEngine.derive(listOf(record("one", 1)), from, to).weeklyDistanceKm())
    }

    @Test
    fun unobservedWeeksDoNotCountAsZeroAndConsecutiveCountIsLatestRecordedStreak() {
        val b =
            BaselineEngine.derive(
                listOf(record("a", 1), record("b", 22), record("c", 29)),
                from,
                to,
            )
        assertEquals(10.0, b.weeklyDistanceKm())
        assertEquals(3, b.weeksWithRuns())
        assertEquals(2, b.consistentWeeks())
        assertEquals(1, b.gaps().size)
        assertEquals(21L, b.gaps().single().daysWithoutRecordedRun())
    }

    @Test
    fun recordedOffsetsControlWeekMembershipAndOnlyMissingOffsetFallsBackToUtc() {
        val mondayUtc = record("utc", 7)
        val localSunday = record("local", 7, offset = Offsets.ofTotalSeconds(-3 * 3600))
        val a = BaselineEngine.derive(listOf(mondayUtc), from, to).weeklyHistory().single()
        val b = BaselineEngine.derive(listOf(localSunday), from, to).weeklyHistory().single()
        assertEquals(CalendarDates.of(1970, 1, 12), a.weekStarting())
        assertEquals(CalendarDates.of(1970, 1, 5), b.weekStarting())
    }

    @Test
    fun windowIsStartInclusiveEndExclusiveAndInputOrderOnlyBreaksSourceIdentityDuplicates() {
        val b =
            BaselineEngine.derive(
                listOf(
                    record("before", -1),
                    record("first", 0),
                    record("last", 55),
                    record("after", 56),
                ),
                from,
                to,
            )
        assertEquals(2, b.activityCount())
        assertEquals(at(55), b.lastActivityAt())
        assertEquals(
            b,
            BaselineEngine.derive(listOf(record("last", 55), record("first", 0)), from, to),
        )
    }

    @Test
    fun explicitResultsRetainManualVersusRollingProvenanceAndNanosecondTimes() {
        val result =
            RecordedResult(
                "result",
                PerformanceDistance.FIVE_K,
                Durations.ofSeconds(1500, 123),
                CalendarDates.of(1970, 2, 1),
                null,
                ResultSource.MANUAL,
                null,
                null,
            )
        val b = BaselineEngine.derive(history(), listOf(result), from, to)
        assertEquals(result.duration(), b.recent5k())
        assertEquals(BaselineProvenance.MANUAL, b.provenance()[RunnerBaseline.RECENT_5K])
        assertNull(b.recent10k())
        val outside =
            RecordedResult(
                "outside",
                PerformanceDistance.FIVE_K,
                Durations.ofSeconds(1200),
                CalendarDates.of(1970, 3, 2),
                null,
                ResultSource.MANUAL,
                null,
                null,
            )
        assertNull(BaselineEngine.derive(history(), listOf(outside), from, to).recent5k())
    }

    @Test
    fun manualOverlayChangesOnlyExplicitFieldsAndNeverRaisesMeasuredConfidence() {
        val b = baseline()
        val over = BaselineEngine.applyManualOverlay(b, manual(longest = 12.0), at(55))
        assertEquals(12.0, over.longestRunKm())
        assertEquals(b.weeklyDistanceKm(), over.weeklyDistanceKm())
        assertEquals(b.activityCount(), over.activityCount())
        assertEquals(b.weeklyHistory(), over.weeklyHistory())
        assertEquals(b.confidence(), over.confidence())
        assertEquals(BaselineProvenance.MANUAL, over.provenance()[RunnerBaseline.LONGEST_RUN])
        assertEquals(at(55), over.manualRecordedAt())
        assertNull(b.manualRecordedAt())
        assertEquals(10.0, b.longestRunKm())
        assertEquals(
            over.manualRecordedAt(),
            BaselineEngine.applyManualOverlay(over, ManualBaselineInput.empty(), to)
                .manualRecordedAt(),
        )
    }

    @Test
    fun materialCorrectionLowersConfidenceAtOriginalStrictThreshold() {
        val b = baseline()
        assertEquals(
            BaselineConfidence.STRONG,
            BaselineEngine.applyManualOverlay(b, manual(distance = 37.5)).confidence(),
        )
        assertEquals(
            BaselineConfidence.MODERATE,
            BaselineEngine.applyManualOverlay(b, manual(distance = 37.51)).confidence(),
        )
    }

    @Test
    fun mixedEvidenceReviewDeadlineUsesEarlierRecordedExpiry() {
        val b = BaselineEngine.applyManualOverlay(baseline(), manual(distance = 30.0), to)
        val expected = Instants.ofEpochSecond(b.lastActivityAt()!!.getEpochSecond() + 14 * 86400L)
        val decision = PerformanceEligibilityEvaluator.evaluate(b, to)
        assertTrue(decision.eligible())
        assertEquals(expected, decision.validUntil())
        assertTrue(PerformanceEligibilityEvaluator.evaluate(b, expected, expected).eligible())
        val later = Instants.ofEpochSecond(expected.getEpochSecond(), 1)
        assertFalse(PerformanceEligibilityEvaluator.evaluate(b, later, later).eligible())
    }

    @Test
    fun entirelyManualHistoryNeedsExplicitRecentReviewAndNeverFabricatesWorkouts() {
        val b =
            BaselineEngine.applyManualOverlay(
                RunnerBaseline.empty(from, to),
                manual(30.0, 3.0, 10.0, 8),
            )
        assertFalse(PerformanceEligibilityEvaluator.evaluate(b, to).eligible())
        assertTrue(PerformanceEligibilityEvaluator.evaluate(b, to, to).eligible())
        assertFalse(
            PerformanceEligibilityEvaluator.evaluate(
                    b,
                    to,
                    Instants.ofEpochSecond(to.getEpochSecond() + 1),
                )
                .eligible()
        )
        assertEquals(0, b.activityCount())
        assertEquals(BaselineConfidence.LOW, b.confidence())
        assertNull(b.lastActivityAt())
    }

    @Test
    fun assessmentWithOnlyEnteredTimeNeverInventsSplitsSensorsOrEasyPace() {
        val r =
            AssessmentResult(
                "assessment",
                BaselineAssessment(3, AssessmentMode.CONTROLLED),
                Durations.ofSeconds(900, 300),
                5.5,
                to,
                null,
            )
        val s = AssessmentAnalyzer.analyze(r, null, baseline())
        assertEquals((900.0 + 0.0000003) / 3, s.averagePaceSecondsPerKm())
        assertNull(s.firstKilometrePace())
        assertNull(s.finalKilometrePace())
        assertNull(s.paceFadePercent())
        assertNull(s.averageHeartRate())
        assertNull(s.cadence())
        assertTrue(s.findings().any { it.contains("controlled assessment") })
        assertEquals(1.0 / 9.0, s.evidenceWeight())
        assertFails { (s.findings() as MutableList).clear() }
    }

    @Test
    fun assessmentLinkMustMatchAndAggregateDistanceCannotFabricateReliableSplits() {
        val r =
            AssessmentResult(
                "a",
                BaselineAssessment(1, AssessmentMode.BEST_EFFORT),
                Durations.ofSeconds(300),
                null,
                to,
                "correct",
            )
        assertFailsWith<IllegalArgumentException> {
            AssessmentAnalyzer.analyze(r, record("wrong", 1, 1000.0), baseline())
        }
        val s = AssessmentAnalyzer.analyze(r, record("correct", 1, 1000.0), baseline())
        assertNull(s.firstKilometrePace())
        assertNull(s.finalKilometrePace())
        assertTrue(s.findings().any { it.contains("Reliable kilometre samples are unavailable") })
    }

    @Test
    fun resultSnapshotsCannotBeMutatedThroughSourceAliasesOrReturnedCollections() {
        val reasons = mutableListOf("original")
        val decision = EligibilityDecision(true, reasons, to)
        val summary =
            AssessmentSummary(
                300.0,
                null,
                null,
                null,
                null,
                null,
                BaselineConfidence.LOW,
                1.0,
                reasons,
            )
        reasons.add("later")
        assertEquals(listOf("original"), decision.reasons())
        assertEquals(listOf("original"), summary.findings())
        assertFails { (decision.reasons() as MutableList).clear() }
        val b = baseline()
        assertFails { (b.provenance() as MutableMap).clear() }
        assertFails { (b.weeklyHistory() as MutableList).clear() }
    }

    @Test
    fun baselineAndObservedWeekValidationStillRejectsUnsupportedEvidence() {
        assertFailsWith<IllegalArgumentException> { RunnerBaseline.empty(to, from) }
        assertFailsWith<IllegalArgumentException> {
            WeeklyRunningSummary(CalendarDates.of(1970, 1, 5), 0, null, true)
        }
        assertFailsWith<IllegalArgumentException> { RecordedTrainingGap(from, to, 0) }
        assertFailsWith<IllegalArgumentException> {
            AssessmentRecommendation(6, false, emptyList())
        }
        assertFailsWith<NullPointerException> { EligibilityDecision(false, emptyList(), null) }
    }
}
