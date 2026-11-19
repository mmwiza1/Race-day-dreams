package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.health.*
import org.racedaydreams.domain.presentation.*
import org.racedaydreams.domain.time.*

class HealthContextContractTest {
    private val today = CalendarDates.of(2026, 11, 15)

    private fun instant(day: LocalDate, second: Long = 0) =
        Instants.ofEpochSecond(day.toEpochDay() * 86400 + second)

    private fun row(
        metric: HealthIntelligence.Metric,
        daysAgo: Int,
        value: Double,
        source: String = "same",
        second: Long = 0,
    ): HealthIntelligence.Observation {
        val date = today.minusDays(daysAgo.toLong())
        val start = instant(date, second)
        return HealthIntelligence.Observation(
            "${metric.name}-$daysAgo-$second-$source",
            metric,
            date,
            start,
            Instants.ofEpochSecond(start.getEpochSecond() + 60),
            value,
            source,
            instant(today),
        )
    }

    private fun trend(
        metric: HealthIntelligence.Metric,
        interpretation: String,
        recentDays: Int = 7,
        referenceDays: Int = 28,
    ) =
        HealthIntelligence.Trend(
            metric,
            "same",
            50.0,
            60.0,
            recentDays,
            referenceDays,
            interpretation,
            "Supporting context, not diagnosis.",
        )

    @Test
    fun optionalMissingDataStaysUnknownAndDoesNotCreateAReadinessScore() {
        val trends = HealthIntelligence.evaluate(emptyList(), today)
        assertEquals(4, trends.size)
        for (t in trends) {
            assertNull(t.recent())
            assertNull(t.baseline())
            assertEquals(0, t.recentDays())
            assertEquals("Not available", HealthContextSummary.of(t).value())
        }
        val recovery = RecoveryCoaching.evaluate(trends, false, false, false)
        assertFalse(recovery.reviewSuggested())
        assertEquals(listOf("Keep planned workout"), recovery.options())
        assertTrue(recovery.evidence()!!.single().contains("Missing signals remain unknown"))
    }

    @Test
    fun aNewSourceCannotBorrowAnOldSourcesBaseline() {
        val metric = HealthIntelligence.Metric.RESTING_HR
        val input = (1..34).map { row(metric, it, 60.0, "old") } + row(metric, 0, 90.0, "new")
        val t = HealthIntelligence.evaluate(input, today).first { it.metric() == metric }
        assertEquals("new", t.source())
        assertEquals(1, t.recentDays())
        assertEquals(0, t.referenceDays())
        assertNull(t.baseline())
        assertEquals("Insufficient evidence", t.interpretation())
    }

    @Test
    fun duplicateSleepUsesTheLongestSessionAndOtherSignalsUseLatestSameSource() {
        val input =
            listOf(
                row(HealthIntelligence.Metric.SLEEP_MINUTES, 0, 480.0),
                row(HealthIntelligence.Metric.SLEEP_MINUTES, 0, 300.0, second = 1),
                row(HealthIntelligence.Metric.RESTING_HR, 0, 60.0),
                row(HealthIntelligence.Metric.RESTING_HR, 0, 70.0, second = 1),
            )
        val trends = HealthIntelligence.evaluate(input, today)
        assertEquals(
            480.0,
            trends.first { it.metric() == HealthIntelligence.Metric.SLEEP_MINUTES }.recent(),
        )
        assertEquals(
            70.0,
            trends.first { it.metric() == HealthIntelligence.Metric.RESTING_HR }.recent(),
        )
        assertEquals(
            1,
            trends.first { it.metric() == HealthIntelligence.Metric.SLEEP_MINUTES }.recentDays(),
        )
    }

    @Test
    fun thirtyFiveDayWindowSeparatesSevenRecentDaysFromTwentyEightReferenceDays() {
        val metric = HealthIntelligence.Metric.RESTING_HR
        val input = (-1..35).map { row(metric, it, if (it in 0..6) 90.0 else 60.0) }
        val t = HealthIntelligence.evaluate(input, today).first { it.metric() == metric }
        assertEquals(7, t.recentDays())
        assertEquals(28, t.referenceDays())
        assertEquals(90.0, t.recent())
        assertEquals(60.0, t.baseline())
        assertEquals("Above recent personal baseline", t.interpretation())
    }

    @Test
    fun interpretationNeedsBothRecentAndReferenceCoverage() {
        val metric = HealthIntelligence.Metric.HRV_RMSSD
        val input = (0..2).map { row(metric, it, 20.0) } + (7..34).map { row(metric, it, 50.0) }
        val t = HealthIntelligence.evaluate(input, today).first { it.metric() == metric }
        assertEquals(3, t.recentDays())
        assertEquals("Insufficient evidence", t.interpretation())
        assertEquals("Not enough comparable readings", HealthContextSummary.of(t).context())
        assertFalse(RecoveryCoaching.evaluate(listOf(t), false, true, false).reviewSuggested())
    }

    @Test
    fun isolatedSignalsNeverBecomeAnAutomaticRecoveryChange() {
        val sleep = trend(HealthIntelligence.Metric.SLEEP_MINUTES, "Below recent personal baseline")
        val rhr = trend(HealthIntelligence.Metric.RESTING_HR, "Above recent personal baseline")
        val hrv = trend(HealthIntelligence.Metric.HRV_RMSSD, "Below recent personal baseline")
        assertFalse(RecoveryCoaching.evaluate(listOf(sleep), true, false, false).reviewSuggested())
        assertFalse(RecoveryCoaching.evaluate(listOf(hrv), false, false, false).reviewSuggested())
        assertTrue(
            RecoveryCoaching.evaluate(listOf(sleep, rhr), true, false, false).reviewSuggested()
        )
        assertTrue(RecoveryCoaching.evaluate(listOf(hrv), false, true, false).reviewSuggested())
        assertEquals(5, RecoveryCoaching.evaluate(listOf(hrv), false, true, false).options()!!.size)
    }

    @Test
    fun easyAlternativeKeepsTheOriginalCeilingsAndProtectsRaceGoals() {
        val original =
            PlannedWorkout(
                "w",
                today,
                WorkoutCategory.INTERVALS,
                5.0,
                Durations.ofMinutes(40),
                emptyList(),
                "Original",
            )
        val alternative = RecoveryCoaching.easyAlternative(original, "Athlete choice")
        assertEquals(WorkoutCategory.INTERVALS, original.category())
        assertEquals(WorkoutCategory.EASY_RUN, alternative.category())
        assertEquals(original.id(), alternative.id())
        assertEquals(original.date(), alternative.date())
        assertEquals(original.distanceKm(), alternative.distanceKm())
        assertEquals(original.duration(), alternative.duration())
        assertEquals(3.0, alternative.segments().single().rpe())
        val race =
            PlannedWorkout("race", today, WorkoutCategory.RACE, 5.0, null, emptyList(), "Race")
        assertFailsWith<IllegalArgumentException> {
            RecoveryCoaching.easyAlternative(race, "Choice")
        }
    }

    @Test
    fun accessAndHistoryWindowsUseProviderFactsAndPreserveExactTime() {
        assertEquals(
            HealthReadPolicy.Access.UNAVAILABLE,
            HealthReadPolicy.access(false, true, true),
        )
        assertEquals(HealthReadPolicy.Access.DENIED, HealthReadPolicy.access(true, false, true))
        assertEquals(HealthReadPolicy.Access.PARTIAL, HealthReadPolicy.access(true, true, false))
        assertEquals(HealthReadPolicy.Access.READY, HealthReadPolicy.access(true, true, true))
        val now = Instants.ofEpochSecond(today.toEpochDay() * 86400, 123456789)
        for (extended in listOf(false, true)) {
            val from = HealthReadPolicy.windowStart(now, extended)
            assertEquals(123456789, from.getNano())
            assertEquals(
                (if (extended) 84 else 29) * 86400L,
                Durations.between(from, now).getSeconds(),
            )
        }
    }

    @Test
    fun observationValidationKeepsMissingDistinctFromZeroAndRejectsInvalidTimes() {
        assertEquals(0.0, row(HealthIntelligence.Metric.STEPS, 0, 0.0).value())
        assertEquals(0.0, row(HealthIntelligence.Metric.SLEEP_MINUTES, 0, 0.0).value())
        for (metric in
            listOf(
                HealthIntelligence.Metric.RESTING_HR,
                HealthIntelligence.Metric.HRV_RMSSD,
            )) assertFailsWith<IllegalArgumentException> { row(metric, 0, 0.0) }
        assertFailsWith<IllegalArgumentException> {
            row(HealthIntelligence.Metric.STEPS, 0, Double.NaN)
        }
        assertFailsWith<IllegalArgumentException> { row(HealthIntelligence.Metric.STEPS, 0, -1.0) }
    }

    @Test
    fun summaryLabelsRemainMeasurementsAndGeneralContextNotDiagnosis() {
        val hrv = trend(HealthIntelligence.Metric.HRV_RMSSD, "Within recent personal range")
        assertEquals("50 ms", HealthContextSummary.of(hrv).value())
        assertEquals("Around your usual range", HealthContextSummary.of(hrv).context())
        assertEquals("-0", healthWholeDecimal(-0.0))
        assertEquals("-1", healthWholeDecimal(-0.5))
        assertEquals("1", healthWholeDecimal(0.5))
        assertEquals("1,234", healthGrouped(1234))
        assertEquals("-9,223,372,036,854,775,808", healthGrouped(Long.MIN_VALUE))
    }

    private fun run(
        id: String,
        provider: String = "hc",
        secondsAgo: Long = 3600,
    ): org.racedaydreams.domain.activity.ActivityRecord {
        val start = Instants.ofEpochSecond(instant(today).getEpochSecond() - secondsAgo)
        return org.racedaydreams.domain.activity.ActivityRecord(
            "$provider:$id",
            org.racedaydreams.domain.activity.SourceIdentity(provider, id, null),
            start,
            Instants.ofEpochSecond(start.getEpochSecond() + 600),
            Offsets.ofTotalSeconds(0),
            Offsets.ofTotalSeconds(0),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            emptyList(),
            emptyList(),
            null,
            null,
        )
    }

    private fun batch(
        upserts: List<org.racedaydreams.domain.activity.ActivityRecord>,
        deleted: List<String> = emptyList(),
        complete: Boolean = false,
    ) =
        HealthImportPolicy.Batch(
            "hc",
            upserts,
            deleted,
            "next",
            Instants.ofEpochSecond(instant(today).getEpochSecond() - 86400),
            instant(today),
            complete,
        )

    @Test
    fun partialHealthKitVisibilityNeverMeansRetainedRecordsDisappeared() {
        val existing =
            listOf(run("a"), run("b"), run("manual", "manual"), run("old", secondsAgo = 90 * 86400))
        assertEquals(emptyList(), HealthImportPolicy.deletions(existing, batch(emptyList())))
        assertEquals(
            listOf("a", "b"),
            HealthImportPolicy.deletions(existing, batch(emptyList(), complete = true)),
        )
    }

    @Test
    fun explicitDeletionKeepsOutsideWindowMeaningAndAnUpsertWins() {
        val a = run("a")
        val outside = run("old", secondsAgo = 90 * 86400)
        assertEquals(
            listOf("old"),
            HealthImportPolicy.deletions(listOf(a, outside), batch(listOf(a), listOf("a", "old"))),
        )
        val straddles = run("edge", secondsAgo = 86401)
        assertEquals(
            emptyList(),
            HealthImportPolicy.deletions(listOf(straddles), batch(emptyList(), complete = true)),
        )
    }

    @Test
    fun importIdentityIsAnOpaqueTupleAndBatchOwnershipIsFrozen() {
        val a = run("first\nsecond", "provider")
        val b = run("second", "provider\nfirst")
        assertEquals(listOf(a, b), HealthImportPolicy.unique(listOf(a, b, a)))
        val original = mutableListOf(run("a"))
        val deleted = mutableListOf("old")
        val staged = batch(original, deleted)
        original.clear()
        deleted.clear()
        assertEquals(1, staged.upserts().size)
        assertEquals(listOf("old"), staged.deletedRecordIds())
    }

    @Test
    fun invalidOrMixedProviderImportsAreRejectedBeforeReplacement() {
        assertFailsWith<IllegalArgumentException> { batch(listOf(run("a"), run("a"))) }
        assertFailsWith<IllegalArgumentException> { batch(listOf(run("a", "manual"))) }
        assertFailsWith<IllegalArgumentException> {
            HealthImportPolicy.Batch(
                "hc",
                emptyList(),
                emptyList(),
                null,
                instant(today),
                instant(today),
                true,
            )
        }
    }
}
