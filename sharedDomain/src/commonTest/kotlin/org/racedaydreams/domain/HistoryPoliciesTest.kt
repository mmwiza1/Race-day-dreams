package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.time.*

class HistoryPoliciesTest {
    private val today = CalendarDates.of(2026, 11, 13)

    private fun result(
        id: String,
        seconds: Long,
        day: Int,
        source: ResultSource = ResultSource.MANUAL,
        link: String? = null,
    ) =
        RecordedResult(
            id,
            PerformanceDistance.FIVE_K,
            Durations.ofSeconds(seconds, 1),
            CalendarDates.of(2026, 11, day),
            null,
            source,
            link,
            null,
        )

    @Test
    fun comparableHistoryPreservesDailyOrderingAuthorityAndActualImprovement() {
        val rows =
            listOf(
                result("late", 1400, 5),
                result("a", 1600, 1),
                result("gps", 1500, 3, ResultSource.ROLLING, "linked"),
                result("chip", 1600, 3, ResultSource.OFFICIAL_CHIP, "linked"),
                result("better", 1500, 1),
            )
        val history = PersonalBestCalculator.history(rows.reversed())
        assertEquals(listOf("better", "late"), history.map { it.result().id() })
        assertNull(history[0].previousBest())
        assertNull(history[0].improvement())
        assertEquals(Durations.ofSeconds(100), history[1].improvement())
        assertEquals(5, rows.size)
        assertEquals(
            "chip",
            PersonalBestCalculator.current(listOf(rows[2], rows[3])).single().result().id(),
        )
        assertEquals("late", PersonalBestCalculator.current(rows).single().result().id())
        assertFails { (history as MutableList<PersonalBestEntry>).clear() }
    }

    @Test
    fun conflictingIdentitiesAndDistanceCodesStillRejectInsteadOfInventingRecords() {
        val row = result("same", 1500, 1)
        assertEquals(1, PersonalBestCalculator.history(listOf(row, row)).size)
        assertFailsWith<IllegalArgumentException> {
            PersonalBestCalculator.history(listOf(row, result("same", 1600, 1)))
        }
        assertFailsWith<IllegalArgumentException> {
            PersonalBestCalculator.history(
                listOf(
                    row,
                    row.copy(id = "other", distance = PerformanceDistance("5k", 5000.000002, "5K")),
                )
            )
        }
        assertFailsWith<NullPointerException> { PersonalBestCalculator.history(listOf(null)) }
        assertFailsWith<NullPointerException> { PersonalBestCalculator.history(null) }
    }

    @Test
    fun effortAndAnalysisSnapshotsRetainQualificationsAndExactTimes() {
        val reasons = mutableListOf("gap")
        val effort =
            BestEffort(
                PerformanceDistance.FIVE_K,
                Durations.ofNanos(1),
                Durations.ofSeconds(1),
                Durations.ofSeconds(1, 1),
                SampleQuality.UNCERTAIN,
                reasons,
            )
        val efforts = mutableListOf(effort)
        val analysis = BestEffortAnalysis(efforts, SampleQuality.UNCERTAIN, reasons)
        val splits =
            mutableListOf(RunSplit(1, 1000.0, Durations.ofMinutes(5), null, null, null, false))
        val splitAnalysis = SplitAnalysis(splits, SampleQuality.REJECTED, reasons)
        reasons.clear()
        efforts.clear()
        splits.clear()
        assertEquals(listOf("gap"), effort.reasons())
        assertEquals(effort, analysis.efforts().single())
        assertEquals(1, splitAnalysis.splits().size)
        assertEquals(SampleQuality.REJECTED, splitAnalysis.quality())
        assertFailsWith<IllegalArgumentException> {
            effort.copy(endElapsed = Durations.ofSeconds(1))
        }
        assertFails { (analysis.efforts() as MutableList<BestEffort>).clear() }
        assertEquals(Durations.ofNanos(-1), Durations.ofSeconds(1).minus(Durations.ofSeconds(1, 1)))
    }

    private fun activity(id: String, distance: Double?, shoeId: String?) =
        ActivityRecord(
            id,
            SourceIdentity("rdd", id, null),
            Instants.ofEpochSecond(100),
            Instants.ofEpochSecond(200),
            null,
            null,
            distance,
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
            shoeId,
        )

    private val shoe =
        Shoe(" shoe ", " brand ", " model ", CalendarDates.of(2026, 11, 1), null, false, null, true)

    @Test
    fun shoeAssignmentsRecomputeWithoutInventingMissingEarlierMileage() {
        val rows =
            listOf(
                activity("a", 5000.0, " shoe "),
                activity("b", null, " shoe "),
                activity("c", 2000.0, "different"),
            )
        val metrics = ShoeMileage.calculate(shoe, rows, today)
        assertEquals(" shoe ", shoe.id())
        assertEquals("brand", shoe.brand())
        assertEquals("model", shoe.model())
        assertEquals(5000.0, metrics.recordedDistanceMetres())
        assertNull(metrics.totalDistanceMetres())
        assertEquals(2, metrics.runCount())
        assertEquals(1, metrics.runsWithKnownDistance())
        assertEquals(12L, metrics.ageDays())
        assertEquals(5000.0, metrics.averageRunDistanceMetres())
        assertTrue(requireNotNull(metrics.wearGuidance()).contains("incomplete"))
        val reassigned = ShoeMileage.calculate(shoe, rows.drop(1), today)
        assertEquals(0.0, reassigned.recordedDistanceMetres())
        assertNull(reassigned.averageRunDistanceMetres())
        assertNull(
            ShoeMileage.calculate(
                    shoe.copy(purchaseDate = CalendarDates.of(2027, 1, 1)),
                    rows,
                    today,
                )
                .ageDays()
        )
        assertEquals(
            600000.0,
            ShoeMileage.calculate(shoe.copy(startingDistanceMetres = 595000.0), rows, today)
                .totalDistanceMetres(),
        )
    }

    @Test
    fun shoeValidationAndUnknownSummaryFieldsRemainOriginal() {
        assertFailsWith<IllegalArgumentException> { shoe.copy(retired = true) }
        assertFailsWith<IllegalArgumentException> { shoe.copy(startingDistanceMetres = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { shoe.copy(model = " ") }
        assertEquals("\u00a0", shoe.copy(brand = "\u00a0").brand())
        assertNull(RecordedWeek(null, null, 0, 0, 0, null).distanceMetres())
        assertNull(Milestone(null, null, null, null, null, null).date())
        assertNull(
            ShoeMetrics(null, 0.0, null, 0, 0, null, null, null, null, null).firstRecordedRun()
        )
    }

    @Test
    fun sharedRepositoryContractsRetainNullableCursorCheckpointAndAnnotations() {
        var checkpoint: String? = "retained"
        var imported: List<ActivityRecord> = emptyList()
        val repository =
            object : org.racedaydreams.domain.health.HealthImportRepository {
                override fun activities() = imported

                override fun importToken(provider: String) = checkpoint

                override fun applyImport(
                    provider: String,
                    upserts: List<ActivityRecord>,
                    deletedRecordIds: List<String>,
                    nextToken: String?,
                ) {
                    imported = upserts
                    checkpoint = nextToken
                }
            }
        repository.applyImport("test", listOf(activity("actual", null, null)), emptyList(), null)
        assertNull(repository.importToken("test"))
        assertNull(repository.activities().single().distanceMetres())
    }
}
