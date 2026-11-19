package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.race.*
import org.racedaydreams.domain.time.*

class RaceAndChoiceContractTest {
    private val today = CalendarDates.of(2026, 11, 15)
    private val now = Instants.ofEpochSecond(today.toEpochDay() * 86400 + 43200)

    private fun option(id: String, action: AdjustmentOption.Action) =
        AdjustmentOption(id, id, action, emptyList(), "Athlete chooses")

    private fun result(id: String, days: Int = 0, seconds: Long = 3000, activity: String? = null) =
        RecordedResult(
            id,
            PerformanceDistance.TEN_K,
            Durations.ofSeconds(seconds),
            today.minusDays(days.toLong()),
            null,
            ResultSource.MANUAL,
            activity,
            null,
        )

    private fun run(id: String, days: Int, metres: Double?): ActivityRecord {
        val at = Instants.ofEpochSecond(now.getEpochSecond() - days * 86400L)
        return ActivityRecord(
            id,
            SourceIdentity("native", id, null),
            at,
            Instants.ofEpochSecond(at.getEpochSecond() + 3600),
            Offsets.ofTotalSeconds(0),
            Offsets.ofTotalSeconds(0),
            metres,
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

    private fun endurance() =
        (0..3).flatMap { week ->
            listOf(run("long$week", week * 7, 31000.0), run("other$week", week * 7 + 1, 12000.0))
        }

    @Test
    fun actualObservationsKeepMissingMetricsAndOriginalIds() {
        val actual = CompletedWorkout(" id ", null, now, null, null, null, null, null)
        assertEquals(" id ", actual.id())
        assertNull(actual.plannedWorkoutId())
        assertNull(actual.distanceKm())
        assertNull(actual.duration())
        assertNull(actual.averageHeartRate())
        assertNull(actual.rpe())
        assertNull(actual.note())
        assertFailsWith<IllegalArgumentException> {
            CompletedWorkout("a", null, now, -1.0, null, null, null, null)
        }
        assertFailsWith<IllegalArgumentException> {
            CompletedWorkout("a", null, now, null, null, null, 11.0, null)
        }
    }

    @Test
    fun gymEvidenceRequiresItsOwnTimestampAndNeverAssumesAnExternalProvider() {
        val gym =
            GymSession(
                "g",
                now,
                GymSession.Type.FULL_BODY,
                Durations.ofSeconds(0),
                GymSession.Effort.MODERATE,
                null,
                GymSession.Source.MANUAL,
                null,
            )
        assertEquals(GymSession.Source.MANUAL, gym.source())
        assertNull(gym.externalId())
        assertTrue(gym.duration().isZero())
        assertFailsWith<NullPointerException> {
            GymSession(
                "g",
                null,
                GymSession.Type.OTHER,
                Durations.ofMinutes(30),
                GymSession.Effort.EASY,
                null,
                GymSession.Source.MANUAL,
                null,
            )
        }
    }

    @Test
    fun recoveryObservationsRemainOptionalAndNonDiagnostic() {
        val missing = RecoveryMetrics(null, null, null, null, "Unknown")
        assertNull(missing.sleep())
        assertNull(missing.restingHeartRate())
        assertNull(missing.fatigue())
        assertNull(missing.soreness())
        assertEquals(
            0.0,
            RecoveryMetrics(Durations.ofSeconds(0), 0.0, 0.0, 0.0, null).restingHeartRate(),
        )
        assertFailsWith<IllegalArgumentException> {
            RecoveryMetrics(null, Double.NaN, null, null, null)
        }
    }

    @Test
    fun aProposalMustRetainARealKeepOriginalChoiceAndValidSelection() {
        val keep = option("keep", AdjustmentOption.Action.KEEP_ORIGINAL)
        val change = option("easy", AdjustmentOption.Action.EASY_RUN)
        val options = mutableListOf(keep, change)
        val proposal = PlanAdjustment("a", "p", now, "Optional", "Evidence", options, null)
        options.clear()
        assertEquals(2, proposal.options().size)
        assertNull(proposal.selectedOptionId())
        assertFailsWith<IllegalArgumentException> {
            PlanAdjustment("a", "p", now, "Optional", "Evidence", listOf(change, change), null)
        }
        assertFailsWith<IllegalArgumentException> {
            PlanAdjustment("a", "p", now, "Optional", "Evidence", listOf(keep, change), "missing")
        }
        assertEquals(
            "keep",
            PlanAdjustment("a", "p", now, "Optional", "Evidence", listOf(keep, change), "keep")
                .selectedOptionId(),
        )
    }

    @Test
    fun predictionEvidenceIsFrozenAndRangesKeepFractionalTime() {
        val evidence = mutableListOf("Recorded result")
        val lower = Durations.ofSeconds(1, 1)
        val upper = Durations.ofSeconds(1, 2)
        val prediction =
            RacePrediction(RaceDistance.FIVE_K, lower, upper, now, evidence, "Not a guarantee")
        evidence.clear()
        assertEquals(listOf("Recorded result"), prediction.evidence())
        assertEquals(1, prediction.lowerFinishTime().getNano())
        assertEquals(2, prediction.upperFinishTime().getNano())
        assertFailsWith<IllegalArgumentException> {
            RacePrediction(RaceDistance.FIVE_K, upper, lower, now, emptyList(), "Limits")
        }
        assertFailsWith<IllegalArgumentException> {
            RacePrediction(
                RaceDistance.FIVE_K,
                Durations.ofSeconds(0),
                upper,
                now,
                emptyList(),
                "Limits",
            )
        }
    }

    @Test
    fun internalReadinessIsBoundedAndCopiesItsEvidence() {
        val rows = mutableListOf(RaceReadiness.Indicator("Training", 50, "Internal context"))
        val readiness = RaceReadiness(50, now, rows)
        rows.clear()
        assertEquals(1, readiness.indicators().size)
        assertFailsWith<IllegalArgumentException> { RaceReadiness(-1, now, emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            RaceReadiness.Indicator("Training", 101, "Limits")
        }
    }

    @Test
    fun missingOrHillyTargetsUseEffortWithoutInventingPaces() {
        for (hilly in listOf(false, true)) {
            val stages = RaceStrategyEngine.create(5000.0, null, hilly)
            assertEquals(1, stages.size)
            assertNull(stages.single().fastPace())
            assertNull(stages.single().slowPace())
            assertEquals(5000.0, stages.single().to())
        }
        val hilly = RaceStrategyEngine.create(5000.0, 1800, true)
        assertNull(hilly.single().fastPace())
        assertTrue(hilly.single().guidance()!!.contains("No grade-adjusted pace is inferred"))
    }

    @Test
    fun targetStrategyHasContiguousStagesAndKeepsLateEffortOptional() {
        val stages = RaceStrategyEngine.create(10000.0, 3600, false)
        assertEquals(3, stages.size)
        assertEquals(1500.0, stages[0].to())
        assertEquals(stages[0].to(), stages[1].from())
        assertEquals(8500.0, stages[1].to())
        assertEquals(stages[1].to(), stages[2].from())
        assertEquals(10000.0, stages[2].to())
        assertEquals(363.6, stages[0].fastPace()!!, 1e-9)
        assertEquals(372.6, stages[0].slowPace()!!, 1e-9)
        assertNull(stages[2].fastPace())
        assertTrue(stages[2].guidance()!!.contains("no sprint is required"))
    }

    @Test
    fun unsupportedEvidenceDoesNotFabricateARaceRange() {
        for (results in
            listOf(
                emptyList(),
                listOf(result("one")),
                listOf(result("a", activity = "same"), result("b", activity = "same")),
                listOf(result("old", 121), result("future", -1)),
            )) {
            val p = RacePredictionEngine.predict(5000.0, results, endurance(), today)
            assertNull(p.lowSeconds())
            assertNull(p.highSeconds())
            assertEquals("Insufficient evidence", p.confidence())
        }
    }

    @Test
    fun longRacePredictionRequiresActualEnduranceBeyondShortDistanceSpeed() {
        val results = listOf(result("a"), result("b", 10), result("c", 20))
        val missing = RacePredictionEngine.predict(42195.0, results, emptyList(), today)
        assertNull(missing.lowSeconds())
        val unknown = (0..3).map { run("u$it", it * 7, null) }
        assertNull(RacePredictionEngine.predict(42195.0, results, unknown, today).lowSeconds())
        val supported = RacePredictionEngine.predict(42195.0, results, endurance(), today)
        assertNotNull(supported.lowSeconds())
        assertNotNull(supported.highSeconds())
        assertTrue(supported.lowSeconds()!! < supported.highSeconds()!!)
        assertEquals(0L, supported.lowSeconds()!! % 60)
        assertEquals(0L, supported.highSeconds()!! % 60)
        assertTrue(supported.reasons()!!.last().contains("Your goal is unchanged"))
    }

    @Test
    fun conflictingResultsWidenTheRangeWithoutChangingTheGoal() {
        val stable =
            RacePredictionEngine.predict(
                10000.0,
                listOf(result("a"), result("b", 10)),
                endurance(),
                today,
            )
        val conflicting =
            RacePredictionEngine.predict(
                10000.0,
                listOf(result("a"), result("b", 10, 6000)),
                endurance(),
                today,
            )
        assertTrue(conflicting.reasons()!!.contains("Conflicting performances widen the range."))
        assertTrue(
            conflicting.highSeconds()!! - conflicting.lowSeconds()!! >
                stable.highSeconds()!! - stable.lowSeconds()!!
        )
    }

    @Test
    fun invalidDistancesAndTargetsFailBeforeStrategyOrPrediction() {
        for (distance in listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -1.0)) {
            assertFailsWith<IllegalArgumentException> {
                RaceStrategyEngine.create(distance, null, false)
            }
            assertFailsWith<IllegalArgumentException> {
                RacePredictionEngine.predict(distance, emptyList(), emptyList(), today)
            }
        }
        assertFailsWith<IllegalArgumentException> { RaceStrategyEngine.create(5000.0, 0, false) }
        assertFailsWith<IllegalArgumentException> {
            RacePredictionEngine.predict(100001.0, emptyList(), emptyList(), today)
        }
    }
}
